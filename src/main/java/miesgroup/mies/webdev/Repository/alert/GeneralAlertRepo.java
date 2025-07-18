package miesgroup.mies.webdev.Repository.alert;

import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.alert.GeneralAlert;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import java.util.Optional;

@ApplicationScoped
public class GeneralAlertRepo implements PanacheRepositoryBase<GeneralAlert, Long> {

    public boolean existsByUserId(int userId) {
        return count("utente.id", userId) > 0;
    }

    public boolean deleteByUserId(int userId) {
        return delete("utente.id", userId) > 0;
    }

    public Optional<GeneralAlert> findByUserId(int userId) {
        return find("utente.id", userId).firstResultOptional();
    }

    public boolean saveOrUpdate(Cliente cliente, double max, double min, boolean check) {
        Optional<GeneralAlert> existing = find("idUtente", cliente.getId()).firstResultOptional();

        GeneralAlert alert = existing.orElseGet(GeneralAlert::new);

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