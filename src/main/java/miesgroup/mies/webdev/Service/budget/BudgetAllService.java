// BudgetAllService.java
package miesgroup.mies.webdev.Service.budget;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.budget.BudgetAll;
import miesgroup.mies.webdev.Repository.budget.BudgetAllRepo;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BudgetAllService {

    private final BudgetAllRepo allRepo;

    @PersistenceContext
    private EntityManager em;

    @Inject
    public BudgetAllService(BudgetAllRepo allRepo) {
        this.allRepo = allRepo;
    }

    /**
     * Upsert di BudgetAll: prima risolvo il Cliente, poi delego al repo.
     *
     * @return
     */
    @Transactional
    public boolean upsertAggregato(BudgetAll nuovo) {
        BudgetAll existing = em.createQuery(
                        "SELECT b FROM BudgetAll b WHERE b.cliente.id = :idUtente AND b.anno = :anno AND b.mese = :mese AND b.idPod = :pod",
                        BudgetAll.class
                )
                .setParameter("idUtente", nuovo.getCliente().getId())
                .setParameter("anno", nuovo.getAnno())
                .setParameter("mese", nuovo.getMese())
                .setParameter("pod", nuovo.getIdPod())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (existing == null) {
            em.persist(nuovo); // prima volta → insert
        } else {
            // update valori base, preservando percentuali già salvate
            existing.setPrezzoEnergiaBase(nuovo.getPrezzoEnergiaBase());
            existing.setConsumiBase(nuovo.getConsumiBase());
            existing.setOneriBase(nuovo.getOneriBase());
            // se vuoi aggiornare anche percentuali solo se erano null:
            if (existing.getPrezzoEnergiaPerc() == null) {
                existing.setPrezzoEnergiaPerc(nuovo.getPrezzoEnergiaPerc());
            }
            if (existing.getConsumiPerc() == null) {
                existing.setConsumiPerc(nuovo.getConsumiPerc());
            }
            if (existing.getOneriPerc() == null) {
                existing.setOneriPerc(nuovo.getOneriPerc());
            }
            em.merge(existing); // update
        }
        return false;
    }


    @Transactional(Transactional.TxType.SUPPORTS)
    public List<BudgetAll> getAggregatiPerUtenteEAnno(Long idUtente, Integer anno) {
        return allRepo.getByUtenteAndAnno(idUtente, anno);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<BudgetAll> getAggregatoSingolo(Long idUtente, Integer anno, Integer mese) {
        return allRepo.findByUtenteAnnoMese(idUtente, anno, mese);
    }

    @Transactional
    public long rimuoviAggregatiUtenteAnno(Long idUtente, Integer anno) {
        return allRepo.deleteByUtenteAndAnno(idUtente, anno);
    }
}
