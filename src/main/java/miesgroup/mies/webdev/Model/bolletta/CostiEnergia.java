package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "costi_energia")
public class CostiEnergia extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToMany(mappedBy = "costiEnergia", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<CostiPeriodi> costiPeriodi = new ArrayList<>();

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "tipo_prezzo", nullable = false, length = 20)
    private String tipoPrezzo;

    @Column(name = "tipo_tariffa", length = 20)
    private String tipoTariffa;

    // Usa BigDecimal per valori monetari con scala fissa
    @Column(name = "percentage_variable", precision = 5, scale = 2)
    private BigDecimal percentageVariable;

    @Column(name = "cost_f0", precision = 15, scale = 8)
    private BigDecimal costF0;

    @Column(name = "cost_f1", precision = 15, scale = 8)
    private BigDecimal costF1;

    @Column(name = "cost_f2", precision = 15, scale = 8)
    private BigDecimal costF2;

    @Column(name = "cost_f3", precision = 15, scale = 8)
    private BigDecimal costF3;

    @Column(name = "spread_f1", precision = 15, scale = 8)
    private BigDecimal spreadF1;

    @Column(name = "spread_f2", precision = 15, scale = 8)
    private BigDecimal spreadF2;

    @Column(name = "spread_f3", precision = 15, scale = 8)
    private BigDecimal spreadF3;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // Aggiorna i getter e setter per BigDecimal
    public BigDecimal getPercentageVariable() { return percentageVariable; }
    public void setPercentageVariable(BigDecimal percentageVariable) { this.percentageVariable = percentageVariable; }

    public BigDecimal getCostF0() { return costF0; }
    public void setCostF0(BigDecimal costF0) { this.costF0 = costF0; }

    public BigDecimal getCostF1() { return costF1; }
    public void setCostF1(BigDecimal costF1) { this.costF1 = costF1; }

    public BigDecimal getCostF2() { return costF2; }
    public void setCostF2(BigDecimal costF2) { this.costF2 = costF2; }

    public BigDecimal getCostF3() { return costF3; }
    public void setCostF3(BigDecimal costF3) { this.costF3 = costF3; }

    public BigDecimal getSpreadF1() { return spreadF1; }
    public void setSpreadF1(BigDecimal spreadF1) { this.spreadF1 = spreadF1; }

    public BigDecimal getSpreadF2() { return spreadF2; }
    public void setSpreadF2(BigDecimal spreadF2) { this.spreadF2 = spreadF2; }

    public BigDecimal getSpreadF3() { return spreadF3; }
    public void setSpreadF3(BigDecimal spreadF3) { this.spreadF3 = spreadF3; }

    // Mantieni gli altri getter/setter invariati
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public List<CostiPeriodi> getCostiPeriodi() { return costiPeriodi; }
    public void setCostiPeriodi(List<CostiPeriodi> costiPeriodi) { this.costiPeriodi = costiPeriodi; }

    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getTipoPrezzo() { return tipoPrezzo; }
    public void setTipoPrezzo(String tipoPrezzo) { this.tipoPrezzo = tipoPrezzo; }

    public String getTipoTariffa() { return tipoTariffa; }
    public void setTipoTariffa(String tipoTariffa) { this.tipoTariffa = tipoTariffa; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
