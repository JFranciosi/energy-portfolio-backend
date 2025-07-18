package miesgroup.mies.webdev.Service.pbi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// Scegliamo solo le importazioni di RESTEasy Classic
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// JSON-P per la manipolazione di JSON
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

// Logging
import org.jboss.logging.Logger;

// Utility
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// Config
import org.eclipse.microprofile.config.inject.ConfigProperty;



@ApplicationScoped
public class PowerBIService {

    private static final String BASE_URL = "https://api.powerbi.com/v1.0/myorg/groups/d62409c0-b987-4280-b892-67d8a24f9755/datasets/";

    @Inject
    private AzureADService azureADService;

    // =============================================
    // METODI PER L'INTERAZIONE CON POWER BI
    // =============================================

    /**
     * Elimina tutte le righe da una tabella in PowerBI
     */
    public boolean eliminazioneRighe(String datasetId, String tableName) throws Exception {
        String token = azureADService.getPowerBIAccessToken();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(100))
                .build();

        String deleteRowsUrl = BASE_URL + datasetId + "/tables/" + tableName + "/rows";

        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(deleteRowsUrl))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(300))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        if (deleteResponse.statusCode() == 200) {
            System.out.println("✅ Righe esistenti cancellate con successo da " + tableName);
            return true;
        } else {
            System.err.println("❌ Errore nella cancellazione delle righe: " + deleteResponse.statusCode());
            System.err.println("Dettagli: " + deleteResponse.body());
            return false;
        }
    }

    /**
     * Invia dati a una tabella in PowerBI
     */
    public Response invioDati(String datasetId, String tableName, String jsonData) {
        try {
            String token = azureADService.getPowerBIAccessToken();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(100))
                    .build();

            String powerBIUrl = BASE_URL + datasetId + "/tables/" + tableName + "/rows";

            HttpRequest powerBIRequest = HttpRequest.newBuilder()
                    .uri(URI.create(powerBIUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(300))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();

            HttpResponse<String> powerBIResponse = client.send(powerBIRequest, HttpResponse.BodyHandlers.ofString());

            if (powerBIResponse.statusCode() == 200) {
                System.out.println("✅ Dati inviati con successo a Power BI: " + tableName);
                return Response.ok("{\"status\":\"Dati inviati con successo a Power BI\"}").build();
            } else {
                System.err.println("Errore PowerBI Status: " + powerBIResponse.statusCode());
                System.err.println("Errore PowerBI Body: " + powerBIResponse.body());

                return Response.status(powerBIResponse.statusCode())
                        .entity("{\"error\":\"Errore durante l'invio a Power BI: " + powerBIResponse.body() + "\"}")
                        .build();
            }
        } catch (Exception e) {
            System.err.println("Errore durante l'invio a Power BI: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore durante l'invio a Power BI: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Aggiorna completamente i dati di una tabella (cancella e reinserisce)
     */
    public Response aggiornaTabella(String datasetId, String tableName, String jsonData) {
        try {
            // Prima cancella le righe esistenti
            eliminazioneRighe(datasetId, tableName);
            // Poi inserisci i nuovi dati
            return invioDati(datasetId, tableName, jsonData);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore durante l'aggiornamento della tabella: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // =============================================
    // METODI PER L'INTERAZIONE CON API ESTERNE
    // =============================================

    /**
     * Esegue una chiamata GET per ottenere dati da un'API esterna
     */
    public HttpResponse<String> getExternalData(String url, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(100))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(300))
                .GET();

        // Aggiungi gli header specificati
        headers.forEach((key, value) -> requestBuilder.header(key, value));

        HttpRequest request = requestBuilder.build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }


    // =============================================
    // METODI DI UTILITY E CONVERSIONE DATI
    // =============================================

    /**
     * Formatta i dati JSON degli articoli nel formato richiesto da PowerBI.
     */
    public String wrapArticoliForPowerBI(String rawJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Presumo che rawJson sia un array di oggetti
        JsonNode articoliArray = mapper.readTree(rawJson);

        ObjectNode root = mapper.createObjectNode();
        root.set("rows", articoliArray);

        return mapper.writeValueAsString(root);
    }

    /**
     * Formatta i dati JSON dei POD nel formato richiesto da PowerBI.
     */
    public String wrapPodForPowerBI(String rawJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Presumo che rawJson sia un array di oggetti
        JsonNode podArray = mapper.readTree(rawJson);

        ObjectNode root = mapper.createObjectNode();
        root.set("rows", podArray);

        return mapper.writeValueAsString(root);
    }

    /**
     * Formatta i dati JSON delle bollette nel formato richiesto da PowerBI.
     */
    public String wrapBolletteForPowerBI(String rawJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Presumo che rawJson sia un array di oggetti
        JsonNode bolletteArray = mapper.readTree(rawJson);

        ObjectNode root = mapper.createObjectNode();
        root.set("rows", bolletteArray);

        return mapper.writeValueAsString(root);
    }


    private static final Logger LOG = Logger.getLogger(PowerBIService.class);

    @ConfigProperty(name = "powerbi.workspace.id", defaultValue = "d62409c0-b987-4280-b892-67d8a24f9755")
    String workspaceId;

    @ConfigProperty(name = "powerbi.api.base.url", defaultValue = "https://api.powerbi.com/v1.0/myorg")
    String powerBIApiBaseUrl;

    /**
     * Ottiene le informazioni di embed (URL e token) per un report Power BI
     * @param reportId ID del report
     * @param accessToken Token di accesso Azure AD
     * @return Mappa con token di embed e URL di embed
     */
    public Map<String, Object> getEmbedInfo(String reportId, String accessToken) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Ottieni l'URL di embed direttamente dal report nel workspace
            String embedUrl = getReportEmbedUrlFromWorkspace(reportId, accessToken);
            result.put("embedUrl", embedUrl);

            // 2. Genera un token di embed per il report
            String embedToken = generateEmbedToken(reportId, accessToken);
            result.put("token", embedToken);

            return result;
        } catch (Exception e) {
            LOG.error("Error getting embed info", e);
            throw e;
        }
    }

    /**
     * Ottiene l'URL di embed dal report all'interno del workspace
     */
    private String getReportEmbedUrlFromWorkspace(String reportId, String accessToken) throws Exception {
        Client client = ClientBuilder.newClient();
        // Usa l'endpoint specifico che include il groupId (workspaceId)
        String endpoint = powerBIApiBaseUrl + "/groups/" + workspaceId + "/reports/" + reportId;

        LOG.info("Calling Power BI API: " + endpoint);

        Response response = client.target(endpoint)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .get();

        if (response.getStatus() != 200) {
            String errorResponse = response.readEntity(String.class);
            LOG.error("Failed to get report info. Status: " + response.getStatus() +
                    ", Response: " + errorResponse);
            throw new Exception("Failed to get report info. Status: " + response.getStatus() +
                    ", Response: " + errorResponse);
        }

        String jsonString = response.readEntity(String.class);
        LOG.info("Report info response received");

        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = jsonReader.readObject();
            String embedUrl = jsonObject.getString("embedUrl");
            LOG.info("Embed URL obtained: " + embedUrl.substring(0, Math.min(embedUrl.length(), 50)) + "...");
            return embedUrl;
        }
    }

    /**
     * Genera un token di embed per un report
     */
    private String generateEmbedToken(String reportId, String accessToken) throws Exception {
        Client client = ClientBuilder.newClient();
        // Usa l'endpoint specifico che include il groupId (workspaceId)
        String endpoint = powerBIApiBaseUrl + "/groups/" + workspaceId + "/reports/" + reportId + "/GenerateToken";

        LOG.info("Generating embed token: " + endpoint);

        // Payload per generare un token di sola visualizzazione
        JsonObject payload = Json.createObjectBuilder()
                .add("accessLevel", "View")
                .build();

        Response response = client.target(endpoint)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .post(Entity.entity(payload.toString(), MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            String errorResponse = response.readEntity(String.class);
            LOG.error("Failed to generate embed token. Status: " + response.getStatus() +
                    ", Response: " + errorResponse);
            throw new Exception("Failed to generate embed token. Status: " + response.getStatus() +
                    ", Response: " + errorResponse);
        }

        String jsonString = response.readEntity(String.class);
        LOG.info("Generate token response received");

        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
            JsonObject jsonObject = jsonReader.readObject();
            String token = jsonObject.getString("token");
            LOG.info("Token generated successfully");
            return token;
        }
    }
}