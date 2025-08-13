package miesgroup.mies.webdev.Service.file;//package miesgroup.mies.webdev.Service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.*;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Model.file.PDFFile;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Model.budget.BudgetAll;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.bolletta.PodRepo;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;
import miesgroup.mies.webdev.Repository.file.FileRepo;
import miesgroup.mies.webdev.Rest.Model.BollettaPodResponse;
import miesgroup.mies.webdev.Rest.Model.FileDto;
import miesgroup.mies.webdev.Service.DateUtils;
import miesgroup.mies.webdev.Service.bolletta.BollettaPodService;
import miesgroup.mies.webdev.Service.bolletta.verBollettaPodService;
import miesgroup.mies.webdev.Service.budget.BudgetAllService;
import miesgroup.mies.webdev.Service.budget.BudgetService;
import miesgroup.mies.webdev.Service.cliente.SessionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.sql.SQLException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class FileService {

    private final FileRepo fileRepo;
    private final BollettaPodRepo BollettaPodRepo;
    private final BollettaPodService bollettaPodService;
    private final SessionService sessionService;
    private final PodRepo podRepo;
    private final ClienteRepo clienteRepo;
    private final verBollettaPodService verBollettaPodService;
    @Inject
    BollettaPodRepo bollettaPodRepo;

    public FileService(FileRepo fileRepo, BollettaPodRepo BollettaPodRepo, BollettaPodService bollettaPodService, SessionService sessionService, PodRepo podRepo, ClienteRepo clienteRepo, verBollettaPodService verBollettaPodService) {
        this.fileRepo = fileRepo;
        this.BollettaPodRepo = BollettaPodRepo;
        this.bollettaPodService = bollettaPodService;
        this.sessionService = sessionService;
        this.podRepo = podRepo;
        this.clienteRepo = clienteRepo;
        this.verBollettaPodService = verBollettaPodService;
    }

    @Inject
    BudgetService budgetService;

    @Inject
    BudgetAllService budgetAllService;

    @Inject
    SessionRepo sessionRepo;

    // ─────────────────────────────────────────────────────────────
    // DEBUG UTILS
    // ─────────────────────────────────────────────────────────────
    private static void logTitle(String title) {
        System.out.println("\n================ " + title + " ================");
    }
    private static void logKV(String key, Object value) {
        System.out.println("• " + key + ": " + String.valueOf(value));
    }
    private static void logWarn(String msg) {
        System.out.println("⚠️  " + msg);
    }
    private static void logOk(String msg) {
        System.out.println("✅ " + msg);
    }
    // Mappe: Mese -> (Categoria -> Valore Double)
    private static void logNestedDoubles(String title, Map<String, Map<String, Double>> m) {
        logTitle(title);
        if (m == null || m.isEmpty()) { System.out.println("(vuoto)"); return; }
        m.forEach((mese, catMap) -> {
            System.out.println("Mese: " + mese);
            if (catMap == null || catMap.isEmpty()) {
                System.out.println("  (nessuna categoria)");
            } else {
                catMap.forEach((cat, val) -> System.out.printf("  - %-35s : %.6f%n", cat, val));
            }
        });
    }
    // Mappe: Mese -> (Categoria -> Lista<Double>)
    private static void logNestedLists(String title, Map<String, Map<String, List<Double>>> m) {
        logTitle(title);
        if (m == null || m.isEmpty()) { System.out.println("(vuoto)"); return; }
        m.forEach((mese, catMap) -> {
            System.out.println("Mese: " + mese);
            if (catMap == null || catMap.isEmpty()) {
                System.out.println("  (nessuna categoria)");
            } else {
                catMap.forEach((cat, list) -> System.out.println("  - " + cat + " : " + list));
            }
        });
    }
    // Mappe letture: Mese -> (Categoria -> (Fascia -> Integer))
    private static void logLetture(String title, Map<String, Map<String, Map<String, Integer>>> m) {
        logTitle(title);
        if (m == null || m.isEmpty()) { System.out.println("(vuoto)"); return; }
        m.forEach((mese, catMap) -> {
            System.out.println("Mese: " + mese);
            if (catMap == null || catMap.isEmpty()) {
                System.out.println("  (nessuna categoria)");
            } else {
                catMap.forEach((cat, fasciaMap) -> {
                    System.out.println("  Categoria: " + cat);
                    fasciaMap.forEach((fascia, v) -> System.out.printf("    %-3s -> %d%n", fascia, v));
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────

    @Transactional
    public int saveFile(String fileName, byte[] fileData) throws SQLException {
        logTitle("saveFile");
        logKV("fileName", fileName);
        logKV("fileData.length", (fileData != null ? fileData.length : "null"));
        if (fileName == null || fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("File name and data must not be null or empty");
        }
        PDFFile pdfFile = new PDFFile();
        pdfFile.setFileName(fileName);
        pdfFile.setFileData(fileData);
        int id = fileRepo.insert(pdfFile);
        logOk("File salvato con id=" + id);
        return id;
    }

    @Transactional
    public PDFFile getFile(int id) {
        logTitle("getFile");
        logKV("id", id);
        PDFFile f = fileRepo.findById(id);
        logKV("trovato", (f != null));
        return f;
    }

    // CONVERTI FILE IN XML
    @Transactional
    public Document convertPdfToXml(byte[] pdfData) throws IOException, ParserConfigurationException {
        logTitle("convertPdfToXml");
        if (pdfData == null) logWarn("pdfData è null");
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            logTitle("PDF -> Testo estratto");
            System.out.println(text);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document xmlDocument = documentBuilder.newDocument();

            Element rootElement = xmlDocument.createElement("PDFContent");
            xmlDocument.appendChild(rootElement);

            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                Element lineElement = xmlDocument.createElement("Line");
                lineElement.appendChild(xmlDocument.createTextNode(line));
                rootElement.appendChild(lineElement);
            }
            logOk("Convertito PDF->XML con " + lines.length + " righe.");
            return xmlDocument;
        }
    }

    @Transactional
    public String convertDocumentToString(Document doc) throws TransformerException {
        logTitle("convertDocumentToString");
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{https://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        logOk("XML convertito in stringa (indentato).");
        return writer.getBuffer().toString();
    }

    @Transactional
    public String extractValuesFromXmlA2A(byte[] xmlData, String idPod) {
        logTitle("extractValuesFromXmlA2A");
        logKV("idPod", idPod);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlData));

            String nomeBolletta = extractBollettaNome(document);
            if (nomeBolletta == null) {
                logWarn("Nessun numero bolletta trovato.");
                return null;
            } else {
                logOk("Nome/Numero bolletta: " + nomeBolletta);
                BollettaPodRepo.A2AisPresent(nomeBolletta, idPod);
            }

            Map<String, Map<String, Map<String, Integer>>> lettureMese = extractLetture(document);
            Map<String, Map<String, Double>> spesePerMese = extractSpesePerMese(document, lettureMese);
            Map<String, Map<String, Double>> kWhPerMese = extractKwhPerMese(document);
            Periodo periodo = extractPeriodo(document);

            logKV("Periodo estratto", periodo.getInizio() + " -> " + periodo.getDataFine() + " (anno " + periodo.getAnno() + ")");
            logLetture("LETTURE - Mappa completa", lettureMese);
            logNestedDoubles("SPESE - Mappa aggregata per mese", spesePerMese);
            logNestedDoubles("KWH - Mappa aggregata per mese", kWhPerMese);

            if (lettureMese.isEmpty()) {
                logWarn("lettureMese vuoto. Interrompo.");
                return null;
            }

            logTitle("Persistenza dati su DB");
            logKV("idPod", idPod);
            logKV("nomeBolletta", nomeBolletta);
            logKV("Periodo (fine)", periodo.getDataFine());
            fileRepo.saveDataToDatabase(lettureMese, spesePerMese, idPod, nomeBolletta, periodo, kWhPerMese);

            // ——————— AGGIUNTA BUDGET E BUDGET_ALL ———————
            String mese = DateUtils.getMonthFromDateLocalized(periodo.getDataFine()); // es: "giugno"
            String anno = periodo.getAnno();
            BollettaPod bolletta = BollettaPodRepo.find(
                    "nomeBolletta = ?1 and idPod = ?2 and mese = ?3 and anno = ?4",
                    nomeBolletta, idPod, mese, anno).firstResult();

            if (bolletta != null) {
                logOk("Bolletta trovata in DB: id=" + bolletta.getId() + "  pod=" + bolletta.getIdPod()
                        + "  mese=" + bolletta.getMese() + "  anno=" + bolletta.getAnno());
                logKV("SpeseEnergia", bolletta.getSpeseEne());
                logKV("TotAttiva(kWh)", bolletta.getTotAtt());
                logKV("Oneri", bolletta.getOneri());

                // Recupera il cliente dal POD
                Pod pod = podRepo.findById(bolletta.getIdPod());
                if (pod == null || pod.getUtente() == null) {
                    throw new IllegalStateException("Impossibile trovare il cliente associato al POD " + bolletta.getIdPod());
                }
                Cliente cliente = pod.getUtente();

                // --- Inserisci/aggiorna BUDGET
                Budget budget = new Budget();
                budget.setPodId(bolletta.getIdPod());
                budget.setAnno(Integer.parseInt(bolletta.getAnno()));
                budget.setMese(Integer.parseInt(DateUtils.getMonthNumber(mese))); // es. giugno -> 6
                budget.setPrezzoEnergiaBase(bolletta.getSpeseEne() != null ? bolletta.getSpeseEne() : 0.0);
                budget.setConsumiBase(bolletta.getTotAtt()      != null ? bolletta.getTotAtt()     : 0.0);
                budget.setOneriBase(bolletta.getOneri()            != null ? bolletta.getOneri()         : 0.0);
                budget.setPrezzoEnergiaPerc(0.0);
                budget.setConsumiPerc(0.0);
                budget.setOneriPerc(0.0);
                budget.setCliente(cliente);

                logTitle("BUDGET - Insert/Update");
                logKV("Cliente", cliente.getId());
                logKV("POD", budget.getPodId());
                logKV("Anno/Mese", budget.getAnno() + "/" + budget.getMese());
                logKV("PrezzoEnergiaBase", budget.getPrezzoEnergiaBase());
                logKV("ConsumiBase", budget.getConsumiBase());
                logKV("OneriBase", budget.getOneriBase());
                budgetService.creaBudget(budget);

                // --- Inserisci/aggiorna BUDGET_ALL
                BudgetAll budgetAll = new BudgetAll();
                budgetAll.setIdPod("ALL");
                budgetAll.setCliente(cliente);
                budgetAll.setAnno(Integer.parseInt(bolletta.getAnno()));
                budgetAll.setMese(Integer.parseInt(DateUtils.getMonthNumber(mese))); // es. 6 = giugno

                budgetAll.setPrezzoEnergiaBase(bolletta.getSpeseEne() != null ? bolletta.getSpeseEne() : 0.0);
                budgetAll.setConsumiBase(bolletta.getTotAtt() != null ? bolletta.getTotAtt() : 0.0);
                budgetAll.setOneriBase(bolletta.getOneri() != null ? bolletta.getOneri() : 0.0);
                budgetAll.setPrezzoEnergiaPerc(0.0);
                budgetAll.setConsumiPerc(0.0);
                budgetAll.setOneriPerc(0.0);

                logTitle("BUDGET_ALL - Upsert");
                logKV("Cliente", cliente.getId());
                logKV("Anno/Mese", budgetAll.getAnno() + "/" + budgetAll.getMese());
                logKV("PrezzoEnergiaBase", budgetAll.getPrezzoEnergiaBase());
                logKV("ConsumiBase", budgetAll.getConsumiBase());
                logKV("OneriBase", budgetAll.getOneriBase());
                budgetAllService.upsertAggregato(budgetAll);

            } else {
                logWarn("Bolletta non trovata in DB dopo il salvataggio per mese/anno.");
            }
            // ——————— FINE AGGIUNTA ———————

            logOk("Completato extractValuesFromXmlA2A per bolletta " + nomeBolletta);
            return nomeBolletta;

        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public void verificaA2APiuMesi(String nomeB) throws SQLException {
        logTitle("Verifica A2A su più mesi");
        logKV("nomeBolletta", nomeB);
        List<BollettaPod> b = BollettaPodRepo.find("nomeBolletta", nomeB).list();
        logKV("Bollette trovate", b.size());
        for (BollettaPod bollettaPod : b) {
            System.out.printf(" - id=%s pod=%s mese=%s anno=%s%n",
                    bollettaPod.getId(), bollettaPod.getIdPod(),
                    bollettaPod.getMese(), bollettaPod.getAnno());
            verBollettaPodService.A2AVerifica(bollettaPod);
        }
    }

    private Periodo extractPeriodo(Document document) {
        logTitle("extractPeriodo");
        NodeList lineNodes = document.getElementsByTagName("Line");

        Date dataInizio = null;
        Date dataFine = null;

        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node lineNode = lineNodes.item(i);
            if (lineNode.getNodeType() == Node.ELEMENT_NODE) {
                String lineText = lineNode.getTextContent();

                if (lineText.contains("Fascia oraria")) {
                    ArrayList<Date> dates = extractDates(lineText);

                    if (dates.size() == 2) {
                        dataInizio = dates.get(0);
                        dataFine = dates.get(1);
                        break;
                    }
                }
            }
        }

        if (dataInizio != null && dataFine != null) {
            String anno = String.valueOf(dataFine.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear());
            logTitle("Periodo fattura");
            logKV("Data inizio", dataInizio);
            logKV("Data fine", dataFine);
            logKV("Anno", String.valueOf(anno));
            return new Periodo(dataInizio, dataFine, anno);
        } else {
            logWarn("Impossibile estrarre il periodo: date mancanti.");
            throw new IllegalStateException("Impossibile estrarre il periodo: date mancanti.");
        }
    }

    private Map<String, Map<String, Double>> extractSpesePerMese(Document document, Map<String, Map<String, Map<String, Integer>>> lettureMese) {
        logTitle("extractSpesePerMese");
        Map<String, Map<String, List<Double>>> spesePerMese = new HashMap<>();
        Set<String> categorieGiaViste = new HashSet<>();
        String categoriaCorrente = "";
        String sottoCategoria = "";
        boolean sezioneCorretta = false;
        String meseCorrente = null;
        boolean controlloAttivo = false;
        int righeSenzaEuro = 0;
        boolean ricercaAvanzata = false;
        boolean categoriaAppenaTrovata = false;
        String meseLetto = lettureMese.keySet().iterator().next();

        // Flag per riconoscere sezioni specifiche
        boolean sezioneOneri = false;
        boolean sezioneTrasporti = false;
        String tipoComponente = ""; // ASOS o ARIM
        String tipoQuota = ""; // FISSA, POTENZA, VARIABILE

        Set<String> stopParsingKeywords = Set.of(
                "TOTALE FORNITURA ENERGIA ELETTRICA E IMPOSTE",
                "RICALCOLO"
        );

        NodeList lineNodes = document.getElementsByTagName("Line");
        System.out.println("🔍 INIZIO ESTRAZIONE SPESE - Totale righe da analizzare: " + lineNodes.getLength());

        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node lineNode = lineNodes.item(i);
            if (lineNode.getNodeType() != Node.ELEMENT_NODE) continue;

            String lineText = lineNode.getTextContent().trim();
            String lowerLine = lineText.toLowerCase();

            if (stopParsingKeywords.stream().anyMatch(lineText::contains)) {
                System.out.println("🛑 STOP PARSING: Trovata keyword di stop: " + lineText);
                break;
            }

            String meseEstratto = estraiMese(lineText);
            if (meseEstratto != null) {
                meseCorrente = meseEstratto;
                System.out.println("📅 MESE ESTRATTO: " + meseCorrente);
            }

            // RICONOSCIMENTO SEZIONI PRINCIPALI CON ESTRAZIONE DIRETTA DEL TOTALE
            if (lineText.contains("SPESA PER LA MATERIA ENERGIA")) {
                categoriaCorrente = "Materia Energia";
                categorieGiaViste.add(categoriaCorrente);
                System.out.println("🏷️ SEZIONE: " + categoriaCorrente);

                // Reset diretto dei flag
                sezioneOneri = false;
                sezioneTrasporti = false;
                sezioneCorretta = false;
                ricercaAvanzata = false;
                categoriaAppenaTrovata = true;
                controlloAttivo = false;
                righeSenzaEuro = 0;
                sottoCategoria = "";
                tipoComponente = "";
                tipoQuota = "";
                continue;

            } else if (lineText.contains("SPESA PER ONERI DI SISTEMA")) {
                categoriaCorrente = "Oneri di Sistema";
                categorieGiaViste.add(categoriaCorrente);
                System.out.println("🏷️ SEZIONE: " + categoriaCorrente);

                // ESTRAE IL TOTALE DALLA STESSA RIGA DELL'INTESTAZIONE
                if (lineText.contains("€")) {
                    Double valore = extractEuroValue(lineText);
                    if (valore != null && meseCorrente != null) {
                        spesePerMese
                                .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                                .computeIfAbsent("ONERI_TOTALE", k -> new ArrayList<>())
                                .add(valore);
                        System.out.println("✅ TOTALE ONERI estratto: " + valore + " per mese: " + meseCorrente);
                    } else {
                        System.out.println("❌ ERRORE estrazione totale oneri da: " + lineText);
                    }
                } else {
                    System.out.println("⚠️ ONERI: Riga senza € - " + lineText);
                }

                sezioneOneri = true;
                sezioneTrasporti = false;
                sezioneCorretta = false;
                ricercaAvanzata = false;
                categoriaAppenaTrovata = false;
                controlloAttivo = false;
                righeSenzaEuro = 0;
                sottoCategoria = "";
                tipoComponente = "";
                tipoQuota = "";
                continue;

            } else if (lineText.contains("SPESA PER IL TRASPORTO E LA GESTIONE DEL CONTATORE") ||
                    lineText.contains("Spesa per il trasporto e la gestione del contatore")) {
                categoriaCorrente = "Trasporto e Gestione Contatore";
                categorieGiaViste.add(categoriaCorrente);
                System.out.println("🏷️ SEZIONE: " + categoriaCorrente);

                // ESTRAZIONE TOTALE - GESTIONE MIGLIORATA PER PATTERN MULTIPLI
                Double valore = null;

                // Tentativo 1: Cerca nella stessa riga
                if (lineText.contains("€")) {
                    valore = extractEuroValue(lineText);
                    System.out.println("🔍 Tentativo 1 - Stessa riga: " + (valore != null ? "✅ " + valore : "❌"));
                }

                // Tentativo 2: Cerca nella riga successiva se non trovato
                if (valore == null && i + 1 < lineNodes.getLength()) {
                    String nextLine = lineNodes.item(i + 1).getTextContent().trim();
                    System.out.println("🔍 Controllo riga successiva: " + nextLine);
                    if (nextLine.contains("€")) {
                        valore = extractEuroValue(nextLine);
                        System.out.println("🔍 Tentativo 2 - Riga successiva: " + (valore != null ? "✅ " + valore : "❌"));
                    }
                }

                // Tentativo 3: Pattern alternativi nella stessa riga
                if (valore == null) {
                    // Cerca pattern come "trasporto: XX.XXX,XX €" o "contatore: XX.XXX,XX €"
                    if (lineText.matches(".*[Tt]rasporto.*[Cc]ontatore.*€.*") ||
                            lineText.matches(".*TRASPORTO.*CONTATORE.*€.*") ||
                            lineText.matches(".*[Cc]ontatore.*€.*")) {
                        valore = extractEuroValue(lineText);
                        System.out.println("🔍 Tentativo 3 - Pattern specifico: " + (valore != null ? "✅ " + valore : "❌"));
                    }
                }

                if (valore != null && meseCorrente != null) {
                    spesePerMese
                            .computeIfAbsent(meseLetto, k -> new HashMap<>())
                            .computeIfAbsent("TRASPORTI_TOTALE", k -> new ArrayList<>())
                            .add(valore);
                    System.out.println("✅ TOTALE TRASPORTI estratto: " + valore + " per mese: " + meseCorrente);
                } else {
                    System.out.println("❌ ERRORE estrazione totale trasporti da: " + lineText);
                    // Debug aggiuntivo: mostra i primi caratteri per analizzare il pattern
                    System.out.println("🔍 DEBUG - Primi 100 caratteri: " + lineText.substring(0, Math.min(100, lineText.length())));
                }

                sezioneOneri = false;
                sezioneTrasporti = true;
                sezioneCorretta = false;
                ricercaAvanzata = false;
                categoriaAppenaTrovata = false;
                controlloAttivo = false;
                righeSenzaEuro = 0;
                sottoCategoria = "";
                tipoComponente = "";
                tipoQuota = "";
                continue;

            } else if (lineText.contains("TOTALE IMPOSTE")) {
                categoriaCorrente = "Totale Imposte";
                categorieGiaViste.add(categoriaCorrente);
                System.out.println("🏷️ SEZIONE: " + categoriaCorrente);

                // Reset diretto dei flag
                sezioneOneri = false;
                sezioneTrasporti = false;
                sezioneCorretta = false;
                ricercaAvanzata = false;
                categoriaAppenaTrovata = false;
                controlloAttivo = false;
                righeSenzaEuro = 0;
                sottoCategoria = "";
                tipoComponente = "";
                tipoQuota = "";
                continue;

            } else if (lowerLine.contains("penalit")) {
                categoriaCorrente = "Altro";
                categorieGiaViste.add(categoriaCorrente);
                System.out.println("🏷️ SEZIONE: " + categoriaCorrente);

                // Reset diretto dei flag
                sezioneOneri = false;
                sezioneTrasporti = false;
                sezioneCorretta = false;
                ricercaAvanzata = false;
                categoriaAppenaTrovata = false;
                controlloAttivo = false;
                righeSenzaEuro = 0;
                sottoCategoria = "";
                tipoComponente = "";
                tipoQuota = "";
                continue;
            }

            // RICERCA AGGIUNTIVA PER TOTALE TRASPORTI (fallback)
            // Cerca righe che contengono i pattern dei trasporti anche fuori dalle intestazioni
            if (meseCorrente != null && !sezioneTrasporti && !sezioneOneri &&
                    (lineText.matches(".*[Tt]rasporto.*[Cc]ontatore.*€.*[0-9]+.*") ||
                            lineText.matches(".*[Gg]estione.*[Cc]ontatore.*€.*[0-9]+.*"))) {
                Double valore = extractEuroValue(lineText);
                if (valore != null) {
                    spesePerMese
                            .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                            .computeIfAbsent("TRASPORTI_TOTALE", k -> new ArrayList<>())
                            .add(valore);
                    System.out.println("✅ TOTALE TRASPORTI estratto (fallback): " + valore + " per mese: " + meseCorrente);
                }
            }

            // Gestione specifica sezione ONERI per ASOS/ARIM
            if (sezioneOneri) {
                // Riconoscimento tipo di quota
                if (lowerLine.contains("quota fissa")) {
                    tipoQuota = "FISSA";
                    System.out.println("🔧 ONERI - Quota: " + tipoQuota);
                } else if (lowerLine.contains("quota potenza")) {
                    tipoQuota = "POTENZA";
                    System.out.println("🔧 ONERI - Quota: " + tipoQuota);
                } else if (lowerLine.contains("quota variabile")) {
                    tipoQuota = "VARIABILE";
                    System.out.println("🔧 ONERI - Quota: " + tipoQuota);
                }

                // Riconoscimento componente ASOS
                if (lowerLine.contains("componente asos") ||
                        lowerLine.contains("sostegno delle fonti rinnovabili")) {
                    tipoComponente = "ASOS";
                    if (!tipoQuota.isEmpty()) {
                        sottoCategoria = "ASOS_" + tipoQuota;
                        System.out.println("🎯 ONERI - Sottocategoria: " + sottoCategoria);
                    }
                }
                // Riconoscimento componente ARIM
                else if (lowerLine.contains("componente arim") ||
                        lowerLine.contains("altri oneri relativi ad attività")) {
                    tipoComponente = "ARIM";
                    if (!tipoQuota.isEmpty()) {
                        sottoCategoria = "ARIM_" + tipoQuota;
                        System.out.println("🎯 ONERI - Sottocategoria: " + sottoCategoria);
                    }
                }
            }

            // Gestione specifica sezione TRASPORTI per quote specifiche
            if (sezioneTrasporti) {
                if (lowerLine.contains("quota fissa")) {
                    sottoCategoria = "TRASPORTI_FISSA";
                    System.out.println("🎯 TRASPORTI - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("quota potenza")) {
                    sottoCategoria = "TRASPORTI_POTENZA";
                    System.out.println("🎯 TRASPORTI - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("quota variabile")) {
                    sottoCategoria = "TRASPORTI_VARIABILE";
                    System.out.println("🎯 TRASPORTI - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("penalità energia reattiva")) {
                    sottoCategoria = "PENALITA_REATTIVA";
                    System.out.println("🎯 TRASPORTI - Sottocategoria: " + sottoCategoria);
                }
            }

            // Gestione materia energia
            if ("Materia Energia".equals(categoriaCorrente) || (sezioneCorretta && ricercaAvanzata)) {
                sezioneCorretta = true;

                if (lowerLine.contains("perdite di rete f1")) {
                    sottoCategoria = "Perdite F1";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("perdite di rete f2")) {
                    sottoCategoria = "Perdite F2";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("perdite di rete f3")) {
                    sottoCategoria = "Perdite F3";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("materia energia f1") || lowerLine.contains("quota vendita f1")) {
                    sottoCategoria = "Materia energia F1";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("materia energia f2") || lowerLine.contains("quota vendita f2")) {
                    sottoCategoria = "Materia energia F2";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("materia energia f3") || lowerLine.contains("quota vendita f3")) {
                    sottoCategoria = "Materia energia F3";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("materia energia") || lowerLine.contains("quota vendita")) {
                    sottoCategoria = "Materia energia F0";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("corrispettivi di dispacciamento del")) {
                    sottoCategoria = "dispacciamento";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("corrispettivo mercato capacità ore fuori")) {
                    sottoCategoria = "Fuori Picco";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("corrispettivo mercato capacità ore picco")) {
                    sottoCategoria = "Picco";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                }
            }

            // Estrazione valori Euro
            if ((categoriaCorrente != null && !categoriaCorrente.isEmpty()) && lineText.contains("€")) {
                Double valore = extractEuroValue(lineText);
                if (valore != null) {
                    String chiave;

                    if ((sezioneOneri || sezioneTrasporti) && !sottoCategoria.isEmpty()) {
                        // Usa la sottocategoria specifica per ASOS/ARIM/TRASPORTI
                        chiave = sottoCategoria;
                    } else if (categoriaAppenaTrovata) {
                        chiave = categoriaCorrente;
                        categoriaAppenaTrovata = false;
                        ricercaAvanzata = true;
                    } else {
                        chiave = !sottoCategoria.isEmpty() ? sottoCategoria : categoriaCorrente;
                    }

                    if (categorieGiaViste.contains(categoriaCorrente)) {
                        categorieGiaViste.remove(categoriaCorrente);
                        controlloAttivo = true;
                        righeSenzaEuro = 0;
                    }

                    if (meseCorrente == null) meseCorrente = "MeseSconosciuto";

                    spesePerMese
                            .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                            .computeIfAbsent(chiave, k -> new ArrayList<>())
                            .add(valore);

                    System.out.println("💰 VALORE AGGIUNTO: " + valore + " € -> Mese: " + meseCorrente + " | Chiave: " + chiave);
                    righeSenzaEuro = 0;
                    controlloAttivo = true;
                }
            } else if (controlloAttivo) {
                righeSenzaEuro++;
                if (righeSenzaEuro >= 10 &&
                        !lineText.matches(".*(QUOTA|Componente|Corrispettivi|€/kWh|€/kW/mese|€/cliente/mese|QUOTA VARIABILE).*")) {
                    System.out.println("🔄 RESET: Troppe righe senza € (" + righeSenzaEuro + ")");
                    sottoCategoria = "";
                    sezioneCorretta = false;
                    controlloAttivo = false;
                    righeSenzaEuro = 0;
                }
            } else {
                righeSenzaEuro = 0;
            }
        }

        // STAMPA RIEPILOGO COMPLETO DEI DATI RACCOLTI
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 RIEPILOGO COMPLETO DATI RACCOLTI (LISTE)");
        System.out.println("=".repeat(80));

        for (Map.Entry<String, Map<String, List<Double>>> meseEntry : spesePerMese.entrySet()) {
            String mese = meseEntry.getKey();
            System.out.println("\n🗓️ MESE: " + mese);
            System.out.println("-".repeat(50));

            for (Map.Entry<String, List<Double>> catEntry : meseEntry.getValue().entrySet()) {
                String categoria = catEntry.getKey();
                List<Double> valori = catEntry.getValue();
                double somma = valori.stream().mapToDouble(Double::doubleValue).sum();

                System.out.printf("   📁 %-40s : %s (SOMMA: %.2f €)%n",
                        categoria, valori.toString(), somma);
            }
        }

        Map<String, Map<String, Double>> result = processSpesePerMese(spesePerMese);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📈 DATI FINALI AGGREGATI (DOPO PROCESSAMENTO)");
        System.out.println("=".repeat(80));

        for (Map.Entry<String, Map<String, Double>> meseEntry : result.entrySet()) {
            String mese = meseEntry.getKey();
            System.out.println("\n🗓️ MESE: " + mese);
            System.out.println("-".repeat(50));

            for (Map.Entry<String, Double> catEntry : meseEntry.getValue().entrySet()) {
                String categoria = catEntry.getKey();
                Double valore = catEntry.getValue();

                System.out.printf("   💰 %-40s : %.2f €%n", categoria, valore);
            }
        }
        System.out.println("=".repeat(80));

        logNestedLists("RAW - Spese per mese (liste)", spesePerMese);
        return result;
    }


    private Map<String, Map<String, Double>> extractKwhPerMese(Document document) {
        logTitle("extractKwhPerMese");
        Map<String, Map<String, List<Double>>> kwhPerMese = new HashMap<>();

        Set<String> categorieGiaViste = new HashSet<>();
        String macroCategoria = "";
        String sottoCategoria = "";
        boolean sezioneMateria = false;
        String meseCorrente = null;
        boolean controlloAttivo = false;
        int righeSenzaKwh = 0;

        // AGGIUNTE per coerenza con extractSpesePerMese
        boolean sezioneOneri = false;
        boolean sezioneTrasporti = false;

        Set<String> stopParsingKeywords = Set.of("TOTALE FORNITURA ENERGIA ELETTRICA E IMPOSTE", "RICALCOLO");

        NodeList lineNodes = document.getElementsByTagName("Line");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node lineNode = lineNodes.item(i);
            if (lineNode.getNodeType() != Node.ELEMENT_NODE) continue;

            String lineText = lineNode.getTextContent().trim();
            String lowerLine = lineText.toLowerCase();

            if (stopParsingKeywords.stream().anyMatch(lineText::contains)) break;

            String meseEstratto = estraiMese(lineText);
            if (meseEstratto != null) meseCorrente = meseEstratto;

            // RICONOSCIMENTO SEZIONI PRINCIPALI (MODIFICATO)
            if (lineText.contains("SPESA PER LA MATERIA ENERGIA")) {
                macroCategoria = "Materia Energia";
                categorieGiaViste.add(macroCategoria);
                controlloAttivo = false;
                righeSenzaKwh = 0;
                sezioneMateria = false;
                sezioneOneri = false;
                sezioneTrasporti = false;
                sottoCategoria = "";
                continue;
            } else if (lineText.contains("SPESA PER ONERI DI SISTEMA")) {
                macroCategoria = "Oneri di Sistema";
                categorieGiaViste.add(macroCategoria);

                // ESTRAE IL TOTALE DALLA STESSA RIGA DELL'INTESTAZIONE (ADATTATO per kWh)
                if (lineText.contains("€")) {
                    Double valore = extractEuroValue(lineText);
                    if (valore != null && meseCorrente != null) {
                        kwhPerMese
                                .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                                .computeIfAbsent("Oneri di Sistema_TOTALE", k -> new ArrayList<>())
                                .add(valore);
                        System.out.println("✅ TOTALE ONERI estratto (kWh): " + valore);
                    }
                }

                sezioneOneri = true;
                sezioneTrasporti = false;
                sezioneMateria = false;
                controlloAttivo = false;
                righeSenzaKwh = 0;
                sottoCategoria = "";
                continue;
            } else if (lineText.contains("SPESA PER IL TRASPORTO E LA GESTIONE DEL CONTATORE")) {
                macroCategoria = "Trasporto e Gestione Contatore";
                categorieGiaViste.add(macroCategoria);

                // ESTRAE IL TOTALE DALLA STESSA RIGA DELL'INTESTAZIONE (ADATTATO per kWh)
                if (lineText.contains("€")) {
                    Double valore = extractEuroValue(lineText);
                    if (valore != null && meseCorrente != null) {
                        kwhPerMese
                                .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                                .computeIfAbsent("Trasporto e Gestione Contatore_TOTALE", k -> new ArrayList<>())
                                .add(valore);
                        System.out.println("✅ TOTALE TRASPORTI estratto (kWh): " + valore);
                    }
                }

                sezioneOneri = false;
                sezioneTrasporti = true;
                sezioneMateria = false;
                controlloAttivo = false;
                righeSenzaKwh = 0;
                sottoCategoria = "";
                continue;
            } else if (lineText.contains("TOTALE IMPOSTE")) {
                macroCategoria = "";
                sottoCategoria = "";
                sezioneMateria = false;
                sezioneOneri = false;
                sezioneTrasporti = false;
                continue;
            }

            if (lowerLine.contains("penalità") && lineText.contains("kVARh")) {
                Double valore = extractKwhValue(lineText);
                if (valore != null) {
                    String mese = meseCorrente != null ? meseCorrente : "MeseSconosciuto";
                    kwhPerMese
                            .computeIfAbsent(mese, k -> new HashMap<>())
                            .computeIfAbsent("Altro", k -> new ArrayList<>())
                            .add(valore);
                }
                continue;
            }

            if ("Materia Energia".equals(macroCategoria) || sezioneMateria) {
                sezioneMateria = true;

                if (lowerLine.contains("perdite di rete f1")) {
                    sottoCategoria = "Perdite F1";
                } else if (lowerLine.contains("perdite di rete f2")) {
                    sottoCategoria = "Perdite F2";
                } else if (lowerLine.contains("perdite di rete f3")) {
                    sottoCategoria = "Perdite F3";
                } else if (lowerLine.contains("materia energia f1") || lowerLine.contains("quota vendita f1")) {
                    sottoCategoria = "Materia energia F1";
                } else if (lowerLine.contains("materia energia f2") || lowerLine.contains("quota vendita f2")) {
                    sottoCategoria = "Materia energia F2";
                } else if (lowerLine.contains("materia energia f3") || lowerLine.contains("quota vendita f3")) {
                    sottoCategoria = "Materia energia F3";
                } else if (lowerLine.contains("materia energia") || lowerLine.contains("quota vendita")) {
                    sottoCategoria = "Materia energia F0";
                } else if (lowerLine.contains("corrispettivi di dispacciamento del")) {
                    sottoCategoria = "dispacciamento";
                } else if (lowerLine.contains("corrispettivo mercato capacità ore fuori")) {
                    sottoCategoria = "Fuori Picco";
                } else if (lowerLine.contains("corrispettivo mercato capacità ore picco")) {
                    sottoCategoria = "Picco";
                }
            }

            if (!sottoCategoria.isEmpty() && (lineText.contains("kWh") || lineText.contains("kVARh"))) {
                Double kwhValue = extractKwhValue(lineText);
                if (kwhValue != null) {
                    if (categorieGiaViste.contains(macroCategoria)) {
                        categorieGiaViste.remove(macroCategoria);
                        controlloAttivo = true;
                        righeSenzaKwh = 0;
                    }
                    if (meseCorrente == null) meseCorrente = "MeseSconosciuto";

                    kwhPerMese
                            .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                            .computeIfAbsent(sottoCategoria, k -> new ArrayList<>())
                            .add(kwhValue);

                    controlloAttivo = true;
                    righeSenzaKwh = 0;
                }
            } else if (controlloAttivo) {
                righeSenzaKwh++;
                if (righeSenzaKwh >= 10 &&
                        !lineText.matches(".*(QUOTA|Componente|Corrispettivi|€/kWh|€/kW/mese|€/cliente/mese|QUOTA VARIABILE).*")) {
                    macroCategoria = "";
                    controlloAttivo = false;
                    righeSenzaKwh = 0;
                    sottoCategoria = "";
                    sezioneMateria = false;
                }
            } else {
                righeSenzaKwh = 0;
            }
        }

        logNestedLists("RAW - kWh/kVARh per mese (liste)", kwhPerMese);
        return processKwhPerMese(kwhPerMese);
    }

    private Map<String, Map<String, Double>> processKwhPerMese(Map<String, Map<String, List<Double>>> kwhPerMese) {
        Map<String, Map<String, Double>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, List<Double>>> entryMese : kwhPerMese.entrySet()) {
            String mese = entryMese.getKey();
            Map<String, Double> catToSum = new HashMap<>();
            for (Map.Entry<String, List<Double>> entryCat : entryMese.getValue().entrySet()) {
                double somma = entryCat.getValue().stream().mapToDouble(Double::doubleValue).sum();
                catToSum.put(entryCat.getKey(), somma);
            }
            result.put(mese, catToSum);
        }
        logNestedDoubles("AGGREGATE - kWh/kVARh per mese (somma per categoria)", result);
        return result;
    }

    private Double extractKwhValue(String text) {
        Pattern pattern = Pattern.compile("([0-9]{1,3}(?:\\.[0-9]{3})*(?:,[0-9]{1,2})?)\\s*k(?:Wh|VARh)");
        Matcher matcher = pattern.matcher(text);
        Double lastMatch = null;

        while (matcher.find()) {
            String match = matcher.group(1);
            match = match.replace(".", "").replace(",", ".");
            try {
                lastMatch = Double.parseDouble(match);
            } catch (NumberFormatException e) {
                System.err.println("Errore nel parsing del valore kWh/kVARh: " + match);
            }
        }

        if (lastMatch != null) {
            System.out.println("✅ Valore kWh/kVARh estratto: " + lastMatch);
            return lastMatch;
        } else {
            System.out.println("❌ Nessun valore kWh/kVARh trovato in: " + text);
            return null;
        }
    }

    private Map<String, Map<String, Double>> processSpesePerMese(
            Map<String, Map<String, List<Double>>> spesePerMese) {

        Map<String, Map<String, Double>> speseFinali = new HashMap<>();

        for (Map.Entry<String, Map<String, List<Double>>> entryMese : spesePerMese.entrySet()) {
            String mese = entryMese.getKey();
            Map<String, Double> categorieSomma = new HashMap<>();

            for (Map.Entry<String, List<Double>> entryCat : entryMese.getValue().entrySet()) {
                String categoria = entryCat.getKey();
                double somma = entryCat.getValue().stream().mapToDouble(Double::doubleValue).sum();
                categorieSomma.put(categoria, somma);
            }

            speseFinali.put(mese, categorieSomma);
        }

        logNestedDoubles("AGGREGATE - Spese per mese (somma per categoria)", speseFinali);
        return speseFinali;
    }

    private String estraiMese(String lineText) {
        Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");
        Matcher matcher = pattern.matcher(lineText);

        if (matcher.find()) {
            String dataTrovata = matcher.group(1);
            LocalDate parsedDate = LocalDate.parse(dataTrovata,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            String nomeMese = parsedDate.getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.ITALIAN);

            if (nomeMese != null) {
                logOk("Mese estratto: " + nomeMese);
            }
            return nomeMese;
        }
        return null;
    }

    // Normalizza (maiuscole, spazi singoli, senza accenti)
    private static String norm(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.toUpperCase(Locale.ITALIAN).replaceAll("\\s+", " ").trim();
    }

    // Riconoscimento categoria su riga corrente con contesto di una riga prima e una dopo
    private static String detectCategoriaLetturaSmart(String prev, String curr, String next) {
        String P = norm(prev), C = norm(curr), N = norm(next);
        String PC = (P + " " + C).trim();
        String CN = (C + " " + N).trim();

        // 1) Specifiche prima (anche su due righe)
        if (PC.contains("ENERGIA REATTIVA CAPACITIVA IMMESSA") ||
                CN.contains("ENERGIA REATTIVA CAPACITIVA IMMESSA") ||
                C.contains("ENERGIA REATTIVA CAPACITIVA IMMESSA")) {
            return "Energia Reattiva Capacitiva Immessa";
        }
        if (PC.contains("ENERGIA REATTIVA INDUTTIVA IMMESSA") ||
                CN.contains("ENERGIA REATTIVA INDUTTIVA IMMESSA") ||
                C.contains("ENERGIA REATTIVA INDUTTIVA IMMESSA")) {
            return "Energia Reattiva Induttiva Immessa";
        }

        // 2) Generiche (solo se NON appaiono "IMMESSA" attorno)
        boolean aroundHasImmessa = PC.contains("IMMESSA") || CN.contains("IMMESSA");
        if (!aroundHasImmessa && (C.contains("ENERGIA REATTIVA") || PC.contains("ENERGIA REATTIVA") || CN.contains("ENERGIA REATTIVA"))) {
            return "Energia Reattiva";
        }

        // 3) Altre
        if (C.contains("ENERGIA ATTIVA") || PC.contains("ENERGIA ATTIVA") || CN.contains("ENERGIA ATTIVA")) {
            return "Energia Attiva";
        }
        if (C.contains("POTENZA") || PC.contains("POTENZA") || CN.contains("POTENZA")) {
            return "Potenza";
        }
        return null;
    }


    // ─────────────────────────────────────────────────────────────
// Estrazione letture mese -> categoria -> fascia -> valore (SOMMA se ripetute)
// Con distinzione tra: Reattiva, Reattiva Capacitiva Immessa, Reattiva Induttiva Immessa
// ─────────────────────────────────────────────────────────────
    private Map<String, Map<String, Map<String, Integer>>> extractLetture(Document document) {
        logTitle("extractLetture (multi-line aware: Reattiva / Reattiva C. Immessa / Reattiva I. Immessa separati)");
        Map<String, Map<String, Map<String, Integer>>> lettureMese = new HashMap<>();

        String categoriaCorrente = null;
        String meseCorrente = null;

        NodeList lineNodes = document.getElementsByTagName("Line");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            String curr = lineNodes.item(i).getTextContent();
            if (curr == null) continue;

            String prev = (i > 0) ? lineNodes.item(i - 1).getTextContent() : "";
            String next = (i + 1 < lineNodes.getLength()) ? lineNodes.item(i + 1).getTextContent() : "";

            // 1) Riconosci categoria guardando prev+curr+next (titoli spezzati)
            String maybeCat = detectCategoriaLetturaSmart(prev, curr, next);
            if (maybeCat != null) {
                if (!maybeCat.equals(categoriaCorrente)) {
                    categoriaCorrente = maybeCat;
                    meseCorrente = null; // sarà ricalcolato dalla riga "Fascia oraria ..."
                    logOk("Categoria letture attivata: " + categoriaCorrente);
                }
                continue; // passa alla prossima riga: i valori arrivano sotto
            }

            // 2) Se siamo in una categoria e la riga è una riga di fascia, estrai
            if (categoriaCorrente != null && curr.contains("Fascia oraria")) {
                ArrayList<Date> dates = extractDates(curr);
                if (dates.size() >= 2) {
                    meseCorrente = DateUtils.getMonthFromDateLocalized(dates.get(1));
                }
                if (meseCorrente == null) {
                    meseCorrente = Optional.ofNullable(estraiMese(curr)).orElse("MeseSconosciuto");
                }

                String fascia = extractFasciaOraria(curr);              // F1/F2/F3
                Double value = extractValueFromLine(curr);              // numero (kWh/kVARh/kW)

                if (fascia != null && value != null) {
                    int nuovo = value.intValue();
                    lettureMese
                            .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                            .computeIfAbsent(categoriaCorrente, k -> new HashMap<>())
                            .merge(fascia, nuovo, Integer::sum);

                    int tot = lettureMese.get(meseCorrente).get(categoriaCorrente).get(fascia);
                    System.out.printf("  [+] %s | %s | %s -> +%d (TOT=%d)%n",
                            meseCorrente, categoriaCorrente, fascia, nuovo, tot);
                } else {
                    System.out.println("  (skip) Riga senza fascia/valore utilizzabile: " + curr);
                }
            }
        }

        logLetture("Letture separate (Attiva / Reattiva / Reattiva C. Immessa / Reattiva I. Immessa / Potenza)", lettureMese);
        return lettureMese;
    }



    private static Double extractEuroValue(String lineText) {
        try {
            System.out.println("🧐 Tentativo di estrarre valore monetario da: " + lineText);
            String regex = "€\\s*([0-9]+(?:\\.[0-9]{3})*,[0-9]+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(lineText);

            if (matcher.find()) {
                String valueString = matcher.group(1);
                valueString = valueString.replaceAll("\\.(?=[0-9]{3},)", "").replace(",", ".");
                System.out.println("✅ Valore estratto: " + valueString);
                return Double.parseDouble(valueString);
            } else {
                System.out.println("❌ Nessun valore in € trovato in: " + lineText);
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Errore durante il parsing del valore in euro: " + lineText);
        }
        return null;
    }

    private String extractBollettaNome(Document document) {
        NodeList lineNodes = document.getElementsByTagName("Line");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node lineNode = lineNodes.item(i);
            if (lineNode.getNodeType() == Node.ELEMENT_NODE) {
                String lineText = lineNode.getTextContent();
                if (lineText.contains("Bolletta n")) {
                    String nome = extractBollettaNumero(lineText);
                    if (nome != null) {
                        logOk("Bolletta trovata: n. " + nome);
                    }
                    return nome;
                }
            }
        }
        return null;
    }

    private static String extractFasciaOraria(String lineText) {
        String regex = "F\\d";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lineText);

        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public static String extractBollettaNumero(String lineText) {
        Pattern pattern = Pattern.compile("Bolletta n\\. (\\d+)");
        Matcher matcher = pattern.matcher(lineText);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static ArrayList<Date> extractDates(String lineText) {
        ArrayList<Date> dates = new ArrayList<>();
        Pattern datePattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
        Matcher matcher = datePattern.matcher(lineText);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        while (matcher.find()) {
            String dateString = matcher.group();
            try {
                Date date = new Date(dateFormat.parse(dateString).getTime());
                dates.add(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        logKV("Date trovate nella riga", dates.size());
        return dates;
    }

    private static Double extractValueFromLine(String lineText) {
        try {
            System.out.println("Extracting value from line: " + lineText);
            String regexDateAtStart = "^(\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}\\s+){1,2}";
            String lineTextWithoutDate = lineText.replaceAll(regexDateAtStart, "");
            String lineTextWithoutF = lineTextWithoutDate.replaceFirst("F\\d", "F");
            String valueString = lineTextWithoutF.replaceAll("[^\\d.,-]", "").replace("€", "");
            valueString = valueString.replace(".", "").replace(",", ".");
            return Double.parseDouble(valueString);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing value: " + lineText);
            return null;
        }
    }

    private static Double extractKWhFromLine(String lineText) {
        Pattern pattern = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d+)?)\\s*kWh", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(lineText);
        if (matcher.find()) {
            String value = matcher.group(1).replace(".", "").replace(",", ".");
            Double numero = Double.parseDouble(value);
            return numero;
        }
        return null;
    }

    @Transactional
    public void abbinaPod(int idFile, String idPod) {
        logTitle("abbinaPod");
        logKV("idFile", idFile);
        logKV("idPod", idPod);
        fileRepo.abbinaPod(idFile, idPod);
        logOk("Abbinamento completato");
    }

    @Transactional
    public byte[] getXmlData(int id) {
        logTitle("getXmlData");
        logKV("id", id);
        byte[] data = fileRepo.getFile(id);
        logKV("bytes", (data != null ? data.length : "null"));
        return data;
    }

    @Transactional
    public List<BollettaPodResponse> getDati(int idSessione) {
        logTitle("getDati");
        logKV("idSessione", idSessione);
        var utente = clienteRepo.findById(sessionService.trovaUtentebBySessione(idSessione));
        logKV("utente", (utente != null ? utente.getId() : "null"));
        var pods = podRepo.find("utente", utente).list();
        logKV("pods", (pods != null ? pods.size() : "null"));

        List<BollettaPodResponse> out = pods.stream()
                .flatMap(pod ->
                        BollettaPodRepo.find("idPod", pod.getId())
                                .<BollettaPod>stream())
                .map(BollettaPodResponse::new)
                .collect(Collectors.toList());

        logKV("bollette mappate", out.size());
        return out;
    }

    public List<BollettaPod> getDatiRicalcoli(int idSessione, String idPod) {
        logTitle("getDatiRicalcoli");
        logKV("idSessione", idSessione);
        logKV("idPod", idPod);
        List<BollettaPod> res = BollettaPodRepo.find("idPod", idPod).list();
        logKV("record", res.size());
        return res;
    }

    @Transactional
    public void verificaA2APostRicalcoli(BollettaPod bolletta) {
        logTitle("verificaA2APostRicalcoli");
        logKV("idBolletta", bolletta != null ? bolletta.getId() : "null");
        verBollettaPodService.A2AVerifica(bolletta);
        logOk("Verifica A2A completata");
    }

    @Transactional
    public void controlloRicalcoliInBolletta(byte[] xmlData, String idPod, String nomeB, Integer idSessione) {
        logTitle("controlloRicalcoliInBolletta");
        logKV("idPod", idPod);
        logKV("nomeB", nomeB);
        logKV("idSessione", idSessione);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlData));

            Map<String, Map<String, Double>> ricalcoliPerMese = extractRicalcoliPerMese(document);
            Periodo periodo = extractPeriodo(document);

            if (ricalcoliPerMese.isEmpty()) {
                logWarn("Nessun ricalcolo trovato.");
                return;
            }

            String annoRicalcolo = periodo.getAnno();
            logKV("Periodo (anno ricalcolo)", annoRicalcolo);

            List<BollettaPod> bolletteEsistenti = getDatiRicalcoli(idSessione, idPod);

            for (Map.Entry<String, Map<String, Double>> entry : ricalcoliPerMese.entrySet()) {
                String meseRicalcolo = entry.getKey();
                Map<String, Double> valoriRicalcolati = entry.getValue();

                logTitle("RICALCOLO - Verifica mese: " + meseRicalcolo);
                System.out.println(valoriRicalcolati);

                Optional<BollettaPod> bollettaEsistenteOpt = bolletteEsistenti.stream()
                        .filter(b -> b.getMese().equalsIgnoreCase(meseRicalcolo) &&
                                b.getAnno().equals(annoRicalcolo) &&
                                b.getIdPod().equals(idPod))
                        .findFirst();

                if (bollettaEsistenteOpt.isPresent()) {
                    logOk("RICALCOLO - Bolletta esistente trovata. Aggiorno valori e verifico A2A.");
                    BollettaPod bollettaEsistente = bollettaEsistenteOpt.get();
                    aggiornaBollettaConRicalcoli(bollettaEsistente, valoriRicalcolati);
                    bollettaPodRepo.updateBolletta(bollettaEsistente);
                    verificaA2APostRicalcoli(bollettaEsistente);
                } else {
                    logWarn("RICALCOLO - Nessuna bolletta trovata per " + meseRicalcolo + " " + annoRicalcolo + ". Salvo come nuova voce ricalcolo.");
                    bollettaPodRepo.saveRicalcoliToDatabase(ricalcoliPerMese, idPod, nomeB, periodo);
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    private void aggiornaBollettaConRicalcoli(BollettaPod bolletta, Map<String, Double> valoriRicalcolati) {
        logTitle("aggiornaBollettaConRicalcoli");
        if (valoriRicalcolati.containsKey("Ricalcolo Materia Energia")) {
            bolletta.setSpeseEne(valoriRicalcolati.get("Ricalcolo Materia Energia"));
            logKV("SpeseEnergia", valoriRicalcolati.get("Ricalcolo Materia Energia"));
        }
        if (valoriRicalcolati.containsKey("Ricalcolo Trasporto e Gestione Contatore")) {
            bolletta.setSpeseTrasp(valoriRicalcolati.get("Ricalcolo Trasporto e Gestione Contatore"));
            logKV("Trasporti", valoriRicalcolati.get("Ricalcolo Trasporto e Gestione Contatore"));
        }
        if (valoriRicalcolati.containsKey("Ricalcolo Oneri di Sistema")) {
            bolletta.setOneri(valoriRicalcolati.get("Ricalcolo Oneri di Sistema"));
            logKV("Oneri", valoriRicalcolati.get("Ricalcolo Oneri di Sistema"));
        }
        if (valoriRicalcolati.containsKey("Ricalcolo Imposte")) {
            bolletta.setImposte(valoriRicalcolati.get("Ricalcolo Imposte"));
            logKV("Imposte", valoriRicalcolati.get("Ricalcolo Imposte"));
        }
    }

    private Map<String, Map<String, Double>> extractRicalcoliPerMese(Document document) {
        logTitle("extractRicalcoliPerMese");
        Map<String, Map<String, List<Double>>> ricalcoliPerMese = new HashMap<>();

        String categoriaCorrente = null;
        String meseCorrente = null;
        boolean controlloAttivo = false;
        int righeSenzaEuro = 0;

        String stopParsingKeyword = "TOTALE FORNITURA ENERGIA ELETTRICA E IMPOSTE";

        NodeList lineNodes = document.getElementsByTagName("Line");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node lineNode = lineNodes.item(i);
            if (lineNode.getNodeType() == Node.ELEMENT_NODE) {
                String lineText = lineNode.getTextContent().trim();

                if (lineText.contains(stopParsingKeyword)) {
                    break;
                }

                String meseEstratto = estraiMese(lineText);
                if (meseEstratto != null) {
                    meseCorrente = meseEstratto;
                }

                if (lineText.contains("RICALCOLO PER RETTIFICA SPESA PER LA MATERIA ENERGIA")) {
                    categoriaCorrente = "Ricalcolo Materia Energia";
                    controlloAttivo = false;
                    righeSenzaEuro = 0;
                    continue;
                }
                if (lineText.contains("RICALCOLO PER RETTIFICA SPESA PER IL TRASPORTO E LA GESTIONE")) {
                    categoriaCorrente = "Ricalcolo Trasporto e Gestione Contatore";
                    controlloAttivo = false;
                    righeSenzaEuro = 0;
                    continue;
                }
                if (lineText.contains("RICALCOLO PER RETTIFICA SPESA PER ONERI DI SISTEMA")) {
                    categoriaCorrente = "Ricalcolo Oneri di Sistema";
                    controlloAttivo = false;
                    righeSenzaEuro = 0;
                    continue;
                }
                if (lineText.contains("RICALCOLO PER RETTIFICA IMPOSTE")) {
                    categoriaCorrente = "Ricalcolo Imposte";
                    controlloAttivo = false;
                    righeSenzaEuro = 0;
                    continue;
                }

                if (categoriaCorrente != null && lineText.contains("€")) {
                    Double valore = extractEuroValue(lineText);
                    if (valore != null) {
                        if (meseCorrente == null) {
                            meseCorrente = "MeseSconosciuto";
                        }
                        ricalcoliPerMese
                                .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                                .computeIfAbsent(categoriaCorrente, k -> new ArrayList<>())
                                .add(valore);

                        controlloAttivo = true;
                        righeSenzaEuro = 0;
                    }
                } else if (controlloAttivo) {
                    righeSenzaEuro++;
                    if (righeSenzaEuro >= 10 &&
                            !lineText.matches(".*(QUOTA|Componente|Corrispettivi|€/kWh|€/kW/mese|€/cliente/mese|QUOTA VARIABILE).*")) {
                        categoriaCorrente = null;
                        controlloAttivo = false;
                        righeSenzaEuro = 0;
                    }
                } else {
                    righeSenzaEuro = 0;
                }
            }
        }

        Map<String, Map<String, Double>> agg = processSpesePerMese(ricalcoliPerMese);
        logNestedDoubles("RICALCOLI - Aggregati per mese/categoria", agg);
        return agg;
    }

    @Transactional
    public List<FileDto> getDatiByUserId(int userId) {
        logTitle("getDatiByUserId");
        logKV("userId", userId);
        List<String> podIds = podRepo.getEntityManager()
                .createQuery(
                        "SELECT p.id FROM Pod p WHERE p.utente.id = :userId",
                        String.class
                )
                .setParameter("userId", userId)
                .getResultList();

        logKV("podIds.size", (podIds != null ? podIds.size() : "null"));
        if (podIds == null || podIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<PDFFile> files = PDFFile.find("idPod in ?1", podIds).list();
        logKV("files.size", (files != null ? files.size() : "null"));

        List<FileDto> out = files.stream()
                .map(FileDto::new)
                .collect(Collectors.toList());
        logKV("dto.size", out.size());
        return out;
    }
/*
    @Transactional
    public byte[] generateExcelFromPdfData(byte[] pdfData) throws IOException {
        logTitle("generateExcelFromPdfData");
        if (pdfData == null) logWarn("pdfData è null");
        Document xmlDoc;
        try {
            xmlDoc = convertPdfToXml(pdfData);
        } catch (Exception e) {
            throw new IOException("Errore durante conversione PDF-XML", e);
        }

        Map<String, Map<String, Double>> spesePerMese = extractSpesePerMese(xmlDoc, lettureMese);
        Map<String, Map<String, Double>> kwhPerMese = extractKwhPerMese(xmlDoc);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bolletta");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Mese");
            header.createCell(1).setCellValue("Categoria");
            header.createCell(2).setCellValue("Spesa (€)");
            header.createCell(3).setCellValue("Consumo (kWh)");

            int rowNum = 1;

            Set<String> mesi = new HashSet<>();
            mesi.addAll(spesePerMese.keySet());
            mesi.addAll(kwhPerMese.keySet());

            for (String mese : mesi) {
                Map<String, Double> speseMap = spesePerMese.getOrDefault(mese, Collections.emptyMap());
                Map<String, Double> kwhMap = kwhPerMese.getOrDefault(mese, Collections.emptyMap());

                Set<String> categorie = new HashSet<>();
                categorie.addAll(speseMap.keySet());
                categorie.addAll(kwhMap.keySet());

                for (String categoria : categorie) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(mese);
                    row.createCell(1).setCellValue(categoria);
                    row.createCell(2).setCellValue(speseMap.getOrDefault(categoria, 0.0));
                    row.createCell(3).setCellValue(kwhMap.getOrDefault(categoria, 0.0));
                }
            }

            for (int i = 0; i <= 3; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            bos.close();

            logOk("Excel generato. Bytes=" + bos.size());
            return bos.toByteArray();
        }
    }
 */
}
