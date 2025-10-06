package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "bolletta_voce")
public class BollettaVoce extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_Voce")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Id_Bolletta", nullable = false)
    private BollettaPod bolletta;

    /* ============== CAMPI VOCI (allineati all’Excel "Dati") ============== */

    @Column(name = "Cod")
    private String cod;

    @Column(name = "Descrizione", columnDefinition = "TEXT")
    private String descrizione;

    @Column(name = "Unita_Misura")
    private String unitaMisura;

    @Column(name = "Quantita", precision = 18, scale = 6)
    private BigDecimal quantita;

    @Column(name = "Corrispettivo_Unitario", precision = 18, scale = 6)
    private BigDecimal corrispettivoUnitario;

    @Column(name = "Totale_Voce", precision = 18, scale = 6)
    private BigDecimal totaleVoce;

    // Può essere percentuale (es. 10) o valore monetario (dipende dal formato sorgente)
    @Column(name = "IVA", precision = 18, scale = 6)
    private BigDecimal iva;

    @Column(name = "Parziali", precision = 18, scale = 6)
    private BigDecimal parziali;

    @Column(name = "Totale2", precision = 18, scale = 6)
    private BigDecimal totale2;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    // Nel tracciato Excel compare “COMPETENZA”: lo tratto come testo generico.
    @Column(name = "Competenza")
    private String competenza;

    /* ================== GETTER/SETTER ================== */

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public BollettaPod getBolletta() { return bolletta; }
    public void setBolletta(BollettaPod bolletta) { this.bolletta = bolletta; }

    public String getCod() { return cod; }
    public void setCod(String cod) { this.cod = cod; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public String getUnitaMisura() { return unitaMisura; }
    public void setUnitaMisura(String unitaMisura) { this.unitaMisura = unitaMisura; }

    public BigDecimal getQuantita() { return quantita; }
    public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }

    public BigDecimal getCorrispettivoUnitario() { return corrispettivoUnitario; }
    public void setCorrispettivoUnitario(BigDecimal corrispettivoUnitario) { this.corrispettivoUnitario = corrispettivoUnitario; }

    public BigDecimal getTotaleVoce() { return totaleVoce; }
    public void setTotaleVoce(BigDecimal totaleVoce) { this.totaleVoce = totaleVoce; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public BigDecimal getParziali() { return parziali; }
    public void setParziali(BigDecimal parziali) { this.parziali = parziali; }

    public BigDecimal getTotale2() { return totale2; }
    public void setTotale2(BigDecimal totale2) { this.totale2 = totale2; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCompetenza() { return competenza; }
    public void setCompetenza(String competenza) { this.competenza = competenza; }

    /* ============== EQUALS/HASHCODE PER ID ============== */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BollettaVoce)) return false;
        BollettaVoce that = (BollettaVoce) o;
        return Objects.equals(id, that.id);
    }
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
