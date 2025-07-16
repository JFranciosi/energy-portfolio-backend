package miesgroup.mies.webdev.Rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Model.Cliente;
import miesgroup.mies.webdev.Model.CostoEnergia;
import miesgroup.mies.webdev.Rest.Model.ClienteRequest;
import miesgroup.mies.webdev.Rest.Model.ClienteResponse;
import miesgroup.mies.webdev.Service.ClienteService;
import miesgroup.mies.webdev.Service.CostoEnergiaService;
import miesgroup.mies.webdev.Service.SessionService;

import java.util.List;
import java.util.Map;

@Path("/cliente")
public class ClienteResource {

    private final ClienteService clienteService;
    private final SessionService sessionService;
    private final CostoEnergiaService costoEnergiaService;

    public ClienteResource(ClienteService clienteService,
                           SessionService sessionService,
                           CostoEnergiaService costoEnergiaService) {
        this.clienteService = clienteService;
        this.sessionService = sessionService;
        this.costoEnergiaService = costoEnergiaService;
    }

    // Endpoint per creazione utente
    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCliente(ClienteRequest clienteRequest) {
        try {
            Cliente clienteCreato = clienteService.createCliente(clienteRequest);
            return Response.status(Response.Status.CREATED)
                    .entity(clienteService.parseResponse(clienteCreato))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Errore nella creazione dell'utente: " + e.getMessage())
                    .build();
        }
    }

    // Recupera il cliente basato sul sessionId
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCliente(@CookieParam("SESSION_COOKIE") Integer sessionId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }
        int idUtente = sessionService.trovaUtentebBySessione(sessionId);
        Cliente cliente = clienteService.getCliente(idUtente);

        if (cliente == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Cliente non trovato")
                    .build();
        }

        return Response.ok(clienteService.parseResponse(cliente)).build();
    }

    // Aggiorna il profilo personale (utente loggato)
    @PUT
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfiloPersonale(Map<String, Object> updateData,
                                           @CookieParam("SESSION_COOKIE") Integer sessionId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }
        int idUtente = sessionService.trovaUtentebBySessione(sessionId);
        if (idUtente == 0) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }

        if (updateData == null || updateData.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Nessun dato da aggiornare")
                    .build();
        }

        Cliente clienteCorrente = clienteService.getCliente(idUtente);
        if (clienteCorrente == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Cliente non trovato")
                    .build();
        }

        for (Map.Entry<String, Object> entry : updateData.entrySet()) {
            String field = entry.getKey();
            if ("id".equalsIgnoreCase(field)) continue;

            String newValue = entry.getValue() == null
                    ? null
                    : entry.getValue().toString();
            String currentValue = clienteCorrente.getTipologia();

            if ("tipologia".equalsIgnoreCase(field)) {
                if (!newValue.equalsIgnoreCase(currentValue)) {
                    // Blocca sempre se si tenta di cambiare da o verso Admin
                    if ("Admin".equalsIgnoreCase(currentValue) ||
                            "Admin".equalsIgnoreCase(newValue)) {
                        return Response.status(Response.Status.FORBIDDEN)
                                .entity("Non è possibile modificare la tipologia da o verso Admin")
                                .build();
                    }
                }
            }

            boolean updated = clienteService.updateCliente(idUtente, field, newValue);
            if (!updated) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Aggiornamento fallito per il campo: " + field)
                        .build();
            }
        }

        Cliente clienteAggiornato = clienteService.getCliente(idUtente);
        return Response.ok(clienteService.parseResponse(clienteAggiornato)).build();
    }

    // Aggiorna un cliente specifico (solo admin)
    @PUT
    @Path("/update/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateClienteById(@PathParam("id") int idUtente,
                                      Map<String, Object> updateData,
                                      @CookieParam("SESSION_COOKIE") Integer sessionId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }
        int idUtenteSession = sessionService.trovaUtentebBySessione(sessionId);
        if (idUtenteSession == 0) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }

        Cliente sessionUser = clienteService.getCliente(idUtenteSession);
        if (sessionUser == null || !"Admin".equalsIgnoreCase(sessionUser.getTipologia())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Permessi insufficienti")
                    .build();
        }

        if (updateData == null || updateData.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Nessun dato da aggiornare")
                    .build();
        }

        Cliente clienteCorrente = clienteService.getCliente(idUtente);
        if (clienteCorrente == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Cliente non trovato")
                    .build();
        }

        for (Map.Entry<String, Object> entry : updateData.entrySet()) {
            String field = entry.getKey();
            if ("id".equalsIgnoreCase(field)) continue;

            String newValue = entry.getValue() == null
                    ? null
                    : entry.getValue().toString();

            // Blocca modifica tipologia se utente è Admin
            if ("tipologia".equalsIgnoreCase(field)) {
                String currentValue = clienteCorrente.getTipologia();
                if ("Admin".equalsIgnoreCase(currentValue) &&
                        !newValue.equalsIgnoreCase(currentValue)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("Non è possibile modificare la tipologia di un utente Admin")
                            .build();
                }
            }

            boolean updated = clienteService.updateCliente(idUtente, field, newValue);
            if (!updated) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Aggiornamento fallito per il campo: " + field)
                        .build();
            }
        }

        Cliente clienteAggiornato = clienteService.getCliente(idUtente);
        ClienteResponse responseDto = clienteService.parseResponse(clienteAggiornato);
        return Response.ok(responseDto).build();
    }

    // Elimina cliente (solo admin) — ora elimina anche le sessioni collegate
    @DELETE
    @Path("/delete/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCliente(@PathParam("id") int id,
                                  @CookieParam("SESSION_COOKIE") Integer sessionId) {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }
        int idUtenteSession = sessionService.trovaUtentebBySessione(sessionId);
        if (idUtenteSession == 0) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }

        Cliente sessionUser = clienteService.getCliente(idUtenteSession);
        if (sessionUser == null || !"Admin".equalsIgnoreCase(sessionUser.getTipologia())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Permessi insufficienti")
                    .build();
        }

        sessionService.deleteSessionsByUserId(id);
        boolean deleted = clienteService.deleteCliente(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Cliente non trovato o impossibile eliminare")
                    .build();
        }
        return Response.ok("Cliente eliminato con successo").build();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllClienti() {
        List<Cliente> utenti = clienteService.getAllClienti();
        List<ClienteResponse> responseList = utenti.stream()
                .map(clienteService::parseResponse)
                .toList();
        return Response.ok(responseList).build();
    }

    // Ritorna SEMPRE la lista dei costi (vuota o piena), mai 404
    @GET
    @Path("/costi-energia")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCostiEnergia(@CookieParam("SESSION_COOKIE") int sessionId) {
        Integer idUtente = sessionService.trovaUtentebBySessione(sessionId);
        if (idUtente == null || idUtente == 0) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }
        List<CostoEnergia> costi = costoEnergiaService.getCostiEnergia(idUtente);
        return Response.ok(costi).build();
    }

    @POST
    @Path("/costi-energia/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertCostoEnergia(@CookieParam("SESSION_COOKIE") int sessionId,
                                       List<CostoEnergia> costiEnergia) {
        Integer idUtente = sessionService.trovaUtentebBySessione(sessionId);
        if (idUtente == null || idUtente == 0) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Sessione non valida")
                    .build();
        }

        Cliente cliente = clienteService.getCliente(idUtente);
        if (cliente == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Cliente non trovato")
                    .build();
        }

        for (CostoEnergia costo : costiEnergia) {
            costo.setCliente(cliente);
            if (costo.getNomeCosto() == null || costo.getCostoEuro() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Nome costo e costo in euro sono obbligatori per ogni elemento")
                        .build();
            }
            costoEnergiaService.persistOrUpdateCostoEnergia(costo);
        }

        return Response.ok().build();
    }
}
