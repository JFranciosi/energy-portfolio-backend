package miesgroup.mies.webdev.Repository.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import miesgroup.mies.webdev.Model.bolletta.CostiPeriodi;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CostiPeriodiRepo implements PanacheRepositoryBase<CostiPeriodi, Integer> {

    @PersistenceContext
    EntityManager em;

    /* ===== Base finders ===== */

    public List<CostiPeriodi> findByEnergyCostId(Integer energyCostId) {
        return list("energyCostId", energyCostId);
    }

    public Optional<CostiPeriodi> findByEnergyCostIdAndMonth(Integer energyCostId, Integer monthStart) {
        return find("energyCostId = ?1 AND monthStart = ?2", energyCostId, monthStart).firstResultOptional();
    }

    public List<CostiPeriodi> findByMonthStart(Integer monthStart) {
        return list("monthStart", monthStart);
    }

    public boolean existsByEnergyCostIdAndMonth(Integer energyCostId, Integer monthStart) {
        return count("energyCostId = ?1 AND monthStart = ?2", energyCostId, monthStart) > 0;
    }

    /* ===== Letture utili ===== */

    public BigDecimal getCostF1ByPeriod(Integer energyCostId, Integer monthStart) {
        CostiPeriodi c = find("energyCostId = ?1 AND monthStart = ?2", energyCostId, monthStart).firstResult();
        return c != null ? Optional.ofNullable(c.getCostF1()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal getCostF2ByPeriod(Integer energyCostId, Integer monthStart) {
        CostiPeriodi c = find("energyCostId = ?1 AND monthStart = ?2", energyCostId, monthStart).firstResult();
        return c != null ? Optional.ofNullable(c.getCostF2()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public BigDecimal getCostF3ByPeriod(Integer energyCostId, Integer monthStart) {
        CostiPeriodi c = find("energyCostId = ?1 AND monthStart = ?2", energyCostId, monthStart).firstResult();
        return c != null ? Optional.ofNullable(c.getCostF3()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
    }


    public List<CostiPeriodi> getPeriodiOrderedByMonth(Integer energyCostId) {
        return list("energyCostId = ?1 ORDER BY monthStart ASC", energyCostId);
    }

    /* ===== Update mirati ===== */

    public void updateCostiByPeriod(Double costF1, Double costF2, Double costF3,
                                    Integer energyCostId, Integer monthStart) {
        update("costF1 = ?1, costF2 = ?2, costF3 = ?3 WHERE energyCostId = ?4 AND monthStart = ?5",
                costF1, costF2, costF3, energyCostId, monthStart);
    }

    public void updateCostF1(Double costF1, Integer energyCostId, Integer monthStart) {
        update("costF1 = ?1 WHERE energyCostId = ?2 AND monthStart = ?3",
                costF1, energyCostId, monthStart);
    }

    public void updateCostF2(Double costF2, Integer energyCostId, Integer monthStart) {
        update("costF2 = ?1 WHERE energyCostId = ?2 AND monthStart = ?3",
                costF2, energyCostId, monthStart);
    }

    public void updateCostF3(Double costF3, Integer energyCostId, Integer monthStart) {
        update("costF3 = ?1 WHERE energyCostId = ?2 AND monthStart = ?3",
                costF3, energyCostId, monthStart);
    }

    /* ===== Operazioni batch ===== */

    public void deleteAllByEnergyCostId(Integer energyCostId) {
        delete("energyCostId", energyCostId);
    }

    public void savePeriodsBatch(List<CostiPeriodi> periodi) {
        persist(periodi);
    }

    /* ===== Query complesse ===== */

    public List<CostiPeriodi> getPeriodiByMonthRange(Integer energyCostId, Integer monthStart, Integer monthEnd) {
        return list("energyCostId = ?1 AND monthStart >= ?2 AND monthStart <= ?3",
                energyCostId, monthStart, monthEnd);
    }

    public List<CostiPeriodi> getPeriodiWithHighCosts(Double threshold) {
        return list("(costF1 > ?1 OR costF2 > ?1 OR costF3 > ?1)", threshold);
    }

    /* ===== Aggregazioni ===== */

    public Double getMediaCostF1ByEnergyCostId(Integer energyCostId) {
        Double result = find("SELECT AVG(c.costF1) FROM CostiPeriodi c WHERE c.energyCostId = ?1", energyCostId)
                .project(Double.class).firstResult();
        return result != null ? result : 0.0;
    }

    public Double getMaxCostF1ByEnergyCostId(Integer energyCostId) {
        Double result = find("SELECT MAX(c.costF1) FROM CostiPeriodi c WHERE c.energyCostId = ?1", energyCostId)
                .project(Double.class).firstResult();
        return result != null ? result : 0.0;
    }

    public Double getMinCostF1ByEnergyCostId(Integer energyCostId) {
        Double result = find("SELECT MIN(c.costF1) FROM CostiPeriodi c WHERE c.energyCostId = ?1", energyCostId)
                .project(Double.class).firstResult();
        return result != null ? result : 0.0;
    }

    public Long countPeriodiByEnergyCostId(Integer energyCostId) {
        return count("energyCostId", energyCostId);
    }

    /* ===== Utilità per periodi dinamici ===== */

    public Integer getLastMonthForEnergyCost(Integer energyCostId) {
        CostiPeriodi result = find("energyCostId = ?1 ORDER BY monthStart DESC", energyCostId).firstResult();
        return result != null ? result.getMonthStart() : null;
    }

    public Integer getFirstMonthForEnergyCost(Integer energyCostId) {
        CostiPeriodi result = find("energyCostId = ?1 ORDER BY monthStart ASC", energyCostId).firstResult();
        return result != null ? result.getMonthStart() : null;
    }

    public List<Integer> getAvailableMonthsForEnergyCost(Integer energyCostId) {
        return find("SELECT DISTINCT c.monthStart FROM CostiPeriodi c WHERE c.energyCostId = ?1 ORDER BY c.monthStart", energyCostId)
                .project(Integer.class).list();
    }

    /* ===== Validazioni ===== */

    public boolean hasOverlappingPeriods(Integer energyCostId) {
        Long count = find("SELECT COUNT(DISTINCT c.monthStart) FROM CostiPeriodi c WHERE c.energyCostId = ?1", energyCostId)
                .project(Long.class).firstResult();
        Long totalCount = count("energyCostId", energyCostId);
        return !count.equals(totalCount);
    }

    public boolean isValidPeriodSequence(Integer energyCostId) {
        List<Integer> months = getAvailableMonthsForEnergyCost(energyCostId);
        if (months.isEmpty()) return true;

        for (int i = 0; i < months.size() - 1; i++) {
            if (months.get(i + 1) - months.get(i) != 1 &&
                    !(months.get(i) == 12 && months.get(i + 1) == 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Trova il periodo attivo per un energy_cost_id e mese specifico
     */
    public Optional<CostiPeriodi> findActiveByEnergyIdAndMonth(Integer energyCostId, Integer mese) {
        // Trova il periodo con month_start <= mese, ordinato per month_start DESC (il più recente)
        return find("energyCostId = ?1 AND monthStart <= ?2 ORDER BY monthStart DESC",
                energyCostId, mese).firstResultOptional();
    }

    public long deleteByEnergyCostIdAndMonth(Integer energyCostId, Integer monthStart) {
        return delete("energyCostId = ?1 AND monthStart = ?2", energyCostId, monthStart);
    }
}

