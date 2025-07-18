package miesgroup.mies.webdev.Repository.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.bolletta.CostoEnergia;

@ApplicationScoped
public class CostoEnergiaRepo implements PanacheRepositoryBase<CostoEnergia, Integer> {

    private final EntityManager entityManager;

    public CostoEnergiaRepo(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public CostoEnergia findByNomeCostoAndCliente(String nomeCosto, Cliente cliente) {
        return find("nomeCosto = ?1 and cliente = ?2", nomeCosto, cliente).firstResult();
    }

    public int updateCostoEnergia(Integer id, Double costoEuro) {
        return entityManager.createQuery(
                        "UPDATE CostoEnergia c SET c.costoEuro = :costoEuro WHERE c.id = :id")
                .setParameter("costoEuro", costoEuro)
                .setParameter("id", id)
                .executeUpdate();
    }

}
