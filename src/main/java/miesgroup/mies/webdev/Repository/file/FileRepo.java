package miesgroup.mies.webdev.Repository.file;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.Periodo;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.file.PDFFile;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
public class FileRepo implements PanacheRepositoryBase<PDFFile, Integer> {

    private final BollettaPodRepo bollettaRepo;

    public FileRepo(BollettaPodRepo bollettaRepo) {
        this.bollettaRepo = bollettaRepo;
    }

    public int insert(PDFFile pdfFile) {
        // Evita duplicati per fileName
        Optional<PDFFile> existingFile = find("fileName", pdfFile.getFileName()).firstResultOptional();
        if (existingFile.isPresent()) {
            throw new RuntimeException("File name already exists in the database");
        }
        pdfFile.setUploadDate(LocalDateTime.now());
        pdfFile.persist();
        return pdfFile.getIdFile();
    }

    public void abbinaPod(int idFile, String idPod) {
        update("idPod = ?1 WHERE idFile = ?2", idPod, idFile);
    }

    public PDFFile findById(int id) {
        return PDFFile.findById(id);
    }

    public byte[] getFile(int id) {
        PDFFile f = findById(id);
        return f.getFileData();
    }

    // ------------------------------------------------------------
    // Salvataggio dei dati estratti (letture, spese, kWh)
    // Allineato alla nuova entity e nuovo schema con acronimi.
    // ------------------------------------------------------------
    @Transactional
    public void saveDataToDatabase(
            Map<String, Map<String, Map<String, Integer>>> lettureMese,
            Map<String, Map<String, Double>> spesePerMese,
            String idPod,
            String nomeBolletta,
            Periodo periodo,
            Map<String, Map<String, Double>> kWhPerMese
    ) {
        System.out.println("🔍 INIZIO SALVATAGGIO DATABASE");
        System.out.println("   idPod: " + idPod);
        System.out.println("   nomeBolletta: " + nomeBolletta);
        System.out.println("   periodo: " + periodo.getInizio() + " -> " + periodo.getFine());

        for (Map.Entry<String, Map<String, Map<String, Integer>>> meseEntry : lettureMese.entrySet()) {
            String mese = meseEntry.getKey();
            Map<String, Map<String, Integer>> categorie = meseEntry.getValue();

            System.out.println("\n📅 PROCESSANDO MESE: " + mese);

            // --- Letture (kWh/kVARh/kW per fascia) ---
            Double f1Att = getCategoriaConsumo(categorie, "Energia Attiva", "F1");
            Double f2Att = getCategoriaConsumo(categorie, "Energia Attiva", "F2");
            Double f3Att = getCategoriaConsumo(categorie, "Energia Attiva", "F3");

            Double f1R   = getCategoriaConsumo(categorie, "Energia Reattiva", "F1");
            Double f2R   = getCategoriaConsumo(categorie, "Energia Reattiva", "F2");
            Double f3R   = getCategoriaConsumo(categorie, "Energia Reattiva", "F3");

            // Nuove categorie: Reattiva Capacitiva/Induttiva IMMESSA
            Double f1RCapI = getCategoriaConsumo(categorie, "Energia Reattiva Capacitiva Immessa", "F1");
            Double f2RCapI = getCategoriaConsumo(categorie, "Energia Reattiva Capacitiva Immessa", "F2");
            Double f3RCapI = getCategoriaConsumo(categorie, "Energia Reattiva Capacitiva Immessa", "F3");

            Double f1RIndI = getCategoriaConsumo(categorie, "Energia Reattiva Induttiva Immessa", "F1");
            Double f2RIndI = getCategoriaConsumo(categorie, "Energia Reattiva Induttiva Immessa", "F2");
            Double f3RIndI = getCategoriaConsumo(categorie, "Energia Reattiva Induttiva Immessa", "F3");

            Double f1Pot = getCategoriaConsumo(categorie, "Potenza", "F1");
            Double f2Pot = getCategoriaConsumo(categorie, "Potenza", "F2");
            Double f3Pot = getCategoriaConsumo(categorie, "Potenza", "F3");

            Double totAtt  = safeSum(f1Att, f2Att, f3Att);
            Double totReatt = safeSum(f1R, f2R, f3R);
            Double totRCapI = safeSum(f1RCapI, f2RCapI, f3RCapI);
            Double totRIndI = safeSum(f1RIndI, f2RIndI, f3RIndI);

            // --- Spese in € per mese ---
            Map<String, Double> speseMese = spesePerMese.getOrDefault(mese, Collections.emptyMap());

            System.out.println("💰 SPESE DISPONIBILI PER IL MESE:");
            speseMese.forEach((k, v) -> System.out.println("   " + k + ": " + v + " €"));

            // Macro-voci (valori delle singole categorie - NON i totali)
            Double speseTrasp   = getCI(speseMese, "Spesa per il trasporto e la gestione del contatore", "Trasporto e Gestione Contatore");
            Double oneri        = getCI(speseMese, "Spesa per oneri di sistema", "Oneri di Sistema");
            Double imposte      = getCI(speseMese, "Totale imposte", "TOTALE IMPOSTE");
            Double dispacciamento = getCI(speseMese, "dispacciamento", "Corrispettivi di dispacciamento del");

            // Materia energia per fasce + perdite + (eventuali) picco/ fuori picco
            Double f0Euro_   = getCI(speseMese, "Materia energia", "Quota vendita", "Materia Energia");
            Double f1Euro_   = getCI(speseMese, "Materia energia f1", "Quota vendita f1");
            Double f2Euro_   = getCI(speseMese, "Materia energia f2", "Quota vendita f2");
            Double f3Euro_   = getCI(speseMese, "Materia energia f3", "Quota vendita f3");

            Double f1PerdEuro_ = getCI(speseMese, "Perdite f1");
            Double f2PerdEuro_ = getCI(speseMese, "Perdite f2");
            Double f3PerdEuro_ = getCI(speseMese, "Perdite f3");

            Double euroPicco     = getCI(speseMese, "Picco", "corrispettivo mercato capacità ore picco");
            Double euroFuoriPicco= getCI(speseMese, "Fuori Picco", "corrispettivo mercato capacità ore fuori");

            // Penalità reattiva capacitiva immessa (ex Altro)
            Double penRCapI = getCI(speseMese, "Altro", "Penalità", "Penalita", "PENALITA_REATTIVA");

            // Spesa energia totale (materia + perdite + picco + dispacciamento)
            Double speseEne = safeSum(f0Euro_, f1Euro_, f2Euro_, f3Euro_, f1PerdEuro_, f2PerdEuro_, f3PerdEuro_, euroPicco, euroFuoriPicco, dispacciamento);

            // --- ESTRAZIONE TOTALI ONERI E TRASPORTI (CORRETTA) ---
            Double totaleOneri = getCI(speseMese, "ONERI_TOTALE", "Oneri di Sistema_TOTALE", "TOTALE_ONERI");
            Double totaleTrasporti = getCI(speseMese, "TRASPORTI_TOTALE", "Trasporto e Gestione Contatore_TOTALE", "TOTALE_TRASPORTI");

            System.out.println("🔍 DEBUG TOTALI ESTRATTI:");
            System.out.println("   totaleOneri: " + totaleOneri);
            System.out.println("   totaleTrasporti: " + totaleTrasporti);
            System.out.println("   oneri (somma categorie): " + oneri);
            System.out.println("   speseTrasp (somma categorie): " + speseTrasp);

            // Usa i totali specifici se disponibili (> 0), altrimenti la somma delle voci
            Double oneriFinale = (totaleOneri != null && totaleOneri > 0) ? totaleOneri : oneri;
            Double trasportiFinale = (totaleTrasporti != null && totaleTrasporti > 0) ? totaleTrasporti : speseTrasp;

            System.out.println("✅ VALORI FINALI PER IL DATABASE:");
            System.out.println("   oneriFinale: " + oneriFinale);
            System.out.println("   trasportiFinale: " + trasportiFinale);

            // --- kWh per mese (se presenti in mappa kWhPerMese) ---
            Map<String, Double> kwhMese = kWhPerMese.getOrDefault(mese, Collections.emptyMap());
            Double f0Kwh   = getCI(kwhMese, "Materia energia f0", "Materia energia");
            Double f1Kwh   = getCI(kwhMese, "Materia energia f1");
            Double f2Kwh   = getCI(kwhMese, "Materia energia f2");
            Double f3Kwh   = getCI(kwhMese, "Materia energia f3");
            Double f1PerdK = getCI(kwhMese, "Perdite f1");
            Double f2PerdK = getCI(kwhMese, "Perdite f2");
            Double f3PerdK = getCI(kwhMese, "Perdite f3");
            Double piccoKwh    = getCI(kwhMese, "Picco");
            Double fuoriPiccoKwh = getCI(kwhMese, "Fuori Picco");

            // --- Controllo idPod ---
            if (idPod == null || idPod.trim().isEmpty()) {
                System.err.printf("ATTENZIONE: idPod nullo o vuoto, non salvo. nomeBolletta=%s, mese=%s, anno=%s%n",
                        nomeBolletta, mese, periodo.getAnno());
                continue;
            }

            // --- Entità e salvataggio ---
            BollettaPod b = new BollettaPod();
            b.setIdPod(idPod);
            b.setNomeBolletta(nomeBolletta);
            b.setMese(mese);
            b.setAnno(periodo.getAnno());
            b.setPeriodoInizio(new java.sql.Date(periodo.getInizio().getTime()));
            b.setPeriodoFine(new java.sql.Date(periodo.getFine().getTime()));
            b.setMeseAnno(capitalizeFirstThree(mese) + " " + periodo.getAnno());

            // Letture (tutti impostati a 0 se null)
            b.setF1Att(nz(f1Att)); b.setF2Att(nz(f2Att)); b.setF3Att(nz(f3Att));
            b.setF1R(nz(f1R));     b.setF2R(nz(f2R));     b.setF3R(nz(f3R));
            b.setF1RCapI(nz(f1RCapI)); b.setF2RCapI(nz(f2RCapI)); b.setF3RCapI(nz(f3RCapI));
            b.setF1RIndI(nz(f1RIndI)); b.setF2RIndI(nz(f2RIndI)); b.setF3RIndI(nz(f3RIndI));
            b.setF1Pot(nz(f1Pot)); b.setF2Pot(nz(f2Pot)); b.setF3Pot(nz(f3Pot));

            b.setTotAtt(nz(totAtt));
            b.setTotR(nz(totReatt));
            b.setTotRCapI(nz(totRCapI));
            b.setTotRIndI(nz(totRIndI));

            // Spese macro - USA I VALORI FINALI (UNA SOLA VOLTA!)
            b.setSpeseEne(nz(speseEne));
            b.setSpeseTrasp(nz(trasportiFinale));  // ✅ Usa il totale specifico se disponibile
            b.setOneri(nz(oneriFinale));           // ✅ Usa il totale specifico se disponibile
            b.setImposte(nz(imposte));
            b.setDispacciamento(nz(dispacciamento));

            // CALCOLO GENERATION (COME RICHIESTO)
            Double verificaDispacciamento = nz(dispacciamento);
            Double generation = (double) Math.round(nz(speseEne) - verificaDispacciamento);
            b.setGeneration(generation);

            System.out.println("⚙️ GENERATION CALCOLATA: " + generation + " (speseEne: " + nz(speseEne) + " - dispacciamento: " + verificaDispacciamento + ")");

            // Penalità capacitiva immessa
            b.setPenRCapI(nz(penRCapI));

            // Dettaglio € per fasce/perdite
            b.setF0Euro(nz(f0Euro_));
            b.setF1Euro(nz(f1Euro_));
            b.setF2Euro(nz(f2Euro_));
            b.setF3Euro(nz(f3Euro_));
            b.setF1PerdEuro(nz(f1PerdEuro_));
            b.setF2PerdEuro(nz(f2PerdEuro_));
            b.setF3PerdEuro(nz(f3PerdEuro_));

            // Picco/ Fuori Picco
            b.setEuroPicco(nz(euroPicco));
            b.setEuroFuoriPicco(nz(euroFuoriPicco));
            b.setPiccoKwh(nz(piccoKwh));
            b.setFuoriPiccoKwh(nz(fuoriPiccoKwh));

            // kWh dettaglio
            b.setF0Kwh(nz(f0Kwh));
            b.setF1Kwh(nz(f1Kwh));
            b.setF2Kwh(nz(f2Kwh));
            b.setF3Kwh(nz(f3Kwh));
            b.setF1PerdKwh(nz(f1PerdK));
            b.setF2PerdKwh(nz(f2PerdK));
            b.setF3PerdKwh(nz(f3PerdK));

            // Totale attiva perdite (se vuoi salvarlo separatamente)
            b.setTotAttPerd(nz(safeSum(f1PerdK, f2PerdK, f3PerdK)));

            // Quote trasporti SPECIFICHE (MODIFICATA)
            b.setQFixTrasp(nz(getCI(speseMese, "TRASPORTI_FISSA", "quota fissa trasporti", "€/cliente/giorno quota fissa", "quota fissa")));
            b.setQPotTrasp(nz(getCI(speseMese, "TRASPORTI_POTENZA", "€/kW/giorno", "quota potenza trasporti", "quota potenza")));
            b.setQVarTrasp(nz(getCI(speseMese, "TRASPORTI_VARIABILE", "€/kWh", "quota variabile trasporti", "quota variabile del trasporto", "quota variabile")));

            // MAPPATURA ASOS E ARIM (MODIFICATA)
            // Mappatura ASOS
            b.setQEnOnASOS(nz(getCI(speseMese, "ASOS_VARIABILE", "quota energia oneri asos", "q energia oneri asos")));
            b.setQFixOnASOS(nz(getCI(speseMese, "ASOS_FISSA", "quota fissa oneri asos", "q fissa oneri asos")));
            b.setQPotOnASOS(nz(getCI(speseMese, "ASOS_POTENZA", "quota potenza oneri asos", "q potenza oneri asos")));

            // Mappatura ARIM
            b.setQEnOnARIM(nz(getCI(speseMese, "ARIM_VARIABILE", "quota energia oneri arim", "q energia oneri arim")));
            b.setQFixOnARIM(nz(getCI(speseMese, "ARIM_FISSA", "quota fissa oneri arim", "q fissa oneri arim")));
            b.setQPotOnARIM(nz(getCI(speseMese, "ARIM_POTENZA", "quota potenza oneri arim", "q potenza oneri arim")));

            System.out.println("💾 SALVANDO NEL DATABASE:");
            System.out.println("   ID Pod: " + b.getIdPod());
            System.out.println("   Mese: " + b.getMese());
            System.out.println("   Anno: " + b.getAnno());
            System.out.println("   Spese Energia: " + b.getSpeseEne());
            System.out.println("   Spese Trasporti: " + b.getSpeseTrasp());
            System.out.println("   Oneri: " + b.getOneri());
            System.out.println("   Generation: " + b.getGeneration());

            // Persist
            bollettaRepo.persist(b);
            System.out.println("✅ RECORD SALVATO CON SUCCESSO");
        }

        System.out.println("🏁 SALVATAGGIO DATABASE COMPLETATO");
    }


    // ------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------
    private static Double nz(Double d) { return d == null ? 0.0 : d; }

    private static Double safeSum(Double... vals) {
        double s = 0.0;
        if (vals != null) {
            for (Double v : vals) s += (v == null ? 0.0 : v);
        }
        return s;
    }

    private Double getCategoriaConsumo(Map<String, Map<String, Integer>> categorie, String categoria, String fascia) {
        Map<String, Integer> map = categorie.getOrDefault(categoria, Collections.emptyMap());
        return map.containsKey(fascia) ? map.get(fascia).doubleValue() : 0.0;
    }

    /** Recupera Double da mappa in modo case-insensitive e senza accenti */
    private static Double getCI(Map<String, Double> map, String... keys) {
        if (map == null || map.isEmpty()) return 0.0;
        // Pre-costruisci mappa normalizzata
        Map<String, Double> norm = new HashMap<>();
        for (Map.Entry<String, Double> e : map.entrySet()) {
            norm.put(normKey(e.getKey()), e.getValue());
        }
        for (String k : keys) {
            Double v = norm.get(normKey(k));
            if (v != null) return v;
        }
        return 0.0;
    }

    private static String normKey(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.toLowerCase(Locale.ITALIAN).replaceAll("\\s+", " ").trim();
    }

    private String capitalizeFirstThree(String mese) {
        if (mese == null || mese.isEmpty()) return mese;
        String firstThree = mese.length() > 3 ? mese.substring(0, 3) : mese;
        return firstThree.substring(0, 1).toUpperCase() + firstThree.substring(1).toLowerCase();
    }

    // ------------------------------------------------------------
    // Ricerca file per utente (lasciata com'era, ma vedi nota sotto)
    // ------------------------------------------------------------
    public List<PDFFile> findByUserId(int userId) {
        List<BollettaPod> bolletteUtente = bollettaRepo.find("idUtente", userId).list();
        if (bolletteUtente == null || bolletteUtente.isEmpty()) return new ArrayList<>();

        List<Integer> idFiles = new ArrayList<>();
        for (BollettaPod b : bolletteUtente) {
            if (b.getIdFile() != null) idFiles.add(b.getIdFile());
        }
        if (idFiles.isEmpty()) return new ArrayList<>();
        return find("idFile IN ?1", idFiles).list();
    }
}
