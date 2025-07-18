package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.persistence.PersistenceException;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.bolletta.Fixing;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;
import miesgroup.mies.webdev.Repository.bolletta.FixingRepo;
import miesgroup.mies.webdev.Rest.Model.FixingResponse;
import miesgroup.mies.webdev.Service.cliente.SessionService;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class FixingService {

    private final SessionService sessionService;
    private final ClienteRepo clienteRepo;
    private final FixingRepo fixingRepo;

    public FixingService(SessionService sessionService, ClienteRepo clienteRepo, FixingRepo fixingRepo) {
        this.sessionService = sessionService;
        this.clienteRepo = clienteRepo;
        this.fixingRepo = fixingRepo;
    }

    public List<FixingResponse> getFixing(Integer sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID non può essere null");
        }

        try {
            Integer idC = sessionService.trovaUtentebBySessione(sessionId);
            if (idC == null) {
                throw new IllegalArgumentException("Nessun utente trovato per sessionId: " + sessionId);
            }

            List<Fixing> fixingList = fixingRepo.getFixing(idC);
            if (fixingList == null || fixingList.isEmpty()) {
                throw new IllegalArgumentException("Fixing non trovati per sessionId: " + sessionId);
            }

            return fixingList.stream()
                    .map(f -> new FixingResponse(f.getId(), f.getDescrizione(), f.getCosto(), f.getUnitaMisura(), f.getPeriodoInizio(), f.getPeriodoFine()))
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Errore nel recupero dei Fixing per sessionId: " + sessionId, e);
        }
    }


    @Transactional
    public void addFixing(Integer sessionId, List<Fixing> fixingList) {
        try {
            Integer idC = sessionService.trovaUtentebBySessione(sessionId);
            Cliente cliente = Optional.ofNullable(clienteRepo.findById(idC))
                    .orElseThrow(() -> new IllegalArgumentException("Cliente non trovato per sessionId: " + sessionId));

            for (Fixing f : fixingList) {
                f.setUtente(cliente);
                fixingRepo.persist(f);
            }
        } catch (PersistenceException e) {
            throw new RuntimeException("Errore durante il salvataggio dei Fixing", e);
        } catch (Exception e) {
            throw new RuntimeException("Errore imprevisto durante l'aggiunta dei Fixing", e);
        }
    }

    @Transactional
    public void updateFixing(Integer sessionId, Fixing fixing) {
        try {
            Integer idC = sessionService.trovaUtentebBySessione(sessionId);
            Cliente cliente = Optional.ofNullable(clienteRepo.findById(idC))
                    .orElseThrow(() -> new IllegalArgumentException("Cliente non trovato per sessionId: " + sessionId));

            fixing.setUtente(cliente);
            fixingRepo.persist(fixing);
        } catch (PersistenceException e) {
            throw new RuntimeException("Errore durante l'aggiornamento del Fixing", e);
        } catch (Exception e) {
            throw new RuntimeException("Errore imprevisto durante l'aggiornamento del Fixing", e);
        }
    }

    @Transactional
    public void deleteFixing(Integer sessionId, Integer id) {
        try {
            Integer idC = sessionService.trovaUtentebBySessione(sessionId);
            Cliente cliente = Optional.ofNullable(clienteRepo.findById(idC))
                    .orElseThrow(() -> new IllegalArgumentException("Cliente non trovato per sessionId: " + sessionId));

            fixingRepo.deleteById(id);
        } catch (PersistenceException e) {
            throw new RuntimeException("Errore durante l'eliminazione del Fixing", e);
        } catch (Exception e) {
            throw new RuntimeException("Errore imprevisto durante l'eliminazione del Fixing", e);
        }
    }
}
