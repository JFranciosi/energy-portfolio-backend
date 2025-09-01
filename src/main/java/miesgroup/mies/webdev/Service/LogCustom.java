package miesgroup.mies.webdev.Service;

import java.util.List;
import java.util.Map;

public class LogCustom {

    // ===== Config minima (facoltativa) =====
    private static volatile boolean ENABLED = true;

    public enum Level { DEBUG, INFO, WARN, ERROR }
    private static volatile Level MIN_LEVEL = Level.DEBUG;

    public static void setEnabled(boolean enabled) { ENABLED = enabled; }
    public static void setMinLevel(Level level) { if (level != null) MIN_LEVEL = level; }

    private static boolean allow(Level level) {
        return ENABLED && level.ordinal() >= MIN_LEVEL.ordinal();
    }

    // ===== API NUOVA (usata da LetturaBolletta) =====
    public static void title(String title) {
        if (!allow(Level.INFO)) return;
        System.out.println("\n================ " + title + " ================");
    }

    public static void kv(String key, Object value) {
        if (!allow(Level.INFO)) return;
        System.out.println("• " + key + ": " + String.valueOf(value));
    }

    public static void warn(String msg) {
        if (!allow(Level.WARN)) return;
        System.out.println("⚠️  " + msg);
    }

    public static void ok(String msg) {
        if (!allow(Level.INFO)) return;
        System.out.println("✅ " + msg);
    }

    public static void debug(String msg) {
        if (!allow(Level.DEBUG)) return;
        System.out.println("  · " + msg);
    }

    // ===== Helper per strutture annidate =====
    // Mappe: Mese -> (Categoria -> Valore Double)
    public static void nestedDoubles(String title, Map<String, Map<String, Double>> m) {
        if (!allow(Level.DEBUG)) return;
        title(title);
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
    public static void nestedLists(String title, Map<String, Map<String, List<Double>>> m) {
        if (!allow(Level.DEBUG)) return;
        title(title);
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
    public static void letture(String title, Map<String, Map<String, Map<String, Integer>>> m) {
        if (!allow(Level.DEBUG)) return;
        title(title);
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

    // ====== RETRO-COMPATIBILITÀ (alias vecchi nomi) ======
    /** @deprecated usare {@link #title(String)} */
    @Deprecated public static void logTitle(String t) { title(t); }
    /** @deprecated usare {@link #kv(String, Object)} */
    @Deprecated public static void logKV(String k, Object v) { kv(k, v); }
    /** @deprecated usare {@link #warn(String)} */
    @Deprecated public static void logWarn(String m) { warn(m); }
    /** @deprecated usare {@link #ok(String)} */
    @Deprecated public static void logOk(String m) { ok(m); }
    /** @deprecated usare {@link #nestedDoubles(String, Map)} */
    @Deprecated public static void logNestedDoubles(String title, Map<String, Map<String, Double>> m) { nestedDoubles(title, m); }
    /** @deprecated usare {@link #nestedLists(String, Map)} */
    @Deprecated public static void logNestedLists(String title, Map<String, Map<String, List<Double>>> m) { nestedLists(title, m); }
    /** @deprecated usare {@link #letture(String, Map)} */
    @Deprecated public static void logLetture(String title, Map<String, Map<String, Map<String, Integer>>> m) { letture(title, m); }

}
