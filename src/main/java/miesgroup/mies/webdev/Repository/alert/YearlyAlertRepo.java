package miesgroup.mies.webdev.Repository.alert;

import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.alert.YearlyAlert;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import java.util.Optional;

@ApplicationScoped
public class YearlyAlertRepo implements PanacheRepositoryBase<YearlyAlert, Long> {

    public boolean existsByUserId(int userId) {
        return count("utente.id", userId) > 0;
    }

    public boolean deleteByUserId(int userId) {
        return delete("utente.id", userId) > 0;
    }

    public Optional<YearlyAlert> findByUserId(int userId) {
        return find("utente.id", userId).firstResultOptional();
    }

    public boolean saveOrUpdate(Cliente cliente, double max, double min, boolean check) {
        Optional<YearlyAlert> existing = find("idUtente", cliente.getId()).firstResultOptional();

        YearlyAlert alert = existing.orElseGet(YearlyAlert::new);

        alert.setIdUtente(cliente.getId());
        alert.setUtente(cliente);
        alert.setMaxPriceValue(max);
        alert.setMinPriceValue(min);
        alert.setCheckModality(check);

        if (existing.isEmpty()) {
            persist(alert); // solo se nuovo
        }

        return true;
    }
}
