package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.bolletta.CostoEnergia;
import miesgroup.mies.webdev.Repository.bolletta.CostoEnergiaRepo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class CostoEnergiaService {

    private final CostoEnergiaRepo costoEnergiaRepo;

    public CostoEnergiaService(CostoEnergiaRepo costoEnergiaRepo) {
        this.costoEnergiaRepo = costoEnergiaRepo;
    }

    @Transactional
    public Double verificaMateriaEnergia(Cliente cliente, String tipoCosto) {
        // Recupera tutti i costi per il cliente (eventualmente filtrare per periodo se necessario)
        List<CostoEnergia> costi = costoEnergiaRepo.find("cliente", cliente).list();

        // Costruisce una Map dove la chiave è il nome del costo e il valore è il costo in euro
        Map<String, Double> mappaCosti = costi.stream()
                .collect(Collectors.toMap(
                        CostoEnergia::getNomeCosto,
                        CostoEnergia::getCostoEuro,
                        (v1, v2) -> v1  // in caso di duplicati, prendi il primo
                ));

        // Restituisce il costo per il tipo specificato, oppure 0.0 se non presente
        Double costo = mappaCosti.getOrDefault(tipoCosto, 0.0);
        System.out.println("Costo per " + tipoCosto + ": " + costo);
        return costo;
    }

    @Transactional
    public List<CostoEnergia> getCostiEnergia(int idUtente) {
        return costoEnergiaRepo.find("cliente.id", idUtente).list();
    }

    @Transactional
    public void persistOrUpdateCostoEnergia(CostoEnergia costoEnergia) {
        // Cerca un record esistente per nomeCosto e cliente
        CostoEnergia existingCosto = costoEnergiaRepo.findByNomeCostoAndCliente(
                costoEnergia.getNomeCosto(), costoEnergia.getCliente());

        if (existingCosto != null) {
            // Se esiste, aggiorna il campo costoEuro tramite una query
            costoEnergiaRepo.updateCostoEnergia(existingCosto.getId(), costoEnergia.getCostoEuro());
        } else {
            // Altrimenti, persisti il nuovo record
            costoEnergiaRepo.persist(costoEnergia);
        }
    }



    @Transactional
    public void updateCostoEnergia(CostoEnergia costoEnergia) {
        Integer idCosto = costoEnergia.getId();
        CostoEnergia costo = costoEnergiaRepo.findById(idCosto);

        if (costo == null) {
            throw new IllegalArgumentException("Costo non trovato: " + idCosto);
        }

        costoEnergiaRepo.update("nomeCosto = ?1, costoEuro = ?2 where id = ?3",
                costoEnergia.getNomeCosto(), costoEnergia.getCostoEuro(), idCosto);


    }
}
