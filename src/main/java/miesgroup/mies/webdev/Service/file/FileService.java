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
import miesgroup.mies.webdev.Repository.file.FileRepo;
import miesgroup.mies.webdev.Rest.Model.BollettaPodResponse;
import miesgroup.mies.webdev.Rest.Model.FileDto;
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
import java.io.IOException;
import java.time.*;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    @Inject verBollettaPodService verBollettaService;
    @Inject LetturaBollettaNuova letturaBollettaNuova;

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

    // Dispatcher: chiama la funzione giusta in base alla valutazione
    enum TipoBolletta { NORMALE, RICALCOLO_SOSPETTO, RICALCOLO }

    // ======================================================
    // ENTRY POINT
    // ======================================================
    // ======================================================
// ENTRY POINT - VERSIONE AGGIORNATA PER DUAL FORMAT
// ======================================================
    public void processaBolletta(byte[] xmlData, Document doc, String idPod) {
        logTitle("processaBolletta - Inizio analisi");

        // 1) PROVA AD ESTRARRE IL PERIODO CON ENTRAMBI I METODI
        Periodo periodoTotale = null;
        boolean isNewFormat = false;

        try {
            // Prova prima con il metodo nuovo
            periodoTotale = letturaBollettaNuova.extractPeriodo(doc);
            if (periodoTotale != null && periodoTotale.getInizio() != null) {
                logOk("Periodo estratto con metodo NUOVO");
                isNewFormat = true;
            }
        } catch (Exception e) {
            logWarn("Metodo nuovo fallito: " + e.getMessage());
        }

        if (periodoTotale == null) {
            try {
                // Fallback al metodo vecchio
                periodoTotale = lettura.extractPeriodo(doc);
                if (periodoTotale != null && periodoTotale.getInizio() != null) {
                    logOk("Periodo estratto con metodo VECCHIO");
                    isNewFormat = false;
                }
            } catch (Exception e) {
                logWarn("Metodo vecchio fallito: " + e.getMessage());
            }
        }

        if (periodoTotale == null || periodoTotale.getInizio() == null) {
            logWarn("⚠️ Impossibile estrarre periodo. Abbandono.");
            return;
        }

        logKV("Periodo estratto", fmt(periodoTotale.getInizio()) + " → " + fmt(periodoTotale.getFine()));

        // 2) DETERMINA IL FORMATO IN BASE ALLA DATA
        // Se il periodo inizia da giugno 2025 in poi → nuovo formato
        Calendar cal = Calendar.getInstance();
        cal.setTime(periodoTotale.getFine()); // usa la data di fine
        int anno = cal.get(Calendar.YEAR);
        int mese = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH è 0-based

        // Giugno 2025 o dopo → nuovo formato
        boolean useNewFormat = (anno > 2025) || (anno == 2025 && mese >= 6);

        logKV("Anno bolletta", anno);
        logKV("Mese bolletta", mese);
        logKV("Formato da usare", useNewFormat ? "NUOVO" : "VECCHIO");

        // 3) SE È NUOVO FORMATO, ESEGUI DIRETTAMENTE (SENZA RICALCOLI)
        if (useNewFormat) {
            logOk("📋 Processamento con NUOVO FORMATO (ignora ricalcoli)");
            letturaBollettaNuova.extractValuesFromXmlNewFormat(xmlData, idPod);

            // 🔵 Budget Sync: mesi del documento
            syncBudgetAndBudgetAll(
                    idPod,
                    monthsBetweenInclusive(
                            toYearMonth(periodoTotale.getInizio()),
                            toYearMonth(periodoTotale.getFine())
                    )
            );

            try {
                verBollettaService.verificaBolletteDaPeriodo(idPod, periodoTotale.getInizio(), periodoTotale.getFine());
                logOk("Verifica A2A completata per POD: " + idPod);
            } catch (Exception e) {
                logWarn("Errore verifica A2A: " + e.getMessage());
            }

            logOk("✅ Processamento NUOVO formato completato");
            return;
        }

        // 4) FORMATO VECCHIO: APPLICA LOGICA RICALCOLO
        logOk("📋 Processamento con VECCHIO FORMATO (con gestione ricalcoli)");

        // Periodicità dal POD (DB) — fallback: "Mensile"
        String periodicita = podRepo.getPeriodicitaByPodId(idPod);
        logKV("Periodicità POD", periodicita);

        // Prima stima: se range mesi > periodicità → sospetto ricalcolo
        TipoBolletta tipo = valutaTipoBolletta(periodoTotale, periodicita);

        // Ricerca sicura: "RICALCOLI PERIODO: dd.mm.yyyy – dd.mm.yyyy"
        Periodo periodoRicalcolo = extractPeriodoRicalcoli(doc);
        if (periodoRicalcolo != null) {
            tipo = TipoBolletta.RICALCOLO;
            logOk("✅ Trovato RICALCOLI PERIODO nel documento");
        }

        logKV("Tipo bolletta", tipo.name());

        // Mese/i correnti effettivi = (mesi in periodoTotale) – (mesi in periodoRicalcolo)
        List<YearMonth> mesiCorrentiEffettivi = trovaMesiCorrentiEffettivi(periodoTotale, periodoRicalcolo);
        logKV("Mesi correnti effettivi", mesiCorrentiEffettivi.toString());

        // 5) DIRAMA + BUDGET SYNC (SOLO PER FORMATO VECCHIO)
        if (tipo == TipoBolletta.RICALCOLO) {
            // Flusso ricalcolo con range confermato
            letturaRicalcoloBolletta.processaBollettaConRicalcolo(
                    doc,
                    idPod,
                    periodoTotale,
                    periodoRicalcolo,
                    periodicita
            );

            try {
                verBollettaService.verificaBolletteDaPeriodo(idPod, periodoTotale.getInizio(), periodoTotale.getFine());
                logOk("Verifica A2A completata per POD: " + idPod);
            } catch (Exception e) {
                logWarn("Errore verifica A2A: " + e.getMessage());
            }

            // 🔵 Budget Sync: mesi ricalcolo + mesi correnti
            List<YearMonth> target = new ArrayList<>();
            target.addAll(monthsBetweenInclusive(toYearMonth(periodoRicalcolo.getInizio()), toYearMonth(periodoRicalcolo.getFine())));
            target.addAll(mesiCorrentiEffettivi);
            target = target.stream().distinct().collect(Collectors.toList());
            syncBudgetAndBudgetAll(idPod, target);

        } else if (tipo == TipoBolletta.RICALCOLO_SOSPETTO) {
            // Nessuna riga "RICALCOLI PERIODO", ma range > periodicità
            if (!mesiCorrentiEffettivi.isEmpty()) {
                letturaRicalcoloBolletta.processaBollettaConRicalcolo(
                        doc,
                        idPod,
                        periodoTotale,
                        null, // ricalcolo non confermato
                        periodicita
                );

                try {
                    verBollettaService.verificaBolletteDaPeriodo(idPod, periodoTotale.getInizio(), periodoTotale.getFine());
                    logOk("Verifica A2A completata per POD: " + idPod);
                } catch (Exception e) {
                    logWarn("Errore verifica A2A: " + e.getMessage());
                }

                // 🔵 Budget Sync: tutti i mesi del documento
                syncBudgetAndBudgetAll(
                        idPod,
                        monthsBetweenInclusive(toYearMonth(periodoTotale.getInizio()), toYearMonth(periodoTotale.getFine()))
                );
            } else {
                // NORMALE
                letturaBolletta.extractValuesFromXmlA2A(xmlData, idPod);

                // 🔵 Budget Sync: mesi del documento
                syncBudgetAndBudgetAll(
                        idPod,
                        monthsBetweenInclusive(toYearMonth(periodoTotale.getInizio()), toYearMonth(periodoTotale.getFine()))
                );
            }
        } else {
            // NORMALE
            letturaBolletta.extractValuesFromXmlA2A(xmlData, idPod);

            // 🔵 Budget Sync: mesi del documento
            syncBudgetAndBudgetAll(
                    idPod,
                    monthsBetweenInclusive(toYearMonth(periodoTotale.getInizio()), toYearMonth(periodoTotale.getFine()))
            );
        }

        logOk("✅ Processamento VECCHIO formato completato");
    }


    private boolean isNuovoFormatoBolletta(Periodo periodo) {
        // Semplice check: da luglio 2025 in poi bolletta considerata nuova
        Calendar cutoff = Calendar.getInstance();
        cutoff.set(2025, Calendar.JUNE, 30, 23, 59, 59);
        Date cutoffDate = cutoff.getTime();
        return periodo.getInizio().after(cutoffDate) || periodo.getFine().after(cutoffDate);
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

    // ======================================================
    // FILE CRUD
    // ======================================================

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
        String[] mesi = {"Gennaio","Febbraio","Marzo","Aprile","Maggio","Giugno","Luglio","Agosto","Settembre","Ottobre","Novembre","Dicembre"};
        return mesi[ym.getMonthValue()-1];
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
        Map<String, Map<String, Integer>> catFasce = new LinkedHashMap<>();
        for (LetturaRicalcoloBolletta.VoceMisura vm : misureMese) {
            int lastSpace = vm.item.lastIndexOf(' ');
            if (lastSpace <= 0) continue;
            String categoria = vm.item.substring(0, lastSpace).trim(); // es. "Energia Attiva"
            String fascia    = vm.item.substring(lastSpace + 1).trim(); // es. "F1"
            int valore       = (int)Math.round(vm.consumo);
            catFasce.computeIfAbsent(categoria, k -> new LinkedHashMap<>()).merge(fascia, valore, Integer::sum);
        }
        Map<String, Map<String, Map<String, Integer>>> out = new LinkedHashMap<>();
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

    // ======================================================
    // 🔵 BUDGET SYNC (Budget + BudgetAll)
    // ======================================================
    @Transactional
    protected void syncBudgetAndBudgetAll(String idPod, List<YearMonth> mesi) {
        if (mesi == null || mesi.isEmpty()) return;
        try {
            Pod pod = podRepo.findById(idPod);
            if (pod == null) {
                logWarn("[BudgetSync] POD non trovato: " + idPod);
                return;
            }
            Cliente utente = pod.getUtente();
            int idUtente = utente.getId();

            EntityManager em = BollettaPodRepo.getEntityManager();

            for (YearMonth ym : mesi) {
                int anno = ym.getYear();
                int mese = ym.getMonthValue();
                String meseNome = monthNameOf(ym);

                // --- Aggregati bolletta per POD/mese/anno
                Object[] arr = (Object[]) em.createNativeQuery(
                                "SELECT " +
                                        " COALESCE(SUM(TOT_Att),0), " +
                                        " COALESCE(SUM(Spese_Ene),0), " +
                                        " COALESCE(SUM(COALESCE(Spese_Trasp,0)+COALESCE(Oneri,0)),0) " +
                                        "FROM bolletta_pod WHERE id_pod=:pod AND Anno=:anno AND Mese=:mese"
                        )
                        .setParameter("pod", idPod)
                        .setParameter("anno", String.valueOf(anno))
                        .setParameter("mese", meseNome)
                        .getSingleResult();

                double consumoTot       = ((Number) arr[0]).doubleValue();
                double spesaEnergiaTot  = ((Number) arr[1]).doubleValue();
                double oneriTot         = ((Number) arr[2]).doubleValue();

                // Invece di lanciare warn, fai solo skip su mesi senza dati
                if (consumoTot <= 0 && spesaEnergiaTot <= 0 && oneriTot <= 0) {
                    logKV("[BudgetSync] Skip mese senza dati", monthNameOf(ym) + " " + anno + " per POD " + idPod);
                    continue;
                }

                // --- Upsert BUDGET (per POD)
                upsertBudgetBase(em, idPod, idUtente, anno, mese, consumoTot, spesaEnergiaTot, oneriTot);

                // --- Aggregati ALL (tutti i POD dell'utente)
                Object[] aggAll = (Object[]) em.createNativeQuery(
                                "SELECT " +
                                        " COALESCE(SUM(TOT_Att),0), " +
                                        " COALESCE(SUM(Spese_Ene),0), " +
                                        " COALESCE(SUM(COALESCE(Spese_Trasp,0)+COALESCE(Oneri,0)),0) " +
                                        "FROM bolletta_pod " +
                                        "WHERE id_pod IN (SELECT id_pod FROM pod WHERE id_utente=:idUtente) " +
                                        "AND Anno=:anno AND Mese=:mese"
                        )
                        .setParameter("idUtente", idUtente)
                        .setParameter("anno", String.valueOf(anno))
                        .setParameter("mese", meseNome)
                        .getSingleResult();

                double consAll   = ((Number) aggAll[0]).doubleValue();
                double spesaAll  = ((Number) aggAll[1]).doubleValue();
                double oneriAll  = ((Number) aggAll[2]).doubleValue();

                if (consAll <= 0 && spesaAll <= 0 && oneriAll <= 0) {
                    logKV("[BudgetSync] Skip ALL senza dati", monthNameOf(ym) + " " + anno + " user=" + idUtente);
                    continue;
                }

                // --- Upsert BUDGET_ALL (id_pod='ALL')
                try {
                    BudgetAll nuovo = new BudgetAll();
                    nuovo.setIdPod("ALL");
                    nuovo.setAnno(anno);
                    nuovo.setMese(mese);
                    nuovo.setCliente(utente);
                    nuovo.setPrezzoEnergiaBase(spesaAll);
                    nuovo.setConsumiBase(consAll);
                    nuovo.setOneriBase(oneriAll);
                    // percentuali di default: preservate dalla repo se già presenti
                    nuovo.setPrezzoEnergiaPerc(0d);
                    nuovo.setConsumiPerc(0d);
                    nuovo.setOneriPerc(0d);

                    // metodo service/repo che fa insert/update senza esplodere su unique key
                    budgetAllService.upsertAggregato(nuovo);
                } catch (Throwable ex) {
                    // Fallback: MySQL upsert nativo (se il service non espone upsert)
                    logWarn("[BudgetSync] upsertAggregato non disponibile, uso fallback SQL: " + ex.getMessage());
                    em.createNativeQuery(
                                    "INSERT INTO budget_all (id_pod, anno, mese, prezzo_energia_base, consumi_base, oneri_base, " +
                                            "prezzo_energia_perc, consumi_perc, oneri_perc, Id_Utente, editable) " +
                                            "VALUES ('ALL', :anno, :mese, :spesa, :consumo, :oneri, 0, 0, 0, :idUtente, 1) " +
                                            "ON DUPLICATE KEY UPDATE " +
                                            "prezzo_energia_base=VALUES(prezzo_energia_base), " +
                                            "consumi_base=VALUES(consumi_base), " +
                                            "oneri_base=VALUES(oneri_base)"
                            )
                            .setParameter("anno", anno)
                            .setParameter("mese", mese)
                            .setParameter("spesa", spesaAll)
                            .setParameter("consumo", consAll)
                            .setParameter("oneri", oneriAll)
                            .setParameter("idUtente", idUtente)
                            .executeUpdate();
                }
            }
        } catch (Exception e) {
            logWarn("[BudgetSync] Errore generale syncBudgetAndBudgetAll: " + e.getMessage());
        }
    }

    private void upsertBudgetBase(EntityManager em, String pod, int idUtente, int anno, int mese,
                                  double consumo, double spesaEnergia, double oneri) {
        // Verifica se esiste già
        List<?> res = em.createNativeQuery(
                        "SELECT id FROM budget WHERE id_pod=:pod AND anno=:anno AND mese=:mese"
                )
                .setParameter("pod", pod)
                .setParameter("anno", anno)
                .setParameter("mese", mese)
                .getResultList();

        if (res == null || res.isEmpty()) {
            // insert
            em.createNativeQuery(
                            "INSERT INTO budget (id_pod, anno, mese, prezzo_energia_base, consumi_base, oneri_base, " +
                                    "prezzo_energia_perc, consumi_perc, oneri_perc, Id_Utente) " +
                                    "VALUES (:pod, :anno, :mese, :spesa, :consumo, :oneri, 0, 0, 0, :idUtente)"
                    )
                    .setParameter("pod", pod)
                    .setParameter("anno", anno)
                    .setParameter("mese", mese)
                    .setParameter("spesa", spesaEnergia)
                    .setParameter("consumo", consumo)
                    .setParameter("oneri", oneri)
                    .setParameter("idUtente", idUtente)
                    .executeUpdate();
            logOk(String.format("[BudgetSync] INSERT budget %s %02d/%d (kWh=%.2f, energia=%.2f€, oneri=%.2f€)",
                    pod, mese, anno, consumo, spesaEnergia, oneri));
        } else {
            // update
            Number id = (Number) res.get(0);
            em.createNativeQuery(
                            "UPDATE budget SET prezzo_energia_base=:spesa, consumi_base=:consumo, oneri_base=:oneri " +
                                    "WHERE id=:id"
                    )
                    .setParameter("spesa", spesaEnergia)
                    .setParameter("consumo", consumo)
                    .setParameter("oneri", oneri)
                    .setParameter("id", id.longValue())
                    .executeUpdate();
            logOk(String.format("[BudgetSync] UPDATE budget %s %02d/%d (kWh=%.2f, energia=%.2f€, oneri=%.2f€)",
                    pod, mese, anno, consumo, spesaEnergia, oneri));
        }
    }
}
