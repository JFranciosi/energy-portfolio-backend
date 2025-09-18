package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import miesgroup.mies.webdev.Model.bolletta.CostiEnergia;
import miesgroup.mies.webdev.Model.bolletta.CostiPeriodi;
import miesgroup.mies.webdev.Repository.bolletta.CostiPeriodiRepo;
import miesgroup.mies.webdev.Repository.bolletta.CostoEnergiaRepo;
import miesgroup.mies.webdev.Service.cliente.ClienteService;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CostiEnergiaService {

    private final CostoEnergiaRepo costiEnergiaRepo;
    private final CostiPeriodiRepo costiPeriodiRepo;
    private final ClienteService clienteService;

    public CostiEnergiaService(CostoEnergiaRepo costiEnergiaRepo,
                               CostiPeriodiRepo costiPeriodiRepo,
                               ClienteService clienteService) {
        this.costiEnergiaRepo = costiEnergiaRepo;
        this.costiPeriodiRepo = costiPeriodiRepo;
        this.clienteService = clienteService;
    }

    /* ===== Metodi di lettura per mostrare dati al cliente ===== */

    /**
     * Recupera i costi energetici di un cliente per un anno specifico
     */
    public Optional<CostiEnergia> getCostiByClienteAndAnno(Integer clientId, Integer year) {
        if (!isValidClient(clientId)) {
            return Optional.empty();
        }
        return costiEnergiaRepo.findByClientIdAndYear(clientId, year);
    }

    /**
     * Recupera tutti i costi di un cliente
     */
    public List<CostiEnergia> getAllCostiByCliente(Integer clientId) {
        if (!isValidClient(clientId)) {
            return List.of();
        }
        return costiEnergiaRepo.findByClientId(clientId);
    }

    /**
     * Recupera i periodi dinamici per un costo energia specifico
     */
    public List<CostiPeriodi> getPeriodiDinamici(Integer energyCostId, Integer clientId) {
        // Verifica autorizzazione cliente
        if (!isClientAuthorizedForCosto(clientId, energyCostId)) {
            return List.of();
        }
        return costiPeriodiRepo.findByEnergyCostId(energyCostId);
    }

    /**
     * Verifica se esistono già costi per cliente e anno
     */
    public boolean existsCostiForClienteAndAnno(Integer clientId, Integer year) {
        return costiEnergiaRepo.existsByClientIdAndYear(clientId, year);
    }

    /* ===== Metodi di salvataggio ===== */

    /**
     * Salva o aggiorna i costi energetici (principale metodo di business logic)
     */
    @Transactional
    public CostiEnergia saveOrUpdateCostiEnergia(@NotNull CostiEnergia nuoviCosti) {
        // Validazioni iniziali
        if (!isValidCostiEnergia(nuoviCosti)) {
            throw new IllegalArgumentException("Dati costi energia non validi");
        }

        if (!isValidClient(nuoviCosti.getClientId())) {
            throw new IllegalArgumentException("Cliente non valido o non esistente");
        }

        // Controllo se esistono già costi per questo cliente/anno
        Optional<CostiEnergia> esistente = costiEnergiaRepo.findByClientIdAndYear(
                nuoviCosti.getClientId(), nuoviCosti.getYear());

        if (esistente.isPresent()) {
            return updateCostiEsistenti(esistente.get(), nuoviCosti);
        } else {
            return createNuoviCosti(nuoviCosti);
        }
    }

    /**
     * Aggiorna costi esistenti
     */
    @Transactional
    public CostiEnergia updateCostiEsistenti(CostiEnergia esistente, CostiEnergia nuoviCosti) {
        // Aggiorna campi principali
        esistente.setTipoPrezzo(nuoviCosti.getTipoPrezzo());
        esistente.setTipoTariffa(nuoviCosti.getTipoTariffa());

        if (!"dinamico".equals(nuoviCosti.getTipoPrezzo())) {
            esistente.setPercentageVariable(nuoviCosti.getPercentageVariable());
        } else {
            // Per i costi dinamici, mantieni NULL nella tabella principale
            esistente.setPercentageVariable(null);
        }

        // Aggiorna costi fasce
        esistente.setCostF0(nuoviCosti.getCostF0());
        esistente.setCostF1(nuoviCosti.getCostF1());
        esistente.setCostF2(nuoviCosti.getCostF2());
        esistente.setCostF3(nuoviCosti.getCostF3());

        // Aggiorna spread per indicizzati
        esistente.setSpreadF1(nuoviCosti.getSpreadF1());
        esistente.setSpreadF2(nuoviCosti.getSpreadF2());
        esistente.setSpreadF3(nuoviCosti.getSpreadF3());

        esistente.setUpdatedAt(Timestamp.from(Instant.now()));

        // Gestisce periodi dinamici se necessario
        if ("dinamico".equals(nuoviCosti.getTipoPrezzo()) &&
                nuoviCosti.getCostiPeriodi() != null && !nuoviCosti.getCostiPeriodi().isEmpty()) {
            updatePeriodiDinamici(esistente, nuoviCosti.getCostiPeriodi());
        } else {
            // Rimuove eventuali periodi dinamici se il tipo non è più dinamico
            costiPeriodiRepo.deleteAllByEnergyCostId(esistente.getId());
        }

        costiEnergiaRepo.persist(esistente);
        return esistente;
    }

    /**
     * Crea nuovi costi
     */
    @Transactional
    public CostiEnergia createNuoviCosti(CostiEnergia nuoviCosti) {
        nuoviCosti.setCreatedAt(Timestamp.from(Instant.now()));
        nuoviCosti.setUpdatedAt(Timestamp.from(Instant.now()));

        costiEnergiaRepo.persist(nuoviCosti);

        // Se il tipo è dinamico e non è stata passata la lista, crea periodi base
        if ("dinamico".equals(nuoviCosti.getTipoPrezzo())) {
            List<CostiPeriodi> periodi = nuoviCosti.getCostiPeriodi();

            if (periodi != null && !periodi.isEmpty()) {
                savePeriodiDinamici(nuoviCosti, periodi);
            }
        }

        return nuoviCosti;
    }


    /**
     * Gestisce l'aggiornamento dei periodi dinamici
     */
    @Transactional
    public void updatePeriodiDinamici(CostiEnergia costiEnergia, List<CostiPeriodi> nuoviPeriodi) {
        // Elimina tutti i periodi esistenti
        costiPeriodiRepo.deleteAllByEnergyCostId(costiEnergia.getId());

        // Salva i nuovi periodi
        savePeriodiDinamici(costiEnergia, nuoviPeriodi);
    }

    /**
     * Salva i periodi dinamici
     */
    @Transactional
    public void savePeriodiDinamici(CostiEnergia costiEnergia, List<CostiPeriodi> periodi) {
        for (CostiPeriodi periodo : periodi) {
            if (!isValidPeriodo(periodo)) {
                throw new IllegalArgumentException("Periodo non valido: " + periodo.getMonthStart());
            }

            periodo.setCostiEnergia(costiEnergia);
            periodo.setCreatedAt(Timestamp.from(Instant.now()));
            costiPeriodiRepo.persist(periodo);
        }
    }

    /* ===== Metodi di eliminazione ===== */

    /**
     * Elimina tutti i costi di un cliente per un anno
     */
    @Transactional
    public boolean deleteCostiByClienteAndAnno(Integer clientId, Integer year) {
        if (!isValidClient(clientId)) {
            return false;
        }

        Optional<CostiEnergia> costi = costiEnergiaRepo.findByClientIdAndYear(clientId, year);
        if (costi.isPresent()) {
            // Prima elimina i periodi dinamici associati
            costiPeriodiRepo.deleteAllByEnergyCostId(costi.get().getId());
            // Poi elimina il costo principale
            costiEnergiaRepo.delete(costi.get());
            return true;
        }
        return false;
    }

    /* ===== Metodi di validazione e controllo ===== */

    /**
     * Verifica se il cliente è valido
     */
    private boolean isValidClient(Integer clientId) {
        if (clientId == null || clientId <= 0) {
            return false;
        }
        // Verifica esistenza cliente tramite ClienteService
        return clienteService.existsById(clientId);
    }

    /**
     * Verifica se il cliente è autorizzato ad accedere a questo costo
     */
    private boolean isClientAuthorizedForCosto(Integer clientId, Integer energyCostId) {
        if (!isValidClient(clientId) || energyCostId == null) {
            return false;
        }

        CostiEnergia costi = costiEnergiaRepo.findById(energyCostId);
        return costi != null && costi.getClientId().equals(clientId);
    }

    /**
     * Valida i dati di CostiEnergia
     */
    private boolean isValidCostiEnergia(CostiEnergia costi) {
        if (costi == null || costi.getClientId() == null || costi.getYear() == null) {
            return false;
        }

        // Valida anno (deve essere ragionevole)
        if (costi.getYear() < 2020 || costi.getYear() > 2050) {
            return false;
        }

        // Valida tipo prezzo
        if (!isValidTipoPrezzo(costi.getTipoPrezzo())) {
            return false;
        }

        // Validazioni specifiche per tipo
        if ("fisso".equals(costi.getTipoPrezzo()) && costi.getTipoTariffa() == null) {
            return false;
        }

        if ("indicizzato".equals(costi.getTipoPrezzo()) && !hasValidSpread(costi)) {
            return false;
        }

        // Valida che i costi non siano negativi
        return !hasNegativeCosts(costi);
    }

    /**
     * Verifica se il tipo prezzo è valido
     */
    private boolean isValidTipoPrezzo(String tipoPrezzo) {
        return tipoPrezzo != null &&
                (tipoPrezzo.equals("fisso") || tipoPrezzo.equals("indicizzato") ||
                        tipoPrezzo.equals("misto") || tipoPrezzo.equals("dinamico"));
    }

    /**
     * Verifica se ci sono valori spread validi per prezzi indicizzati
     */
    private boolean hasValidSpread(CostiEnergia costi) {
        return (costi.getSpreadF1() != null && costi.getSpreadF1().compareTo(BigDecimal.ZERO) >= 0) ||
                (costi.getSpreadF2() != null && costi.getSpreadF2().compareTo(BigDecimal.ZERO) >= 0) ||
                (costi.getSpreadF3() != null && costi.getSpreadF3().compareTo(BigDecimal.ZERO) >= 0);
    }

    /**
     * Verifica che non ci siano costi negativi
     */
    private boolean hasNegativeCosts(CostiEnergia costi) {
        return isNegative(costi.getCostF0()) || isNegative(costi.getCostF1()) ||
                isNegative(costi.getCostF2()) || isNegative(costi.getCostF3()) ||
                isNegative(costi.getSpreadF1()) || isNegative(costi.getSpreadF2()) ||
                isNegative(costi.getSpreadF3());
    }

    /**
     * Controlla se un valore è negativo
     */
    private boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Valida un periodo dinamico
     */
    private boolean isValidPeriodo(CostiPeriodi periodo) {
        if (periodo == null || periodo.getMonthStart() == null) {
            return false;
        }

        // Valida mese (1-12)
        if (periodo.getMonthStart() < 1 || periodo.getMonthStart() > 12) {
            return false;
        }

        // Valida che almeno un costo sia presente e non negativo
        return (periodo.getCostF1() != null && periodo.getCostF1().compareTo(BigDecimal.ZERO) >= 0) ||
                (periodo.getCostF2() != null && periodo.getCostF2().compareTo(BigDecimal.ZERO) >= 0) ||
                (periodo.getCostF3() != null && periodo.getCostF3().compareTo(BigDecimal.ZERO) >= 0);
    }

    /* ===== Metodi di utilità per business logic ===== */

    /**
     * Calcola il costo medio per un cliente in un range di anni
     */
    public Double getCostoMedioByClienteAndRange(Integer clientId, Integer yearStart, Integer yearEnd) {
        if (!isValidClient(clientId)) {
            return 0.0;
        }

        List<CostiEnergia> costiRange = costiEnergiaRepo.getCostiEnergiaByClientAndYearRange(
                clientId, yearStart, yearEnd);

        return costiRange.stream()
                .flatMap(c -> List.of(c.getCostF1(), c.getCostF2(), c.getCostF3()).stream())
                .filter(cost -> cost != null && cost.signum() > 0) // Usa signum() invece di >
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Verifica se un cliente ha configurazioni di prezzo dinamico
     */
    public boolean hasDynamicPricing(Integer clientId, Integer year) {
        return costiEnergiaRepo.isDynamicPricing(clientId, year);
    }

    /**
     * Ottiene gli anni per cui il cliente ha configurazioni costi
     */
    public List<Integer> getAnniConfigurati(Integer clientId) {
        if (!isValidClient(clientId)) {
            return List.of();
        }

        return costiEnergiaRepo.findByClientId(clientId)
                .stream()
                .map(CostiEnergia::getYear)
                .distinct()
                .sorted()
                .toList();
    }

    /* ===== Metodi per statistiche e report ===== */

    /**
     * Conta quanti clienti usano un determinato tipo di prezzo
     */
    public Long countClientiByTipoPrezzo(String tipoPrezzo) {
        return costiEnergiaRepo.countByTipoPrezzo(tipoPrezzo);
    }

    /**
     * Ottiene la media dei costi F1 per un anno specifico
     */
    public Double getMediaCostiF1ByYear(Integer year) {
        return costiEnergiaRepo.getMediaCostiF1ByYear(year);
    }
}
