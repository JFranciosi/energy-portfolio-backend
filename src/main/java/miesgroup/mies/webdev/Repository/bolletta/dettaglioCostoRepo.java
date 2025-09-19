package miesgroup.mies.webdev.Repository.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.bolletta.dettaglioCosto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class dettaglioCostoRepo implements PanacheRepositoryBase<dettaglioCosto, Integer> {

    public dettaglioCostoRepo() {
        System.out.println("dettaglioCostoRepo instance created");
    }

    public boolean aggiungiCosto(dettaglioCosto costo) {
        System.out.println("aggiungiCosto called with costo=" + costo);
        costo.persist();
        boolean persisted = costo.isPersistent();
        System.out.println("costo.persist() called, isPersistent=" + persisted);
        return persisted;
    }

    public List<dettaglioCosto> getAllCosti() {
        System.out.println("getAllCosti called");
        List<dettaglioCosto> list = listAll();
        System.out.println("listAll returned list size=" + (list == null ? 0 : list.size()));
        return list;
    }

    public dettaglioCosto getSum(String intervalloPotenza) {
        System.out.println("getSum called with intervalloPotenza=" + intervalloPotenza);
        Optional<Double> sommaCosto = find("intervalloPotenza = ?1 AND categoria = 'trasporti' " +
                "AND (modality = 2 OR checkModality IS NOT NULL) " +
                "AND unitaMisura = '€/KWh'", intervalloPotenza)
                .project(Double.class)
                .firstResultOptional();
        if (sommaCosto.isEmpty()) {
            System.out.println("No sum risultato for intervalloPotenza=" + intervalloPotenza);
            return null;
        }
        dettaglioCosto costo = new dettaglioCosto();
        costo.setCosto(sommaCosto.get());
        System.out.println("Sum found: " + sommaCosto.get());
        return costo;
    }

    public void deleteCosto(int id) {
        System.out.println("deleteCosto called with id=" + id);
        deleteById(id);
        System.out.println("deleteById executed");
    }

    public boolean updateCosto(dettaglioCosto c) {
        System.out.println("updateCosto called with costo id=" + c.getId());
        int rowsUpdated = update("descrizione = ?1, categoria = ?2, unitaMisura = ?3, " +
                        "modality = ?4, checkModality = ?5, costo = ?6, " +
                        "intervalloPotenza = ?7, classeAgevolazione = ?8 " +
                        "WHERE id = ?9",
                c.getItemDescription(), c.getCategoria(), c.getUnitaMisura(),
                c.getModality(), c.getCheckModality(), c.getCosto(),
                c.getIntervalloPotenza(), c.getClasseAgevolazione(), c.getId());
        System.out.println("update query executed, rows updated: " + rowsUpdated);
        return rowsUpdated > 0;
    }

    public Optional<Double> findByCategoriaUnitaTrimestre(String categoria, String unitaMisura, String intervalloPotenza, int modality, String annoBolletta) {
        System.out.println("findByCategoriaUnitaTrimestre called with categoria=" + categoria + ", unitaMisura=" + unitaMisura + ", intervalloPotenza=" + intervalloPotenza + ", modality=" + modality + ", annoBolletta=" + annoBolletta);
        List<dettaglioCosto> costi = find("categoria = ?1 AND unitaMisura = ?2 AND intervalloPotenza = ?3 AND (modality = ?4 OR checkModality IS NOT NULL) AND annoRiferimento = ?5",
                categoria, unitaMisura, intervalloPotenza, modality, annoBolletta).list();
        System.out.println("Query returned " + (costi == null ? 0 : costi.size()) + " records");
        if (costi.isEmpty()) {
            System.out.println("No costi found");
            return Optional.empty();
        } else {
            double somma = 0;
            for (dettaglioCosto c : costi) {
                somma += c.getCosto();
                System.out.println("Adding costo: " + c.getCosto() + ", running sum: " + somma);
            }
            System.out.println("Total sum of costi: " + somma);
            return Optional.of(somma);
        }
    }

    public List<dettaglioCosto> getArticoli(String checkModality, String mese, String categoria, String rangePotenza) {
        System.out.println("getArticoli called with checkModality=" + checkModality + ", mese=" + mese + ", categoria=" + categoria + ", rangePotenza=" + rangePotenza);
        int modality = switch (mese.toLowerCase()) {
            case "gennaio", "febbraio", "marzo" -> 1;
            case "aprile", "maggio", "giugno" -> 2;
            case "luglio", "agosto", "settembre" -> 3;
            default -> 4;
        };
        System.out.println("Determined modality to be " + modality);
        List<dettaglioCosto> result = find("""
                categoria = ?1\s
                AND annoRiferimento = ?2\s
                AND intervalloPotenza = ?3\s
                AND (modality = ?4 OR checkModality IS NOT NULL)
                \s""", categoria, checkModality, rangePotenza, modality).list();
        System.out.println("Query returned " + (result == null ? 0 : result.size()) + " articles");
        return result;
    }

    public List<dettaglioCosto> getArticoliDispacciamento(String checkModality, String mese, String categoria) {
        System.out.println("getArticoliDispacciamento called with checkModality=" + checkModality + ", mese=" + mese + ", categoria=" + categoria);
        int modality = switch (mese.toLowerCase()) {
            case "gennaio", "febbraio", "marzo" -> 1;
            case "aprile", "maggio", "giugno" -> 2;
            case "luglio", "agosto", "settembre" -> 3;
            default -> 4;
        };
        System.out.println("Determined modality to be " + modality);
        List<dettaglioCosto> result = find("""
                categoria = ?1\s
                AND annoRiferimento = ?2\s
                AND (modality = ?3 OR checkModality IS NOT NULL)
                \s""", categoria, checkModality, modality).list();
        System.out.println("Query returned " + (result == null ? 0 : result.size()) + " articles");
        return result;
    }

    public PanacheQuery<dettaglioCosto> getQueryAllCosti() {
        System.out.println("getQueryAllCosti called");
        return findAll();
    }

    public long deleteIds(List<Long> ids) {
        System.out.println("deleteIds called with ids: " + ids);
        long deletedCount = delete("id IN ?1", ids);
        System.out.println("Deleted records count: " + deletedCount);
        return deletedCount;
    }

    public List<String> getAnniRiferimento() {
        System.out.println("getAnniRiferimento called");
        List<String> anni = getEntityManager()
                .createQuery("SELECT DISTINCT d.annoRiferimento FROM dettaglioCosto d WHERE d.annoRiferimento IS NOT NULL ORDER BY d.annoRiferimento DESC", String.class)
                .getResultList();
        System.out.println("Returned anni count: " + (anni == null ? 0 : anni.size()));
        return anni;
    }

    public List<String> getIntervalliPotenza() {
        System.out.println("getIntervalliPotenza called");
        List<String> intervalli = getEntityManager()
                .createQuery("SELECT DISTINCT d.intervalloPotenza FROM dettaglioCosto d WHERE d.intervalloPotenza IS NOT NULL ORDER BY d.intervalloPotenza", String.class)
                .getResultList();
        System.out.println("Returned intervalli count: " + (intervalli == null ? 0 : intervalli.size()));
        return intervalli;
    }

    public List<String> getCategorie() {
        System.out.println("getCategorie called");
        List<String> categorie = getEntityManager()
                .createQuery("SELECT DISTINCT d.categoria FROM dettaglioCosto d WHERE d.categoria IS NOT NULL ORDER BY d.categoria", String.class)
                .getResultList();
        System.out.println("Returned categorie count: " + (categorie == null ? 0 : categorie.size()));
        return categorie;
    }

    public List<String> getClassiAgevolazione() {
        System.out.println("getClassiAgevolazione called");
        List<String> classi = getEntityManager()
                .createQuery("SELECT DISTINCT d.classeAgevolazione FROM dettaglioCosto d WHERE d.classeAgevolazione IS NOT NULL ORDER BY d.classeAgevolazione", String.class)
                .getResultList();
        System.out.println("Returned classi count: " + (classi == null ? 0 : classi.size()));
        return classi;
    }

    public List<String> getUnitaMisure() {
        System.out.println("getUnitaMisure called");
        List<String> unita = getEntityManager()
                .createQuery("SELECT DISTINCT d.unitaMisura FROM dettaglioCosto d WHERE d.unitaMisura IS NOT NULL ORDER BY d.unitaMisura", String.class)
                .getResultList();
        System.out.println("Returned unita count: " + (unita == null ? 0 : unita.size()));
        return unita;
    }

    public List<String> getItem() {
        System.out.println("getItem called");
        List<String> items = getEntityManager()
                .createQuery("SELECT DISTINCT d.item FROM dettaglioCosto d WHERE d.item IS NOT NULL ORDER BY d.item", String.class)
                .getResultList();
        System.out.println("Returned items count: " + (items == null ? 0 : items.size()));
        return items;
    }

    // ---- Helpers comuni ----

    private static String mapIntervalloPotenza(double potenza, String tensione) {
        System.out.println("mapIntervalloPotenza called with potenza=" + potenza + ", tensione=" + tensione);
        String t = tensione == null ? "" : tensione.trim().toLowerCase();
        if ("bassa".equals(t)) {
            String result = (potenza > 16.5) ? ">16,5 Kw" : null;
            System.out.println("mapIntervalloPotenza result for bassa: " + result);
            return result;
        } else if ("media".equals(t)) {
            if (potenza > 500) {
                System.out.println("mapIntervalloPotenza result for media: >500 Kw");
                return ">500 Kw";
            }
            if (potenza > 100) {
                System.out.println("mapIntervalloPotenza result for media: 100-500 Kw");
                return "100-500 Kw";
            }
            if (potenza > 16.5) {
                System.out.println("mapIntervalloPotenza result for media: >16,5 Kw");
                return ">16,5 Kw";
            }
            System.out.println("mapIntervalloPotenza result for media: null");
            return null;
        }
        System.out.println("mapIntervalloPotenza result: null");
        return null;
    }

    private static int modTri(int mese) {
        System.out.println("modTri called with mese=" + mese);
        return (mese - 1) / 3;
    }

    private static int modMen(int mese) {
        System.out.println("modMen called with mese=" + mese);
        return (mese - 1);
    }

    /**
     * Funzione GENERICA per ottenere la somma dei costi dalla tabella dettaglioCosto
     * in base ai filtri passati.
     *
     * @param categoria es. "corrispettivi" | "trasporti" | "dispacciamento" | "oneri" | "penali cosphi"
     * @param itemEq (opz.) item = :itemEq
     * @param itemLike (opz.) item LIKE :itemLike
     * @param itemNotLike (opz.) item NOT LIKE :itemNotLike
     * @param unitaMisura es. "€/kWh" | "€/Mese" | "€/(kWxMese)" | "€/kVARh"
     * @param mese 1..12
     * @param potenza potenza impegnata per mappare l'intervallo potenza
     * @param tensione "bassa" | "media"
     * @param annoRiferimento es. "2024"
     * @param classeAgevolazione (opz.) es. "Fat1" | "Fat2" | "Fat3" | "Val"
     * @return somma dei costi che matchano i filtri, 0.0 se nessun match/parametri incoerenti
     */
    public double getCosto(String categoria, String itemEq, String itemLike, String itemNotLike, String unitaMisura,
                           int mese, double potenza, String tensione, String annoRiferimento, String classeAgevolazione) {
        System.out.println("getCosto called with categoria=" + categoria + ", itemEq=" + itemEq + ", itemLike=" + itemLike + ", itemNotLike=" + itemNotLike + ", unitaMisura=" + unitaMisura + ", mese=" + mese + ", potenza=" + potenza + ", tensione=" + tensione + ", annoRiferimento=" + annoRiferimento + ", classeAgevolazione=" + classeAgevolazione);
        String intervallo = mapIntervalloPotenza(potenza, tensione);
        if (intervallo == null || mese < 1 || mese > 12) {
            System.out.println("Invalid intervallo or mese: intervallo=" + intervallo + ", mese=" + mese);
            return 0.0;
        }
        StringBuilder q = new StringBuilder();
        Map<String, Object> p = new HashMap<>();
        q.append("categoria = :cat ");
        p.put("cat", categoria);
        q.append("AND annoRiferimento = :anno ");
        p.put("anno", annoRiferimento);
        q.append("AND intervalloPotenza = :intv ");
        p.put("intv", intervallo);
        q.append("AND unitaMisura = :um ");
        p.put("um", unitaMisura);
        q.append("AND costo > 0 ");
        if (classeAgevolazione != null) {
            q.append("AND classeAgevolazione = :cl ");
            p.put("cl", classeAgevolazione);
        }
        if (itemEq != null) {
            q.append("AND item = :itEq ");
            p.put("itEq", itemEq);
        }
        if (itemLike != null) {
            q.append("AND item LIKE :itLike ");
            p.put("itLike", itemLike);
        }
        if (itemNotLike != null) {
            q.append("AND item NOT LIKE :itNotLike ");
            p.put("itNotLike", itemNotLike);
        }
        q.append("AND ((checkModality = 1 AND modality = 0) " +
                "OR (checkModality = 2 AND modality = :mtri) " +
                "OR (checkModality = 3 AND modality = :mmen)) ");
        p.put("mtri", modTri(mese));
        p.put("mmen", modMen(mese));

        System.out.println("Constructed query: " + q.toString());
        System.out.println("With parameters: " + p);

        List<dettaglioCosto> lista = find(q.toString(), p).list();
        double sum = lista.stream().mapToDouble(dettaglioCosto::getCosto).sum();
        System.out.println("Total sum returned from query: " + sum);
        return sum;
    }

    private String translateArticle(String item, String anno) {
        try {
            int year = Integer.parseInt(anno);
            if (year < 2025) {
                return item; // nessuna traduzione
            }
        } catch (NumberFormatException e) {
            System.out.println("Anno non valido: " + anno + ", mantengo item originale");
            return item;
        }

        // Traduzioni post 2025
        Map<String, String> translationMap = new HashMap<>();
        translationMap.put("Art. 25 bis", "Art. 25 bis");
        translationMap.put("Art. 45 (Annuale)", "Art. 3-24.3");
        translationMap.put("Art. 44.3", "Art. 3-24.4");
        translationMap.put("Art. 46", "Art. 3-24.5");
        translationMap.put("Art. 73", "Art. 3-24.6");
        translationMap.put("Art. 44 bis", "Art. 3-24.7");
        translationMap.put("Art. 45 (Trimestrale)", "Art. 3-24.8,1");

        String translated = translationMap.get(item);
        if (translated == null) {
            System.out.println("Nessuna traduzione trovata per: " + item + " (anno=" + anno + ")");
            return item;
        }

        return translated;
    }


    // Corrispettivi DISPACCIAMENTO (€/kWh)
    public Double getCorrispettiviDispacciamentoA2A(String item, int mese, String anno, double potenza, String tensione) {
        System.out.println("getCorrispettiviDispacciamentoA2A called with item=" + item + ", mese=" + mese + ", anno=" + anno + ", potenza=" + potenza + ", tensione=" + tensione);

        String translatedItem = translateArticle(item, anno);
        System.out.println("Tradotto item=" + translatedItem);

        return getCosto("dispacciamento", translatedItem, null, null,
                "€/kWh", mese, potenza, tensione, anno, null);
    }


    // TRASPORTI (var/fissa/potenza)
    public Double getCostiTrasporto(String item, int mese, double potenza, String tensione, String um, String anno) {
        System.out.println("getCostiTrasporto called with item=" + item + ", mese=" + mese + ", potenza=" + potenza + ", tensione=" + tensione + ", um=" + um + ", anno=" + anno);
        return getCosto("trasporti", item, null, null,
                um, mese, potenza, tensione, anno, null);
    }

    // ONERI (richiede classe agevolazione)
    public Double getCostiOneri(String item, int mese, double potenza, String tensione, String um, String classe, String anno) {
        System.out.println("getCostiOneri called with item=" + item + ", mese=" + mese + ", potenza=" + potenza + ", tensione=" + tensione + ", um=" + um + ", classe=" + classe + ", anno=" + anno);
        return getCosto("oneri", item, null, null,
                um, mese, potenza, tensione, anno, classe);
    }

    // Corrispettivi Peak e Off Peak (€/kWh)
    public Double getCostoPicco(int mese, double potenza, String tensione, String anno) {
        System.out.println("getCostoPicco called with mese=" + mese + ", potenza=" + potenza + ", tensione=" + tensione + ", anno=" + anno);
        return getCosto("corrispettivi", null, "%Peak%", "%Off Peak%",
                "€/kWh", mese, potenza, tensione, anno, null);
    }

    public Double getCostoFuoriPicco(int mese, double potenza, String tensione, String anno) {
        System.out.println("getCostoFuoriPicco called with mese=" + mese + ", potenza=" + potenza + ", tensione=" + tensione + ", anno=" + anno);
        return getCosto("corrispettivi", null, "%Off Peak%", null,
                "€/kWh", mese, potenza, tensione, anno, null);
    }

    // Penali cosphi (€/kVARh)
    public Double getPenaliSotto75(int mese, double potenza, String tensione, String anno, String classe) {
        System.out.println("getPenaliSotto75 called with mese=" + mese + ", potenza=" + potenza + ", tensione=" + tensione + ", anno=" + anno + ", classe=" + classe);
        return getCosto("penali cosphi", "Penali 33%", null, null,
                "€/kVARh", mese, potenza, tensione, anno, classe);
    }

    public Double getPenaliSopra75(int mese, double potenza, String tensione, String anno, String classe) {
        System.out.println("getPenaliSopra75 called with mese=" + mese + ", potenza=" + potenza + ", tensione=" + tensione + ", anno=" + anno + ", classe=" + classe);
        return getCosto("penali cosphi", "Penali 75%", null, null,
                "€/kVARh", mese, potenza, tensione, anno, classe);
    }

}
