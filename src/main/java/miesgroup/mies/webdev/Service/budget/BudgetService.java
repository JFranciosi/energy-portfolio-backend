package miesgroup.mies.webdev.Service.budget;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Repository.bolletta.AggregatoBollette;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.budget.BudgetRepo;
import miesgroup.mies.webdev.Rest.Model.BudgetDto;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BudgetService {

    @Inject
    BudgetRepo budgetRepo;
    @Inject
    BollettaPodRepo bollettaRepo;
    @Inject
    EntityManager em;

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
            budget = new Budget();
            budget.setPodId(podId);
            budget.setAnno(anno);
            budget.setMese(mese);
            budget.setPrezzoEnergiaPerc(prezzoPerc);
            budget.setConsumiPerc(consumiPerc);
            budget.setOneriPerc(oneriPerc);
            budget.setPrezzoEnergiaBase(0.1); // default
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

    /**
     * Aggiorna il Budget a partire dai dati aggregati della bolletta
     */
    @Transactional
    public void aggiornaBudgetDaBolletta(String podId, Integer anno, Integer mese) {
        AggregatoBollette agg = bollettaRepo.getAggregatiPerPodAnnoMese(podId, anno, mese);
        if (agg == null || agg.getConsumoTotale() == null || agg.getConsumoTotale() == 0) {
            throw new IllegalArgumentException("Dati bolletta insufficienti o zero consumo");
        }
        if (agg.getSpesaEnergiaTotale() == null || agg.getSpesaEnergiaTotale() <= 0) {
            throw new IllegalArgumentException("Spesa energia totale nulla o negativa");
        }

        // Calcolo corretto del prezzo energia base (€/kWh)
        double prezzoEnergiaBase = agg.getSpesaEnergiaTotale() / agg.getConsumoTotale();

        // Controllo valori anomali per sicurezza
        if (prezzoEnergiaBase <= 0 || prezzoEnergiaBase > 5) {
            throw new IllegalArgumentException("Prezzo energia base anomalo: " + prezzoEnergiaBase
                    + " (Spesa: " + agg.getSpesaEnergiaTotale() + ", Consumo: " + agg.getConsumoTotale() + ")");
        }

        Optional<Budget> optBudget = budgetRepo.findByPodAnnoMese(podId, anno, mese);
        Budget budget = optBudget.orElseGet(() -> {
            Budget b = new Budget();
            b.setPodId(podId);
            b.setAnno(anno);
            b.setMese(mese);
            b.setPrezzoEnergiaPerc(0.0);
            b.setConsumiPerc(0.0);
            b.setOneriPerc(0.0);
            return b;
        });

        // Salva i valori base calcolati
        budget.setPrezzoEnergiaBase(prezzoEnergiaBase);
        budget.setConsumiBase(agg.getConsumoTotale());
        budget.setOneriBase(agg.getOneriTotale() != null ? agg.getOneriTotale() : 0.0);

        budgetRepo.upsert(budget);
    }

    public double calcolaPrezzoEnergiaUnitarioDaBudget(Budget budget) {
        if (budget == null) throw new IllegalArgumentException("Budget nullo");
        if (budget.getConsumiBase() == null || budget.getConsumiBase() == 0)
            throw new IllegalArgumentException("Consumi base nulli o zero");
        if (budget.getPrezzoEnergiaBase() == null || budget.getPrezzoEnergiaBase() <= 0)
            throw new IllegalArgumentException("Prezzo energia base nullo o negativo");

        return budget.getPrezzoEnergiaBase() / budget.getConsumiBase();
    }

}
