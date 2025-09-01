package miesgroup.mies.webdev.Service.file;

import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.Periodo;
import miesgroup.mies.webdev.Service.DateUtils;
import miesgroup.mies.webdev.Service.LogCustom;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class Lettura {

    public String extractBollettaNome(Document document) {
        NodeList lineNodes = document.getElementsByTagName("Line");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node lineNode = lineNodes.item(i);
            if (lineNode.getNodeType() == Node.ELEMENT_NODE) {
                String lineText = lineNode.getTextContent();
                if (lineText.contains("Bolletta n")) {
                    String nome = extractBollettaNumero(lineText);
                    if (nome != null) {
                        LogCustom.logOk("Bolletta trovata: n. " + nome);
                    }
                    return nome;
                }
            }
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
        LogCustom.logKV("Date trovate nella riga", dates.size());
        return dates;
    }

    public static Double extractValueFromLine(String lineText) {
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

    public Map<String, Map<String, Double>> extractSpesePerMese(Document document, Map<String, Map<String, Map<String, Integer>>> lettureMese) {
        LogCustom.logTitle("extractSpesePerMese");
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

            // ──────────────────────────────────────────────────────────────
            // SEZIONE: SPESA PER LA MATERIA ENERGIA (TOTALE HEADER)
            // ──────────────────────────────────────────────────────────────
            if (lineText.contains("SPESA PER LA MATERIA ENERGIA")) {
                categoriaCorrente = "Materia Energia";
                categorieGiaViste.add(categoriaCorrente);
                System.out.println("🏷️ SEZIONE: " + categoriaCorrente);

                Double valore = null;
                if (lineText.contains("€")) {
                    valore = extractEuroValue(lineText);
                    System.out.println("🔍 Materia - Tentativo 1 (stessa riga): " + (valore != null ? "✅ " + valore : "❌"));
                }
                if (valore == null && i + 1 < lineNodes.getLength()) {
                    String next1 = lineNodes.item(i + 1).getTextContent().trim();
                    if (next1.contains("€")) {
                        valore = extractEuroValue(next1);
                        System.out.println("🔍 Materia - Tentativo 2 (riga +1): " + (valore != null ? "✅ " + valore : "❌"));
                    }
                }
                if (valore == null && i + 2 < lineNodes.getLength()) {
                    String next2 = lineNodes.item(i + 2).getTextContent().trim();
                    if (next2.contains("€")) {
                        valore = extractEuroValue(next2);
                        System.out.println("🔍 Materia - Tentativo 3 (riga +2): " + (valore != null ? "✅ " + valore : "❌"));
                    }
                }

                if (valore != null) {
                    spesePerMese
                            .computeIfAbsent(meseLetto, k -> new HashMap<>())
                            .computeIfAbsent("MATERIA_TOTALE", k -> new ArrayList<>())
                            .add(valore);
                    System.out.println("✅ TOTALE MATERIA ENERGIA estratto: " + valore + " (mese: " + meseCorrente + ")");
                } else {
                    System.out.println("❌ ERRORE: impossibile estrarre TOTALE MATERIA dalla/e riga/he di intestazione.");
                }

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

            } else if (lineText.contains("SPESA PER ONERI DI SISTEMA")) {
                categoriaCorrente = "Oneri di Sistema";
                categorieGiaViste.add(categoriaCorrente);
                System.out.println("🏷️ SEZIONE: " + categoriaCorrente);

                if (lineText.contains("€")) {
                    Double valore = extractEuroValue(lineText);
                    if (valore != null) {
                        spesePerMese
                                .computeIfAbsent(meseLetto, k -> new HashMap<>())
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

                Double valore = null;
                if (lineText.contains("€")) {
                    valore = extractEuroValue(lineText);
                    System.out.println("🔍 Tentativo 1 - Stessa riga: " + (valore != null ? "✅ " + valore : "❌"));
                }
                if (valore == null && i + 1 < lineNodes.getLength()) {
                    String nextLine = lineNodes.item(i + 1).getTextContent().trim();
                    System.out.println("🔍 Controllo riga successiva: " + nextLine);
                    if (nextLine.contains("€")) {
                        valore = extractEuroValue(nextLine);
                        System.out.println("🔍 Tentativo 2 - Riga successiva: " + (valore != null ? "✅ " + valore : "❌"));
                    }
                }
                if (valore == null) {
                    if (lineText.matches(".*[Tt]rasporto.*[Cc]ontatore.*€.*") ||
                            lineText.matches(".*TRASPORTO.*CONTATORE.*€.*") ||
                            lineText.matches(".*[Cc]ontatore.*€.*")) {
                        valore = extractEuroValue(lineText);
                        System.out.println("🔍 Tentativo 3 - Pattern specifico: " + (valore != null ? "✅ " + valore : "❌"));
                    }
                }

                if (valore != null) {
                    spesePerMese
                            .computeIfAbsent(meseLetto, k -> new HashMap<>())
                            .computeIfAbsent("TRASPORTI_TOTALE", k -> new ArrayList<>())
                            .add(valore);
                    System.out.println("✅ TOTALE TRASPORTI estratto: " + valore + " per mese: " + meseCorrente);
                } else {
                    System.out.println("❌ ERRORE estrazione totale trasporti da: " + lineText);
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

            // ──────────────────────────────────────────────────────────────
            // PENALITÀ ENERGIA REATTIVA (F1/F2/F3 oltre 33% o 75%)
            // Esempi:
            // "Penalità energia reattiva F1 oltre il 33% dell'energia attiva"
            // "Penalità energia reattiva F2 oltre il 75% dell'energia attiva"
            // ──────────────────────────────────────────────────────────────
            if (lowerLine.contains("penalità energia reattiva")) {
                String fascia = findFascia(lineText);
                if (fascia == null && i + 1 < lineNodes.getLength()) fascia = findFascia(lineNodes.item(i + 1).getTextContent());
                if (fascia == null && i + 2 < lineNodes.getLength()) fascia = findFascia(lineNodes.item(i + 2).getTextContent());

                Integer soglia = findSoglia(lineText);
                if (soglia == null && i + 1 < lineNodes.getLength()) soglia = findSoglia(lineNodes.item(i + 1).getTextContent());
                if (soglia == null && i + 2 < lineNodes.getLength()) soglia = findSoglia(lineNodes.item(i + 2).getTextContent());

                Double importo = extractEuroValueSafe(lineText);
                if (importo == null && i + 1 < lineNodes.getLength()) importo = extractEuroValueSafe(lineNodes.item(i + 1).getTextContent());
                if (importo == null && i + 2 < lineNodes.getLength()) importo = extractEuroValueSafe(lineNodes.item(i + 2).getTextContent());
                if (importo == null && i + 3 < lineNodes.getLength()) importo = extractEuroValueSafe(lineNodes.item(i + 3).getTextContent());

                if (fascia != null && soglia != null && importo != null) {
                    String key = fascia + "Penale" + soglia; // es. F1Penale33
                    spesePerMese
                            .computeIfAbsent(meseLetto, k -> new HashMap<>())
                            .computeIfAbsent(key, k -> new ArrayList<>())
                            .add(importo);

                    System.out.println("✅ " + key + " = " + importo + " € (mese: " + (meseCorrente != null ? meseCorrente : meseLetto) + ")");
                    // evitiamo il ramo generico "penalità"
                    continue;
                } else {
                    System.out.println("⚠️ Penalità reattiva rilevata ma incompleta — fascia=" + fascia + ", soglia=" + soglia + ", importo=" + importo);
                }
            }

            // Ramo generico "penalità" (mantieni per altre penalità non mappate)
            else if (lowerLine.contains("penalit")) {
                categoriaCorrente = "Altro";
                categorieGiaViste.add(categoriaCorrente);
                System.out.println("🏷️ SEZIONE: " + categoriaCorrente);

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

            // Fallback totale TRASPORTI fuori intestazione
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

            // Gestione specifica ONERI
            if (sezioneOneri) {
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

                if (lowerLine.contains("componente asos") ||
                        lowerLine.contains("sostegno delle fonti rinnovabili")) {
                    tipoComponente = "ASOS";
                    if (!tipoQuota.isEmpty()) {
                        sottoCategoria = "ASOS_" + tipoQuota;
                        System.out.println("🎯 ONERI - Sottocategoria: " + sottoCategoria);
                    }
                } else if (lowerLine.contains("componente arim") ||
                        lowerLine.contains("altri oneri relativi ad attività")) {
                    tipoComponente = "ARIM";
                    if (!tipoQuota.isEmpty()) {
                        sottoCategoria = "ARIM_" + tipoQuota;
                        System.out.println("🎯 ONERI - Sottocategoria: " + sottoCategoria);
                    }
                }
            }

            // Gestione specifica TRASPORTI
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

            // Gestione Materia Energia
            if ("Materia Energia".equals(categoriaCorrente) || (sezioneCorretta && ricercaAvanzata)) {
                sezioneCorretta = true;

                if (lowerLine.contains("perdite di rete f1")) {
                    sottoCategoria = "Perdite F1";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                } else if (lowerLine.contains("perdite di rete f2")) {
                    sottoCategoria = "Perdite F2";
                    System.out.println("🎯 ENERGIA - Sottocategoria: " + sottoCategoria);
                }else if (lowerLine.contains("corrispettivo variabile di vendita energia")) {
                    sottoCategoria = "Corrispettivo variabile";
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

            // Estrazione valori Euro generica
            if ((categoriaCorrente != null && !categoriaCorrente.isEmpty()) && lineText.contains("€")) {
                Double valore = extractEuroValue(lineText);
                if (valore != null) {
                    String chiave;
                    if ((sezioneOneri || sezioneTrasporti) && !sottoCategoria.isEmpty()) {
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

        // RiepiLogCustom.logo + processamento (immutato)
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 RIEPILOGO COMPLETO DEI DATI RACCOLTI (LISTE)");
        System.out.println("=".repeat(80));
        for (Map.Entry<String, Map<String, List<Double>>> meseEntry : spesePerMese.entrySet()) {
            String mese = meseEntry.getKey();
            System.out.println("\n🗓️ MESE: " + mese);
            System.out.println("-".repeat(50));
            for (Map.Entry<String, List<Double>> catEntry : meseEntry.getValue().entrySet()) {
                String categoria = catEntry.getKey();
                List<Double> valori = catEntry.getValue();
                double somma = valori.stream().mapToDouble(Double::doubleValue).sum();
                System.out.printf("   📁 %-40s : %s (SOMMA: %.2f €)%n", categoria, valori.toString(), somma);
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

        LogCustom.logNestedLists("RAW - Spese per mese (liste)", spesePerMese);
        return result;
    }

    private Double extractEuroValueSafe(String s) {
        if (s == null) return null;
        try {
            Double v = extractEuroValue(s); // usa già la tua funzione
            if (v != null && v >= 0) return v;
        } catch (Exception ignored) {}
        // piccolo fallback: cerca "€" e primo numero dopo
        String t = s.replace("\u00A0", " ");
        int euro = t.indexOf('€');
        if (euro >= 0) {
            String tail = t.substring(euro + 1).replace(".", "").replace(",", ".").replaceAll("[^0-9.\\-]", " ").trim();
            for (String part : tail.split("\\s+")) {
                try { return Double.parseDouble(part); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public Map<String, Map<String, Double>> extractKwhPerMese(Document document) {
        LogCustom.logTitle("extractKwhPerMese");
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
                }else if (lowerLine.contains("corrispettivo variabile di vendita energia")) {
                    sottoCategoria = "Corrispettivo variabile";
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

        LogCustom.logNestedLists("RAW - kWh/kVARh per mese (liste)", kwhPerMese);
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
        LogCustom.logNestedDoubles("AGGREGATE - kWh/kVARh per mese (somma per categoria)", result);
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

        LogCustom.logNestedDoubles("AGGREGATE - Spese per mese (somma per categoria)", speseFinali);
        return speseFinali;
    }

    public String estraiMese(String lineText) {
        Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");
        Matcher matcher = pattern.matcher(lineText);

        if (matcher.find()) {
            String dataTrovata = matcher.group(1);
            LocalDate parsedDate = LocalDate.parse(dataTrovata,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            String nomeMese = parsedDate.getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.ITALIAN);

            if (nomeMese != null) {
                LogCustom.logOk("Mese estratto: " + nomeMese);
            }
            return nomeMese;
        }
        return null;
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

    private static String findFascia(String s) {
        String t = normalize(s);
        if (t.contains(" f1") || t.contains("f1")) return "F1";
        if (t.contains(" f2") || t.contains("f2")) return "F2";
        if (t.contains(" f3") || t.contains("f3")) return "F3";
        return null;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ITALY);
    }

    private static Integer findSoglia(String s) {
        String t = normalize(s).replace(" ", "");
        if (t.contains("33%") || t.contains("oltreil33") || t.contains("superioreal33")) return 33;
        if (t.contains("75%") || t.contains("oltreil75") || t.contains("superioreal75")) return 75;
        return null;
    }

    public Map<String, Map<String, Map<String, Integer>>> extractLetture(Document document) {
        LogCustom.logTitle("extractLetture (multi-line aware: Reattiva / Reattiva C. Immessa / Reattiva I. Immessa separati)");
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
                    LogCustom.logOk("Categoria letture attivata: " + categoriaCorrente);
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

        LogCustom.logLetture("Letture separate (Attiva / Reattiva / Reattiva C. Immessa / Reattiva I. Immessa / Potenza)", lettureMese);
        return lettureMese;
    }

    // Normalizza (maiuscole, spazi singoli, senza accenti)
    private static String norm(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.toUpperCase(Locale.ITALIAN).replaceAll("\\s+", " ").trim();
    }

    // Riconoscimento categoria su riga corrente con contesto di una riga prima e una dopo
    public static String detectCategoriaLetturaSmart(String prev, String curr, String next) {
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

    public static String extractFasciaOraria(String lineText) {
        String regex = "F\\d";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lineText);

        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public Periodo extractPeriodo(Document document) {
        LogCustom.logTitle("extractPeriodo");
        NodeList lineNodes = document.getElementsByTagName("Line");

        // Useremo LocalDate per il parsing e poi convertiremo a java.util.Date
        java.time.LocalDate startLD = null;
        java.time.LocalDate endLD   = null;

        // 1) Cerca "bolletta per i consumi" e cattura qualche riga successiva
        StringBuilder window = new StringBuilder();
        int hitIndex = -1;
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node n = lineNodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;

            String text = n.getTextContent();
            if (text == null) continue;

            String norm = text.toLowerCase().trim();
            if (norm.contains("bolletta per i consumi")) {
                hitIndex = i;
                window.append(text).append(" ");
                for (int k = 1; k <= 6 && (i + k) < lineNodes.getLength(); k++) {
                    Node next = lineNodes.item(i + k);
                    if (next.getNodeType() == Node.ELEMENT_NODE) {
                        String t = next.getTextContent();
                        if (t != null) window.append(t).append(" ");
                    }
                }
                break;
            }
        }

        // Mappa mesi IT → numero
        java.util.Map<String, Integer> itMonth = new java.util.HashMap<>();
        {
            String[][] months = {
                    {"gennaio","01"},{"febbraio","02"},{"marzo","03"},{"aprile","04"},
                    {"maggio","05"},{"giugno","06"},{"luglio","07"},{"agosto","08"},
                    {"settembre","09"},{"ottobre","10"},{"novembre","11"},{"dicembre","12"}
            };
            for (String[] m : months) itMonth.put(m[0], Integer.valueOf(m[1]));
        }

        if (hitIndex != -1) {
            String blob = window.toString().replaceAll("\\s+", " ").toLowerCase();

            java.util.regex.Pattern pText = java.util.regex.Pattern.compile(
                    "dal\\s+(\\d{1,2})\\s+([a-zà-ù]+)\\s+(\\d{4})\\s+al\\s+(\\d{1,2})\\s+([a-zà-ù]+)\\s+(\\d{4})"
            );
            java.util.regex.Pattern pNum = java.util.regex.Pattern.compile(
                    "dal\\s+(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})\\s+al\\s+(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})"
            );

            java.util.regex.Matcher mText = pText.matcher(blob);
            java.util.regex.Matcher mNum  = pNum.matcher(blob);

            try {
                if (mText.find()) {
                    int d1 = Integer.parseInt(mText.group(1));
                    String m1s = mText.group(2);
                    int y1 = Integer.parseInt(mText.group(3));
                    int d2 = Integer.parseInt(mText.group(4));
                    String m2s = mText.group(5);
                    int y2 = Integer.parseInt(mText.group(6));

                    Integer m1 = itMonth.get(m1s);
                    Integer m2 = itMonth.get(m2s);
                    if (m1 != null && m2 != null) {
                        startLD = java.time.LocalDate.of(y1, m1, d1);
                        endLD   = java.time.LocalDate.of(y2, m2, d2);
                    }
                } else if (mNum.find()) {
                    int d1 = Integer.parseInt(mNum.group(1));
                    int m1 = Integer.parseInt(mNum.group(2));
                    int y1 = Integer.parseInt(mNum.group(3));
                    int d2 = Integer.parseInt(mNum.group(4));
                    int m2 = Integer.parseInt(mNum.group(5));
                    int y2 = Integer.parseInt(mNum.group(6));

                    startLD = java.time.LocalDate.of(y1, m1, d1);
                    endLD   = java.time.LocalDate.of(y2, m2, d2);
                }
            } catch (Exception e) {
                LogCustom.logWarn("Parsing date bolletta per i consumi fallito: " + e.getMessage());
            }
        }

        // 2) Conversione a java.util.Date (come prima) + log
        if (startLD != null && endLD != null) {
            java.time.ZoneId zone = java.time.ZoneId.systemDefault();
            java.util.Date dataInizio = java.util.Date.from(startLD.atStartOfDay(zone).toInstant());
            java.util.Date dataFine   = java.util.Date.from(endLD.atStartOfDay(zone).toInstant());
            String anno = String.valueOf(endLD.getYear());

            // Log in duplice formato per debug
            java.time.format.DateTimeFormatter iso = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
            LogCustom.logTitle("Periodo fattura (da 'bolletta per i consumi' o fallback)");
            LogCustom.logKV("Data inizio", dataInizio.toString() + " | " + startLD.format(iso)); // es: Wed Jan 31 ... | 2024-01-31
            LogCustom.logKV("Data fine",   dataFine.toString()   + " | " + endLD.format(iso));   // es: Thu Feb 29 ... | 2024-02-29
            LogCustom.logKV("Anno", anno);

            return new Periodo(dataInizio, dataFine, anno);
        } else {
            LogCustom.logWarn("Impossibile estrarre il periodo: date mancanti.");
            throw new IllegalStateException("Impossibile estrarre il periodo: date mancanti.");
        }
    }

}
