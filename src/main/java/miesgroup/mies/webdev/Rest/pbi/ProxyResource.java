package miesgroup.mies.webdev.Rest.pbi;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import miesgroup.mies.webdev.Model.cliente.Sessione;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Service.pbi.PowerBIService;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Path("/proxy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProxyResource {
    // Costanti per i dataset e le tabelle
    private static final String DATASET_ID_CONTROLLO = "5ee09b8c-9750-4fee-9050-eb4f59de94f5";
    private static final String ARTICOLI = "dettaglio_articolo";
    private static final String BOLLETTE = "bolletta_pod";
    private static final String POD = "pod_info";
    private static final String CALENDARIO = "calendario";

    // URL di base per le API
    private static final String BASE_URL_PROD = "https://energyportfolio.it";
    private static final String BASE_URL_DEV = "http://localhost:8081";
    private static final String API_PORT_PROD = ":8081";

    // Flag per ambiente di sviluppo/produzione
    private static final boolean IS_DEV_ENV = true; // Cambia a false per produzione

    private final SessionRepo sessionRepo;
    private final PowerBIService powerBIService;

    public ProxyResource(SessionRepo sessionRepo, PowerBIService powerBIService) {
        this.sessionRepo = sessionRepo;
        this.powerBIService = powerBIService;
    }

    /**
     * Valida il cookie di sessione e restituisce l'ID sessione.
     */
    private String validateSessionCookie(Integer sessionCookie) {
        if (sessionCookie == null) {
            return null;
        }

        return sessionRepo.getSessionById(sessionCookie)
                .map(Sessione::getId)
                .map(String::valueOf)
                .orElse(null);
    }

    @GET
    @Path("/articoli")
    @Produces(MediaType.APPLICATION_JSON)
    public Response inviaArticoliAPowerBI(@CookieParam("SESSION_COOKIE") Integer sessionCookie) {
        try {
            // Valida la sessione
            String sessionId = validateSessionCookie(sessionCookie);

            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }

            System.out.println("cookie: " + sessionId);

            // Determina l'URL corretto in base all'ambiente
            String baseUrl = IS_DEV_ENV ? BASE_URL_DEV : BASE_URL_PROD;
            String targetUrl = baseUrl + "/costo-articolo?session_id=" + sessionId;

            // Prepara gli header
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "MiesApp/1.0");
            headers.put("X-Session-Id", sessionId);

            // Esegui la richiesta
            HttpResponse<String> response = powerBIService.getExternalData(targetUrl, headers);

            if (response.statusCode() != 200) {
                System.out.println("Response Code: " + response.statusCode());
                System.out.println("Response Body: " + response.body());
                return Response.status(response.statusCode())
                        .entity("{\"error\":\"Errore dal server dati\"}")
                        .build();
            }

            // Riceviamo i dati come JSON
            String rawJson = response.body();
            System.out.println("Dati ricevuti da energyportfolio: " + rawJson);

            // Convertilo nel formato richiesto da Power BI e aggiorna la tabella
            String powerBIJson = powerBIService.wrapArticoliForPowerBI(rawJson);
            return powerBIService.aggiornaTabella(DATASET_ID_CONTROLLO, ARTICOLI, powerBIJson);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore generale: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/pod")
    @Produces(MediaType.APPLICATION_JSON)
    public Response inviaPodAPowerBI(@CookieParam("SESSION_COOKIE") Integer sessionCookie) {
        try {
            // Valida la sessione
            String sessionId = validateSessionCookie(sessionCookie);

            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }

            System.out.println("cookie: " + sessionId);

            // Determina l'URL corretto in base all'ambiente
            String baseUrl = IS_DEV_ENV ? BASE_URL_DEV : (BASE_URL_PROD + API_PORT_PROD);
            String targetUrl = baseUrl + "/pod/dati?session_id=" + sessionId;

            // Prepara gli header
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "MiesApp/1.0");
            headers.put("X-Session-Id", sessionId);

            // Esegui la richiesta
            HttpResponse<String> response = powerBIService.getExternalData(targetUrl, headers);

            if (response.statusCode() != 200) {
                System.out.println("Response Code: " + response.statusCode());
                System.out.println("Response Body: " + response.body());
                return Response.status(response.statusCode())
                        .entity("{\"error\":\"Errore dal server dati\"}")
                        .build();
            }

            // Riceviamo i dati come JSON
            String rawJson = response.body();
            System.out.println("Dati ricevuti da energyportfolio: " + rawJson);

            // Convertilo nel formato richiesto da Power BI e aggiorna la tabella
            String powerBIJson = powerBIService.wrapPodForPowerBI(rawJson);
            return powerBIService.aggiornaTabella(DATASET_ID_CONTROLLO, POD, powerBIJson);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore generale: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/bollette")
    @Produces(MediaType.APPLICATION_JSON)
    public Response inviaBolletteAPowerBI(@CookieParam("SESSION_COOKIE") Integer sessionCookie) {
        try {
            // Valida la sessione
            String sessionId = validateSessionCookie(sessionCookie);

            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }

            System.out.println("cookie: " + sessionId);

            // Determina l'URL corretto in base all'ambiente
            String baseUrl = IS_DEV_ENV ? BASE_URL_DEV : (BASE_URL_PROD + API_PORT_PROD);
            String targetUrl = baseUrl + "/files/dati?session_id=" + sessionId;

            // Prepara gli header
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "MiesApp/1.0");
            headers.put("X-Session-Id", sessionId);

            // Esegui la richiesta
            HttpResponse<String> response = powerBIService.getExternalData(targetUrl, headers);

            if (response.statusCode() != 200) {
                System.out.println("Response Code: " + response.statusCode());
                System.out.println("Response Body: " + response.body());
                return Response.status(response.statusCode())
                        .entity("{\"error\":\"Errore dal server dati\"}")
                        .build();
            }

            // Riceviamo i dati come JSON
            String rawJson = response.body();
            System.out.println("Dati ricevuti da energyportfolio: " + rawJson);

            // Convertilo nel formato richiesto da Power BI e aggiorna la tabella
            String powerBIJson = powerBIService.wrapBolletteForPowerBI(rawJson);
            return powerBIService.aggiornaTabella(DATASET_ID_CONTROLLO, BOLLETTE, powerBIJson);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore generale: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}