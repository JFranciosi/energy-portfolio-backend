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
    }


    public boolean aggiungiCosto(dettaglioCosto costo) {
        costo.persist();
        return costo.isPersistent(); // Restituisce true se l'entità è stata salvata correttamente
    }


    public List<dettaglioCosto> getAllCosti() {
        return listAll();
    }


    public dettaglioCosto getSum(String intervalloPotenza) {
        Optional<Double> sommaCosto = find("intervalloPotenza = ?1 AND categoria = 'trasporti' " +
                "AND (modality = 2 OR checkModality IS NOT NULL) " +
                "AND unitaMisura = '€/KWh'", intervalloPotenza)
                .project(Double.class)
                .firstResultOptional();

        if (sommaCosto.isEmpty()) {
            return null;
        }

        dettaglioCosto costo = new dettaglioCosto();
        costo.setCosto(sommaCosto.get());
        return costo;
    }


    public void deleteCosto(int id) {
        deleteById(id);
    }


    public boolean updateCosto(dettaglioCosto c) {
        return update("descrizione = ?1, categoria = ?2, unitaMisura = ?3, " +
                        "modality = ?4, checkModality = ?5, costo = ?6, " +
                        "intervalloPotenza = ?7, classeAgevolazione = ?8 " +
                        "WHERE id = ?9",
                c.getItemDescription(), c.getCategoria(), c.getUnitaMisura(),
                c.getModality(), c.getCheckModality(), c.getCosto(),
                c.getIntervalloPotenza(), c.getClasseAgevolazione(), c.getId()) > 0;
    }


    public Optional<Double> findByCategoriaUnitaTrimestre(String categoria, String unitaMisura, String intervalloPotenza, int modality, String annoBolletta) {
        List<dettaglioCosto> costi = find("categoria = ?1 AND unitaMisura = ?2 AND intervalloPotenza = ?3 AND (modality = ?4 OR checkModality IS NOT NULL) AND annoRiferimento = ?5",
                categoria, unitaMisura, intervalloPotenza, modality, annoBolletta).list();

        if (costi.isEmpty()) {
            return Optional.empty();
        } else {
            double somma = 0;
            for (dettaglioCosto c : costi) {
                somma += c.getCosto();
            }
            return Optional.of(somma);
        }

    }

    public List<dettaglioCosto> getArticoli(String checkModality, String mese, String categoria, String rangePotenza) {
        int modality = switch (mese.toLowerCase()) {
            case "gennaio", "febbraio", "marzo" -> 1;
            case "aprile", "maggio", "giugno" -> 2;
            case "luglio", "agosto", "settembre" -> 3;
            default -> 4;
        };

        return find("""
                 categoria = ?1\s
                 AND annoRiferimento = ?2\s
                 AND intervalloPotenza = ?3\s
                 AND (modality = ?4 OR checkModality IS NOT NULL)
                \s""", categoria, checkModality, rangePotenza, modality).list();
    }


    public List<dettaglioCosto> getArticoliDispacciamento(String checkModality, String mese, String categoria) {
        int modality = switch (mese.toLowerCase()) {
            case "gennaio", "febbraio", "marzo" -> 1;
            case "aprile", "maggio", "giugno" -> 2;
            case "luglio", "agosto", "settembre" -> 3;
            default -> 4;
        };

        return find("""
                 categoria = ?1\s
                 AND annoRiferimento = ?2\s
                 AND (modality = ?3 OR checkModality IS NOT NULL)
                \s""", categoria, checkModality, modality).list();
    }

    public PanacheQuery<dettaglioCosto> getQueryAllCosti() {
        return findAll();
    }

    public long deleteIds(List<Long> ids) {
        return delete("id IN ?1", ids);
    }

    public List<String> getAnniRiferimento() {
        return getEntityManager()
                .createQuery("SELECT DISTINCT d.annoRiferimento FROM dettaglioCosto d WHERE d.annoRiferimento IS NOT NULL ORDER BY d.annoRiferimento DESC", String.class)
                .getResultList();
    }

    public List<String> getIntervalliPotenza() {
        return getEntityManager()
                .createQuery("SELECT DISTINCT d.intervalloPotenza FROM dettaglioCosto d WHERE d.intervalloPotenza IS NOT NULL ORDER BY d.intervalloPotenza", String.class)
                .getResultList();
    }

    public List<String> getCategorie() {
        return getEntityManager()
                .createQuery("SELECT DISTINCT d.categoria FROM dettaglioCosto d WHERE d.categoria IS NOT NULL ORDER BY d.categoria", String.class)
                .getResultList();
    }

    public List<String> getClassiAgevolazione() {
        return getEntityManager()
                .createQuery("SELECT DISTINCT d.classeAgevolazione FROM dettaglioCosto d WHERE d.classeAgevolazione IS NOT NULL ORDER BY d.classeAgevolazione", String.class)
                .getResultList();
    }

    public List<String> getUnitaMisure() {
        return getEntityManager()
                .createQuery("SELECT DISTINCT d.unitaMisura FROM dettaglioCosto d WHERE d.unitaMisura IS NOT NULL ORDER BY d.unitaMisura", String.class)
                .getResultList();
    }

    public List<String> getItem() {
        return getEntityManager()
                .createQuery("SELECT DISTINCT d.item FROM dettaglioCosto d WHERE d.item IS NOT NULL ORDER BY d.item", String.class)
                .getResultList();
    }

    // ---- Helpers comuni ----
    private static String mapIntervalloPotenza(double potenza, String tensione) {
        String t = tensione == null ? "" : tensione.trim().toLowerCase();
        if ("bassa".equals(t)) {
            return (potenza > 16.5) ? ">16,5 Kw" : null;
        } else if ("media".equals(t)) {
            if (potenza > 500)  return ">500 Kw";
            if (potenza > 100)  return "100-500 Kw";
            if (potenza > 16.5) return ">16,5 Kw";
            return null;
        }
        return null;
    }

    private static int modTri(int mese) { return (mese - 1) / 3; }
    private static int modMen(int mese) { return (mese - 1); }

    /**
     * Funzione GENERICA per ottenere la somma dei costi dalla tabella dettaglioCosto
     * in base ai filtri passati.
     *
     * @param categoria          es. "corrispettivi" | "trasporti" | "dispacciamento" | "oneri" | "penali cosphi"
     * @param itemEq             (opz.) item = :itemEq
     * @param itemLike           (opz.) item LIKE :itemLike
     * @param itemNotLike        (opz.) item NOT LIKE :itemNotLike
     * @param unitaMisura        es. "€/kWh" | "€/Mese" | "€/(kWxMese)" | "€/kVARh"
     * @param mese               1..12
     * @param potenza            potenza impegnata per mappare l'intervallo potenza
     * @param tensione           "bassa" | "media"
     * @param annoRiferimento    es. "2024"
     * @param classeAgevolazione (opz.) es. "Fat1" | "Fat2" | "Fat3" | "Val"
     * @return somma dei costi che matchano i filtri, 0.0 se nessun match/parametri incoerenti
     */
    public double getCosto(String categoria, String itemEq, String itemLike, String itemNotLike, String unitaMisura,
            int mese, double potenza, String tensione, String annoRiferimento, String classeAgevolazione){

        String intervallo = mapIntervalloPotenza(potenza, tensione);
        if (intervallo == null || mese < 1 || mese > 12) return 0.0;

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

        // modality: annuale OR trimestrale OR mensile
        q.append("AND ((checkModality = 1 AND modality = 0) ")
                .append("OR (checkModality = 2 AND modality = :mtri) ")
                .append("OR (checkModality = 3 AND modality = :mmen)) ");
        p.put("mtri", modTri(mese));
        p.put("mmen", modMen(mese));

        List<dettaglioCosto> lista = find(q.toString(), p).list();
        return lista.stream().mapToDouble(dettaglioCosto::getCosto).sum();
    }

    // Corrispettivi DISPACCIAMENTO (€/kWh)
    public Double getCorrispettiviDispacciamentoA2A(String item, int mese, String anno, double potenza, String tensione) {
        return getCosto("dispacciamento", item, null, null,
                "€/kWh", mese, potenza, tensione, anno, null);
    }

    // TRASPORTI (var/fissa/potenza)
    public Double getCostiTrasporto(String item, int mese, double potenza, String tensione, String um, String anno) {
        return getCosto("trasporti", item, null, null,
                um, mese, potenza, tensione, anno, null);
    }

    // ONERI (richiede classe agevolazione)
    public Double getCostiOneri(String item,int mese, double potenza, String tensione, String um, String classe, String anno) {
        return getCosto("oneri", item, null, null,
                um, mese, potenza, tensione, anno, classe);
    }

    // Corrispettivi Peak e Off Peak (€/kWh)
    public Double getCostoPicco(int mese, double potenza, String tensione, String anno) {
        return getCosto("corrispettivi", null, "%Peak%", "%Off Peak%",
                "€/kWh", mese, potenza, tensione, anno, null);
    }
    public Double getCostoFuoriPicco(int mese, double potenza, String tensione, String anno) {
        return getCosto("corrispettivi", null, "%Off Peak%", null,
                "€/kWh", mese, potenza, tensione, anno, null);
    }

    // Penali cosphi (€/kVARh)
    public Double getPenaliSotto75(int mese, double potenza, String tensione, String anno, String classe) {
        return getCosto("penali cosphi", "Penali 33%", null, null,
                "€/kVARh", mese, potenza, tensione, anno, classe);
    }
    public Double getPenaliSopra75(int mese, double potenza, String tensione, String anno, String classe) {
        return getCosto("penali cosphi", "Penali 75%", null, null,
                "€/kVARh", mese, potenza, tensione, anno, classe);
    }


}
