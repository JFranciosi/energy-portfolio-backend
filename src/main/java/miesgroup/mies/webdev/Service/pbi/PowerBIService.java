package miesgroup.mies.webdev.Service.pbi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.util.*;

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
            System.err.println("Errore nella cancellazione delle righe: " + deleteResponse.statusCode());
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
                System.err.println("❌ Errore PowerBI Status: " + powerBIResponse.statusCode());
                System.err.println("❌ Errore PowerBI Body: " + powerBIResponse.body());
                return Response.status(powerBIResponse.statusCode())
                        .entity("{\"error\":\"Errore durante l'invio a Power BI: " + powerBIResponse.body() + "\"}")
                        .build();
            }

        } catch (Exception e) {
            System.err.println("❌ Errore durante l'invio a Power BI: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore durante l'invio a Power BI: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Aggiorna completamente i dati di una tabella (cancella e reinserisce)
     */
    public Response aggiornaTabellaCompleta(String datasetId, String tableName, String jsonData) {
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

    /**
     * Aggiorna completamente un dataset con la struttura completa (schema + dati)
     * Utilizzato per aggiornare dataset con multiple tabelle
     */
    public Response aggiornaDataset(String datasetId, String datasetJson) {
        try {
            String token = azureADService.getPowerBIAccessToken();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(100))
                    .build();

            // Endpoint per aggiornare l'intero dataset
            String powerBIUrl = BASE_URL + "/" + datasetId;

            HttpRequest powerBIRequest = HttpRequest.newBuilder()
                    .uri(URI.create(powerBIUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(300))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(datasetJson))
                    .build();

            HttpResponse<String> powerBIResponse = client.send(powerBIRequest, HttpResponse.BodyHandlers.ofString());

            if (powerBIResponse.statusCode() == 200) {
                System.out.println("Dataset aggiornato con successo: " + datasetId);
                return Response.ok("Dataset aggiornato con successo").build();
            } else {
                System.err.println("Errore PowerBI Status: " + powerBIResponse.statusCode());
                System.err.println("Errore PowerBI Body: " + powerBIResponse.body());
                return Response.status(powerBIResponse.statusCode())
                        .entity("{\"error\":\"Errore durante l'aggiornamento del dataset: " + powerBIResponse.body() + "\"}")
                        .build();
            }

        } catch (Exception e) {
            System.err.println("Errore durante l'aggiornamento del dataset: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore durante l'aggiornamento del dataset: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * CANCELLA tutte le righe da TUTTE le tabelle del dataset
     */
    public Response cancellaDatasetCompleto(String datasetId) {
        try {
            // Cancella da tutte e 3 le tabelle con i nomi corretti
            eliminazioneRighe(datasetId, "budget");
            eliminazioneRighe(datasetId, "calendario");
            eliminazioneRighe(datasetId, "bolletta_pod");

            return Response.ok("{\"status\":\"Dataset cancellato completamente\"}").build();
        } catch (Exception e) {
            System.err.println("Errore cancellazione dataset: " + e.getMessage());
            return Response.status(500).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * INVIA dati a tutte le tabelle del dataset (dopo averle cancellate)
     */
    public Response inviaDatasetCompleto(String datasetId, String budgetRows, String calendarioRows, String bollettaPodRows) {
        try {
            System.out.println("===== INIZIO INVIO DATASET COMPLETO =====");

            // 1. Cancella tutto prima
            cancellaDatasetCompleto(datasetId);

            // 2. Invia Budget
            System.out.println("📤 Inviando dati Budget...");
            Response budgetResponse = invioDati(datasetId, "budget", budgetRows);
            System.out.println("Budget response status: " + budgetResponse.getStatus());

            // 3. Invia Calendario
            System.out.println("📤 Inviando dati Calendario...");
            Response calendarioResponse = invioDati(datasetId, "calendario", calendarioRows);
            System.out.println("Calendario response status: " + calendarioResponse.getStatus());

            // 4. Invia Bolletta_pod
            System.out.println("📤 Inviando dati Bolletta_pod...");
            Response bollettaResponse = invioDati(datasetId, "bolletta_pod", bollettaPodRows);
            System.out.println("Bolletta_pod response status: " + bollettaResponse.getStatus());

            System.out.println("===== DATASET COMPLETO INVIATO =====");

            return Response.ok("{\"status\":\"Dataset completo aggiornato con successo\"}").build();

        } catch (Exception e) {
            System.err.println("Errore invio dataset completo: " + e.getMessage());
            e.printStackTrace();
            return Response.status(500).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    public <T> String wrapDataForPowerBI(List<T> dataList) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Crea il wrapper con le rows
        ObjectNode wrapper = mapper.createObjectNode();
        ArrayNode rows = mapper.valueToTree(dataList);

        // Normalizza i campi in tutte le righe
        for (JsonNode row : rows) {
            if (row.isObject()) {
                ObjectNode obj = (ObjectNode) row;

                // 1. Normalizza i nomi dei campi
                normalizeFieldNames(obj);

                // 2. Normalizza i tipi di dato
                normalizeDataTypes(obj);
            }
        }

        wrapper.set("rows", rows);
        String result = mapper.writeValueAsString(wrapper);

        // Log di debug
        System.out.println("🔍 JSON normalizzato (primi 800 caratteri): " +
                result.substring(0, Math.min(800, result.length())));

        return result;
    }

    private void normalizeFieldNames(ObjectNode obj) {
        List<Map.Entry<String, JsonNode>> updates = new ArrayList<>();

        // Mappa completa dei nomi da normalizzare secondo lo schema Power BI
        Map<String, String> fieldMappings = new HashMap<>();

        // BudgetBollettaDTO fields
        fieldMappings.put("idBolletta", "IdBolletta");
        fieldMappings.put("idbolletta", "IdBolletta");
        fieldMappings.put("idpod", "idpod");
        fieldMappings.put("idPod", "idpod");
        fieldMappings.put("IDPOD", "idpod");
        fieldMappings.put("nomeBolletta", "NomeBolletta");
        fieldMappings.put("nomebolletta", "NomeBolletta");
        fieldMappings.put("totAttiva", "TOTAttiva");
        fieldMappings.put("totattiva", "TOTAttiva");
        fieldMappings.put("TOTattiva", "TOTAttiva");
        fieldMappings.put("budgetEnergia", "BudgetEnergia");
        fieldMappings.put("budgetenergia", "BudgetEnergia");
        fieldMappings.put("budgetTrasporto", "BudgetTrasporto");
        fieldMappings.put("budgettrasporto", "BudgetTrasporto");
        fieldMappings.put("budgetOneri", "BudgetOneri");
        fieldMappings.put("budgetoneri", "BudgetOneri");
        fieldMappings.put("budgetImposte", "BudgetImposte");
        fieldMappings.put("budgetimposte", "BudgetImposte");
        fieldMappings.put("budgetTotale", "BudgetTotale");
        fieldMappings.put("budgettotale", "BudgetTotale");
        fieldMappings.put("budgetPenali", "BudgetPenali");
        fieldMappings.put("budgetpenali", "BudgetPenali");
        fieldMappings.put("budgetAltro", "BudgetAltro");
        fieldMappings.put("budgetaltro", "BudgetAltro");

        // CalendarioDTO fields
        fieldMappings.put("anno", "Anno");
        fieldMappings.put("ANNO", "Anno");
        fieldMappings.put("meseNumero", "Mese Numero");
        fieldMappings.put("mesenumero", "Mese Numero");
        fieldMappings.put("MeseNumero", "Mese Numero");
        fieldMappings.put("mesNumero", "Mese Numero");
        fieldMappings.put("meseNome", "Mese Nome");
        fieldMappings.put("mesenome", "Mese Nome");
        fieldMappings.put("MeseNome", "Mese Nome");
        fieldMappings.put("meseAbbreviato", "Mese Abbreviato");
        fieldMappings.put("meseabbreviato", "Mese Abbreviato");
        fieldMappings.put("MeseAbbreviato", "Mese Abbreviato");
        fieldMappings.put("trimestre", "Trimestre");
        fieldMappings.put("TRIMESTRE", "Trimestre");
        fieldMappings.put("periodo", "Periodo");
        fieldMappings.put("PERIODO", "Periodo");
        fieldMappings.put("periodoTrimestre", "PeriodoTrimestre");
        fieldMappings.put("periodotrimestre", "PeriodoTrimestre");
        fieldMappings.put("PeriodoTrimestre", "PeriodoTrimestre");

        // Anno-Mese (campo comune a tutti i DTO)
        fieldMappings.put("annoMese", "Anno-Mese");
        fieldMappings.put("annomese", "Anno-Mese");
        fieldMappings.put("AnnoMese", "Anno-Mese");
        fieldMappings.put("anno-mese", "Anno-Mese");
        fieldMappings.put("ANNO-MESE", "Anno-Mese");

        // BollettaPodDTO fields
        fieldMappings.put("f1Attiva", "F1Attiva");
        fieldMappings.put("f1attiva", "F1Attiva");
        fieldMappings.put("F1attiva", "F1Attiva");
        fieldMappings.put("f2Attiva", "F2Attiva");
        fieldMappings.put("f2attiva", "F2Attiva");
        fieldMappings.put("F2attiva", "F2Attiva");
        fieldMappings.put("f3Attiva", "F3Attiva");
        fieldMappings.put("f3attiva", "F3Attiva");
        fieldMappings.put("F3attiva", "F3Attiva");
        fieldMappings.put("f1Reattiva", "F1Reattiva");
        fieldMappings.put("f1reattiva", "F1Reattiva");
        fieldMappings.put("F1reattiva", "F1Reattiva");
        fieldMappings.put("f2Reattiva", "F2Reattiva");
        fieldMappings.put("f2reattiva", "F2Reattiva");
        fieldMappings.put("F2reattiva", "F2Reattiva");
        fieldMappings.put("f3Reattiva", "F3Reattiva");
        fieldMappings.put("f3reattiva", "F3Reattiva");
        fieldMappings.put("F3reattiva", "F3Reattiva");
        fieldMappings.put("f1Potenza", "F1Potenza");
        fieldMappings.put("f1potenza", "F1Potenza");
        fieldMappings.put("F1potenza", "F1Potenza");
        fieldMappings.put("f2Potenza", "F2Potenza");
        fieldMappings.put("f2potenza", "F2Potenza");
        fieldMappings.put("F2potenza", "F2Potenza");
        fieldMappings.put("f3Potenza", "F3Potenza");
        fieldMappings.put("f3potenza", "F3Potenza");
        fieldMappings.put("F3potenza", "F3Potenza");
        fieldMappings.put("speseEnergia", "SpeseEnergia");
        fieldMappings.put("speseenergia", "SpeseEnergia");
        fieldMappings.put("SpeseEnergia", "SpeseEnergia");
        fieldMappings.put("speseTrasporto", "SpeseTrasporto");
        fieldMappings.put("spesetrasporto", "SpeseTrasporto");
        fieldMappings.put("SpeseTrasporto", "SpeseTrasporto");
        fieldMappings.put("oneri", "Oneri");
        fieldMappings.put("ONERI", "Oneri");
        fieldMappings.put("imposte", "Imposte");
        fieldMappings.put("IMPOSTE", "Imposte");
        fieldMappings.put("periodoInizio", "PeriodoInizio");
        fieldMappings.put("periodoinizio", "PeriodoInizio");
        fieldMappings.put("PeriodoInizio", "PeriodoInizio");
        fieldMappings.put("periodoFine", "PeriodoFine");
        fieldMappings.put("periodofine", "PeriodoFine");
        fieldMappings.put("PeriodoFine", "PeriodoFine");
        fieldMappings.put("mese", "Mese");
        fieldMappings.put("MESE", "Mese");
        fieldMappings.put("totReattiva", "TOTReattiva");
        fieldMappings.put("totreattiva", "TOTReattiva");
        fieldMappings.put("TOTreattiva", "TOTReattiva");
        fieldMappings.put("TOTReattiva", "TOTReattiva");
        fieldMappings.put("generation", "Generation");
        fieldMappings.put("GENERATION", "Generation");
        fieldMappings.put("dispacciamento", "Dispacciamento");
        fieldMappings.put("Dispacciamento", "Dispacciamento");
        fieldMappings.put("dispackciamento", "Dispacciamento");
        fieldMappings.put("verificaTrasporti", "VerificaTrasporti");
        fieldMappings.put("verificatrasporti", "VerificaTrasporti");
        fieldMappings.put("VerificaTrasporti", "VerificaTrasporti");
        fieldMappings.put("penali33", "Penali33");
        fieldMappings.put("Penali33", "Penali33");
        fieldMappings.put("penali75", "Penali75");
        fieldMappings.put("Penali75", "Penali75");
        fieldMappings.put("verificaOneri", "VerificaOneri");
        fieldMappings.put("verificaoneri", "VerificaOneri");
        fieldMappings.put("VerificaOneri", "VerificaOneri");
        fieldMappings.put("verificaImposte", "VerificaImposte");
        fieldMappings.put("verificaimposte", "VerificaImposte");
        fieldMappings.put("VerificaImposte", "VerificaImposte");
        fieldMappings.put("altro", "Altro");
        fieldMappings.put("ALTRO", "Altro");
        fieldMappings.put("piccokwh", "piccokwh");
        fieldMappings.put("piccoKwh", "piccokwh");
        fieldMappings.put("PiccoKwh", "piccokwh");
        fieldMappings.put("fuoripiccokwh", "fuoripiccokwh");
        fieldMappings.put("fuoripiccoKwh", "fuoripiccokwh");
        fieldMappings.put("FuoripiccoKwh", "fuoripiccokwh");
        fieldMappings.put("picco", "picco");
        fieldMappings.put("Picco", "picco");
        fieldMappings.put("fuoripicco", "fuoripicco");
        fieldMappings.put("Fuoripicco", "fuoripicco");
        fieldMappings.put("verificapicco", "verificapicco");
        fieldMappings.put("verificaPicco", "verificapicco");
        fieldMappings.put("VerificaPicco", "verificapicco");
        fieldMappings.put("verificafuoripicco", "verificafuoripicco");
        fieldMappings.put("verificaFuoripicco", "verificafuoripicco");
        fieldMappings.put("VerificaFuoripicco", "verificafuoripicco");

        // Scansiona tutti i campi della riga
        obj.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            if (fieldMappings.containsKey(fieldName)) {
                String correctFieldName = fieldMappings.get(fieldName);
                if (!fieldName.equals(correctFieldName)) {
                    updates.add(new AbstractMap.SimpleEntry<>(fieldName, entry.getValue()));
                }
            }
        });

        // Esegui le rinominate
        for (Map.Entry<String, JsonNode> update : updates) {
            String oldName = update.getKey();
            String newName = fieldMappings.get(oldName);
            JsonNode value = obj.get(oldName);
            obj.remove(oldName);
            obj.set(newName, value);
        }
    }

    private void normalizeDataTypes(ObjectNode obj) {
        // Definizione dei campi secondo lo schema Power BI

        // ===== CAMPI DOUBLE (double in Power BI) =====
        Set<String> doubleFields = Set.of(
                // Bolletta_pod - tutti i campi numerici sono double
                "Id_Bolletta", "Nome_Bolletta",
                "F1_Attiva", "F2_Attiva", "F3_Attiva",
                "F1_Reattiva", "F2_Reattiva", "F3_Reattiva",
                "F1_Potenza", "F2_Potenza", "F3_Potenza",
                "Spese_Energia", "Spese_Trasporto", "Oneri", "Imposte",
                "TOT_Attiva", "TOT_Reattiva",
                "Generation", "Dispacciamento",
                "Verifica_Trasporti", "Penali33", "Penali75",
                "Verifica_Oneri", "Verifica_Imposte",
                "Anno", "picco_kwh", "fuori_picco_kwh",
                "€_picco", "€_fuori_picco", "verifica_picco", "verifica_fuori_picco"
        );

        // ===== CAMPI INT64 (int64 in Power BI) =====
        Set<String> int64Fields = Set.of(
                // Calendario
                "Anno", "Mese Numero",
                // Budget
                "Id_Bolletta", "Nome_Bolletta", "TOT_Attiva",
                "Budget_Energia", "Budget_Trasporto", "Budget_Oneri",
                "Budget_Imposte", "Budget_Totale", "Budget_Penali", "Budget_Altro",
                // Bolletta_pod (solo Altro è int64)
                "Altro"
        );

        // ===== CAMPI STRING =====
        Set<String> stringFields = Set.of(
                "id_pod", "Mese Nome", "Anno-Mese", "Mese Abbreviato",
                "Trimestre", "Periodo", "PeriodoTrimestre",
                "Mese", "Periodo_Inizio", "Periodo_Fine"
        );

        // ===== CAMPI DATETIME (dateTime in Power BI) =====
        Set<String> dateTimeFields = Set.of(
                "Periodo_Inizio", "Periodo_Fine"
        );

        // Itera sui campi dell'oggetto
        Iterator<String> fieldNames = obj.fieldNames();
        List<Map.Entry<String, Object>> conversions = new ArrayList<>();

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode value = obj.get(fieldName);

            // ===== CONVERTI CAMPI INT64 =====
            if (int64Fields.contains(fieldName)) {
                if (value.isNull()) {
                    conversions.add(new AbstractMap.SimpleEntry<>(fieldName, null));
                } else if (value.isTextual()) {
                    String textValue = value.asText().trim();
                    if (textValue.isEmpty() || textValue.equalsIgnoreCase("null")) {
                        conversions.add(new AbstractMap.SimpleEntry<>(fieldName, null));
                    } else {
                        try {
                            Long numValue = Long.parseLong(textValue);
                            conversions.add(new AbstractMap.SimpleEntry<>(fieldName, numValue));
                        } catch (NumberFormatException e) {
                            System.err.println("⚠️ Impossibile convertire '" + textValue +
                                    "' in int64 per campo '" + fieldName + "'. Impostato a null.");
                            conversions.add(new AbstractMap.SimpleEntry<>(fieldName, null));
                        }
                    }
                } else if (value.isNumber() && !value.isIntegralNumber()) {
                    // Converti double in int64 se necessario
                    long numValue = value.asLong();
                    conversions.add(new AbstractMap.SimpleEntry<>(fieldName, numValue));
                }
            }

            // ===== CONVERTI CAMPI DOUBLE =====
            else if (doubleFields.contains(fieldName)) {
                if (value.isNull()) {
                    conversions.add(new AbstractMap.SimpleEntry<>(fieldName, null));
                } else if (value.isTextual()) {
                    String textValue = value.asText().trim();
                    if (textValue.isEmpty() || textValue.equalsIgnoreCase("null")) {
                        conversions.add(new AbstractMap.SimpleEntry<>(fieldName, null));
                    } else {
                        try {
                            Double numValue = Double.parseDouble(textValue);
                            conversions.add(new AbstractMap.SimpleEntry<>(fieldName, numValue));
                        } catch (NumberFormatException e) {
                            System.err.println("⚠️ Impossibile convertire '" + textValue +
                                    "' in double per campo '" + fieldName + "'. Impostato a null.");
                            conversions.add(new AbstractMap.SimpleEntry<>(fieldName, null));
                        }
                    }
                }
            }

            // ===== CONVERTI CAMPI STRING =====
            else if (stringFields.contains(fieldName)) {
                if (value.isNull()) {
                    conversions.add(new AbstractMap.SimpleEntry<>(fieldName, null));
                } else if (!value.isTextual()) {
                    // Converti numeri o altri tipi in stringa
                    String stringValue = value.asText();
                    conversions.add(new AbstractMap.SimpleEntry<>(fieldName, stringValue));
                }
            }

            // ===== CONVERTI CAMPI DATETIME =====
            else if (dateTimeFields.contains(fieldName)) {
                if (value.isNull()) {
                    conversions.add(new AbstractMap.SimpleEntry<>(fieldName, null));
                } else if (!value.isTextual()) {
                    // Assicurati che sia una stringa (formato ISO 8601)
                    String dateValue = value.asText();
                    conversions.add(new AbstractMap.SimpleEntry<>(fieldName, dateValue));
                }
            }
        }

        // Applica le conversioni
        for (Map.Entry<String, Object> conversion : conversions) {
            String fieldName = conversion.getKey();
            Object newValue = conversion.getValue();

            if (newValue == null) {
                obj.putNull(fieldName);
            } else if (newValue instanceof Double) {
                obj.put(fieldName, (Double) newValue);
            } else if (newValue instanceof Long) {
                obj.put(fieldName, (Long) newValue);
            } else if (newValue instanceof String) {
                obj.put(fieldName, (String) newValue);
            }
        }
    }




    // =============================================
    // METODI PER L'INTERAZIONE CON API ESTERNE
    // =============================================

    /**
     * Esegue una chiamata GET per ottenere dati da un'API esterna
     */
    public HttpResponse<String> getExternalData(String url, Map<String, String> headers) throws IOException, InterruptedException {
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