package miesgroup.mies.webdev.Rest.pbi;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import miesgroup.mies.webdev.Model.cliente.Sessione;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Rest.Model.*;
import miesgroup.mies.webdev.Service.bolletta.BudgetBollettaService;
import miesgroup.mies.webdev.Service.pbi.PowerBIService;

import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.io.IOException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

@Path("/proxy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProxyResource {
    // Dataset e tabelle
    private static final String DATASET_ID_CONTROLLO = "5ee09b8c-9750-4fee-9050-eb4f59de94f5";
    private static final String EnergyPortfolio_Complete_Dataset = "8e97a316-dce7-4c03-b281-0fa027c13ab4";
    private static final String BUDGET_DATASET = "b6257f45-90e0-4ef3-910f-86e3bf6aaba2";
    private static final String ARTICOLI   = "dettaglio_articolo";
    private static final String BOLLETTE   = "bolletta_pod";
    private static final String POD        = "pod_info";
    private static final String CALENDARIO = "calendario";
    private static final String BUDGET     = "budget";

    // URL base
    private static final String BASE_URL_PROD = "https://energyportfolio.it";
    private static final String BASE_URL_DEV  = "http://localhost:8081";
    private static final String API_PORT_PROD = ":8081";

    // Ambiente
    private static final boolean IS_DEV_ENV = true; // metti false in produzione

    private final SessionRepo sessionRepo;
    private final PowerBIService powerBIService;
    @Inject
    SessionRepo sessioneRepo;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public ProxyResource(SessionRepo sessionRepo, PowerBIService powerBIService) {
        this.sessionRepo = sessionRepo;
        this.powerBIService = powerBIService;
    }

    /** Valida il cookie e restituisce l'id sessione come stringa */
    private String validateSessionCookie(Integer sessionCookie) {
        if (sessionCookie == null) return null;
        return sessionRepo.getSessionById(sessionCookie)
                .map(Sessione::getId)
                .map(String::valueOf)
                .orElse(null);
    }

    /** Header standard per propagare la sessione anche come Cookie */
    private Map<String, String> buildSessionHeaders(String sessionId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "MiesApp/1.0");
        headers.put("X-Session-Id", sessionId);
        // Alcuni endpoint validano SOLO il cookie HTTP
        headers.put("Cookie", "SESSION_COOKIE=" + sessionId);
        headers.put("Accept", "application/json");
        return headers;
    }

    @GET
    @Path("/articoli")
    public Response inviaArticoliAPowerBI(@CookieParam("SESSION_COOKIE") Integer sessionCookie) {
        try {
            String sessionId = validateSessionCookie(sessionCookie);
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }
            System.out.println("[/proxy/articoli] session=" + sessionId);

            Integer userId = sessioneRepo.getUserIdBySessionId(sessionCookie);

            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"User not found for session\"}")
                        .build();
            }

            return powerBIService.inviaArticoliAPowerBIDB(userId, DATASET_ID_CONTROLLO, ARTICOLI);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore generale: " + e.getMessage() + "\"}")
                    .build();
        }
    }



    @GET
    @Path("/pod")
    public Response inviaPodAPowerBI(@CookieParam("SESSION_COOKIE") Integer sessionCookie) {
        try {
            String sessionId = validateSessionCookie(sessionCookie);
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }
            System.out.println("[/proxy/pod] session=" + sessionId);

            String baseUrl  = IS_DEV_ENV ? BASE_URL_DEV : (BASE_URL_PROD + API_PORT_PROD);
            String targetUrl = baseUrl + "/pod/dati?session_id=" + sessionId;

            HttpResponse<String> resp = powerBIService.getExternalData(targetUrl, buildSessionHeaders(sessionId));
            if (resp.statusCode() != 200) {
                System.out.println("Response Code: " + resp.statusCode());
                System.out.println("Response Body: " + resp.body());
                return Response.status(resp.statusCode()).entity(resp.body()).build();
            }

            String powerBIJson = powerBIService.wrapPodForPowerBI(resp.body());
            return powerBIService.aggiornaTabellaCompleta(DATASET_ID_CONTROLLO, POD, powerBIJson);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore generale: " + e.getMessage() + "\"}")
                    .build();
        }
    }



    @GET
    @Path("/calendario")
    public Response inviaCalendarioAPowerBI() {
        try {
            List<CalendarioBollettaDTO> calendarioList = powerBIService.getCalendarioCompleto();

            ArrayNode rows = mapper.createArrayNode();

            for (CalendarioBollettaDTO dto : calendarioList) {
                ObjectNode row = mapper.createObjectNode();
                row.put("id", dto.getId());
                row.put("mese_numero", dto.getMeseNumeroAnno());
                row.put("anno", dto.getAnno());
                row.put("nome_mese", dto.getNomeMese());
                row.put("nome_mese_abbreviato", dto.getNomeMeseAbbreviato());
                row.put("data_completa", dto.getDataCompleta());
                row.put("meseAnno", dto.getMeseAnno());
                rows.add(row);
            }

            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set("rows", rows);
            String powerBIJson = mapper.writeValueAsString(wrapper);

            return powerBIService.aggiornaTabellaCompleta(DATASET_ID_CONTROLLO, CALENDARIO, powerBIJson);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore calendario: " + e.getMessage() + "\"}")
                    .build();
        }
    }


    @GET
    @Path("/bollette")
    public Response inviaBolletteAPowerBI(@CookieParam("SESSION_COOKIE") Integer sessionCookie) {
        System.out.println("\n========== INIZIO /proxy/bollette ==========");
        System.out.println("[DEBUG] Timestamp: " + new java.util.Date());

        try {
            System.out.println("[DEBUG] Step 1: Validazione session cookie");
            System.out.println("[DEBUG] sessionCookie ricevuto: " + sessionCookie);

            String sessionId = validateSessionCookie(sessionCookie);
            if (sessionId == null) {
                System.out.println("[ERROR] Session cookie non valido o mancante");
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }

            System.out.println("[DEBUG] sessionId validato: " + sessionId);
            //Integer userId = estraiUserIdDaSessione(sessionId);  // Devi implementare/mettere a disposizione questo metodo
            Integer userId = sessioneRepo.getUserIdBySessionId(sessionCookie);
            System.out.println("[DEBUG] userId estratto: " + userId);

            // Chiama il servizio PowerBI passando userId, dataset id e nome tabella
            return powerBIService.inviaBolletteAPowerBIDB(userId, DATASET_ID_CONTROLLO, BOLLETTE);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore generale: " + e.getMessage() + "\"}")
                    .build();
        }
    }



    /**
     * Aggrega i dati budget per TUTTI i POD per l'anno indicato (default: anno corrente)
     * e li invia alla tabella Power BI "Budget".
     * Opzionale: ?year=YYYY
     */
    /*
    @GET
    @Path("/budget")
    public Response inviaBudgetAPowerBI(@CookieParam("SESSION_COOKIE") Integer sessionCookie,
                                        @QueryParam("year") Integer yearParam) {
        try {
            String sessionId = validateSessionCookie(sessionCookie);
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }
            final int year = (yearParam != null) ? yearParam : LocalDate.now().getYear();
            System.out.println("[/proxy/budget] session=" + sessionId + " year=" + year);

            String baseUrlData = IS_DEV_ENV ? BASE_URL_DEV : (BASE_URL_PROD + API_PORT_PROD);

            // 1) POD list
            String podsUrl = baseUrlData + "/pod/dati?session_id=" + sessionId;
            HttpResponse<String> podsResp = powerBIService.getExternalData(podsUrl, buildSessionHeaders(sessionId));
            if (podsResp.statusCode() != 200) {
                System.out.println("POD Response Code: " + podsResp.statusCode());
                System.out.println("POD Response Body: " + podsResp.body());
                return Response.status(podsResp.statusCode()).entity(podsResp.body()).build();
            }

            ArrayNode podsArray = (ArrayNode) mapper.readTree(podsResp.body());
            ArrayNode rows = mapper.createArrayNode();

            // 2) Per ciascun POD → /budget/{pod}/{year}
            for (JsonNode podNode : podsArray) {
                String podId = firstText(podNode, "id", "pod", "idPod");
                if (podId == null || podId.isEmpty()) continue;

                String podEnc = URLEncoder.encode(podId, StandardCharsets.UTF_8);
                String budgetUrl = baseUrlData + "/budget/" + podEnc + "/" + year + "?session_id=" + sessionId;

                HttpResponse<String> budResp = powerBIService.getExternalData(budgetUrl, buildSessionHeaders(sessionId));
                if (budResp.statusCode() != 200) {
                    System.out.println("Budget POD " + podId + " Code: " + budResp.statusCode());
                    System.out.println("Budget POD " + podId + " Body: " + budResp.body());
                    continue;
                }

                JsonNode arr = mapper.readTree(budResp.body());
                if (!arr.isArray()) continue;

                for (JsonNode n : arr) {
                    BudgetDtoPBI dto = toBudgetDtoPBI(n, podId, year);

                    ObjectNode row = mapper.createObjectNode();
                    row.put("Id_Bolletta", nz(dto.getIdBolletta()));
                    row.put("id_pod", nz(dto.getIdPod()));
                    row.put("Nome_Bolletta", nz(dto.getNomeBolletta()));
                    row.put("TOT_Attiva", nz(dto.getTotAttiva()));
                    row.put("Budget_Energia", nz(dto.getBudgetEnergia()));
                    row.put("Budget_Trasporto", nz(dto.getBudgetTrasporto()));
                    row.put("Budget_Oneri", nz(dto.getBudgetOneri()));
                    row.put("Budget_Imposte", nz(dto.getBudgetImposte()));
                    row.put("Budget_Totale", nz(dto.getBudgetTotale()));
                    row.put("Budget_Penali", nz(dto.getBudgetPenali()));
                    row.put("Budget_Altro", nz(dto.getBudgetAltro()));
                    row.put("Anno-Mese", nz(dto.getAnnoMese()));

                    rows.add(row);
                }
            }

            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set("rows", rows);
            String powerBIJson = mapper.writeValueAsString(wrapper);

            return powerBIService.aggiornaTabellaCompleta(BUDGET_DATASET, BUDGET, powerBIJson);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"JSON non valido: " + ioe.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore generale: " + e.getMessage() + "\"}")
                    .build();
        }
    }

     */

    // =========================
    //      MAPPERS & UTILS
    // =========================

    /** Converte un JsonNode generico della tua API in BollettaPodDtoPBI, con fallback sui nomi storici. */
    private BollettaPodDtoPBI toBollettaDtoPBI(JsonNode n) {
        BollettaPodDtoPBI d = new BollettaPodDtoPBI();
        d.setIdBolletta(firstText(n, "idBolletta", "id", "Id_Bolletta"));
        d.setIdPod(firstText(n, "idPod", "pod", "id_pod"));
        d.setNomeBolletta(firstText(n, "nomeBolletta", "Nome_Bolletta"));

        d.setF1Attiva(firstDouble(n, "f1Attiva", "f1Att", "F1_Attiva"));
        d.setF2Attiva(firstDouble(n, "f2Attiva", "f2Att", "F2_Attiva"));
        d.setF3Attiva(firstDouble(n, "f3Attiva", "f3Att", "F3_Attiva"));

        d.setF1Reattiva(firstDouble(n, "f1Reattiva", "f1R", "F1_Reattiva"));
        d.setF2Reattiva(firstDouble(n, "f2Reattiva", "f2R", "F2_Reattiva"));
        d.setF3Reattiva(firstDouble(n, "f3Reattiva", "f3R", "F3_Reattiva"));

        d.setF1Potenza(firstDouble(n, "f1Potenza", "f1Pot", "F1_Potenza"));
        d.setF2Potenza(firstDouble(n, "f2Potenza", "f2Pot", "F2_Potenza"));
        d.setF3Potenza(firstDouble(n, "f3Potenza", "f3Pot", "F3_Potenza"));

        d.setSpeseEnergia(firstDouble(n, "speseEnergia", "speseEne", "Spese_Energia"));
        d.setSpeseTrasporto(firstDouble(n, "speseTrasporto", "speseTrasp", "Spese_Trasporto"));
        d.setOneri(firstDouble(n, "oneri", "Oneri"));
        d.setImposte(firstDouble(n, "imposte", "Imposte"));

        d.setPeriodoInizio(firstText(n, "periodoInizio", "Periodo_Inizio"));
        d.setPeriodoFine(firstText(n, "periodoFine", "Periodo_Fine"));

        Integer mese = firstInt(n, "mese", "Mese");
        d.setMese(mese);

        d.setTotAttiva(firstDouble(n, "totAttiva", "totAtt", "TOT_Attiva"));
        d.setTotReattiva(firstDouble(n, "totReattiva", "totR", "TOT_Reattiva"));

        d.setGeneration(firstDouble(n, "generation", "Generation"));
        d.setDispacciamento(firstDouble(n, "dispacciamento", "Dispacciamento"));

        // Penali: se hai F1/F2 le sommo come fallback
        Double pen33 = firstDouble(n, "penali33", "Penali33");
        if (pen33 == null) pen33 = sumDoubles(n, "f1Pen33", "f2Pen33");
        d.setPenali33(pen33);

        Double pen75 = firstDouble(n, "penali75", "Penali75");
        if (pen75 == null) pen75 = sumDoubles(n, "f1Pen75", "f2Pen75");
        d.setPenali75(pen75);

        d.setVerificaTrasporti(firstDouble(n, "verificaTrasporti", "Verifica_Trasporti"));
        d.setVerificaOneri(firstDouble(n, "verificaOneri", "Verifica_Oneri"));
        d.setVerificaImposte(firstDouble(n, "verificaImposte", "Verifica_Imposte"));
        d.setAltro(firstDouble(n, "altro", "Altro"));

        Integer anno = firstInt(n, "anno", "Anno");
        if (anno == null) {
            String a = firstText(n, "anno");
            if (a != null && a.matches("\\d+")) anno = Integer.parseInt(a);
        }
        d.setAnno(anno);

        d.setPiccoKwh(firstDouble(n, "piccoKwh", "picco_kwh"));
        d.setFuoriPiccoKwh(firstDouble(n, "fuoriPiccoKwh", "fuori_picco_kwh"));
        d.setEuroPicco(firstDouble(n, "euroPicco", "€_picco"));
        d.setEuroFuoriPicco(firstDouble(n, "euroFuoriPicco", "€_fuori_picco"));
        d.setVerificaPicco(firstDouble(n, "verificaPicco", "verifica_picco"));
        d.setVerificaFuoriPicco(firstDouble(n, "verificaFuoriPicco", "verifica_fuori_picco"));

        d.setPodNome(firstText(n, "podNome", "pod"));
        d.setAnnoMese(firstText(n, "annoMese", "meseAnno", "Anno-Mese"));
        return d;
    }

    /** Converte un JsonNode generico in BudgetDtoPBI, con fallback e derivazione anno-mese. */
    private BudgetDtoPBI toBudgetDtoPBI(JsonNode n, String fallbackPod, int fallbackYear) {
        BudgetDtoPBI d = new BudgetDtoPBI();
        d.setIdBolletta(firstText(n, "idBolletta", "Id_Bolletta"));
        d.setIdPod(nz(firstText(n, "idPod", "pod", "id_pod", "podId", "id")) != null ? firstText(n, "idPod", "pod", "id_pod", "podId", "id") : fallbackPod);
        d.setNomeBolletta(firstText(n, "nomeBolletta", "Nome_Bolletta"));

        d.setTotAttiva(firstDouble(n, "totAttiva", "totAtt", "TOT_Attiva"));
        d.setBudgetEnergia(firstDouble(n, "budgetEnergia", "Budget_Energia"));
        d.setBudgetTrasporto(firstDouble(n, "budgetTrasporto", "Budget_Trasporto"));
        d.setBudgetOneri(firstDouble(n, "budgetOneri", "Budget_Oneri"));
        d.setBudgetImposte(firstDouble(n, "budgetImposte", "Budget_Imposte"));
        d.setBudgetTotale(firstDouble(n, "budgetTotale", "Budget_Totale"));
        d.setBudgetPenali(firstDouble(n, "budgetPenali", "Budget_Penali"));
        d.setBudgetAltro(firstDouble(n, "budgetAltro", "Budget_Altro"));

        String annoMese = firstText(n, "annoMese", "meseAnno", "Anno-Mese");
        if (annoMese == null || annoMese.isBlank()) {
            Integer anno = firstInt(n, "anno", "Anno");
            if (anno == null) anno = fallbackYear;
            Integer mese = firstInt(n, "mese", "Mese");
            if (mese == null) mese = 1;
            annoMese = String.format("%04d-%02d", anno, mese);
        }
        d.setAnnoMese(annoMese);

        return d;
    }

    // ---------------- Utils mapping ----------------

    private static String firstText(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.get(k);
            if (v != null && !v.isNull()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) return s;
            }
        }
        return null;
    }

    private static Integer firstInt(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.get(k);
            if (v != null && v.isInt()) return v.asInt();
            if (v != null && v.isTextual()) {
                try { return Integer.parseInt(v.asText()); } catch (Exception ignored) {}
            }
            if (v != null && v.isNumber()) return v.asInt();
        }
        return null;
    }

    private static Double firstDouble(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.get(k);
            if (v != null && v.isNumber()) return v.asDouble();
            if (v != null && v.isTextual()) {
                try { return Double.parseDouble(v.asText().replace(",", ".")); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static Double sumDoubles(JsonNode n, String... keys) {
        double sum = 0d;
        boolean any = false;
        for (String k : keys) {
            Double v = firstDouble(n, k);
            if (v != null) { sum += v; any = true; }
        }
        return any ? sum : null;
    }

    private static String ensureIsoDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        // se è già ISO con 'T' la lascio, altrimenti aggiungo orario UTC
        return (s.contains("T")) ? s : (s + "T00:00:00Z");
    }

    @Inject
    BudgetBollettaService budgetBollettaService;

    @POST
    @Path("/budget/consolidato/push")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pushBudgetConsolidato(
            @QueryParam("year") Integer year,
            @QueryParam("pod") String pod) {

        try {
            System.out.println("==== START pushBudgetConsolidato ====");
            System.out.println("Year: " + year + ", Pod: " + pod);

            // 1. Recupera i dati dal service
            EnergyPortfolioCompleteDatasetDTO dataset = budgetBollettaService.getBudgetConsolidato(year, pod);

            // 2. Converte ogni lista in JSON formato Power BI {"rows": [...]}
            String budgetJson = powerBIService.wrapDataForPowerBI(dataset.getBudget());
            String calendarioJson = powerBIService.wrapDataForPowerBI(dataset.getCalendario());
            String bollettaPodJson = powerBIService.wrapDataForPowerBI(dataset.getBolletta_pod());

            // 3. Log dei JSON inviati (utile per debug)
            System.out.println("Budget JSON: " + budgetJson);
            System.out.println("Calendario JSON: " + calendarioJson);
            System.out.println("Bolletta POD JSON: " + bollettaPodJson);

            System.out.println("Budget:");
            dataset.getBudget().forEach(item -> System.out.println(item));

            System.out.println("\nCalendario:");
            dataset.getCalendario().forEach(item -> System.out.println(item));

            System.out.println("\nBolletta POD:");
            dataset.getBolletta_pod().forEach(item -> System.out.println(item));


            // 4. Cancella dati esistenti su tutte le tabelle nel dataset
            boolean budgetDeleted = powerBIService.eliminazioneRighe(BUDGET_DATASET, "budget");
            boolean calendarioDeleted = powerBIService.eliminazioneRighe(BUDGET_DATASET, "calendario");
            boolean bollettaDeleted = powerBIService.eliminazioneRighe(BUDGET_DATASET, "bolletta_pod");

            if (!budgetDeleted || !calendarioDeleted || !bollettaDeleted) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"Errore nella cancellazione dati PowerBI prima del caricamento\"}")
                        .build();
            }

            // 5. Invia dati a tutte le tabelle Power BI
            Response respBudget = powerBIService.invioDati(BUDGET_DATASET, "budget", budgetJson);
            Response respCalendario = powerBIService.invioDati(BUDGET_DATASET, "calendario", calendarioJson);
            Response respBolletta = powerBIService.invioDati(BUDGET_DATASET, "bolletta_pod", bollettaPodJson);

            // 6. Verifica lo stato delle risposte per tutti i dataset
            if (respBudget.getStatus() != 200) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"Errore invio dati tabella budget: " + respBudget.getEntity() + "\"}")
                        .build();
            }
            if (respCalendario.getStatus() != 200) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"Errore invio dati tabella calendario: " + respCalendario.getEntity() + "\"}")
                        .build();
            }
            if (respBolletta.getStatus() != 200) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"Errore invio dati tabella bolletta_pod: " + respBolletta.getEntity() + "\"}")
                        .build();
            }

            System.out.println("==== END pushBudgetConsolidato SUCCESS ====");

            // 7. Restituisci risposta JSON di successo con dettagli
            return Response.ok()
                    .entity("{\"status\":\"Dataset Power BI aggiornato con successo\","
                            + "\"year\":" + year + ","
                            + "\"pod\":\"" + (pod != null ? pod : "") + "\"}")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Eccezione durante il caricamento Power BI: " + e.getMessage() + "\"}")
                    .build();
        }
    }


    // Metodi helper per creare le strutture delle tabelle
    private ObjectNode createTableSchema(String tableName, ArrayNode columns, ArrayNode rows) {
        System.out.println("Creando schema per tabella: " + tableName);
        System.out.println("- Colonne: " + columns.size());
        System.out.println("- Righe: " + rows.size());

        ObjectNode table = mapper.createObjectNode();
        table.put("name", tableName);
        table.set("columns", columns);
        table.set("rows", rows);

        System.out.println("Schema tabella " + tableName + " completato");
        return table;
    }

    private ArrayNode createBudgetColumns() {
        ArrayNode columns = mapper.createArrayNode();
        columns.add(createColumn("Id_Bolletta", "String"));
        columns.add(createColumn("id_pod", "String"));
        columns.add(createColumn("Nome_Bolletta", "String"));
        columns.add(createColumn("TOT_Attiva", "Double"));
        columns.add(createColumn("Budget_Energia", "Double"));
        columns.add(createColumn("Budget_Trasporto", "Double"));
        columns.add(createColumn("Budget_Oneri", "Double"));
        columns.add(createColumn("Budget_Imposte", "Double"));
        columns.add(createColumn("Budget_Totale", "Double"));
        columns.add(createColumn("Budget_Penali", "Double"));
        columns.add(createColumn("Budget_Altro", "Double"));
        columns.add(createColumn("Anno-Mese", "String"));
        return columns;
    }

    private ArrayNode createCalendarioColumns() {
        ArrayNode columns = mapper.createArrayNode();
        columns.add(createColumn("Anno", "Int64"));
        columns.add(createColumn("Mese_Numero", "Int64"));
        columns.add(createColumn("Mese_Nome", "String"));
        columns.add(createColumn("Anno-Mese", "String"));
        columns.add(createColumn("Mese_Abbreviato", "String"));
        columns.add(createColumn("Trimestre", "Int64"));
        columns.add(createColumn("Periodo", "String"));
        columns.add(createColumn("Periodo_Trimestre", "String"));
        return columns;
    }

    private ArrayNode createBollettaPodColumns() {
        ArrayNode columns = mapper.createArrayNode();
        columns.add(createColumn("IdBolletta", "String"));
        columns.add(createColumn("idpod", "String"));
        columns.add(createColumn("NomeBolletta", "String"));
        columns.add(createColumn("F1_Attiva", "Double"));
        columns.add(createColumn("F2_Attiva", "Double"));
        columns.add(createColumn("F3_Attiva", "Double"));
        columns.add(createColumn("F1_Reattiva", "Double"));
        columns.add(createColumn("F2_Reattiva", "Double"));
        columns.add(createColumn("F3_Reattiva", "Double"));
        columns.add(createColumn("F1_Potenza", "Double"));
        columns.add(createColumn("F2_Potenza", "Double"));
        columns.add(createColumn("F3_Potenza", "Double"));
        columns.add(createColumn("Spese_Energia", "Double"));
        columns.add(createColumn("Spese_Trasporto", "Double"));
        columns.add(createColumn("Oneri", "Double"));
        columns.add(createColumn("Imposte", "Double"));
        columns.add(createColumn("Periodo_Inizio", "DateTime"));
        columns.add(createColumn("Periodo_Fine", "DateTime"));
        columns.add(createColumn("Mese", "Int64"));
        columns.add(createColumn("TOT_Attiva", "Double"));
        columns.add(createColumn("TOT_Reattiva", "Double"));
        columns.add(createColumn("Generation", "Double"));
        columns.add(createColumn("Dispacciamento", "Double"));
        columns.add(createColumn("Verifica_Trasporti", "Double"));
        columns.add(createColumn("Penali33", "Double"));
        columns.add(createColumn("Penali75", "Double"));
        columns.add(createColumn("Verifica_Oneri", "Double"));
        columns.add(createColumn("Verifica_Imposte", "Double"));
        columns.add(createColumn("Altro", "Double"));
        columns.add(createColumn("Anno", "Int64"));
        columns.add(createColumn("picco_kwh", "Double"));
        columns.add(createColumn("fuori_picco_kwh", "Double"));
        columns.add(createColumn("€_picco", "Double"));
        columns.add(createColumn("€_fuori_picco", "Double"));
        columns.add(createColumn("verifica_picco", "Double"));
        columns.add(createColumn("verifica_fuori_picco", "Double"));
        columns.add(createColumn("podNome", "String"));
        columns.add(createColumn("Anno-Mese", "String"));
        return columns;
    }

    private ObjectNode createColumn(String name, String dataType) {
        ObjectNode column = mapper.createObjectNode();
        column.put("name", name);
        column.put("dataType", dataType);
        return column;
    }

    private ArrayNode convertBudgetToRows(List<BudgetBollettaDTO> budgetList) {
        ArrayNode rows = mapper.createArrayNode();
        for (BudgetBollettaDTO dto : budgetList) {
            ObjectNode row = mapper.createObjectNode();
            row.put("IdBolletta", dto.getIdBolletta() != null ? dto.getIdBolletta() : 0);
            row.put("idpod", dto.getIdPod() != null ? dto.getIdPod() : "");                                  // ✅ getIdpod()
            row.put("NomeBolletta", dto.getNomeBolletta() != null ? dto.getNomeBolletta().intValue() : 0);             // ✅ getNomeBolletta()
            row.put("TOTAttiva", dto.getTotAttiva() != null ? dto.getTotAttiva() : 0);                       // ✅ getTOTAttiva()
            row.put("BudgetEnergia", dto.getBudgetEnergia() != null ? dto.getBudgetEnergia() : 0);           // ✅ getBudgetEnergia()
            row.put("BudgetTrasporto", dto.getBudgetTrasporto() != null ? dto.getBudgetTrasporto() : 0);     // ✅ getBudgetTrasporto()
            row.put("BudgetOneri", dto.getBudgetOneri() != null ? dto.getBudgetOneri() : 0);                 // ✅ getBudgetOneri()
            row.put("BudgetImposte", dto.getBudgetImposte() != null ? dto.getBudgetImposte() : 0);           // ✅ getBudgetImposte()
            row.put("BudgetTotale", dto.getBudgetTotale() != null ? dto.getBudgetTotale() : 0);              // ✅ getBudgetTotale()
            row.put("BudgetPenali", dto.getBudgetPenali() != null ? dto.getBudgetPenali() : 0);              // ✅ getBudgetPenali()
            row.put("BudgetAltro", dto.getBudgetAltro() != null ? dto.getBudgetAltro() : 0);                 // ✅ getBudgetAltro()
            row.put("Anno-Mese", dto.getAnnoMese() != null ? dto.getAnnoMese() : "");                        // ✅ getAnnoMese()
            rows.add(row);
        }
        return rows;
    }

    private ArrayNode convertCalendarioToRows(List<CalendarioDTO> calendarioList) {
        ArrayNode rows = mapper.createArrayNode();
        for (CalendarioDTO dto : calendarioList) {
            ObjectNode row = mapper.createObjectNode();
            row.put("Anno", dto.getAnno() != null ? dto.getAnno() : 0);                                      // ✅ getAnno()
            row.put("Mese Numero", dto.getMeseNumero() != null ? dto.getMeseNumero() : 0);                   // ✅ getMeseNumero() + spazio nel nome colonna
            row.put("Mese Nome", dto.getMeseNome() != null ? dto.getMeseNome() : "");                        // ✅ getMeseNome() + spazio nel nome colonna
            row.put("Anno-Mese", dto.getAnnoMese() != null ? dto.getAnnoMese() : "");                        // ✅ getAnnoMese()
            row.put("Mese Abbreviato", dto.getMeseAbbreviato() != null ? dto.getMeseAbbreviato() : "");      // ✅ getMeseAbbreviato() + spazio nel nome colonna
            row.put("Trimestre", dto.getTrimestre() != null ? dto.getTrimestre() : "");                      // ✅ getTrimestre() (String, non Long!)
            row.put("Periodo", dto.getPeriodo() != null ? dto.getPeriodo() : "");                            // ✅ getPeriodo()
            row.put("PeriodoTrimestre", dto.getPeriodoTrimestre() != null ? dto.getPeriodoTrimestre() : ""); // ✅ getPeriodoTrimestre()
            rows.add(row);
        }
        return rows;
    }

/*
    private ArrayNode convertBollettaPodToRows(List<BollettaPodDTO> bollettaPodList) {
        ArrayNode rows = mapper.createArrayNode();
        for (BollettaPodDTO dto : bollettaPodList) {
            ObjectNode row = mapper.createObjectNode();
            row.put("IdBolletta", dto.getIdBolletta() != null ? dto.getIdBolletta() : 0);
            row.put("idpod", dto.getIdpod() != null ? dto.getIdpod() : "");                                  // ✅ getIdpod()
            row.put("NomeBolletta", dto.getNomeBolletta() != null ? dto.getNomeBolletta().intValue() : 0);           // ✅ getNomeBolletta()
            row.put("F1Attiva", dto.getF1Attiva() != null ? dto.getF1Attiva() : 0);                         // ✅ getF1Attiva() (senza underscore!)
            row.put("F2Attiva", dto.getF2Attiva() != null ? dto.getF2Attiva() : 0);                         // ✅ getF2Attiva()
            row.put("F3Attiva", dto.getF3Attiva() != null ? dto.getF3Attiva() : 0);                         // ✅ getF3Attiva()
            row.put("F1Reattiva", dto.getF1Reattiva() != null ? dto.getF1Reattiva() : 0);                   // ✅ getF1Reattiva()
            row.put("F2Reattiva", dto.getF2Reattiva() != null ? dto.getF2Reattiva() : 0);                   // ✅ getF2Reattiva()
            row.put("F3Reattiva", dto.getF3Reattiva() != null ? dto.getF3Reattiva() : 0);                   // ✅ getF3Reattiva()
            row.put("F1Potenza", dto.getF1Potenza() != null ? dto.getF1Potenza() : 0);                      // ✅ getF1Potenza()
            row.put("F2Potenza", dto.getF2Potenza() != null ? dto.getF2Potenza() : 0);                      // ✅ getF2Potenza()
            row.put("F3Potenza", dto.getF3Potenza() != null ? dto.getF3Potenza() : 0);                      // ✅ getF3Potenza()
            row.put("SpeseEnergia", dto.getSpeseEnergia() != null ? dto.getSpeseEnergia() : 0);              // ✅ getSpeseEnergia()
            row.put("SpeseTrasporto", dto.getSpeseTrasporto() != null ? dto.getSpeseTrasporto() : 0);        // ✅ getSpeseTrasporto()
            row.put("Oneri", dto.getOneri() != null ? dto.getOneri() : 0);                                   // ✅ getOneri()
            row.put("Imposte", dto.getImposte() != null ? dto.getImposte() : 0);                             // ✅ getImposte()
            row.put("PeriodoInizio", dto.getPeriodoInizio() != null ? dto.getPeriodoInizio().toString() : null); // ✅ getPeriodoInizio()
            row.put("PeriodoFine", dto.getPeriodoFine() != null ? dto.getPeriodoFine().toString() : null);   // ✅ getPeriodoFine()
            row.put("Mese", dto.getMese() != null ? dto.getMese() : "");                                     // ✅ getMese() (String, non Long!)
            row.put("TOTAttiva", dto.getTOTAttiva() != null ? dto.getTOTAttiva() : 0);                       // ✅ getTOTAttiva()
            row.put("TOTReattiva", dto.getTOTReattiva() != null ? dto.getTOTReattiva() : 0);                 // ✅ getTOTReattiva()
            row.put("Generation", dto.getGeneration() != null ? dto.getGeneration() : 0);                    // ✅ getGeneration()
            row.put("Dispacciamento", dto.getDisspacciamento() != null ? dto.getDisspacciamento() : 0);      // ✅ getDispackciamento()
            row.put("VerificaTrasporti", dto.getVerificaTrasporti() != null ? dto.getVerificaTrasporti() : 0); // ✅ getVerificaTrasporti()
            row.put("Penali33", dto.getPenali33() != null ? dto.getPenali33() : 0);                          // ✅ getPenali33()
            row.put("Penali75", dto.getPenali75() != null ? dto.getPenali75() : 0);                          // ✅ getPenali75()
            row.put("VerificaOneri", dto.getVerificaOneri() != null ? dto.getVerificaOneri() : 0);           // ✅ getVerificaOneri()
            row.put("VerificaImposte", dto.getVerificaImposte() != null ? dto.getVerificaImposte() : 0);     // ✅ getVerificaImposte()
            row.put("Altro", dto.getAltro());                                  // ✅ getAltro()
            row.put("Anno", dto.getAnno() != null ? dto.getAnno() : 0);                                      // ✅ getAnno()
            row.put("piccokwh", dto.getPiccokwh() != null ? dto.getPiccokwh() : 0);                          // ✅ getPiccokwh()
            row.put("fuoripiccokwh", dto.getFuoripiccokwh() != null ? dto.getFuoripiccokwh() : 0);           // ✅ getFuoripiccokwh()
            row.put("picco", dto.getPicco() != null ? dto.getPicco() : 0);                                   // ✅ getPicco()
            row.put("fuoripicco", dto.getFuoripicco() != null ? dto.getFuoripicco() : 0);                    // ✅ getFuoripicco()
            row.put("verificapicco", dto.getVerificapicco() != null ? dto.getVerificapicco() : 0);           // ✅ getVerificapicco()
            row.put("verificafuoripicco", dto.getVerificafuoripicco() != null ? dto.getVerificafuoripicco() : 0); // ✅ getVerificafuoripicco()
            row.put("Anno-Mese", dto.getAnnoMese() != null ? dto.getAnnoMese() : "");                        // ✅ getAnnoMese()
            rows.add(row);
        }
        return rows;
    }

 */


    private double safeToDouble(Object o) {
        if (o == null) return 0d;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception ignored) {}
        return 0d;
    }

    // --- Null-safe helpers ---
    private static String nz(String s) { return s == null ? "" : s; }
    private static double nz(Double d) { return d == null ? 0d : d; }
}