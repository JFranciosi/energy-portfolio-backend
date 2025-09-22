package miesgroup.mies.webdev.Rest.pbi;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import miesgroup.mies.webdev.Model.cliente.Sessione;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Rest.Model.BudgetBollettaDTO;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import miesgroup.mies.webdev.Rest.Model.BollettaPodDtoPBI;
import miesgroup.mies.webdev.Rest.Model.BudgetDtoPBI;

import static org.apache.commons.lang3.math.NumberUtils.toDouble;

@Path("/proxy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProxyResource {
    // Dataset e tabelle
    private static final String DATASET_ID_CONTROLLO = "5ee09b8c-9750-4fee-9050-eb4f59de94f5";
    private static final String EnergyPortfolio_Complete_Dataset = "8e97a316-dce7-4c03-b281-0fa027c13ab4";
    private static final String ARTICOLI   = "dettaglio_articolo";
    private static final String BOLLETTE   = "bolletta_pod";
    private static final String POD        = "pod_info";
    private static final String CALENDARIO = "calendario";
    private static final String BUDGET     = "Budget";

    // URL base
    private static final String BASE_URL_PROD = "https://energyportfolio.it";
    private static final String BASE_URL_DEV  = "http://localhost:8081";
    private static final String API_PORT_PROD = ":8081";

    // Ambiente
    private static final boolean IS_DEV_ENV = true; // metti false in produzione

    private final SessionRepo sessionRepo;
    private final PowerBIService powerBIService;
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

            String baseUrl  = IS_DEV_ENV ? BASE_URL_DEV : BASE_URL_PROD;
            String targetUrl = baseUrl + "/costo-articolo?session_id=" + sessionId;

            HttpResponse<String> resp = powerBIService.getExternalData(targetUrl, buildSessionHeaders(sessionId));
            if (resp.statusCode() != 200) {
                System.out.println("Response Code: " + resp.statusCode());
                System.out.println("Response Body: " + resp.body());
                return Response.status(resp.statusCode()).entity(resp.body()).build();
            }

            String powerBIJson = powerBIService.wrapArticoliForPowerBI(resp.body());
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
            return powerBIService.aggiornaTabella(DATASET_ID_CONTROLLO, POD, powerBIJson);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore generale: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/calendario")
    public Response inviaCalendarioAPowerBI(@QueryParam("startYear") Integer startYear,
                                            @QueryParam("endYear") Integer endYear) {
        try {
            int from = (startYear != null) ? startYear : 2020;
            int to   = (endYear != null)   ? endYear   : 2030;

            ArrayNode rows = mapper.createArrayNode();

            for (int year = from; year <= to; year++) {
                for (int month = 1; month <= 12; month++) {
                    ObjectNode row = mapper.createObjectNode();
                    row.put("Anno", year);
                    row.put("Mese_Numero", month);
                    row.put("Mese_Nome", Month.of(month).getDisplayName(TextStyle.FULL, Locale.ITALIAN));
                    row.put("Anno-Mese", String.format("%04d-%02d", year, month));
                    row.put("Mese_Abbreviato", Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ITALIAN));
                    row.put("Trimestre", ((month - 1) / 3) + 1);
                    row.put("Periodo", year + "-" + String.format("%02d", month));
                    row.put("Periodo_Trimestre", year + " Q" + (((month - 1) / 3) + 1));
                    rows.add(row);
                }
            }

            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set("rows", rows);
            String powerBIJson = mapper.writeValueAsString(wrapper);

            return powerBIService.aggiornaTabella(EnergyPortfolio_Complete_Dataset, CALENDARIO, powerBIJson);

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
        try {
            String sessionId = validateSessionCookie(sessionCookie);
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }
            System.out.println("[/proxy/bollette] session=" + sessionId);

            String baseUrl  = IS_DEV_ENV ? BASE_URL_DEV : (BASE_URL_PROD + API_PORT_PROD);
            String targetUrl = baseUrl + "/files/dati?session_id=" + sessionId;

            HttpResponse<String> resp = powerBIService.getExternalData(targetUrl, buildSessionHeaders(sessionId));
            if (resp.statusCode() != 200) {
                System.out.println("Response Code: " + resp.statusCode());
                System.out.println("Response Body: " + resp.body());
                return Response.status(resp.statusCode()).entity(resp.body()).build();
            }

            // --- Parse → DTO PBI con fallback ---
            ArrayNode srcArray = (ArrayNode) mapper.readTree(resp.body());
            ArrayNode rows = mapper.createArrayNode();

            for (JsonNode n : srcArray) {
                BollettaPodDtoPBI dto = toBollettaDtoPBI(n);

                ObjectNode row = mapper.createObjectNode();
                row.put("Id_Bolletta", nz(dto.getIdBolletta()));
                row.put("id_pod", nz(dto.getIdPod()));
                row.put("Nome_Bolletta", nz(dto.getNomeBolletta()));
                row.put("F1_Attiva", nz(dto.getF1Attiva()));
                row.put("F2_Attiva", nz(dto.getF2Attiva()));
                row.put("F3_Attiva", nz(dto.getF3Attiva()));
                row.put("F1_Reattiva", nz(dto.getF1Reattiva()));
                row.put("F2_Reattiva", nz(dto.getF2Reattiva()));
                row.put("F3_Reattiva", nz(dto.getF3Reattiva()));
                row.put("F1_Potenza", nz(dto.getF1Potenza()));
                row.put("F2_Potenza", nz(dto.getF2Potenza()));
                row.put("F3_Potenza", nz(dto.getF3Potenza()));
                row.put("Spese_Energia", nz(dto.getSpeseEnergia()));
                row.put("Spese_Trasporto", nz(dto.getSpeseTrasporto()));
                row.put("Oneri", nz(dto.getOneri()));
                row.put("Imposte", nz(dto.getImposte()));
                row.put("Periodo_Inizio", ensureIsoDateTime(dto.getPeriodoInizio()));
                row.put("Periodo_Fine", ensureIsoDateTime(dto.getPeriodoFine()));
                row.put("Mese", dto.getMese() == null ? 0 : dto.getMese());
                row.put("TOT_Attiva", nz(dto.getTotAttiva()));
                row.put("TOT_Reattiva", nz(dto.getTotReattiva()));
                row.put("Generation", nz(dto.getGeneration()));
                row.put("Dispacciamento", nz(dto.getDispacciamento()));
                row.put("Verifica_Trasporti", nz(dto.getVerificaTrasporti()));
                row.put("Penali33", nz(dto.getPenali33()));
                row.put("Penali75", nz(dto.getPenali75()));
                row.put("Verifica_Oneri", nz(dto.getVerificaOneri()));
                row.put("Verifica_Imposte", nz(dto.getVerificaImposte()));
                row.put("Altro", nz(dto.getAltro()));
                row.put("Anno", dto.getAnno() == null ? 0 : dto.getAnno());
                row.put("picco_kwh", nz(dto.getPiccoKwh()));
                row.put("fuori_picco_kwh", nz(dto.getFuoriPiccoKwh()));
                row.put("€_picco", nz(dto.getEuroPicco()));
                row.put("€_fuori_picco", nz(dto.getEuroFuoriPicco()));
                row.put("verifica_picco", nz(dto.getVerificaPicco()));
                row.put("verifica_fuori_picco", nz(dto.getVerificaFuoriPicco()));
                row.put("podNome", nz(dto.getPodNome() != null ? dto.getPodNome() : dto.getIdPod()));
                row.put("Anno-Mese", nz(dto.getAnnoMese()));

                rows.add(row);
            }

            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set("rows", rows);
            String powerBIJson = mapper.writeValueAsString(wrapper);

            return powerBIService.aggiornaTabella(EnergyPortfolio_Complete_Dataset, BOLLETTE, powerBIJson);

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

    /**
     * Aggrega i dati budget per TUTTI i POD per l'anno indicato (default: anno corrente)
     * e li invia alla tabella Power BI "Budget".
     * Opzionale: ?year=YYYY
     */
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

            return powerBIService.aggiornaTabella(EnergyPortfolio_Complete_Dataset, BUDGET, powerBIJson);

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
    public Response inviaBudgetConsolidato(@CookieParam("SESSION_COOKIE") Integer sessionCookie,
                                           @QueryParam("year") Integer year,
                                           @QueryParam("pod") String pod) {
        try {
            String sessionId = validateSessionCookie(sessionCookie);
            if (sessionId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Missing SESSION_COOKIE\"}")
                        .build();
            }

            var lista = budgetBollettaService.getBudgetConsolidato(year, pod);

            ArrayNode jsonRows = mapper.createArrayNode();
            for (BudgetBollettaDTO dto : lista) {
                ObjectNode row = mapper.createObjectNode();
                row.put("Id_Bolletta", ""); // nel dataset è String, lasciamo vuoto
                row.put("id_pod", dto.id_pod != null ? dto.id_pod : "");
                row.put("Nome_Bolletta", dto.Nome_Bolletta != null ? dto.Nome_Bolletta : "");
                row.put("TOT_Attiva", dto.TOT_Attiva != null ? dto.TOT_Attiva : 0);
                row.put("Budget_Energia", dto.Budget_Energia != null ? dto.Budget_Energia : 0);
                row.put("Budget_Trasporto", dto.Budget_Trasporto != null ? dto.Budget_Trasporto : 0);
                row.put("Budget_Oneri", dto.Budget_Oneri != null ? dto.Budget_Oneri : 0);
                row.put("Budget_Imposte", dto.Budget_Imposte != null ? dto.Budget_Imposte : 0);
                row.put("Budget_Totale", dto.Budget_Totale != null ? dto.Budget_Totale : 0);
                row.put("Budget_Penali", dto.Budget_Penali != null ? dto.Budget_Penali : 0);
                row.put("Budget_Altro", dto.Budget_Altro != null ? dto.Budget_Altro : 0);
                row.put("Anno-Mese", dto.AnnoMese != null ? dto.AnnoMese : "");
                jsonRows.add(row);
            }

            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set("rows", jsonRows);

            String powerBIJson = mapper.writeValueAsString(wrapper);
            return powerBIService.aggiornaTabella(EnergyPortfolio_Complete_Dataset, "Budget", powerBIJson);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Errore consolidato: " + e.getMessage() + "\"}")
                    .build();
        }
    }



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