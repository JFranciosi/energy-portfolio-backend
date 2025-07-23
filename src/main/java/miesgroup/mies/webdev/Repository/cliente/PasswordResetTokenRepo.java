package miesgroup.mies.webdev.Repository.cliente;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import miesgroup.mies.webdev.Model.cliente.PasswordResetToken;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class PasswordResetTokenRepo implements PanacheRepository<PasswordResetToken> {

    public Optional<PasswordResetToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    public void deleteByToken(String token) {
        delete("token", token);
    }

    public void deleteExpiredTokens() {
        delete("expiryDate < current_timestamp()");
    }
}
