package miesgroup.mies.webdev.Repository.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.CostoArticolo;

import java.util.List;

@ApplicationScoped
public class CostoArticoloRepo implements PanacheRepositoryBase<CostoArticolo, Integer> {


    public void aggiungiCostoArticolo(BollettaPod b, Double costoArticolo, String nomeArticolo, String categoriaArticolo) {
        CostoArticolo costo = new CostoArticolo();
        costo.setCostoUnitario(costoArticolo);
        costo.setNomeArticolo(nomeArticolo);
        costo.setMese(b.getMese());
        costo.setBolletta(b);  // Associa la bolletta tramite la relazione
        costo.setCategoriaArticolo(categoriaArticolo);
        costo.persist();
    }


    public List<CostoArticolo> getCostiArticoli(List<BollettaPod> bollettePods) {
        return list("bolletta in ?1", bollettePods);
    }

}
