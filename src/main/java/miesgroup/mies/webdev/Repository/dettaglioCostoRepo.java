package miesgroup.mies.webdev.Repository;


import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.dettaglioCosto;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class dettaglioCostoRepo implements PanacheRepositoryBase<dettaglioCosto, Integer> {
    private final DataSource dataSource;

    public dettaglioCostoRepo(DataSource dataSource) {
        this.dataSource = dataSource;
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

}
