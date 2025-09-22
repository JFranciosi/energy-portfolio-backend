package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.file.PDFFile;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.bolletta.FixingRepo;
import miesgroup.mies.webdev.Repository.bolletta.PodRepo;
import miesgroup.mies.webdev.Repository.budget.BudgetRepo;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Rest.Exception.NotYourPodException;
import miesgroup.mies.webdev.Rest.Model.PodResponse;
import miesgroup.mies.webdev.Service.cliente.ClienteService;
import miesgroup.mies.webdev.Service.cliente.SessionService;
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
import java.util.stream.Collectors;

@ApplicationScoped
public class PodService {

    private final PodRepo podRepo;
    private final SessionService sessionService;
    private final SessionRepo sessionRepo;
    private final ClienteRepo clienteRepo;
    private final BudgetRepo budgetRepo;
    private final BollettaPodRepo bollettaRepo;

    @Inject
    private ClienteService clienteService;

    public PodService(
            PodRepo podRepo,
            SessionService sessionService,
            SessionRepo sessionRepo,
            ClienteRepo clienteRepo,
            FixingRepo fixingRepo,
            BudgetRepo budgetRepo,
            BollettaPodRepo bollettaRepo
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
        String periodFattur = null; // ← nuovo campo estratto
        boolean exists = false;
        String classeAgevolazione = null; // nuovo campo per Decreto Energivori

        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(new ByteArrayInputStream(xmlData));
            NodeList lines = doc.getElementsByTagName("Line");

            // ---------- Scansione linee ----------
            for (int i = 0; i < lines.getLength(); i++) {
                Node node = lines.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                String txt = safeText(node);

                // 1) Individua "POD (punto di prelievo)"
                if (idPod.isEmpty() && containsIgnoreCase(txt, "POD (punto di prelievo)")) {
                    for (int look = 1; look <= 3 && (i + look) < lines.getLength() && idPod.isEmpty(); look++) {
                        String candidate = safeText(lines.item(i + look));
                        if (candidate.isEmpty()) continue;
                        String extracted = extractPodFromCandidate(candidate);
                        if (!extracted.isEmpty()) {
                            idPod = extracted;
                            break;
                        }
                    }
                }

                // 2) Estrazione fornitore
                if (txt.contains("SEGNALAZIONE GUASTI ELETTRICITA")) {
                    if (i + 2 < lines.getLength()) {
                        fornitore = safeText(lines.item(i + 2));
                    }
                }

                // 3) Estrazione tensione/potenze (max 3 valori)
                if ((txt.contains("Tensione di alimentazione")
                        || txt.contains("Potenza impegnata")
                        || txt.contains("Potenza disponibile"))
                        && vals.size() < 3) {
                    if (i + 1 < lines.getLength()) {
                        String raw = safeText(lines.item(i + 1))
                                .replaceAll("^(\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}\\s+){1,2}", "")
                                .replaceAll("[^\\d,\\.\\-]", "")
                                .replace(".", "")
                                .replace(",", ".");
                        try { vals.add(Double.parseDouble(raw)); } catch (Exception ignored) {}
                    }
                }

                // 4) Estrazione indirizzo
                if (txt.contains("Indirizzo di fornitura")) {
                    StringBuilder sb = new StringBuilder();
                    int k = i + 1;
                    while (k < lines.getLength()) {
                        String part = safeText(lines.item(k++));
                        if (part.isEmpty() || part.contains("Tipologia cliente")) break;
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(part);
                    }

                    String[] sp = sb.toString().split(" - ");
                    if (sp.length == 2) {
                        sede = sp[0].trim();
                        String[] cc = sp[1].split(" ", 2);
                        cap = cc.length > 0 ? cc[0] : "";
                        citta = cc.length > 1 ? cc[1] : "";
                    }
                }

                // 4-bis) Estrazione "Periodicità di fatturazione"
                // Gestisce possibili varianti/accents/spazi: "Periodicità di fatturazione", "Periodicita di fatturazione", ecc.
                if (periodFattur == null && containsIgnoreCase(stripAccents(txt), "periodicita di fatturazione")) {
                    // Prova a prendere sulla stessa riga dopo ":" o "-"
                    String sameLine = txt;
                    String value = null;
                    // cerca separatori comuni
                    int colon = sameLine.indexOf(':');
                    int dash = sameLine.indexOf('-');
                    if (colon != -1 && colon + 1 < sameLine.length()) {
                        value = sameLine.substring(colon + 1).trim();
                    } else if (dash != -1 && dash + 1 < sameLine.length()) {
                        value = sameLine.substring(dash + 1).trim();
                    }

                    // se non trovato sulla stessa riga, cerca nelle prossime due righe non vuote
                    if (value == null || value.isEmpty()) {
                        for (int look = 1; look <= 2 && (i + look) < lines.getLength(); look++) {
                            String cand = safeText(lines.item(i + look)).trim();
                            if (!cand.isEmpty()) { value = cand; break; }
                        }
                    }

                    if (value != null && !value.isEmpty()) {
                        periodFattur = normalizePeriodicita(value); // normalizza alle nostre etichette canoniche quando possibile
                        // evita trascinamenti di testo aggiuntivo sulla stessa riga
                        // es. "Mensile (fatturazione ...)" -> prendi prima parola utile
                        String[] toks = periodFattur.split("[,;()]", 2);
                        periodFattur = toks[0].trim();
                    }
                }

                // 6. Estrazione Decreto Energivori - classe di agevolazione (su 3 righe)
                if (classeAgevolazione == null && containsIgnoreCase(txt, "Decreto Energivori") &&
                        containsIgnoreCase(txt, "classe di")) {
                    String value = null;
                    // Controlla se questa riga contiene l'inizio della dicitura
                    boolean isStartLine = containsIgnoreCase(txt, "Decreto Energivori") &&
                            containsIgnoreCase(txt, "classe di");

                    if (isStartLine) {
                        // Cerca "agevolazione" nella riga successiva
                        if (i + 1 < lines.getLength()) {
                            String secondLine = safeText(lines.item(i + 1)).trim();
                            if (containsIgnoreCase(secondLine, "agevolazione")) {
                                // Se trovato "agevolazione", il valore è nella terza riga
                                if (i + 2 < lines.getLength()) {
                                    String thirdLine = safeText(lines.item(i + 2)).trim();
                                    if (!thirdLine.isEmpty()) {
                                        // Prende la prima parola della terza riga
                                        String[] parts = thirdLine.split("\\s+");
                                        if (parts.length > 0 && !parts[0].isEmpty()) {
                                            value = parts[0].trim();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (value != null && !value.isEmpty()) {
                        classeAgevolazione = value;
                        System.out.println("classeAgevolazione estratta: " + classeAgevolazione);
                    }
                }
            }

            // Aggiorna sempre la classe di agevolazione se trovata (indipendentemente se POD esiste già)
            if (classeAgevolazione != null && !classeAgevolazione.isEmpty()) {
                String normalized = normalizeClasseAgevolazione(classeAgevolazione);
                if (normalized != null && !normalized.isEmpty()) classeAgevolazione = normalized;
                else classeAgevolazione = null;
                System.out.println("classeAgevolazione aggiornata: " + classeAgevolazione);
                // Aggiorna la tabella cliente
                clienteService.updateCliente(idUtente, "classeAgevolazione", classeAgevolazione);
            }

            // 5) Se trovato POD valido → creaPod + salva periodicità
            if (!idPod.isEmpty()) {
                creaPod(vals, idUtente, idPod, fornitore, citta, cap, sede, periodFattur);
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return idPod;
    }

    private String normalizeClasseAgevolazione(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase();

        // Mantieni "Val" come è
        if (s.equals("VAL")) return "Val";

        // Converti ASOS1/2/3 in Fat1/2/3
        if (s.equals("ASOS1")) return "Fat1";
        if (s.equals("ASOS2")) return "Fat2";
        if (s.equals("ASOS3")) return "Fat3";

        // Se non riconosciuto, restituisci il valore originale pulito
        return raw.trim();
    }

    /** Normalizza la periodicità a una forma canonica (MENSILE, BIMESTRALE, TRIMESTRALE, QUADRIMESTRALE, SEMESTRALE, ANNUALE) quando riconosciuta. */
    private String normalizePeriodicita(String raw) {
        if (raw == null) return null;
        String s = stripAccents(raw).trim().toUpperCase();
        if (s.startsWith("MEN")) return "MENSILE";
        if (s.startsWith("BIM")) return "BIMESTRALE";
        if (s.startsWith("TRI")) return "TRIMESTRALE";
        if (s.startsWith("QUA")) return "QUADRIMESTRALE";
        if (s.startsWith("SEM")) return "SEMESTRALE";
        if (s.startsWith("ANN")) return "ANNUALE";
        return raw.trim(); // lasciamo com'è se non riconosciuta
    }

    /** Rimuove gli accenti per confronti robusti. */
    private String stripAccents(String input) {
        if (input == null) return null;
        String norm = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return norm.replaceAll("\\p{M}+", "");
    }

    /* ====================== UTILITIES (no regex per il POD) ====================== */

    private static String safeText(Node n) {
        if (n == null || n.getNodeType() != Node.ELEMENT_NODE) return "";
        String t = ((Element) n).getTextContent();
        return t == null ? "" : t.trim();
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null &&
                haystack.toLowerCase(Locale.ITALY).contains(needle.toLowerCase(Locale.ITALY));
    }

    /**
     * Estrae un possibile POD dalla riga candidata SENZA regex:
     * - rimuove spazi
     * - tokenizza solo su caratteri alfanumerici (A-Z, 0-9)
     * - prende il primo token che:
     * - inizia con "IT"
     * - ha almeno 1 cifra dopo "IT"
     * - lunghezza compresa tra 12 e 16
     * - contiene più cifre che lettere dopo "IT" (per evitare "ITORE..." ecc.)
     */
    private static String extractPodFromCandidate(String line) {
        if (line == null) return "";
        String up = line.toUpperCase(Locale.ITALY).replace(" ", "");

        // tokenizza senza regex: sequenze alfanumeriche
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < up.length(); i++) {
            char c = up.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                cur.append(c);
            } else {
                if (cur.length() > 0) { tokens.add(cur.toString()); cur.setLength(0); }
            }
        }
        if (cur.length() > 0) tokens.add(cur.toString());

        for (String tok : tokens) {
            if (!tok.startsWith("IT")) continue;
            String tail = tok.substring(2);
            if (tail.isEmpty()) continue;

            // requisiti minimi per sembrare un POD
            int len = tok.length();
            if (len < 12 || len > 16) continue;

            boolean hasDigit = false;
            int digits = 0, letters = 0;
            for (int i = 0; i < tail.length(); i++) {
                char c = tail.charAt(i);
                if (c >= '0' && c <= '9') { hasDigit = true; digits++; }
                else if (c >= 'A' && c <= 'Z') { letters++; }
            }

            if (!hasDigit) continue;
            if (digits < letters) continue; // più numerico che alfabetico

            // OK, questo è il candidato POD
            return tok;
        }

        return "";
    }

    @Transactional
    public void creaPod(List<Double> vals, int idUtente, String idPod,
                        String fornitore, String nazione, String cap, String sede, String PeriodFattur) {
        Cliente c = clienteRepo.findById(idUtente);

        // Controlla se il POD esiste già
        Pod existingPod = podRepo.findById(idPod);

        if (existingPod != null) {
            // Il POD esiste già, aggiorna i campi
            existingPod.setFornitore(fornitore);
            existingPod.setTensioneAlimentazione(vals.size() > 0 ? vals.get(0) : existingPod.getTensioneAlimentazione());
            existingPod.setPotenzaImpegnata(vals.size() > 1 ? vals.get(1) : existingPod.getPotenzaImpegnata());
            existingPod.setPotenzaDisponibile(vals.size() > 2 ? vals.get(2) : existingPod.getPotenzaDisponibile());
            existingPod.setSede(sede);
            existingPod.setNazione(nazione);
            existingPod.setCap(cap);
            existingPod.setPeriodFattur(PeriodFattur);

            double t = existingPod.getTensioneAlimentazione();
            existingPod.setTipoTensione(t <= 1000 ? "Bassa" : t <= 35000 ? "Media" : "Alta");

            // Non serve fare persist perché è già un'entità gestita
        } else {
            // Il POD non esiste, creane uno nuovo
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
            p.setPeriodFattur(PeriodFattur);

            double t = p.getTensioneAlimentazione();
            p.setTipoTensione(t <= 1000 ? "Bassa" : t <= 35000 ? "Media" : "Alta");

            podRepo.persist(p);
        }
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
                    if (b.getSpeseEne() != null && b.getTotAtt() != null && b.getTotAtt() > 0) {
                        sumP += b.getSpeseEne() / b.getTotAtt();
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
