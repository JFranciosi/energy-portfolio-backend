package miesgroup.mies.webdev.Repository.futures;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.futures.Yearly;

@ApplicationScoped
public class YearlyRepo implements PanacheRepository<Yearly> {
}

