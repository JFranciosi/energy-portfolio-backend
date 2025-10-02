package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.verBollettaPod;
import miesgroup.mies.webdev.Repository.budget.BudgetRepo;
import miesgroup.mies.webdev.Rest.Model.*;
import java.time.LocalDate;
import java.util.*;

@ApplicationScoped
public class BudgetBollettaService {

    @Inject
    BudgetRepo budgetRepo;

    @Inject
    EntityManager entityManager;

    private static final String[] MESINOMI = {"Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"};

    private static final String[] MESIABBREVIATI = {"Gen", "Feb", "Mar", "Apr", "Mag", "Giu",
            "Lug", "Ago", "Set", "Ott", "Nov", "Dic"};

    public EnergyPortfolioCompleteDatasetDTO getBudgetConsolidato() {
        return getBudgetConsolidato(null, null);
    }

    public EnergyPortfolioCompleteDatasetDTO getBudgetConsolidato(Integer year) {
        return getBudgetConsolidato(year, null);
    }

    public EnergyPortfolioCompleteDatasetDTO getBudgetConsolidato(Integer year, String pod) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        System.out.println("getBudgetConsolidato - Anno: " + year + ", POD: " + pod);
        EnergyPortfolioCompleteDatasetDTO dataset = new EnergyPortfolioCompleteDatasetDTO();

        dataset.setBudget(getBudgetData(year, pod));
        dataset.setCalendario(getCalendarioData(year));
        dataset.setBolletta_pod(getBollettaPodData(year, pod));

        return dataset;
    }

    /**
     * Metodo per recuperare i dati della tabella Budget
     */
    private List<BudgetBollettaDTO> getBudgetData(Integer year, String pod) {
        List<BudgetBollettaDTO> budgetList = new ArrayList<>();

        // Mappatura numeri mesi -> nomi mesi italiani
        String[] MESI_ITALIANI = {
                "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
                "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"
        };

        String queryStr = "SELECT b FROM Budget b WHERE b.anno = :year";
        if (pod != null && !pod.trim().isEmpty()) {
            queryStr += " AND b.idPod = :pod";
        }

        Query query = entityManager.createQuery(queryStr);
        query.setParameter("year", year);
        if (pod != null && !pod.trim().isEmpty()) {
            query.setParameter("pod", pod);
        }

        List<Budget> budgets = query.getResultList();

        // Recupera ID e mesi delle bollette per anno e pod
        Map<String, Integer> podMeseBollettaIdMap = new HashMap<>();
        String bollettaQueryStr = "SELECT bp.idPod, bp.mese, bp.id FROM BollettaPod bp WHERE bp.anno = :year";
        if (pod != null && !pod.trim().isEmpty()) {
            bollettaQueryStr += " AND bp.idPod = :pod";
        }

        Query bollettaQuery = entityManager.createQuery(bollettaQueryStr);
        bollettaQuery.setParameter("year", year.toString());
        if (pod != null && !pod.trim().isEmpty()) {
            bollettaQuery.setParameter("pod", pod);
        }

        List<Object[]> bollettaResults = bollettaQuery.getResultList();
        for (Object[] result : bollettaResults) {
            String idPod = (String) result[0];
            String meseNome = (String) result[1]; // Nome del mese (gennaio, febbraio, etc.)
            Integer bollettaId = (Integer) result[2];

            // Crea chiave con il nome del mese (come arriva dal database)
            String chiave = idPod + "_" + meseNome.toLowerCase();
            podMeseBollettaIdMap.put(chiave, bollettaId);
        }

        // Recupera nomi bollette per compatibilità
        Map<String, String> podNomeBollettaMap = new HashMap<>();
        String nomeBollettaQueryStr = "SELECT DISTINCT bp.idPod, bp.nomeBolletta FROM BollettaPod bp WHERE bp.anno = :year";
        if (pod != null && !pod.trim().isEmpty()) {
            nomeBollettaQueryStr += " AND bp.idPod = :pod";
        }

        Query nomeBollettaQuery = entityManager.createQuery(nomeBollettaQueryStr);
        nomeBollettaQuery.setParameter("year", year.toString());
        if (pod != null && !pod.trim().isEmpty()) {
            nomeBollettaQuery.setParameter("pod", pod);
        }

        List<Object[]> nomeBollettaResults = nomeBollettaQuery.getResultList();
        for (Object[] result : nomeBollettaResults) {
            String idPod = (String) result[0];
            String nomeBolletta = (String) result[1];

            if (!podNomeBollettaMap.containsKey(idPod)) {
                podNomeBollettaMap.put(idPod, nomeBolletta);
            }
        }

        for (Budget budget : budgets) {
            BudgetBollettaDTO dto = new BudgetBollettaDTO();

            String idPodStr = String.valueOf(budget.getIdPod());

            // CONVERSIONE CRUCIALE: da numero mese a nome mese italiano
            String meseNome = null;
            if (budget.getMese() != null && budget.getMese() >= 1 && budget.getMese() <= 12) {
                meseNome = MESI_ITALIANI[budget.getMese() - 1]; // Array 0-based
            }

            // Costruisci la chiave con il nome del mese
            String chiaveBolletta = idPodStr + "_" + meseNome;

            // Trova l'ID della bolletta corrispondente
            Integer bollettaId = podMeseBollettaIdMap.get(chiaveBolletta);

            // Assegna l'ID bolletta trovato o 0 se non trovato
            dto.setIdBolletta(bollettaId != null ? bollettaId : 0);

            System.out.println("Budget mese: " + budget.getMese() + " -> Nome mese: " + meseNome +
                    " -> Chiave: " + chiaveBolletta + " -> ID Bolletta: " + bollettaId);

            dto.setIdPod(idPodStr);

            String nomeBolletta = podNomeBollettaMap.get(idPodStr);
            if (nomeBolletta != null) {
                try {
                    dto.setNomeBolletta(Integer.parseInt(nomeBolletta));
                } catch (NumberFormatException e) {
                    // Il nome bolletta non è un numero - mantieni come stringa o imposta valore di default
                    dto.setNomeBolletta(0);
                }
            } else {
                dto.setNomeBolletta(0);
            }

            dto.setTotAttiva(budget.getConsumiBase() != null ? budget.getConsumiBase().intValue() : 0);

            if (budget.getPrezzoEnergiaBase() != null && budget.getPrezzoEnergiaPerc() != null) {
                Double budgetEnergia = budget.getPrezzoEnergiaBase() + (budget.getPrezzoEnergiaBase() * budget.getPrezzoEnergiaPerc());
                dto.setBudgetEnergia(budgetEnergia.intValue());
            }

            if (budget.getOneriBase() != null && budget.getOneriPerc() != null) {
                Double budgetOneri = budget.getOneriBase() + (budget.getOneriBase() * budget.getOneriPerc());
                dto.setBudgetOneri(budgetOneri.intValue());
            }

            dto.setBudgetTrasporto(null);
            dto.setBudgetImposte(null);
            dto.setBudgetTotale(null);
            dto.setBudgetPenali(null);
            dto.setBudgetAltro(null);

            if (budget.getMese() != null && budget.getMese() >= 1 && budget.getMese() <= 12) {
                dto.setAnnoMese(MESINOMI[budget.getMese() - 1] + "-" + year);
            }

            budgetList.add(dto);
        }

        return budgetList;
    }

    /**
     * Metodo per generare i dati della tabella calendario (statica)
     */
    private List<CalendarioDTO> getCalendarioData(Integer year) {
        List<CalendarioDTO> calendarioList = new ArrayList<>();

        for (int mese = 1; mese <= 12; mese++) {
            CalendarioDTO dto = new CalendarioDTO();

            dto.setAnno((long) year);
            dto.setMeseNumero((long) mese);
            dto.setMeseNome(MESINOMI[mese - 1]);
            dto.setAnnoMese(MESINOMI[mese - 1] + "-" + year);
            dto.setMeseAbbreviato(MESIABBREVIATI[mese - 1]);

            String trimestre = String.valueOf((mese - 1) / 3 + 1);
            dto.setTrimestre(trimestre);
            dto.setPeriodo(trimestre + "-" + year);
            dto.setPeriodoTrimestre(year + "-T" + trimestre);

            calendarioList.add(dto);
        }

        return calendarioList;
    }

    /**
     * Metodo per recuperare i dati della tabella bolletta_pod con JOIN a verBollettaPod
     */
    private List<BollettaPodDTO> getBollettaPodData(Integer year, String pod) {
        List<BollettaPodDTO> bollettaPodList = new ArrayList<>();

        // Query con LEFT JOIN a verBollettaPod per recuperare i campi di verifica
        String queryStr = "SELECT bp, vbp FROM BollettaPod bp " +
                "LEFT JOIN verBollettaPod vbp ON vbp.bollettaId = bp " +
                "WHERE bp.anno = :year";
        if (pod != null && !pod.trim().isEmpty()) {
            queryStr += " AND bp.idPod = :pod";
        }

        Query query = entityManager.createQuery(queryStr);
        query.setParameter("year", year.toString());
        if (pod != null && !pod.trim().isEmpty()) {
            query.setParameter("pod", pod);
        }

        List<Object[]> results = query.getResultList();

        for (Object[] result : results) {
            BollettaPod bolletta = (BollettaPod) result[0];
            verBollettaPod verifica = (verBollettaPod) result[1];

            BollettaPodDTO dto = new BollettaPodDTO();

            // Campi base da bolletta_pod
            dto.setIdBolletta(Double.valueOf(bolletta.getId()));
            dto.setIdpod(bolletta.getIdPod());

            if (bolletta.getNomeBolletta() != null) {
                try {
                    dto.setNomeBolletta(Double.parseDouble(bolletta.getNomeBolletta()));
                } catch (NumberFormatException e) {
                    dto.setNomeBolletta(0.0);
                }
            }

            // Fasce Attiva
            dto.setF1Attiva(bolletta.getF1Att());
            dto.setF2Attiva(bolletta.getF2Att());
            dto.setF3Attiva(bolletta.getF3Att());

            // Fasce Reattiva
            dto.setF1Reattiva(bolletta.getF1R());
            dto.setF2Reattiva(bolletta.getF2R());
            dto.setF3Reattiva(bolletta.getF3R());

            // Fasce Potenza
            dto.setF1Potenza(bolletta.getF1Pot());
            dto.setF2Potenza(bolletta.getF2Pot());
            dto.setF3Potenza(bolletta.getF3Pot());

            // Spese principali
            dto.setSpeseEnergia(bolletta.getSpeseEne());
            dto.setSpeseTrasporto(bolletta.getSpeseTrasp());
            dto.setOneri(bolletta.getOneri());
            dto.setImposte(bolletta.getImposte());

            // Periodo (formato "YYYY-MM-DD")
            if (bolletta.getPeriodoInizio() != null) {
                dto.setPeriodoInizio(bolletta.getPeriodoInizio().toString());
            }
            if (bolletta.getPeriodoFine() != null) {
                dto.setPeriodoFine(bolletta.getPeriodoFine().toString());
            }

            // Mese (nome completo)
            if (bolletta.getMese() != null) {
                try {
                    int meseNum = Integer.parseInt(bolletta.getMese());
                    if (meseNum >= 1 && meseNum <= 12) {
                        dto.setMese(MESINOMI[meseNum - 1]);
                    } else {
                        dto.setMese(bolletta.getMese());
                    }
                } catch (NumberFormatException e) {
                    dto.setMese(bolletta.getMese());
                }
            }

            // Totali
            dto.setTOTAttiva(bolletta.getTotAtt());
            dto.setTOTReattiva(bolletta.getTotR());

            // Altri importi
            dto.setGeneration(bolletta.getGeneration());
            dto.setDispackciamento(bolletta.getDispacciamento());

            // Penali - SOMMA di F1 + F2
            Double penali33 = safeAdd(bolletta.getF1Pen33(), bolletta.getF2Pen33());
            Double penali75 = safeAdd(bolletta.getF1Pen75(), bolletta.getF2Pen75());
            dto.setPenali33(penali33);
            dto.setPenali75(penali75);

            // Altro = penRCapI
            dto.setAltro(bolletta.getPenRCapI() != null ? bolletta.getPenRCapI() : 0.0);

            // Anno
            try {
                dto.setAnno(Double.parseDouble(bolletta.getAnno()));
            } catch (NumberFormatException e) {
                dto.setAnno(year.doubleValue());
            }

            // Picco/Fuori picco da bolletta_pod
            dto.setPiccokwh(bolletta.getPiccoKwh());
            dto.setFuoripiccokwh(bolletta.getFuoriPiccoKwh());
            dto.setPicco(bolletta.getEuroPicco());
            dto.setFuoripicco(bolletta.getEuroFuoriPicco());

            // ✅ CAMPI DI VERIFICA da verBollettaPod
            if (verifica != null) {
                dto.setVerificaTrasporti(verifica.getSpeseTrasp());
                dto.setVerificaOneri(verifica.getOneri());
                dto.setVerificaImposte(verifica.getImposte());
                dto.setVerificapicco(verifica.getEuroPicco());
                dto.setVerificafuoripicco(verifica.getEuroFuoriPicco());
            } else {
                // Se non esiste record di verifica, metti null o 0
                dto.setVerificaTrasporti(null);
                dto.setVerificaOneri(null);
                dto.setVerificaImposte(null);
                dto.setVerificapicco(null);
                dto.setVerificafuoripicco(null);
            }

            // Anno-Mese
            if (bolletta.getMese() != null) {
                try {
                    int meseNum = Integer.parseInt(bolletta.getMese());
                    if (meseNum >= 1 && meseNum <= 12) {
                        dto.setAnnoMese(MESINOMI[meseNum - 1] + "-" + year);
                    } else {
                        dto.setAnnoMese(bolletta.getMese() + "-" + year);
                    }
                } catch (NumberFormatException e) {
                    dto.setAnnoMese(bolletta.getMese() + "-" + year);
                }
            }

            bollettaPodList.add(dto);
        }

        return bollettaPodList;
    }

    /**
     * Utility per sommare in sicurezza valori Double (null = 0)
     */
    private Double safeAdd(Double a, Double b) {
        double valA = (a != null) ? a : 0.0;
        double valB = (b != null) ? b : 0.0;
        return valA + valB;
    }
}
