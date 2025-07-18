// BudgetAllService.java
package miesgroup.mies.webdev.Service.budget;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.budget.BudgetAll;
import miesgroup.mies.webdev.Model.cliente.Cliente;
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
     */
    @Transactional
    public boolean upsertAggregato(BudgetAll budgetAll) {
        // Imposto IdPod su "ALL" per l'aggregato
        budgetAll.setIdPod("ALL");

        // 1) risolvo l'entità Cliente dal DB
        Long clienteId = (long) budgetAll.getCliente().getId();
        Cliente managedCliente = em.find(Cliente.class, clienteId);
        if (managedCliente == null) {
            throw new IllegalArgumentException("Cliente non trovato: " + clienteId);
        }
        budgetAll.setCliente(managedCliente);

        // 2) delego al repository
        return allRepo.upsertBudgetAll(budgetAll);
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
