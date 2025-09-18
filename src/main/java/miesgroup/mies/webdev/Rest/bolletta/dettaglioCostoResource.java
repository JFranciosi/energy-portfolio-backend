package miesgroup.mies.webdev.Rest.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.bolletta.dettaglioCosto;
import miesgroup.mies.webdev.Rest.Model.dettaglioCostoDTO;
import miesgroup.mies.webdev.Rest.Model.FormData;
import miesgroup.mies.webdev.Service.bolletta.dettaglioCostoService;
import miesgroup.mies.webdev.Service.cliente.SessionService;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

@Path("/costi")
public class dettaglioCostoResource {
    private final dettaglioCostoService dettaglioCostoService;
    private final SessionService sessionService;

    public dettaglioCostoResource(dettaglioCostoService costiService, SessionService sessionService) {
        this.dettaglioCostoService = costiService;
        this.sessionService = sessionService;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCosti(
            @CookieParam("SESSION_COOKIE") Integer idSessione,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {

        Cliente c = sessionService.trovaUtenteCategoryBySessione(idSessione);
        if (!"Admin".equals(c.getTipologia())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // Calcolo offset
        int offset = page * size;

        // Query con paginazione
        PanacheQuery<dettaglioCosto> query = dettaglioCostoService.getQueryAllCosti(idSessione);
        List<dettaglioCosto> paginatedList = query
                .range(offset, offset + size - 1)
                .list();

        // Opzionale: includere anche info su totale pagine, elementi, pagina corrente, ecc.
        long total = query.count();

        Map<String, Object> response = new HashMap<>();
        response.put("contenuto", paginatedList);
        response.put("pagina", page);
        response.put("dimensione", size);
        response.put("totaleElementi", total);
        response.put("totalePagine", (int) Math.ceil((double) total / size));

        return Response.ok(response).build();
    }


    @POST
    @Path("/aggiungi")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCosto(dettaglioCosto dettaglioCosto) throws SQLException {
        boolean verifica = dettaglioCostoService.createDettaglioCosto(
                dettaglioCosto.getItem(),                 // item
                dettaglioCosto.getUnitaMisura(),          // unitaMisura
                dettaglioCosto.getModality(),             // modality
                dettaglioCosto.getCheckModality(),        // checkModality
                dettaglioCosto.getCosto(),                // costo
                dettaglioCosto.getCategoria(),            // categoria
                dettaglioCosto.getIntervalloPotenza(),    // intervalloPotenza
                dettaglioCosto.getClasseAgevolazione(),   // classeAgevolazione
                dettaglioCosto.getAnnoRiferimento(),      // annoRiferimento
                dettaglioCosto.getItemDescription()       // itemDescription
        );
        if (!verifica) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/{IntervalloPotenza}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public dettaglioCosto getSum(@PathParam("IntervalloPotenza") String intervalloPotenza) throws SQLException {
        return dettaglioCostoService.getSum(intervalloPotenza);
    }

    @Path("/delete/{id}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCosto(@CookieParam("SESSION_COOKIE") int idSessione, @PathParam("id") int id) throws SQLException {
        Cliente c = sessionService.trovaUtenteCategoryBySessione(idSessione);
        if (c.getTipologia().equals("Admin")) {
            dettaglioCostoService.deleteCosto(id);
        } else {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadExcelFile(@MultipartForm FormData formData) {
        try {
            InputStream excelInputStream = formData.getFile();
            dettaglioCostoService.processExcelFile(excelInputStream);
            return Response.ok("File elaborato con successo").build();
        } catch (Exception e) {
            return Response.serverError().entity("Errore: " + e.getMessage()).build();
        }
    }


    @Path("/update")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadCosto(dettaglioCosto dettaglioCosto) {
        boolean verifica = dettaglioCostoService.updateDettaglioCosto(
                dettaglioCosto.getItem(),
                dettaglioCosto.getUnitaMisura(),
                dettaglioCosto.getModality(),
                dettaglioCosto.getCheckModality(),
                dettaglioCosto.getCosto(),
                dettaglioCosto.getCategoria(),
                dettaglioCosto.getIntervalloPotenza(),
                dettaglioCosto.getClasseAgevolazione(),
                dettaglioCosto.getAnnoRiferimento(),
                dettaglioCosto.getItemDescription()
        );

        if (!verifica) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok("Update avvenuto con successo").build();
    }

    @Path("/downloadExcel")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response downloadExcel() {
        try {
            ByteArrayOutputStream out = dettaglioCostoService.generateExcelFile();
            byte[] excelData = out.toByteArray(); // Salva i dati prima di chiudere il flusso
            return Response.ok(excelData)
                    .header("Content-Disposition", "attachment; filename=costi.xlsx")
                    .build();
        } catch (Exception e) {
            return Response.serverError().entity("Errore nella generazione del file Excel: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/delete-ids")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCostiByIds(List<Long> ids, @CookieParam("SESSION_COOKIE") int idSessione) {
        if (ids == null || ids.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Lista di ID vuota o nulla")
                    .build();
        }
        // Verifica se l'utente ha i permessi per eliminare
        Cliente c = sessionService.trovaUtenteCategoryBySessione(idSessione);
        if (!"Admin".equals(c.getTipologia())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Long deletedCount = dettaglioCostoService.deleteIds(ids);
        return Response.ok(Map.of("deleted", deletedCount)).build();
    }

    @GET
    @Path("/filtrati")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getCostiFiltrati(
            @QueryParam("categoria") Optional<String> categoria,
            @QueryParam("anno") Optional<String> anno,
            @QueryParam("annoRiferimento") Optional<String> annoRif,
            @QueryParam("intervalloPotenza") Optional<String> intervalloPotenza,
            @QueryParam("id") Optional<Integer> id,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size
    ) {
        // Recupera la lista dei costi filtrati con paginazione
        List<dettaglioCostoDTO> dtoList = dettaglioCostoService.getCostiFiltrati(categoria, anno, annoRif, intervalloPotenza, id, page, size);

        // Recupera il totale degli elementi filtrati
        long total = dettaglioCostoService.countCostiFiltrati(categoria, anno, annoRif, intervalloPotenza);
        int totalPages = (int) Math.ceil((double) total / size);

        Map<String, Object> response = new HashMap<>();
        response.put("contenuto", dtoList);
        response.put("pagina", page);
        response.put("dimensione", size);
        response.put("totaleElementi", total);
        response.put("totalePagine", totalPages);

        return Response.ok(response).build();
    }

    @GET
    @Path("/anniRiferimento")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAnniRiferimento() {
        try {
            List<String> anni = dettaglioCostoService.getAnniRiferimento();
            return Response.ok(anni).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Errore nel recupero degli anni di riferimento: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/intervalliPotenza")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntervalliPotenza() {
        try {
            List<String> intervalli = dettaglioCostoService.getIntervalliPotenza();
            return Response.ok(intervalli).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Errore nel recupero degli intervalli di potenzad: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/categorie")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategorie() {
        try {
            List<String> categorie = dettaglioCostoService.getCategorie();
            return Response.ok(categorie).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Errore nel recupero delle categorie: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/classeAgevolazione")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClasseAgevolazione() {
        try {
            List<String> categorie = dettaglioCostoService.getClasseAgevolazione();
            return Response.ok(categorie).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Errore nel recupero delle classe di agevolazione: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/unitaMisure")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitaMisure() {
        try {
            List<String> categorie = dettaglioCostoService.getUnitaMisure();
            return Response.ok(categorie).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Errore nel recupero delle unità di misura: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/item")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItem() {
        try {
            List<String> categorie = dettaglioCostoService.getItem();
            return Response.ok(categorie).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Errore nel recupero degli item: " + e.getMessage()).build();
        }
    }
}