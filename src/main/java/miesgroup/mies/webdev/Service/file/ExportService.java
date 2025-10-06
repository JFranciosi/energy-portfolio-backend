package miesgroup.mies.webdev.Service.file;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import miesgroup.mies.webdev.Rest.Model.ExportRequest;
import miesgroup.mies.webdev.Rest.Model.PreviewResponse;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class ExportService {

    @Inject DataSource dataSource;

    /* ===================== Mapping BASE (sempre uppercase) ===================== */
    // Chiave CANONICA (uppercase) -> espressione SQL senza "AS"
    private static final Map<String, String> KEY_TO_SQL_BASE = new LinkedHashMap<>();
    static {
        // bolletta_pod
        KEY_TO_SQL_BASE.put("ID_POD",          "bp.id_pod");
        KEY_TO_SQL_BASE.put("NUMERO_FATTURA",  "bp.Numero_Fattura");
        KEY_TO_SQL_BASE.put("DATA_FATTURA",    "bp.Data_Fattura");

        // label competenza (preferisce bv)
        KEY_TO_SQL_BASE.put("COMPETENZA",
                "COALESCE(bv.Competenza_Label, CONCAT(DATE_FORMAT(bp.Periodo_Inizio,'%d/%m/%Y'),' - ',DATE_FORMAT(bp.Periodo_Fine,'%d/%m/%Y')))");

        // bolletta_voce
        KEY_TO_SQL_BASE.put("COD",                    "bv.Cod");
        KEY_TO_SQL_BASE.put("DESCRIZIONE",            "bv.Descrizione");
        KEY_TO_SQL_BASE.put("UNITA_MISURA",           "bv.Unita_Misura");
        KEY_TO_SQL_BASE.put("QUANTITA",               "bv.Quantita");
        KEY_TO_SQL_BASE.put("CORRISPETTIVO_UNITARIO", "bv.Corrispettivo_Unit");
        KEY_TO_SQL_BASE.put("TOTALE_VOCE",            "bv.Totale_Voce");
        KEY_TO_SQL_BASE.put("IVA_PERCENT",            "bv.IVA_Percent");
        KEY_TO_SQL_BASE.put("PARZIALI",               "bv.Parziali");
        KEY_TO_SQL_BASE.put("TOTALE2",                "bv.Totale2");
        KEY_TO_SQL_BASE.put("NOTE",                   "bv.Note");

        // N.B. INDIRIZZO / CAP / LOCALITA / FORNITORE / DISTRIBUTORE sono dinamici su POD
    }

    // Chiavi dinamiche POD (sempre uppercase)
    private static final Set<String> DYN_POD_KEYS = Set.of("INDIRIZZO","CAP","LOCALITA","FORNITORE","DISTRIBUTORE");

    /* ===================== Cache colonne POD ===================== */
    private volatile Set<String> podColsLower;

    private void ensurePodColumnsLoaded() {
        if (podColsLower != null) return;
        synchronized (this) {
            if (podColsLower != null) return;
            Set<String> out = new HashSet<>();
            try (Connection cn = dataSource.getConnection()) {
                DatabaseMetaData md = cn.getMetaData();
                try (ResultSet rs = md.getColumns(cn.getCatalog(), null, "pod", "%")) {
                    while (rs.next()) {
                        String col = rs.getString("COLUMN_NAME");
                        if (col != null) out.add(col.toLowerCase(Locale.ROOT));
                    }
                }
            } catch (SQLException ignored) {}
            podColsLower = out;
        }
    }
    private boolean podHas(String col) {
        ensurePodColumnsLoaded();
        return podColsLower.contains(col.toLowerCase(Locale.ROOT));
    }
    private String resolvePodCol(String... names) {
        for (String n : names) if (podHas(n)) return "p." + n;
        return null;
    }

    /* ===================== Utils chiavi ===================== */
    private static String canonicalKey(String key) {
        if (key == null) return null;
        String k = key.trim();
        if (k.equalsIgnoreCase("id_pod")) return "ID_POD";
        if (k.equalsIgnoreCase("iva"))    return "IVA_PERCENT"; // accettiamo anche "IVA"
        return k.toUpperCase(Locale.ROOT);
    }

    /* ===================== Validazione ===================== */
    private void validate(ExportRequest req) {
        if (req == null) throw new IllegalArgumentException("Payload mancante");
        if (req.getFields() == null || req.getFields().isEmpty())
            throw new IllegalArgumentException("Nessun campo selezionato");

        boolean noPods  = (req.getPodIds() == null || req.getPodIds().isEmpty());
        boolean noBills = (req.getBillIds() == null || req.getBillIds().isEmpty());
        if (noPods && noBills)
            throw new IllegalArgumentException("Seleziona almeno un POD o una bolletta");

        for (ExportRequest.Field f : req.getFields()) {
            String can = canonicalKey(f.key);
            boolean allowed = KEY_TO_SQL_BASE.containsKey(can) || DYN_POD_KEYS.contains(can);
            if (!allowed) throw new IllegalArgumentException("Campo non consentito: " + can);
        }
    }

    /* ===================== Preview ===================== */
    public PreviewResponse getPreview(ExportRequest req) {
        validate(req);
        BuildResult br = buildQuery(req, true);

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(br.sql)) {
            bindParams(ps, br.params);

            int max = req.getLimit() != null ? req.getLimit() : 200;
            int read = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next() && read < max) {
                    rows.add(mapRow(rs, req));
                    read++;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore preview: " + e.getMessage(), e);
        }
        return new PreviewResponse(br.headers, rows);
    }

    /* ===================== Export Excel ===================== */
    public byte[] exportExcel(ExportRequest req) throws Exception {
        validate(req);
        BuildResult br = buildQuery(req, false);

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(br.sql)) {
            bindParams(ps, br.params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapRow(rs, req));
            }
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Export");
            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont(); bold.setBold(true); headerStyle.setFont(bold);

            Row header = sh.createRow(0);
            for (int c = 0; c < br.headers.size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(br.headers.get(c));
                cell.setCellStyle(headerStyle);
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sh.createRow(r + 1);
                Map<String, Object> data = rows.get(r);
                for (int c = 0; c < br.headers.size(); c++) {
                    String col = br.headers.get(c);
                    Object v = data.get(col);
                    row.createCell(c).setCellValue(v == null ? "" : String.valueOf(v));
                }
            }
            for (int c = 0; c < br.headers.size(); c++) sh.autoSizeColumn(c);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /* ===================== Build query ===================== */
    private static class BuildResult {
        String sql;
        List<Object> params = new ArrayList<>();
        List<String> headers;
    }

    private BuildResult buildQuery(ExportRequest req, boolean forPreview) {
        // intestazioni visibili (alias dal FE o chiave)
        List<String> headers = new ArrayList<>();
        for (ExportRequest.Field f : req.getFields()) {
            headers.add((f.alias != null && !f.alias.isBlank()) ? f.alias : f.key);
        }

        StringBuilder sb = new StringBuilder("SELECT bp.Id_Bolletta AS __ID__");

        boolean joinPod = false;
        boolean joinVoce = requiresVoceJoin(req.getFields());

        for (ExportRequest.Field f : req.getFields()) {
            String can = canonicalKey(f.key);

            if (DYN_POD_KEYS.contains(can)) {
                switch (can) {
                    case "INDIRIZZO": {
                        // preferisci 'sede', fallback 'indirizzo'
                        String col = resolvePodCol("sede", "indirizzo");
                        sb.append(", ").append(col != null ? col : "NULL").append(" AS ").append(can);
                        if (col != null) joinPod = true;
                        break;
                    }
                    case "CAP": {
                        String col = resolvePodCol("cap");
                        sb.append(", ").append(col != null ? col : "NULL").append(" AS ").append(can);
                        if (col != null) joinPod = true;
                        break;
                    }
                    case "LOCALITA": {
                        // preferisci 'citta', fallback 'localita'
                        String col = resolvePodCol("citta", "localita");
                        sb.append(", ").append(col != null ? col : "NULL").append(" AS ").append(can);
                        if (col != null) joinPod = true;
                        break;
                    }
                    case "FORNITORE": {
                        String expr = podHas("fornitore")
                                ? "COALESCE(bp.Fornitore, p.fornitore)"
                                : "bp.Fornitore";
                        sb.append(", ").append(expr).append(" AS ").append(can);
                        if (podHas("fornitore")) joinPod = true;
                        break;
                    }
                    case "DISTRIBUTORE": {
                        String expr = podHas("distributore")
                                ? "COALESCE(bp.Distributore, p.distributore)"
                                : "bp.Distributore";
                        sb.append(", ").append(expr).append(" AS ").append(can);
                        if (podHas("distributore")) joinPod = true;
                        break;
                    }
                }
            } else {
                // statico: prendi espressione base e alias come CANONICAL KEY
                String base = KEY_TO_SQL_BASE.get(can);
                if (base != null) {
                    sb.append(", ").append(base).append(" AS ").append(can);
                }
            }
        }

        // FROM + JOIN
        sb.append(" FROM bolletta_pod bp ");
        if (joinPod)  sb.append(" LEFT JOIN pod p ON p.id_pod = bp.id_pod ");
        if (joinVoce) sb.append(" LEFT JOIN bolletta_voce bv ON bv.Id_Bolletta = bp.Id_Bolletta ");

        // WHERE
        List<Object> params = new ArrayList<>();
        List<String> cond = new ArrayList<>();
        if (req.getPodIds() != null && !req.getPodIds().isEmpty()) {
            cond.add("bp.id_pod IN (" + placeholders(req.getPodIds().size()) + ")");
            params.addAll(req.getPodIds());
        }
        if (req.getBillIds() != null && !req.getBillIds().isEmpty()) {
            cond.add("bp.Id_Bolletta IN (" + placeholders(req.getBillIds().size()) + ")");
            params.addAll(req.getBillIds());
        }
        if ((req.getPeriodStart() != null && !req.getPeriodStart().isBlank())
                || (req.getPeriodEnd() != null && !req.getPeriodEnd().isBlank())) {
            LocalDate start = parseDate(req.getPeriodStart());
            LocalDate end   = parseDate(req.getPeriodEnd());
            if (start != null) { cond.add("NOT (bp.Periodo_Fine < ?)");   params.add(Date.valueOf(start)); }
            if (end   != null) { cond.add("NOT (bp.Periodo_Inizio > ?)"); params.add(Date.valueOf(end)); }
        }
        if (!cond.isEmpty()) sb.append(" WHERE ").append(String.join(" AND ", cond));

        // ORDER BY
        String groupBy = nvl(req.getGroupBy(), "fattura").toLowerCase(Locale.ITALY);
        switch (groupBy) {
            case "pod"  -> sb.append(" ORDER BY bp.id_pod, bp.Data_Fattura IS NULL, bp.Data_Fattura, bp.Id_Bolletta ");
            case "mese" -> sb.append(" ORDER BY bp.Anno, bp.Mese, bp.Id_Bolletta ");
            default     -> sb.append(" ORDER BY bp.Data_Fattura IS NULL, bp.Data_Fattura, bp.Numero_Fattura, bp.Id_Bolletta ");
        }

        if (forPreview && req.getLimit() != null && req.getLimit() > 0)
            sb.append(" LIMIT ").append(req.getLimit());

        BuildResult br = new BuildResult();
        br.sql = sb.toString();
        br.params = params;
        br.headers = headers;
        return br;
    }

    private static boolean requiresVoceJoin(List<ExportRequest.Field> fields) {
        for (ExportRequest.Field f : fields) {
            String can = canonicalKey(f.key);
            switch (can) {
                case "COD":
                case "DESCRIZIONE":
                case "UNITA_MISURA":
                case "QUANTITA":
                case "CORRISPETTIVO_UNITARIO":
                case "TOTALE_VOCE":
                case "IVA_PERCENT":
                case "PARZIALI":
                case "TOTALE2":
                case "NOTE":
                case "COMPETENZA":
                    return true;
            }
        }
        return false;
    }

    /* ===================== Row mapping & formatting ===================== */
    private Map<String, Object> mapRow(ResultSet rs, ExportRequest req) throws SQLException {
        String decimalSep  = nvl(req.getDecimalSep(), "comma");
        String dateFormat  = nvl(req.getDateFormat(), "dd/MM/yyyy");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat);
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.ROOT);
        if ("comma".equalsIgnoreCase(decimalSep)) { dfs.setDecimalSeparator(','); dfs.setGroupingSeparator('.'); }
        else { dfs.setDecimalSeparator('.'); dfs.setGroupingSeparator(','); }
        DecimalFormat numFmt = new DecimalFormat("#,##0.######", dfs);

        Map<String, Object> out = new LinkedHashMap<>();
        for (ExportRequest.Field f : req.getFields()) {
            String can = canonicalKey(f.key);                 // label presente in ResultSet
            String alias = (f.alias != null && !f.alias.isBlank()) ? f.alias : f.key; // nome colonna in output
            Object raw = rs.getObject(can);
            out.put(alias, formatValue(raw, numFmt, dtf));
        }
        return out;
    }

    private static Object formatValue(Object raw, DecimalFormat numFmt, DateTimeFormatter dtf) {
        if (raw == null) return null;

        if (raw instanceof java.sql.Date sdate) {
            return dtf.format(sdate.toLocalDate());
        }
        if (raw instanceof java.sql.Timestamp ts) {
            return dtf.format(ts.toLocalDateTime().toLocalDate());
        }
        if (raw instanceof java.util.Date udate) {
            LocalDate d = udate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return dtf.format(d);
        }
        if (raw instanceof BigDecimal bd) return numFmt.format(bd);
        if (raw instanceof Number n)      return numFmt.format(n.doubleValue());
        return String.valueOf(raw);
    }

    /* ===================== Misc utils ===================== */
    private static String placeholders(int n) { return String.join(",", Collections.nCopies(n, "?")); }
    private static void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        int i = 1;
        for (Object p : params) {
            if (p instanceof java.util.Date && !(p instanceof java.sql.Date)) {
                ps.setDate(i++, new java.sql.Date(((java.util.Date)p).getTime()));
            } else if (p instanceof LocalDate) {
                ps.setDate(i++, Date.valueOf((LocalDate) p));
            } else {
                ps.setObject(i++, p);
            }
        }
    }
    private static String nvl(String v, String def) { return (v == null || v.isBlank()) ? def : v; }
    private static LocalDate parseDate(String s) {
        try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s); }
        catch (Exception e) { return null; }
    }
}
