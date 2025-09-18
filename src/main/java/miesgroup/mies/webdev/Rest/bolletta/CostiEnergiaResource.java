package miesgroup.mies.webdev.Rest.bolletta;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;
import miesgroup.mies.webdev.Model.bolletta.CostiEnergia;
import miesgroup.mies.webdev.Model.bolletta.CostiPeriodi;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Rest.bolletta.DTO.CostiEnergiaRequest;
import miesgroup.mies.webdev.Rest.bolletta.DTO.CostiEnergiaResponse;
import miesgroup.mies.webdev.Service.bolletta.CostiEnergiaService;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/costi-energia")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CostiEnergiaResource {

    private final CostiEnergiaService costiEnergiaService;

    @Inject
    SessionRepo sessionRepo;

    public CostiEnergiaResource(CostiEnergiaService costiEnergiaService) {
        this.costiEnergiaService = costiEnergiaService;
    }

    /**
     * Recupera i costi energetici di un cliente per un anno specifico
     */
    @GET
    @Path("/{year}")
    public Response getCostiByClienteAndAnno(
            @PathParam("year") Integer year,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            // Verifica sessione
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            Optional<CostiEnergia> costi = costiEnergiaService.getCostiByClienteAndAnno(userId, year);
            if (costi.isPresent()) {
                return Response.ok(new CostiEnergiaResponse(costi.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Nessun costo trovato per il cliente e l'anno specificati"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno del server: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Recupera tutti i costi di un cliente
     */
    @GET
    public Response getAllCostiByCliente(
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            List<CostiEnergia> costi = costiEnergiaService.getAllCostiByCliente(userId);
            List<CostiEnergiaResponse> response = costi.stream()
                    .map(CostiEnergiaResponse::new)
                    .toList();
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Crea o aggiorna i costi energetici
     */
    @POST
    public Response saveOrUpdateCostiEnergia(
            @Valid CostiEnergiaRequest request,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {

        System.out.println("========== [DEBUG] saveOrUpdateCostiEnergia ==========");

        // stampa grezza dell’oggetto request
        System.out.println("[DEBUG] Request ricevuta: " + request);

        // stampa singoli campi per capire se arrivano null o con valore
        System.out.println("[DEBUG] Anno: " + request.getYear());
        System.out.println("[DEBUG] TipoPrezzo: " + request.getTipoPrezzo());
        System.out.println("[DEBUG] TipoTariffa: " + request.getTipoTariffa());
        System.out.println("[DEBUG] ClientId: " + request.getClientId());

        System.out.println("[DEBUG] costF0: " + request.getCostF0());
        System.out.println("[DEBUG] costF1: " + request.getCostF1());
        System.out.println("[DEBUG] costF2: " + request.getCostF2());
        System.out.println("[DEBUG] costF3: " + request.getCostF3());

        System.out.println("[DEBUG] spreadF1: " + request.getSpreadF1());
        System.out.println("[DEBUG] spreadF2: " + request.getSpreadF2());
        System.out.println("[DEBUG] spreadF3: " + request.getSpreadF3());

        System.out.println("[DEBUG] percentageVariable: " + request.getPercentageVariable());

        try {
            System.out.println("[DEBUG] Recupero userId dalla sessione con id: " + sessionId);
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            System.out.println("[DEBUG] userId recuperato: " + userId);

            if (userId == null) {
                System.out.println("[DEBUG] Sessione non valida, ritorno 401");
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            // Imposta il clientId
            request.setClientId(userId);
            System.out.println("[DEBUG] ClientId settato su request: " + request.getClientId());

            // Conversione in entità
            CostiEnergia costiEnergia = request.toEntity();
            System.out.println("[DEBUG] Entità convertita: " + costiEnergia);

            // Stampa i valori principali dell’entità
            System.out.println("[DEBUG] Entità - anno: " + costiEnergia.getYear());
            System.out.println("[DEBUG] Entità - tipoPrezzo: " + costiEnergia.getTipoPrezzo());
            System.out.println("[DEBUG] Entità - tipoTariffa: " + costiEnergia.getTipoTariffa());
            System.out.println("[DEBUG] Entità - costF0: " + costiEnergia.getCostF0());
            System.out.println("[DEBUG] Entità - costF1: " + costiEnergia.getCostF1());
            System.out.println("[DEBUG] Entità - costF2: " + costiEnergia.getCostF2());
            System.out.println("[DEBUG] Entità - costF3: " + costiEnergia.getCostF3());
            System.out.println("[DEBUG] Entità - spreadF1: " + costiEnergia.getSpreadF1());
            System.out.println("[DEBUG] Entità - spreadF2: " + costiEnergia.getSpreadF2());
            System.out.println("[DEBUG] Entità - spreadF3: " + costiEnergia.getSpreadF3());
            System.out.println("[DEBUG] Entità - percentageVariable: " + costiEnergia.getPercentageVariable());

            // Salvataggio
            System.out.println("[DEBUG] Salvataggio nel service...");
            CostiEnergia saved = costiEnergiaService.saveOrUpdateCostiEnergia(costiEnergia);
            System.out.println("[DEBUG] Entità salvata: " + saved);

            System.out.println("========== [DEBUG] Fine OK ==========");

            return Response.status(Response.Status.CREATED)
                    .entity(new CostiEnergiaResponse(saved))
                    .build();

        } catch (IllegalArgumentException e) {
            System.out.println("========== [DEBUG] IllegalArgumentException ==========");
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            System.out.println("========== [DEBUG] Exception ==========");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }


    /**
     * Elimina i costi di un cliente per un anno
     */
    @DELETE
    @Path("/{year}")
    public Response deleteCostiByClienteAndAnno(
            @PathParam("year") Integer year,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            boolean deleted = costiEnergiaService.deleteCostiByClienteAndAnno(userId, year);
            if (deleted) {
                return Response.ok(Map.of("message", "Costi eliminati con successo")).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Nessun costo trovato da eliminare"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Recupera gli anni configurati per un cliente
     */
    @GET
    @Path("/anni")
    public Response getAnniConfigurati(
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            List<Integer> anni = costiEnergiaService.getAnniConfigurati(userId);
            return Response.ok(Map.of("anni", anni)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Recupera i periodi dinamici per un costo energia specifico
     */
    @GET
    @Path("/{energyCostId}/periodi-dinamici")
    public Response getPeriodiDinamici(
            @PathParam("energyCostId") Integer energyCostId,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            System.out.println(">>> [getPeriodiDinamici] INIZIO chiamata");
            System.out.println(">>> Parametri ricevuti:");
            System.out.println("    energyCostId = " + energyCostId);
            System.out.println("    sessionId    = " + sessionId);

            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            System.out.println(">>> userId trovato da sessionRepo = " + userId);

            if (userId == null) {
                System.out.println(">>> Sessione non valida, ritorno 401");
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            System.out.println(">>> Chiamo costiEnergiaService.getPeriodiDinamici con:");
            System.out.println("    energyCostId = " + energyCostId + ", userId = " + userId);

            List<CostiPeriodi> periodi = costiEnergiaService.getPeriodiDinamici(energyCostId, userId);

            if (periodi == null) {
                System.out.println(">>> Il service ha restituito NULL");
            } else {
                System.out.println(">>> Il service ha restituito " + periodi.size() + " periodi");
                for (int i = 0; i < periodi.size(); i++) {
                    System.out.println("    [" + i + "] " + periodi.get(i));
                }
            }

            System.out.println(">>> [getPeriodiDinamici] FINE chiamata con successo");
            return Response.ok(periodi).build();
        } catch (Exception e) {
            System.out.println(">>> [getPeriodiDinamici] ERRORE: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }


    /**
     * Verifica se esistono costi per cliente e anno
     */
    @GET
    @Path("/{year}/exists")
    public Response existsCostiForClienteAndAnno(
            @PathParam("year") Integer year,
            @CookieParam("SESSION_COOKIE") Integer sessionId) {
        try {
            Integer userId = sessionRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Sessione non valida"))
                        .build();
            }

            boolean exists = costiEnergiaService.existsCostiForClienteAndAnno(userId, year);
            return Response.ok(Map.of("exists", exists)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Errore interno: " + e.getMessage()))
                    .build();
        }
    }
}
