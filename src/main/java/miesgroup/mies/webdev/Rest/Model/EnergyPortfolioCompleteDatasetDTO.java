package miesgroup.mies.webdev.Rest.Model;

import java.util.List;

public class EnergyPortfolioCompleteDatasetDTO {
    private String name = "EnergyPortfolio_Complete_Dataset";
    private String defaultMode = "Push";
    private List<BudgetBollettaDTO> Budget;
    private List<CalendarioDTO> calendario;
    private List<BollettaPodDTO> bolletta_pod;

    // Constructor e getters/setters
    public EnergyPortfolioCompleteDatasetDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDefaultMode() { return defaultMode; }
    public void setDefaultMode(String defaultMode) { this.defaultMode = defaultMode; }

    public List<BudgetBollettaDTO> getBudget() { return Budget; }
    public void setBudget(List<BudgetBollettaDTO> budget) { this.Budget = budget; }

    public List<CalendarioDTO> getCalendario() { return calendario; }
    public void setCalendario(List<CalendarioDTO> calendario) { this.calendario = calendario; }

    public List<BollettaPodDTO> getBolletta_pod() { return bolletta_pod; }
    public void setBolletta_pod(List<BollettaPodDTO> bolletta_pod) { this.bolletta_pod = bolletta_pod; }
}
