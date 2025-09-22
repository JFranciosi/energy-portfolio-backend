package miesgroup.mies.webdev.Repository.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import miesgroup.mies.webdev.Model.Periodo;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.Pod;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class BollettaPodRepo implements PanacheRepositoryBase<BollettaPod, Integer> {

    @PersistenceContext
    EntityManager em;

    private final PodRepo podRepo;

    public BollettaPodRepo(PodRepo podRepo) {
        this.podRepo = podRepo;
    }

    /* ===== Base finders ===== */

    public Optional<BollettaPod> findOne(String nomeBolletta, String mese, String anno) {
        return find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResultOptional();
    }

    public List<BollettaPod> findBollettaPodByPods(List<Pod> pods) {
        List<String> podIds = pods.stream().map(Pod::getId).toList();
        return list("idPod IN ?1", podIds);
    }

    public boolean existsByNomeBollettaAndPod(String nomeBolletta, String idPod) {
        return count("nomeBolletta = ?1 AND idPod = ?2", nomeBolletta, idPod) > 0;
    }

    /* ===== Letture utili ===== */

    public double getSommaAttiva(String nomeBolletta) {
        Double res = find(
                "SELECT COALESCE(b.f1Att,0) + COALESCE(b.f2Att,0) + COALESCE(b.f3Att,0) " +
                        "FROM BollettaPod b WHERE b.nomeBolletta = ?1", nomeBolletta
        ).project(Double.class).firstResult();
        return res != null ? res : 0.0;
    }

    public Double getF1Att(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        return b != null ? Optional.ofNullable(b.getF1Att()).orElse(0.0) : 0.0;
    }

    public Double getF2Att(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        return b != null ? Optional.ofNullable(b.getF2Att()).orElse(0.0) : 0.0;
    }

    public Double getF3Att(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        return b != null ? Optional.ofNullable(b.getF3Att()).orElse(0.0) : 0.0;
    }

    public Double getF1R(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        return b != null ? Optional.ofNullable(b.getF1R()).orElse(0.0) : 0.0;
    }

    public Double getF2R(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        return b != null ? Optional.ofNullable(b.getF2R()).orElse(0.0) : 0.0;
    }

    public Double getF3R(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        return b != null ? Optional.ofNullable(b.getF3R()).orElse(0.0) : 0.0;
    }

    public Double getF3RCapI(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        return b != null ? Optional.ofNullable(b.getF3RCapI()).orElse(0.0) : 0.0;
    }

    public Double getPiccoKwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getPiccoKwh()).orElse(0.0) : 0.0;
    }

    public Double getFuoriPiccoKwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getFuoriPiccoKwh()).orElse(0.0) : 0.0;
    }

    public Double getMaggiorePotenza(String nomeBolletta) {
        BollettaPod b = find("nomeBolletta", nomeBolletta).firstResult();
        if (b == null) return 0.0;
        double f1 = Optional.ofNullable(b.getF1Pot()).orElse(0.0);
        double f2 = Optional.ofNullable(b.getF2Pot()).orElse(0.0);
        double f3 = Optional.ofNullable(b.getF3Pot()).orElse(0.0);
        return Math.max(f1, Math.max(f2, f3));
    }

    public List<BollettaPod> findByIdPodAndMeseAndAnno(String idPod, String mese, String anno) {
        return list("idPod = ?1 AND mese = ?2 AND anno = ?3", idPod, mese, anno);
    }

    public void updateBolletta(BollettaPod bollettaEsistente) {
        update("speseEnergia = ?1, trasporti = ?2, oneri = ?3, imposte = ?4 " +
                        "WHERE nomeBolletta = ?5 AND mese = ?6 AND anno = ?7",
                bollettaEsistente.getSpeseEne(),
                bollettaEsistente.getSpeseTrasp(),
                bollettaEsistente.getOneri(),
                bollettaEsistente.getImposte(),
                bollettaEsistente.getNomeBolletta(),
                bollettaEsistente.getMese(),
                bollettaEsistente.getAnno());
    }

    public void saveRicalcoliToDatabase(Map<String, Map<String, Double>> ricalcoliPerMese,
                                        String idPod, String nomeB, Periodo periodo) {

        for (Map.Entry<String, Map<String, Double>> entry : ricalcoliPerMese.entrySet()) {
            String mese = entry.getKey();
            Map<String, Double> ricalcoli = entry.getValue();

            BollettaPod bolletta = new BollettaPod();
            bolletta.setNomeBolletta(nomeB);
            bolletta.setMese(mese);
            bolletta.setAnno(periodo.getAnno());
            bolletta.setIdPod(idPod);
            bolletta.setMeseAnno(capitalizeFirstThree(mese) + " " + periodo.getAnno());
            System.out.println("meseAnno: " + bolletta.getMeseAnno());

            // --- Helper lettura con default 0.0
            java.util.function.Function<String, Double> val = k -> {
                Double v = ricalcoli.get(k);
                return v != null ? v : 0.0;
            };

            // Se usi nomi diversi/alias nelle mappe in ingresso, aggiungi altri val.apply(...) qui

            // ======================
            // Fasce (qui nel tuo codice salvavi f1/f2/f3 in Potenza)
            // ======================
            bolletta.setF1Pot(val.apply("f1"));
            bolletta.setF2Pot(val.apply("f2"));
            bolletta.setF3Pot(val.apply("f3"));

            // ======================
            // Totali
            // ======================
            bolletta.setTotAtt(val.apply("totAttiva"));
            bolletta.setTotR(val.apply("totReattiva"));

            // Se nei ricalcoli separi anche le reattive “immessa”, popola questi totali:
            bolletta.setTotRCapI(val.apply("totReattivaCapacitivaImmessa"));   // opzionale
            bolletta.setTotRIndI(val.apply("totReattivaInduttivaImmessa"));    // opzionale

            // ======================
            // Picco / Fuori picco
            // ======================
            bolletta.setPiccoKwh(val.apply("piccoKwh"));
            bolletta.setFuoriPiccoKwh(val.apply("fuoriPiccoKwh"));
            // Se hai anche gli € del picco:
            // bolletta.setEuroPicco(val.apply("euroPicco"));
            // bolletta.setEuroFuoriPicco(val.apply("euroFuoriPicco"));

            // ======================
            // Spese macro
            // ======================
            bolletta.setSpeseEne(val.apply("speseEnergia"));
            bolletta.setSpeseTrasp(val.apply("trasporti"));
            bolletta.setOneri(val.apply("oneri"));
            bolletta.setImposte(val.apply("imposte"));

            // ======================
            // Altri importi
            // ======================
            bolletta.setGeneration(val.apply("generation"));
            bolletta.setDispacciamento(val.apply("dispacciamento")); // prima scrivevi su verificaDispacciamento

            // ======================
            // Penali
            // ======================
            // Prova a leggere per fascia, se presenti:
            Double f1Pen33 = ricalcoli.get("penali33_F1");
            Double f2Pen33 = ricalcoli.get("penali33_F2");
            Double f1Pen75 = ricalcoli.get("penali75_F1");
            Double f2Pen75 = ricalcoli.get("penali75_F2");

            // fallback sul tuo naming precedente (tutto in una chiave)
            if (f1Pen33 == null && ricalcoli.get("penali33") != null) f1Pen33 = ricalcoli.get("penali33");
            if (f2Pen33 == null && ricalcoli.get("penali33") != null) f2Pen33 = 0.0; // nessun dato → 0
            if (f1Pen75 == null && ricalcoli.get("penali75") != null) f1Pen75 = ricalcoli.get("penali75");
            if (f2Pen75 == null && ricalcoli.get("penali75") != null) f2Pen75 = 0.0;

            bolletta.setF1Pen33(f1Pen33 != null ? f1Pen33 : 0.0);
            bolletta.setF2Pen33(f2Pen33 != null ? f2Pen33 : 0.0);
            bolletta.setF1Pen75(f1Pen75 != null ? f1Pen75 : 0.0);
            bolletta.setF2Pen75(f2Pen75 != null ? f2Pen75 : 0.0);

            // Penalità reattiva capacitiva immessa (ex Altro) se presente
            Double penRCapI = ricalcoli.get("penalitaReattivaCapacitivaImmessa");
            if (penRCapI != null) bolletta.setPenRCapI(penRCapI);

            // ======================
            // Trasporti: quote
            // ======================
            bolletta.setQVarTrasp(val.apply("quotaVariabileTrasporti"));
            bolletta.setQFixTrasp(val.apply("quotaFissaTrasporti"));
            bolletta.setQPotTrasp(val.apply("quotaPotenzaTrasporti"));

            // ======================
            // Oneri di sistema: ARIM / ASOS
            // ======================
            // Se hai già i valori separati, usali:
            Double qEnOnASOS = ricalcoli.get("quotaEnergiaOneriASOS");
            Double qEnOnARIM = ricalcoli.get("quotaEnergiaOneriARIM");
            Double qFixOnASOS = ricalcoli.get("quotaFissaOneriASOS");
            Double qFixOnARIM = ricalcoli.get("quotaFissaOneriARIM");
            Double qPotOnASOS = ricalcoli.get("quotaPotenzaOneriASOS");
            Double qPotOnARIM = ricalcoli.get("quotaPotenzaOneriARIM");

            // Fallback: se arrivano ancora gli aggregati, mappali su ASOS (ARIM = 0)
            if (qEnOnASOS == null && qEnOnARIM == null) {
                qEnOnASOS = ricalcoli.get("quotaEnergiaOneri");
                qEnOnARIM = 0.0;
            }
            if (qFixOnASOS == null && qFixOnARIM == null) {
                qFixOnASOS = ricalcoli.get("quotaFissaOneri");
                qFixOnARIM = 0.0;
            }
            if (qPotOnASOS == null && qPotOnARIM == null) {
                qPotOnASOS = ricalcoli.get("quotaPotenzaOneri");
                qPotOnARIM = 0.0;
            }

            bolletta.setQEnOnASOS(qEnOnASOS != null ? qEnOnASOS : 0.0);
            bolletta.setQEnOnARIM(qEnOnARIM != null ? qEnOnARIM : 0.0);
            bolletta.setQFixOnASOS(qFixOnASOS != null ? qFixOnASOS : 0.0);
            bolletta.setQFixOnARIM(qFixOnARIM != null ? qFixOnARIM : 0.0);
            bolletta.setQPotOnASOS(qPotOnASOS != null ? qPotOnASOS : 0.0);
            bolletta.setQPotOnARIM(qPotOnARIM != null ? qPotOnARIM : 0.0);

            // ======================
            // Perdite attiva totale
            // ======================
            bolletta.setTotAttPerd(val.apply("totAttivaPerdite"));

            // ======================
            // Persist
            // ======================
            persist(bolletta);
        }
    }

    private String capitalizeFirstThree(String mese) {
        if (mese == null || mese.length() < 3) return mese;
        String firstThree = mese.substring(0, 3).toLowerCase();
        return firstThree.substring(0, 1).toUpperCase() + firstThree.substring(1);
    }
    /* ===== Dati dal POD ===== */

    public Double getPotenzaImpegnata(String idPod) {
        Pod pod = podRepo.find("id", idPod).firstResult();
        return pod != null ? pod.getPotenzaImpegnata() : 0.0;
    }

    public String getTipoTensione(String idPod) {
        Pod pod = podRepo.find("id", idPod).firstResult();
        return pod != null ? pod.getTipoTensione() : null;
    }

    public boolean A2AisPresent(String nomeBolletta, String idPod) {
        return count("nomeBolletta = ?1 AND idPod = ?2", nomeBolletta, idPod) > 0;
    }
    /* ===== Update mirati (nomi = campi entity) ===== */

    public void updateTotAttPerd(Double totAttPerd, String nomeBolletta, String mese) {
        update("totAttPerd = ?1 WHERE nomeBolletta = ?2 AND mese = ?3",
                totAttPerd, nomeBolletta, mese);
    }

    public void updateDispacciamento(Double dispacciamento, String nomeBolletta, String mese) {
        update("dispacciamento = ?1 WHERE nomeBolletta = ?2 AND mese = ?3",
                dispacciamento, nomeBolletta, mese);
    }

    public void updateGeneration(Double generation, String nomeBolletta, String mese) {
        update("generation = ?1 WHERE nomeBolletta = ?2 AND mese = ?3",
                generation, nomeBolletta, mese);
    }

    public void updateQuoteTrasporti(Double qVar, Double qFix, Double qPot,
                                     String nomeBolletta, String mese) {
        update("qVarTrasp = ?1, qFixTrasp = ?2, qPotTrasp = ?3 WHERE nomeBolletta = ?4 AND mese = ?5",
                qVar, qFix, qPot, nomeBolletta, mese);
    }

    public void updateSpeseTrasp(Double speseTrasp, String nomeBolletta, String mese) {
        update("speseTrasp = ?1 WHERE nomeBolletta = ?2 AND mese = ?3",
                speseTrasp, nomeBolletta, mese);
    }

    public void updateImposte(Double imposte, String nomeBolletta, String mese) {
        update("imposte = ?1 WHERE nomeBolletta = ?2 AND mese = ?3",
                imposte, nomeBolletta, mese);
    }

    public void updateOneriASOS(Double qEn, Double qFix, Double qPot,
                                String nomeBolletta, String mese) {
        update("qEnOnASOS = ?1, qFixOnASOS = ?2, qPotOnASOS = ?3 WHERE nomeBolletta = ?4 AND mese = ?5",
                qEn, qFix, qPot, nomeBolletta, mese);
    }

    public void updateOneri(Double oneri, String nomeBolletta, String mese) {
        update("oneri = ?1 WHERE nomeBolletta = ?2 AND mese = ?3",
                oneri, nomeBolletta, mese);
    }

    public void updatePenali(Double f1Pen33, Double f1Pen75, Double f2Pen33, Double f2Pen75,
                             String nomeBolletta, String mese) {
        update("f1Pen33 = ?1, f1Pen75 = ?2, f2Pen33 = ?3, f2Pen75 = ?4 " +
                        "WHERE nomeBolletta = ?5 AND mese = ?6",
                f1Pen33, f1Pen75, f2Pen33, f2Pen75, nomeBolletta, mese);
    }

    public void updatePicchi(Double euroPicco, Double euroFuoriPicco,
                             String nomeBolletta, String mese) {
        update("euroPicco = ?1, euroFuoriPicco = ?2 WHERE nomeBolletta = ?3 AND mese = ?4",
                euroPicco, euroFuoriPicco, nomeBolletta, mese);
    }

    public AggregatoBollette getAggregatiPerPodAnnoMese(String podId, Integer anno, Integer mese) {
        String jpql = "SELECT SUM(b.totAtt), SUM(b.speseEne), SUM(b.oneri) " +
                "FROM BollettaPod b WHERE b.idPod = :podId AND b.anno = :anno AND b.mese = :mese";

        Object[] result = (Object[]) em.createQuery(jpql)
                .setParameter("podId", podId)
                .setParameter("anno", anno)
                .setParameter("mese", mese)
                .getSingleResult();

        return new AggregatoBollette(
                (Double) result[0],  // somma totAttiva
                (Double) result[1],  // somma speseEnergia
                (Double) result[2]   // somma oneri (se usati)
        );
    }

    // Metodi per recuperare kWh
    public Double getF0Kwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getF0Kwh()).orElse(0.0) : 0.0;
    }

    public Double getF1Kwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getF1Kwh()).orElse(0.0) : 0.0;
    }

    public Double getF2Kwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getF2Kwh()).orElse(0.0) : 0.0;
    }

    public Double getF3Kwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getF3Kwh()).orElse(0.0) : 0.0;
    }

    // Metodi per recuperare perdite kWh (nota: i getter sono getF1PerdKwh non getF1PerditeKwh)
    public Double getF1PerditeKwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getF1PerdKwh()).orElse(0.0) : 0.0;
    }

    public Double getF2PerditeKwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getF2PerdKwh()).orElse(0.0) : 0.0;
    }

    public Double getF3PerditeKwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3",
                nomeBolletta, mese, anno).firstResult();
        return b != null ? Optional.ofNullable(b.getF3PerdKwh()).orElse(0.0) : 0.0;
    }
}
