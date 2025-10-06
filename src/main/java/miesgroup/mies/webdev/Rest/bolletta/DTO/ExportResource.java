package miesgroup.mies.webdev.Rest.bolletta.DTO;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import miesgroup.mies.webdev.Rest.Model.ExportRequest;
import miesgroup.mies.webdev.Service.file.ExportService;

@Path("/api/export")
@Consumes(MediaType.APPLICATION_JSON)
public class ExportResource {

    @Inject
    ExportService exportService;

    @POST
    @Path("/preview")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response preview(ExportRequest req) {
        try {
            // CHIAMATA DI ISTANZA (non statica)
            return Response.ok(exportService.getPreview(req)).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.BAD_REQUEST).entity(iae.getMessage()).build();
        } catch (Exception e) {
            return Response.serverError().entity("Errore preview: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/excel")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Transactional
    public Response excel(ExportRequest req) {
        try {
            byte[] bin = exportService.exportExcel(req);
            return Response.ok(bin)
                    .header("Content-Disposition", "attachment; filename=\"export_bollette.xlsx\"")
                    .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                    .build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.BAD_REQUEST).entity(iae.getMessage()).build();
        } catch (Exception e) {
            return Response.serverError().entity("Errore export: " + e.getMessage()).build();
        }
    }
}
