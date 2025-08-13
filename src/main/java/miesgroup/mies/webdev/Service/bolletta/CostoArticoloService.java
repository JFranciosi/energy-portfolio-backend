package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.CostoArticolo;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Model.bolletta.dettaglioCosto;
import miesgroup.mies.webdev.Repository.bolletta.CostoArticoloRepo;
import miesgroup.mies.webdev.Rest.Model.CostoArticoloResponse;
import miesgroup.mies.webdev.Service.cliente.ClienteService;
import miesgroup.mies.webdev.Service.cliente.SessionService;
import org.hibernate.SessionException;

import java.util.List;

@ApplicationScoped
public class CostoArticoloService {

    private final CostoArticoloRepo costoArticoloRepo;
    private final PodService podService;
    private final BollettaPodService bolletteService;

    public CostoArticoloService(CostoArticoloRepo costoArticoloRepo, SessionService sessionService, ClienteService clienteService, PodService podService, BollettaPodService bolletteService) {
        this.costoArticoloRepo = costoArticoloRepo;
        this.podService = podService;
        this.bolletteService = bolletteService;
    }


    public void calcolaCostiArticoli(List<dettaglioCosto> articoli, BollettaPod b, Double maggiorePotenza, String categoriaArticolo) {
        // Calcola i costi degli articoli
        articoli.forEach(articolo -> {
            Double costoArticolo = switch (articolo.getUnitaMisura()) {

                case "€/KWh" ->
                    // Calcola il costo dell'articolo in base al consumo e al costo unitario
                        articolo.getCosto() * b.getTotAtt();
                case "€/KW/Month" ->
                    // Calcola il costo dell'articolo in base al consumo e al costo unitario
                        articolo.getCosto() * maggiorePotenza;
                case "€/Month" ->
                    // Calcola il costo dell'articolo in base al costo unitario
                        articolo.getCosto();
                default -> 0.0;
                // Calcola il costo dell'articolo
            };
            // Salva il costo dell'articolo
            costoArticoloRepo.aggiungiCostoArticolo(b, costoArticolo, articolo.getItem(), categoriaArticolo);
        });
    }

    public List<CostoArticoloResponse> getCostoArticoli(Integer idSessione) {
        // 1) Verifica la sessione
        if (idSessione == null) {
            throw new SessionException("Sessione non valida");
        }

        // 2) Recupera i Pod
        List<Pod> pods = podService.findPodByIdUser(idSessione);
        // Se la lista è nulla o vuota, lanciamo un'eccezione
        if (pods == null || pods.isEmpty()) {
            throw new NotFoundException("Nessun POD trovato");
        }

        // 3) Recupera le Bollette associate ai Pod
        List<BollettaPod> bollettePods = bolletteService.findBollettaPodByPods(pods);
        // Se la lista è nulla o vuota, lanciamo un'eccezione
        if (bollettePods == null || bollettePods.isEmpty()) {
            throw new NotFoundException("Nessuna bolletta trovata");
        }

        // 4) Recupera i costi articoli associati alle bollette
        List<CostoArticolo> costoArticoli = costoArticoloRepo.getCostiArticoli(bollettePods);
        return costoArticoli.stream()
                .map(CostoArticoloResponse::new)
                .toList();
    }

}