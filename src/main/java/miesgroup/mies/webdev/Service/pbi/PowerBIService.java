package miesgroup.mies.webdev.Service.pbi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// Scegliamo solo le importazioni di RESTEasy Classic
import jakarta.transaction.Transactional;
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
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Model.bolletta.verBollettaPod;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.bolletta.PodRepo;
import miesgroup.mies.webdev.Repository.bolletta.verBollettaPodRepo;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;
import miesgroup.mies.webdev.Rest.Model.CalendarioBollettaDTO;
import org.jboss.logging.Logger;

// Utility
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Config
import org.eclipse.microprofile.config.inject.ConfigProperty;



@ApplicationScoped
public class PowerBIService {

    private static final String BASE_URL = "https://api.powerbi.com/v1.0/myorg/groups/d62409c0-b987-4280-b892-67d8a24f9755/datasets/";

    @Inject
    private AzureADService azureADService;

    @Inject
    PodRepo podRepo;

    @Inject
    BollettaPodRepo bollettaPodRepo;

    @Inject
    verBollettaPodRepo verBollettaPodRepo;

    @Inject
    ClienteRepo clienteRepo;

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
        System.out.println("[DEBUG] Raw JSON input:");
        System.out.println(rawJson);

        ObjectMapper mapper = new ObjectMapper();

        // Presumo che rawJson sia un array di oggetti
        JsonNode articoliArray = mapper.readTree(rawJson);
        System.out.println("[DEBUG] Parsed JSON array node, size: " + articoliArray.size());

        for (int i = 0; i < articoliArray.size(); i++) {
            JsonNode articolo = articoliArray.get(i);
            System.out.println("[DEBUG] Articolo " + i + ": " + articolo.toString());
        }

        ObjectNode root = mapper.createObjectNode();
        root.set("rows", articoliArray);

        String wrappedJson = mapper.writeValueAsString(root);
        System.out.println("[DEBUG] Wrapped JSON output:");
        System.out.println(wrappedJson);

        return wrappedJson;
    }

    @Transactional
    public Response inviaArticoliAPowerBIDB(Integer userId, String datasetId, String tableName) {
        System.out.println("\n[DEBUG PBIService] === Inizio invio articoli da DB ===");
        System.out.println("[DEBUG PBIService] userId: " + userId);
        System.out.println("[DEBUG PBIService] datasetId: " + datasetId);
        System.out.println("[DEBUG PBIService] tableName: " + tableName);

        ObjectMapper mapper = new ObjectMapper();

        try {
            // 1. Recupera tutti i POD dell'utente
            Cliente cliente = clienteRepo.findById(userId);
            List<Pod> userPods = podRepo.list("utente", cliente);
            System.out.println("[DEBUG PBIService] POD trovati: " + userPods.size());
            if (userPods.isEmpty()) {
                System.out.println("[WARN PBIService] Nessun POD trovato per userId: " + userId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Nessun POD trovato per l'utente\"}")
                        .build();
            }

            // 2. Recupera tutte le bollette associate ai POD
            List<BollettaPod> bollette = bollettaPodRepo.findBollettaPodByPods(userPods);
            System.out.println("[DEBUG PBIService] Bollette trovate: " + bollette.size());

            // Prepara mappa idBolletta -> BollettaPod per rapida ricerca mese
            Map<Integer, BollettaPod> mappaBollette = new HashMap<>();
            for (BollettaPod b : bollette) {
                mappaBollette.put(b.getId(), b);
            }

            // 3. Recupera tutti verBollettaPod associati ai POD dell'utente
            List<verBollettaPod> articoli = verBollettaPodRepo.findByPodList(userPods);
            System.out.println("[DEBUG PBIService] Articoli trovati: " + articoli.size());

            if (articoli.isEmpty()) {
                System.out.println("[WARN PBIService] Nessun articolo trovato");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Nessun articolo trovato\"}")
                        .build();
            }

            // 4. Crea JSON per Power BI, ogni costo unitario come riga distinta
            ArrayNode rows = mapper.createArrayNode();
            int idCounter = 1;

            for (verBollettaPod vp : articoli) {
                System.out.println("\n[DEBUG PBIService] Elaborazione articolo verBollettaPod ID: " + vp.getId());
                System.out.println("[DEBUG PBIService] nomeBolletta: " + vp.getNomeBolletta());

                // Recupera idBolletta da verBollettaPod (adattare se bollettaId è oggetto)
                Integer idBolletta = (vp.getBollettaId() != null) ? vp.getBollettaId().getId() : null;

                // Recupera mese da mappa bollette
                String mese = "unknown";
                if (idBolletta != null && mappaBollette.containsKey(idBolletta)) {
                    mese = mappaBollette.get(idBolletta).getMese();
                }

                System.out.println("[DEBUG PBIService] Mese associato: " + mese);
                //imposte = imposte e penale reattiva induttiva = altro
                // Costruzione righe dei costi unitari passa mese come argomento
                rows.add(createArticoloRow(vp, "F0_Euro", vp.getF0Euro(), idCounter++, mese, "MATERIA ENERGIA"));
                rows.add(createArticoloRow(vp, "F1_Euro", vp.getF1Euro(), idCounter++, mese, "MATERIA ENERGIA"));
                rows.add(createArticoloRow(vp, "F1_Perd_Euro", vp.getF1PerdEuro(), idCounter++, mese, "MATERIA ENERGIA"));
                rows.add(createArticoloRow(vp, "F2_Euro", vp.getF2Euro(), idCounter++, mese, "MATERIA ENERGIA"));
                rows.add(createArticoloRow(vp, "F2_Perd_Euro", vp.getF2PerdEuro(), idCounter++, mese, "MATERIA ENERGIA"));//ALTRO, DISPACCIAMENTO, TRASPORTO, ONERI, IMPOSTE
                rows.add(createArticoloRow(vp, "F3_Euro", vp.getF3Euro(), idCounter++, mese, "MATERIA ENERGIA"));
                rows.add(createArticoloRow(vp, "F3_Perd_Euro", vp.getF3PerdEuro(), idCounter++, mese, "MATERIA ENERGIA"));

                rows.add(createArticoloRow(vp, "QFix_Trasp", vp.getQFixTrasp(), idCounter++, mese, "TRASPORTO"));
                rows.add(createArticoloRow(vp, "QPot_Trasp", vp.getQPotTrasp(), idCounter++, mese, "TRASPORTO"));
                rows.add(createArticoloRow(vp, "QVar_Trasp", vp.getQVarTrasp(), idCounter++, mese, "TRASPORTO"));

                rows.add(createArticoloRow(vp, "QEnOn_ASOS", vp.getQEnOnASOS(), idCounter++, mese, "ONERI"));
                rows.add(createArticoloRow(vp, "QEnOn_ARIM", vp.getQEnOnARIM(), idCounter++, mese, "ONERI"));
                rows.add(createArticoloRow(vp, "QFixOn_ASOS", vp.getQFixOnASOS(), idCounter++, mese, "ONERI"));
                rows.add(createArticoloRow(vp, "QFixOn_ARIM", vp.getQFixOnARIM(), idCounter++, mese, "ONERI"));
                rows.add(createArticoloRow(vp, "QPotOn_ASOS", vp.getQPotOnASOS(), idCounter++, mese, "ONERI"));
                rows.add(createArticoloRow(vp, "QPotOn_ARIM", vp.getQPotOnARIM(), idCounter++, mese, "ONERI"));

                rows.add(createArticoloRow(vp, "Art25bis", vp.getArt25bis(), idCounter++, mese, "DISPACCIAMENTO"));
                rows.add(createArticoloRow(vp, "Art44_3", vp.getArt44_3(), idCounter++, mese, "DISPACCIAMENTO"));
                rows.add(createArticoloRow(vp, "Art44bis", vp.getArt44bis(), idCounter++, mese, "DISPACCIAMENTO"));
                rows.add(createArticoloRow(vp, "Art46", vp.getArt46(), idCounter++, mese, "DISPACCIAMENTO"));
                rows.add(createArticoloRow(vp, "Art48", vp.getArt48(), idCounter++, mese, "DISPACCIAMENTO"));
                rows.add(createArticoloRow(vp, "Art73", vp.getArt73(), idCounter++, mese, "DISPACCIAMENTO"));
                rows.add(createArticoloRow(vp, "Art45Ann", vp.getArt45Ann(), idCounter++, mese, "DISPACCIAMENTO"));
                rows.add(createArticoloRow(vp, "Art45Tri", vp.getArt45Tri(), idCounter++, mese, "DISPACCIAMENTO"));

                rows.add(createArticoloRow(vp, "UC3_UC6", vp.getUc3Uc6(), idCounter++, mese, "TRASPORTO"));
                rows.add(createArticoloRow(vp, "Trasp_QEne", vp.getTraspQEne(), idCounter++, mese, "TRASPORTO"));
                rows.add(createArticoloRow(vp, "Distr_QEne", vp.getDistrQEne(), idCounter++, mese, "TRASPORTO"));
                rows.add(createArticoloRow(vp, "Distr_QPot", vp.getDistrQPot(), idCounter++, mese, "TRASPORTO"));
                rows.add(createArticoloRow(vp, "Mis_QFix", vp.getMisQFix(), idCounter++, mese, "TRASPORTO"));
                rows.add(createArticoloRow(vp, "Distr_QFix", vp.getDistrQFix(), idCounter++, mese, "TRASPORTO"));

                rows.add(createArticoloRow(vp, "f1Penale33", vp.getF1Pen33(), idCounter++, mese, "ALTRO"));
                rows.add(createArticoloRow(vp, "f1Penale75", vp.getF1Pen75(), idCounter++, mese, "ALTRO"));
                rows.add(createArticoloRow(vp, "f2Penale33", vp.getF2Pen33(), idCounter++, mese, "ALTRO"));
                rows.add(createArticoloRow(vp, "f2Penale75", vp.getF2Pen75(), idCounter++, mese, "ALTRO"));
                rows.add(createArticoloRow(vp, "Pen_RCapI", vp.getPenRCapI(), idCounter++, mese, "ALTRO"));
            }

            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set("rows", rows);

            String powerBIJson = mapper.writeValueAsString(wrapper);

            System.out.println("[DEBUG PBIService] JSON creato - " + rows.size() + " righe");
            System.out.println("[DEBUG PBIService] Lunghezza JSON: " + powerBIJson.length() + " chars");

            return aggiornaTabellaCompleta(datasetId, tableName, powerBIJson);

        } catch (Exception e) {
            System.err.println("[ERROR PBIService] Errore: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }


    private ObjectNode createArticoloRow(verBollettaPod vp, String nomeArticolo, Double costoUnitario, int id, String mese, String categoriaDispacciamento) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode row = mapper.createObjectNode();

        row.put("id", id);
        row.put("costoUnitario", costoUnitario != null ? costoUnitario : 0.0);
        row.put("mese", mese != null ? mese : "unknown");
        row.put("nomeArticolo", nomeArticolo);
        row.put("nomeBolletta", vp.getNomeBolletta());
        row.put("categoriaArticolo", categoriaDispacciamento);

        System.out.println("[DEBUG PBIService] Articolo creato - id: " + id + ", idPod: " + vp.getIdPod() + ", nomeArticolo: " + nomeArticolo + ", costoUnitario: " + costoUnitario + ", mese: " + mese + ", nomeBolletta: " + vp.getNomeBolletta());

        return row;
    }

    private static final String[] MESINOMI = {
            "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
    };

    private static final String[] MESIABBREVIATI = {
            "Gen", "Feb", "Mar", "Apr", "Mag", "Giu",
            "Lug", "Ago", "Set", "Ott", "Nov", "Dic"
    };

    public List<CalendarioBollettaDTO> getCalendarioCompleto() {
        List<Integer> anni = bollettaPodRepo.findDistinctAnni(); // Metodo da implementare o ricavato

        List<CalendarioBollettaDTO> calendarioList = new ArrayList<>();
        int idCounter = 1;

        for (Integer anno : anni) {
            for (int mese = 1; mese <= 12; mese++) {
                CalendarioBollettaDTO dto = new CalendarioBollettaDTO();

                dto.setId((long) idCounter++);
                dto.setMeseNumeroAnno(String.valueOf(mese));
                dto.setAnno(String.valueOf(anno));
                dto.setNomeMese(MESINOMI[mese - 1]);
                dto.setNomeMeseAbbreviato(MESIABBREVIATI[mese - 1]);
                dto.setDataCompleta(String.format("%d-%02d-01", anno, mese));
                dto.setMeseAnno(MESIABBREVIATI[mese - 1] + " " + anno);

                // Stampo i dati per debug
                System.out.println("Generato CalendarioBollettaDTO: " + dto.toString());

                calendarioList.add(dto);
            }
        }
        return calendarioList;
    }



    /**
     * Formatta i dati JSON dei POD nel formato richiesto da PowerBI.
     */
    public String wrapPodForPowerBI(String rawJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Stampa il JSON in ingresso
        System.out.println("Input JSON: " + rawJson);

        // Presumo che rawJson sia un array di oggetti
        JsonNode podArray = mapper.readTree(rawJson);

        ObjectNode root = mapper.createObjectNode();
        root.set("rows", podArray);

        String wrappedJson = mapper.writeValueAsString(root);

        // Stampa il JSON di output
        System.out.println("Output JSON: " + wrappedJson);

        return wrappedJson;
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

    @Transactional
    public Response inviaBolletteAPowerBIDB(Integer userId, String datasetId, String tableName) {
        System.out.println("\n[DEBUG PBIService] === Inizio invio bollette da DB ===");
        System.out.println("[DEBUG PBIService] userId: " + userId);
        System.out.println("[DEBUG PBIService] datasetId: " + datasetId);
        System.out.println("[DEBUG PBIService] tableName: " + tableName);
        ObjectMapper mapper = new ObjectMapper();

        try {
            // 1. Recupera tutti i POD dell'utente
            Cliente cliente = clienteRepo.findById(userId);
            List<Pod> userPods = podRepo.list("utente", cliente);

            System.out.println("[DEBUG PBIService] POD trovati: " + userPods.size());

            if (userPods.isEmpty()) {
                System.out.println("[WARN PBIService] Nessun POD trovato per userId: " + userId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Nessun POD trovato per l'utente\"}")
                        .build();
            }

            // 2. Recupera tutte le bollette dei POD dell'utente
            List<BollettaPod> bollette = bollettaPodRepo.findBollettaPodByPods(userPods);
            System.out.println("[DEBUG PBIService] Bollette trovate: " + bollette.size());

            if (bollette.isEmpty()) {
                System.out.println("[WARN PBIService] Nessuna bolletta trovata");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Nessuna bolletta trovata\"}")
                        .build();
            }

            // 3. Crea JSON per Power BI
            ArrayNode rows = mapper.createArrayNode();

            for (BollettaPod b : bollette) {
                System.out.println("\n[DEBUG PBIService] Elaborazione bolletta ID: " + b.getId());

                verBollettaPod ver = verBollettaPodRepo.findByBollettaId(b);
                System.out.println("[DEBUG PBIService] Verifica trovata: " + (ver != null));

                ObjectNode row = mapper.createObjectNode();

                System.out.println("[DEBUG PBIService] id: " + nz(b.getId()));
                row.put("id", nz(b.getId()));

                System.out.println("[DEBUG PBIService] idPod: " + nz(b.getIdPod()));
                row.put("idPod", nz(b.getIdPod()));

                System.out.println("[DEBUG PBIService] nomeBolletta: " + nz(b.getNomeBolletta()));
                row.put("nomeBolletta", nz(b.getNomeBolletta()));

                System.out.println("[DEBUG PBIService] f1A: " + nz(b.getF1Att()));
                row.put("f1A", nz(b.getF1Att()));

                System.out.println("[DEBUG PBIService] f2A: " + nz(b.getF2Att()));
                row.put("f2A", nz(b.getF2Att()));

                System.out.println("[DEBUG PBIService] f3A: " + nz(b.getF3Att()));
                row.put("f3A", nz(b.getF3Att()));

                System.out.println("[DEBUG PBIService] f1R: " + nz(b.getF1R()));
                row.put("f1R", nz(b.getF1R()));

                System.out.println("[DEBUG PBIService] f2R: " + nz(b.getF2R()));
                row.put("f2R", nz(b.getF2R()));

                System.out.println("[DEBUG PBIService] f3R: " + nz(b.getF3R()));
                row.put("f3R", nz(b.getF3R()));

                System.out.println("[DEBUG PBIService] f1P: " + nz(b.getF1Pot()));
                row.put("f1P", nz(b.getF1Pot()));

                System.out.println("[DEBUG PBIService] f2P: " + nz(b.getF2Pot()));
                row.put("f2P", nz(b.getF2Pot()));

                System.out.println("[DEBUG PBIService] f3P: " + nz(b.getF3Pot()));
                row.put("f3P", nz(b.getF3Pot()));

                System.out.println("[DEBUG PBIService] totAttiva: " + nz(b.getTotAtt()));
                row.put("totAttiva", nz(b.getTotAtt()));

                System.out.println("[DEBUG PBIService] totReattiva: " + nz(b.getTotR()));
                row.put("totReattiva", nz(b.getTotR()));

                System.out.println("[DEBUG PBIService] speseEnergia: " + nz(b.getSpeseEne()));
                row.put("speseEnergia", nz(b.getSpeseEne()));

                System.out.println("[DEBUG PBIService] trasporti: " + nz(b.getSpeseTrasp()));
                row.put("trasporti", nz(b.getSpeseTrasp()));

                System.out.println("[DEBUG PBIService] oneri: " + nz(b.getOneri()));
                row.put("oneri", nz(b.getOneri()));

                System.out.println("[DEBUG PBIService] imposte: " + nz(b.getImposte()));
                row.put("imposte", nz(b.getImposte()));

                System.out.println("[DEBUG PBIService] generation: " + nz(b.getGeneration()));
                row.put("generation", nz(b.getGeneration()));

                System.out.println("[DEBUG PBIService] dispacciamento: " + nz(b.getDispacciamento()));
                row.put("dispacciamento", nz(b.getDispacciamento()));

                Double penali33 = sommaDoubles(b.getF1Pen33(), b.getF2Pen33());
                System.out.println("[DEBUG PBIService] penali33 (somma): " + penali33);
                row.put("penali33", nz(penali33));

                Double penali75 = sommaDoubles(b.getF1Pen75(), b.getF2Pen75());
                System.out.println("[DEBUG PBIService] penali75 (somma): " + penali75);
                row.put("penali75", nz(penali75));

                String periodoInizioStr = toSimpleDateString(b.getPeriodoInizio());
                System.out.println("[DEBUG PBIService] periodoInizio: " + periodoInizioStr);
                row.put("periodoInizio", periodoInizioStr);

                String periodoFineStr = toSimpleDateString(b.getPeriodoFine());
                System.out.println("[DEBUG PBIService] periodoFine: " + periodoFineStr);
                row.put("periodoFine", periodoFineStr);

                System.out.println("[DEBUG PBIService] anno: " + nz(b.getAnno()));
                row.put("anno", nz(b.getAnno()));

                System.out.println("[DEBUG PBIService] mese: " + nz(b.getMese()));
                row.put("mese", nz(b.getMese()));

                System.out.println("[DEBUG PBIService] meseAnno: " + nz(b.getMeseAnno()));
                row.put("meseAnno", nz(b.getMeseAnno()));

                System.out.println("[DEBUG PBIService] piccoKwh: " + nz(b.getPiccoKwh()));
                row.put("piccoKwh", nz(b.getPiccoKwh()));

                System.out.println("[DEBUG PBIService] fuoriPiccoKwh: " + nz(b.getFuoriPiccoKwh()));
                row.put("fuoriPiccoKwh", nz(b.getFuoriPiccoKwh()));

                System.out.println("[DEBUG PBIService] costoPicco: " + nz(b.getEuroPicco()));
                row.put("costoPicco", nz(b.getEuroPicco()));

                System.out.println("[DEBUG PBIService] costoFuoriPicco: " + nz(b.getEuroFuoriPicco()));
                row.put("costoFuoriPicco", nz(b.getEuroFuoriPicco()));

                // Se verify esiste
                if (ver != null) {
                    System.out.println("[DEBUG PBIService] verificaOneri: " + nz(ver.getOneri()));
                    row.put("verificaOneri", nz(ver.getOneri()));

                    System.out.println("[DEBUG PBIService] verificaTrasporti: " + nz(ver.getSpeseTrasp()));
                    row.put("verificaTrasporti", nz(ver.getSpeseTrasp()));

                    System.out.println("[DEBUG PBIService] verificaImposte: " + nz(ver.getImposte()));
                    row.put("verificaImposte", nz(ver.getImposte()));

                    System.out.println("[DEBUG PBIService] verificaPicco: " + nz(ver.getEuroPicco()));
                    row.put("verificaPicco", nz(ver.getEuroPicco()));

                    System.out.println("[DEBUG PBIService] verificaFuoriPicco: " + nz(ver.getEuroFuoriPicco()));
                    row.put("verificaFuoriPicco", nz(ver.getEuroFuoriPicco()));

                    System.out.println("[DEBUG PBIService] verificaSpesaMateriaEnergia: " + ver.getF0Euro()+ver.getF1Euro()+ver.getF2Euro()+ver.getF3Euro());
                    row.put("verificaSpesaMateriaEnergia", ver.getF0Euro()+ver.getF1Euro()+ver.getF2Euro()+ver.getF3Euro() + ver.getF1PerdEuro() + ver.getF2PerdEuro() + ver.getF3PerdEuro() + ver.getDispacciamento() + ver.getEuroPicco() + ver.getEuroFuoriPicco());

                    System.out.println("[DEBUG PBIService] altro: " + nz(b.getPenRCapI()));
                    row.put("altro", nz(b.getPenRCapI()));

                    System.out.println("[DEBUG PBIService] verificaDispacciamento: " + nz(ver.getDispacciamento()));
                    row.put("verificaDispacciamento", nz(ver.getDispacciamento()));

                    System.out.println("[DEBUG PBIService] f0Euro: " + nz(ver.getF0Euro()));
                    row.put("f0Euro", nz(ver.getF0Euro()));

                    System.out.println("[DEBUG PBIService] f1Euro: " + nz(ver.getF1Euro()));
                    row.put("f1Euro", nz(ver.getF1Euro()));

                    System.out.println("[DEBUG PBIService] f2Euro: " + nz(ver.getF2Euro()));
                    row.put("f2Euro", nz(ver.getF2Euro()));

                    System.out.println("[DEBUG PBIService] f3Euro: " + nz(ver.getF3Euro()));
                    row.put("f3Euro", nz(ver.getF3Euro()));

                    System.out.println("[DEBUG PBIService] f1PerditeEuro: " + nz(ver.getF1PerdEuro()));
                    row.put("f1PerditeEuro", nz(ver.getF1PerdEuro()));

                    System.out.println("[DEBUG PBIService] f2PerditeEuro: " + nz(ver.getF2PerdEuro()));
                    row.put("f2PerditeEuro", nz(ver.getF2PerdEuro()));

                    System.out.println("[DEBUG PBIService] f3PerditeEuro: " + nz(ver.getF3PerdEuro()));
                    row.put("f3PerditeEuro", nz(ver.getF3PerdEuro()));

                    System.out.println("[DEBUG PBIService] f0Kwh: " + nz(ver.getF0Kwh()));
                    row.put("f0Kwh", nz(ver.getF0Kwh()));

                    System.out.println("[DEBUG PBIService] f1Kwh: " + nz(ver.getF1Kwh()));
                    row.put("f1Kwh", nz(ver.getF1Kwh()));

                    System.out.println("[DEBUG PBIService] f2Kwh: " + nz(ver.getF2Kwh()));
                    row.put("f2Kwh", nz(ver.getF2Kwh()));

                    System.out.println("[DEBUG PBIService] f3Kwh: " + nz(ver.getF3Kwh()));
                    row.put("f3Kwh", nz(ver.getF3Kwh()));

                    System.out.println("[DEBUG PBIService] f1PerditeKwh: " + nz(ver.getF1PerdKwh()));
                    row.put("f1PerditeKwh", nz(ver.getF1PerdKwh()));

                    System.out.println("[DEBUG PBIService] f2PerditeKwh: " + nz(ver.getF2PerdKwh()));
                    row.put("f2PerditeKwh", nz(ver.getF2PerdKwh()));

                    System.out.println("[DEBUG PBIService] f3PerditeKwh: " + nz(ver.getF3PerdKwh()));
                    row.put("f3PerditeKwh", nz(ver.getF3PerdKwh()));

                    System.out.println("[DEBUG PBIService] totAttivaPerdite: " + nz(ver.getTotAttPerd()));
                    row.put("totAttivaPerdite", nz(ver.getTotAttPerd()));

                    System.out.println("[DEBUG PBIService] quotaVariabileTrasporti: " + nz(ver.getQVarTrasp()));
                    row.put("quotaVariabileTrasporti", nz(ver.getQVarTrasp()));

                    System.out.println("[DEBUG PBIService] quotaFissaTrasporti: " + nz(ver.getQFixTrasp()));
                    row.put("quotaFissaTrasporti", nz(ver.getQFixTrasp()));

                    System.out.println("[DEBUG PBIService] quotaPotenzaTrasporti: " + nz(ver.getQPotTrasp()));
                    row.put("quotaPotenzaTrasporti", nz(ver.getQPotTrasp()));

                    Double quotaEnergiaOneri = sommaDoubles(ver.getQEnOnASOS(), ver.getQEnOnARIM());
                    System.out.println("[DEBUG PBIService] quotaEnergiaOneri (somma): " + quotaEnergiaOneri);
                    row.put("quotaEnergiaOneri", nz(quotaEnergiaOneri));

                    Double quotaFissaOneri = sommaDoubles(ver.getQFixOnASOS(), ver.getQFixOnARIM());
                    System.out.println("[DEBUG PBIService] quotaFissaOneri (somma): " + quotaFissaOneri);
                    row.put("quotaFissaOneri", nz(quotaFissaOneri));

                    Double quotaPotenzaOneri = sommaDoubles(ver.getQPotOnASOS(), ver.getQPotOnARIM());
                    System.out.println("[DEBUG PBIService] quotaPotenzaOneri (somma): " + quotaPotenzaOneri);
                    row.put("quotaPotenzaOneri", nz(quotaPotenzaOneri));

                    // Campi TODO (mettiamo a zero per ora)
                    System.out.println("[DEBUG PBIService] verificaF0: 0.0");
                    row.put("verificaF0", 0.0);

                    System.out.println("[DEBUG PBIService] verificaF1: 0.0");
                    row.put("verificaF1", 0.0);

                    System.out.println("[DEBUG PBIService] verificaF2: 0.0");
                    row.put("verificaF2", 0.0);

                    System.out.println("[DEBUG PBIService] verificaF3: 0.0");
                    row.put("verificaF3", 0.0);

                    System.out.println("[DEBUG PBIService] verificaF1Perdite: 0.0");
                    row.put("verificaF1Perdite", 0.0);

                    System.out.println("[DEBUG PBIService] verificaF2Perdite: 0.0");
                    row.put("verificaF2Perdite", 0.0);

                    System.out.println("[DEBUG PBIService] verificaF3Perdite: 0.0");
                    row.put("verificaF3Perdite", 0.0);

                } else {
                    System.out.println("[DEBUG PBIService] Nessuna verifica, imposto tutto a zero di default");
                    String[] zeroFields = {
                            "verificaOneri", "verificaTrasporti", "verificaImposte", "verificaPicco", "verificaFuoriPicco",
                            "verificaSpesaMateriaEnergia", "verificaDispacciamento", "f0Euro", "f1Euro", "f2Euro", "f3Euro",
                            "f1PerditeEuro", "f2PerditeEuro", "f3PerditeEuro", "f0Kwh", "f1Kwh", "f2Kwh", "f3Kwh", "f1PerditeKwh",
                            "f2PerditeKwh", "f3PerditeKwh", "verificaF0", "verificaF1", "verificaF2", "verificaF3", "verificaF1Perdite",
                            "verificaF2Perdite", "verificaF3Perdite", "totAttivaPerdite", "quotaVariabileTrasporti", "quotaFissaTrasporti",
                            "quotaPotenzaTrasporti", "quotaEnergiaOneri", "quotaFissaOneri", "quotaPotenzaOneri" };
                    for (String field : zeroFields) {
                        System.out.println("[DEBUG PBIService] " + field + ": 0.0");
                        row.put(field, 0.0);
                    }
                }

                rows.add(row);
            }

            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set("rows", rows);
            String powerBIJson = mapper.writeValueAsString(wrapper);

            System.out.println("[DEBUG PBIService] JSON creato - " + rows.size() + " righe");
            System.out.println("[DEBUG PBIService] Lunghezza JSON: " + powerBIJson.length() + " chars");

            return aggiornaTabellaCompleta(datasetId, tableName, powerBIJson);

        } catch (Exception e) {
            System.err.println("[ERROR PBIService] Errore: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    public String toSimpleDateString(java.sql.Date date) {
        if (date == null) {
            return null;
        }
        LocalDate localDate = LocalDate.parse(date.toString()); // data in formato yyyy-MM-dd
        return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE); // "yyyy-MM-dd"
    }

    // Helper per sommare due Double (null-safe)
    private Double sommaDoubles(Double a, Double b) {
        return Optional.ofNullable(a).orElse(0.0) + Optional.ofNullable(b).orElse(0.0);
    }

    // Helper per convertire java.sql.Date a ISO DateTime String
    private String ensureIsoDateTime(java.sql.Date date) {
        if (date == null) return null;
        return date.toLocalDate().atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    // Helper per valori null (già esistente, ma per sicurezza)
    private String nz(Object val) {
        if (val == null) return "";
        if (val instanceof Double || val instanceof Integer) {
            return val.toString();
        }
        return val.toString();
    }

    private Double nz(Double val) {
        return val != null ? val : 0.0;
    }

    private Integer nz(Integer val) {
        return val != null ? val : 0;
    }

}