package miesgroup.mies.webdev.Repository.bolletta;


import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.dettaglioCosto;
import miesgroup.mies.webdev.Model.Periodo;
import miesgroup.mies.webdev.Model.bolletta.Pod;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BollettaRepo implements PanacheRepositoryBase<BollettaPod, Integer> {

    @PersistenceContext
    EntityManager em;

    private final dettaglioCostoRepo costiRepo;
    private final PodRepo podRepo;

    public BollettaRepo(dettaglioCostoRepo costiRepo, PodRepo podRepo) {
        this.costiRepo = costiRepo;
        this.podRepo = podRepo;
    }

    public Double getCorrispettiviDispacciamentoA2A(int trimestre, String annoRiferimento) {
        List<dettaglioCosto> lista = costiRepo.find("unitaMisura = ?1 AND categoria = 'dispacciamento' AND (trimestre = ?2 OR anno IS NOT NULL) AND annoRiferimento = ?3", "€/KWh", trimestre, annoRiferimento).list();
        double somma = 0;
        for (dettaglioCosto c : lista) {
            somma += c.getCosto();
        }
        return somma;
    }

    public Double getConsumoA2A(String nome, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nome, mese).firstResult();
        return b.getTotAttiva();
    }

    public String getTipoTensione(String idPod) {
        Pod pod = podRepo.find("id", idPod).firstResult();
        return pod.getTipoTensione();
    }

    public void updateVerificaDispacciamentoA2A(double verificaDispacciamento, String nome, String mese) {
        update("SET verificaDispacciamento = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", verificaDispacciamento, nome, mese);
    }


    public void updateGenerationA2A(Double generation, String nome, String mese) {
        update("SET generation = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", generation, nome, mese);
    }


    public Double getPotenzaImpegnata(String idPod) {
        Pod pod = podRepo.find("id", idPod).firstResult();
        return pod.getPotenzaImpegnata();
    }

    public Double getCostiTrasporto(int trimestre, String intervalloPotenza, String unitaMisura, String annoBolletta) {
        return costiRepo.findByCategoriaUnitaTrimestre("trasporti", unitaMisura, intervalloPotenza, trimestre, annoBolletta)
                .orElse(0.0);
    }

    public Double getCostiOneri(int trimestre, String intervalloPotenza, String unitaMisura, String classeAgevolazione, String annoRiferimento) {
        List<dettaglioCosto> lista = costiRepo.find("categoria = 'oneri' AND unitaMisura = ?1 AND intervalloPotenza = ?2 AND (trimestre = ?3 OR anno IS NOT NULL) AND classeAgevolazione = ?4 AND annoRiferimento = ?5",
                unitaMisura, intervalloPotenza, trimestre, classeAgevolazione, annoRiferimento).list();
        if (lista.isEmpty()) {
            return 0.0;
        }

        Double somma = 0.0;
        for (dettaglioCosto c : lista) {
            somma += c.getCosto();
        }
        return somma;
    }

    public void updateVerificaTrasportiA2A(double trasporti, String nomeBolletta, String mese) {
        update("SET verificaTrasporti = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", trasporti, nomeBolletta, mese);
    }


    public void updateF1Penale33(double F1Penale33, String nomeBolletta, String mese) {
        update("SET F1Penale33 = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", F1Penale33, nomeBolletta, mese);
    }


    public void updateF1Penale75(double F1Penale75, String nomeBolletta, String mese) {
        update("SET F1Penale75 = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", F1Penale75, nomeBolletta, mese);
    }


    public void updateF2Penale33(double F2Penale33, String nomeBolletta, String mese) {
        update("SET F2Penale33 = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", F2Penale33, nomeBolletta, mese);
    }


    public void updateF2Penale75(double F2Penale75, String nomeBolletta, String mese) {
        update("SET F2Penale75 = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", F2Penale75, nomeBolletta, mese);
    }


    public void updateVerificaOneri(Double costiOneri, String nomeBolletta, String mese) {
        update("SET verificaOneri = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", costiOneri, nomeBolletta, mese);
    }


    public Double getMaggiorePotenza(String nomeBolletta) {
        BollettaPod bolletta = find("nomeBolletta", nomeBolletta).firstResult();
        Double maggiore = Math.max(bolletta.getF1P(), Math.max(bolletta.getF2P(), bolletta.getF3P()));
        return maggiore;
    }

    public boolean A2AisPresent(String nomeBolletta, String idPod) {
        return count("nomeBolletta = ?1 AND idPod = ?2", nomeBolletta, idPod) > 0;
    }

    public Double getF1A(String nomeBolletta, String mese) {

        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();

        Double f1 = b.getF1A();
        return f1;
    }

    public Double getF2A(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        Double f2 = b.getF2A();
        return f2;

    }

    public Double getF1P(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        Double f1 = b.getF1P();
        return f1;
    }

    public Double getF2P(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        Double f2 = b.getF2P();
        return f2;

    }

    public Double getPenaliSotto75(String annoRiferimento) {
        List<dettaglioCosto> costi = costiRepo.find("categoria = 'penali' AND descrizione = '>33%&75%<' AND annoRiferimento = ?1", annoRiferimento).list();
        Double somma = 0.0;
        for (dettaglioCosto c : costi) {
            somma += c.getCosto();
        }
        return somma;
    }

    public Double getPenaliSopra75(String annoRiferiemnto) {
        List<dettaglioCosto> c = costiRepo.find("categoria = 'penali' AND descrizione = '>75%' AND annoRiferimento = ?1", annoRiferiemnto).list();
        Double somma = 0.0;
        for (dettaglioCosto costi : c) {
            somma += costi.getCosto();
        }
        return somma;
    }

    public void updateVerificaImposte(double costiImposte, String nomeBolletta, String mese) {
        update("SET verificaImposte = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", costiImposte, nomeBolletta, mese);
    }


    public Double getF1R(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        Double f1 = b.getF1R();
        return f1;
    }

    public Double getF2R(String nomeBolletta, String mese) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2", nomeBolletta, mese).firstResult();
        Double f2 = b.getF2R();
        return f2;
    }

    public Double getPiccoKwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3", nomeBolletta, mese, anno).firstResult();
        return b.getPiccoKwh();
    }

    public Double getFuoriPiccoKwh(String nomeBolletta, String mese, String anno) {
        BollettaPod b = find("nomeBolletta = ?1 AND mese = ?2 AND anno = ?3", nomeBolletta, mese, anno).firstResult();
        return b.getFuoriPiccoKwh();
    }

    public Double getCostoFuoriPicco(int trimestre, String annoBolletta, String intervalloPotenza) {
        List<dettaglioCosto> furoiPicco = costiRepo.find("categoria = 'fuori picco' AND ( trimestre = ?1 OR anno IS NOT NULL ) AND annoRiferimento = ?2 AND intervalloPotenza = ?3",
                trimestre, annoBolletta, intervalloPotenza).list();

        if (furoiPicco.isEmpty()) {
            return 0.0;
        }

        double somma = 0;
        for (dettaglioCosto c : furoiPicco) {
            somma += c.getCosto();
        }
        return somma;
    }

    public Double getCostoPicco(int trimestre, String anno, String rangePotenza) {
        List<dettaglioCosto> picco = costiRepo.find("categoria = 'picco' AND ( trimestre = ?1 OR anno IS NOT NULL ) AND annoRiferimento = ?2 AND intervalloPotenza = ?3",
                trimestre, anno, rangePotenza).list();

        if (picco.isEmpty()) {
            return 0.0;
        }

        double somma = 0;
        for (dettaglioCosto c : picco) {
            somma += c.getCosto();
        }
        return somma;
    }

    public void updateVerificaPicco(Double verificaPicco, String nomeBolletta, String mese) {
        update("SET verificaPicco = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", verificaPicco, nomeBolletta, mese);
    }


    public void updateVerificaFuoriPicco(Double verificaFuoriPicco, String nomeBolletta, String mese) {
        update("SET verificaFuoriPicco = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", verificaFuoriPicco, nomeBolletta, mese);
    }


    public void updateTOTAttivaPerdite(Double totAttivaPerdite, String nomeBolletta, String mese) {
        update("SET totAttivaPerdite = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", totAttivaPerdite, nomeBolletta, mese);
    }

    public void updateQuoteTrasporto(Double quotaVariabileT, Double quotaFissaT, Double quotaPotenzaT, String nomeBolletta, String mese) {
        update("SET quotaVariabileTrasporti = ?1, quotaFissaTrasporti = ?2, quotaPotenzaTrasporti = ?3 WHERE nomeBolletta = ?4 AND mese = ?5",
                quotaVariabileT, quotaFissaT, quotaPotenzaT, nomeBolletta, mese);
    }

    public void updateQuoteOneri(Double quotaEnergiaOneri, Double quotaFissaOneri, Double quotaPotenzaOneri, String nomeBolletta, String mese) {
        update("SET quotaEnergiaOneri = ?1, quotaFissaOneri = ?2, quotaPotenzaOneri = ?3 WHERE nomeBolletta = ?4 AND mese = ?5",
                quotaEnergiaOneri, quotaFissaOneri, quotaPotenzaOneri, nomeBolletta, mese);
    }

    public void updateBolletta(BollettaPod bollettaEsistente) {
        update("speseEnergia = ?1, trasporti = ?2, oneri = ?3, imposte = ?4 " +
                        "WHERE nomeBolletta = ?5 AND mese = ?6 AND anno = ?7",
                bollettaEsistente.getSpeseEnergia(),
                bollettaEsistente.getTrasporti(),
                bollettaEsistente.getOneri(),
                bollettaEsistente.getImposte(),
                bollettaEsistente.getNomeBolletta(),
                bollettaEsistente.getMese(),
                bollettaEsistente.getAnno());
    }

    public void saveRicalcoliToDatabase(Map<String, Map<String, Double>> ricalcoliPerMese, String idPod, String nomeB, Periodo periodo) {
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
            // Impostazione diretta con controllo null
            bolletta.setF1P(ricalcoli.get("f1") != null ? ricalcoli.get("f1") : 0.0);
            bolletta.setF2P(ricalcoli.get("f2") != null ? ricalcoli.get("f2") : 0.0);
            bolletta.setF3P(ricalcoli.get("f3") != null ? ricalcoli.get("f3") : 0.0);
            bolletta.setTotAttiva(ricalcoli.get("totAttiva") != null ? ricalcoli.get("totAttiva") : 0.0);
            bolletta.setTotReattiva(ricalcoli.get("totReattiva") != null ? ricalcoli.get("totReattiva") : 0.0);
            bolletta.setPiccoKwh(ricalcoli.get("piccoKwh") != null ? ricalcoli.get("piccoKwh") : 0.0);
            bolletta.setFuoriPiccoKwh(ricalcoli.get("fuoriPiccoKwh") != null ? ricalcoli.get("fuoriPiccoKwh") : 0.0);
            bolletta.setSpeseEnergia(ricalcoli.get("speseEnergia") != null ? ricalcoli.get("speseEnergia") : 0.0);
            bolletta.setTrasporti(ricalcoli.get("trasporti") != null ? ricalcoli.get("trasporti") : 0.0);
            bolletta.setOneri(ricalcoli.get("oneri") != null ? ricalcoli.get("oneri") : 0.0);
            bolletta.setImposte(ricalcoli.get("imposte") != null ? ricalcoli.get("imposte") : 0.0);
            bolletta.setVerificaTrasporti(ricalcoli.get("verificaTrasporti") != null ? ricalcoli.get("verificaTrasporti") : 0.0);
            bolletta.setVerificaOneri(ricalcoli.get("verificaOneri") != null ? ricalcoli.get("verificaOneri") : 0.0);
            bolletta.setVerificaImposte(ricalcoli.get("verificaImposte") != null ? ricalcoli.get("verificaImposte") : 0.0);
            bolletta.setVerificaPicco(ricalcoli.get("verificaPicco") != null ? ricalcoli.get("verificaPicco") : 0.0);
            bolletta.setVerificaFuoriPicco(ricalcoli.get("verificaFuoriPicco") != null ? ricalcoli.get("verificaFuoriPicco") : 0.0);
            bolletta.setTotAttivaPerdite(ricalcoli.get("totAttivaPerdite") != null ? ricalcoli.get("totAttivaPerdite") : 0.0);
            bolletta.setGeneration(ricalcoli.get("generation") != null ? ricalcoli.get("generation") : 0.0);
            bolletta.setVerificaDispacciamento(ricalcoli.get("dispacciamento") != null ? ricalcoli.get("dispacciamento") : 0.0);
            bolletta.setF1Penale33(ricalcoli.get("penali33") != null ? ricalcoli.get("penali33") : 0.0);
            bolletta.setF2Penale33(ricalcoli.get("penali75") != null ? ricalcoli.get("penali75") : 0.0);
            bolletta.setQuotaVariabileTrasporti(ricalcoli.get("quotaVariabileTrasporti") != null ? ricalcoli.get("quotaVariabileTrasporti") : 0.0);
            bolletta.setQuotaFissaTrasporti(ricalcoli.get("quotaFissaTrasporti") != null ? ricalcoli.get("quotaFissaTrasporti") : 0.0);
            bolletta.setQuotaPotenzaTrasporti(ricalcoli.get("quotaPotenzaTrasporti") != null ? ricalcoli.get("quotaPotenzaTrasporti") : 0.0);
            bolletta.setQuotaEnergiaOneri(ricalcoli.get("quotaEnergiaOneri") != null ? ricalcoli.get("quotaEnergiaOneri") : 0.0);
            bolletta.setQuotaFissaOneri(ricalcoli.get("quotaFissaOneri") != null ? ricalcoli.get("quotaFissaOneri") : 0.0);
            bolletta.setQuotaPotenzaOneri(ricalcoli.get("quotaPotenzaOneri") != null ? ricalcoli.get("quotaPotenzaOneri") : 0.0);

            persist(bolletta);
        }
    }

    private String capitalizeFirstThree(String mese) {
        if (mese == null || mese.length() < 3) return mese;
        String firstThree = mese.substring(0, 3).toLowerCase();
        return firstThree.substring(0, 1).toUpperCase() + firstThree.substring(1);
    }

    public void updateVerificaMateriaEnergia(Double spesaMateriaEnergia, String nomeBolletta, String mese) {
        update("SET verificaSpesaMateriaEnergia = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", spesaMateriaEnergia, nomeBolletta, mese);
    }

    public void updateCostoF0(Double f0Tot, String nomeBolletta, String mese) {
        update("SET verificaF0 = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", f0Tot, nomeBolletta, mese);
    }

    public void updateCostoF1(Double f1Tot, String nomeBolletta, String mese) {
        update("SET verificaF1 = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", f1Tot, nomeBolletta, mese);
    }

    public void updateCostoF2(Double f2Tot, String nomeBolletta, String mese) {
        update("SET verificaF2 = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", f2Tot, nomeBolletta, mese);
    }

    public void updateCostoF3(Double f3Tot, String nomeBolletta, String mese) {
        update("SET verificaF3 = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", f3Tot, nomeBolletta, mese);
    }

    public void updateCostoF1Perdite(Double f1PerditeTot, String nomeBolletta, String mese) {
        update("SET verificaF1Perdite = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", f1PerditeTot, nomeBolletta, mese);
    }

    public void updateCostoF2Perdite(Double f2PerditeTot, String nomeBolletta, String mese) {
        update("SET verificaF2Perdite = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", f2PerditeTot, nomeBolletta, mese);
    }

    public void updateCostoF3Perdite(Double f3PerditeTot, String nomeBolletta, String mese) {
        update("SET verificaF3Perdite = ?1 WHERE nomeBolletta = ?2 AND mese = ?3", f3PerditeTot, nomeBolletta, mese);
    }

    public List<BollettaPod> findBollettaPodByPods(List<Pod> pods) {
        List<String> podIds = pods.stream().map(Pod::getId).toList();
        return list("idPod IN ?1", podIds);
    }

    public List<BollettaPod> findByUserId(int userId) {
        return find("idUser", userId).list();
    }

    public AggregatoBollette getAggregatiPerPodAnnoMese(String podId, Integer anno, Integer mese) {
        String jpql = "SELECT SUM(b.totAttiva), SUM(b.speseEnergia), SUM(b.oneri) " +
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

}
