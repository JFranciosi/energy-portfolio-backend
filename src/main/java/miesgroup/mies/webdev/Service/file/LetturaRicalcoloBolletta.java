package miesgroup.mies.webdev.Service.file;

import io.smallrye.common.constraint.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.Periodo;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.file.FileRepo;
import miesgroup.mies.webdev.Service.bolletta.verBollettaPodService;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.time.YearMonth;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static miesgroup.mies.webdev.Service.file.Lettura.detectCategoriaLetturaSmart;

@ApplicationScoped
public class LetturaRicalcoloBolletta {

    @Inject FileRepo fileRepo;
    @Inject BollettaPodRepo bollettaPodRepo;
    @Inject Lettura lettura;
    @Inject verBollettaPodService verBollettaService;
    // ======================================================
// ===============   DTO SEMPLICI   =====================
// ======================================================
    static class VoceMisura { // per il blocco Contatore matricola ...
        String item;    // es.: "Energia Attiva F1", "Energia Reattiva F2", "Potenza F3", "Reattiva Capacitiva Immessa F1"
        Date dataFine; // data fine riga (mappa il mese)
        double consumo;  // kWh / kW / kvarh (numerico)
    }

    static class VocePrezzo { // per Mese Corrente (quadro di dettaglio) e RICALCOLI (mensilizzati)
        String item;    // es.: "Materia energia F1", "Perdite di rete F2", "Dispacciamento", "TRASPORTI_FISSA", "ASOS_VARIABILE", ecc.
        Date   dataFine; // per i ricalcoli: data fine periodo del mese (es. 31.01.2024 -> Gennaio 2024)
        double prezzo;  // € per quella voce e mese
    }

// ======================================================
// ===============   API PRINCIPALE   ===================
// ======================================================

    /**
     * Elabora una bolletta con ricalcolo secondo la tua specifica:
     * - usa i Periodo già estratti (range totale e range ricalcolo)
     * - aggiorna letture "Contatore matricola ..." su mesi storici (solo differenze) e salva il mese corrente
     * - salva mese corrente "SPESA PER ..." (con sottovoci)
     * - applica ricalcoli mensili "RICALCOLO PER RETTIFICA ..." (solo differenze)
     */
    @Transactional
    public void processaBollettaConRicalcolo(
            Document document,
            String idPod,
            Periodo periodoTotale,              // es.: 01/01/2024 -> 31/10/2024
            @Nullable Periodo periodoRicalcolo, // es.: 01/01/2024 -> 30/09/2024 (può essere null)
            String periodicitaFatturazione      // es.: "Mensile" (già estratta tab POD)
    ) {
        long t0 = System.nanoTime();
        System.out.println("\n" + "=".repeat(100));
        System.out.println("▶ AVVIO processaBollettaConRicalcolo");
        System.out.println("- idPod: " + idPod);
        System.out.println("- periodicitaFatturazione: " + periodicitaFatturazione);
        System.out.println("- periodoTotale: " + FileService.fmt(periodoTotale.getInizio()) + " → " + FileService.fmt(periodoTotale.getFine()));
        if (periodoRicalcolo != null) {
            System.out.println("- periodoRicalcolo: " + FileService.fmt(periodoRicalcolo.getInizio()) + " → " + FileService.fmt(periodoRicalcolo.getFine()));
        } else {
            System.out.println("- periodoRicalcolo: <null> (nessun ricalcolo mensile atteso)");
        }

        // Info grezze sull'XML
        try {
            NodeList nl = document.getElementsByTagName("Line");
            System.out.println("- XML <Line> count: " + (nl != null ? nl.getLength() : 0));
        } catch (Exception ex) {
            System.out.println("! WARN: impossibile contare <Line>: " + ex.getMessage());
        }

        // 1) Mese corrente (dalla fine del range totale)
        String meseCorrente = FileService.canonizzaMese(FileService.monthNameOf(periodoTotale.getFine()));
        int annoCorrente    = FileService.yearOf(periodoTotale.getFine());
        String nomeBolletta = lettura.extractBollettaNome(document);
        System.out.println("- meseCorrente: " + meseCorrente + " " + annoCorrente);
        System.out.println("- nomeBolletta: " + nomeBolletta);

        // 2) ===== LETTURE BLOCCO "CONTATORE MATRICOLA ..." =====
        long tMis0 = System.nanoTime();
        List<VoceMisura> misure = extractLettureContatoreAsList(document); // parsing tabelle consumi per mese, F1/F2/F3
        System.out.println("\n[CONTATORE] Letture contatore trovate: " + misure.size());
        if (misure.isEmpty()) {
            System.out.println("! WARN: nessuna voce trovata nel blocco Contatore. Verificare regex/tabella PDF.");
        } else {
            for (VoceMisura vm : misure) {
                System.out.println("   • " + FileService.fmt(vm.dataFine) + " | item=" + vm.item + " | consumo=" + vm.consumo);
            }
        }

        // raggruppa per mese (canonizzato) -> lista voci
        Map<String, List<VoceMisura>> misurePerMese = misure.stream()
                .collect(Collectors.groupingBy(vm -> FileService.canonizzaMese(FileService.monthNameOf(vm.dataFine)),
                        LinkedHashMap::new, Collectors.toList()));

        // riepilogo grouping
        System.out.println("[CONTATORE] Raggruppamento per mese:");
        if (misurePerMese.isEmpty()) {
            System.out.println("   • <vuoto>");
        } else {
            for (Map.Entry<String, List<VoceMisura>> e : misurePerMese.entrySet()) {
                double f1=0,f2=0,f3=0,
                        r1=0,r2=0,r3=0,
                        rcap1=0,rcap2=0,rcap3=0,        // Reattiva Capacitiva (prelevata)
                        rcapI1=0,rcapI2=0,rcapI3=0,    // Reattiva Capacitiva Immessa
                        p1=0,p2=0,p3=0;

                for (VoceMisura v : e.getValue()) {
                    // Attiva
                    if (v.item.equals("Energia Attiva F1")) f1 += v.consumo;
                    if (v.item.equals("Energia Attiva F2")) f2 += v.consumo;
                    if (v.item.equals("Energia Attiva F3")) f3 += v.consumo;

                    // Reattiva (prelevata)
                    if (v.item.equals("Energia Reattiva F1")) r1 += v.consumo;
                    if (v.item.equals("Energia Reattiva F2")) r2 += v.consumo;
                    if (v.item.equals("Energia Reattiva F3")) r3 += v.consumo;

                    // Reattiva CAPACITIVA (prelevata)
                    if (v.item.equals("Energia Reattiva Capacitiva F1")) rcap1 += v.consumo;
                    if (v.item.equals("Energia Reattiva Capacitiva F2")) rcap2 += v.consumo;
                    if (v.item.equals("Energia Reattiva Capacitiva F3")) rcap3 += v.consumo;

                    // Reattiva CAPACITIVA IMMessa (RCapI)
                    if (v.item.equals("Energia Reattiva Capacitiva Immessa F1")) rcapI1 += v.consumo;
                    if (v.item.equals("Energia Reattiva Capacitiva Immessa F2")) rcapI2 += v.consumo;
                    if (v.item.equals("Energia Reattiva Capacitiva Immessa F3")) rcapI3 += v.consumo;

                    // Potenza
                    if (v.item.equals("Potenza F1")) p1 += v.consumo;
                    if (v.item.equals("Potenza F2")) p2 += v.consumo;
                    if (v.item.equals("Potenza F3")) p3 += v.consumo;
                }

                System.out.println(
                        "   • " + e.getKey()
                                + " | F1_ATT=" + f1 + " F2_ATT=" + f2 + " F3_ATT=" + f3
                                + " | F1_R=" + r1 + " F2_R=" + r2 + " F3_R=" + r3
                                + " | F1_RCap=" + rcap1 + " F2_RCap=" + rcap2 + " F3_RCap=" + rcap3
                                + " | F1_RCapI=" + rcapI1 + " F2_RCapI=" + rcapI2 + " F3_RCapI=" + rcapI3
                                + " | F1_POT=" + p1 + " F2_POT=" + p2 + " F3_POT=" + p3
                );
            }
        }

        long tMis1 = System.nanoTime();
        System.out.println("[CONTATORE] Tempo parsing + grouping: " + FileService.ms(tMis0, tMis1) + " ms");

        // Aggiorna mesi storici; il mese corrente NON lo persistiamo qui (ci pensa saveDataToDatabase)
        System.out.println("\n[CONTATORE] Aggiornamento mesi storici (escludo mese corrente: " + meseCorrente + " " + annoCorrente + ")");
        int persistCreati = 0;

        for (Map.Entry<String, List<VoceMisura>> e : misurePerMese.entrySet()) {
            String mese = e.getKey();
            boolean isMeseCorrente = meseCorrente.equalsIgnoreCase(mese);

            // per robustezza, calcolo l'anno dal gruppo (se incroci di anno: uso l'anno della prima voce)
            int annoGruppo = e.getValue().isEmpty() ? annoCorrente : FileService.yearOf(e.getValue().get(0).dataFine);

            BollettaPod b = bollettaPodRepo.find("idPod = ?1 and mese = ?2 and anno = ?3", idPod, mese, String.valueOf(annoGruppo)).firstResult();
            boolean isNew = false;
            if (b == null) { b = new BollettaPod(); isNew = true; }

            // log sintetico prima di applicare
            System.out.println("   → Mese " + mese + " " + annoGruppo + (isMeseCorrente ? " [MESE CORRENTE]" : " [storico]"));
            System.out.println("     Voci da applicare: " + e.getValue().size());

            // applica misure
            applyMisureToEntity(b, e.getValue()); // mappa: Energia Attiva/Reattiva/Potenza/Cap/Ind immessa -> F1/F2/F3

            // riepilogo post-applicazione
            System.out.println("     Post-apply: TOT_Att=" + nz(b.getTotAtt()) + " (F1=" + nz(b.getF1Att()) + ", F2=" + nz(b.getF2Att()) + ", F3=" + nz(b.getF3Att()) + ")"
                    + " | TOT_R=" + nz(b.getTotR())
                    + " | TOT_RCapI=" + nz(b.getTotRCapI())
                    + " | TOT_RIndI=" + nz(b.getTotRIndI()));

            // persistenza
            if (isMeseCorrente) {
                System.out.println("     🔒 Skip persist (mese corrente sarà gestito da saveDataToDatabase)");
            } else {
                if (isNew) {
                    bollettaPodRepo.persist(b);
                    persistCreati++;
                    System.out.println("     ✔ Persist creato (storico)");
                } else {
                    System.out.println("     ✔ Riga storica già esistente: id=" + b.getId() + " (dirty-check JPA)");
                }
            }
        }
        System.out.println("[CONTATORE] Persist creati (storici): " + persistCreati);

        // 3) ===== MESE CORRENTE – QUADRO DI DETTAGLIO "SPESA PER ..." (con sottovoci) =====

        // a) Letture del mese corrente (costruite dalle misure del blocco "Contatore")
        List<VoceMisura> misureMeseCorrente = misurePerMese.getOrDefault(meseCorrente, Collections.emptyList());
        System.out.println("\n[QUADRO] Misure mese corrente (" + meseCorrente + "): " + misureMeseCorrente.size());
        if (misureMeseCorrente.isEmpty()) {
            System.out.println("! WARN: misure mese corrente vuote. I kWh da misure saranno 0.");
        }

        Map<String, Map<String, Map<String, Integer>>> lettureMeseStub =
                FileService.buildLettureStubFromMisure(meseCorrente, misureMeseCorrente);
        System.out.println("[QUADRO] LettureMeseStub:");
        FileService.logNestedIntMap(lettureMeseStub);

        // b) Spese del mese corrente dal quadro "Spesa per ..."
        Map<String, Map<String, Double>> speseCorrenteRaw = lettura.extractSpesePerMese(document, lettureMeseStub);
        System.out.println("[QUADRO] SpeseCorrenteRaw (keys=" + speseCorrenteRaw.keySet() + ")");
        FileService.logNestedDoubleMap(speseCorrenteRaw);

        // Normalizzo i mesi ed accorpo eventuali duplicati "Ottobre"/"ottobre"
        Map<String, Map<String, Double>> speseCorrente = FileService.normalizeAndMergeNested(speseCorrenteRaw);
        System.out.println("[QUADRO] SpeseCorrente (normalizzate, keys=" + speseCorrente.keySet() + ")");
        FileService.logNestedDoubleMap(speseCorrente);

        // c) kWh dal quadro dedicato (se il PDF li espone: Perdite, Picco, ecc.)
        Map<String, Map<String, Double>> kwhEstratti = lettura.extractKwhPerMese(document);
        System.out.println("[QUADRO] kWhEstratti (da quadro dedicato, keys=" + (kwhEstratti != null ? kwhEstratti.keySet() : "null") + ")");
        FileService.logNestedDoubleMap(kwhEstratti);

        // ✅ Verifiche chiavi critiche sul mese corrente
        System.out.println("[CHECK] Chiavi critiche nel mese corrente (" + meseCorrente + "):");
        Map<String, Double> cat = (kwhEstratti != null) ? kwhEstratti.get(meseCorrente) : null;
        if (cat == null) {
            System.out.println("! WARN: kWhPerMese non contiene la chiave del mese corrente: " + meseCorrente);
        } else {
            boolean hasFuoriPicco = FileService.containsKeyLike(cat, "fuori", "picco");
            boolean hasPicco      = FileService.containsKeyLike(cat, "picco") || FileService.containsKeyLike(cat, "ore picco");
            boolean hasPerdF1     = FileService.containsKeyLike(cat, "perdite", "f1");
            boolean hasPerdF2     = FileService.containsKeyLike(cat, "perdite", "f2");
            boolean hasPerdF3     = FileService.containsKeyLike(cat, "perdite", "f3");
            boolean hasEnVerde    = FileService.containsKeyLike(cat, "energia", "verde") || FileService.containsKeyLike(cat, "vendita energia verde");

            System.out.println("   • Fuori Picco kWh presente? " + hasFuoriPicco);
            System.out.println("   • Picco kWh presente?       " + hasPicco);
            System.out.println("   • Perdite F1/F2/F3 presenti? " + (hasPerdF1 && hasPerdF2 && hasPerdF3));
            System.out.println("   • Energia Verde kWh presente? " + hasEnVerde);

            if (!hasFuoriPicco) System.out.println("! WARN: Manca 'Fuori Picco kWh' nel payload kWhPerMese → controllare regex nel quadro.");
            if (!(hasPerdF1 && hasPerdF2 && hasPerdF3)) System.out.println("! WARN: Manca almeno una 'Perdite di rete F*' nel payload → controllare quadro/estrattore.");
            if (!hasEnVerde) System.out.println("! WARN: 'Energia Verde kWh' non presente nel payload → controllare estrazione (Corrispettivo variabile di vendita energia verde).");
        }

        // 🔎 DEBUG payload verso save
        System.out.println("\n" + "-".repeat(100));
        System.out.println("📦 PAYLOAD verso saveDataToDatabase (MESE CORRENTE: " + meseCorrente + ")");
        System.out.println("- LettureMeseStub:");
        FileService.logNestedIntMap(lettureMeseStub);
        System.out.println("- SpeseCorrente:");
        FileService.logNestedDoubleMap(speseCorrente);
        System.out.println("- kWhPerMese:");
        FileService.logNestedDoubleMap(kwhEstratti);
        System.out.println("-".repeat(100) + "\n");

        // Salvataggio (riempie F1/F2/F3, TOT_Att/TOT_R..., f*_kwh, perdite_kwh, ecc.)
        long tSave0 = System.nanoTime();

        // Periodo mese corrente (1→fine mese)
        Periodo periodoMeseCorrente = FileService.toPeriodoMeseFromDate(periodoTotale.getFine());
        fileRepo.saveDataToDatabase(lettureMeseStub, speseCorrente, idPod, nomeBolletta, periodoMeseCorrente, kwhEstratti);

        long tSave1 = System.nanoTime();
        System.out.println("[SAVE] saveDataToDatabase eseguito in " + FileService.ms(tSave0, tSave1) + " ms");

        // 4) ===== RICALCOLI MENSILI "RICALCOLO PER RETTIFICA ..." =====
        if (periodoRicalcolo != null) {
            System.out.println("\n[RICALCOLI] Avvio lettura ricalcolo per bolletta: " + nomeBolletta + " - POD: " + idPod);
            System.out.println("[RICALCOLI] Periodo ricalcolo totale: " + FileService.fmt(periodoRicalcolo.getInizio()) + " → " + FileService.fmt(periodoRicalcolo.getFine()));
            long tRic0 = System.nanoTime();

            // (a) Estrai dalla bolletta i dati di ricalcolo -> lista (item, data, prezzo)
            List<VocePrezzo> prezziRicalcolo = extractRicalcoliDettaglioAsList(document);
            System.out.println("[RICALCOLI] Voci ricalcolo trovate: " + prezziRicalcolo.size());
            if (prezziRicalcolo.isEmpty()) {
                System.out.println("! WARN: nessuna voce ricalcolo trovata. Controllare intestazioni 'RICALCOLO PER RETTIFICA ...'");
            } else {
                for (VocePrezzo vp : prezziRicalcolo) {
                    System.out.println("   • " + FileService.fmt(vp.dataFine) + " | item=" + vp.item + " | prezzo(€)=" + vp.prezzo);
                }
            }

            // (b) Raggruppo per YearMonth (robusto anche se attraversa anni)
            Map<YearMonth, List<VocePrezzo>> prezziRicPerYM = prezziRicalcolo.stream()
                    .collect(Collectors.groupingBy(v -> FileService.ymOf(v.dataFine), LinkedHashMap::new, Collectors.toList()));

            System.out.println("[RICALCOLI] Raggruppamento per YearMonth:");
            for (Map.Entry<YearMonth, List<VocePrezzo>> en : prezziRicPerYM.entrySet()) {
                double sommaYM = 0;
                for (VocePrezzo vp : en.getValue()) sommaYM += nz(vp.prezzo);
                System.out.println("   • " + en.getKey() + " -> voci=" + en.getValue().size() + " | sommaPrezzi=" + round2(sommaYM));
            }

            // (c) Applico differenze al DB, mese per mese
            int mesiModificati = 0, mesiInvariati = 0, mesiCreati = 0;
            for (Map.Entry<YearMonth, List<VocePrezzo>> en : prezziRicPerYM.entrySet()) {
                YearMonth ym = en.getKey();
                String meseNome = FileService.monthNameOf(ym);  // "Gennaio", ...
                String annoStr  = String.valueOf(ym.getYear());

                System.out.println("[RICALCOLI] Elaboro: " + meseNome + " " + annoStr
                        + " (voci=" + en.getValue().size() + ")");

                BollettaPod row = bollettaPodRepo.find("idPod = ?1 and mese = ?2 and anno = ?3", idPod, meseNome, annoStr).firstResult();

                if (row == null) {
                    System.out.println("   • Nessuna riga trovata in DB → creazione nuova");
                    row = new BollettaPod();
                    row.setIdPod(idPod);
                    row.setNomeBolletta(nomeBolletta);
                    row.setMese(meseNome);
                    row.setAnno(annoStr);
                    row.setMeseAnno(FileService.capitalizeFirstThree(meseNome) + " " + annoStr);
                    row.setPeriodoInizio(new java.sql.Date(periodoTotale.getInizio().getTime()));
                    row.setPeriodoFine(new java.sql.Date(periodoTotale.getFine().getTime()));
                    bollettaPodRepo.persist(row);
                    mesiCreati++;
                } else {
                    System.out.println("   • Riga DB trovata: id=" + row.getId());
                }

                boolean changed = applyPrezziRicalcoloDiff(row, en.getValue()); // aggiorna SOLO campi diversi
                if (changed) {
                    double newGeneration = round2(nz(row.getSpeseEne()) - nz(row.getDispacciamento()));
                    row.setGeneration(newGeneration);
                    System.out.println("   ✔ Differenze applicate. Nuova Generation: " + newGeneration);
                    mesiModificati++;
                } else {
                    System.out.println("   ⌁ Nessuna differenza rilevata: riga invariata");
                    mesiInvariati++;
                }
            }
            long tRic1 = System.nanoTime();
            System.out.println("[RICALCOLI] Summary → mesiCreati=" + mesiCreati + ", mesiModificati=" + mesiModificati + ", mesiInvariati=" + mesiInvariati
                    + " | tempo: " + FileService.ms(tRic0, tRic1) + " ms");
        } else {
            System.out.println("[RICALCOLI] Skippati (periodoRicalcolo == null)");
        }

        long t1 = System.nanoTime();
        System.out.println("⏱ Tempo totale processaBollettaConRicalcolo: " + FileService.ms(t0, t1) + " ms");
        System.out.println("■ FINE processaBollettaConRicalcolo");
        System.out.println("=".repeat(100) + "\n");
    }

    // cerca chiavi "simili" (case-insensitive, tutti i token devono comparire nel nome chiave)
    private boolean containsKeyLike(Map<String, ?> map, String... tokens) {
        if (map == null || map.isEmpty()) return false;
        outer:
        for (String k : map.keySet()) {
            String lk = k.toLowerCase(Locale.ITALY).replace(" ", "");
            for (String t : tokens) {
                if (!lk.contains(t.toLowerCase(Locale.ITALY).replace(" ", ""))) continue outer;
            }
            return true;
        }
        return false;
    }

    private Periodo toPeriodoMeseFromDate(Date anyDayInMonth) {
        Calendar c = Calendar.getInstance();
        c.setTime(anyDayInMonth);
        String year  = String.valueOf(c.get(Calendar.YEAR));
        int month = c.get(Calendar.MONTH) + 1; // 1..12

        java.time.YearMonth ym = java.time.YearMonth.of(Integer.parseInt(year), month);
        java.time.LocalDate start = ym.atDay(1);
        java.time.LocalDate end   = ym.atEndOfMonth();

        return new Periodo(
                java.sql.Date.valueOf(start),
                java.sql.Date.valueOf(end),
                year
        );
    }


// ======================================================
// ===============   PARSING: CONTATORE   ===============
// ======================================================

    /**
     * Estrae le righe dalle tabelle “Contatore matricola ...”
     * (Energia Attiva/Reattiva/Potenza/Cap/Ind immessa).
     * Restituisce un elenco di (item, dataFine, consumo).
     */
    private List<VoceMisura> extractLettureContatoreAsList(Document document) {
        List<VoceMisura> out = new ArrayList<>();
        NodeList lines = document.getElementsByTagName("Line");

        String categoriaCorrente = null;
        String meseCorrente = null;

        for (int i = 0; i < lines.getLength(); i++) {
            String curr = lines.item(i).getTextContent();
            if (curr == null) continue;
            curr = curr.trim();

            String prev = (i > 0) ? lines.item(i - 1).getTextContent() : "";
            String next = (i + 1 < lines.getLength()) ? lines.item(i + 1).getTextContent() : "";

            // Attiva solo dopo incontrato "Contatore matricola"
            if (curr.startsWith("Contatore matricola")) {
                categoriaCorrente = ""; // Reset categoria all'inizio blocco
                meseCorrente = null;
                continue;
            }

            // Stop se si arriva a spese
            if (curr.contains("COSA MI VIENE FATTURATO") || curr.startsWith("SPESA PER LA MATERIA ENERGIA")) break;

            // 1) Identifica categoria lettura con contesto (prev+curr+next)
            String maybeCat = detectCategoriaLetturaSmart(prev, curr, next);
            if (maybeCat != null) {
                if (!maybeCat.equals(categoriaCorrente)) {
                    categoriaCorrente = maybeCat;
                    meseCorrente = null; // sarà recuperato con la riga "Fascia oraria ..."
                    System.out.println("Categoria lettura aggiornata a: " + categoriaCorrente);
                }
                continue; // la categoria è intestazione, la riga corrente non va processata come consumo
            }

            // 2) Se categoria attiva e riga contiene "Fascia oraria", estrai dati
            if (categoriaCorrente != null && !categoriaCorrente.isEmpty() && curr.contains("Fascia oraria")) {
                // Estrai date per derivare il mese
                ArrayList<Date> dates = Lettura.extractDates(curr);
                if (dates.size() >= 2) {
                    meseCorrente = miesgroup.mies.webdev.Service.DateUtils.getMonthFromDateLocalized(dates.get(1));
                }
                if (meseCorrente == null) {
                    meseCorrente = Optional.ofNullable(lettura.estraiMese(curr)).orElse("MeseSconosciuto");
                }

                // Estrai fascia oraria (F1/F2/F3)
                String fascia = lettura.extractFasciaOraria(curr);

                // Estrai valore consumi (kWh/kW/kvarh)
                Double value = Lettura.extractValueFromLine(curr);

                if (fascia != null && value != null) {
                    VoceMisura vm = new VoceMisura();

                    vm.item = (categoriaCorrente + " " + fascia).trim();
                    vm.dataFine = dates.size() >= 2 ? dates.get(1) : null;
                    vm.consumo = value;

                    out.add(vm);

                    System.out.printf("  [+] %s | %s -> %s = %.3f%n",
                            meseCorrente, vm.item, (vm.dataFine != null ? vm.dataFine : "null"), vm.consumo);
                } else {
                    System.out.println("  (skip) Riga senza fascia o valore utilizzabile: " + curr);
                }
            }
        }

        System.out.println("Totale voci contatore estratte: " + out.size());
        return out;
    }


/*
    private void applyMisureToEntity(BollettaPod b, List<VoceMisura> voci) {
        // azzero prima di accumulare (per sicurezza)
        b.setF1R(nz(b.getF1R()));
        b.setF2R(nz(b.getF2R()));
        b.setF3R(nz(b.getF3R()));
        b.setF1RCapI(nz(b.getF1RCapI()));
        b.setF2RCapI(nz(b.getF2RCapI()));
        b.setF3RCapI(nz(b.getF3RCapI()));
        b.setF1RIndI(nz(b.getF1RIndI()));
        b.setF2RIndI(nz(b.getF2RIndI()));
        b.setF3RIndI(nz(b.getF3RIndI()));
        b.setF1Att(nz(b.getF1Att()));
        b.setF2Att(nz(b.getF2Att()));
        b.setF3Att(nz(b.getF3Att()));
        b.setF1Pot(nz(b.getF1Pot()));
        b.setF2Pot(nz(b.getF2Pot()));
        b.setF3Pot(nz(b.getF3Pot()));

        System.out.println("     [applyMisureToEntity] INIZIO accumulo voci: " + voci.size());

        for (VoceMisura v : voci) {
            // ignoro righe 0.0 che altrimenti sovrascrivono/“sporcano”
            boolean isZero = (v.consumo == 0.0);
            switch (v.item) {
                // --- Attiva ---
                case "Energia Attiva F1": {
                    double old = nz(b.getF1Att());
                    double neu = old + v.consumo;
                    b.setF1Att(neu);
                    System.out.println("       Energia Attiva F1: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
                case "Energia Attiva F2": {
                    double old = nz(b.getF2Att());
                    double neu = old + v.consumo;
                    b.setF2Att(neu);
                    System.out.println("       Energia Attiva F2: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
                case "Energia Attiva F3": {
                    double old = nz(b.getF3Att());
                    double neu = old + v.consumo;
                    b.setF3Att(neu);
                    System.out.println("       Energia Attiva F3: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }

                // --- Reattiva (PRELEVATA) -> F*_R (ACCUMULA; ignoro righe a 0.0) ---
                case "Energia Reattiva F1": {
                    if (!isZero) {
                        double old = nz(b.getF1R());
                        double neu = old + v.consumo;
                        b.setF1R(neu);
                        System.out.println("       Energia Reattiva F1: " + old + " + " + v.consumo + " = " + neu);
                    } else {
                        System.out.println("       Energia Reattiva F1: riga 0.0 ignorata");
                    }
                    break;
                }
                case "Energia Reattiva F2": {
                    if (!isZero) {
                        double old = nz(b.getF2R());
                        double neu = old + v.consumo;
                        b.setF2R(neu);
                        System.out.println("       Energia Reattiva F2: " + old + " + " + v.consumo + " = " + neu);
                    } else {
                        System.out.println("       Energia Reattiva F2: riga 0.0 ignorata");
                    }
                    break;
                }
                case "Energia Reattiva F3": {
                    if (!isZero) {
                        double old = nz(b.getF3R());
                        double neu = old + v.consumo;
                        b.setF3R(neu);
                        System.out.println("       Energia Reattiva F3: " + old + " + " + v.consumo + " = " + neu);
                    } else {
                        System.out.println("       Energia Reattiva F3: riga 0.0 ignorata");
                    }
                    break;
                }

                // --- Reattiva CAPACITIVA IMMessa -> F*_RCapI ---
                case "Energia Reattiva Capacitiva Immessa F1": {
                    double old = nz(b.getF1RCapI());
                    double neu = old + v.consumo;
                    b.setF1RCapI(neu);
                    System.out.println("       Reattiva Cap. Imm. F1: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
                case "Energia Reattiva Capacitiva Immessa F2": {
                    double old = nz(b.getF2RCapI());
                    double neu = old + v.consumo;
                    b.setF2RCapI(neu);
                    System.out.println("       Reattiva Cap. Imm. F2: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
                case "Energia Reattiva Capacitiva Immessa F3": {
                    double old = nz(b.getF3RCapI());
                    double neu = old + v.consumo;
                    b.setF3RCapI(neu);
                    System.out.println("       Reattiva Cap. Imm. F3: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }

                // --- Reattiva INDUTTIVA IMMessa -> F*_RIndI ---
                case "Energia Reattiva Induttiva Immessa F1": {
                    double old = nz(b.getF1RIndI());
                    double neu = old + v.consumo;
                    b.setF1RIndI(neu);
                    System.out.println("       Reattiva Ind. Imm. F1: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
                case "Energia Reattiva Induttiva Immessa F2": {
                    double old = nz(b.getF2RIndI());
                    double neu = old + v.consumo;
                    b.setF2RIndI(neu);
                    System.out.println("       Reattiva Ind. Imm. F2: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
                case "Energia Reattiva Induttiva Immessa F3": {
                    double old = nz(b.getF3RIndI());
                    double neu = old + v.consumo;
                    b.setF3RIndI(neu);
                    System.out.println("       Reattiva Ind. Imm. F3: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }

                // --- Potenza ---
                case "Potenza F1": {
                    double old = nz(b.getF1Pot());
                    double neu = old + v.consumo;
                    b.setF1Pot(neu);
                    System.out.println("       Potenza F1: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
                case "Potenza F2": {
                    double old = nz(b.getF2Pot());
                    double neu = old + v.consumo;
                    b.setF2Pot(neu);
                    System.out.println("       Potenza F2: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
                case "Potenza F3": {
                    double old = nz(b.getF3Pot());
                    double neu = old + v.consumo;
                    b.setF3Pot(neu);
                    System.out.println("       Potenza F3: " + old + " + " + v.consumo + " = " + neu);
                    break;
                }
            }
        }

        // totali
        double totAtt = nz(b.getF1Att()) + nz(b.getF2Att()) + nz(b.getF3Att());
        double totR   = nz(b.getF1R())   + nz(b.getF2R())   + nz(b.getF3R());
        double totRCI = nz(b.getF1RCapI()) + nz(b.getF2RCapI()) + nz(b.getF3RCapI());
        double totRII = nz(b.getF1RIndI()) + nz(b.getF2RIndI()) + nz(b.getF3RIndI());

        b.setTotAtt(totAtt);
        b.setTotR(totR);
        b.setTotRCapI(totRCI);
        b.setTotRIndI(totRII);

        System.out.println("     [applyMisureToEntity] TOTALE Att=" + totAtt
                + " | Reattiva=" + totR + " | ReattivaCapI=" + totRCI + " | ReattivaIndI=" + totRII);
    }
 */

    // tolleranza per confronti in double
    private static final double EPS = 1e-6;

    private boolean almostEqual(double a, double b) {
        return Math.abs(a - b) <= EPS;
    }

    private void replaceIfDifferent(String label, double oldVal, double newVal, java.util.function.DoubleConsumer setter) {
        if (almostEqual(oldVal, newVal)) {
            System.out.println("       " + label + ": " + oldVal + " == " + newVal + " (nessuna modifica)");
        } else {
            setter.accept(newVal);
            System.out.println("       " + label + ": " + oldVal + " -> " + newVal + " (sostituito)");
        }
    }

    private void applyMisureToEntity(BollettaPod b, List<VoceMisura> voci) {
        // normalizzo a 0 i null (non azzero i campi! solo nz)
        b.setF1R(nz(b.getF1R()));
        b.setF2R(nz(b.getF2R()));
        b.setF3R(nz(b.getF3R()));
        b.setF1RCapI(nz(b.getF1RCapI()));
        b.setF2RCapI(nz(b.getF2RCapI()));
        b.setF3RCapI(nz(b.getF3RCapI()));
        b.setF1RIndI(nz(b.getF1RIndI()));
        b.setF2RIndI(nz(b.getF2RIndI()));
        b.setF3RIndI(nz(b.getF3RIndI()));
        b.setF1Att(nz(b.getF1Att()));
        b.setF2Att(nz(b.getF2Att()));
        b.setF3Att(nz(b.getF3Att()));
        b.setF1Pot(nz(b.getF1Pot()));
        b.setF2Pot(nz(b.getF2Pot()));
        b.setF3Pot(nz(b.getF3Pot()));

        System.out.println("     [applyMisureToEntity] INIZIO confronto/sostituzione voci: " + voci.size());

        for (VoceMisura v : voci) {
            System.out.println("   [VoceMisura] item=" + v.item + " | consumo=" + v.consumo);
            // ignoro righe 0.0 che altrimenti sovrascrivono/“sporcano”

            switch (v.item) {
                // --- Attiva ---
                case "Energia Attiva F1": {
                    double old = nz(b.getF1Att());
                    replaceIfDifferent("Energia Attiva F1", old, v.consumo, b::setF1Att);
                    break;
                }
                case "Energia Attiva F2": {
                    double old = nz(b.getF2Att());
                    replaceIfDifferent("Energia Attiva F2", old, v.consumo, b::setF2Att);
                    break;
                }
                case "Energia Attiva F3": {
                    double old = nz(b.getF3Att());
                    replaceIfDifferent("Energia Attiva F3", old, v.consumo, b::setF3Att);
                    break;
                }

                // --- Reattiva (PRELEVATA) ---
                case "Energia Reattiva F1": {
                    double old = nz(b.getF1R());
                    replaceIfDifferent("Energia Reattiva F1", old, v.consumo, b::setF1R);
                    break;
                }
                case "Energia Reattiva F2": {
                    double old = nz(b.getF2R());
                    replaceIfDifferent("Energia Reattiva F2", old, v.consumo, b::setF2R);
                    break;
                }
                case "Energia Reattiva F3": {
                    double old = nz(b.getF3R());
                    replaceIfDifferent("Energia Reattiva F3", old, v.consumo, b::setF3R);
                    break;
                }

                // --- Reattiva CAPACITIVA IMMessa ---
                case "Energia Reattiva Capacitiva Immessa F1": {
                    double old = nz(b.getF1RCapI());
                    replaceIfDifferent("Reattiva Cap. Imm. F1", old, v.consumo, b::setF1RCapI);
                    break;
                }
                case "Energia Reattiva Capacitiva Immessa F2": {
                    double old = nz(b.getF2RCapI());
                    replaceIfDifferent("Reattiva Cap. Imm. F2", old, v.consumo, b::setF2RCapI);
                    break;
                }
                case "Energia Reattiva Capacitiva Immessa F3": {
                    double old = nz(b.getF3RCapI());
                    replaceIfDifferent("Reattiva Cap. Imm. F3", old, v.consumo, b::setF3RCapI);
                    break;
                }

                // --- Reattiva INDUTTIVA IMMessa ---
                case "Energia Reattiva Induttiva Immessa F1": {
                    double old = nz(b.getF1RIndI());
                    replaceIfDifferent("Reattiva Ind. Imm. F1", old, v.consumo, b::setF1RIndI);
                    break;
                }
                case "Energia Reattiva Induttiva Immessa F2": {
                    double old = nz(b.getF2RIndI());
                    replaceIfDifferent("Reattiva Ind. Imm. F2", old, v.consumo, b::setF2RIndI);
                    break;
                }
                case "Energia Reattiva Induttiva Immessa F3": {
                    double old = nz(b.getF3RIndI());
                    replaceIfDifferent("Reattiva Ind. Imm. F3", old, v.consumo, b::setF3RIndI);
                    break;
                }

                // --- Potenza ---
                case "Potenza F1": {
                    double old = nz(b.getF1Pot());
                    replaceIfDifferent("Potenza F1", old, v.consumo, b::setF1Pot);
                    break;
                }
                case "Potenza F2": {
                    double old = nz(b.getF2Pot());
                    replaceIfDifferent("Potenza F2", old, v.consumo, b::setF2Pot);
                    break;
                }
                case "Potenza F3": {
                    double old = nz(b.getF3Pot());
                    replaceIfDifferent("Potenza F3", old, v.consumo, b::setF3Pot);
                    break;
                }
            }
        }

        // totali (sempre ricalcolati dai valori attualmente in entity)
        double totAtt = nz(b.getF1Att()) + nz(b.getF2Att()) + nz(b.getF3Att());
        double totR   = nz(b.getF1R())   + nz(b.getF2R())   + nz(b.getF3R());
        double totRCI = nz(b.getF1RCapI()) + nz(b.getF2RCapI()) + nz(b.getF3RCapI());
        double totRII = nz(b.getF1RIndI()) + nz(b.getF2RIndI()) + nz(b.getF3RIndI());

        b.setTotAtt(totAtt);
        b.setTotR(totR);
        b.setTotRCapI(totRCI);
        b.setTotRIndI(totRII);

        System.out.println("     [applyMisureToEntity] TOTALE Att=" + totAtt
                + " | Reattiva=" + totR + " | ReattivaCapI=" + totRCI + " | ReattivaIndI=" + totRII);
    }

// ======================================================
// ===============   PARSING: RICALCOLI   ===============
// ======================================================

    /**
     * Estrae le voci dei blocchi “RICALCOLO PER RETTIFICA …” (Materia/Trasporti/Oneri/Imposte)
     * come lista (item, dataFine, prezzo) per confronto 1:1 con il DB per ogni mese.
     */
    /*
    private List<VocePrezzo> extractRicalcoliDettaglioAsList(Document document) {
        List<VocePrezzo> out = new ArrayList<>();
        NodeList lines = document.getElementsByTagName("Line");

        String sezione = null; // MATERIA | TRASPORTI | ONERI | IMPOSTE
        Pattern range = Pattern.compile("(\\d{2}[./]\\d{2}[./]\\d{4}).*?(\\d{2}[./]\\d{2}[./]\\d{4})");
        Pattern euro  = Pattern.compile("€\\s*([-]?[0-9.]+,[0-9]{2})");

        for (int i = 0; i < lines.getLength(); i++) {
            String t = lines.item(i).getTextContent().trim();

            if (t.startsWith("RICALCOLO PER RETTIFICA SPESA PER LA MATERIA ENERGIA")) { sezione = "MATERIA";    continue; }
            if (t.startsWith("RICALCOLO PER RETTIFICA SPESA PER IL TRASPORTO E LA"))    { sezione = "TRASPORTI";  continue; }
            if (t.startsWith("RICALCOLO PER RETTIFICA SPESA PER ONERI DI SISTEMA"))     { sezione = "ONERI";      continue; }
            if (t.startsWith("RICALCOLO PER RETTIFICA IMPOSTE"))                         { sezione = "IMPOSTE";    continue; }

            if (sezione == null) continue;
            if (t.toLowerCase(Locale.ITALY).contains("storno per rettifica")) continue; // salta totali di periodo

            Matcher mr = range.matcher(t);
            Matcher me = euro.matcher(t);
            if (mr.find() && me.find()) {
                Date end = parsePuntata(mr.group(2));
                double val = toDouble(me.group(1));

                String low = t.toLowerCase(Locale.ITALY);
                String item;
                if (sezione.equals("MATERIA")) {
                    if (low.contains("materia energia f1")) item = "Materia energia F1";
                    else if (low.contains("materia energia f2")) item = "Materia energia F2";
                    else if (low.contains("materia energia f3")) item = "Materia energia F3";
                    else if (low.contains("perdite di rete f1")) item = "Perdite F1";
                    else if (low.contains("perdite di rete f2")) item = "Perdite F2";
                    else if (low.contains("perdite di rete f3")) item = "Perdite F3";
                    else if (low.contains("corrispettivi di dispacciamento")) item = "Dispacciamento";
                    else if (low.contains("mercato capacità ore fuori")) item = "Fuori Picco";
                    else if (low.contains("mercato capacità ore picco")) item = "Picco";
                    else if (low.contains("corrispettivo variabile di vendita energia")) item = "Corrispettivo variabile";
                    else if (low.contains("materia energia")) item = "Materia energia F0"; // righe senza fascia
                    else item = "Materia energia F0";
                } else if (sezione.equals("TRASPORTI")) {
                    if (low.contains("quota fissa")) item = "TRASPORTI_FISSA";
                    else if (low.contains("quota potenza")) item = "TRASPORTI_POTENZA";
                    else if (low.contains("quota variabile")) item = "TRASPORTI_VARIABILE";
                    else if (low.contains("penalità energia reattiva") || low.contains("capacitiva immessa")) item = "PENALITA_REATTIVA";
                    else item = "TRASPORTI_TOTALE?"; // non dovrebbe servire qui
                } else if (sezione.equals("ONERI")) {
                    if (low.contains("componente asos") && low.contains("quota fissa")) item = "ASOS_FISSA";
                    else if (low.contains("componente asos") && low.contains("quota potenza")) item = "ASOS_POTENZA";
                    else if (low.contains("componente asos") && low.contains("quota variabile")) item = "ASOS_VARIABILE";
                    else if (low.contains("componente arim") && low.contains("quota fissa")) item = "ARIM_FISSA";
                    else if (low.contains("componente arim") && low.contains("quota potenza")) item = "ARIM_POTENZA";
                    else if (low.contains("componente arim") && low.contains("quota variabile")) item = "ARIM_VARIABILE";
                    else item = "ONERI_ALTRO";
                } else { // IMPOSTE
                    if (low.contains("fino a 200.000")) item = "IMPOSTE_FINO_200K";
                    else if (low.contains("oltre 200.000") && low.contains("1.200.000")) item = "IMPOSTE_200K_1M2";
                    else if (low.contains("oltre 1.200.000")) item = "IMPOSTE_OLTRE_1M2";
                    else if (low.contains("imposta erariale di consumo")) item = "IMPOSTE_CONSUMO_FISSA";
                    else item = "IMPOSTE_ALTRO";
                }

                VocePrezzo vp = new VocePrezzo();
                vp.item = item;
                vp.dataFine = end;
                vp.prezzo = val;
                out.add(vp);
            }
        }
        return out;
    }

     */

    private List<VocePrezzo> extractRicalcoliDettaglioAsList(Document document) {
        List<VocePrezzo> out = new ArrayList<>();
        NodeList lines = document.getElementsByTagName("Line");

        String sezione = null;       // MATERIA | TRASPORTI | ONERI | IMPOSTE
        String voceCorrente = null;  // per MATERIA/TRASPORTI/IMPOSTE (rimane)
        String sottoOneri = null;    // "ASOS" | "ARIM" (nuovo: contesto ONERI)
        String tipoQuota = null;     // "FISSA" | "POTENZA" | "VARIABILE" (nuovo: contesto ONERI)
        String imposteRange = null;   // "FINO_200K" | "200K_1M2" | "OLTRE_1M2" | "CONSUMO"

        Pattern range = Pattern.compile("(\\d{2}[./]\\d{2}[./]\\d{4}).*?(\\d{2}[./]\\d{2}[./]\\d{4})");
        Pattern euro  = Pattern.compile("€\\s*([-]?[0-9.]+,[0-9]{2})");

        for (int i = 0; i < lines.getLength(); i++) {
            String t = lines.item(i).getTextContent().trim();
            if (t.isEmpty()) continue;

            String low = java.text.Normalizer.normalize(t.toLowerCase(Locale.ITALY), java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");

            // ---- switch sezione ----
            if (t.startsWith("RICALCOLO PER RETTIFICA SPESA PER LA MATERIA ENERGIA")) {
                sezione = "MATERIA";   voceCorrente = null; sottoOneri = null; tipoQuota = null;
                System.out.println("[SEZIONE] MATERIA");
                continue;
            }
            if (t.startsWith("RICALCOLO PER RETTIFICA SPESA PER IL TRASPORTO E LA")) {
                sezione = "TRASPORTI"; voceCorrente = null; sottoOneri = null; tipoQuota = null;
                System.out.println("[SEZIONE] TRASPORTI");
                continue;
            }
            if (t.startsWith("RICALCOLO PER RETTIFICA SPESA PER ONERI DI SISTEMA")) {
                sezione = "ONERI";     voceCorrente = null; sottoOneri = null; tipoQuota = null;
                System.out.println("[SEZIONE] ONERI");
                continue;
            }
            if (t.startsWith("RICALCOLO PER RETTIFICA IMPOSTE")) {
                sezione = "IMPOSTE";
                voceCorrente = null; sottoOneri = null; tipoQuota = null;
                imposteRange = null;                    // <— aggiungi
                System.out.println("[SEZIONE] IMPOSTE");
                continue;
            }

            if (low.contains("storno per rettifica")) continue;
            if (sezione == null) continue;

            // ---- intestazioni di sotto-voce / contesti ----
            if (sezione.equals("MATERIA")) {
                if (low.contains("corrispettivo variabile di vendita energia")) { voceCorrente = "MATERIA_CORR_VAR"; continue; }
                if (low.contains("materia energia f1")) { voceCorrente = "MATERIA_F1"; continue; }
                if (low.contains("materia energia f2")) { voceCorrente = "MATERIA_F2"; continue; }
                if (low.contains("materia energia f3")) { voceCorrente = "MATERIA_F3"; continue; }
                if (low.contains("perdite di rete f1")) { voceCorrente = "PERDITE_F1"; continue; }
                if (low.contains("perdite di rete f2")) { voceCorrente = "PERDITE_F2"; continue; }
                if (low.contains("perdite di rete f3")) { voceCorrente = "PERDITE_F3"; continue; }
                if (low.contains("corrispettivi di dispacciamento")) { voceCorrente = "DISPACCIAMENTO"; continue; }
                if (low.contains("mercato capacita ore fuori")) { voceCorrente = "CAPACITA_FUORI"; continue; }
                if (low.contains("mercato capacita ore picco")) { voceCorrente = "CAPACITA_PICCO"; continue; }
                if (low.matches(".*\\bmateria energia\\b.*")) { voceCorrente = "MATERIA_F0"; continue; }
            }
            else if (sezione.equals("TRASPORTI")) {
                if (low.contains("quota fissa")) { voceCorrente = "TRASP_QUOTA_FISSA"; continue; }
                if (low.contains("quota potenza")) { voceCorrente = "TRASP_QUOTA_POTENZA"; continue; }
                if (low.contains("quota variabile")) { voceCorrente = "TRASP_QUOTA_VARIABILE"; continue; }
                if (low.contains("penalita energia reattiva") || low.contains("capacitiva immessa")) { voceCorrente = "TRASP_PEN_REATTIVA"; continue; }
            }
            else if (sezione.equals("ONERI")) {
                // 1) rileva contesto QUOTA (titolo grande)
                if (low.contains("quota fissa")) {
                    tipoQuota = "FISSA";
                    System.out.println("   [ONERI] tipoQuota=FISSA");
                    continue;
                }
                if (low.contains("quota potenza")) {
                    tipoQuota = "POTENZA";
                    System.out.println("   [ONERI] tipoQuota=POTENZA");
                    continue;
                }
                if (low.contains("quota variabile")) {
                    tipoQuota = "VARIABILE";
                    System.out.println("   [ONERI] tipoQuota=VARIABILE");
                    continue;
                }

                // 2) rileva contesto SOTTO-COMPONENTE (riga "Componente ASOS/ARIM ...")
                if (low.contains("componente asos")) {
                    sottoOneri = "ASOS";
                    System.out.println("   [ONERI] sottoOneri=ASOS");
                    continue;
                }
                if (low.contains("componente arim")) {
                    sottoOneri = "ARIM";
                    System.out.println("   [ONERI] sottoOneri=ARIM");
                    continue;
                }

                // NOTA: non settiamo più voceCorrente qui: i due contesti bastano
            } else { // IMPOSTE
                // Normalizza numeri: rimuovi NBSP e punti. Esempi:
                // "200.000" -> "200000", "1.200.000" -> "1200000"
                String lowNum = low.replace('\u00A0',' ').replaceAll("\\.", "");

                // Titoli di blocco che impostano il RANGE per le righe-dato successive
                if (lowNum.contains("imposta erariale di consumo")) {
                    imposteRange = "CONSUMO";
                    System.out.println("   [IMPOSTE] range=CONSUMO");
                    continue;
                }
                if (lowNum.matches(".*imposta\\s+erariale.*fino\\s*a\\s*200000.*")) {
                    imposteRange = "FINO_200K";
                    System.out.println("   [IMPOSTE] range=FINO_200K");
                    continue;
                }
                if (lowNum.matches(".*imposta\\s+erariale.*oltre\\s*200000.*fino\\s*a\\s*1200000.*")) {
                    imposteRange = "200K_1M2";
                    System.out.println("   [IMPOSTE] range=200K_1M2");
                    continue;
                }
                if (lowNum.matches(".*imposta\\s+erariale.*oltre\\s*1200000.*")) {
                    imposteRange = "OLTRE_1M2";
                    System.out.println("   [IMPOSTE] range=OLTRE_1M2");
                    continue;
                }
            }

            // ---- righe-dato (date + €) ----
            Matcher mr = range.matcher(t);
            Matcher me = euro.matcher(t);
            if (mr.find() && me.find()) {
                Date end = parsePuntata(mr.group(2));
                double val = toDouble(me.group(1));
                String item;

                if ("ONERI".equals(sezione)) {
                    // Costruisci l’item da contesti persistenti
                    if (sottoOneri != null && tipoQuota != null) {
                        item = sottoOneri + "_" + tipoQuota; // es. ASOS_FISSA, ARIM_POTENZA, ASOS_VARIABILE...
                    } else if (tipoQuota != null) {
                        item = "ONERI_" + tipoQuota;         // fallback: ONERI_FISSA/POTENZA/VARIABILE
                    } else {
                        item = "ONERI_ALTRO";                // fallback estremo
                    }
                    System.out.println("   [ONERI] DATA row → item=" + item + " | contesti: sottoOneri=" + sottoOneri + ", tipoQuota=" + tipoQuota + ", val=" + val);
                } else if ("IMPOSTE".equals(sezione)) {
                    if (imposteRange == null) {
                        System.out.println("   [IMPOSTE] ⚠️ riga-dato senza range attivo. Skipping. t=" + t);
                        continue;
                    }
                    switch (imposteRange) {
                        case "FINO_200K": item = "IMPOSTE_FINO_200K"; break;
                        case "200K_1M2":  item = "IMPOSTE_200K_1M2";  break;
                        case "OLTRE_1M2": item = "IMPOSTE_OLTRE_1M2"; break;
                        case "CONSUMO":   item = "IMPOSTE_CONSUMO_FISSA"; break;
                        default:          item = "IMPOSTE_ALTRO";
                    }
                    System.out.println("   [IMPOSTE] DATA row → item=" + item + " | range=" + imposteRange + " | val=" + val);
                } else {
                    // mapping come prima per le altre sezioni, usando voceCorrente
                    if (voceCorrente == null) continue;
                    switch (voceCorrente) {
                        case "MATERIA_CORR_VAR": item = "Corrispettivo variabile"; break;
                        case "MATERIA_F1":       item = "Materia energia F1";      break;
                        case "MATERIA_F2":       item = "Materia energia F2";      break;
                        case "MATERIA_F3":       item = "Materia energia F3";      break;
                        case "PERDITE_F1":       item = "Perdite F1";              break;
                        case "PERDITE_F2":       item = "Perdite F2";              break;
                        case "PERDITE_F3":       item = "Perdite F3";              break;
                        case "DISPACCIAMENTO":   item = "Dispacciamento";          break;
                        case "CAPACITA_FUORI":   item = "Fuori Picco";             break;
                        case "CAPACITA_PICCO":   item = "Picco";                   break;
                        case "MATERIA_F0":       item = "Materia energia F0";      break;

                        case "TRASP_QUOTA_FISSA":     item = "TRASPORTI_FISSA";     break;
                        case "TRASP_QUOTA_POTENZA":   item = "TRASPORTI_POTENZA";   break;
                        case "TRASP_QUOTA_VARIABILE": item = "TRASPORTI_VARIABILE"; break;
                        case "TRASP_PEN_REATTIVA":    item = "PENALITA_REATTIVA";   break;

                        case "IMPOSTE_FINO_200K":     item = "IMPOSTE_FINO_200K";     break;
                        case "IMPOSTE_200K_1M2":      item = "IMPOSTE_200K_1M2";      break;
                        case "IMPOSTE_OLTRE_1M2":     item = "IMPOSTE_OLTRE_1M2";     break;
                        case "IMPOSTE_CONSUMO_FISSA": item = "IMPOSTE_CONSUMO_FISSA"; break;
                        default:
                            item = "ALTRO";
                    }
                }

                VocePrezzo vp = new VocePrezzo();
                vp.item = item;
                vp.dataFine = end;
                vp.prezzo = val;
                out.add(vp);
            }
        }
        return out;
    }

    /**
     * Applica SOLO LE DIFFERENZE dal blocco ricalcoli alla entity DB per quel mese.
     * Ritorna true se ha modificato almeno un campo.
     */
    private boolean applyPrezziRicalcoloDiff(BollettaPod b, List<VocePrezzo> vociMese) {
        boolean changed = false;

        System.out.println("🔎 [applyPrezziRicalcoloDiff] Avvio confronto ricalcoli → DB");
        System.out.println("   Numero voci ricevute: " + (vociMese != null ? vociMese.size() : 0));

        // Accumuli temporanei per imposte
        double impFino200 = 0.0, imp200_1200 = 0.0, impOltre1200 = 0.0, impConsumoFissa = 0.0;
        boolean hasImposte = false;

        for (VocePrezzo v : vociMese) {
            System.out.println("➡️  Elaboro voce: " + v.item + " | Prezzo estratto=" + v.prezzo);

            switch (v.item) {
                // ===== Materia energia + perdite + dispacciamento + capacità =====
                case "Materia energia F0":
                    changed |= logDiff("F0Euro", b.getF0Euro(), v.prezzo, () -> b.setF0Euro(v.prezzo));
                    break;
                case "Materia energia F1":
                    changed |= logDiff("F1Euro", b.getF1Euro(), v.prezzo, () -> b.setF1Euro(v.prezzo));
                    break;
                case "Materia energia F2":
                    changed |= logDiff("F2Euro", b.getF2Euro(), v.prezzo, () -> b.setF2Euro(v.prezzo));
                    break;
                case "Materia energia F3":
                    changed |= logDiff("F3Euro", b.getF3Euro(), v.prezzo, () -> b.setF3Euro(v.prezzo));
                    break;
                case "Perdite F1":
                    changed |= logDiff("F1PerdEuro", b.getF1PerdEuro(), v.prezzo, () -> b.setF1PerdEuro(v.prezzo));
                    break;
                case "Perdite F2":
                    changed |= logDiff("F2PerdEuro", b.getF2PerdEuro(), v.prezzo, () -> b.setF2PerdEuro(v.prezzo));
                    break;
                case "Perdite F3":
                    changed |= logDiff("F3PerdEuro", b.getF3PerdEuro(), v.prezzo, () -> b.setF3PerdEuro(v.prezzo));
                    break;
                case "Dispacciamento":
                    changed |= logDiff("Dispacciamento", b.getDispacciamento(), v.prezzo, () -> b.setDispacciamento(v.prezzo));
                    break;
                case "Fuori Picco":
                    changed |= logDiff("EuroFuoriPicco", b.getEuroFuoriPicco(), v.prezzo, () -> b.setEuroFuoriPicco(v.prezzo));
                    break;
                case "Picco":
                    changed |= logDiff("EuroPicco", b.getEuroPicco(), v.prezzo, () -> b.setEuroPicco(v.prezzo));
                    break;
                case "Corrispettivo variabile":
                    changed |= logDiff("EnVeEuro", b.getEnVeEuro(), v.prezzo, () -> b.setEnVeEuro(v.prezzo));
                    break;

                // ===== Trasporti =====
                case "TRASPORTI_FISSA":
                    changed |= logDiff("QFixTrasp", b.getQFixTrasp(), v.prezzo, () -> b.setQFixTrasp(v.prezzo));
                    break;
                case "TRASPORTI_POTENZA":
                    changed |= logDiff("QPotTrasp", b.getQPotTrasp(), v.prezzo, () -> b.setQPotTrasp(v.prezzo));
                    break;
                case "TRASPORTI_VARIABILE":
                    changed |= logDiff("QVarTrasp", b.getQVarTrasp(), v.prezzo, () -> b.setQVarTrasp(v.prezzo));
                    break;
                case "PENALITA_REATTIVA":
                    changed |= logDiff("PenRCapI", Double.valueOf(b.getPenRCapI()), v.prezzo, () -> b.setPenRCapI(v.prezzo));
                    break;

                // ===== Oneri =====
                case "ASOS_FISSA":
                    changed |= logDiff("QFixOnASOS", b.getQFixOnASOS(), v.prezzo, () -> b.setQFixOnASOS(v.prezzo));
                    break;
                case "ASOS_POTENZA":
                    changed |= logDiff("QPotOnASOS", b.getQPotOnASOS(), v.prezzo, () -> b.setQPotOnASOS(v.prezzo));
                    break;
                case "ASOS_VARIABILE":
                    changed |= logDiff("QEnOnASOS", b.getQEnOnASOS(), v.prezzo, () -> b.setQEnOnASOS(v.prezzo));
                    break;
                case "ARIM_FISSA":
                    changed |= logDiff("QFixOnARIM", b.getQFixOnARIM(), v.prezzo, () -> b.setQFixOnARIM(v.prezzo));
                    break;
                case "ARIM_POTENZA":
                    changed |= logDiff("QPotOnARIM", b.getQPotOnARIM(), v.prezzo, () -> b.setQPotOnARIM(v.prezzo));
                    break;
                case "ARIM_VARIABILE":
                    changed |= logDiff("QEnOnARIM", b.getQEnOnARIM(), v.prezzo, () -> b.setQEnOnARIM(v.prezzo));
                    break;

                // ===== Imposte (somma a fine ciclo) =====
                case "IMPOSTE_FINO_200K":
                    impFino200 += v.prezzo; hasImposte = true;
                    System.out.println("   ➕ Accumulo IMPOSTE_FINO_200K: +" + v.prezzo + " ⇒ parziale=" + impFino200);
                    break;

                case "IMPOSTE_200K_1M2":
                    imp200_1200 += v.prezzo; hasImposte = true;
                    System.out.println("   ➕ Accumulo IMPOSTE_200K_1M2: +" + v.prezzo + " ⇒ parziale=" + imp200_1200);
                    break;

                case "IMPOSTE_OLTRE_1M2":
                    impOltre1200 += v.prezzo; hasImposte = true;
                    System.out.println("   ➕ Accumulo IMPOSTE_OLTRE_1M2: +" + v.prezzo + " ⇒ parziale=" + impOltre1200);
                    break;
                case "IMPOSTE_CONSUMO_FISSA":
                    impConsumoFissa += v.prezzo; hasImposte = true;
                    System.out.println("   ➕ Accumulo IMPOSTE_CONSUMO_FISSA: +" + v.prezzo + " ⇒ parziale=" + impConsumoFissa);
                    break;
                case "IMPOSTE_ALTRO":         break;
            }
        }

        // Imposte totali
        if (impFino200 != 0 || imp200_1200 != 0 || impOltre1200 != 0 || impConsumoFissa != 0) {
            double totImposte = impFino200 + imp200_1200 + impOltre1200 + impConsumoFissa;
            System.out.println("📊 Somma imposte = " + totImposte + " (da ricalcoli)");
            changed |= logDiff("Imposte", b.getImposte(), round2(totImposte), () -> b.setImposte(round2(totImposte)));
        }

        // Ricalcolo totali
        double speseEne = nz(b.getF0Euro()) + nz(b.getF1Euro()) + nz(b.getF2Euro()) + nz(b.getF3Euro())
                + nz(b.getF1PerdEuro()) + nz(b.getF2PerdEuro()) + nz(b.getF3PerdEuro())
                + nz(b.getEuroPicco()) + nz(b.getEuroFuoriPicco()) + nz(b.getDispacciamento());
        changed |= logDiff("SpeseEne", b.getSpeseEne(), round2(speseEne), () -> b.setSpeseEne(round2(speseEne)));

        double speseTrasp = nz(b.getQFixTrasp()) + nz(b.getQPotTrasp()) + nz(b.getQVarTrasp());
        changed |= logDiff("SpeseTrasp", b.getSpeseTrasp(), round2(speseTrasp), () -> b.setSpeseTrasp(round2(speseTrasp)));

        double oneri = nz(b.getQFixOnASOS()) + nz(b.getQPotOnASOS()) + nz(b.getQEnOnASOS())
                + nz(b.getQFixOnARIM()) + nz(b.getQPotOnARIM()) + nz(b.getQEnOnARIM());
        changed |= logDiff("Oneri", b.getOneri(), round2(oneri), () -> b.setOneri(round2(oneri)));

        System.out.println("✅ [applyPrezziRicalcoloDiff] Fine. Changed=" + changed);
        return changed;
    }

    // Helper per loggare le differenze
    private boolean logDiff(String campo, Double oldVal, double newVal, Runnable setter) {
        double oldNz = nz(oldVal);
        if (Math.abs(oldNz - newVal) > 0.01) {
            System.out.println("   ⚠️ Cambio " + campo + ": OLD=" + oldNz + " → NEW=" + newVal);
            setter.run();
            return true;
        } else {
            System.out.println("   ✅ Nessuna variazione per " + campo + " (rimane " + oldNz + ")");
            return false;
        }
    }


// ======================================================
// ===============   UTILS   ============================
// ======================================================

    // === Canonizzazione nomi mesi ===
    private static final Map<String, String> MESE_CANON = new HashMap<>();
    static {
        MESE_CANON.put("gennaio", "Gennaio");   MESE_CANON.put("febbraio", "Febbraio");
        MESE_CANON.put("marzo", "Marzo");       MESE_CANON.put("aprile", "Aprile");
        MESE_CANON.put("maggio", "Maggio");     MESE_CANON.put("giugno", "Giugno");
        MESE_CANON.put("luglio", "Luglio");     MESE_CANON.put("agosto", "Agosto");
        MESE_CANON.put("settembre", "Settembre"); MESE_CANON.put("ottobre", "Ottobre");
        MESE_CANON.put("novembre", "Novembre"); MESE_CANON.put("dicembre", "Dicembre");
    }
    private String canonizzaMese(String raw) {
        if (raw == null) return null;
        String k = raw.trim().toLowerCase(Locale.ITALY);
        return MESE_CANON.getOrDefault(k, raw);
    }

    private boolean setDiff(Supplier<Double> getter, java.util.function.DoubleConsumer setter, double nuovo) {
        double old = nz(getter.get());
        if (Math.abs(old - nuovo) > 0.01) { setter.accept(nuovo); return true; }
        return false;
    }
    private double nz(Double d) { return d != null ? d : 0.0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private String capitalizeFirstThree(String mese) {
        if (mese == null || mese.isEmpty()) return mese;
        return mese.substring(0,1).toUpperCase() + mese.substring(1, Math.min(3, mese.length())).toLowerCase();
    }

    private List<String> monthsBetweenInclusive(Date start, Date end) {
        Calendar c = Calendar.getInstance(); c.setTime(start);
        Calendar e = Calendar.getInstance(); e.setTime(end);
        String[] mesi = {"Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno","Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre"};
        List<String> out = new ArrayList<>();
        while (!(c.get(Calendar.YEAR) == e.get(Calendar.YEAR) && c.get(Calendar.MONTH) == e.get(Calendar.MONTH))) {
            out.add(mesi[c.get(Calendar.MONTH)]);
            c.add(Calendar.MONTH, 1);
        }
        out.add(mesi[e.get(Calendar.MONTH)]);
        return out;
    }

    private String monthNameOf(Date d) {
        String[] mesi = {"Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno","Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre"};
        Calendar c = Calendar.getInstance(); c.setTime(d);
        return mesi[c.get(Calendar.MONTH)];
    }
    private String monthNameOf(YearMonth ym) {
        // ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN) sarebbe minuscolo; canonizzo
        String nome = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN);
        return canonizzaMese(nome);
    }
    private YearMonth ymOf(Date d) {
        Calendar c = Calendar.getInstance(); c.setTime(d);
        return YearMonth.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    private int yearOf(Date d) {
        Calendar c = Calendar.getInstance(); c.setTime(d); return c.get(Calendar.YEAR);
    }

    private Date parsePuntata(String ddmmyyyy) {
        String[] p = ddmmyyyy.replace('/', '.').split("\\.");
        return asDate(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
    }
    private Date asDate(int dd, int mm, int yy) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, yy); c.set(Calendar.MONTH, mm - 1); c.set(Calendar.DAY_OF_MONTH, dd);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
    private double toDouble(String s) { return Double.parseDouble(s.replace(".", "").replace(",", ".")); }
    // Costruisce la mappa letture per saveDataToDatabase a partire dalle misure del "Contatore"
    private Map<String, Map<String, Map<String, Integer>>> buildLettureStubFromMisure(
            String meseCorrente,
            List<VoceMisura> misureMese
    ) {
        Map<String, Map<String, Map<String, Integer>>> out = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> catFasce = new LinkedHashMap<>();

        for (VoceMisura vm : misureMese) {
            // vm.item es.: "Energia Attiva F1" / "Energia Reattiva F2" / "Potenza F3" / "Energia Reattiva Capacitiva Immessa F1"
            int lastSpace = vm.item.lastIndexOf(' ');
            if (lastSpace <= 0) continue;
            String categoria = vm.item.substring(0, lastSpace).trim(); // "Energia Attiva"
            String fascia    = vm.item.substring(lastSpace + 1).trim(); // "F1"
            int valore       = (int)Math.round(vm.consumo);

            catFasce.computeIfAbsent(categoria, k -> new LinkedHashMap<>())
                    .merge(fascia, valore, Integer::sum);
        }
        out.put(meseCorrente, catFasce);
        return out;
    }

    // Deriva i kWh per fascia (F1/F2/F3) dal blocco letture del Contatore (Energia Attiva).
// Popola chiavi attese da saveDataToDatabase: "Materia energia f1/f2/f3" e "Materia energia f0" = somma.
    private Map<String, Map<String, Double>> kwhFromMisureAttiva(
            String meseCorrente,
            List<VoceMisura> misureMese
    ) {
        double f1 = 0, f2 = 0, f3 = 0;
        for (VoceMisura vm : misureMese) {
            if (vm.item.startsWith("Energia Attiva ")) {
                if (vm.item.endsWith("F1")) f1 += vm.consumo;
                else if (vm.item.endsWith("F2")) f2 += vm.consumo;
                else if (vm.item.endsWith("F3")) f3 += vm.consumo;
            }
        }
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        Map<String, Double> cat = new LinkedHashMap<>();
        if (f1 > 0) cat.put("Materia energia f1", f1);
        if (f2 > 0) cat.put("Materia energia f2", f2);
        if (f3 > 0) cat.put("Materia energia f3", f3);
        double f0 = f1 + f2 + f3;
        if (f0 > 0) cat.put("Materia energia f0", f0); // utile a settare f0_kwh

        out.put(meseCorrente, cat);
        return out;
    }

    // Normalizza mesi ("ottobre" -> "Ottobre") e fonde duplicati sommando i valori
    private Map<String, Map<String, Double>> normalizeAndMergeNested(Map<String, Map<String, Double>> in) {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        if (in == null) return out;
        in.forEach((mese, mappa) -> {
            String mk = canonizzaMese(mese);
            Map<String, Double> dest = out.computeIfAbsent(mk, k -> new LinkedHashMap<>());
            if (mappa != null) {
                mappa.forEach((k, v) -> dest.merge(k, v, Double::sum));
            }
        });
        return out;
    }

    // Merge NON distruttivo di due mappe annidate (somma i Double quando le chiavi coincidono)
    private Map<String, Map<String, Double>> mergeNestedDoubleMaps(
            Map<String, Map<String, Double>> a,
            Map<String, Map<String, Double>> b
    ) {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        if (a != null) {
            a.forEach((mese, mappa) -> {
                out.computeIfAbsent(mese, k -> new LinkedHashMap<>());
                if (mappa != null) mappa.forEach((k, v) -> out.get(mese).merge(k, v, Double::sum));
            });
        }
        if (b != null) {
            b.forEach((mese, mappa) -> {
                out.computeIfAbsent(mese, k -> new LinkedHashMap<>());
                if (mappa != null) mappa.forEach((k, v) -> out.get(mese).merge(k, v, Double::sum));
            });
        }
        return out;
    }
}
