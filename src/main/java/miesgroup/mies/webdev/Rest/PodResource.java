package miesgroup.mies.webdev.Rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Model.PDFFile;
import miesgroup.mies.webdev.Model.Pod;
import miesgroup.mies.webdev.Rest.Model.PodResponse;
import miesgroup.mies.webdev.Rest.Model.UpdatePodRequest;
import miesgroup.mies.webdev.Service.PodService;

import java.util.List;
import java.util.Map;

@Path("/pod")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PodResource {

    private final PodService podService;

    public PodResource(PodService podService) {
        this.podService = podService;
    }

    // 1) Tutti i POD (con proxy su queryparam)
    @GET
    public List<PodResponse> allPod(
            @CookieParam("SESSION_COOKIE") Integer sessionId
    ) {
        return podService.tutti(sessionId);
    }

    @GET
    @Path("/dati")
    public List<PodResponse> allPodProxy(
            @QueryParam("session_id") Integer sessionId
    ) {
        return podService.tutti(sessionId);
    }

    // 2) Dettaglio singolo POD
    @GET
    @Path("/{id}")
    public Pod getPod(
            @PathParam("id") String id,
            @CookieParam("SESSION_COOKIE") int idSessione
    ) {
        return podService.getPod(id, idSessione);
    }

    // 3) Aggiungi / modifica sede e nazione
    @PUT
    @Path("/sedeNazione")
    public void updatePod(
            UpdatePodRequest request,
            @CookieParam("SESSION_COOKIE") int idSessione
    ) {
        podService.addSedeNazione(
                request.getIdPod(),
                request.getSede(),
                request.getNazione(),
                idSessione
        );
    }

    @PUT
    @Path("/modifica-sede-nazione")
    public void updatePodSedeNazione(
            UpdatePodRequest request,
            @CookieParam("SESSION_COOKIE") int idSessione
    ) {
        podService.modificaSedeNazione(
                request.getIdPod(),
                request.getSede(),
                request.getNazione(),
                idSessione
        );
    }

    // 4) Spread
    @PUT
    @Path("/spread")
    public Response updateSpread(
            UpdatePodRequest request,
            @CookieParam("SESSION_COOKIE") int idSessione
    ) {
        podService.addSpread(
                request.getIdPod(),
                request.getSpread(),
                idSessione
        );
        return Response.ok().build();
    }

    // 5) Bollette PDF
    @GET
    @Path("/bollette")
    public List<PDFFile> getBollette(
            @CookieParam("SESSION_COOKIE") int idSessione
    ) {
        List<Pod> elencoPod = podService.findPodByIdUser(idSessione);
        return podService.getBollette(elencoPod);
    }

}