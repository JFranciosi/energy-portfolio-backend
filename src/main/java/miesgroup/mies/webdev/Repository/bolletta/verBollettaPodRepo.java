package miesgroup.mies.webdev.Repository.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Model.bolletta.verBollettaPod;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApplicationScoped
public class verBollettaPodRepo implements PanacheRepositoryBase<verBollettaPod, Integer> {

    @PersistenceContext
    EntityManager em;

    /* ======================
       Helpers base
       ====================== */

    public verBollettaPod findByBollettaId(BollettaPod bollettaId) {
        return find("bollettaId", bollettaId).firstResult();
    }

    /**
     * Trova un record per nome bolletta
     */
    public Optional<verBollettaPod> findByNomeBolletta(String nomeBolletta) {
        return find("nomeBolletta", nomeBolletta).firstResultOptional();
    }

    public List<verBollettaPod> findByPodList(List<Pod> pods) {
        List<String> podIds = pods.stream().map(Pod::getId).toList();
        return list("idPod IN ?1", podIds);
    }


    public void deleteByBollettaId(Integer bollettaId) {
        delete("bollettaId", bollettaId);
    }

    /* ======================
       Update identificativi
       ====================== */

    public void updateIdPod(Integer bollettaId, String idPod) {
        update("SET idPod = ?2 WHERE bollettaId = ?1", bollettaId, idPod);
    }

    public void updateNomeBolletta(Integer bollettaId, String nomeBolletta) {
        update("SET nomeBolletta = ?2 WHERE bollettaId = ?1", bollettaId, nomeBolletta);
    }

    /* ======================
       Attiva per fascia
       ====================== */

    public void updateF1Att(Integer bollettaId, Double v){ update("SET f1Att = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateF2Att(Integer bollettaId, Double v){ update("SET f2Att = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateF3Att(Integer bollettaId, Double v){ update("SET f3Att = ?2 WHERE bollettaId = ?1", bollettaId, v); }

    public void updateAttive(Integer bollettaId, Double f1, Double f2, Double f3) {
        update("SET f1Att = ?2, f2Att = ?3, f3Att = ?4 WHERE bollettaId = ?1", bollettaId, f1, f2, f3);
    }

    /* ======================
       Reattiva per fascia
       ====================== */

    public void updateF1R(Integer bollettaId, Double v){ update("SET f1R = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateF2R(Integer bollettaId, Double v){ update("SET f2R = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateF3R(Integer bollettaId, Double v){ update("SET f3R = ?2 WHERE bollettaId = ?1", bollettaId, v); }

    public void updateReattive(Integer bollettaId, Double f1, Double f2, Double f3) {
        update("SET f1R = ?2, f2R = ?3, f3R = ?4 WHERE bollettaId = ?1", bollettaId, f1, f2, f3);
    }

    /* ======================
       Reattiva Capacitiva/Induttiva immessa
       ====================== */

    public void updateRCapI(Integer bollettaId, Double f1, Double f2, Double f3) {
        update("SET f1RCapI = ?2, f2RCapI = ?3, f3RCapI = ?4 WHERE bollettaId = ?1", bollettaId, f1, f2, f3);
    }
    public void updateRIndI(Integer bollettaId, Double f1, Double f2, Double f3) {
        update("SET f1RIndI = ?2, f2RIndI = ?3, f3RIndI = ?4 WHERE bollettaId = ?1", bollettaId, f1, f2, f3);
    }

    /* ======================
       Potenza per fascia
       ====================== */

    public void updatePotenza(Integer bollettaId, Double f1, Double f2, Double f3) {
        update("SET f1Pot = ?2, f2Pot = ?3, f3Pot = ?4 WHERE bollettaId = ?1", bollettaId, f1, f2, f3);
    }

    /* ======================
       Spese macro
       ====================== */

    public void updateSpese(Integer bollettaId, Double speseEne, Double speseTrasp, Double oneri, Double imposte) {
        update("SET speseEne = ?2, speseTrasp = ?3, oneri = ?4, imposte = ?5 WHERE bollettaId = ?1",
                bollettaId, speseEne, speseTrasp, oneri, imposte);
    }
    public void updateSpeseEne(Integer bollettaId, Double v){ update("SET speseEne = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateSpeseTrasp(Integer bollettaId, Double v){ update("SET speseTrasp = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateOneri(Integer bollettaId, Double v){ update("SET oneri = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateImposte(Integer bollettaId, Double v){ update("SET imposte = ?2 WHERE bollettaId = ?1", bollettaId, v); }

    /* ======================
       Totali
       ====================== */

    public void updateTotali(Integer bollettaId, Double totAtt, Double totR, Double totRCapI, Double totRIndI) {
        update("SET totAtt = ?2, totR = ?3, totRCapI = ?4, totRIndI = ?5 WHERE bollettaId = ?1",
                bollettaId, totAtt, totR, totRCapI, totRIndI);
    }
    public void updateTotAtt(Integer bollettaId, Double v){ update("SET totAtt = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateTotR(Integer bollettaId, Double v){ update("SET totR = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateTotRCapI(Integer bollettaId, Double v){ update("SET totRCapI = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateTotRIndI(Integer bollettaId, Double v){ update("SET totRIndI = ?2 WHERE bollettaId = ?1", bollettaId, v); }

    /* ======================
       Altri importi
       ====================== */

    public void updateGeneration(Integer bollettaId, Double v){ update("SET generation = ?2 WHERE bollettaId = ?1", bollettaId, v); }
    public void updateDispacciamento(Integer bollettaId, Double v){ update("SET dispacciamento = ?2 WHERE bollettaId = ?1", bollettaId, v); }

    /* ======================
       Penali
       ====================== */

    public void updatePenali(Integer bollettaId, Double f1Pen33, Double f1Pen75, Double f2Pen33, Double f2Pen75) {
        update("SET f1Penale33 = ?2, f1Penale75 = ?3, f2Penale33 = ?4, f2Penale75 = ?5 WHERE bollettaId = ?1",
                bollettaId, f1Pen33, f1Pen75, f2Pen33, f2Pen75);
    }
    public void updatePenRCapI(Integer bollettaId, Double v){ update("SET penRCapI = ?2 WHERE bollettaId = ?1", bollettaId, v); }

    /* ======================
       Picco / Fuori Picco
       ====================== */

    public void updatePiccoKwh(Integer bollettaId, Double picco, Double fuoriPicco) {
        update("SET piccoKwh = ?2, fuoriPiccoKwh = ?3 WHERE bollettaId = ?1", bollettaId, picco, fuoriPicco);
    }

    public void updatePiccoEuro(Integer bollettaId, Double euroPicco, Double euroFuoriPicco) {
        update("SET euroPicco = ?2, euroFuoriPicco = ?3 WHERE bollettaId = ?1", bollettaId, euroPicco, euroFuoriPicco);
    }

    /* ======================
       Perdite
       ====================== */

    public void updateTotAttPerd(Integer bollettaId, Double v){ update("SET totAttPerd = ?2 WHERE bollettaId = ?1", bollettaId, v); }

    /* ======================
       Breakdown fasce F0/F1/F2/F3
       ====================== */

    public void updateF0(Integer bollettaId, Double euro, Double kwh) {
        update("SET f0Euro = ?2, f0Kwh = ?3 WHERE bollettaId = ?1", bollettaId, euro, kwh);
    }

    public void updateF1(Integer bollettaId, Double euro, Double kwh, Double perdEuro, Double perdKwh) {
        update("SET f1Euro = ?2, f1Kwh = ?3, f1PerdEuro = ?4, f1PerdKwh = ?5 WHERE bollettaId = ?1",
                bollettaId, euro, kwh, perdEuro, perdKwh);
    }

    public void updateF2(Integer bollettaId, Double euro, Double kwh, Double perdEuro, Double perdKwh) {
        update("SET f2Euro = ?2, f2Kwh = ?3, f2PerdEuro = ?4, f2PerdKwh = ?5 WHERE bollettaId = ?1",
                bollettaId, euro, kwh, perdEuro, perdKwh);
    }

    public void updateF3(Integer bollettaId, Double euro, Double kwh, Double perdEuro, Double perdKwh) {
        update("SET f3Euro = ?2, f3Kwh = ?3, f3PerdEuro = ?4, f3PerdKwh = ?5 WHERE bollettaId = ?1",
                bollettaId, euro, kwh, perdEuro, perdKwh);
    }

    /* ======================
       Quote Trasporti
       ====================== */

    public void updateQuoteTrasporti(Integer bollettaId, Double qVar, Double qFix, Double qPot) {
        update("SET qVarTrasp = ?2, qFixTrasp = ?3, qPotTrasp = ?4 WHERE bollettaId = ?1",
                bollettaId, qVar, qFix, qPot);
    }

    /* ======================
       Oneri ASOS/ARIM
       ====================== */

    public void updateOneriASOS(Integer bollettaId, Double qEn, Double qFix, Double qPot) {
        update("SET qEnOnASOS = ?2, qFixOnASOS = ?3, qPotOnASOS = ?4 WHERE bollettaId = ?1",
                bollettaId, qEn, qFix, qPot);
    }

    public void updateOneriARIM(Integer bollettaId, Double qEn, Double qFix, Double qPot) {
        update("SET qEnOnARIM = ?2, qFixOnARIM = ?3, qPotOnARIM = ?4 WHERE bollettaId = ?1",
                bollettaId, qEn, qFix, qPot);
    }

    /* ======================
       Componenti DISPACCIAMENTO
       ====================== */

    public void updateDispacciamentoComponents(Integer bollettaId,
                                               Double art25bis, Double art443, Double art44bis,
                                               Double art46, Double art48, Double art73,
                                               Double art45Ann, Double art45Tri) {
        update("SET art25bis = ?2, art44_3 = ?3, art44bis = ?4, art46 = ?5, art48 = ?6, art73 = ?7, art45Ann = ?8, art45Tri = ?9 WHERE bollettaId = ?1",
                bollettaId, art25bis, art443, art44bis, art46, art48, art73, art45Ann, art45Tri);
    }

    /* ======================
       Componenti TRASPORTO (dettaglio)
       ====================== */

    public void updateTrasportoComponents(Integer bollettaId,
                                          Double uc3uc6, Double traspQEne, Double distrQEne,
                                          Double distrQPot, Double misQFix, Double distrQFix) {
        update("SET uc3_UC6 = ?2, trasp_QEne = ?3, distr_QEne = ?4, distr_QPot = ?5, mis_QFix = ?6, distr_QFix = ?7 WHERE bollettaId = ?1",
                bollettaId, uc3uc6, traspQEne, distrQEne, distrQPot, misQFix, distrQFix);
    }

    /* ======================================================
       UPSERT COMPLETO (carica tutto in una volta) by bollettaId
       ====================================================== */

    /**
     * Upsert FULL della riga su ver_bolletta_pod.
     * Se esiste (per bollettaId) -> merge completo, altrimenti persist.
     */
    public verBollettaPod upsertAllByBollettaId(verBollettaPod payload) {

        System.out.println("[verRepo] upsertAll payload.bollettaId="
                + (payload.getBollettaId() != null ? payload.getBollettaId().getId() : "null"));

        verBollettaPod existing = findByBollettaId(payload.getBollettaId());

        System.out.println("[verRepo] existing="
                + (existing == null ? "null" : existing.getId()));

        if (existing == null) {
            persist(payload);
            System.out.println("[verRepo] persist OK, new id=" + payload.getId());
            return payload;
        } else {
            payload.setId(existing.getId());
            payload.setBollettaId(existing.getBollettaId());
            verBollettaPod merged = em.merge(payload);
            System.out.println("[verRepo] merge OK, id=" + merged.getId());
            return merged;
        }

    }

    /**
     * Variante builder: passi solo bollettaId e una lambda che valorizza tutti i campi.
     *
     * Esempio:
     * repo.upsertAllByBollettaId(123, v -> {
     *     v.setNomeBolletta("A2A-Gen");
     *     v.setTotAtt(12000.0);
     *     v.setQFixTrasp(30.0);
     *     // ... ecc
     * });
     */
    public verBollettaPod upsertAllByBollettaId(BollettaPod bollettaId, Consumer<verBollettaPod> fill) {
        if (bollettaId == null) throw new IllegalArgumentException("bollettaId obbligatorio");
        verBollettaPod existing = findByBollettaId(bollettaId);
        if (existing == null) {
            verBollettaPod created = new verBollettaPod();
            created.setBollettaId(bollettaId);
            if (fill != null) fill.accept(created);
            persist(created);
            return created;
        } else {
            verBollettaPod toMerge = new verBollettaPod();
            toMerge.setId(existing.getId());
            toMerge.setBollettaId(existing.getBollettaId());
            if (fill != null) fill.accept(toMerge);
            return em.merge(toMerge);
        }
    }
}
