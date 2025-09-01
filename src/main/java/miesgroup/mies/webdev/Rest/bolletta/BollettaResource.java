package miesgroup.mies.webdev.Rest.bolletta;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.file.PDFFile;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Rest.Model.FileUploadForm;
import miesgroup.mies.webdev.Rest.Model.SingleFile;
import miesgroup.mies.webdev.Service.file.FileService;
import miesgroup.mies.webdev.Service.bolletta.PodService;
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
    public Response uploadAndProcessFileA2A(@MultipartForm FileUploadForm form, @CookieParam("SESSION_COOKIE") int idSessione) {
        try {
            System.out.println("[DEBUG] Inizio uploadAndProcessFileA2A");

            // 1. Recupera nome e contenuto del file
            String fileName = form.getFileName();
            byte[] fileData = form.getFileData();
            System.out.println("[DEBUG] File ricevuto: " + fileName + ", dimensione: " + (fileData != null ? fileData.length : 0));

            // 2. Recupera l'idUser dalla sessione
            Integer idUser = sessioneRepo.getUserIdBySessionId(idSessione);
            System.out.println("[DEBUG] idSessione: " + idSessione + ", idUser: " + idUser);
            if (idUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("<error>Utente non autenticato (sessione non valida)</error>")
                        .build();
            }

            // 3. Converte il file PDF caricato in un documento XML
            Document xmlDocument = fileService.convertPdfToXml(fileData);
            System.out.println("[DEBUG] Conversione PDF -> XML completata");

            // 4. Converte il documento XML in una stringa
            String xmlString = fileService.convertDocumentToString(xmlDocument);
            byte[] xmlData = xmlString.getBytes();
            System.out.println("[DEBUG] Conversione XML -> stringa completata. Lunghezza byte: " + xmlData.length);

            // 5. Estrae l'ID del POD
            String idPod = podService.extractValuesFromXml(xmlData, idSessione);
            System.out.println("[DEBUG] idPod estratto (RAW): '" + idPod + "'");
            System.out.println("[DEBUG] Lunghezza: " + idPod.length());

            for (int i = 0; i < idPod.length(); i++) {
                System.out.println("[DEBUG] Char " + i + ": '" + idPod.charAt(i) + "' (int: " + (int) idPod.charAt(i) + ")");
            }

            if (idPod == null || idPod.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Impossibile estrarre l'ID del POD dal documento XML.")
                        .build();
            }

            fileService.processaBolletta(xmlData,xmlDocument, idPod);
/*
            // 6. Estrai i dati della bolletta
            String nomeB = fileService.extractValuesFromXmlA2A(xmlData, idPod);
            System.out.println("[DEBUG] nomeB (nome bolletta): " + nomeB);

            if (nomeB == null || nomeB.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Impossibile estrarre i dati della bolletta dal documento XML.")
                        .build();
            }

            // 7. Verifica dati bolletta
            fileService.verificaA2APiuMesi(nomeB);
            System.out.println("[DEBUG] verificaA2APiuMesi completata");

            // 8. Controllo ricalcoli
            //fileService.controlloRicalcoliInBolletta(xmlData, idPod, nomeB, idSessione);
            System.out.println("[DEBUG] controlloRicalcoliInBolletta completato");
            */

            // 9. Salva il file solo dopo tutte le verifiche
            int idFile = fileService.saveFile(fileName, fileData);
            System.out.println("[DEBUG] File salvato con idFile: " + idFile);


            // 10. Associa l'ID POD al file
            fileService.abbinaPod(idFile, idPod);
            System.out.println("[DEBUG] POD associato al file");

            // 11. Restituisci risposta
            System.out.println("[DEBUG] Fine uploadAndProcessFileA2A - Successo");
            return Response.status(Response.Status.OK)
                    .entity("<message>File caricato e processato con successo.</message>")
                    .build();


        } catch (IOException | ParserConfigurationException | TransformerException | SQLException e) {
            System.out.println("[ERROR] Errore tecnico: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>" + e.getMessage() + "</error>")
                    .build();

        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] Input non valido: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("<error>Input non valido: " + e.getMessage() + "</error>")
                    .build();
        } catch (Exception e) {
            System.out.println("[ERROR] Errore generico: " + e.getMessage());
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
                fileService.processaBolletta(xmlData,xmlDocument, idPod);

                if (idPod == null || idPod.isEmpty()) {
                    result.put("status", "error");
                    result.put("message", "Impossibile estrarre l'ID del POD dal documento XML.");
                    results.add(result);
                    continue;
                }
/*
                // --- AGGIUNGI idUser qui ---
                String nomeB = fileService.extractValuesFromXmlA2A(xmlData, idPod);
                if (nomeB == null || nomeB.isEmpty()) {
                    result.put("status", "error");
                    result.put("message", "Impossibile estrarre i dati della bolletta dal documento XML.");
                    results.add(result);
                    continue;
                }

                fileService.verificaA2APiuMesi(nomeB);
                //fileService.controlloRicalcoliInBolletta(xmlData, idPod, nomeB, idSessione);
                fileService.abbinaPod(idFile, idPod);

                result.put("status", "success");
                result.put("message", "File caricato e processato con successo.");

 */
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
    @Produces("application/pdf")
    public Response viewPdf(@PathParam("id") int id) {
        PDFFile pdfFile = fileService.getFile(id);
        if (pdfFile == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        byte[] fileData = pdfFile.getFileData();
        String fileName = pdfFile.getFileName();

        return Response.ok(fileData, "application/pdf")
                .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
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
