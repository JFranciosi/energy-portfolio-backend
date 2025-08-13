package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.bolletta.dettaglioCostoRepo;
import miesgroup.mies.webdev.Service.cliente.ClienteService;
import java.util.List;

@ApplicationScoped
public class BollettaPodService {

    private final BollettaPodRepo bollettaPodRepo;
    private final dettaglioCostoRepo dettaglioCostoRepo;
    private final ClienteService clienteService;
    private final CostoEnergiaService costoEnergiaService;
    private final CostoArticoloService costoArticoloService;
    private final dettaglioCostoService costiService;


    public BollettaPodService(BollettaPodRepo bollettaRepo, ClienteService clienteService, CostoEnergiaService costoEnergiaService, CostoArticoloService costoArticoloService, dettaglioCostoService costiService, dettaglioCostoRepo dettaglioCostoRepo) {
        this.bollettaPodRepo = bollettaRepo;
        this.clienteService = clienteService;
        this.costoEnergiaService = costoEnergiaService;
        this.costoArticoloService = costoArticoloService;
        this.costiService = costiService;
        this.dettaglioCostoRepo = dettaglioCostoRepo;
    }

    @Transactional
    public boolean A2AisPresent(String nomeBolletta, String idPod) {
        return bollettaPodRepo.A2AisPresent(nomeBolletta, idPod);
    }

    public List<BollettaPod> findBollettaPodByPods(List<Pod> pods) {
        return bollettaPodRepo.findBollettaPodByPods(pods);
    }
}