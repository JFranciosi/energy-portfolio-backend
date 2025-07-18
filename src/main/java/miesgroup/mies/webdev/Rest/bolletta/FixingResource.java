package miesgroup.mies.webdev.Rest.bolletta;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Model.bolletta.Fixing;
import miesgroup.mies.webdev.Service.bolletta.FixingService;

import java.util.List;

@Path("/fixing")
public class FixingResource {

    private final FixingService fixingService;

    public FixingResource(FixingService fixingService) {
        this.fixingService = fixingService;

    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFixing(@CookieParam("SESSION_COOKIE") Integer sessionId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Sessione non valida").build();
        }
        try {
            return Response.ok(fixingService.getFixing(sessionId)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore nel recupero dei Fixing: " + e.getMessage()).build();
        }
    }

    @Path("/add")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFixing(@CookieParam("SESSION_COOKIE") Integer sessionId, List<Fixing> fixing) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Sessione non valida").build();
        }
        try {
            fixingService.addFixing(sessionId, fixing);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Errore nell'aggiunta dei Fixing: " + e.getMessage()).build();
        }
    }

    @Path("/update")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFixing(@CookieParam("SESSION_COOKIE") Integer sessionId, Fixing fixing) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Sessione non valida").build();
        }
        try {
            fixingService.updateFixing(sessionId, fixing);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Errore nell'aggiornamento del Fixing: " + e.getMessage()).build();
        }
    }

    @Path("/delete")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFixing(@CookieParam("SESSION_COOKIE") Integer sessionId, Fixing fixing) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Sessione non valida").build();
        }
        try {
            fixingService.deleteFixing(sessionId, fixing.getId());
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Errore nella cancellazione del Fixing: " + e.getMessage()).build();
        }
    }
}
