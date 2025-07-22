package miesgroup.mies.webdev.Repository.budget;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.budget.Budget;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BudgetRepo implements PanacheRepositoryBase<Budget, Long> {

    /**
     * Inserisce o aggiorna un record Budget.
     */
    public boolean upsert(Budget budget) {
        if (budget.getId() == null) {
            persist(budget);
        } else {
            budget = getEntityManager().merge(budget);
        }
        return budget.getId() != null;
    }


    /**
     * Recupera tutti i Budget per un dato POD e anno.
     */
    public List<Budget> getByPodAndAnno(String idPod, Integer anno) {
        return list("idPod = ?1 and anno = ?2", idPod, anno);
    }

    /**
     * Recupera un singolo Budget per POD, anno e mese.
     */
    public Optional<Budget> findByPodAnnoMese(String idPod, Integer anno, Integer mese) {
        return find("idPod = ?1 and anno = ?2 and mese = ?3", idPod, anno, mese)
                .firstResultOptional();
    }

    /**
     * Aggiorna le percentuali di previsione per un record esistente.
     */
    public boolean updatePrevisione(String idPod, Integer anno, Integer mese,
                                    Double prezzoPerc, Double consumiPerc, Double oneriPerc) {
        return update(
                "prezzoEnergiaPerc = ?1, consumiPerc = ?2, oneriPerc = ?3 "
                        + "WHERE idPod = ?4 and anno = ?5 and mese = ?6",
                prezzoPerc, consumiPerc, oneriPerc, idPod, anno, mese
        ) > 0;
    }

    /**
     * Aggiunge un nuovo Budget o aggiorna uno esistente.
     */
    public boolean aggiungiBudget(Budget budget) {
        return upsert(budget);
    }
}
