// BudgetAllRepo.java
package miesgroup.mies.webdev.Repository.budget;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.budget.BudgetAll;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BudgetAllRepo implements PanacheRepositoryBase<BudgetAll, Long> {

    /**
     * Inserisce o aggiorna un aggregato BudgetAll per utente, anno e mese.
     */
    public boolean upsert(BudgetAll ba) {
        if (ba.getId() == null) {
            // Controlla se esiste già una riga con stessa chiave logica (cliente, anno, mese, pod)
            Optional<BudgetAll> existing = find(
                    "cliente.id = ?1 and anno = ?2 and mese = ?3 and idPod = ?4",
                    ba.getCliente().getId(), ba.getAnno(), ba.getMese(), ba.getIdPod()
            ).firstResultOptional();

            if (existing.isPresent()) {
                BudgetAll e = existing.get();
                // aggiorna solo i valori base, preservando percentuali già presenti
                e.setPrezzoEnergiaBase(ba.getPrezzoEnergiaBase());
                e.setConsumiBase(ba.getConsumiBase());
                e.setOneriBase(ba.getOneriBase());
                if (e.getPrezzoEnergiaPerc() == null) e.setPrezzoEnergiaPerc(ba.getPrezzoEnergiaPerc());
                if (e.getConsumiPerc() == null) e.setConsumiPerc(ba.getConsumiPerc());
                if (e.getOneriPerc() == null) e.setOneriPerc(ba.getOneriPerc());
                getEntityManager().merge(e);
                return true;
            } else {
                persist(ba);
                return ba.getId() != null;
            }
        } else {
            ba = getEntityManager().merge(ba);
            return ba.getId() != null;
        }
    }

    /**
     * Recupera tutti i BudgetAll per un dato utente e anno.
     */
    public List<BudgetAll> getByUtenteAndAnno(Long idUtente, Integer anno) {
        return list("cliente.id = ?1 and anno = ?2", idUtente, anno);
    }

    /**
     * Recupera un singolo aggregato di BudgetAll per utente, anno e mese.
     */
    public Optional<BudgetAll> findByUtenteAnnoMese(Long idUtente, Integer anno, Integer mese) {
        return find("cliente.id = ?1 and anno = ?2 and mese = ?3", idUtente, anno, mese)
                .firstResultOptional();
    }

    /**
     * Elimina aggregati per utente e anno (ad esempio per refresh completo).
     */
    public long deleteByUtenteAndAnno(Long idUtente, Integer anno) {
        return delete("cliente.id = ?1 and anno = ?2", idUtente, anno);
    }

    public boolean upsertBudgetAll(BudgetAll budgetAll) {
        // Utilizza il metodo upsert per inserire o aggiornare
        return upsert(budgetAll);
    }
}
