package miesgroup.mies.webdev.Service.cliente;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.Map;

@ApplicationScoped
public class EmailTemplateGenerator {

    public String generateAlertConfigEmail(String clientName, String username, String futuresType, Map<String, Object> configData) {
        StringBuilder summaryContent = new StringBuilder();
        StringBuilder tablesContent = new StringBuilder();

        // Preparazione dei dati per il riepilogo
        summaryContent.append("<p>Il sistema ha registrato le seguenti configurazioni di alert per i prodotti energetici:</p>\n");
        summaryContent.append("<ul>\n");

        if (futuresType.equals("All")) {
            // Per ogni tipo di futures (Yearly, Quarterly, Monthly)
            String[] types = {"Yearly", "Quarterly", "Monthly"};
            double[] max = (double[]) configData.get("max");
            double[] min = (double[]) configData.get("min");
            String[] freq = (String[]) configData.get("freq");
            boolean[] checks = (boolean[]) configData.get("checks");

            for (int i = 0; i < types.length; i++) {
                summaryContent.append("    <li>Prodotti <strong>" + types[i] + "</strong>: livelli tra "
                        + String.format("%.1f%s", min[i], (checks[i] ? "%" : "")) + " e "
                        + String.format("%.1f%s", max[i], (checks[i] ? "%" : ""))+ "</li>\n");
            }
        } else {
            // Per un singolo tipo di futures
            double[] max = (double[]) configData.get("max");
            double[] min = (double[]) configData.get("min");
            boolean[] checks = (boolean[]) configData.get("checks");

            summaryContent.append("    <li>Prodotti <strong>" + futuresType + "</strong>: livelli tra "
                    + String.format("%.1f%s", min[0], (checks[0] ? "%" : "")) + " e "
                    + String.format("%.1f%s", max[0], (checks[0] ? "%" : ""))+ "</li>\n");
        }

        summaryContent.append("</ul>\n");

        // Creazione della tabella dettagliata
        if (futuresType.equals("All")) {
            String[] types = {"Yearly", "Quarterly", "Monthly"};
            double[] max = (double[]) configData.get("max");
            double[] min = (double[]) configData.get("min");
            String[] freq = (String[]) configData.get("freq");
            boolean[] checks = (boolean[]) configData.get("checks");

            tablesContent.append("<p class=\"table-title\">Configurazione degli alert</p>\n");
            tablesContent.append("<table class=\"price-table\">\n");
            tablesContent.append("    <thead>\n");
            tablesContent.append("        <tr>\n");
            tablesContent.append("            <th>Tipo prodotto</th>\n");
            tablesContent.append("            <th>Livello minimo</th>\n");
            tablesContent.append("            <th>Livello massimo</th>\n");
            tablesContent.append("            <th>Modalità</th>\n");
            tablesContent.append("        </tr>\n");
            tablesContent.append("    </thead>\n");
            tablesContent.append("    <tbody>\n");

            for (int i = 0; i < types.length; i++) {
                tablesContent.append("        <tr>\n");
                tablesContent.append("            <td>" + types[i] + "</td>\n");
                tablesContent.append("            <td>" + String.format("%.1f%s", min[i], (checks[i] ? "%" : "")) + "</td>\n");
                tablesContent.append("            <td>" + String.format("%.1f%s", max[i], (checks[i] ? "%" : "")) + "</td>\n");
                tablesContent.append("            <td>" + freq[i] + "</td>\n");
                tablesContent.append("            <td>" + (checks[i] ? "Percentuale" : "Valore assoluto") + "</td>\n");
                tablesContent.append("        </tr>\n");
            }

            tablesContent.append("    </tbody>\n");
            tablesContent.append("</table>\n");
        } else {
            double[] max = (double[]) configData.get("max");
            double[] min = (double[]) configData.get("min");
            String[] freq = (String[]) configData.get("freq");
            boolean[] checks = (boolean[]) configData.get("checks");

            tablesContent.append("<p class=\"table-title\">Configurazione degli alert</p>\n");
            tablesContent.append("<table class=\"price-table\">\n");
            tablesContent.append("    <thead>\n");
            tablesContent.append("        <tr>\n");
            tablesContent.append("            <th>Tipo prodotto</th>\n");
            tablesContent.append("            <th>Livello minimo</th>\n");
            tablesContent.append("            <th>Livello massimo</th>\n");
            tablesContent.append("            <th>Modalità</th>\n");
            tablesContent.append("        </tr>\n");
            tablesContent.append("    </thead>\n");
            tablesContent.append("    <tbody>\n");

            tablesContent.append("        <tr>\n");
            tablesContent.append("            <td>" + futuresType + "</td>\n");
            tablesContent.append("            <td>" + String.format("%.1f%s", min[0], (checks[0] ? "%" : "")) + "</td>\n");
            tablesContent.append("            <td>" + String.format("%.1f%s", max[0], (checks[0] ? "%" : "")) + "</td>\n");
            tablesContent.append("            <td>" + (checks[0] ? "Percentuale" : "Valore assoluto") + "</td>\n");
            tablesContent.append("        </tr>\n");

            tablesContent.append("    </tbody>\n");
            tablesContent.append("</table>\n");
        }

        boolean alertActive = (boolean) configData.get("checkEmail");
        String alertStatus = alertActive ?
                "<p class=\"alert-active\">Gli alert via email sono <strong>attivi</strong> per questa configurazione.</p>" :
                "<p class=\"alert-inactive\">Gli alert via email sono <strong>disattivati</strong> per questa configurazione.</p>";

        String formattedDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return "<!DOCTYPE html>\n" +
                "<html lang=\"it\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>MIES - Configurazione Alert Futures</title>\n" +
                "    " + getEmailStyles() + "\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>MIES</h1>\n" +
                "            <p>Configurazione Alert Futures</p>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Buongiorno " + clientName + ",</h2>\n" +
                "            <div class=\"alert-message\">\n" +
                "                <p>La configurazione degli alert per il monitoraggio dei futures è stata aggiornata con successo per l'account <strong>" + username + "</strong>.</p>\n" +
                "                " + alertStatus + "\n" +
                "            </div>\n" +
                "            <div class=\"summary-box\">\n" +
                summaryContent.toString() +
                "            </div>\n" +
                "            <div class=\"table-section\">\n" +
                tablesContent.toString() +
                "            </div>\n" +
                "            <p>Riceverà notifiche quando i prezzi dei futures raggiungeranno i livelli configurati.</p>\n" +
                "            <p>Per modificare queste impostazioni, acceda alla sezione \"Futures\" all'interno del sito.</p>\n" +
                "            <p>Cordiali saluti,<br>Il Team MIES</p>\n" +
                "        </div>\n" +
                "        " + getEmailFooter() + "\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String getEmailStyles() {
        return "    <style>\n" +
                "        /* Reset CSS */\n" +
                "        body, p, h1, h2, h3, h4, h5, h6, ul, ol, li {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "        body {\n" +
                "            font-family: 'Helvetica Neue', Arial, sans-serif;\n" +
                "            color: #333333;\n" +
                "            line-height: 1.6;\n" +
                "            background-color: #f5f5f5;\n" +
                "        }\n" +
                "        /* Container principale */\n" +
                "        .email-container {\n" +
                "            max-width: 750px;\n" +
                "            margin: 20px auto;\n" +
                "            background-color: #ffffff;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        /* Header */\n" +
                "        .header {\n" +
                "            background: linear-gradient(135deg, #27526A 0%, #1a3d50 100%);\n" +
                "            padding: 25px 30px;\n" +
                "            color: white;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .logo {\n" +
                "            height: auto;\n" +
                "            max-width: 120px;\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .header h1 {\n" +
                "            font-size: 32px;\n" +
                "            font-weight: 700;\n" +
                "            margin-bottom: 5px;\n" +
                "            letter-spacing: 1px;\n" +
                "        }\n" +
                "        .header p {\n" +
                "            font-size: 18px;\n" +
                "            opacity: 0.9;\n" +
                "            font-weight: 300;\n" +
                "        }\n" +
                "        /* Contenuto */\n" +
                "        .content {\n" +
                "            padding: 35px;\n" +
                "            background-color: #ffffff;\n" +
                "        }\n" +
                "        .content h2 {\n" +
                "            font-size: 22px;\n" +
                "            margin-bottom: 25px;\n" +
                "            color: #27526A;\n" +
                "            border-bottom: 2px solid #eaeaea;\n" +
                "            padding-bottom: 10px;\n" +
                "        }\n" +
                "        .content p {\n" +
                "            margin-bottom: 20px;\n" +
                "            font-size: 16px;\n" +
                "            color: #4B5563;\n" +
                "        }\n" +
                "        .alert-message {\n" +
                "            background-color: #f9f9f9;\n" +
                "            border-left: 4px solid #27526A;\n" +
                "            padding: 18px;\n" +
                "            margin-bottom: 30px;\n" +
                "            border-radius: 0 4px 4px 0;\n" +
                "        }\n" +
                "        /* Summary Box */\n" +
                "        .summary-box {\n" +
                "            background: linear-gradient(to right, #f8fbfd, #edf5fa);\n" +
                "            border: none;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 25px;\n" +
                "            margin-bottom: 35px;\n" +
                "            box-shadow: 0 1px 3px rgba(0,0,0,0.05);\n" +
                "        }\n" +
                "        .summary-box p {\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .summary-box strong {\n" +
                "            color: #27526A;\n" +
                "        }\n" +
                "        .summary-box ul {\n" +
                "            list-style-type: none;\n" +
                "            margin: 15px 0;\n" +
                "            padding-left: 10px;\n" +
                "        }\n" +
                "        .summary-box li {\n" +
                "            margin-bottom: 10px;\n" +
                "            position: relative;\n" +
                "            padding-left: 25px;\n" +
                "        }\n" +
                "        .summary-box li:before {\n" +
                "            content: \"•\";\n" +
                "            color: #27526A;\n" +
                "            font-weight: bold;\n" +
                "            font-size: 18px;\n" +
                "            position: absolute;\n" +
                "            left: 0;\n" +
                "        }\n" +
                "        /* Stats Card */\n" +
                "        .stats-card-container {\n" +
                "            display: flex;\n" +
                "            flex-wrap: wrap;\n" +
                "            gap: 20px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .stats-card {\n" +
                "            flex: 1;\n" +
                "            min-width: 200px;\n" +
                "            background: white;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 20px;\n" +
                "            box-shadow: 0 2px 8px rgba(0,0,0,0.08);\n" +
                "            border-top: 4px solid #27526A;\n" +
                "        }\n" +
                "        .stats-card h3 {\n" +
                "            font-size: 16px;\n" +
                "            color: #666;\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .stats-values {\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .stats-value {\n" +
                "            font-size: 28px;\n" +
                "            font-weight: 700;\n" +
                "        }\n" +
                "        .positive {\n" +
                "            color: #28a745;\n" +
                "        }\n" +
                "        .negative {\n" +
                "            color: #dc3545;\n" +
                "        }\n" +
                "        .trend-icon {\n" +
                "            font-size: 24px;\n" +
                "            margin-left: 10px;\n" +
                "        }\n" +
                "        /* Tabelle */\n" +
                "        .table-section {\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .table-title {\n" +
                "            font-size: 20px;\n" +
                "            font-weight: 600;\n" +
                "            margin: 30px 0 15px 0;\n" +
                "            color: #27526A;\n" +
                "            padding-bottom: 8px;\n" +
                "            border-bottom: 2px solid rgba(39, 82, 106, 0.2);\n" +
                "        }\n" +
                "        .price-table {\n" +
                "            width: 100%;\n" +
                "            border-collapse: separate;\n" +
                "            border-spacing: 0;\n" +
                "            margin-bottom: 30px;\n" +
                "            font-size: 14px;\n" +
                "            box-shadow: 0 1px 3px rgba(0,0,0,0.1);\n" +
                "            border-radius: 8px;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        .price-table th {\n" +
                "            background: linear-gradient(to right, #27526A, #1a3d50);\n" +
                "            color: white;\n" +
                "            text-align: left;\n" +
                "            padding: 14px 15px;\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        .price-table td {\n" +
                "            padding: 12px 15px;\n" +
                "            border-top: 1px solid #edf2f7;\n" +
                "        }\n" +
                "        .price-table tr:last-child td {\n" +
                "            border-bottom: none;\n" +
                "        }\n" +
                "        .price-table tr:hover {\n" +
                "            background-color: #f9fafb;\n" +
                "        }\n" +
                "        /* Trend Summary */\n" +
                "        .trend-summary {\n" +
                "            background-color: white;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 15px 20px;\n" +
                "            margin: 15px 0 25px;\n" +
                "            box-shadow: 0 1px 3px rgba(0,0,0,0.05);\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            flex-wrap: wrap;\n" +
                "        }\n" +
                "        .trend-summary p {\n" +
                "            margin: 5px 15px 5px 0;\n" +
                "            white-space: nowrap;\n" +
                "        }\n" +
                "        /* Footer */\n" +
                "        .footer {\n" +
                "            background-color: #f8f9fa;\n" +
                "            padding: 25px 35px;\n" +
                "            font-size: 13px;\n" +
                "            color: #666;\n" +
                "            border-top: 1px solid #e1e1e1;\n" +
                "        }\n" +
                "        .footer p {\n" +
                "            margin-bottom: 5px;\n" +
                "        }\n" +
                "        .company-info {\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .company-info a {\n" +
                "            color: #27526A;\n" +
                "            text-decoration: none;\n" +
                "        }\n" +
                "        .legal-info {\n" +
                "            font-size: 11px;\n" +
                "            color: #888;\n" +
                "            border-top: 1px solid #e1e1e1;\n" +
                "            padding-top: 15px;\n" +
                "            margin-top: 15px;\n" +
                "        }\n" +
                "        .environmental-notice {\n" +
                "            font-style: italic;\n" +
                "            color: #27526A;\n" +
                "            margin-top: 20px;\n" +
                "            text-align: center;\n" +
                "            padding: 10px;\n" +
                "            background-color: #f0f5f8;\n" +
                "            border-radius: 4px;\n" +
                "        }\n" +
                "        /* Responsive */\n" +
                "        @media only screen and (max-width: 600px) {\n" +
                "            .email-container {\n" +
                "                margin: 10px;\n" +
                "                border-radius: 6px;\n" +
                "            }\n" +
                "            .header, .content, .footer {\n" +
                "                padding: 20px;\n" +
                "            }\n" +
                "            .header h1 {\n" +
                "                font-size: 24px;\n" +
                "            }\n" +
                "            .header p {\n" +
                "                font-size: 16px;\n" +
                "            }\n" +
                "            .price-table {\n" +
                "                font-size: 12px;\n" +
                "            }\n" +
                "            .stats-card-container {\n" +
                "                flex-direction: column;\n" +
                "            }\n" +
                "            .trend-summary {\n" +
                "                flex-direction: column;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n";
    }

    private String getEmailFooter() {
        return "        <!-- Footer -->\n" +
                "        <div class=\"footer\">\n" +
                "            <div class=\"company-info\">\n" +
                "                <p><strong>MIES Group - Energy Portfolio</strong></p>\n" +
                "                <p><a href=\"http://www.miesgroup.it\">www.miesgroup.it</a></p>\n" +
                "                <p><strong>Sede Legale</strong></p>\n" +
                "                <p>Via Puricelli, 1 – Gallarate (VA)</p>\n" +
                "                <p>P.IVA 03635250123</p>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class=\"legal-info\">\n" +
                "                <p>** AVVERTENZE AI SENSI DEL DLGS 196/2003 e del Regolamento Europeo 679/2016 (GDPR) **</p>\n" +
                "                <p>**Le informazioni contenute in questo messaggio di posta elettronica e/o nell'eventuale file/s allegato/i, sono da considerarsi strettamente riservate. Il loro utilizzo è consentito esclusivamente al destinatario del messaggio, per le finalità indicate nel messaggio stesso. Costituisce comportamento contrario ai principi dettati dall'art. 616 c.p., ART. 13 Dlgs. 196/2003 e art. 13 UE GDPR il trattenere il messaggio stesso, divulgarlo anche in parte, distribuirlo ad altri soggetti, copiarlo, od utilizzarlo per finalità diverse. Se avete ricevuto questa mail per errore vogliate eliminare il messaggio in modo permanente e darcene cortesemente notizia**</p>\n" +
                "                <p>** The information in this e-mail (which includes any attached files) is confidential and may be legally privileged (art. 616 c.p., Dlgs 196/2003 e UE GDPR). It is intended for the addressee only. Any use, dissemination, forwarding, printing or copying of this e-mail is prohibited by any person other than the addressee. If you have received this e-mail in error please notify us immediately by e-mail promptly and destroy this message**</p>\n" +
                "            </div>\n" +
                "            \n" +
                "            <div class=\"environmental-notice\">\n" +
                "                <p>** Prima di stampare, pensa all'ambiente! ** Think about the environment before printing **</p>\n" +
                "            </div>\n" +
                "        </div>\n";
    }
}
