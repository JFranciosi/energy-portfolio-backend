package miesgroup.mies.webdev.Repository.pbi;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.pbi.Secret;

@ApplicationScoped
public class SecretRepo implements PanacheRepositoryBase<Secret, Integer> {


}
