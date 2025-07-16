package miesgroup.mies.webdev.Rest;

import com.microsoft.aad.msal4j.UserIdentifier;
import io.vertx.mutiny.ext.auth.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import miesgroup.mies.webdev.Model.BollettaPod;
import miesgroup.mies.webdev.Model.Cliente;
import miesgroup.mies.webdev.Model.PDFFile;
import miesgroup.mies.webdev.Repository.ClienteRepo;
import miesgroup.mies.webdev.Repository.SessionRepo;
import miesgroup.mies.webdev.Rest.Model.FileUploadForm;
import miesgroup.mies.webdev.Rest.Model.SingleFile;
import miesgroup.mies.webdev.Service.FileService;
import miesgroup.mies.webdev.Service.PodService;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import java.sql.SQLException;
import org.w3c.dom.Document;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.*;

@Path("/files")
public class BollettaResource {

    private final FileService fileService;
    private final PodService podService;

    public BollettaResource(FileService fileService, PodService podService) {
        this.fileService = fileService;
        this.podService = podService;
    }

    @Inject ClienteRepo clienteRepo;


    @Path("/upload")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadAndProcessFileA2A(
            @MultipartForm FileUploadForm form,
            @CookieParam("SESSION_COOKIE") int idSessione) {
        try {
            // 1. Recupera idUser dalla tabella sessione tramite idSessione
            Integer idUser = sessioneRepo.getUserIdBySessionId(idSessione);
            if (idUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("<error>Utente non autenticato (sessione non valida)</error>")
                        .build();
            }

            // 2. Salva il file e ottieni idFile (puoi anche passare idUser se vuoi associare nel DB file)
            int idFile = fileService.saveFile(form.getFileName(), form.getFileData());

            // 3. Converte PDF in XML
            Document xmlDocument = fileService.convertPdfToXml(form.getFileData());

            // 4. Converte documento XML in stringa e byte[]
            String xmlString = fileService.convertDocumentToString(xmlDocument);
            byte[] xmlData = xmlString.getBytes();

            // 5. Estrae idPod dal documento XML (usando dati di sessione)
            String idPod = podService.extractValuesFromXml(xmlData, idSessione);
            if (idPod == null || idPod.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Impossibile estrarre l'ID del POD dal documento XML.")
                        .build();
            }

            // 6. Estrai i dati bolletta dal documento XML e salvali in DB, associando idUser SOLO dove serve
            String nomeB = fileService.extractValuesFromXmlA2A(xmlData, idPod, idUser);
            if (nomeB == null || nomeB.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Impossibile estrarre i dati della bolletta dal documento XML.")
                        .build();
            }

            // 7. Effettua i calcoli di verifica
            fileService.verificaA2APiuMesi(nomeB);

            // 8. Verifica di possibili ricalcoli
            fileService.controlloRicalcoliInBolletta(xmlData, idPod, nomeB, idSessione);

            // 9. Associa il file caricato al POD
            fileService.abbinaPod(idFile, idPod);

            // 10. Restituisci messaggio di successo
            return Response.status(Response.Status.OK)
                    .entity("<message>File caricato e processato con successo.</message>")
                    .build();

        } catch (IOException | ParserConfigurationException | TransformerException | SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>" + e.getMessage() + "</error>")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("<error>Input non valido: " + e.getMessage() + "</error>")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>Si è verificato un errore inaspettato: " + e.getMessage() + "</error>")
                    .build();
        }
    }



    @Path("/upload-multiplo")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadMultipleFiles(
            @MultipartForm FileUploadForm form,
            @CookieParam("SESSION_COOKIE") int idSessione
    ) {
        // Ottieni lista di SingleFile dal form
        List<SingleFile> files = form.getSingleFileList();

        if (files == null || files.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Nessun file inviato"))
                    .build();
        }
        if (files.size() > 12) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Puoi caricare massimo 12 file alla volta"))
                    .build();
        }

        // 1. Recupera idUser dalla sessione PRIMA del ciclo!
        Integer idUser = sessioneRepo.getUserIdBySessionId(idSessione);
        if (idUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Utente non autenticato (sessione non valida)"))
                    .build();
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (SingleFile file : files) {
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getFileName());
            try {
                int idFile = fileService.saveFile(file.getFileName(), file.getFileData());
                Document xmlDocument = fileService.convertPdfToXml(file.getFileData());
                String xmlString = fileService.convertDocumentToString(xmlDocument);
                byte[] xmlData = xmlString.getBytes();
                String idPod = podService.extractValuesFromXml(xmlData, idSessione);

                if (idPod == null || idPod.isEmpty()) {
                    result.put("status", "error");
                    result.put("message", "Impossibile estrarre l'ID del POD dal documento XML.");
                    results.add(result);
                    continue;
                }

                // --- AGGIUNGI idUser qui ---
                String nomeB = fileService.extractValuesFromXmlA2A(xmlData, idPod, idUser);
                if (nomeB == null || nomeB.isEmpty()) {
                    result.put("status", "error");
                    result.put("message", "Impossibile estrarre i dati della bolletta dal documento XML.");
                    results.add(result);
                    continue;
                }

                fileService.verificaA2APiuMesi(nomeB);
                fileService.controlloRicalcoliInBolletta(xmlData, idPod, nomeB, idSessione);
                fileService.abbinaPod(idFile, idPod);

                result.put("status", "success");
                result.put("message", "File caricato e processato con successo.");
            } catch (Exception e) {
                result.put("status", "error");
                result.put("message", e.getMessage());
            }
            results.add(result);
        }

        return Response.ok(results).build();
    }


    @Path("/xml/{id}")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getXml(@PathParam("id") int id) throws IOException, ParserConfigurationException {
        byte[] xmlData = fileService.getXmlData(id);
        Document xmlDoc = fileService.convertPdfToXml(xmlData);
        return Response.ok(xmlDoc, MediaType.APPLICATION_XML).build();
    }

    @Inject
    SessionRepo sessioneRepo;

    @GET
    @Path("/dati")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDati(@CookieParam("SESSION_COOKIE") int sessionId) {
        try {
            // Recupera userId dalla sessione
            Integer userId = sessioneRepo.getUserIdBySessionId(sessionId);
            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Sessione non valida o scaduta\"}")
                        .build();
            }

            Cliente cliente = clienteRepo.getCliente(userId);
            if (cliente == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Cliente non trovato\"}")
                        .build();
            }

            return Response.ok(fileService.getDatiByUserId(userId)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("id") int id) {
        PDFFile pdfFile = fileService.getFile(id);
        if (pdfFile == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        byte[] fileData = pdfFile.getFileData();
        String fileName = pdfFile.getFileName();

        return Response.ok(fileData, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }

    @Path("/env")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getEnvVars() {
        return Map.of(
                "MAILER_HOST", System.getenv("MAILER_HOST"),
                "MAILER_PORT", System.getenv("MAILER_PORT"),
                "MAILER_PASSWORD", System.getenv("MAILER_PASSWORD")
        );
    }
}
