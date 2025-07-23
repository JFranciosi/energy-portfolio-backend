package miesgroup.mies.webdev.Service.cliente;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.cliente.PasswordResetToken;
import miesgroup.mies.webdev.Repository.cliente.PasswordResetTokenRepo;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PasswordResetService {

    @Inject
    PasswordResetTokenRepo tokenRepo;

    @Inject
    ClienteRepo clienteRepo;

    @Inject
    HashCalculator hashCalculator;

    @Inject
    MailService mailService;

    /**
     * Crea un token per reset password, lo salva, e ritorna il token.
     */
    @Transactional
    public String createPasswordResetToken(String email) {
        Cliente cliente = clienteRepo.find("email", email).firstResult();
        if (cliente == null) {
            return null; // oppure throw eccezione o gestisci come preferisci
        }

        // Crea token univoco
        String token = UUID.randomUUID().toString();

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setCliente(cliente);
        prt.setExpiryDate(LocalDateTime.now().plusHours(1)); // es. token valido 1 ora

        tokenRepo.persist(prt);

        return token;
    }

    /**
     * Verifica che il token sia valido (esiste e non scaduto) e ritorna il cliente associato.
     */
    public Cliente validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> optionalPrt = tokenRepo.findByToken(token);
        if (optionalPrt.isEmpty()) return null;

        PasswordResetToken prt = optionalPrt.get();
        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return null; // token invalido o scaduto
        }
        return prt.getCliente();
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> optionalPrt = tokenRepo.findByToken(token);
        if (optionalPrt.isEmpty()) return false;

        PasswordResetToken prt = optionalPrt.get();
        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false; // token invalido o scaduto
        }

        Cliente cliente = prt.getCliente();
        cliente.setPassword(hashCalculator.calculateHash(newPassword));
        cliente.persist();

        tokenRepo.deleteByToken(token); // elimina token dopo uso

        return true;
    }

    @Transactional
    public void createAndSendResetToken(String email) {
        // Cerca cliente per email
        Cliente cliente = clienteRepo.find("email", email).firstResult();
        if (cliente == null) {
            throw new RuntimeException("Email non trovata");
        }

        // Genera token univoco e sicuro
        String token = UUID.randomUUID().toString();

        // Crea PasswordResetToken
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setCliente(cliente);
        prt.setExpiryDate(LocalDateTime.now().plusHours(1)); // scadenza 1 ora

        // Salva token
        tokenRepo.persist(prt);

        // Costruisci link reset (assumendo frontend su http://localhost:3000)
        String resetLink = "http://localhost:8080/reset-password?token=" + token;

        // Corpo email (puoi migliorare con template HTML)
        String emailBody = "Ciao,\n\nHai richiesto il reset della password. "
                + "Clicca il link seguente per impostare una nuova password:\n"
                + resetLink + "\n\nSe non hai richiesto questo reset, ignora questa email.\n\nSaluti,\nTeam MiesGroup";

        // Invia mail
        mailService.send(
                email,
                "Reset password MiesGroup",
                emailBody
        );
    }
}
