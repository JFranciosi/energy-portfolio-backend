package miesgroup.mies.webdev.Rest.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BudgetBollettaDTO {

    @JsonProperty("IdBolletta")
    private Integer idBolletta;

    @JsonProperty("idpod")
    private String idPod;

    @JsonProperty("NomeBolletta")
    private Integer nomeBolletta;

    @JsonProperty("TOTAttiva")
    private Integer totAttiva;

    @JsonProperty("BudgetEnergia")
    private Integer budgetEnergia;

    @JsonProperty("BudgetTrasporto")
    private Integer budgetTrasporto;

    @JsonProperty("BudgetOneri")
    private Integer budgetOneri;

    @JsonProperty("BudgetImposte")
    private Integer budgetImposte;

    @JsonProperty("BudgetTotale")
    private Integer budgetTotale;

    @JsonProperty("BudgetPenali")
    private Integer budgetPenali;

    @JsonProperty("BudgetAltro")
    private Integer budgetAltro;

    @JsonProperty("Anno-Mese")
    private String annoMese;

    // Constructor
    public BudgetBollettaDTO() {}

    // Getters and Setters

    public Integer getIdBolletta() {
        return idBolletta;
    }

    public void setIdBolletta(Integer idBolletta) {
        this.idBolletta = idBolletta;
    }

    public String getIdPod() {
        return idPod;
    }

    public void setIdPod(String idPod) {
        this.idPod = idPod;
    }

    public Integer getNomeBolletta() {
        return nomeBolletta;
    }

    public void setNomeBolletta(Integer nomeBolletta) {
        this.nomeBolletta = nomeBolletta;
    }

    public Integer getTotAttiva() {
        return totAttiva;
    }

    public void setTotAttiva(Integer totAttiva) {
        this.totAttiva = totAttiva;
    }

    public Integer getBudgetEnergia() {
        return budgetEnergia;
    }

    public void setBudgetEnergia(Integer budgetEnergia) {
        this.budgetEnergia = budgetEnergia;
    }

    public Integer getBudgetTrasporto() {
        return budgetTrasporto;
    }

    public void setBudgetTrasporto(Integer budgetTrasporto) {
        this.budgetTrasporto = budgetTrasporto;
    }

    public Integer getBudgetOneri() {
        return budgetOneri;
    }

    public void setBudgetOneri(Integer budgetOneri) {
        this.budgetOneri = budgetOneri;
    }

    public Integer getBudgetImposte() {
        return budgetImposte;
    }

    public void setBudgetImposte(Integer budgetImposte) {
        this.budgetImposte = budgetImposte;
    }

    public Integer getBudgetTotale() {
        return budgetTotale;
    }

    public void setBudgetTotale(Integer budgetTotale) {
        this.budgetTotale = budgetTotale;
    }

    public Integer getBudgetPenali() {
        return budgetPenali;
    }

    public void setBudgetPenali(Integer budgetPenali) {
        this.budgetPenali = budgetPenali;
    }

    public Integer getBudgetAltro() {
        return budgetAltro;
    }

    public void setBudgetAltro(Integer budgetAltro) {
        this.budgetAltro = budgetAltro;
    }

    public String getAnnoMese() {
        return annoMese;
    }

    public void setAnnoMese(String annoMese) {
        this.annoMese = annoMese;
    }

    @Override
    public String toString() {
        return "BudgetBollettaDTO{" +
                "idBolletta=" + idBolletta +
                ", idPod='" + idPod + '\'' +
                ", nomeBolletta=" + nomeBolletta +
                ", totAttiva=" + totAttiva +
                ", budgetEnergia=" + budgetEnergia +
                ", budgetTrasporto=" + budgetTrasporto +
                ", budgetOneri=" + budgetOneri +
                ", budgetImposte=" + budgetImposte +
                ", budgetTotale=" + budgetTotale +
                ", budgetPenali=" + budgetPenali +
                ", budgetAltro=" + budgetAltro +
                ", annoMese='" + annoMese + '\'' +
                '}';
    }
}
