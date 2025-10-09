package miesgroup.mies.webdev.Service.file;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import miesgroup.mies.webdev.Model.Periodo;
import miesgroup.mies.webdev.Repository.file.FileRepo;
import miesgroup.mies.webdev.Service.DateUtils;
import miesgroup.mies.webdev.Service.LogCustom;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe per l'estrazione di dati dalle bollette A2A nel NUOVO formato (da Luglio 2025 in poi).
 * Il nuovo layout presenta "Scontrino dell'Energia" e struttura semplificata.
 */
@ApplicationScoped
public class LetturaBollettaNuova {

    @Inject
    FileRepo fileRepo;

    // ============================================================================
    // ESTRAZIONE NOME/NUMERO BOLLETTA
    // ============================================================================


    public void extractValuesFromXmlNewFormat(byte[] xmlData, String idPod) {
        LogCustom.logTitle("=== ESTRAZIONE BOLLETTA NUOVO FORMATO ===");

        // Converti XML in Document
        Document doc = convertBytesToDocument(xmlData);

        // Chiama i metodi di estrazione principali
        String numeroBolletta = extractBollettaNome(doc);
        Periodo periodo = extractPeriodo(doc);
        String pod = extractPod(doc);
        String indirizzo = extractIndirizzoFornitura(doc);
        Double potenza = extractPotenzaImpegnata(doc);
        Integer consumoTotale = extractConsumoTotaleFatturato(doc);
        Double totale = extractTotaleDaPagare(doc);
        Map<String, Map<String, Map<String, Integer>>> letture = extractLetture(doc);
        Map<String, Map<String, Double>> spese = extractSpesePerMese(doc);
        Map<String, Map<String, Double>> componenti = extractDettaglioComponenti(doc);
        Map<String, Map<String, Double>> speseDettagliate = extractSpeseDettagliate(doc);
        Map<String, Object> datiLettureConsumi = extractDatiLettureConsumi(doc);

        // ========================================================================
        // PREPARAZIONE DATI PER IL SALVATAGGIO NEL DATABASE
        // ========================================================================

        // Combina le letture dai due metodi di estrazione
        Map<String, Map<String, Map<String, Integer>>> lettureMeseFinale = combinaLetture(letture, datiLettureConsumi);

        // Combina le spese dai vari metodi
        Map<String, Map<String, Double>> spesePerMeseFinale = combinaSpese(spese, componenti, speseDettagliate);

        // Prepara kWhPerMese dai dati estratti
        Map<String, Map<String, Double>> kWhPerMese = preparaKWhPerMese(datiLettureConsumi, speseDettagliate);

        // ========================================================================
        // STAMPA RIEPILOGO DATI ESTRATTI
        // ========================================================================

        LogCustom.logTitle("=== RIEPILOGO DATI ESTRATTI ===");

        LogCustom.logKV("Numero Bolletta", numeroBolletta != null ? numeroBolletta : "NON TROVATO");
        LogCustom.logKV("POD", pod != null ? pod : "NON TROVATO");
        LogCustom.logKV("Indirizzo Fornitura", indirizzo != null ? indirizzo : "NON TROVATO");
        LogCustom.logKV("Potenza Impegnata", potenza != null ? potenza + " kW" : "NON TROVATO");
        LogCustom.logKV("Consumo Totale Fatturato", consumoTotale != null ? consumoTotale + " kWh" : "NON TROVATO");
        LogCustom.logKV("Totale da Pagare", totale != null ? totale + " €" : "NON TROVATO");

        if (periodo != null) {
            LogCustom.logKV("Periodo Inizio", periodo.getInizio().toString());
            LogCustom.logKV("Periodo Fine", periodo.getFine().toString());
            LogCustom.logKV("Anno", periodo.getAnno());
        } else {
            LogCustom.logWarn("Periodo: NON TROVATO");
        }

        // Stampa letture
        LogCustom.logTitle("--- LETTURE ---");
        if (lettureMeseFinale != null && !lettureMeseFinale.isEmpty()) {
            for (Map.Entry<String, Map<String, Map<String, Integer>>> mese : lettureMeseFinale.entrySet()) {
                LogCustom.logOk("Mese: " + mese.getKey());
                for (Map.Entry<String, Map<String, Integer>> tipo : mese.getValue().entrySet()) {
                    System.out.println("  Tipo: " + tipo.getKey());
                    for (Map.Entry<String, Integer> fascia : tipo.getValue().entrySet()) {
                        LogCustom.logKV("    " + fascia.getKey(), fascia.getValue().toString());
                    }
                }
            }
        } else {
            LogCustom.logWarn("Nessuna lettura estratta");
        }

        // Stampa spese per mese
        LogCustom.logTitle("--- SPESE PER MESE ---");
        if (spesePerMeseFinale != null && !spesePerMeseFinale.isEmpty()) {
            for (Map.Entry<String, Map<String, Double>> mese : spesePerMeseFinale.entrySet()) {
                LogCustom.logOk("Mese: " + mese.getKey());
                for (Map.Entry<String, Double> voce : mese.getValue().entrySet()) {
                    LogCustom.logKV("  " + voce.getKey(), voce.getValue() + " €");
                }
            }
        } else {
            LogCustom.logWarn("Nessuna spesa per mese estratta");
        }

        LogCustom.logTitle("--- DATI LETTURE E CONSUMI DETTAGLIATI ---");
        if (datiLettureConsumi != null && !datiLettureConsumi.isEmpty()) {
            LogCustom.logKV("Matricola Contatore", (String) datiLettureConsumi.get("matricolaContatore"));

            printEnergiaMap("Energia Attiva", (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaAttiva"), "kWh");
            printEnergiaMap("Energia Reattiva", (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaReattiva"), "kvarh");
            printEnergiaMap("Potenza", (Map<String, Map<String, Double>>) datiLettureConsumi.get("potenza"), "kW");
            printEnergiaMap("Energia Reattiva Capacitiva", (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaReativaCapacitiva"), "kvarh");
            printEnergiaMap("Energia Reattiva Induttiva", (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaReativaInduttiva"), "kvarh");
        } else {
            LogCustom.logWarn("Nessun dato letture e consumi dettagliato estratto");
        }

        LogCustom.logTitle("=== FINE RIEPILOGO ===");

        // ========================================================================
        // SALVATAGGIO NEL DATABASE
        // ========================================================================

        if (periodo != null && numeroBolletta != null && pod != null) {
            LogCustom.logTitle("=== INIZIO SALVATAGGIO DATABASE ===");
            try {
                fileRepo.saveDataToDatabase(
                        lettureMeseFinale,
                        spesePerMeseFinale,
                        pod,
                        numeroBolletta,
                        periodo,
                        kWhPerMese
                );
                LogCustom.logOk("✅ Dati salvati con successo nel database");
            } catch (Exception e) {
                LogCustom.logWarn("⛔️ Errore durante il salvataggio nel database: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            LogCustom.logWarn("⚠️ Salvataggio database saltato: dati essenziali mancanti (periodo, numeroBolletta o POD)");
        }

        LogCustom.logOk("Processata nuova bolletta per POD " + idPod + ", n. " + numeroBolletta);
    }

// ============================================================================
// METODI HELPER PER PREPARAZIONE DATI DATABASE
// ============================================================================

    private Map<String, Map<String, Map<String, Integer>>> combinaLetture(
            Map<String, Map<String, Map<String, Integer>>> lettureBase,
            Map<String, Object> datiLettureConsumi) {

        Map<String, Map<String, Map<String, Integer>>> risultato = new HashMap<>();

        // Aggiungi letture base se presenti
        if (lettureBase != null) {
            risultato.putAll(lettureBase);
        }

        // Aggiungi dati da datiLettureConsumi
        if (datiLettureConsumi != null) {
            String mese = estraiMeseDaPeriodo(); // Dedurre dal periodo corrente

            Map<String, Map<String, Integer>> datiMese = risultato.computeIfAbsent(mese, k -> new HashMap<>());

            // Energia Attiva
            aggiungiEnergiaDaConsumi(datiMese, "Energia Attiva",
                    (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaAttiva"));

            // Energia Reattiva
            aggiungiEnergiaDaConsumi(datiMese, "Energia Reattiva",
                    (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaReattiva"));

            // Potenza
            aggiungiEnergiaDaConsumi(datiMese, "Potenza",
                    (Map<String, Map<String, Double>>) datiLettureConsumi.get("potenza"));

            // Energia Reattiva Capacitiva Immessa
            aggiungiEnergiaDaConsumi(datiMese, "Energia Reattiva Capacitiva Immessa",
                    (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaReativaCapacitiva"));

            // Energia Reattiva Induttiva Immessa
            aggiungiEnergiaDaConsumi(datiMese, "Energia Reattiva Induttiva Immessa",
                    (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaReativaInduttiva"));
        }

        return risultato;
    }

    private void aggiungiEnergiaDaConsumi(Map<String, Map<String, Integer>> datiMese,
                                          String tipoEnergia,
                                          Map<String, Map<String, Double>> energiaMap) {
        if (energiaMap != null && !energiaMap.isEmpty()) {
            Map<String, Integer> fasce = datiMese.computeIfAbsent(tipoEnergia, k -> new HashMap<>());
            energiaMap.forEach((periodo, fasceDouble) -> {
                fasceDouble.forEach((fascia, valore) -> {
                    fasce.put(fascia, valore.intValue());
                });
            });
        }
    }

    private Map<String, Map<String, Double>> combinaSpese(
            Map<String, Map<String, Double>> spese,
            Map<String, Map<String, Double>> componenti,
            Map<String, Map<String, Double>> speseDettagliate) {

        Map<String, Map<String, Double>> risultato = new HashMap<>();

        // Unisci tutte le mappe di spese
        if (spese != null) {
            spese.forEach((mese, voci) -> {
                risultato.computeIfAbsent(mese, k -> new HashMap<>()).putAll(voci);
            });
        }

        if (componenti != null) {
            componenti.forEach((mese, voci) -> {
                risultato.computeIfAbsent(mese, k -> new HashMap<>()).putAll(voci);
            });
        }

        // Aggiungi speseDettagliate mappando le sezioni
        if (speseDettagliate != null) {
            String mese = estraiMeseDaPeriodo();
            Map<String, Double> vociMese = risultato.computeIfAbsent(mese, k -> new HashMap<>());

            speseDettagliate.forEach((sezione, voci) -> {
                voci.forEach((voce, valore) -> {
                    String chiaveFinale = sezione + " - " + voce;
                    vociMese.put(chiaveFinale, valore);
                });
            });
        }

        return risultato;
    }

    private Map<String, Map<String, Double>> preparaKWhPerMese(
            Map<String, Object> datiLettureConsumi,
            Map<String, Map<String, Double>> speseDettagliate) {

        Map<String, Map<String, Double>> kWhPerMese = new HashMap<>();
        String mese = estraiMeseDaPeriodo();
        Map<String, Double> kwhMese = kWhPerMese.computeIfAbsent(mese, k -> new HashMap<>());

        // Estrai kWh da datiLettureConsumi se disponibili
        if (datiLettureConsumi != null) {
            Map<String, Map<String, Double>> energiaAttiva =
                    (Map<String, Map<String, Double>>) datiLettureConsumi.get("energiaAttiva");

            if (energiaAttiva != null) {
                energiaAttiva.forEach((periodo, fasce) -> {
                    fasce.forEach((fascia, valore) -> {
                        kwhMese.put("Materia energia " + fascia.toLowerCase(), valore);
                    });
                });
            }
        }

        return kWhPerMese;
    }

    private String estraiMeseDaPeriodo() {
        // Questo metodo dovrebbe estrarre il mese dal periodo corrente
        // Per ora ritorna un placeholder, ma dovrebbe usare il periodo estratto
        return "giugno"; // TODO: implementare estrazione corretta
    }


    private void printEnergiaMap(String nome, Map<String, Map<String, Double>> map, String unita) {
        if (map != null && !map.isEmpty()) {
            LogCustom.logOk(nome + ":");
            map.forEach((periodo, fasce) -> fasce.forEach((fascia, valore) ->
                    LogCustom.logKV("  " + fascia, valore + " " + unita)
            ));
        }
    }

    private Document convertBytesToDocument(byte[] xmlData) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xmlData));
        } catch (Exception e) {
            throw new RuntimeException("Errore converti bytes in Document XML", e);
        }
    }


    public String extractBollettaNome(Document document) {
        LogCustom.logTitle("extractBollettaNome (Nuovo Formato)");
        NodeList lineNodes = document.getElementsByTagName("Line");

        // Pattern: "Bolletta n. 525509788826"
        Pattern pattern = Pattern.compile("Bolletta n\\. (\\d+)");

        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node lineNode = lineNodes.item(i);
            if (lineNode.getNodeType() == Node.ELEMENT_NODE) {
                String lineText = lineNode.getTextContent();
                Matcher matcher = pattern.matcher(lineText);
                if (matcher.find()) {
                    String nome = matcher.group(1);
                    LogCustom.logOk("Bolletta trovata: n. " + nome);
                    return nome;
                }
            }
        }

        LogCustom.logWarn("Numero bolletta non trovato");
        return null;
    }

    // ============================================================================
    // ESTRAZIONE PERIODO
    // ============================================================================

    public Periodo extractPeriodo(Document document) {
        LogCustom.logTitle("extractPeriodo (nuovo formato) - cerca righe 'dal' e 'al' consecutive");
        NodeList lineNodes = document.getElementsByTagName("Line");

        String dalLine = null;
        String alLine = null;

        for (int i = 0; i < lineNodes.getLength() - 1; i++) {
            String lineCurr = lineNodes.item(i).getTextContent().trim();
            String lineNext = lineNodes.item(i + 1).getTextContent().trim();

            System.out.println("Linea " + i + ": " + lineCurr);
            System.out.println("Linea " + (i + 1) + ": " + lineNext);

            if (lineCurr.toLowerCase().startsWith("dal") && lineNext.toLowerCase().startsWith("al")) {
                dalLine = lineCurr;
                alLine = lineNext;
                break;
            }
        }

        if (dalLine == null || alLine == null) {
            LogCustom.logWarn("Linee con 'dal' e 'al' consecutivi non trovate");
            throw new IllegalStateException("Impossibile estrarre il periodo: date mancanti o non consecutive.");
        }

        Pattern patternDal = Pattern.compile("dal\\s*(\\d{1,2})\\s+(\\p{L}+)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        Pattern patternAl = Pattern.compile("al\\s*(\\d{1,2})\\s+(\\p{L}+)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

        Matcher mDal = patternDal.matcher(dalLine);
        Matcher mAl = patternAl.matcher(alLine);

        if (!mDal.find() || !mAl.find()) {
            LogCustom.logWarn("Formato righe 'dal' o 'al' non riconosciuto");
            throw new IllegalStateException("Impossibile estrarre il periodo: formato date errato.");
        }

        try {
            int dayStart = Integer.parseInt(mDal.group(1));
            String monthStartStr = mDal.group(2);
            int yearStart = Integer.parseInt(mDal.group(3));

            int dayEnd = Integer.parseInt(mAl.group(1));
            String monthEndStr = mAl.group(2);
            int yearEnd = Integer.parseInt(mAl.group(3));

            int monthStart = mapMeseToNumero(monthStartStr);
            int monthEnd = mapMeseToNumero(monthEndStr);

            if (monthStart == 0 || monthEnd == 0) {
                LogCustom.logWarn("Mese non riconosciuto: " + monthStartStr + " o " + monthEndStr);
                throw new IllegalStateException("Mese non riconosciuto nel periodo");
            }

            Calendar calStart = Calendar.getInstance();
            calStart.set(yearStart, monthStart - 1, dayStart);

            Calendar calEnd = Calendar.getInstance();
            calEnd.set(yearEnd, monthEnd - 1, dayEnd);

            Date dataInizio = calStart.getTime();
            Date dataFine = calEnd.getTime();
            String anno = String.valueOf(yearEnd);

            LogCustom.logKV("Data inizio", dataInizio.toString());
            LogCustom.logKV("Data fine", dataFine.toString());
            LogCustom.logKV("Anno", anno);

            return new Periodo(dataInizio, dataFine, anno);

        } catch (Exception e) {
            LogCustom.logWarn("Errore nel parsing del periodo: " + e.getMessage());
            throw new IllegalStateException("Errore nel parsing del periodo", e);
        }
    }



    // ============================================================================
    // ESTRAZIONE POD
    // ============================================================================

    public String extractPod(Document document) {
        LogCustom.logTitle("extractPod (Nuovo Formato) - Cerca riga successiva");
        NodeList lines = document.getElementsByTagName("Line");
        for (int i = 0; i < lines.getLength() - 1; i++) {
            String current = lines.item(i).getTextContent().trim();
            if (current.toLowerCase().contains("pod (punto di prelievo)")) {
                String pod = lines.item(i + 1).getTextContent().trim();
                LogCustom.logOk("POD trovato: " + pod);
                return pod;
            }
        }
        LogCustom.logWarn("POD non trovato");
        return null;
    }

    // ============================================================================
    // ESTRAZIONE INDIRIZZO FORNITURA
    // ============================================================================

    public String extractIndirizzoFornitura(Document document) {
        LogCustom.logTitle("extractIndirizzoFornitura (Nuovo Formato) - Cerca riga successiva");
        NodeList lines = document.getElementsByTagName("Line");
        StringBuilder indirizzoCompleto = new StringBuilder();
        boolean inIndirizzo = false;
        for (int i = 0; i < lines.getLength(); i++) {
            String current = lines.item(i).getTextContent().trim();
            if (inIndirizzo) {
                if (current.isEmpty()) break; // Fine indirizzo, spesso linea vuota
                indirizzoCompleto.append(current).append(" ");
            }
            if (current.toLowerCase().contains("indirizzo di fornitura")) {
                inIndirizzo = true;
            }
        }
        String indirizzo = indirizzoCompleto.toString().trim();
        if (indirizzo.isEmpty()) {
            LogCustom.logWarn("Indirizzo fornitura non trovato");
            return null;
        }
        LogCustom.logOk("Indirizzo trovato: " + indirizzo);
        return indirizzo;
    }

    // ============================================================================
    // ESTRAZIONE POTENZA IMPEGNATA
    // ============================================================================

    public Double extractPotenzaImpegnata(Document document) {
        LogCustom.logTitle("extractPotenzaImpegnata (Nuovo Formato) - Cerca riga successiva");
        NodeList lines = document.getElementsByTagName("Line");
        for (int i = 0; i < lines.getLength() - 1; i++) {
            String current = lines.item(i).getTextContent().trim().toLowerCase();
            if (current.contains("potenza impegnata")) {
                String potenzaStr = lines.item(i + 1).getTextContent().trim().replace(".", "").replace(",", ".");
                // Esempi: "5.000,00 kW" -> "5000.00 kW"
                Pattern p = Pattern.compile("([\\d.]+)\\s*kW");
                Matcher m = p.matcher(potenzaStr);
                if (m.find()) {
                    try {
                        Double potenza = Double.parseDouble(m.group(1));
                        LogCustom.logOk("Potenza impegnata trovata: " + potenza + " kW");
                        return potenza;
                    } catch (NumberFormatException e) {
                        LogCustom.logWarn("Errore parsing potenza: " + e.getMessage());
                        return null;
                    }
                }
            }
        }
        LogCustom.logWarn("Potenza impegnata non trovata");
        return null;
    }

    // ============================================================================
    // ESTRAZIONE CONSUMO TOTALE FATTURATO (dal periodo corrente)
    // ============================================================================

    public Integer extractConsumoTotaleFatturato(Document document) {
        LogCustom.logTitle("extractConsumoTotaleFatturato (Nuovo Formato)");
        NodeList lineNodes = document.getElementsByTagName("Line");

        // Pattern: "Consumo totale fatturato del periodo 225 kWh"
        Pattern pattern = Pattern.compile("Consumo totale fatturato.*?(\\d+)\\s*kWh", Pattern.CASE_INSENSITIVE);

        for (int i = 0; i < lineNodes.getLength(); i++) {
            String text = lineNodes.item(i).getTextContent();
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                Integer consumo = Integer.parseInt(matcher.group(1));
                LogCustom.logOk("Consumo totale fatturato: " + consumo + " kWh");
                return consumo;
            }
        }

        LogCustom.logWarn("Consumo totale fatturato non trovato");
        return null;
    }

    // ============================================================================
    // ESTRAZIONE TOTALE DA PAGARE
    // ============================================================================

    public Double extractTotaleDaPagare(Document document) {
        LogCustom.logTitle("extractTotaleDaPagare (Nuovo Formato)");
        NodeList lineNodes = document.getElementsByTagName("Line");

        // Pattern: "Totale da pagare 46,00 €"
        Pattern pattern = Pattern.compile("Totale da pagare\\s+([\\d,.]+)\\s*€", Pattern.CASE_INSENSITIVE);

        for (int i = 0; i < lineNodes.getLength(); i++) {
            String text = lineNodes.item(i).getTextContent();
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String valueStr = matcher.group(1).replace(".", "").replace(",", ".");
                Double totale = Double.parseDouble(valueStr);
                LogCustom.logOk("Totale da pagare: " + totale + " €");
                return totale;
            }
        }

        LogCustom.logWarn("Totale da pagare non trovato");
        return null;
    }

    // ============================================================================
    // ESTRAZIONE LETTURE (Energia Attiva per fascia)
    // ============================================================================

    public Map<String, Map<String, Map<String, Integer>>> extractLetture(Document document) {
        LogCustom.logTitle("extractLetture (Nuovo Formato)");
        Map<String, Map<String, Map<String, Integer>>> lettureMese = new HashMap<>();

        NodeList lineNodes = document.getElementsByTagName("Line");
        String meseCorrente = null;

        // Pattern per le letture: "31.05.2025 30.06.2025 Fascia oraria F1 375 Rilevata 378 Rilevata 25,000 75 kWh Effettivo"
        Pattern attiva = Pattern.compile(
                "Fascia oraria F([123]).*?(\\d+)\\s+Rilevata\\s+(\\d+)\\s+Rilevata.*?(\\d+)\\s*kWh\\s+Effettivo",
                Pattern.CASE_INSENSITIVE
        );

        Pattern reattiva = Pattern.compile(
                "Fascia oraria F([123]).*?(\\d+)\\s+Rilevata\\s+(\\d+)\\s+Rilevata.*?(\\d+)\\s*kvarh\\s+Effettivo",
                Pattern.CASE_INSENSITIVE
        );

        Pattern potenza = Pattern.compile(
                "Fascia oraria F([123]).*?([\\d,]+)\\s*kW\\s+Effettivo",
                Pattern.CASE_INSENSITIVE
        );

        for (int i = 0; i < lineNodes.getLength(); i++) {
            String line = lineNodes.item(i).getTextContent();

            // Estrai mese dalla data se presente
            ArrayList<Date> dates = extractDates(line);
            if (dates.size() >= 2) {
                meseCorrente = DateUtils.getMonthFromDateLocalized(dates.get(1));
            }

            if (meseCorrente == null) {
                meseCorrente = "MeseSconosciuto";
            }

            // ENERGIA ATTIVA
            Matcher mAttiva = attiva.matcher(line);
            if (mAttiva.find()) {
                String fascia = "F" + mAttiva.group(1);
                Integer consumo = Integer.parseInt(mAttiva.group(4));

                lettureMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .computeIfAbsent("Energia Attiva", k -> new HashMap<>())
                        .put(fascia, consumo);

                LogCustom.logKV(meseCorrente + " - Energia Attiva " + fascia, consumo + " kWh");
            }

            // ENERGIA REATTIVA
            Matcher mReattiva = reattiva.matcher(line);
            if (mReattiva.find()) {
                String fascia = "F" + mReattiva.group(1);
                Integer consumo = Integer.parseInt(mReattiva.group(4));

                lettureMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .computeIfAbsent("Energia Reattiva", k -> new HashMap<>())
                        .put(fascia, consumo);

                LogCustom.logKV(meseCorrente + " - Energia Reattiva " + fascia, consumo + " kvarh");
            }

            // POTENZA
            Matcher mPotenza = potenza.matcher(line);
            if (mPotenza.find()) {
                String fascia = "F" + mPotenza.group(1);
                String valueStr = mPotenza.group(2).replace(",", ".");
                Integer potenzaInt = (int) (Double.parseDouble(valueStr) * 1000); // conversione a W

                lettureMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .computeIfAbsent("Potenza", k -> new HashMap<>())
                        .put(fascia, potenzaInt);

                LogCustom.logKV(meseCorrente + " - Potenza " + fascia, valueStr + " kW");
            }
        }

        LogCustom.logLetture("Letture estratte (nuovo formato)", lettureMese);
        return lettureMese;
    }

    // ============================================================================
    // ESTRAZIONE SPESE DALLO "SCONTRINO DELL'ENERGIA"
    // ============================================================================

    public Map<String, Map<String, Double>> extractSpesePerMese(Document document) {
        LogCustom.logTitle("extractSpesePerMese (Nuovo Formato - Scontrino Energia)");
        Map<String, Map<String, Double>> spesePerMese = new HashMap<>();

        NodeList lineNodes = document.getElementsByTagName("Line");
        String meseCorrente = "MeseSconosciuto";

        // Estrai mese dal periodo
        try {
            Periodo periodo = extractPeriodo(document);
            Calendar cal = Calendar.getInstance();
            cal.setTime(periodo.getDataFine());
            meseCorrente = DateUtils.getMonthFromDateLocalized(periodo.getDataFine());
        } catch (Exception e) {
            LogCustom.logWarn("Impossibile estrarre mese da periodo");
        }

        boolean inScontrino = false;

        for (int i = 0; i < lineNodes.getLength(); i++) {
            String line = lineNodes.item(i).getTextContent();
            String lowerLine = line.toLowerCase();

            // Inizia lo scontrino
            if (lowerLine.contains("scontrino dell'energia") ||
                    lowerLine.contains("scontrino energia")) {
                inScontrino = true;
                LogCustom.logOk("Trovato Scontrino dell'Energia");
                continue;
            }

            // Fine scontrino
            if (inScontrino && (lowerLine.contains("totale da pagare") ||
                    lowerLine.contains("box dell'offerta"))) {
                inScontrino = false;
            }

            if (!inScontrino) continue;

            // QUOTA PER CONSUMI
            Pattern quotaConsumi = Pattern.compile(
                    "Quota per consumi.*?([\\d,.]+)\\s*€",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher mConsumi = quotaConsumi.matcher(line);
            if (mConsumi.find()) {
                Double valore = parseEuro(mConsumi.group(1));
                spesePerMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .put("Quota Consumi", valore);
                LogCustom.logKV("Quota Consumi", valore + " €");
            }

            // SPESA PER LA VENDITA DI ENERGIA ELETTRICA
            Pattern vendita = Pattern.compile(
                    "spesa per la vendita di energia elettrica.*?([\\d,.]+)\\s*€/kWh\\s+([\\d,.]+)\\s*€",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher mVendita = vendita.matcher(line);
            if (mVendita.find()) {
                Double valore = parseEuro(mVendita.group(2));
                spesePerMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .put("Vendita Energia", valore);
                LogCustom.logKV("Vendita Energia", valore + " €");
            }

            // SPESA PER LA RETE E GLI ONERI GENERALI
            Pattern rete = Pattern.compile(
                    "spesa per la rete e gli oneri generali.*?([\\d,.]+)\\s*€/kWh\\s+([\\d,.]+)\\s*€",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher mRete = rete.matcher(line);
            if (mRete.find()) {
                Double valore = parseEuro(mRete.group(2));
                spesePerMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .put("Rete e Oneri", valore);
                LogCustom.logKV("Rete e Oneri", valore + " €");
            }

            // ENERGIA REATTIVA
            Pattern reattiva = Pattern.compile(
                    "energia reattiva prelevata.*?([\\d,.]+)\\s*€/kvarh\\s+([\\d,.]+)\\s*€",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher mReattiva = reattiva.matcher(line);
            if (mReattiva.find()) {
                Double valore = parseEuro(mReattiva.group(2));
                spesePerMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .put("Energia Reattiva", valore);
                LogCustom.logKV("Energia Reattiva", valore + " €");
            }

            // QUOTA FISSA
            Pattern quotaFissa = Pattern.compile(
                    "Quota fissa.*?([\\d,.]+)\\s*€/mese\\s+([\\d,.]+)\\s*€",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher mFissa = quotaFissa.matcher(line);
            if (mFissa.find()) {
                Double valore = parseEuro(mFissa.group(2));
                spesePerMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .put("Quota Fissa", valore);
                LogCustom.logKV("Quota Fissa", valore + " €");
            }

            // QUOTA POTENZA
            Pattern quotaPotenza = Pattern.compile(
                    "Quota Potenza.*?([\\d,.]+)\\s*kW per.*?([\\d,.]+)\\s*€/kW/mese\\s+([\\d,.]+)\\s*€",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher mPotenza = quotaPotenza.matcher(line);
            if (mPotenza.find()) {
                Double valore = parseEuro(mPotenza.group(3));
                spesePerMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .put("Quota Potenza", valore);
                LogCustom.logKV("Quota Potenza", valore + " €");
            }

            // ACCISE E IVA
            Pattern accise = Pattern.compile(
                    "Accise e IVA\\s+([\\d,.]+)\\s*€",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher mAccise = accise.matcher(line);
            if (mAccise.find()) {
                Double valore = parseEuro(mAccise.group(1));
                spesePerMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .put("Accise e IVA", valore);
                LogCustom.logKV("Accise e IVA", valore + " €");
            }

            // TOTALE BOLLETTA
            Pattern totale = Pattern.compile(
                    "Totale bolletta\\s+([\\d,.]+)\\s*€",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher mTotale = totale.matcher(line);
            if (mTotale.find()) {
                Double valore = parseEuro(mTotale.group(1));
                spesePerMese
                        .computeIfAbsent(meseCorrente, k -> new HashMap<>())
                        .put("Totale Bolletta", valore);
                LogCustom.logKV("Totale Bolletta", valore + " €");
            }
        }

        LogCustom.logNestedDoubles("Spese estratte (nuovo formato)", spesePerMese);
        return spesePerMese;
    }

    // ============================================================================
    // ESTRAZIONE DETTAGLI COMPONENTI (dalla sezione dettagliata)
    // ============================================================================

    public Map<String, Map<String, Double>> extractDettaglioComponenti(Document document) {
        LogCustom.logTitle("extractDettaglioComponenti (Nuovo Formato)");
        Map<String, Map<String, Double>> componenti = new HashMap<>();

        NodeList lineNodes = document.getElementsByTagName("Line");
        String meseCorrente = "MeseSconosciuto";
        String sezioneCorrente = null;

        // Estrai mese
        try {
            Periodo periodo = extractPeriodo(document);
            meseCorrente = DateUtils.getMonthFromDateLocalized(periodo.getDataFine());
        } catch (Exception e) {
            LogCustom.logWarn("Impossibile estrarre mese");
        }

        for (int i = 0; i < lineNodes.getLength(); i++) {
            String line = lineNodes.item(i).getTextContent();
            String lowerLine = line.toLowerCase();

            // Riconosci sezioni
            if (lowerLine.contains("spesa per la vendita di energia elettrica")) {
                sezioneCorrente = "Vendita Energia";
                extractEuroFromLineAndAdd(line, componenti, meseCorrente, "TOTALE_Vendita");
                continue;
            }

            if (lowerLine.contains("spesa per la tariffa per l'uso della rete")) {
                sezioneCorrente = "Rete";
                extractEuroFromLineAndAdd(line, componenti, meseCorrente, "TOTALE_Rete");
                continue;
            }

            if (lowerLine.contains("spesa per oneri di sistema")) {
                sezioneCorrente = "Oneri";
                extractEuroFromLineAndAdd(line, componenti, meseCorrente, "TOTALE_Oneri");
                continue;
            }

            if (lowerLine.contains("totale imposte")) {
                sezioneCorrente = "Imposte";
                extractEuroFromLineAndAdd(line, componenti, meseCorrente, "TOTALE_Imposte");
                continue;
            }

            // Estrai componenti ASOS e ARIM
            if (sezioneCorrente != null && sezioneCorrente.equals("Oneri")) {
                if (lowerLine.contains("componente asos")) {
                    extractEuroFromLineAndAdd(line, componenti, meseCorrente, "ASOS");
                }
                if (lowerLine.contains("componente arim")) {
                    extractEuroFromLineAndAdd(line, componenti, meseCorrente, "ARIM");
                }
            }

            // Penalità energia reattiva
            if (lowerLine.contains("penalità energia reattiva")) {
                String fascia = findFascia(line);
                if (fascia != null) {
                    extractEuroFromLineAndAdd(line, componenti, meseCorrente, "Penalità " + fascia);
                }
            }
        }

        LogCustom.logNestedDoubles("Dettaglio componenti (nuovo formato)", componenti);
        return componenti;
    }

    public Map<String, Map<String, Double>> extractSpeseDettagliate(Document document) {
        LogCustom.logTitle("extractSpeseDettagliate (Nuovo Formato)");

        Map<String, Map<String, Double>> spese = new HashMap<>();
        NodeList lines = document.getElementsByTagName("Line");

        String sezioneCorrente = null;      // es. "Vendita Energia", "Uso Rete", "Oneri di Sistema", "Imposte"
        String sottosezioneCorrente = null; // es. "QUOTA FISSA", "QUOTA ENERGIA", "QUOTA POTENZA", "QUOTA VARIABILE"
        String vocePrecedente = null;       // Descrizione della voce (es. "Componente ASOS...")

        // Pattern per righe con date e importo finale: "dal 01.06.2025 al 30.06.2025 ... € XX,XX"
        Pattern rigaDatiPattern = Pattern.compile("dal\\s+\\d{2}\\.\\d{2}\\.\\d{4}\\s+al\\s+\\d{2}\\.\\d{2}\\.\\d{4}.*€\\s*([\\d\\.]+,[\\d]{2})");

        for (int i = 0; i < lines.getLength(); i++) {
            String line = lines.item(i).getTextContent().trim();
            String lineUpper = line.toUpperCase();

            // Identifica sezione principale
            if (lineUpper.contains("SPESA PER LA VENDITA DI ENERGIA ELETTRICA")) {
                sezioneCorrente = "Vendita Energia";
                spese.putIfAbsent(sezioneCorrente, new HashMap<>());
                LogCustom.logOk("Sezione: " + sezioneCorrente);
                continue;
            }
            if (lineUpper.contains("SPESA PER LA TARIFFA PER L'USO DELLA RETE ELETTRICA")) {
                sezioneCorrente = "Uso Rete";
                spese.putIfAbsent(sezioneCorrente, new HashMap<>());
                LogCustom.logOk("Sezione: " + sezioneCorrente);
                continue;
            }
            if (lineUpper.contains("SPESA PER ONERI DI SISTEMA")) {
                sezioneCorrente = "Oneri di Sistema";
                spese.putIfAbsent(sezioneCorrente, new HashMap<>());
                LogCustom.logOk("Sezione: " + sezioneCorrente);
                continue;
            }
            if (lineUpper.contains("TOTALE IMPOSTE")) {
                sezioneCorrente = "Imposte";
                spese.putIfAbsent(sezioneCorrente, new HashMap<>());
                LogCustom.logOk("Sezione: " + sezioneCorrente);
                continue;
            }

            // Identifica sottosezione
            if (lineUpper.equals("QUOTA FISSA")) {
                sottosezioneCorrente = "QUOTA FISSA";
                vocePrecedente = null;
                System.out.println("Sottosezione: " + sottosezioneCorrente);
                continue;
            }
            if (lineUpper.equals("QUOTA POTENZA")) {
                sottosezioneCorrente = "QUOTA POTENZA";
                vocePrecedente = null;
                System.out.println("Sottosezione: " + sottosezioneCorrente);
                continue;
            }
            if (lineUpper.equals("QUOTA VARIABILE")) {
                sottosezioneCorrente = "QUOTA VARIABILE";
                vocePrecedente = null;
                System.out.println("Sottosezione: " + sottosezioneCorrente);
                continue;
            }
            if (lineUpper.equals("QUOTA ENERGIA")) {
                sottosezioneCorrente = "QUOTA ENERGIA";
                vocePrecedente = null;
                System.out.println("Sottosezione: " + sottosezioneCorrente);
                continue;
            }

            // Se siamo in una sezione valida
            if (sezioneCorrente != null) {
                // Verifica se la riga contiene i dati (dal ... al ... € importo)
                Matcher matcher = rigaDatiPattern.matcher(line);

                if (matcher.find()) {
                    // Estrai importo
                    String valoreStr = matcher.group(1).replace(".", "").replace(",", ".");
                    try {
                        Double valore = Double.parseDouble(valoreStr);

                        // Costruisci chiave
                        String chiave = costruisciChiave(sottosezioneCorrente, vocePrecedente);

                        spese.get(sezioneCorrente).put(chiave, valore);
                        LogCustom.logKV("Estratto: " + chiave, valore + " €");

                        // Reset voce precedente dopo estrazione
                        vocePrecedente = null;

                    } catch (NumberFormatException e) {
                        LogCustom.logWarn("Errore parsing valore: " + valoreStr);
                    }
                } else {
                    // Questa riga potrebbe essere una descrizione di voce
                    // (es. "Componente ASOS a copertura dei costi...")
                    // La memorizziamo per associarla al prossimo dato estratto
                    if (!line.isEmpty() &&
                            !lineUpper.startsWith("UNITÀ DI MISURA") &&
                            !lineUpper.contains("PREZZI UNITARI") &&
                            !lineUpper.contains("QUANTITÀ") &&
                            !lineUpper.contains("IMPORTO TOTALE")) {
                        vocePrecedente = line;
                        System.out.println("Voce descrittiva: " + vocePrecedente);
                    }
                }
            }
        }

        LogCustom.logNestedDoubles("Spese dettagliate estratte", spese);
        return spese;
    }

    // Costruisce chiave combinando sottosezione e voce
    private String costruisciChiave(String sottosezione, String voce) {
        if (sottosezione == null) {
            return voce != null ? voce : "Generico";
        }

        if (voce == null || voce.isEmpty()) {
            return sottosezione;
        }

        // Se la voce contiene componente ASOS/ARIM estraiamo solo la parte significativa
        if (voce.toLowerCase().contains("componente asos")) {
            return sottosezione + " - ASOS";
        }
        if (voce.toLowerCase().contains("componente arim")) {
            return sottosezione + " - ARIM";
        }
        if (voce.toLowerCase().contains("materia energia f1")) {
            return sottosezione + " - Materia Energia F1";
        }
        if (voce.toLowerCase().contains("materia energia f2")) {
            return sottosezione + " - Materia Energia F2";
        }
        if (voce.toLowerCase().contains("materia energia f3")) {
            return sottosezione + " - Materia Energia F3";
        }
        if (voce.toLowerCase().contains("perdite di rete f1")) {
            return sottosezione + " - Perdite Rete F1";
        }
        if (voce.toLowerCase().contains("perdite di rete f2")) {
            return sottosezione + " - Perdite Rete F2";
        }
        if (voce.toLowerCase().contains("perdite di rete f3")) {
            return sottosezione + " - Perdite Rete F3";
        }
        if (voce.toLowerCase().contains("corrispettivi di dispacciamento")) {
            return sottosezione + " - Dispacciamento";
        }
        if (voce.toLowerCase().contains("corrispettivo mercato capacità")) {
            return sottosezione + " - Mercato Capacità";
        }
        if (voce.toLowerCase().contains("penalità energia reattiva")) {
            return sottosezione + " - Penalità Reattiva";
        }
        if (voce.toLowerCase().contains("imposta erariale")) {
            return sottosezione + " - Imposta Erariale";
        }

        // Default: ritorna sottosezione + prime 30 caratteri della voce
        String voceBreve = voce.length() > 30 ? voce.substring(0, 30) + "..." : voce;
        return sottosezione + " - " + voceBreve;
    }


    // ============================================================================
    // METODI UTILITY
    // ============================================================================

    private void extractEuroFromLineAndAdd(String line, Map<String, Map<String, Double>> map,
                                           String mese, String chiave) {
        Double valore = extractEuroValue(line);
        if (valore != null) {
            map.computeIfAbsent(mese, k -> new HashMap<>())
                    .merge(chiave, valore, Double::sum);
            LogCustom.logKV(chiave, valore + " €");
        }
    }

    private static Double extractEuroValue(String lineText) {
        try {
            // Pattern: "€ 35,14" o "35,14 €"
            Pattern pattern = Pattern.compile("€?\\s*([\\d.]+,[\\d]{2})\\s*€?");
            Matcher matcher = pattern.matcher(lineText);

            Double lastValue = null;
            while (matcher.find()) {
                String valueString = matcher.group(1);
                valueString = valueString.replace(".", "").replace(",", ".");
                lastValue = Double.parseDouble(valueString);
            }

            return lastValue;
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseEuro(String str) {
        if (str == null) return null;
        try {
            return Double.parseDouble(str.replace(".", "").replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private static int mapMeseToNumero(String mese) {
        Map<String, Integer> mesi = new HashMap<>();
        mesi.put("gennaio", 1); mesi.put("febbraio", 2); mesi.put("marzo", 3);
        mesi.put("aprile", 4); mesi.put("maggio", 5); mesi.put("giugno", 6);
        mesi.put("luglio", 7); mesi.put("agosto", 7); mesi.put("settembre", 9);
        mesi.put("ottobre", 10); mesi.put("novembre", 11); mesi.put("dicembre", 12);

        // Uppercase variants
        String lower = mese.toLowerCase();
        return mesi.getOrDefault(lower, 0);
    }

    private static String findFascia(String s) {
        String t = s.toLowerCase();
        if (t.contains("f1") || t.contains(" f 1")) return "F1";
        if (t.contains("f2") || t.contains(" f 2")) return "F2";
        if (t.contains("f3") || t.contains(" f 3")) return "F3";
        return null;
    }

    private static ArrayList<Date> extractDates(String lineText) {
        ArrayList<Date> dates = new ArrayList<>();
        Pattern datePattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
        Matcher matcher = datePattern.matcher(lineText);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        while (matcher.find()) {
            String dateString = matcher.group();
            try {
                Date date = dateFormat.parse(dateString);
                dates.add(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return dates;
    }

    // ============================================================================
// ESTRAZIONE DATI LETTURE E CONSUMI (sezione dettagliata con matricola)
// ============================================================================

    public Map<String, Object> extractDatiLettureConsumi(Document document) {
        LogCustom.logTitle("extractDatiLettureConsumi (Nuovo Formato)");

        Map<String, Object> datiLetture = new HashMap<>();
        NodeList lines = document.getElementsByTagName("Line");

        String matricolaContatore = null;
        String tipoEnergiaCorrente = null;
        boolean aspettaCapacitivaOInduttiva = false; // Flag per gestire righe successive

        // Pattern per matricola contatore
        Pattern matricolaPattern = Pattern.compile("Contatore matricola:\\s*([\\d]+)");

        // Pattern per righe con periodo e consumi
        Pattern energiaAttivaPattern = Pattern.compile(
                "(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}\\.\\d{2}\\.\\d{4})\\s+Fascia oraria (F[123])\\s+([\\d,.]+)\\s*kWh\\s+Effettivo"
        );

        Pattern energiaReativaPattern = Pattern.compile(
                "(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}\\.\\d{2}\\.\\d{4})\\s+Fascia oraria (F[123])\\s+([\\d,.]+)\\s*kvarh\\s+Effettivo"
        );

        Pattern potenzaPattern = Pattern.compile(
                "(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}\\.\\d{2}\\.\\d{4})\\s+Fascia oraria (F[123])\\s+([\\d,.]+)\\s*kW\\s+Effettivo"
        );

        Map<String, Map<String, Double>> energiaAttiva = new HashMap<>();
        Map<String, Map<String, Double>> energiaReattiva = new HashMap<>();
        Map<String, Map<String, Double>> potenza = new HashMap<>();
        Map<String, Map<String, Double>> energiaReativaCapacitiva = new HashMap<>();
        Map<String, Map<String, Double>> energiaReativaInduttiva = new HashMap<>();

        for (int i = 0; i < lines.getLength(); i++) {
            String line = lines.item(i).getTextContent().trim();
            String lineUpper = line.toUpperCase();

            // Estrai matricola contatore
            Matcher mMatricola = matricolaPattern.matcher(line);
            if (mMatricola.find()) {
                matricolaContatore = mMatricola.group(1);
                LogCustom.logKV("Matricola Contatore", matricolaContatore);
                continue;
            }

            // Identifica tipo di energia - ORDINE E GESTIONE MULTILINEA

            // Gestione ENERGIA ATTIVA
            if (lineUpper.contains("ENERGIA ATTIVA")) {
                tipoEnergiaCorrente = "ENERGIA_ATTIVA";
                aspettaCapacitivaOInduttiva = false;
                System.out.println("Tipo energia: ENERGIA ATTIVA");
                continue;
            }

            // Gestione POTENZA
            if (lineUpper.contains("POTENZA") && !lineUpper.contains("QUOTA") && !lineUpper.contains("IMPEGNATA")) {
                tipoEnergiaCorrente = "POTENZA";
                aspettaCapacitivaOInduttiva = false;
                System.out.println("Tipo energia: POTENZA");
                continue;
            }

            // Gestione ENERGIA REATTIVA (potrebbe essere seguita da CAPACITIVA/INDUTTIVA)
            if (lineUpper.contains("ENERGIA REATTIVA") &&
                    !lineUpper.contains("CAPACITIVA") &&
                    !lineUpper.contains("INDUTTIVA") &&
                    !lineUpper.contains("IMMESSA")) {

                // Potrebbe essere ENERGIA REATTIVA semplice O l'inizio di CAPACITIVA/INDUTTIVA
                // Guarda le prossime 2 righe per determinare
                boolean isCapacitivaOInduttiva = false;

                if (i + 1 < lines.getLength()) {
                    String nextLine = lines.item(i + 1).getTextContent().trim().toUpperCase();
                    if (nextLine.contains("CAPACITIVA") || nextLine.contains("INDUTTIVA")) {
                        isCapacitivaOInduttiva = true;
                        aspettaCapacitivaOInduttiva = true;
                        tipoEnergiaCorrente = "ENERGIA_REATTIVA_TEMP"; // Temporaneo
                    }
                }

                if (!isCapacitivaOInduttiva) {
                    tipoEnergiaCorrente = "ENERGIA_REATTIVA";
                    aspettaCapacitivaOInduttiva = false;
                    System.out.println("Tipo energia: ENERGIA REATTIVA");
                }
                continue;
            }

            // Se siamo in attesa di CAPACITIVA o INDUTTIVA
            if (aspettaCapacitivaOInduttiva) {
                if (lineUpper.contains("CAPACITIVA")) {
                    tipoEnergiaCorrente = "ENERGIA_REATTIVA_CAPACITIVA_TEMP";
                    System.out.println("Tipo energia: ENERGIA REATTIVA CAPACITIVA (parte 1)");
                    continue;
                }
                if (lineUpper.contains("INDUTTIVA")) {
                    tipoEnergiaCorrente = "ENERGIA_REATTIVA_INDUTTIVA_TEMP";
                    System.out.println("Tipo energia: ENERGIA REATTIVA INDUTTIVA (parte 1)");
                    continue;
                }
                if (lineUpper.contains("IMMESSA")) {
                    if (tipoEnergiaCorrente != null && tipoEnergiaCorrente.equals("ENERGIA_REATTIVA_CAPACITIVA_TEMP")) {
                        tipoEnergiaCorrente = "ENERGIA_REATTIVA_CAPACITIVA";
                        aspettaCapacitivaOInduttiva = false;
                        System.out.println("Tipo energia: ENERGIA REATTIVA CAPACITIVA IMMESSA (completo)");
                    } else if (tipoEnergiaCorrente != null && tipoEnergiaCorrente.equals("ENERGIA_REATTIVA_INDUTTIVA_TEMP")) {
                        tipoEnergiaCorrente = "ENERGIA_REATTIVA_INDUTTIVA";
                        aspettaCapacitivaOInduttiva = false;
                        System.out.println("Tipo energia: ENERGIA REATTIVA INDUTTIVA IMMESSA (completo)");
                    }
                    continue;
                }
            }

            // Estrai dati in base al tipo corrente
            if (tipoEnergiaCorrente != null && !tipoEnergiaCorrente.contains("TEMP")) {
                try {
                    switch (tipoEnergiaCorrente) {
                        case "ENERGIA_ATTIVA":
                            Matcher mAttiva = energiaAttivaPattern.matcher(line);
                            if (mAttiva.find()) {
                                String fascia = mAttiva.group(3);
                                String consumoStr = mAttiva.group(4).replace(".", "").replace(",", ".");
                                Double consumo = Double.parseDouble(consumoStr);
                                energiaAttiva.computeIfAbsent("Periodo", k -> new HashMap<>()).put(fascia, consumo);
                                LogCustom.logKV("Energia Attiva " + fascia, consumo + " kWh");
                            }
                            break;

                        case "ENERGIA_REATTIVA":
                            Matcher mReattiva = energiaReativaPattern.matcher(line);
                            if (mReattiva.find()) {
                                String fascia = mReattiva.group(3);
                                String consumoStr = mReattiva.group(4).replace(".", "").replace(",", ".");
                                Double consumo = Double.parseDouble(consumoStr);
                                energiaReattiva.computeIfAbsent("Periodo", k -> new HashMap<>()).put(fascia, consumo);
                                LogCustom.logKV("Energia Reattiva " + fascia, consumo + " kvarh");
                            }
                            break;

                        case "POTENZA":
                            Matcher mPotenza = potenzaPattern.matcher(line);
                            if (mPotenza.find()) {
                                String fascia = mPotenza.group(3);
                                String consumoStr = mPotenza.group(4).replace(".", "").replace(",", ".");
                                Double pot = Double.parseDouble(consumoStr);
                                potenza.computeIfAbsent("Periodo", k -> new HashMap<>()).put(fascia, pot);
                                LogCustom.logKV("Potenza " + fascia, pot + " kW");
                            }
                            break;

                        case "ENERGIA_REATTIVA_CAPACITIVA":
                            Matcher mCapacitiva = energiaReativaPattern.matcher(line);
                            if (mCapacitiva.find()) {
                                String fascia = mCapacitiva.group(3);
                                String consumoStr = mCapacitiva.group(4).replace(".", "").replace(",", ".");
                                Double consumo = Double.parseDouble(consumoStr);
                                energiaReativaCapacitiva.computeIfAbsent("Periodo", k -> new HashMap<>()).put(fascia, consumo);
                                LogCustom.logKV("Energia Reattiva Capacitiva " + fascia, consumo + " kvarh");
                            }
                            break;

                        case "ENERGIA_REATTIVA_INDUTTIVA":
                            Matcher mInduttiva = energiaReativaPattern.matcher(line);
                            if (mInduttiva.find()) {
                                String fascia = mInduttiva.group(3);
                                String consumoStr = mInduttiva.group(4).replace(".", "").replace(",", ".");
                                Double consumo = Double.parseDouble(consumoStr);
                                energiaReativaInduttiva.computeIfAbsent("Periodo", k -> new HashMap<>()).put(fascia, consumo);
                                LogCustom.logKV("Energia Reattiva Induttiva " + fascia, consumo + " kvarh");
                            }
                            break;
                    }
                } catch (Exception e) {
                    LogCustom.logWarn("Errore parsing riga: " + line + " - " + e.getMessage());
                }
            }
        }

        // Assembla risultato
        datiLetture.put("matricolaContatore", matricolaContatore);
        datiLetture.put("energiaAttiva", energiaAttiva);
        datiLetture.put("energiaReattiva", energiaReattiva);
        datiLetture.put("potenza", potenza);
        datiLetture.put("energiaReativaCapacitiva", energiaReativaCapacitiva);
        datiLetture.put("energiaReativaInduttiva", energiaReativaInduttiva);

        LogCustom.logOk("Dati letture e consumi estratti completamente");
        return datiLetture;
    }

}
