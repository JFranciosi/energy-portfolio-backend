package miesgroup.mies.webdev.Rest.bolletta;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Rest.Model.CostoArticoloResponse;
import miesgroup.mies.webdev.Service.bolletta.CostoArticoloService;
import org.hibernate.SessionException;

import java.util.List;

@Path("/costo-articolo")
public class CostoArticoloResource {

    private final CostoArticoloService costoArticoloService;

    public CostoArticoloResource(CostoArticoloService costoArticoloService) {
        this.costoArticoloService = costoArticoloService;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response articoli(@QueryParam("session_id") Integer idSessione) {
        try {
            // Recupera i costi articoli dal servizio
            List<CostoArticoloResponse> costiArticoli = costoArticoloService.getCostoArticoli(idSessione);

            // Se tutto ok, restituiamo 200 (OK) con la lista in JSON
            return Response.ok(costiArticoli).build();

        } catch (SessionException e) {
            // Sessione non valida: restituiamo 401 (UNAUTHORIZED) o 403 (FORBIDDEN) a seconda della logica
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();

        } catch (NotFoundException e) {
            // Risorsa non trovata: restituiamo 404 (NOT_FOUND)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();

        } catch (Exception e) {
            // Altri errori imprevisti: restituiamo 500 (INTERNAL_SERVER_ERROR)
            System.out.println("Errore: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore interno del server.")
                    .build();
        }
    }

}
