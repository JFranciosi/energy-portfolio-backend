package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "pod") // Nome corretto della tabella nel database
public class Pod extends PanacheEntityBase {

    @Id
    @Column(name = "Id_Pod", length = 255) // Nome corretto della colonna
    private String id;

    @Column(name = "Tensione_Alimentazione", nullable = false)
    private Double tensioneAlimentazione;

    @Column(name = "Potenza_Impegnata", nullable = false)
    private Double potenzaImpegnata;

    @Column(name = "Potenza_Disponibile", nullable = false)
    private Double potenzaDisponibile;

    @ManyToOne
    @JoinColumn(name = "id_utente", nullable = false) // Foreign key verso `utente`
    private Cliente utente;

    @Column(name = "sede")
    private String sede;

    @Column(name = "citta")
    private String nazione;

    @Column(name = "cap")
    private String cap;

    @Column(name = "Tipo_Tensione", nullable = false)
    private String tipoTensione;

    @Column(name = "fornitore")
    private String fornitore;

    @Column(name= "spread")
    @ColumnDefault("0.0")
    private Double spread;

    // GETTER e SETTER

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getTensioneAlimentazione() {
        return tensioneAlimentazione;
    }

    public void setTensioneAlimentazione(double tensioneAlimentazione) {
        this.tensioneAlimentazione = tensioneAlimentazione;
    }

    public double getPotenzaImpegnata() {
        return potenzaImpegnata;
    }

    public void setPotenzaImpegnata(double potenzaImpegnata) {
        this.potenzaImpegnata = potenzaImpegnata;
    }

    public double getPotenzaDisponibile() {
        return potenzaDisponibile;
    }

    public void setPotenzaDisponibile(double potenzaDisponibile) {
        this.potenzaDisponibile = potenzaDisponibile;
    }

    public Cliente getUtente() {
        return utente;
    }

    public void setUtente(Cliente utente) {
        this.utente = utente;
    }

    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }

    public String getNazione() {
        return nazione;
    }

    public void setNazione(String nazione) {
        this.nazione = nazione;
    }

    public String getTipoTensione() {
        return tipoTensione;
    }

    public void setTipoTensione(String tipoTensione) {
        this.tipoTensione = tipoTensione;
    }

    public String getFornitore() {
        return fornitore;
    }

    public void setFornitore(String fornitore) {
        this.fornitore = fornitore;
    }

    public Double getSpread() {
        return spread;
    }

    public void setSpread(Double spread) {
        this.spread = spread;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

}
