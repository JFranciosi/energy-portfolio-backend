package miesgroup.mies.webdev.Repository.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.bolletta.Fixing;

import java.util.List;

@ApplicationScoped
public class FixingRepo implements PanacheRepositoryBase<Fixing, Integer> {

    public List<Fixing> getFixing(Integer idC) {
        return find("utente.id", idC).list();
    }

}
