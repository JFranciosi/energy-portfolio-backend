package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Repository.budget.BudgetRepo;
import miesgroup.mies.webdev.Rest.Model.BudgetBollettaDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class BudgetBollettaService {

    @Inject
    BudgetRepo budgetRepo; // ← usiamo il repository, niente native SQL

    /** Compat: vecchia firma senza argomenti (tutti i POD per anno corrente, mese=1) */
    public List<BudgetBollettaDTO> getBudgetConsolidato() {
        return getBudgetConsolidato(null, null);
    }

    /** Overload: solo anno */
    public List<BudgetBollettaDTO> getBudgetConsolidato(Integer year) {
        return getBudgetConsolidato(year, null);
    }

    /** Overload: anno + pod (entrambi opzionali). Calcolo via repository. */
    public List<BudgetBollettaDTO> getBudgetConsolidato(Integer year, String pod) {
        System.out.println(">>> [getBudgetConsolidato] INIZIO");
        System.out.println(">>> Parametri in input -> year=" + year + ", pod=" + pod);

        final int targetYear = (year != null) ? year : LocalDate.now().getYear();
        final int targetMonth = 1; // come nella tua query (mese=1)
        System.out.println(">>> Filtri effettivi -> anno=" + targetYear + ", mese=" + targetMonth +
                (pod != null && !pod.isBlank() ? (", pod=" + pod) : ", pod=ALL"));

        // ===== Caricamento via repository =====
        List<Budget> budgets;
        if (pod != null && !pod.isBlank()) {
            budgets = budgetRepo.find("anno = ?1 and mese = ?2 and idPod = ?3",
                    targetYear, targetMonth, pod).list(); // usa i campi dell'Entity Budget
        } else {
            budgets = budgetRepo.find("anno = ?1 and mese = ?2", targetYear, targetMonth).list();
        }
        System.out.println(">>> Budget letti dal DB: " + (budgets != null ? budgets.size() : 0));

        // ===== Mapping → DTO + calcoli (stesse formule della SQL originale) =====
        List<BudgetBollettaDTO> out = new ArrayList<>();
        if (budgets == null || budgets.isEmpty()) {
            System.out.println(">>> Nessun record trovato. Ritorno lista vuota.");
            return out;
        }

        final String nomeBolletta = "Budget " + targetYear + "-" + String.format("%02d", targetMonth);
        final String annoMese     = String.format("%04d-%02d", targetYear, targetMonth);

        for (int idx = 0; idx < budgets.size(); idx++) {
            Budget b = budgets.get(idx);
            System.out.println(">>> Riga #" + idx + " -> pod=" + b.getPodId()
                    + ", anno=" + b.getAnno() + ", mese=" + b.getMese()
                    + " | base: prezzo=" + b.getPrezzoEnergiaBase()
                    + ", consumi=" + b.getConsumiBase()
                    + ", oneri=" + b.getOneriBase()
                    + " | perc: prezzo=" + b.getPrezzoEnergiaPerc()
                    + ", consumi=" + b.getConsumiPerc()
                    + ", oneri=" + b.getOneriPerc());

            double prezzoPerc  = nz(b.getPrezzoEnergiaPerc());
            double consumiPerc = nz(b.getConsumiPerc());
            double oneriPerc   = nz(b.getOneriPerc());

            double consumiBase = nz(b.getConsumiBase());
            double prezzoBase  = nz(b.getPrezzoEnergiaBase());
            double oneriBase   = nz(b.getOneriBase());

            // Consumi budget
            double totAttiva = round2(consumiBase * (1 + consumiPerc / 100.0));

            // Energia budget
            double prezzoEnergiaBudget = prezzoBase * (1 + prezzoPerc / 100.0);
            double budgetEnergia = round2(prezzoEnergiaBudget * totAttiva);

            // Oneri (split 60/40)
            double oneriTot = oneriBase * (1 + oneriPerc / 100.0);
            double budgetTrasporto = round2(oneriTot * 0.6);
            double budgetOneri     = round2(oneriTot * 0.4);

            // Imposte: stima 15% su energia
            double budgetImposte = round2(budgetEnergia * 0.15);

            // Totale
            double budgetTotale = round2(budgetEnergia + oneriTot + budgetImposte);

            BudgetBollettaDTO dto = new BudgetBollettaDTO();
            dto.id_pod           = Objects.toString(b.getPodId(), null);
            dto.Nome_Bolletta    = nomeBolletta;
            dto.TOT_Attiva       = totAttiva;
            dto.Budget_Energia   = budgetEnergia;
            dto.Budget_Trasporto = budgetTrasporto;
            dto.Budget_Oneri     = budgetOneri;
            dto.Budget_Imposte   = budgetImposte;
            dto.Budget_Totale    = budgetTotale;
            dto.Budget_Penali    = 0.0;
            dto.Budget_Altro     = 0.0;
            dto.AnnoMese         = annoMese;

            System.out.println(">>> DTO costruito -> pod=" + dto.id_pod
                    + " | TOT_Attiva=" + dto.TOT_Attiva
                    + " | Budget_Energia=" + dto.Budget_Energia
                    + " | Trasporto=" + dto.Budget_Trasporto
                    + " | Oneri=" + dto.Budget_Oneri
                    + " | Imposte=" + dto.Budget_Imposte
                    + " | Totale=" + dto.Budget_Totale
                    + " | Anno-Mese=" + dto.AnnoMese);

            out.add(dto);
        }

        System.out.println(">>> [getBudgetConsolidato] FINE. Totale DTO: " + out.size());
        return out;
    }

    private static double nz(Double d) { return d == null ? 0d : d; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
