package miesgroup.mies.webdev.Service.cliente;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;
import miesgroup.mies.webdev.Rest.Model.ClienteRequest;
import miesgroup.mies.webdev.Rest.Model.ClienteResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

@ApplicationScoped
public class ClienteService {
    private final ClienteRepo clienteRepo;
    private final HashCalculator hashCalculator;

    @PersistenceContext
    EntityManager em;

    public ClienteService(ClienteRepo clienteRepo, HashCalculator hashCalculator) {
        this.clienteRepo = clienteRepo;
        this.hashCalculator = hashCalculator;
    }

    @Transactional
    public Cliente createCliente(ClienteRequest dto) {
        Cliente cliente = new Cliente();
        cliente.setUsername(dto.getUsername());
        cliente.setPassword(hashCalculator.calculateHash(dto.getPassword()));
        cliente.setEmail(dto.getEmail());
        cliente.setpIva(dto.getpIva());
        cliente.setSedeLegale(dto.getSedeLegale());
        cliente.setTelefono(dto.getTelefono());
        cliente.setStato(dto.getStato());
        cliente.setTipologia(dto.getTipologia());
        cliente.setClasseAgevolazione(dto.getClasseAgevolazione());
        cliente.setCodiceAteco(dto.getCodiceAteco());
        cliente.setCodiceAtecoSecondario(dto.getCodiceAtecoSecondario());
        cliente.setConsumoAnnuoEnergia(dto.getConsumoAnnuoEnergia());
        cliente.setFatturatoAnnuo(dto.getFatturatoAnnuo());
        cliente.setEnergivori(null);
        cliente.setGassivori(false);
        cliente.setCheckEmail(false);

        cliente.persist();
        return cliente;
    }

    @Transactional
    public String getClasseAgevolazione(String idPod) {
        return clienteRepo.getClasseAgevolazioneByPod(idPod);
    }

    public Cliente getCliente(int idUtente) {
        return clienteRepo.getCliente(idUtente);
    }

    @Transactional
    public boolean updateCliente(int idUtente, String field, String newValue) {
        if ("password".equals(field)) {
            newValue = hashCalculator.calculateHash(newValue);
        }
        return clienteRepo.updateCliente(idUtente, field, newValue);
    }

    public ClienteResponse parseResponse(Cliente c) {
        return new ClienteResponse(
                c.getId(),
                c.getUsername(),
                c.getEmail(),
                c.getpIva(),
                c.getSedeLegale(),
                c.getTelefono(),
                c.getStato(),
                c.getTipologia(),
                c.getClasseAgevolazione(),
                c.getCodiceAteco(),
                c.getCodiceAtecoSecondario(),
                c.getConsumoAnnuoEnergia(),
                c.getFatturatoAnnuo(),
                c.getEnergivori(),
                c.getGassivori(),
                c.getCheckEmail()
        );
    }

    public List<Cliente> getClientsCheckEmail() {
        return clienteRepo.find("checkEmail", true).list();
    }

    public Cliente getClienteByPod(String idPod){
        return clienteRepo.getClienteByPod(idPod);
    }

    public List<Cliente> getAllClienti() {
        return clienteRepo.findAll().list();
    }

    @Transactional
    public boolean deleteCliente(int id) {
        Cliente c = clienteRepo.findById(id);
        if (c == null) {
            return false;
        }
        clienteRepo.delete(c);
        return true;
    }

    /**
     * Recupera il Cliente associato alla sessione identificata da sessionId.
     * Assumendo che ci sia un'entità Sessione con campo sessionId e clienteId.
     */
    public Cliente getUserFromSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        try {
            TypedQuery<Cliente> query = em.createQuery(
                    "SELECT c FROM Sessione s JOIN Cliente c ON s.utente.id = c.id WHERE s.id = :sessionId",
                    Cliente.class);
            query.setParameter("sessionId", sessionId);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Imposta il flag keepLogged per un cliente.
     * Assicurati che Cliente abbia il campo Boolean keepLogged con getter/setter
     */
    @Transactional
    public boolean setKeepLogged(int idUtente, boolean keepLogged) {
        Cliente cliente = clienteRepo.findById(idUtente);
        if (cliente == null) {
            return false;
        }
        cliente.setKeepLogged(keepLogged);
        cliente.persist();
        return true;
    }

    @Transactional
    public boolean updatePassword(int idUtente, String currentPassword, String newPassword) {
        Cliente cliente = clienteRepo.findById(idUtente);
        if (cliente == null) {
            return false;
        }
        // Verifica che la password attuale corrisponda (usando hashCalculator)
        String currentPasswordHash = hashCalculator.calculateHash(currentPassword);
        if (!cliente.getPassword().equals(currentPasswordHash)) {
            // Password attuale errata
            return false;
        }
        // Aggiorna la password con il nuovo hash
        cliente.setPassword(hashCalculator.calculateHash(newPassword));
        cliente.persist();
        return true;
    }

    @Transactional
    public boolean existsById(Integer clientId) {
        if (clientId == null || clientId <= 0) {
            return false;
        }
        return clienteRepo.findById(clientId) != null;
    }


}
