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
        System.out.println("   nomeBolletta (parametro): " + nomeBolletta);
        System.out.println("   periodo: " + periodo.getInizio() + " -> " + periodo.getFine());
        System.out.println("   mesi (chiavi lettureMese): " + lettureMese.keySet());
        System.out.println("   mesi (chiavi spesePerMese): " + spesePerMese.keySet());

        // Normalizza le chiavi dei mesi in minuscolo (unificate)
        Map<String, Map<String, Map<String, Integer>>> lettureNorm = canonizeMonthKeys2(lettureMese);
        Map<String, Map<String, Double>>              speseNorm   = canonizeMonthKeys(spesePerMese);
        Map<String, Map<String, Double>>              kwhNorm     = canonizeMonthKeys(kWhPerMese);

// Log di controllo (facoltativo)
        System.out.println("   mesi normalizzati (letture): " + lettureNorm.keySet());
        System.out.println("   mesi normalizzati (spese)  : " + speseNorm.keySet());
        System.out.println("   mesi normalizzati (kWh)    : " + kwhNorm.keySet());


        for (Map.Entry<String, Map<String, Map<String, Integer>>> meseEntry : lettureNorm.entrySet()) {
            String mese = meseEntry.getKey();
            Map<String, Map<String, Integer>> categorie = meseEntry.getValue();

            String compositeKey = (nomeBolletta != null ? nomeBolletta : "null") + "-" + mese;
            System.out.println("\n📅 PROCESSANDO MESE: " + mese);
            System.out.println("🔑 CHIAVE UNIV. (NomeBolletta-Mese): " + compositeKey);
            System.out.println("   spesePerMese.containsKey(" + mese + ")? " + speseNorm.containsKey(mese));

            // 👉 DEBUG: verifico se esiste già in DB una riga con stessa (NomeBolletta, Mese)
            try {
                BollettaPod dupByNameMese = bollettaRepo.find("nomeBolletta = ?1 and mese = ?2", nomeBolletta, mese).firstResult();
                if (dupByNameMese != null) {
                    System.out.println("⚠️ TROVATO RECORD ESISTENTE (NomeBolletta, Mese) PRIMA DEL PERSIST:");
                    System.out.println("   -> idPod=" + dupByNameMese.getIdPod()
                            + " | nomeBolletta=" + dupByNameMese.getNomeBolletta()
                            + " | mese=" + dupByNameMese.getMese()
                            + " | anno=" + dupByNameMese.getAnno()
                            + " | periodo=" + dupByNameMese.getPeriodoInizio() + " -> " + dupByNameMese.getPeriodoFine());
                } else {
                    System.out.println("✅ Nessun record con stessa (NomeBolletta, Mese) trovato PRIMA del persist.");
                }
            } catch (Exception e) {
                System.out.println("❗️Errore in verifica (NomeBolletta, Mese): " + e.getMessage());
            }

            // 👉 DEBUG: verifico anche eventuale (idPod, Mese, Anno)
            try {
                BollettaPod dupByPodMeseAnno = bollettaRepo.find(
                        "idPod = ?1 and mese = ?2 and anno = ?3", idPod, mese, periodo.getAnno()
                ).firstResult();
                if (dupByPodMeseAnno != null) {
                    System.out.println("⚠️ TROVATO RECORD ESISTENTE (idPod, Mese, Anno) PRIMA DEL PERSIST:");
                    System.out.println("   -> idPod=" + dupByPodMeseAnno.getIdPod()
                            + " | nomeBolletta=" + dupByPodMeseAnno.getNomeBolletta()
                            + " | mese=" + dupByPodMeseAnno.getMese()
                            + " | anno=" + dupByPodMeseAnno.getAnno());
                } else {
                    System.out.println("✅ Nessun record con stessa (idPod, Mese, Anno) trovato PRIMA del persist.");
                }
            } catch (Exception e) {
                System.out.println("❗️Errore in verifica (idPod, Mese, Anno): " + e.getMessage());
            }

            // --- Letture (kWh/kVARh/kW per fascia) ---
            Double f1Att = getCategoriaConsumo(categorie, "Energia Attiva", "F1");
            Double f2Att = getCategoriaConsumo(categorie, "Energia Attiva", "F2");
            Double f3Att = getCategoriaConsumo(categorie, "Energia Attiva", "F3");

            Double f1R   = getCategoriaConsumo(categorie, "Energia Reattiva", "F1");
            Double f2R   = getCategoriaConsumo(categorie, "Energia Reattiva", "F2");
            Double f3R   = getCategoriaConsumo(categorie, "Energia Reattiva", "F3");

            // Reattiva Capacitiva/Induttiva IMMESSA
            Double f1RCapI = getCategoriaConsumo(categorie, "Energia Reattiva Capacitiva Immessa", "F1");
            Double f2RCapI = getCategoriaConsumo(categorie, "Energia Reattiva Capacitiva Immessa", "F2");
            Double f3RCapI = getCategoriaConsumo(categorie, "Energia Reattiva Capacitiva Immessa", "F3");

            Double f1RIndI = getCategoriaConsumo(categorie, "Energia Reattiva Induttiva Immessa", "F1");
            Double f2RIndI = getCategoriaConsumo(categorie, "Energia Reattiva Induttiva Immessa", "F2");
            Double f3RIndI = getCategoriaConsumo(categorie, "Energia Reattiva Induttiva Immessa", "F3");

            Double f1Pot = getCategoriaConsumo(categorie, "Potenza", "F1");
            Double f2Pot = getCategoriaConsumo(categorie, "Potenza", "F2");
            Double f3Pot = getCategoriaConsumo(categorie, "Potenza", "F3");

            Double totAtt   = safeSum(f1Att, f2Att, f3Att);
            Double totReatt = safeSum(f1R, f2R, f3R);
            Double totRCapI = safeSum(f1RCapI, f2RCapI, f3RCapI);
            Double totRIndI = safeSum(f1RIndI, f2RIndI, f3RIndI);

            // --- Spese in € per mese ---
            Map<String, Double> speseMese = speseNorm.getOrDefault(mese, Collections.emptyMap());

            System.out.println("💰 SPESE DISPONIBILI PER IL MESE:");
            speseMese.forEach((k, v) -> System.out.println("   " + k + ": " + v + " €"));

            Double speseTrasp     = getCI(speseMese, "Spesa per il trasporto e la gestione del contatore", "Trasporto e Gestione Contatore");
            Double oneri          = getCI(speseMese, "Spesa per oneri di sistema", "Oneri di Sistema");
            Double imposte        = getCI(speseMese, "Totale imposte", "TOTALE IMPOSTE");
            Double dispacciamento = getCI(speseMese, "dispacciamento", "Corrispettivi di dispacciamento del", "Corrispettivi di dispacciamento Del.");

            Double f0Euro_ = getCI(speseMese, "Materia energia f0", "Quota vendita", "Materia Energia", "F0_Euro");
            Double f1Euro_ = getCI(speseMese, "Materia energia f1", "Quota vendita f1", "F1_Euro");
            Double f2Euro_ = getCI(speseMese, "Materia energia f2", "Quota vendita f2", "F2_Euro");
            Double f3Euro_ = getCI(speseMese, "Materia energia f3", "Quota vendita f3","F3_Euro");

            Double f1PerdEuro_ = getCI(speseMese, "Perdite f1", "F1_Perd_Euro");
            Double f2PerdEuro_ = getCI(speseMese, "Perdite f2", "F2_Perd_Euro");
            Double f3PerdEuro_ = getCI(speseMese, "Perdite f3", "F3_Perd_Euro");

            Double euroPicco      = getCI(speseMese, "Picco", "corrispettivo mercato capacità ore picco", "Euro_FuoriPicco");
            Double euroFuoriPicco = getCI(speseMese, "Fuori Picco", "corrispettivo mercato capacità ore fuori", "Euro_Picco");

            // --- Energia Verde ---
            Double enVeEuro = getCI(speseMese, "Corrispettivo variabile", "Corrispettivo variabile di vendita energia");

            Double totaleMateria = getCI(
                    speseMese,
                    "MATERIA_TOTALE",
                    "Materia Energia_TOTALE",
                    "TOTALE_MATERIA",
                    "Spesa per la materia energia",
                    "SPESA PER LA MATERIA ENERGIA"
            );

            Double speseEneComponenti = safeSum(
                    f0Euro_, f1Euro_, f2Euro_, f3Euro_,
                    f1PerdEuro_, f2PerdEuro_, f3PerdEuro_,
                    euroPicco, euroFuoriPicco, dispacciamento
            );

            Double penRCapI = getCI(speseMese, "Altro", "Penalità", "Penalita", "PENALITA_REATTIVA", "Pen_RCapI");

            Double f1Pen33 = getCI(speseMese, "F1Penale33");
            Double f1Pen75 = getCI(speseMese, "F1Penale75");
            Double f2Pen33 = getCI(speseMese, "F2Penale33");
            Double f2Pen75 = getCI(speseMese, "F2Penale75");

            Double totaleOneri = getCI(speseMese, "ONERI_TOTALE", "Oneri di Sistema_TOTALE", "TOTALE_ONERI");
            Double totaleTrasporti = getCI(speseMese, "TRASPORTI_TOTALE", "Trasporto e Gestione Contatore_TOTALE", "TOTALE_TRASPORTI");

            System.out.println("🔍 DEBUG TOTALI ESTRATTI:");
            System.out.println("   totaleMateria (header): " + totaleMateria);
            System.out.println("   speseEne (somma componenti): " + speseEneComponenti);
            System.out.println("   totaleOneri: " + totaleOneri);
            System.out.println("   totaleTrasporti: " + totaleTrasporti);
            System.out.println("   oneri (somma categorie): " + oneri);
            System.out.println("   speseTrasp (somma categorie): " + speseTrasp);

            Double oneriFinale = (totaleOneri != null && totaleOneri > 0) ? totaleOneri : oneri;
            Double trasportiFinale = (totaleTrasporti != null && totaleTrasporti > 0) ? totaleTrasporti : speseTrasp;
            Double speseEneFinale = (totaleMateria != null && totaleMateria > 0) ? totaleMateria : speseEneComponenti;

            System.out.println("✅ VALORI FINALI PER IL DATABASE:");
            System.out.println("   speseEneFinale: " + speseEneFinale);
            System.out.println("   oneriFinale: " + oneriFinale);
            System.out.println("   trasportiFinale: " + trasportiFinale);

            Map<String, Double> kwhMese = kwhNorm.getOrDefault(mese, Collections.emptyMap());
            Double f0Kwh   = getCI(kwhMese, "Materia energia f0", "Materia energia");
            Double f1Kwh   = getCI(kwhMese, "Materia energia f1", "f1_kwh");
            Double f2Kwh   = getCI(kwhMese, "Materia energia f2", "f2_kwh");
            Double f3Kwh   = getCI(kwhMese, "Materia energia f3", "f3_kwh");
            Double f1PerdK = getCI(kwhMese, "Perdite f1", "f1_perdite_kwh");
            Double f2PerdK = getCI(kwhMese, "Perdite f2", "f2_perdite_kwh");
            Double f3PerdK = getCI(kwhMese, "Perdite f3", "f3_perdite_kwh");
            Double piccoKwh     = getCI(kwhMese, "Picco", "Picco_kwh");
            Double fuoriPiccoKwh= getCI(kwhMese, "Fuori Picco", "FuoriPicco_kwh");

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

            // Spese macro - USA I VALORI FINALI
            b.setSpeseEne(nz(speseEneFinale));
            b.setSpeseTrasp(nz(trasportiFinale));
            b.setOneri(nz(oneriFinale));
            b.setImposte(nz(imposte));
            b.setDispacciamento(nz(dispacciamento));

            Double verificaDispacciamento = nz(dispacciamento);
            Double generation = nz(speseEneFinale) - verificaDispacciamento;
            b.setGeneration(generation);
            System.out.println("⚙️ GENERATION CALCOLATA: " + generation + " (speseEneFinale: " + nz(speseEneFinale) + " - dispacciamento: " + verificaDispacciamento + ")");

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

            // Energia Verde
            b.setEnVeEuro(nz(enVeEuro));

            // kWh dettaglio
            b.setF0Kwh(nz(f0Kwh));
            b.setF1Kwh(nz(f1Kwh));
            b.setF2Kwh(nz(f2Kwh));
            b.setF3Kwh(nz(f3Kwh));
            b.setF1PerdKwh(nz(f1PerdK));
            b.setF2PerdKwh(nz(f2PerdK));
            b.setF3PerdKwh(nz(f3PerdK));

            b.setTotAttPerd(nz(safeSum(f1PerdK, f2PerdK, f3PerdK)));
            b.setF1Pen33(nz(f1Pen33));
            b.setF1Pen75(nz(f1Pen75));
            b.setF2Pen33(nz(f2Pen33));
            b.setF2Pen75(nz(f2Pen75));

            // Quote trasporti specifiche
            b.setQFixTrasp(nz(getCI(speseMese, "TRASPORTI_FISSA", "quota fissa trasporti", "€/cliente/giorno quota fissa", "quota fissa")));
            b.setQPotTrasp(nz(getCI(speseMese, "TRASPORTI_POTENZA", "€/kW/giorno", "quota potenza trasporti", "quota potenza")));
            b.setQVarTrasp(nz(getCI(speseMese, "TRASPORTI_VARIABILE", "€/kWh", "quota variabile trasporti", "quota variabile del trasporto", "quota variabile")));

            // MAPPATURA ASOS / ARIM
            b.setQEnOnASOS(nz(getCI(speseMese, "ASOS_VARIABILE", "quota energia oneri asos", "q energia oneri asos")));
            b.setQFixOnASOS(nz(getCI(speseMese, "ASOS_FISSA", "quota fissa oneri asos", "q fissa oneri asos")));
            b.setQPotOnASOS(nz(getCI(speseMese, "ASOS_POTENZA", "quota potenza oneri asos", "q potenza oneri asos")));

            b.setQEnOnARIM(nz(getCI(speseMese, "ARIM_VARIABILE", "quota energia oneri arim", "q energia oneri arim")));
            b.setQFixOnARIM(nz(getCI(speseMese, "ARIM_FISSA", "quota fissa oneri arim", "q fissa oneri arim")));
            b.setQPotOnARIM(nz(getCI(speseMese, "ARIM_POTENZA", "quota potenza oneri arim", "q potenza oneri arim")));

            System.out.println("💾 SALVANDO NEL DATABASE (tentativo INSERT):");
            System.out.println("   ID Pod: " + b.getIdPod());
            System.out.println("   NomeBolletta: " + b.getNomeBolletta());
            System.out.println("   Mese: " + b.getMese());
            System.out.println("   Anno: " + b.getAnno());
            System.out.println("   Chiave univoca attesa (NomeBolletta-Mese): " + compositeKey);
            System.out.println("   Spese Energia (finale): " + b.getSpeseEne());
            System.out.println("   Spese Trasporti: " + b.getSpeseTrasp());
            System.out.println("   Oneri: " + b.getOneri());
            System.out.println("   Generation: " + b.getGeneration());

            try {
                bollettaRepo.persist(b);
                System.out.println("✅ RECORD SALVATO CON SUCCESSO");
            } catch (Exception ex) {
                System.out.println("⛔️ ERRORE IN INSERT su chiave " + compositeKey + ": " + ex.getMessage());
                System.out.println("   Suggerimento: esiste già (NomeBolletta, Mese)? Vedi log sopra.");
                throw ex; // rilancio per non alterare il comportamento originale
            }
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

    // --- Helpers mesi (stessa logica ovunque) ---
    private static final Map<String,String> MESE_CANON = new HashMap<>();
    static {
        MESE_CANON.put("gennaio","Gennaio"); MESE_CANON.put("febbraio","Febbraio");
        MESE_CANON.put("marzo","Marzo");     MESE_CANON.put("aprile","Aprile");
        MESE_CANON.put("maggio","Maggio");   MESE_CANON.put("giugno","Giugno");
        MESE_CANON.put("luglio","Luglio");   MESE_CANON.put("agosto","Agosto");
        MESE_CANON.put("settembre","Settembre"); MESE_CANON.put("ottobre","Ottobre");
        MESE_CANON.put("novembre","Novembre");   MESE_CANON.put("dicembre","Dicembre");
    }
    private String canonMese(String raw) {
        if (raw == null) return null;
        String k = raw.trim().toLowerCase(java.util.Locale.ITALY);
        return MESE_CANON.getOrDefault(k, raw);
    }
    private static <V> Map<String,V> canonizeMonthKeys(Map<String,V> in) {
        Map<String,V> out = new LinkedHashMap<>();
        if (in == null) return out;
        in.forEach((k,v) -> out.put(
                k==null ? null : k.trim().toLowerCase(java.util.Locale.ITALY), v));
        return out;
    }
    private static <V> Map<String,Map<String,V>> canonizeMonthKeys2(Map<String,Map<String,V>> in) {
        Map<String,Map<String,V>> out = new LinkedHashMap<>();
        if (in == null) return out;
        in.forEach((k,v) -> out.put(
                k==null ? null : k.trim().toLowerCase(java.util.Locale.ITALY), v));
        return out;
    }
    private static <V> Map<String,V> getMonthMapCI(Map<String,Map<String,V>> big, String mese) {
        if (big == null) return java.util.Collections.emptyMap();
        Map<String,V> val = big.get(mese);
        if (val != null) return val;
        // fallback case-insensitive
        for (Map.Entry<String,Map<String,V>> e : big.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(mese)) return e.getValue();
        }
        return java.util.Collections.emptyMap();
    }

}
