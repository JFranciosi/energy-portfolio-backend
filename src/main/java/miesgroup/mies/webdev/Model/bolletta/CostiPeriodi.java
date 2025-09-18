package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "costi_periodi")
public class CostiPeriodi extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "energy_cost_id", nullable = false)
    @JsonbTransient
    private CostiEnergia costiEnergia;

    @Column(name = "energy_cost_id", nullable = false, insertable = false, updatable = false)
    private Integer energyCostId;

    @Column(name = "month_start", nullable = false)
    private Integer monthStart;

    // Usa BigDecimal per valori monetari con scala fissa
    @Column(name = "cost_f1", nullable = false, precision = 15, scale = 8)
    private BigDecimal costF1;

    @Column(name = "cost_f2", nullable = false, precision = 15, scale = 8)
    private BigDecimal costF2;

    @Column(name = "cost_f3", nullable = false, precision = 15, scale = 8)
    private BigDecimal costF3;

    @Column(name = "percentage_variable", precision = 5, scale = 2)
    private BigDecimal percentageVariable;

    @Column(name = "created_at")
    private Timestamp createdAt;

    // Aggiorna i getter e setter per BigDecimal
    public BigDecimal getCostF1() { return costF1; }
    public void setCostF1(BigDecimal costF1) { this.costF1 = costF1; }

    public BigDecimal getCostF2() { return costF2; }
    public void setCostF2(BigDecimal costF2) { this.costF2 = costF2; }

    public BigDecimal getCostF3() { return costF3; }
    public void setCostF3(BigDecimal costF3) { this.costF3 = costF3; }

    // Mantieni gli altri getter/setter invariati
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public CostiEnergia getCostiEnergia() { return costiEnergia; }
    public void setCostiEnergia(CostiEnergia costiEnergia) { this.costiEnergia = costiEnergia; }

    public Integer getEnergyCostId() { return energyCostId; }
    public void setEnergyCostId(Integer energyCostId) { this.energyCostId = energyCostId; }

    public Integer getMonthStart() { return monthStart; }

    public void setMonthStart(Integer monthStart) { this.monthStart = monthStart; }

    public BigDecimal getPercentageVariable() {
        return percentageVariable;
    }

    public void setPercentageVariable(BigDecimal percentageVariable) {
        this.percentageVariable = percentageVariable;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
