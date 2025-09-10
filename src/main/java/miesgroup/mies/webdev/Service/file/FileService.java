package miesgroup.mies.webdev.Service.file;//package miesgroup.mies.webdev.Service;

import io.smallrye.common.constraint.Nullable;
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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
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

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class FileService {

    @Inject FileRepo fileRepo;
    @Inject BollettaPodRepo BollettaPodRepo;
    @Inject BollettaPodService bollettaPodService;
    @Inject SessionService sessionService;
    @Inject PodRepo podRepo;
    @Inject ClienteRepo clienteRepo;
    @Inject verBollettaPodService verBollettaPodService;
    @Inject BudgetService budgetService;
    @Inject BudgetAllService budgetAllService;
    @Inject LetturaRicalcoloBolletta letturaRicalcoloBolletta;
    @Inject LetturaBolletta letturaBolletta;
    @Inject Lettura lettura;

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

    // ─────────────────────────────────────────────────────────────

    // Mappa periodicità → numero di mesi attesi
    private int mesiPerPeriodicita(String periodicita) {
        if (periodicita == null) return 1; // fallback conservativo
        switch (periodicita.trim().toUpperCase()) {
            case "MENSILE":        return 1;
            case "BIMESTRALE":     return 2;
            case "TRIMESTRALE":    return 3;
            case "QUADRIMESTRALE": return 4;
            case "SEMESTRALE":     return 6;
            case "ANNUALE":        return 12;
            default:               return 1; // fallback conservativo
        }
    }

    // Calcola i mesi coperti in modo inclusivo, normalizzando ai limiti di mese
    private int mesiCopertiInclusivi(java.util.Date inizio, java.util.Date fine) {
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        java.time.LocalDate start = inizio.toInstant().atZone(zone).toLocalDate();
        java.time.LocalDate end   = fine.toInstant().atZone(zone).toLocalDate();

        // Normalizza a inizio mese / inizio mese per contare i mesi in modo robusto
        java.time.LocalDate s = start.withDayOfMonth(1);
        java.time.LocalDate e = end.withDayOfMonth(1);

        long diff = java.time.temporal.ChronoUnit.MONTHS.between(s, e);
        return (int) diff + 1; // inclusivo
    }

    // Dispatcher: chiama la funzione giusta in base alla valutazione
    // Se non l'hai già
    enum TipoBolletta { NORMALE, RICALCOLO_SOSPETTO, RICALCOLO }

    // ======================================================
// ENTRY POINT
// ======================================================
    public void processaBolletta(byte[] xmlData, Document doc, String idPod) {
        // 1) Periodo totale
        Periodo periodoTotale = lettura.extractPeriodo(doc);  // es: 01/01/2024 -> 31/10/2024

        // 2) Periodicità dal POD (DB) — fallback: "Mensile"
        String periodicita = podRepo.getPeriodicitaByPodId(idPod); // es. "Mensile", "Bimestrale", ...

        // 3) Prima stima: se range mesi > periodicità -> sospetto ricalcolo
        TipoBolletta tipo = valutaTipoBolletta(periodoTotale, periodicita);

        // 4) Ricerca sicura: "RICALCOLI  PERIODO: dd.mm.yyyy – dd.mm.yyyy"
        Periodo periodoRicalcolo = extractPeriodoRicalcoli(doc); // può essere null
        if (periodoRicalcolo != null) {
            tipo = TipoBolletta.RICALCOLO;
        }
        logKV("Tipo bolletta", tipo.name());

        // 5) Mese/i correnti effettivi = (mesi in periodoTotale) – (mesi in periodoRicalcolo)
        List<YearMonth> mesiCorrentiEffettivi = trovaMesiCorrentiEffettivi(periodoTotale, periodoRicalcolo);
        logKV("Mesi correnti effettivi", mesiCorrentiEffettivi.toString());

        // 6) Dirama
        if (tipo == TipoBolletta.RICALCOLO) {
            // Flusso ricalcolo con range confermato
            letturaRicalcoloBolletta.processaBollettaConRicalcolo(
                    doc,
                    idPod,
                    periodoTotale,
                    periodoRicalcolo,
                    periodicita
            );
        } else if (tipo == TipoBolletta.RICALCOLO_SOSPETTO) {
            // Nessuna riga "RICALCOLI PERIODO", ma range > periodicità: gestisco come ricalcolo "soft"
            if (!mesiCorrentiEffettivi.isEmpty()) {
                letturaRicalcoloBolletta.processaBollettaConRicalcolo(
                        doc,
                        idPod,
                        periodoTotale,
                        null, // ricalcolo non confermato nel PDF
                        periodicita
                );
            } else {
                // Nessun indizio solido di ricalcolo: parsing normale
                letturaBolletta.extractValuesFromXmlA2A(xmlData, idPod);
            }
        } else {
            // NORMALE
            letturaBolletta.extractValuesFromXmlA2A(xmlData, idPod);
        }
    }

    // ======================================================
// DECISIONE TIPO BOLLETTA (range vs periodicità)
// ======================================================
    private TipoBolletta valutaTipoBolletta(Periodo periodoTotale, String periodicita) {
        try {
            if (periodoTotale == null || periodoTotale.getInizio() == null || periodoTotale.getFine() == null) {
                return TipoBolletta.NORMALE;
            }
            YearMonth start = toYearMonth(periodoTotale.getInizio());
            YearMonth end   = toYearMonth(periodoTotale.getFine());
            int mesiNelRange = monthsDiffInclusive(start, end); // inclusivo

            int mesiAttesi = mesiPerPeriodicita(periodicita);   // es: Mensile=1, Bimestrale=2, ...
            if (mesiAttesi <= 0) mesiAttesi = 1;               // default: mensile

            if (mesiNelRange > mesiAttesi) {
                return TipoBolletta.RICALCOLO_SOSPETTO;
            }
            return TipoBolletta.NORMALE;
        } catch (Exception e) {
            return TipoBolletta.NORMALE;
        }
    }

// ======================================================
// ESTRAZIONE PERIODO RICALCOLI DAL PDF
// ======================================================
    /**
     * Cerca la riga:
     *   RICALCOLI
     *   PERIODO: 01.01.2024 – 30.09.2024
     * Restituisce un Periodo (inizio/fine) oppure null se assente.
     */
    private Periodo extractPeriodoRicalcoli(Document document) {
        NodeList lines = document.getElementsByTagName("Line");
        // accetta "-", "–", "—"
        Pattern p = Pattern.compile(
                "PERIODO\\s*:\\s*(\\d{2}[./]\\d{2}[./]\\d{4})\\s*[–—-]\\s*(\\d{2}[./]\\d{2}[./]\\d{4})",
                Pattern.CASE_INSENSITIVE
        );

        java.util.Date inizio = null, fine = null;

        for (int i = 0; i < lines.getLength(); i++) {
            Node n = lines.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            String t = n.getTextContent().trim();
            if (t.toUpperCase(Locale.ITALY).contains("RICALCOLI")) {
                // guarda fino a qualche riga dopo
                for (int k = 0; k <= 6 && i + k < lines.getLength(); k++) {
                    String s = lines.item(i + k).getTextContent().trim();
                    Matcher m = p.matcher(s);
                    if (m.find()) {
                        java.util.Date d1 = parsePuntata1(m.group(1));
                        java.util.Date d2 = parsePuntata1(m.group(2));
                        inizio = d1; fine = d2;
                        break;
                    }
                }
            }
            if (inizio != null && fine != null) break;
        }

        if (inizio != null && fine != null) {
            String anno = String.valueOf(toYearMonth(fine).getYear());
            return new Periodo(inizio, fine, anno);
        }
        return null;
    }

// ======================================================
// MESE/I CORRENTE/I EFFETTIVI
// ======================================================
    /**
     * Restituisce i mesi nel range totale che NON sono compresi nel range ricalcoli.
     * Esempio: totale Jan–Oct, ricalcoli Jan–Sep => ritorna [Oct].
     * Se periodoRicalcolo == null => ritorna l'ultimo mese del range totale.
     */
    private List<YearMonth> trovaMesiCorrentiEffettivi(Periodo periodoTotale, Periodo periodoRicalcolo) {
        List<YearMonth> out = new ArrayList<>();
        if (periodoTotale == null || periodoTotale.getInizio() == null || periodoTotale.getFine() == null) return out;

        List<YearMonth> mesiTot = monthsBetweenInclusive(toYearMonth(periodoTotale.getInizio()), toYearMonth(periodoTotale.getFine()));

        if (periodoRicalcolo == null || periodoRicalcolo.getInizio() == null || periodoRicalcolo.getFine() == null) {
            // Nessun ricalcolo certo: considera il SOLO ultimo mese come "corrente"
            if (!mesiTot.isEmpty()) out.add(mesiTot.get(mesiTot.size() - 1));
            return out;
        }

        List<YearMonth> mesiRic = monthsBetweenInclusive(toYearMonth(periodoRicalcolo.getInizio()), toYearMonth(periodoRicalcolo.getFine()));

        // differenza insiemistica: mesiTot - mesiRic
        for (YearMonth ym : mesiTot) {
            if (!mesiRic.contains(ym)) out.add(ym);
        }
        return out;
    }

    // ======================================================
// UTILS DATA
// ======================================================
    private YearMonth toYearMonth(java.util.Date d) {
        Instant inst = d.toInstant();
        ZonedDateTime zdt = inst.atZone(ZoneId.systemDefault());
        return YearMonth.of(zdt.getYear(), zdt.getMonthValue());
    }

    private List<YearMonth> monthsBetweenInclusive(YearMonth start, YearMonth end) {
        List<YearMonth> res = new ArrayList<>();
        YearMonth cur = start;
        while (!cur.isAfter(end)) {
            res.add(cur);
            cur = cur.plusMonths(1);
        }
        return res;
    }

    private int monthsDiffInclusive(YearMonth start, YearMonth end) {
        return (int) (ChronoUnit.MONTHS.between(start, end) + 1);
    }

    private java.util.Date parsePuntata1(String ddmmyyyy) {
        String[] p = ddmmyyyy.replace('/', '.').split("\\.");
        int d = Integer.parseInt(p[0]); int m = Integer.parseInt(p[1]); int y = Integer.parseInt(p[2]);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, m - 1); c.set(Calendar.DAY_OF_MONTH, d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }



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

    // ─────────────────────────────────────────────────────────────
// FileService - Utility condivise per bollette/ricalcoli
// ─────────────────────────────────────────────────────────────
    public static String fmt(Date d) {
        if (d == null) return "null";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(d);
    }

    public static String ms(long t0, long t1) {
        return String.valueOf(Math.round((t1 - t0) / 1_000_000.0));
    }

    // === Canonizzazione nomi mesi ===
    private static final Map<String, String> MESE_CANON = new HashMap<>();
    static {
        MESE_CANON.put("gennaio","Gennaio"); MESE_CANON.put("febbraio","Febbraio");
        MESE_CANON.put("marzo","Marzo");     MESE_CANON.put("aprile","Aprile");
        MESE_CANON.put("maggio","Maggio");   MESE_CANON.put("giugno","Giugno");
        MESE_CANON.put("luglio","Luglio");   MESE_CANON.put("agosto","Agosto");
        MESE_CANON.put("settembre","Settembre"); MESE_CANON.put("ottobre","Ottobre");
        MESE_CANON.put("novembre","Novembre");   MESE_CANON.put("dicembre","Dicembre");
    }
    public static String canonizzaMese(String raw) {
        if (raw == null) return null;
        String k = raw.trim().toLowerCase(Locale.ITALY);
        return MESE_CANON.getOrDefault(k, raw);
    }

    public static String monthNameOf(Date d) {
        String[] mesi = {"Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno","Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre"};
        Calendar c = Calendar.getInstance(); c.setTime(d);
        return mesi[c.get(Calendar.MONTH)];
    }
    public static String monthNameOf(YearMonth ym) {
        String nome = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ITALIAN);
        return canonizzaMese(nome);
    }
    public static YearMonth ymOf(Date d) {
        Calendar c = Calendar.getInstance(); c.setTime(d);
        return YearMonth.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }
    public static int yearOf(Date d) {
        Calendar c = Calendar.getInstance(); c.setTime(d);
        return c.get(Calendar.YEAR);
    }
    public static String capitalizeFirstThree(String mese) {
        if (mese == null || mese.isEmpty()) return mese;
        return mese.substring(0,1).toUpperCase() + mese.substring(1, Math.min(3, mese.length())).toLowerCase();
    }

    public static Periodo toPeriodoMeseFromDate(Date anyDayInMonth) {
        Calendar c = Calendar.getInstance();
        c.setTime(anyDayInMonth);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        Date inizio = c.getTime();
        c.add(Calendar.MONTH, 1);
        c.add(Calendar.DAY_OF_MONTH, -1);
        c.set(Calendar.HOUR_OF_DAY,23); c.set(Calendar.MINUTE,59); c.set(Calendar.SECOND,59); c.set(Calendar.MILLISECOND,999);
        Date fine = c.getTime();
        String anno = String.valueOf(yearOf(anyDayInMonth));
        return new Periodo(inizio, fine, anno);
    }

    // stampa Map<String, Map<String, Double>>
    public static void logNestedDoubleMap(Map<String, Map<String, Double>> m) {
        if (m == null || m.isEmpty()) { System.out.println("   • <vuoto>"); return; }
        m.forEach((k,v) -> System.out.println("   • " + k + " -> " + v));
    }
    // stampa Map<String, Map<String, Map<String, Integer>>>
    public static void logNestedIntMap(Map<String, Map<String, Map<String, Integer>>> m) {
        if (m == null || m.isEmpty()) { System.out.println("   • <vuoto>"); return; }
        m.forEach((mese,cat) -> System.out.println("   • " + mese + " -> " + cat));
    }

    public static boolean containsKeyLike(Map<String, Double> map, String... tokens) {
        if (map == null || map.isEmpty()) return false;
        for (String key : map.keySet()) {
            String low = key.toLowerCase(Locale.ITALIAN);
            boolean all = true;
            for (String t : tokens) {
                if (!low.contains(t.toLowerCase(Locale.ITALIAN))) { all = false; break; }
            }
            if (all) return true;
        }
        return false;
    }

    // Costruisce la mappa letture per saveDataToDatabase a partire dal blocco "Contatore"
    public static Map<String, Map<String, Map<String, Integer>>> buildLettureStubFromMisure(
            String meseCorrente,
            List<LetturaRicalcoloBolletta.VoceMisura> misureMese
    ) {
        Map<String, Map<String, Map<String, Integer>>> out = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> catFasce = new LinkedHashMap<>();
        for (LetturaRicalcoloBolletta.VoceMisura vm : misureMese) {
            int lastSpace = vm.item.lastIndexOf(' ');
            if (lastSpace <= 0) continue;
            String categoria = vm.item.substring(0, lastSpace).trim(); // es. "Energia Attiva"
            String fascia    = vm.item.substring(lastSpace + 1).trim(); // es. "F1"
            int valore       = (int)Math.round(vm.consumo);
            catFasce.computeIfAbsent(categoria, k -> new LinkedHashMap<>()).merge(fascia, valore, Integer::sum);
        }
        out.put(meseCorrente, catFasce);
        return out;
    }

    // Normalizza mesi ("ottobre" -> "Ottobre") e fonde duplicati sommando i valori
    public static Map<String, Map<String, Double>> normalizeAndMergeNested(Map<String, Map<String, Double>> in) {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        if (in == null) return out;
        in.forEach((mese, inner) -> {
            String canon = canonizzaMese(mese);
            Map<String, Double> dest = out.computeIfAbsent(canon, k -> new LinkedHashMap<>());
            if (inner != null) inner.forEach((k,v) -> dest.merge(k, v, Double::sum));
        });
        return out;
    }

    // Merge 2 mappe annidate (somma valori su chiavi duplicate)
    public static Map<String, Map<String, Double>> mergeNestedDoubleMaps(
            Map<String, Map<String, Double>> a,
            Map<String, Map<String, Double>> b
    ) {
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        if (a != null) a.forEach((mese,inner) -> out.put(mese, new LinkedHashMap<>(inner)));
        if (b != null) b.forEach((mese,inner) -> {
            Map<String, Double> dest = out.computeIfAbsent(mese, k -> new LinkedHashMap<>());
            inner.forEach((k,v) -> dest.merge(k, v, Double::sum));
        });
        return out;
    }

    // (opzionale) Deriva kWh da misure Attiva F1/F2/F3 se vuoi riutilizzarlo
    public static Map<String, Map<String, Double>> kwhFromMisureAttiva(
            String meseCorrente,
            List<LetturaRicalcoloBolletta.VoceMisura> misureMese
    ) {
        double f1 = 0, f2 = 0, f3 = 0;
        for (LetturaRicalcoloBolletta.VoceMisura vm : misureMese) {
            if (vm.item.startsWith("Energia Attiva ")) {
                if (vm.item.endsWith("F1")) f1 += vm.consumo;
                else if (vm.item.endsWith("F2")) f2 += vm.consumo;
                else if (vm.item.endsWith("F3")) f3 += vm.consumo;
            }
        }
        Map<String, Map<String, Double>> out = new LinkedHashMap<>();
        Map<String, Double> cat = new LinkedHashMap<>();
        if (f1 > 0) cat.put("Materia energia f1", f1);
        if (f2 > 0) cat.put("Materia energia f2", f2);
        if (f3 > 0) cat.put("Materia energia f3", f3);
        double f0 = f1 + f2 + f3;
        if (f0 > 0) cat.put("Materia energia f0", f0);
        out.put(meseCorrente, cat);
        return out;
    }

}
