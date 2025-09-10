package miesgroup.mies.webdev.Service.budget;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Model.budget.BudgetAll;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Service.DateUtils;

@ApplicationScoped
public class BudgetManagerService {

    @Inject
    BudgetService budgetService;
    @Inject BudgetAllService budgetAllService;

    public void gestisciBudget(BollettaPod bolletta, Cliente cliente, String mese) {
        Budget budget = new Budget();
        budget.setPodId(bolletta.getIdPod());
        budget.setAnno(Integer.parseInt(bolletta.getAnno()));
        budget.setMese(Integer.parseInt(DateUtils.getMonthNumber(mese)));
        budget.setPrezzoEnergiaBase(bolletta.getSpeseEne() != null ? bolletta.getSpeseEne() : 0.0);
        budget.setConsumiBase(bolletta.getTotAtt() != null ? bolletta.getTotAtt() : 0.0);
        budget.setOneriBase(bolletta.getOneri() != null ? bolletta.getOneri() : 0.0);
        budget.setPrezzoEnergiaPerc(0.0);
        budget.setConsumiPerc(0.0);
        budget.setOneriPerc(0.0);
        budget.setCliente(cliente);

        budgetService.creaBudget(budget);
    }

    public void gestisciBudgetAll(BollettaPod bolletta, Cliente cliente, String mese) {
        BudgetAll budgetAll = new BudgetAll();
        budgetAll.setIdPod("ALL");
        budgetAll.setCliente(cliente);
        budgetAll.setAnno(Integer.parseInt(bolletta.getAnno()));
        budgetAll.setMese(Integer.parseInt(DateUtils.getMonthNumber(mese)));

        budgetAll.setPrezzoEnergiaBase(bolletta.getSpeseEne() != null ? bolletta.getSpeseEne() : 0.0);
        budgetAll.setConsumiBase(bolletta.getTotAtt() != null ? bolletta.getTotAtt() : 0.0);
        budgetAll.setOneriBase(bolletta.getOneri() != null ? bolletta.getOneri() : 0.0);
        budgetAll.setPrezzoEnergiaPerc(0.0);
        budgetAll.setConsumiPerc(0.0);
        budgetAll.setOneriPerc(0.0);

        budgetAllService.upsertAggregato(budgetAll);
    }
}
