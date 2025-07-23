package miesgroup.mies.webdev.Rest.cliente;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Rest.Model.ClienteRequest;
import miesgroup.mies.webdev.Rest.Model.ClienteResponse;
import miesgroup.mies.webdev.Rest.Model.LoginRequest;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.cliente.Sessione;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Service.cliente.AutenticationService;
import miesgroup.mies.webdev.Service.cliente.ClienteService;
import miesgroup.mies.webdev.Service.cliente.PasswordResetService;
import miesgroup.mies.webdev.Service.exception.ClienteCreationException;
import miesgroup.mies.webdev.Service.exception.SessionCreationException;
import miesgroup.mies.webdev.Service.exception.WrongUsernameOrPasswordException;
import miesgroup.mies.webdev.Service.cliente.SessionService;

import java.util.Map;
import java.util.Optional;

@Path("/Autentication")
public class AutenticationResource {

    @Inject
    PasswordResetService passwordResetService;

    private final AutenticationService autenticationService;
    private final ClienteService clienteService;
    private final ClienteRepo clienteRepo;
    private final SessionRepo sessionRepo;
    private final SessionService sessionService;

    public AutenticationResource(
            AutenticationService autenticationService,
            ClienteService clienteService,
            ClienteRepo clienteRepo,
            SessionRepo sessionRepo,
            SessionService sessionService) {

        this.autenticationService = autenticationService;
        this.clienteService = clienteService;
        this.clienteRepo = clienteRepo;
        this.sessionRepo = sessionRepo;
        this.sessionService = sessionService;
    }

    @POST
    @Path("/Register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(ClienteRequest clienteRequest) throws ClienteCreationException {
        Cliente cliente = clienteService.createCliente(clienteRequest);
        return Response.ok("utente registrato").build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest request) {
        try {
            Optional<Cliente> maybeUtente = clienteRepo.findByUsername(request.getUsername());
            NewCookie sessionCookie = null;

            if (maybeUtente.isPresent()) {
                Optional<Sessione> maybeSessione = sessionRepo.getSessionByUserId(maybeUtente.get().getId());

                // Se l'utente ha già una sessione attiva, la invalidiamo
                if (maybeSessione.isPresent()) {
                    autenticationService.logout(maybeSessione.get().getId());

                    // Cookie che scade subito per invalidare quello esistente
                    sessionCookie = new NewCookie.Builder("SESSION_COOKIE")
                            .value("") // Vuoto per invalidarlo
                            .path("/")
                            .maxAge(0) // Scade immediatamente
                            .build();
                }
            }

            // Ora creiamo una nuova sessione
            int sessione = autenticationService.login(request.getUsername(), request.getPassword());

            // Creiamo il nuovo session cookie
            NewCookie newSessionCookie = new NewCookie.Builder("SESSION_COOKIE")
                    .value(String.valueOf(sessione))
                    .path("/")
                    .sameSite(NewCookie.SameSite.LAX)
                    .secure(false)
                    .httpOnly(true)
                    .build();

            return Response.ok()
                    .cookie(sessionCookie, newSessionCookie) // Passiamo entrambi per invalidare e creare
                    .build();

        } catch (SessionCreationException | WrongUsernameOrPasswordException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Login failed: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An unexpected error occurred: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/logout")
    public Response logout(@CookieParam("SESSION_COOKIE") int sessionId) {
        try {
            autenticationService.logout(sessionId);
            NewCookie sessionCookie = new NewCookie.Builder("SESSION_COOKIE")
                    .path("/")
                    .build();
            return Response.ok()
                    .cookie(sessionCookie)
                    .build();
        } catch (Exception e) {
            System.err.println("Errore durante il logout: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante il logout")
                    .build();
        }
    }

    @GET
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response check(@CookieParam("SESSION_COOKIE") int sessionId) {
        try {
            Integer sessione = sessionService.trovaUtentebBySessione(sessionId);
            if (sessione != null) {
                return Response.ok("Sessione presente").build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Sessione non valida")
                        .build();
            }
        } catch (Exception e) {
            System.err.println("Errore durante il check della sessione: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore interno durante il controllo della sessione")
                    .build();
        }
    }

    @GET
    @Path("/checkCategoria")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkCategoria(@CookieParam("SESSION_COOKIE") int sessionId) {
        try {
            Cliente c = sessionService.trovaUtenteCategoryBySessione(sessionId);
            if (c == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Cliente non trovato")
                        .build();
            } else {
                ClienteResponse response = clienteService.parseResponse(c);
                return Response.ok(response).build();
            }
        } catch (Exception e) {
            System.err.println("Errore durante il check della categoria: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore interno durante il controllo della categoria")
                    .build();
        }
    }
    @POST
    @Path("/forgot-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> forgotPassword(Map<String, String> request) throws Exception {
        String email = request.get("email");
        passwordResetService.createAndSendResetToken(email);
        return Map.of("message", "Email inviata");
    }

    @POST
    @Path("/reset-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> resetPassword(Map<String, String> request) throws Exception {
        String token = request.get("token");
        String newPassword = request.get("password");
        passwordResetService.resetPassword(token, newPassword);
        return Map.of("message", "Password aggiornata con successo");
    }
}
