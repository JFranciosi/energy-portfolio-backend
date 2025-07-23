package miesgroup.mies.webdev.Service.cliente;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MailService {

    @Inject
    Mailer mailer;

    /**
     * Invia una mail semplice testuale.
     *
     * @param to destinatario
     * @param subject oggetto della mail
     * @param body testo della mail
     */
    public void send(String to, String subject, String body) {
        mailer.send(
                Mail.withText(to, subject, body)
        );
    }
}
