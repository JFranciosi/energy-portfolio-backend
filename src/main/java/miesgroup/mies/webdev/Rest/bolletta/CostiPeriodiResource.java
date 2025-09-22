package miesgroup.mies.webdev.Rest.bolletta;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Rest.bolletta.DTO.UpdatePeriodoRequest;
import miesgroup.mies.webdev.Service.bolletta.CostiPeriodiService;
import java.util.List;
import java.util.Map;

@Path("/costi-periodi")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CostiPeriodiResource {

    private final CostiPeriodiService costiPeriodiService;

    @Inject
    SessionRepo sessionRepo;

    public CostiPeriodiResource(CostiPeriodiService costiPeriodiService) {
        this.costiPeriodiService = costiPeriodiService;
    }

    /**
     * Aggiorna un singolo periodo dinamico
     */
    @PUT
    @Path("/{energyCostId}/{monthStart}")
    public Response updateSinglePeriodo(
            @PathParam("energyCostId") Integer energyCostId,
            @PathParam("monthStart") Integer monthStart,
            UpdatePeriodoRequest request,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            // Passa anche percentageVariable al service
            boolean updated = costiPeriodiService.updateSinglePeriodo(
                    energyCostId,
                    monthStart,
                    request.getCostF1(),
                    request.getCostF2(),
                    request.getCostF3(),
                    request.getPercentageVariable(), // ← AGGIUNGI QUESTO
                    userId
            );

            if (updated) {
                return Response.ok(Map.of("message", "Periodo aggiornato con successo")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Periodo non trovato o non autorizzato"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Verifica se la sequenza di periodi è valida
     */
    @GET
    @Path("/{energyCostId}/valid")
    public Response isValidSequenceForClient(
            @PathParam("energyCostId") Integer energyCostId,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            boolean isValid = costiPeriodiService.isValidSequenceForClient(energyCostId, userId);
            return Response.ok(Map.of("valid", isValid)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Ottiene i mesi disponibili per un costo energia
     */
    @GET
    @Path("/{energyCostId}/months")
    public Response getAvailableMonths(
            @PathParam("energyCostId") Integer energyCostId,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            List<Integer> months = costiPeriodiService.getAvailableMonths(energyCostId, userId);
            return Response.ok(Map.of("months", months)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Elimina un singolo periodo dinamico
     */
    @DELETE
    @Path("/{energyCostId}/{monthStart}")
    public Response deleteSinglePeriodo(
            @PathParam("energyCostId") Integer energyCostId,
            @PathParam("monthStart") Integer monthStart,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            boolean deleted = costiPeriodiService.deleteSinglePeriodo(energyCostId, monthStart, userId);

            if (deleted) {
                return Response.ok(Map.of("message", "Periodo eliminato con successo")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Periodo non trovato o non autorizzato"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }

}
