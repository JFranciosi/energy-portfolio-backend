package miesgroup.mies.webdev.Model.cliente;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "sessione") // Nome corretto della tabella nel database
public class Sessione extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment su MySQL
    @Column(name = "Id_Sessione")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_utente", nullable = false) // Foreign key verso `utente`
    private Cliente utente;

    @Column(name = "Data_Sessione", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dataSessione;

    // Costruttore vuoto richiesto da Hibernate
    public Sessione() {}

    // GETTER e SETTER
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

    public Timestamp getDataSessione() {
        return dataSessione;
    }

    public void setDataSessione(Timestamp dataSessione) {
        this.dataSessione = dataSessione;
    }
}
