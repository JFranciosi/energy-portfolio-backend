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
import miesgroup.mies.webdev.Service.budget.BudgetService;

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

    @GET
    @Path("/{pod}/{anno}")
    public Response getByPodAndAnno(@PathParam("pod") String pod,
                                    @PathParam("anno") Integer anno) {
        List<Budget> list = service.getBudgetsPerPodEAnno(pod, anno);
        return Response.ok(list).build();
    }

    @GET
    @Path("/{pod}/{anno}/{mese}")
    public Response getSingolo(@PathParam("pod") String pod,
                               @PathParam("anno") Integer anno,
                               @PathParam("mese") Integer mese) {
        Optional<Budget> b = service.getBudgetSingolo(pod, anno, mese);
        return b.map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Non trovato")))
                .build();
    }

    /**
     * PUT /budget/previsioni?pod=...&anno=...&mese=...
     * Aggiorna o inserisce le percentuali di previsione.
     * Body JSON: {"prezzoEnergiaPerc":Double, "consumiPerc":Double, "oneriPerc":Double}
     */
    @PUT
    @Path("/previsioni")
    @Transactional
    public Response upsertPrevisione(@QueryParam("pod") String podId,
                                     @QueryParam("anno") Integer anno,
                                     @QueryParam("mese") Integer mese,
                                     Map<String, Object> body) {

        Double prezzoPerc = extractDouble(body, "prezzoEnergiaPerc", 0.0);
        Double consumiPerc = extractDouble(body, "consumiPerc", 0.0);
        Double oneriPerc = extractDouble(body, "oneriPerc", 0.0);

        Optional<Budget> existing = budgetRepo.findByPodAnnoMese(podId, anno, mese);
        Budget budget;

        if (existing.isPresent()) {
            // Se esiste già record per anno richiesto, aggiorna solo percentuali
            budget = existing.get();
            budget.setPrezzoEnergiaPerc(prezzoPerc);
            budget.setConsumiPerc(consumiPerc);
            budget.setOneriPerc(oneriPerc);
        } else {
            // Altrimenti crea nuovo record copiando base dal 2025, poi aggiorna percentuali
            Optional<Budget> base2025Opt = budgetRepo.findByPodAnnoMese(podId, 2025, mese);
            if (base2025Opt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Dati base 2025 non trovati"))
                        .build();
            }
            Budget base2025 = base2025Opt.get();
            budget = new Budget();
            budget.setPod(podId);
            budget.setAnno(anno);
            budget.setMese(mese);
            // Copia valori base dal 2025
            budget.setPrezzoEnergiaBase(base2025.getPrezzoEnergiaBase());
            budget.setConsumiBase(base2025.getConsumiBase());
            budget.setOneriBase(base2025.getOneriBase());

            // Setta percentuali modificate da client
            budget.setPrezzoEnergiaPerc(prezzoPerc);
            budget.setConsumiPerc(consumiPerc);
            budget.setOneriPerc(oneriPerc);
        }

        Cliente cliente = new Cliente();
        cliente.setId(1); // Ottenere id reale utente dalla sessione/security
        budget.setCliente(cliente);

        boolean ok = budgetRepo.saveOrUpdate(budget);

        if (!ok) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore nel salvataggio"))
                    .build();
        }
        return Response.noContent().build();
    }


    private Double extractDouble(Map<String, Object> body, String prezzoEnergiaPerc, double v) {
        Object value = body.get(prezzoEnergiaPerc);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return v; // ritorna il valore di default se non è un numero valido
            }
        }
        return v; // ritorna il valore di default se non presente o non convertibile
    }

}
