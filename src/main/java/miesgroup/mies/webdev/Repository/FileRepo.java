package miesgroup.mies.webdev.Repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.BollettaPod;
import miesgroup.mies.webdev.Model.PDFFile;
import miesgroup.mies.webdev.Model.Periodo;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class FileRepo implements PanacheRepositoryBase<PDFFile, Integer> {

    private final DataSource dataSource;
    private final BollettaRepo bollettaRepo;

    public FileRepo(DataSource dataSources, BollettaRepo bollettaRepo) {
        this.dataSource = dataSources;
        this.bollettaRepo = bollettaRepo;
    }

    public int insert(PDFFile pdfFile) {
        // Check if the file name already exists
        Optional<PDFFile> existingFile = find("fileName", pdfFile.getFileName()).firstResultOptional();
        if (existingFile.isPresent()) {
            throw new RuntimeException("File name already exists in the database");
        }

        // Persist the new file
        pdfFile.persist();

        return pdfFile.getIdFile(); // Returns the auto-generated ID
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

    @Transactional
    public void saveDataToDatabase(
            Map<String, Map<String, Map<String, Integer>>> lettureMese,
            Map<String, Map<String, Double>> spese,
            String idPod,
            String nomeBolletta,
            Periodo periodo,
            Map<String, Map<String, Double>> kWhPerMese,
            int userId // <-- AGGIUNTO!
    ) {
        for (Map.Entry<String, Map<String, Map<String, Integer>>> meseEntry : lettureMese.entrySet()) {
            String mese = meseEntry.getKey();
            Map<String, Map<String, Integer>> categorie = meseEntry.getValue();

            // 1. Consumi elettrici (letture)
            Double f1Attiva = getCategoriaConsumo(categorie, "Energia Attiva", "F1");
            Double f2Attiva = getCategoriaConsumo(categorie, "Energia Attiva", "F2");
            Double f3Attiva = getCategoriaConsumo(categorie, "Energia Attiva", "F3");
            Double f1Reattiva = getCategoriaConsumo(categorie, "Energia Reattiva", "F1");
            Double f2Reattiva = getCategoriaConsumo(categorie, "Energia Reattiva", "F2");
            Double f3Reattiva = getCategoriaConsumo(categorie, "Energia Reattiva", "F3");
            Double f1Potenza = getCategoriaConsumo(categorie, "Potenza", "F1");
            Double f2Potenza = getCategoriaConsumo(categorie, "Potenza", "F2");
            Double f3Potenza = getCategoriaConsumo(categorie, "Potenza", "F3");

            Double totAttiva = (f1Attiva + f2Attiva + f3Attiva);
            Double totReattiva = (f1Reattiva + f2Reattiva + f3Reattiva);

            // 2. Estrazione dei costi (in €)
            Map<String, Double> speseMese = spese.getOrDefault(mese, new HashMap<>());

            Double spesaTrasporto = speseMese.getOrDefault("Trasporto e Gestione Contatore", 0.0);
            Double oneri = speseMese.getOrDefault("Oneri di Sistema", 0.0);
            Double imposte = speseMese.getOrDefault("Totale Imposte", 0.0);
            Double dispacciamento = speseMese.getOrDefault("dispacciamento", 0.0);
            Double altro = speseMese.getOrDefault("Altro", 0.0);

            Double f0Euro = speseMese.getOrDefault("Materia Energia", 0.0);
            Double f1Euro = speseMese.getOrDefault("Materia energia F1", 0.0);
            Double f2Euro = speseMese.getOrDefault("Materia energia F2", 0.0);
            Double f3Euro = speseMese.getOrDefault("Materia energia F3", 0.0);
            Double f1PerditeEuro = speseMese.getOrDefault("Perdite F1", 0.0);
            Double f2PerditeEuro = speseMese.getOrDefault("Perdite F2", 0.0);
            Double f3PerditeEuro = speseMese.getOrDefault("Perdite F3", 0.0);
            Double costoPicco = speseMese.getOrDefault("Picco", 0.0);
            Double costoFuoriPicco = speseMese.getOrDefault("Fuori Picco", 0.0);

            Double spesaEnergia = f0Euro + f1Euro + f2Euro + f3Euro + f1PerditeEuro + f2PerditeEuro + f3PerditeEuro + costoPicco + costoFuoriPicco + dispacciamento;

            // 3. Consumi in kWh
            Map<String, Double> kwhMese = kWhPerMese.getOrDefault(mese, new HashMap<>());
            Double f0Kwh = kwhMese.getOrDefault("Materia energia F0", 0.0);
            Double f1Kwh = kwhMese.getOrDefault("Materia energia F1", 0.0);
            Double f2Kwh = kwhMese.getOrDefault("Materia energia F2", 0.0);
            Double f3Kwh = kwhMese.getOrDefault("Materia energia F3", 0.0);
            Double f1PerditeKwh = kwhMese.getOrDefault("Perdite F1", 0.0);
            Double f2PerditeKwh = kwhMese.getOrDefault("Perdite F2", 0.0);
            Double f3PerditeKwh = kwhMese.getOrDefault("Perdite F3", 0.0);
            Double consumoPicco = kwhMese.getOrDefault("Picco", 0.0);
            Double consumoFuoriPicco = kwhMese.getOrDefault("Fuori Picco", 0.0);
            Double penalitaKvar = kwhMese.getOrDefault("Altro", 0.0);

            // --- PATCH: controllo idPod ---
            if (idPod == null || idPod.trim().isEmpty()) {
                System.err.println("ATTENZIONE: idPod nullo o vuoto, la bolletta NON verrà salvata! "
                        + "nomeBolletta=" + nomeBolletta + ", mese=" + mese + ", anno=" + periodo.getAnno());
                continue; // salta il salvataggio!
            }

            // --- LOG per debug ---
            System.out.println("Sto salvando la bolletta con idPod: " + idPod + ", idUser: " + userId +
                    ", nomeBolletta: " + nomeBolletta + " - mese: " + mese + " - anno: " + periodo.getAnno());

            // 4. Creazione e salvataggio dell'entità BollettaPod
            BollettaPod bolletta = new BollettaPod();

            bolletta.setIdPod(idPod);
            bolletta.setNomeBolletta(nomeBolletta);
            bolletta.setMese(mese);
            bolletta.setAnno(periodo.getAnno());
            bolletta.setPeriodoInizio(new java.sql.Date(periodo.getInizio().getTime()));
            bolletta.setPeriodoFine(new java.sql.Date(periodo.getFine().getTime()));
            bolletta.setMeseAnno(capitalizeFirstThree(mese) + " " + periodo.getAnno());

            // Consumi elettrici (letture)
            bolletta.setF1A(f1Attiva != null ? f1Attiva : 0.0);
            bolletta.setF2A(f2Attiva != null ? f2Attiva : 0.0);
            bolletta.setF3A(f3Attiva != null ? f3Attiva : 0.0);
            bolletta.setF1R(f1Reattiva != null ? f1Reattiva : 0.0);
            bolletta.setF2R(f2Reattiva != null ? f2Reattiva : 0.0);
            bolletta.setF3R(f3Reattiva != null ? f3Reattiva : 0.0);
            bolletta.setF1P(f1Potenza != null ? f1Potenza : 0.0);
            bolletta.setF2P(f2Potenza != null ? f2Potenza : 0.0);
            bolletta.setF3P(f3Potenza != null ? f3Potenza : 0.0);
            bolletta.setTotAttiva(totAttiva);
            bolletta.setTotReattiva(totReattiva);

            // dettaglioCosto (in €)
            bolletta.setSpeseEnergia(spesaEnergia);
            bolletta.setTrasporti(spesaTrasporto);
            bolletta.setOneri(oneri);
            bolletta.setImposte(imposte);
            bolletta.setDispacciamento(dispacciamento);
            bolletta.setAltro(altro);

            // dettaglioCosto specifici per consumo (in €)
            bolletta.setF0Euro(f0Euro);
            bolletta.setF1Euro(f1Euro);
            bolletta.setF2Euro(f2Euro);
            bolletta.setF3Euro(f3Euro);
            bolletta.setF1PerditeEuro(f1PerditeEuro);
            bolletta.setF2PerditeEuro(f2PerditeEuro);
            bolletta.setF3PerditeEuro(f3PerditeEuro);

            // Dati "Picco" e "Fuori Picco"
            bolletta.setPiccoKwh(consumoPicco);
            bolletta.setCostoPicco(costoPicco);
            bolletta.setFuoriPiccoKwh(consumoFuoriPicco);
            bolletta.setCostoFuoriPicco(costoFuoriPicco);

            // Consumi in kWh (valori separati)
            bolletta.setF0Kwh(f0Kwh);
            bolletta.setF1Kwh(f1Kwh);
            bolletta.setF2Kwh(f2Kwh);
            bolletta.setF3Kwh(f3Kwh);
            bolletta.setF1PerditeKwh(f1PerditeKwh);
            bolletta.setF2PerditeKwh(f2PerditeKwh);
            bolletta.setF3PerditeKwh(f3PerditeKwh);

            // PATCH FINALE: set idUser
            bolletta.setIdUser(userId);

            bollettaRepo.persist(bolletta);
            System.out.println("Bolletta salvata con idPod: " + bolletta.getIdPod() + ", idUser: " + bolletta.getIdUser());
        }
    }



    private String capitalizeFirstThree(String mese) {
        if (mese == null || mese.length() < 3) return mese;
        String firstThree = mese.substring(0, 3).toLowerCase();
        return firstThree.substring(0, 1).toUpperCase() + firstThree.substring(1);
    }

    private Double getCategoriaConsumo(Map<String, Map<String, Integer>> categorie, String categoria, String fascia) {
        return categorie.getOrDefault(categoria, Collections.emptyMap()).getOrDefault(fascia, 0).doubleValue();
    }

}
