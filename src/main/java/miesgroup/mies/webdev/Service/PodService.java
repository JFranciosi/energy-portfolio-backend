package miesgroup.mies.webdev.Service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.BollettaPod;
import miesgroup.mies.webdev.Model.Cliente;
import miesgroup.mies.webdev.Model.PDFFile;
import miesgroup.mies.webdev.Model.Pod;
import miesgroup.mies.webdev.Repository.*;
import miesgroup.mies.webdev.Rest.Exception.NotYourPodException;
import miesgroup.mies.webdev.Rest.Model.PodResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class PodService {

    private final PodRepo podRepo;
    private final SessionService sessionService;
    private final SessionRepo sessionRepo;
    private final ClienteRepo clienteRepo;
    private final BudgetRepo budgetRepo;
    private final BollettaRepo bollettaRepo;

    public PodService(
            PodRepo podRepo,
            SessionService sessionService,
            SessionRepo sessionRepo,
            ClienteRepo clienteRepo,
            FixingRepo fixingRepo,
            BudgetRepo budgetRepo,
            BollettaRepo bollettaRepo
    ) {
        this.podRepo = podRepo;
        this.sessionService = sessionService;
        this.sessionRepo = sessionRepo;
        this.clienteRepo = clienteRepo;
        this.budgetRepo = budgetRepo;
        this.bollettaRepo = bollettaRepo;
    }

    // ————————— XML / creazione POD —————————

    @Transactional
    public String extractValuesFromXml(byte[] xmlData, int sessione) {
        List<Double> vals = new ArrayList<>();
        String idPod = "";
        int idUtente = sessionService.trovaUtentebBySessione(sessione);
        String fornitore = "", sede = "", cap = "", citta = "";
        boolean exists = false;

        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(new ByteArrayInputStream(xmlData));

            NodeList lines = doc.getElementsByTagName("Line");
            // REGEX PER TROVARE IL POD OVUNQUE
            Pattern podPattern = Pattern.compile("IT[0-9A-Z]{13}");

            for (int i = 0; i < lines.getLength() && !exists; i++) {
                Node node = lines.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                String txt = node.getTextContent();

                // CERCA IL POD IN QUALUNQUE LINEA
                Matcher matcher = podPattern.matcher(txt);
                if (matcher.find()) {
                    idPod = matcher.group();
                } else if (i + 1 < lines.getLength()) {
                    // fallback: anche la linea dopo
                    String nextLine = ((Element) lines.item(i + 1)).getTextContent().trim();
                    matcher = podPattern.matcher(nextLine);
                    if (matcher.find()) idPod = matcher.group();
                }

                // esiste già?
                if (!idPod.isEmpty() && podRepo.verificaSePodEsiste(idPod, idUtente) != null) {
                    exists = true;
                    return idPod;
                }

                // estrai fornitore
                if (txt.contains("SEGNALAZIONE GUASTI ELETTRICITA")) {
                    fornitore = ((Element) lines.item(i + 2)).getTextContent().trim();
                }
                // estrai tensione/potenze
                if ((txt.contains("Tensione di alimentazione") ||
                        txt.contains("Potenza impegnata") ||
                        txt.contains("Potenza disponibile"))
                        && vals.size() < 3) {
                    String raw = ((Element) lines.item(i + 1))
                            .getTextContent()
                            .replaceAll("^(\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}\\s+){1,2}", "")
                            .replaceAll("[^\\d,\\.\\-]", "")
                            .replace(".", "")
                            .replace(",", ".");
                    try {
                        vals.add(Double.parseDouble(raw));
                    } catch (Exception ignored) {
                    }
                }
                // estrai indirizzo
                if (txt.contains("Indirizzo di fornitura")) {
                    StringBuilder sb = new StringBuilder();
                    int j = i + 1;
                    while (j < lines.getLength()) {
                        Node nn = lines.item(j++);
                        if (nn.getNodeType() != Node.ELEMENT_NODE) break;
                        String part = nn.getTextContent().trim();
                        if (part.isEmpty() || part.contains("Tipologia cliente")) break;
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(part);
                    }
                    String[] sp = sb.toString().split(" - ");
                    if (sp.length == 2) {
                        sede = sp[0].trim();
                        String[] cc = sp[1].split(" ", 2);
                        cap = cc[0];
                        citta = cc.length > 1 ? cc[1] : "";
                    }
                }
            }
            // SOLO SE È STATO TROVATO UN POD VALIDO
            if (!idPod.isEmpty()) {
                creaPod(vals, idUtente, idPod, fornitore, citta, cap, sede);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return idPod;
    }

    @Transactional
    public void creaPod(List<Double> vals, int idUtente, String idPod,
                        String fornitore, String nazione, String cap, String sede) {
        Cliente c = clienteRepo.findById(idUtente);
        Pod p = new Pod();
        p.setUtente(c);
        p.setId(idPod);
        p.setFornitore(fornitore);
        p.setTensioneAlimentazione(vals.size() > 0 ? vals.get(0) : 0.0);
        p.setPotenzaImpegnata(vals.size() > 1 ? vals.get(1) : 0.0);
        p.setPotenzaDisponibile(vals.size() > 2 ? vals.get(2) : 0.0);
        p.setSede(sede);
        p.setNazione(nazione);
        p.setCap(cap);
        double t = p.getTensioneAlimentazione();
        p.setTipoTensione(t <= 1000 ? "Bassa" : t <= 35000 ? "Media" : "Alta");
        podRepo.persist(p);
    }

    @Transactional
    public List<PodResponse> tutti(int sessione) {
        return podRepo.findAll(sessionRepo.find(sessione))
                .stream().map(PodResponse::new).collect(Collectors.toList());
    }

    @Transactional
    public Pod getPod(String id, int sessione) {
        return podRepo.cercaIdPod(id, sessionRepo.find(sessione));
    }

    @Transactional
    public void addSedeNazione(String idPod, String sede, String nazione, int sessione) {
        podRepo.aggiungiSedeNazione(idPod, sede, nazione, sessionRepo.find(sessione));
    }

    @Transactional
    public void modificaSedeNazione(String idPod, String sede, String nazione, int sessione) {
        int ut = sessionService.trovaUtentebBySessione(sessione);
        if (ut != podRepo.findById(idPod).getUtente().getId())
            throw new NotYourPodException("Non puoi modificare il POD di un altro utente");
        podRepo.modificaSedeNazione(idPod, sede, nazione);
    }

    @Transactional
    public void addSpread(String idPod, Double spread, int sessione) {
        int ut = sessionService.trovaUtentebBySessione(sessione);
        if (ut != podRepo.findById(idPod).getUtente().getId())
            throw new NotYourPodException("Non puoi modificare il POD di un altro utente");
        podRepo.aggiungiSpread(idPod, spread);
    }

    @Transactional
    public List<PDFFile> getBollette(List<Pod> elenco) {
        return podRepo.getBollette(elenco);
    }

    @Transactional
    public List<Pod> findPodByIdUser(int sessione) {
        return podRepo.findPodByIdUser(sessionRepo.find(sessione));
    }

    // ———————— NUOVE API ————————

    /**
     * Per ciascun mese, media fasce energetiche e oneri su tutte le sedi
     */
    @Transactional
    public List<Map<String, Object>> getPrezziEnergiaTutti(int anno, int sessione) {
        List<Pod> pods = findPodByIdUser(sessione);
        List<Map<String, Object>> out = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            double sumP = 0, sumO = 0;
            int cnt = 0;
            for (Pod p : pods) {
                List<BollettaPod> bl = bollettaRepo.list(
                        "idPod = ?1 AND anno = ?2 AND mese = ?3",
                        p.getId(), String.valueOf(anno), String.valueOf(m)
                );
                for (BollettaPod b : bl) {
                    if (b.getSpeseEnergia() != null && b.getTotAttiva() != null && b.getTotAttiva() > 0) {
                        sumP += b.getSpeseEnergia() / b.getTotAttiva();
                        sumO += b.getOneri() != null ? b.getOneri() : 0;
                        cnt++;
                    }
                }
            }
            if (cnt > 0) {
                sumP /= cnt;
                sumO /= cnt;
            }
            Map<String, Object> mappa = new HashMap<>();
            mappa.put("mese", m);
            mappa.put("prezzoEnergia", sumP);
            mappa.put("oneri", sumO);
            out.add(mappa);
        }
        return out;
    }
}