package miesgroup.mies.webdev.Rest.cliente;


import io.quarkus.mailer.Mailer;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;
import miesgroup.mies.webdev.Service.cliente.EmailService;
import miesgroup.mies.webdev.Service.cliente.SessionService;

import java.util.HashMap;
import java.util.Map;

@Path("/email")
public class EmailResource {

    @Inject
    EmailService emailService;
    @Inject
    ClienteRepo clienteService;
    @Inject
    SessionService sessionService;
    @Inject
    Mailer mailer;

    @Path("/checkAlert")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkAlert(@CookieParam("SESSION_COOKIE") int sessionId, JsonObject params) {
        int idUtente = sessionService.trovaUtentebBySessione(sessionId);
        if (idUtente == 0) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }

        Map<String, Boolean> checkAlertStates = emailService.checkAlertStates(idUtente);
        String futureType = params.getString("futuresType");

        if (checkAlertStates.getOrDefault("MonthlyAlert", false) &&
                checkAlertStates.getOrDefault("QuarterlyAlert", false) &&
                checkAlertStates.getOrDefault("YearlyAlert", false)) {
            return Response.ok(futureType.equals("All") ? "ok" : "All").build();
        }
        if (checkAlertStates.getOrDefault("MonthlyAlert", false)) {
            return Response.ok(futureType.equals("Monthly") ? "ok" : "Monthly").build();
        }
        if (checkAlertStates.getOrDefault("QuarterlyAlert", false)) {
            return Response.ok(futureType.equals("Quarterly") ? "ok" : "Quarterly").build();
        }
        if (checkAlertStates.getOrDefault("YearlyAlert", false)) {
            return Response.ok(futureType.equals("Yearly") ? "ok" : "Yearly").build();
        }
        if (checkAlertStates.getOrDefault("GeneralAlert", false)) {
            return Response.ok(futureType.equals("General") ? "ok" : "General").build();
        }

        return Response.ok("Nessun alert attivo").build();
    }

    @Path("/checkAlertField")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkAlertField(@CookieParam("SESSION_COOKIE") Integer sessionId) {
        if (sessionId == null || sessionId <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid session cookie.")
                    .build();
        }

        try {
            int idUtente = sessionService.trovaUtentebBySessione(sessionId);
            if (idUtente == 0) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Sessione non valida o scaduta.")
                        .build();
            }

            var alerts = emailService.checkUserAlertFillField(idUtente);
            var checkEmail = emailService.getCheckEmailStatus(idUtente);
            var response = new HashMap<>();
            response.put("alerts", alerts);
            response.put("checkEmail", checkEmail);

            return Response.ok(response).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while checking alerts.")
                    .build();
        }
    }

    @Path("/send-email")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendEmailToCliente(@CookieParam("SESSION_COOKIE") int sessionId, JsonObject params) {
        try {
            int idUtente = sessionService.trovaUtentebBySessione(sessionId);
            if (idUtente == 0) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Sessione non valida").build();
            }

            Cliente cliente = clienteService.findById(idUtente);
            if (cliente == null || cliente.getEmail() == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Cliente o email non trovati").build();
            }

            // Gestione della cancellazione alert
            if (params.containsKey("deleteAlert")) {
                JsonObject deleteAlert = params.getJsonObject("deleteAlert");
                if (deleteAlert != null && deleteAlert.getBoolean("active", false)) {
                    String futuresTypeToDelete = deleteAlert.getString("message");
                    Map<String, Boolean> result = emailService.deleteUserAlert(idUtente, futuresTypeToDelete);
                }
            }

            emailService.sendAlertConfigurationEmail(cliente, params);
            return Response.ok("Email inviata con successo a " + cliente.getEmail()).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante l'invio dell'email.").build();
        }
    }


}
