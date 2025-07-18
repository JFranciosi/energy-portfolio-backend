package miesgroup.mies.webdev.Rest.budget;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import miesgroup.mies.webdev.Rest.Model.BudgetAllDto;
import miesgroup.mies.webdev.Model.budget.BudgetAll;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Service.budget.BudgetAllService;

import java.util.List;
import java.util.Map;

@Path("/budget/all")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BudgetAllResource {

    @Inject
    BudgetAllService service;

    @Context
    SecurityContext securityContext;

    @GET
    @Path("/tutti/{anno}")
    public Response getAggregati(@PathParam("anno") Integer anno) {
        Long idUtente = getCurrentUserId();
        List<BudgetAll> list = service.getAggregatiPerUtenteEAnno(idUtente, anno);
        return Response.ok(list).build();
    }

    @POST
    @Transactional
    @RolesAllowed("USER")
    public Response upsertAggregato(BudgetAllDto dto) {
        // Costruisco l'entità
        BudgetAll entity = new BudgetAll();
        entity.setAnno(dto.getAnno());
        entity.setMese(dto.getMese());
        entity.setPrezzoEnergiaBase(dto.getPrezzoEnergiaBase());
        entity.setConsumiBase(dto.getConsumiBase());
        entity.setOneriBase(dto.getOneriBase());
        entity.setPrezzoEnergiaPerc(dto.getPrezzoEnergiaPerc());
        entity.setConsumiPerc(dto.getConsumiPerc());
        entity.setOneriPerc(dto.getOneriPerc());

        // Associo il cliente
        Long userId = dto.getUtenteId() != null ? dto.getUtenteId() : getCurrentUserId();
        Cliente cliente = new Cliente();
        cliente.setId(Math.toIntExact(userId));
        entity.setCliente(cliente);

        // Salvo
        boolean ok = service.upsertAggregato(entity);
        if (!ok) {
            return Response.serverError()
                    .entity(Map.of("error", "Non è stato possibile salvare l'aggregato"))
                    .build();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    private Long getCurrentUserId() {
        // TODO: estrarre effettivo ID da securityContext.getUserPrincipal()
        return 1L;
    }
}
