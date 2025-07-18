package miesgroup.mies.webdev.Rest.pbi;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Service.pbi.AzureADService;
import miesgroup.mies.webdev.Service.pbi.PowerBIService;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Path("/api/pbitoken")
public class PBIApiResource {
    private static final Logger LOG = Logger.getLogger(PBIApiResource.class);

    @Inject
    AzureADService azureADService;
    @Inject
    PowerBIService powerBIService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPBIAccessToken() {
        try {
            String accessToken = azureADService.getPowerBIAccessToken();
            System.out.println("Access Token: " + accessToken);
            return Response.ok(Map.of("token", accessToken)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get Power BI access token", "details", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/embed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPBIEmbedInfo(@QueryParam("reportId") String reportId) {
        LOG.info("Getting embed info for report: " + reportId);

        if (reportId == null || reportId.isEmpty()) {
            LOG.warn("Report ID is missing in the request");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Report ID is required"))
                    .build();
        }

        try {
            // 1. Ottieni il token di accesso da Azure AD
            LOG.info("Getting Power BI access token...");
            String accessToken = azureADService.getPowerBIAccessToken();
            System.out.println("Access Token: " + accessToken);
            LOG.info("Power BI access token obtained successfully");

            // 2. Ottieni le informazioni di embed utilizzando il token di accesso
            LOG.info("Getting embed info using access token...");
            Map<String, Object> embedInfo = powerBIService.getEmbedInfo(reportId, accessToken);
            LOG.info("Embed info obtained successfully");

            return Response.ok(embedInfo).build();
        } catch (Exception e) {
            LOG.error("Failed to get Power BI embed info", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get Power BI embed info");
            errorResponse.put("details", e.getMessage());
            errorResponse.put("reportId", reportId);

            // Aggiungi la traccia dello stack per il debug
            StringBuffer stackTrace = new StringBuffer();
            for (StackTraceElement element : e.getStackTrace()) {
                stackTrace.append(element.toString()).append("\n");
            }
            errorResponse.put("stackTrace", stackTrace.toString());

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponse)
                    .build();
        }
    }
}
