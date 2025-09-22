package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.bolletta.CostiEnergia;
import miesgroup.mies.webdev.Model.bolletta.CostiPeriodi;
import miesgroup.mies.webdev.Repository.bolletta.CostiPeriodiRepo;
import miesgroup.mies.webdev.Repository.bolletta.CostoEnergiaRepo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CostiPeriodiService {

    private final CostiPeriodiRepo costiPeriodiRepo;
    private final CostoEnergiaRepo costiEnergiaRepo;

    public CostiPeriodiService(CostiPeriodiRepo costiPeriodiRepo,
                               CostoEnergiaRepo costiEnergiaRepo) {
        this.costiPeriodiRepo = costiPeriodiRepo;
        this.costiEnergiaRepo = costiEnergiaRepo;
    }

    /**
     * Aggiorna un singolo periodo dinamico
     */
    @Transactional
    public boolean updateSinglePeriodo(Integer energyCostId, Integer monthStart,
                                       BigDecimal costF1, BigDecimal costF2, BigDecimal costF3,
                                       Integer clientId) {
        // Verifica autorizzazione
        if (!isClientAuthorizedForEnergyCost(clientId, energyCostId)) {
            return false;
        }

        Optional<CostiPeriodi> periodo = costiPeriodiRepo.findByEnergyCostIdAndMonth(energyCostId, monthStart);
        if (periodo.isPresent()) {
            CostiPeriodi p = periodo.get();
            p.setCostF1(costF1);
            p.setCostF2(costF2);
            p.setCostF3(costF3);
            costiPeriodiRepo.persist(p);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean updateSinglePeriodo(Integer energyCostId, Integer monthStart,
                                       BigDecimal costF1, BigDecimal costF2, BigDecimal costF3,
                                       BigDecimal percentageVariable, Integer clientId) {

        // Verifica autorizzazione
        if (!isClientAuthorizedForEnergyCost(clientId, energyCostId)) {
            return false;
        }

        Optional<CostiPeriodi> periodo = costiPeriodiRepo.findByEnergyCostIdAndMonth(energyCostId, monthStart);

        if (periodo.isPresent()) {
            // AGGIORNA periodo esistente
            CostiPeriodi p = periodo.get();
            p.setCostF1(costF1);
            p.setCostF2(costF2);
            p.setCostF3(costF3);
            p.setPercentageVariable(percentageVariable);
            costiPeriodiRepo.persist(p);
            return true;
        } else {
            // CREA nuovo periodo se non esiste
            CostiEnergia costiEnergia = costiEnergiaRepo.findById(energyCostId);
            if (costiEnergia != null && costiEnergia.getClientId().equals(clientId)) {
                CostiPeriodi nuovoPeriodo = new CostiPeriodi();
                nuovoPeriodo.setCostiEnergia(costiEnergia);
                nuovoPeriodo.setMonthStart(monthStart);
                nuovoPeriodo.setCostF1(costF1);
                nuovoPeriodo.setCostF2(costF2);
                nuovoPeriodo.setCostF3(costF3);
                nuovoPeriodo.setPercentageVariable(percentageVariable);
                nuovoPeriodo.setCreatedAt(Timestamp.from(Instant.now()));
                costiPeriodiRepo.persist(nuovoPeriodo);
                return true;
            }
            return false;
        }
    }


    /**
     * Verifica se la sequenza di periodi è valida per un cliente
     */
    public boolean isValidSequenceForClient(Integer energyCostId, Integer clientId) {
        if (!isClientAuthorizedForEnergyCost(clientId, energyCostId)) {
            return false;
        }
        return costiPeriodiRepo.isValidPeriodSequence(energyCostId);
    }

    /**
     * Ottiene i mesi disponibili per un costo energia
     */
    public List<Integer> getAvailableMonths(Integer energyCostId, Integer clientId) {
        if (!isClientAuthorizedForEnergyCost(clientId, energyCostId)) {
            return List.of();
        }
        return costiPeriodiRepo.getAvailableMonthsForEnergyCost(energyCostId);
    }

    /**
     * Verifica autorizzazione cliente per energy cost
     */
    private boolean isClientAuthorizedForEnergyCost(Integer clientId, Integer energyCostId) {
        CostiEnergia costEnergia = costiEnergiaRepo.findById(energyCostId);
        if (costEnergia != null) {
            return costEnergia.getClientId().equals(clientId); // assumendo che getClientId() esista
        }
        return false;
    }

    /**
     * Elimina un singolo periodo dinamico
     */
    @Transactional
    public boolean deleteSinglePeriodo(Integer energyCostId, Integer monthStart, Integer userId) {
        System.out.println("=== INIZIO deleteSinglePeriodo ===");
        System.out.println("energyCostId: " + energyCostId);
        System.out.println("monthStart: " + monthStart);
        System.out.println("userId: " + userId);

        try {
            System.out.println("1. Verifico autorizzazione del cliente...");

            // Verifica autorizzazione del cliente per questo energy cost
            boolean isAuthorized = isClientAuthorizedForEnergyCost(userId, energyCostId);
            System.out.println("1a. isClientAuthorizedForEnergyCost result: " + isAuthorized);

            if (!isAuthorized) {
                System.out.println("1b. ERRORE: Cliente non autorizzato per energyCostId: " + energyCostId);
                return false;
            }

            System.out.println("2. Verifico esistenza del periodo...");

            // Verifica che il periodo esista prima di eliminarlo
            Optional<CostiPeriodi> periodo = costiPeriodiRepo.findByEnergyCostIdAndMonth(energyCostId, monthStart);
            System.out.println("2a. Periodo trovato: " + periodo.isPresent());

            if (!periodo.isPresent()) {
                System.out.println("2b. ERRORE: Periodo non trovato");
                return false;
            }

            CostiPeriodi periodoToDelete = periodo.get();
            System.out.println("2c. ID del periodo da eliminare: " + periodoToDelete.getId());

            System.out.println("3. Doppia verifica proprietà periodo...");
            CostiEnergia costiEnergia = periodoToDelete.getCostiEnergia();

            if (costiEnergia == null || !costiEnergia.getClientId().equals(userId)) {
                System.out.println("3a. ERRORE: Il periodo non appartiene al cliente");
                return false;
            }

            System.out.println("4. Procedo con l'eliminazione tramite query diretta...");

            // USA LA QUERY DIRETTA invece di delete(entity)
            long deletedRows = costiPeriodiRepo.deleteByEnergyCostIdAndMonth(energyCostId, monthStart);
            System.out.println("4a. Righe eliminate: " + deletedRows);

            if (deletedRows > 0) {
                System.out.println("=== SUCCESSO: Periodo eliminato dal database ===");
                return true;
            } else {
                System.out.println("4b. ERRORE: Nessuna riga eliminata");
                return false;
            }

        } catch (Exception e) {
            System.out.println("ECCEZIONE in deleteSinglePeriodo:");
            System.out.println("Messaggio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}

