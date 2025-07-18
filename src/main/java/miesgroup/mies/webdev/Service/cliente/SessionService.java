package miesgroup.mies.webdev.Service.cliente;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.cliente.SessionRepo;

@ApplicationScoped
public class SessionService {

    @PersistenceContext
    EntityManager em;

    private final SessionRepo sessionRepo;

    public SessionService(SessionRepo sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @Transactional
    public Integer trovaUtentebBySessione(int id_sessione) {
        return sessionRepo.find(id_sessione);
    }

    @Transactional
    public Cliente trovaUtenteCategoryBySessione(int sessionId) {
        int id = sessionRepo.find(sessionId);
        return sessionRepo.findCategory(id);
    }

    @Transactional
    public void deleteSessionsByUserId(int userId) {
        em.createQuery("DELETE FROM Sessione s WHERE s.utente.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }
}