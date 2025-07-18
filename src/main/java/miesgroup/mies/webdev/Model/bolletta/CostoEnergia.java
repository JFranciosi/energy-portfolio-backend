package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import miesgroup.mies.webdev.Model.cliente.Cliente;

@Entity
@Table(name = "costi_energia")
public class CostoEnergia extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relazione con il Cliente: ogni costo è associato a un solo cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Nome dinamico del costo, ad esempio "f0", "f1", "f1_perdite", ecc.
    @Column(name = "nome_costo", nullable = false)
    private String nomeCosto;

    // Valore in euro del costo
    @Column(name = "costo_euro", nullable = false)
    private Double costoEuro;

    public CostoEnergia() {
        // Costruttore no-arg richiesto da JPA
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getNomeCosto() {
        return nomeCosto;
    }

    public void setNomeCosto(String nomeCosto) {
        this.nomeCosto = nomeCosto;
    }

    public Double getCostoEuro() {
        return costoEuro;
    }

    public void setCostoEuro(Double costoEuro) {
        this.costoEuro = costoEuro;
    }
}