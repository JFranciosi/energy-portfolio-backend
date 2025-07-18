package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.persistence.*;
import miesgroup.mies.webdev.Model.cliente.Cliente;

import java.time.LocalDate;


@Entity
@Table(name = "fixing")
public class Fixing extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_utente", nullable = false)
    private Cliente utente;

    @Column(name = "descrizione", nullable = false, length = 255)
    private String descrizione;

    @Column(name = "costo_euro", nullable = false)
    private Double costo;

    @Column(name = "unita_misura", nullable = false, length = 40)
    private String unitaMisura;

    @Column(name = "periodo_inizio", nullable = false)
    @Temporal(TemporalType.DATE)
    @JsonbDateFormat("yyyy-MM-dd")
    private LocalDate periodoInizio;

    @Column(name = "periodo_fine", nullable = false)
    @Temporal(TemporalType.DATE)
    @JsonbDateFormat("yyyy-MM-dd")
    private LocalDate periodoFine;

    // Costruttori
    public Fixing() {
    }

    public Fixing(Cliente utente, String descrizione, Double costo, String unitaMisura, LocalDate periodoInizio, LocalDate periodoFine) {
        this.utente = utente;
        this.descrizione = descrizione;
        this.costo = costo;
        this.unitaMisura = unitaMisura;
        this.periodoInizio = periodoInizio;
        this.periodoFine = periodoFine;
    }

    // Getter e Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Cliente getUtente() {
        return utente;
    }

    public void setUtente(Cliente utente) {
        this.utente = utente;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Double getCosto() {
        return costo;
    }

    public void setCosto(Double costo) {
        this.costo = costo;
    }

    public String getUnitaMisura() {
        return unitaMisura;
    }

    public void setUnitaMisura(String unitaMisura) {
        this.unitaMisura = unitaMisura;
    }

    public LocalDate getPeriodoInizio() {
        return periodoInizio;
    }

    public void setPeriodoInizio(LocalDate periodoInizio) {
        this.periodoInizio = periodoInizio;
    }

    public LocalDate getPeriodoFine() {
        return periodoFine;
    }

    public void setPeriodoFine(LocalDate periodoFine) {
        this.periodoFine = periodoFine;
    }

}
