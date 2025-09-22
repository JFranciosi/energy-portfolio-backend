package miesgroup.mies.webdev.Rest.Model;

public class BudgetDtoPBI {
    private String idBolletta;
    private String idPod;
    private String nomeBolletta;
    private Double totAttiva;
    private Double budgetEnergia;
    private Double budgetTrasporto;
    private Double budgetOneri;
    private Double budgetImposte;
    private Double budgetTotale;
    private Double budgetPenali;
    private Double budgetAltro;
    private String annoMese;

    public BudgetDtoPBI() {}

    // --- Getters & Setters ---
    public String getIdBolletta() {
        return idBolletta;
    }
    public void setIdBolletta(String idBolletta) {
        this.idBolletta = idBolletta;
    }

    public String getIdPod() {
        return idPod;
    }
    public void setIdPod(String idPod) {
        this.idPod = idPod;
    }

    public String getNomeBolletta() {
        return nomeBolletta;
    }
    public void setNomeBolletta(String nomeBolletta) {
        this.nomeBolletta = nomeBolletta;
    }

    public Double getTotAttiva() {
        return totAttiva;
    }
    public void setTotAttiva(Double totAttiva) {
        this.totAttiva = totAttiva;
    }

    public Double getBudgetEnergia() {
        return budgetEnergia;
    }
    public void setBudgetEnergia(Double budgetEnergia) {
        this.budgetEnergia = budgetEnergia;
    }

    public Double getBudgetTrasporto() {
        return budgetTrasporto;
    }
    public void setBudgetTrasporto(Double budgetTrasporto) {
        this.budgetTrasporto = budgetTrasporto;
    }

    public Double getBudgetOneri() {
        return budgetOneri;
    }
    public void setBudgetOneri(Double budgetOneri) {
        this.budgetOneri = budgetOneri;
    }

    public Double getBudgetImposte() {
        return budgetImposte;
    }
    public void setBudgetImposte(Double budgetImposte) {
        this.budgetImposte = budgetImposte;
    }

    public Double getBudgetTotale() {
        return budgetTotale;
    }
    public void setBudgetTotale(Double budgetTotale) {
        this.budgetTotale = budgetTotale;
    }

    public Double getBudgetPenali() {
        return budgetPenali;
    }
    public void setBudgetPenali(Double budgetPenali) {
        this.budgetPenali = budgetPenali;
    }

    public Double getBudgetAltro() {
        return budgetAltro;
    }
    public void setBudgetAltro(Double budgetAltro) {
        this.budgetAltro = budgetAltro;
    }

    public String getAnnoMese() {
        return annoMese;
    }
    public void setAnnoMese(String annoMese) {
        this.annoMese = annoMese;
    }
}
