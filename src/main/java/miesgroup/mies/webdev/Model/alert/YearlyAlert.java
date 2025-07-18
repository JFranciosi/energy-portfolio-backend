package miesgroup.mies.webdev.Model.alert;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import miesgroup.mies.webdev.Model.cliente.Cliente;

@Entity
@Table(name = "YearlyAlert")
public class YearlyAlert extends PanacheEntityBase {

    @Id
    @Column(name = "Id_Utente")
    private Integer idUtente; // usato come PK

    private Double maxPriceValue;
    private Double minPriceValue;
    private Boolean checkModality;

    @ManyToOne
    @JoinColumn(name = "Id_Utente", insertable = false, updatable = false)
    private Cliente utente;

    public YearlyAlert() {}

    public YearlyAlert(Double maxPriceValue, Double minPriceValue, Integer idUtente, Boolean checkModality) {
        this.maxPriceValue = maxPriceValue;
        this.minPriceValue = minPriceValue;
        this.idUtente = idUtente;
        this.checkModality = checkModality;
    }

    // Getter e Setter

    public Double getMaxPriceValue(){
        return maxPriceValue;
    }

    public void setMaxPriceValue(Double maxPriceValue){
        this.maxPriceValue = maxPriceValue;
    }

    public Double getMinPriceValue(){
        return minPriceValue;
    }

    public void setMinPriceValue(Double minPriceValue){
        this.minPriceValue = minPriceValue;
    }

    public Integer getIdUtente(){
        return idUtente;
    }

    public void setIdUtente(Integer idUtente){
        this.idUtente = idUtente;
    }

    public Boolean getCheckModality(){
        return checkModality;
    }

    public void setCheckModality(Boolean checkModality){
        this.checkModality = checkModality;
    }

    public Cliente getUtente(){
        return utente;
    }

    public void setUtente(Cliente utente){
        this.utente = utente;
    }
}
