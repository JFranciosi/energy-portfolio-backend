package miesgroup.mies.webdev.Repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import miesgroup.mies.webdev.Model.BollettaPod;
import miesgroup.mies.webdev.Model.Cliente;
import miesgroup.mies.webdev.Model.Pod;
import miesgroup.mies.webdev.Model.Sessione;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class SessionRepo implements PanacheRepositoryBase<Sessione, Integer> {
    private final DataSource dataSources;
    private final ClienteRepo clienteRepo;
    private final SessionRepo sessionRepo;

    public SessionRepo(DataSource dataSources, ClienteRepo clienteRepo, SessionRepo sessionRepo) {
        this.dataSources = dataSources;
        this.clienteRepo = clienteRepo;
        this.sessionRepo = sessionRepo;
    }

    @Inject
    PodRepo podRepo;

    @Inject
    BollettaRepo bollettaRepo;


    public int insertSession(int idUtente) {
        Cliente cliente = clienteRepo.findById(idUtente);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente con ID " + idUtente + " non trovato.");
        }

        Sessione sessione = new Sessione();
        sessione.setUtente(cliente);
        sessione.setDataSessione(new Timestamp(System.currentTimeMillis())); // Imposta la data attuale
        sessione.persist();

        return sessione.getId(); // Hibernate aggiorna automaticamente l'ID dopo il persist
    }


    public Optional<Sessione> getSessionByUserId(int userId) {
        return find("utente.id", userId).firstResultOptional();
    }


    public void delete(int sessionId) {
        delete("id", sessionId);
    }

    public Integer find(int idSessione) {
        Sessione sessione = sessionRepo.findById(idSessione);
        return (sessione != null) ? sessione.getUtente().getId() : null;
    }


    public Cliente findCategory(int idUtente) {
        return clienteRepo.findById(idUtente);
    }

    public Optional<Sessione> getSessionById(Integer sessionCookie) {
        return find("id", sessionCookie).firstResultOptional();
    }

    public Integer getUserIdBySessionId(int sessionId) {
        Optional<Sessione> sessioneOpt = find("id", sessionId).firstResultOptional();
        if (sessioneOpt.isPresent() && sessioneOpt.get().getUtente() != null) {
            return sessioneOpt.get().getUtente().getId();
        } else {
            return null; // Sessione non trovata o utente mancante
        }
    }

    public List<BollettaPod> findByUserId(int userId) {
        // 1. Recupera tutti i POD dell'utente
        List<Pod> podsUtente = podRepo.find("utente.id", userId).list();

        // 2. Se non ci sono POD, ritorna lista vuota
        if (podsUtente == null || podsUtente.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. Ottieni tutti gli idPod
        List<String> idPods = podsUtente.stream()
                .map(Pod::getId)
                .collect(Collectors.toList());

        // 4. Recupera tutte le bollette per questi idPod
        if (idPods.isEmpty()) {
            return new ArrayList<>();
        }

        // Questo metodo dipende dalla tua implementazione del repo,
        // ma in Hibernate Panache puoi fare così:
        return bollettaRepo.list("idPod in ?1", idPods);
    }

}

