// BudgetService.java
package miesgroup.mies.webdev.Service.budget;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Repository.budget.BudgetRepo;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BudgetService {

    private final BudgetRepo budgetRepo;

    public BudgetService(BudgetRepo budgetRepo) {
        this.budgetRepo = budgetRepo;
    }

    /**
     * Crea un nuovo Budget.
     */
    @Transactional
    public boolean creaBudget(Budget budget) {
        return budgetRepo.aggiungiBudget(budget);
    }

    /**
     * Recupera tutti i Budget per un singolo POD e anno.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Budget> getBudgetsPerPodEAnno(String podId, Integer anno) {
        return budgetRepo.getByPodAndAnno(podId, anno);
    }

    /**
     * Recupera un singolo Budget per POD, anno e mese.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<Budget> getBudgetSingolo(String podId, Integer anno, Integer mese) {
        return budgetRepo.findByPodAnnoMese(podId, anno, mese);
    }

    /**
     * Aggiorna le percentuali di previsione per un Budget esistente.
     */
    @Transactional
    public boolean aggiornaPrevisione(
            String podId,
            Integer anno,
            Integer mese,
            Double prezzoPerc,
            Double consumiPerc,
            Double oneriPerc) {
        return budgetRepo.updatePrevisione(podId, anno, mese, prezzoPerc, consumiPerc, oneriPerc);
    }

    @Transactional
    public boolean upsertPrevisione(String podId, Integer anno, Integer mese,
                                    Double prezzoPerc, Double consumiPerc, Double oneriPerc) {
        Optional<Budget> optionalBudget = budgetRepo.findByPodAnnoMese(podId, anno, mese);
        Budget budget;
        if (optionalBudget.isPresent()) {
            budget = optionalBudget.get();
            budget.setPrezzoEnergiaPerc(prezzoPerc);
            budget.setConsumiPerc(consumiPerc);
            budget.setOneriPerc(oneriPerc);
            // aggiorna entity (JPA fa update automatico)
            return true;
        } else {
            // crea nuovo budget con valori base da qualche parte, se servono
            budget = new Budget();
            budget.setPodId(podId);
            budget.setAnno(anno);
            budget.setMese(mese);
            budget.setPrezzoEnergiaPerc(prezzoPerc);
            budget.setConsumiPerc(consumiPerc);
            budget.setOneriPerc(oneriPerc);
            // Imposta valori base se puoi recuperarli, altrimenti metti 0 o default
            budget.setPrezzoEnergiaBase(0.1); // esempio
            budget.setConsumiBase(0.0);
            budget.setOneriBase(0.0);
            return budgetRepo.aggiungiBudget(budget);
        }
    }

    /**
     * Elimina un Budget per ID.
     */

    public List<String> getAllPodIds() {
        return budgetRepo.findAll()
                .stream()
                .map(Budget::getPodId)
                .distinct()
                .toList();
    }
}
