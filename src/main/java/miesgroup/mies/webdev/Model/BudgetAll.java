package miesgroup.mies.webdev.Model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "budget_all",
        uniqueConstraints = @UniqueConstraint(name = "uk_budgetall_mese",
                columnNames = {"id_pod","anno","mese"}))
public class BudgetAll implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_pod", nullable = false, length = 14)
    private String idPod;

    @Column(nullable = false)
    private Integer anno;

    @Column(nullable = false)
    private Integer mese;

    // Usando Double, precision/scale non servono più:
    @Column(name = "prezzo_energia_base", nullable = false)
    private Double prezzoEnergiaBase;

    @Column(name = "consumi_base", nullable = false)
    private Double consumiBase;

    @Column(name = "oneri_base", nullable = false)
    private Double oneriBase;

    @Column(name = "prezzo_energia_perc", nullable = false)
    private Double prezzoEnergiaPerc;

    @Column(name = "consumi_perc", nullable = false)
    private Double consumiPerc;

    @Column(name = "oneri_perc", nullable = false)
    private Double oneriPerc;

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

    public Double getPrezzoEnergiaBase() { return prezzoEnergiaBase; }
    public void setPrezzoEnergiaBase(Double prezzoEnergiaBase) { this.prezzoEnergiaBase = prezzoEnergiaBase; }

    public Double getConsumiBase() { return consumiBase; }
    public void setConsumiBase(Double consumiBase) { this.consumiBase = consumiBase; }

    public Double getOneriBase() { return oneriBase; }
    public void setOneriBase(Double oneriBase) { this.oneriBase = oneriBase; }

    public Double getPrezzoEnergiaPerc() { return prezzoEnergiaPerc; }
    public void setPrezzoEnergiaPerc(Double prezzoEnergiaPerc) { this.prezzoEnergiaPerc = prezzoEnergiaPerc; }

    public Double getConsumiPerc() { return consumiPerc; }
    public void setConsumiPerc(Double consumiPerc) { this.consumiPerc = consumiPerc; }

    public Double getOneriPerc() { return oneriPerc; }
    public void setOneriPerc(Double oneriPerc) { this.oneriPerc = oneriPerc; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}
