package miesgroup.mies.webdev.Model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "budget_all",
        uniqueConstraints = @UniqueConstraint(name = "uk_budgetall_mese",
                columnNames = {"id_pod","anno","mese"}))
public class BudgetAll implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_pod", nullable = false, length = 255)
    private String idPod;

    @Column(nullable = false)
    private Integer anno;

    @Column(nullable = false)
    private Integer mese;

    @Column(name = "prezzo_energia_base", nullable = false, precision = 10, scale = 4)
    private BigDecimal prezzoEnergiaBase;

    @Column(name = "consumi_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal consumiBase;

    @Column(name = "oneri_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal oneriBase;

    @Column(name = "prezzo_energia_perc", nullable = false, precision = 5, scale = 2)
    private BigDecimal prezzoEnergiaPerc;

    @Column(name = "consumi_perc", nullable = false, precision = 5, scale = 2)
    private BigDecimal consumiPerc;

    @Column(name = "oneri_perc", nullable = false, precision = 5, scale = 2)
    private BigDecimal oneriPerc;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utente", referencedColumnName = "Id_Utente")
    private Cliente cliente;

    // --- getters & setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdPod() { return idPod; }
    public void setIdPod(String idPod) { this.idPod = idPod; }

    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }

    public Integer getMese() { return mese; }
    public void setMese(Integer mese) { this.mese = mese; }

    public BigDecimal getPrezzoEnergiaBase() { return prezzoEnergiaBase; }
    public void setPrezzoEnergiaBase(BigDecimal prezzoEnergiaBase) { this.prezzoEnergiaBase = prezzoEnergiaBase; }

    public BigDecimal getConsumiBase() { return consumiBase; }
    public void setConsumiBase(BigDecimal consumiBase) { this.consumiBase = consumiBase; }

    public BigDecimal getOneriBase() { return oneriBase; }
    public void setOneriBase(BigDecimal oneriBase) { this.oneriBase = oneriBase; }

    public BigDecimal getPrezzoEnergiaPerc() { return prezzoEnergiaPerc; }
    public void setPrezzoEnergiaPerc(BigDecimal prezzoEnergiaPerc) { this.prezzoEnergiaPerc = prezzoEnergiaPerc; }

    public BigDecimal getConsumiPerc() { return consumiPerc; }
    public void setConsumiPerc(BigDecimal consumiPerc) { this.consumiPerc = consumiPerc; }

    public BigDecimal getOneriPerc() { return oneriPerc; }
    public void setOneriPerc(BigDecimal oneriPerc) { this.oneriPerc = oneriPerc; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}
