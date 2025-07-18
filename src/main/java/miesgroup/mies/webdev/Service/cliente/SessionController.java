package miesgroup.mies.webdev.Service.cliente;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import miesgroup.mies.webdev.Model.cliente.Sessione;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;

import java.util.Optional;

@ApplicationScoped // Mantiene il valore durante la richiesta
@Path("/session")
public class SessionController {

    private String sessionValue;
    private Integer userId; // Aggiungiamo un campo per l'ID utente

    private final SessionRepo sessionRepo; // Supponiamo che esista un repository per le sessioni

    public SessionController(SessionRepo sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @GET
    @Path("/extract-cookie")
    public void extractCookie(@CookieParam("SESSION_COOKIE") Integer sessionCookie) {
        setSessionValue(String.valueOf(sessionCookie));

        // Recuperiamo l'utente associato a questa sessione dal database
        Optional<Sessione> sessione = sessionRepo.getSessionById(sessionCookie);
        sessione.ifPresent(s -> setUserId(s.getUtente().getId()));

/*        System.out.println("Session ID: " + this.sessionValue);
        System.out.println("User ID associato: " + this.userId);*/
    }

    public String getSessionValue() {
        return this.sessionValue;
    }

    public void setSessionValue(String sessionValue) {
        this.sessionValue = sessionValue;
    }

    public Integer getUserId() {
        return this.userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}


