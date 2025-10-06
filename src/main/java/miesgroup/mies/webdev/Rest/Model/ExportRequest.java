package miesgroup.mies.webdev.Rest.Model;

import java.util.ArrayList;
import java.util.List;

// Modello per la richiesta di esportazione dati (per preview o export vero e proprio)
public class ExportRequest {

    public static class Field {
        public String key;    // es: "id_pod", "NUMERO_FATTURA", "COD", ...
        public String alias;  // es: "POD", "Numero Fattura", "COD", ...
        public Field() {}
        public Field(String key, String alias) { this.key = key; this.alias = alias; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
    }

    private List<String> podIds;
    private List<Integer> billIds;

    private String groupBy;      // "fattura" | "mese" | "pod"
    private String orientation;  // "wide" | "long" (wide = default)
    private String decimalSep;   // "comma" | "dot"
    private String dateFormat;   // es. "dd/MM/yyyy" (default)

    private String periodStart;  // "YYYY-MM-DD" (opzionale)
    private String periodEnd;    // "YYYY-MM-DD" (opzionale)

    private Integer limit;       // per preview
    private String layout;       // "summary" | "detail" (default: "summary")

    private List<Field> fields = new ArrayList<>();

    public List<String> getPodIds() { return podIds; }
    public void setPodIds(List<String> podIds) { this.podIds = podIds; }
    public List<Integer> getBillIds() { return billIds; }
    public void setBillIds(List<Integer> billIds) { this.billIds = billIds; }
    public String getGroupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }
    public String getDecimalSep() { return decimalSep; }
    public void setDecimalSep(String decimalSep) { this.decimalSep = decimalSep; }
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    public String getPeriodStart() { return periodStart; }
    public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }
    public String getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public String getLayout() { return layout; }
    public void setLayout(String layout) { this.layout = layout; }
    public List<Field> getFields() { return fields; }
    public void setFields(List<Field> fields) { this.fields = fields; }
}
