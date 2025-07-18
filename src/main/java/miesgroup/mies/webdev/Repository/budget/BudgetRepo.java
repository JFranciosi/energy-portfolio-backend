// BudgetRepo.java
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
        // PanacheRepositoryBase.persist() gestisce sia persist che merge
        persist(budget);
        // considera persistito se ha un id generato
        return budget.getId() != null;
    }

    /**
     * Recupera tutti i Budget per un dato POD e anno.
     */
    public List<Budget> getByPodAndAnno(String podId, Integer anno) {
        return list("podId = ?1 and anno = ?2", podId, anno);
    }

    /**
     * Recupera un singolo Budget per POD, anno e mese.
     */
    public Optional<Budget> findByPodAnnoMese(String podId, Integer anno, Integer mese) {
        return find("podId = ?1 and anno = ?2 and mese = ?3", podId, anno, mese)
                .firstResultOptional();
    }

    /**
     * Aggiorna le percentuali di previsione per un record esistente.
     */
    public boolean updatePrevisione(String podId, Integer anno, Integer mese,
                                    Double prezzoPerc, Double consumiPerc, Double oneriPerc) {
        return update(
                "prezzoEnergiaPerc = ?1, consumiPerc = ?2, oneriPerc = ?3 "
                        + "WHERE podId = ?4 and anno = ?5 and mese = ?6",
                prezzoPerc, consumiPerc, oneriPerc, podId, anno, mese
        ) > 0;
    }

    /**
     * Aggiunge un nuovo Budget o aggiorna uno esistente.
     */

    public boolean aggiungiBudget(Budget budget) {
        // Utilizza il metodo upsert per inserire o aggiornare
        return upsert(budget);
    }

    public boolean saveOrUpdate(Budget budget) {
        if (budget.getId() == null) {
            persist(budget); // Inserisce un nuovo record
        } else {
            budget = getEntityManager().merge(budget); // Aggiorna un record esistente
        }
        return budget.getId() != null; // Restituisce true se l'operazione ha avuto successo
    }

}
