package miesgroup.mies.webdev.Repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.Cliente;
import miesgroup.mies.webdev.Model.Pod;
import miesgroup.mies.webdev.Service.LoggerService;

import javax.sql.DataSource;
import java.util.Optional;

@ApplicationScoped
public class ClienteRepo implements PanacheRepositoryBase<Cliente, Integer> {
    private final DataSource dataSource;
    private final LoggerService loggerService;
    private final PodRepo podRepo;

    public ClienteRepo(DataSource dataSources, LoggerService loggerService, PodRepo podRepo) {
        this.dataSource = dataSources;
        this.loggerService = loggerService;
        this.podRepo = podRepo;
    }

    public boolean existsByUsername(String username) {
        return count("username", username) > 0;
    }

    public Optional<Cliente> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public Optional<Cliente> findByUsernamelAndPasswordHash(String username, String password) {
        return find("username = ?1 and password = ?2", username, password).firstResultOptional();
    }

    public String getClasseAgevolazioneByPod(String idPod) {
        Pod p = Pod.find("id", idPod).firstResult();
        if (p == null) {
            return null;
        }
        Cliente c = p.getUtente();
        if (c == null) {
            return null;
        }
        return c.getClasseAgevolazione();
    }

    public Cliente getCliente(Integer idUtente) {
        return findById(idUtente);
    }

    @Transactional
    public boolean updateCliente(int idUtente, String field, String newValue) {
        Cliente cliente = findById(idUtente);
        if (cliente == null) {
            return false;
        }

        switch (field) {
            case "username":
                cliente.setUsername(newValue);
                break;
            case "password":
                cliente.setPassword(newValue);
                break;
            case "sedeLegale":
                cliente.setSedeLegale(newValue);
                break;
            case "pIva":
                cliente.setpIva(newValue);
                break;
            case "stato":
                cliente.setStato(newValue);
                break;
            case "email":
                cliente.setEmail(newValue);
                break;
            case "telefono":
                cliente.setTelefono(newValue);
                break;
            case "tipologia":
                cliente.setTipologia(newValue);
                break;
            case "classeAgevolazione":
                cliente.setClasseAgevolazione(newValue);
                break;
            case "codiceAteco":
                cliente.setCodiceAteco(newValue);
                break;
            case "codiceAtecoSecondario":
                cliente.setCodiceAtecoSecondario(newValue);
                break;

            case "energivori":
                if (newValue == null || newValue.trim().isEmpty()) {
                    cliente.setEnergivori(false);
                } else {
                    cliente.setEnergivori(Boolean.parseBoolean(newValue));
                }
                break;

            case "gassivori":
                if (newValue == null || newValue.trim().isEmpty()) {
                    cliente.setGassivori(false);
                } else {
                    cliente.setGassivori(Boolean.parseBoolean(newValue));
                }
                break;

            case "checkEmail":
                if (newValue == null || newValue.trim().isEmpty()) {
                    cliente.setCheckEmail(false);
                } else {
                    cliente.setCheckEmail(Boolean.parseBoolean(newValue));
                }
                break;

            case "consumoAnnuoEnergia":
                if (newValue == null || newValue.trim().isEmpty()) {
                    cliente.setConsumoAnnuoEnergia(null);
                } else {
                    try {
                        cliente.setConsumoAnnuoEnergia(Float.parseFloat(newValue));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                break;

            case "fatturatoAnnuo":
                if (newValue == null || newValue.trim().isEmpty()) {
                    cliente.setFatturatoAnnuo(null);
                } else {
                    try {
                        cliente.setFatturatoAnnuo(Float.parseFloat(newValue));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                break;

            default:
                return false;
        }
        return true;
    }

    public Cliente getClienteByPod(String idPod) {
        Pod p = podRepo.find("id", idPod).firstResult();
        if (p == null) {
            return null;
        }
        return p.getUtente();
    }
}
