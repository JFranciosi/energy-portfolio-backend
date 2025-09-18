package miesgroup.mies.webdev.Repository.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import miesgroup.mies.webdev.Model.bolletta.CostiEnergia;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CostoEnergiaRepo implements PanacheRepositoryBase<CostiEnergia, Integer> {

    @PersistenceContext
    EntityManager em;

    private final CostiPeriodiRepo costiPeriodiRepo;

    public CostoEnergiaRepo(CostiPeriodiRepo costiPeriodiRepo) {
        this.costiPeriodiRepo = costiPeriodiRepo;
    }

    /* ===== Base finders ===== */

    public Optional<CostiEnergia> findByClientIdAndYear(Integer clientId, Integer year) {
        return find("clientId = ?1 AND year = ?2", clientId, year).firstResultOptional();
    }

    public List<CostiEnergia> findByClientId(Integer clientId) {
        return list("clientId", clientId);
    }

    public List<CostiEnergia> findByYear(Integer year) {
        return list("year", year);
    }

    public List<CostiEnergia> findByTipoPrezzo(String tipoPrezzo) {
        return list("tipoPrezzo", tipoPrezzo);
    }

    public boolean existsByClientIdAndYear(Integer clientId, Integer year) {
        return count("clientId = ?1 AND year = ?2", clientId, year) > 0;
    }

    /* ===== Letture utili ===== */

    public BigDecimal getCostF1(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null ? Optional.ofNullable(c.getCostF1()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal getCostF2(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null ? Optional.ofNullable(c.getCostF2()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal getCostF3(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null ? Optional.ofNullable(c.getCostF3()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal getSpreadF1(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null ? Optional.ofNullable(c.getSpreadF1()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal getSpreadF2(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null ? Optional.ofNullable(c.getSpreadF2()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal getSpreadF3(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null ? Optional.ofNullable(c.getSpreadF3()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public String getTipoTariffa(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null ? c.getTipoTariffa() : null;
    }

    public BigDecimal getPercentageVariable(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null ? Optional.ofNullable(c.getPercentageVariable()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }


    /* ===== Update mirati ===== */

    public void updateCostiByClientAndYear(Double costF1, Double costF2, Double costF3,
                                           Integer clientId, Integer year) {
        update("costF1 = ?1, costF2 = ?2, costF3 = ?3 WHERE clientId = ?4 AND year = ?5",
                costF1, costF2, costF3, clientId, year);
    }

    public void updateSpreadByClientAndYear(Double spreadF1, Double spreadF2, Double spreadF3,
                                            Integer clientId, Integer year) {
        update("spreadF1 = ?1, spreadF2 = ?2, spreadF3 = ?3 WHERE clientId = ?4 AND year = ?5",
                spreadF1, spreadF2, spreadF3, clientId, year);
    }

    public void updateTipoTariffa(String tipoTariffa, Integer clientId, Integer year) {
        update("tipoTariffa = ?1 WHERE clientId = ?2 AND year = ?3",
                tipoTariffa, clientId, year);
    }

    public void updatePercentageVariable(Double percentageVariable, Integer clientId, Integer year) {
        update("percentageVariable = ?1 WHERE clientId = ?2 AND year = ?3",
                percentageVariable, clientId, year);
    }

    /* ===== Query complesse ===== */

    public List<CostiEnergia> getCostiEnergiaByClientAndYearRange(Integer clientId, Integer yearStart, Integer yearEnd) {
        return list("clientId = ?1 AND year >= ?2 AND year <= ?3", clientId, yearStart, yearEnd);
    }

    public List<CostiEnergia> getCostiEnergiaByTipoPrezzoAndYear(String tipoPrezzo, Integer year) {
        return list("tipoPrezzo = ?1 AND year = ?2", tipoPrezzo, year);
    }

    /* ===== Aggregazioni ===== */

    public Double getMediaCostiF1ByYear(Integer year) {
        Double result = find("SELECT AVG(c.costF1) FROM CostiEnergia c WHERE c.year = ?1 AND c.costF1 IS NOT NULL", year)
                .project(Double.class).firstResult();
        return result != null ? result : 0.0;
    }

    public Long countByTipoPrezzo(String tipoPrezzo) {
        return count("tipoPrezzo", tipoPrezzo);
    }

    /* ===== Validazioni ===== */

    public boolean isDynamicPricing(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null && "dinamico".equals(c.getTipoPrezzo());
    }

    public boolean hasSpreadValues(Integer clientId, Integer year) {
        CostiEnergia c = find("clientId = ?1 AND year = ?2", clientId, year).firstResult();
        return c != null && "indicizzato".equals(c.getTipoPrezzo()) &&
                (c.getSpreadF1() != null || c.getSpreadF2() != null || c.getSpreadF3() != null);
    }
}

