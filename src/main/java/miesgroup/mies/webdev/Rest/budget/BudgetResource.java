// BudgetResource.java
package miesgroup.mies.webdev.Rest.budget;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.budget.BudgetRepo;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Service.budget.BudgetService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/budget")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BudgetResource {

    @Inject
    BudgetService service;

    @Inject
    BudgetRepo budgetRepo;

    @Inject
    BudgetService budgetService;
    @Inject
    SessionRepo sessioneRepo;

    /**
     * GET /budget/{pod}/{anno}
     * Se i dati per anno richiesto non esistono,
     * prova a clonare quelli dell'anno precedente creando nuove righe.
     */
    @GET
    @Path("/{pod}/{anno}")
    @Transactional
    public Response getByPodAndAnno(@PathParam("pod") String pod,
                                    @PathParam("anno") Integer anno) {

        List<Budget> list = service.getBudgetsPerPodEAnno(pod, anno);

        if (list == null || list.isEmpty()) {
            // Prova a clonare dati anno precedente
            List<Budget> datiPrecedenti = service.getBudgetsPerPodEAnno(pod, anno - 1);

            if (datiPrecedenti == null || datiPrecedenti.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Dati base " + anno + " non trovati e nessun anno precedente disponibile"))
                        .build();
            }

            List<Budget> nuoviDati = new ArrayList<>();
            for (Budget b : datiPrecedenti) {
                Budget clone = new Budget();
                clone.setPod((String) b.getIdPod());
                clone.setAnno(anno);
                clone.setMese(b.getMese());
                clone.setPrezzoEnergiaBase(b.getPrezzoEnergiaBase());
                clone.setConsumiBase(b.getConsumiBase());
                clone.setOneriBase(b.getOneriBase());

                // Percentuali a zero (valori base non modificati)
                clone.setPrezzoEnergiaPerc(0.0);
                clone.setConsumiPerc(0.0);
                clone.setOneriPerc(0.0);

                // Imposta cliente uguale (o gestisci secondo il tuo modello)
                clone.setCliente(b.getCliente());

                budgetRepo.persist(clone);
                nuoviDati.add(clone);
            }

            return Response.ok(nuoviDati).build();
        }

        return Response.ok(list).build();
    }

    @GET
    @Path("/{pod}/{anno}/{mese}")
    public Response getSingolo(@PathParam("pod") String pod,
                               @PathParam("anno") Integer anno,
                               @PathParam("mese") Integer mese) {
        try {
            // Aggiorna il budget calcolando i dati base da bolletta (se implementato)
            service.aggiornaBudgetDaBolletta(pod, anno, mese);
        } catch (IllegalArgumentException e) {
            // Puoi loggare o gestire errore qui
        }

        Optional<Budget> b = service.getBudgetSingolo(pod, anno, mese);
        return b.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Non trovato")))
                .build();
    }

    /**
     * PUT /budget/previsioni?pod=...&anno=...&mese=...
     * Aggiorna o inserisce le percentuali di previsione.
     * Se il record non esiste, tenta di copiare i dati base 2025 come base.
     */
    @PUT
    @Path("/previsioni")
    @Transactional
    public Response upsertPrevisione(@CookieParam("SESSION_COOKIE") Integer sessionCookie,
                                     @QueryParam("pod") String podId,
                                     @QueryParam("anno") Integer anno,
                                     @QueryParam("mese") Integer mese,
                                     Map<String, Object> body) {
        try {
            // Validazione session cookie e estrazione cliente id
            if (sessionCookie == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Cookie di sessione mancante"))
                        .build();
            }

            Integer clienteId = sessioneRepo.getUserIdBySessionId(sessionCookie);
            if (clienteId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Cliente non trovato dalla sessione"))
                        .build();
            }

            Double prezzoPerc = extractDouble(body, "prezzoEnergiaPerc", 0.0);
            Double consumiPerc = extractDouble(body, "consumiPerc", 0.0);
            Double oneriPerc = extractDouble(body, "oneriPerc", 0.0);

            Optional<Budget> existing = budgetRepo.findByPodAnnoMese(podId, anno, mese);
            Budget budget;

            if (existing.isPresent()) {
                budget = existing.get();
                budget.setPrezzoEnergiaPerc(prezzoPerc);
                budget.setConsumiPerc(consumiPerc);
                budget.setOneriPerc(oneriPerc);
            } else {
                Optional<Budget> baseOpt = budgetRepo.findByPodAnnoMese(podId, anno, mese);
                if (baseOpt.isEmpty()) {
                    baseOpt = budgetRepo.findByPodAnnoMese(podId, 2025, mese);
                }
                if (baseOpt.isEmpty()) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Dati base non trovati per creazione nuovo record"))
                            .build();
                }

                Budget base = baseOpt.get();
                budget = new Budget();
                budget.setPod(podId);
                budget.setAnno(anno);
                budget.setMese(mese);
                budget.setPrezzoEnergiaBase(base.getPrezzoEnergiaBase());
                budget.setConsumiBase(base.getConsumiBase());
                budget.setOneriBase(base.getOneriBase());
                budget.setPrezzoEnergiaPerc(prezzoPerc);
                budget.setConsumiPerc(consumiPerc);
                budget.setOneriPerc(oneriPerc);
                // Non settare cliente qui perché lo settiamo dopo da sessione
            }

            Cliente cliente = new Cliente();
            cliente.setId(clienteId);
            budget.setCliente(cliente);

            boolean ok = budgetRepo.upsert(budget);
            if (!ok) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Errore nel salvataggio"))
                        .build();
            }
            return Response.noContent().build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }


    /**
     * GET /budget/prezzo-unitario?pod=...&anno=...&mese=...
     * Calcola e restituisce il prezzo unitario = prezzoEnergiaBase / consumiBase
     */
    @GET
    @Path("/prezzo-unitario")
    public Response getPrezzoUnitario(
            @QueryParam("pod") String podId,
            @QueryParam("anno") Integer anno,
            @QueryParam("mese") Integer mese
    ) {
        if (podId == null || anno == null || mese == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parametri pod, anno e mese obbligatori")
                    .build();
        }

        Optional<Budget> optBudget = budgetService.getBudgetSingolo(podId, anno, mese);
        if (optBudget.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Budget non trovato per pod, anno e mese specificati")
                    .build();
        }

        Budget budget = optBudget.get();

        if (budget.getConsumiBase() == null || budget.getConsumiBase() == 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Consumi base nulli o zero")
                    .build();
        }
        if (budget.getPrezzoEnergiaBase() == null || budget.getPrezzoEnergiaBase() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Prezzo energia base nullo o negativo")
                    .build();
        }

        double prezzoUnitario = budget.getPrezzoEnergiaBase() / budget.getConsumiBase();

        return Response.ok(prezzoUnitario).build();
    }

    private Double extractDouble(Map<String, Object> body, String key, double defaultValue) {
        Object value = body.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
