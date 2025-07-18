package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "costo_articolo")
public class CostoArticolo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome_articolo", nullable = false)
    private String nomeArticolo;

    @Column(name = "costo_unitario", nullable = false)
    private Double costoUnitario;

    // Relazione ManyToOne: riferimento alla bolletta tramite la chiave primaria
    @ManyToOne
    @JoinColumn(name = "bolletta_id", nullable = false)
    private BollettaPod bolletta;

    @Column(name = "mese")
    private String mese;

    @Column(name = "categoria_articolo")
    private String categoriaArticolo;

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNomeArticolo() { return nomeArticolo; }
    public void setNomeArticolo(String nomeArticolo) { this.nomeArticolo = nomeArticolo; }

    public Double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(Double costoUnitario) { this.costoUnitario = costoUnitario; }

    public BollettaPod getBolletta() { return bolletta; }
    public void setBolletta(BollettaPod bolletta) { this.bolletta = bolletta; }

    public String getMese() { return mese; }
    public void setMese(String mese) { this.mese = mese; }

    public String getCategoriaArticolo() { return categoriaArticolo; }
    public void setCategoriaArticolo(String categoriaArticolo) { this.categoriaArticolo = categoriaArticolo; }
}
