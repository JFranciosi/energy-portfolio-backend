package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "dettaglioCosto") // Nome corretto della tabella
public class dettaglioCosto extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment su MySQL
    @Column(name = "Id") // Nome corretto della colonna ID
    private Integer id;

    @Column(name = "item", nullable = false)
    private String item;

    @Column(name = "unitaMisura", nullable = false)
    private String unitaMisura;

    @Column(name = "modality")
    private Integer modality;

    @Column(name = "checkModality")
    private Integer checkModality;

    @Column(name = "costo")
    private Double costo;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "intervalloPotenza")
    private String intervalloPotenza;

    @Column(name = "classeAgevolazione")
    private String classeAgevolazione;

    @Column(name = "annoRiferimento")
    private String annoRiferimento;

    @Column(name = "itemDescription")
    private String itemDescription;

    // Costruttore di default
    public dettaglioCosto() {
    }

    // GETTER e SETTER
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getUnitaMisura() {
        return unitaMisura;
    }

    public void setUnitaMisura(String unitaMisura) {
        this.unitaMisura = unitaMisura;
    }

    public Integer getModality() {
        return modality;
    }

    public void setModality(Integer modality) {
        this.modality = modality;
    }

    public Integer getCheckModality() {
        return checkModality;
    }

    public void setCheckModality(Integer checkModality) {
        this.checkModality = checkModality;
    }

    public Double getCosto() {
        return costo;
    }

    public void setCosto(Double costo) {
        this.costo = costo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getIntervalloPotenza() {
        return intervalloPotenza;
    }

    public void setIntervalloPotenza(String intervalloPotenza) {
        this.intervalloPotenza = intervalloPotenza;
    }

    public String getClasseAgevolazione() {
        return classeAgevolazione;
    }

    public void setClasseAgevolazione(String classeAgevolazione) {
        this.classeAgevolazione = classeAgevolazione;
    }

    public String getAnnoRiferimento() {
        return annoRiferimento;
    }

    public void setAnnoRiferimento(String annoRiferimento) {
        this.annoRiferimento = annoRiferimento;
    }

    public String getItem() {
        return item;
    }
    public void setItem(String item) {
        this.item = item;
    }
}
