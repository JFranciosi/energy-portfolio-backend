package miesgroup.mies.webdev.Rest.futures;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Service.futures.FuturesService;

import java.util.List;
import java.util.Map;

@Path("/futures")
public class FuturesResource {
    private final FuturesService futuresService;

    @Inject
    public FuturesResource(FuturesService futuresService) {
        this.futuresService = futuresService;
    }

    // Endpoint generico per dati future in base a tipo e data
    @GET
    @Path("/FuturesData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFuturesData(
            @QueryParam("date") String date,
            @QueryParam("type") String type) {

        List<Map<String, Object>> futuresList = futuresService.getFutures(date, type);

        if (futuresList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Dati non trovati per la selezione fornita.")
                    .build();
        }

        return Response.ok(futuresList).build();
    }

    @GET
    @Path("/futuresYear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFuturesYear(@QueryParam("date") String date) {
        List<Map<String, Object>> futuresList = futuresService.getFuturesYear(date);

        if (futuresList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Dati non trovati per la selezione fornita.")
                    .build();
        }

        return Response.ok(futuresList).build();
    }

    @GET
    @Path("/futuresQuarter")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFuturesQuarter(@QueryParam("date") String date) {
        List<Map<String, Object>> futuresList = futuresService.getFuturesQuarter(date);

        if (futuresList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Dati non trovati per la selezione fornita.")
                    .build();
        }

        return Response.ok(futuresList).build();
    }

    @GET
    @Path("/futuresMonth")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFuturesMonth(@QueryParam("date") String date) {
        List<Map<String, Object>> futuresList = futuresService.getFuturesMonth(date);

        if (futuresList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Dati non trovati per la selezione fornita.")
                    .build();
        }

        return Response.ok(futuresList).build();
    }

    @GET
    @Path("/last-date")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastDate(){
        String lastDate = futuresService.getLastDate();

        if (lastDate == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Ultima data non trovata.")
                    .build();
        }

        return Response.ok(lastDate).build();
    }
}
