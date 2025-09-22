package miesgroup.mies.webdev.Rest.Model;

public class BudgetBollettaDTO {
    public Integer Id_Bolletta;
    public String  id_pod;
    public String  Nome_Bolletta;

    public Double  TOT_Attiva;
    public Double  Budget_Energia;
    public Double  Budget_Trasporto;
    public Double  Budget_Oneri;
    public Double  Budget_Imposte;
    public Double  Budget_Totale;
    public Double  Budget_Penali;
    public Double  Budget_Altro;

    // 👉 aggiunti per filtro/report
    public Integer Anno;
    public Integer Mese;

    public String  AnnoMese;

    // --- getters/setters ---

    public Integer getId_Bolletta() { return Id_Bolletta; }
    public void setId_Bolletta(Integer id_Bolletta) { Id_Bolletta = id_Bolletta; }

    public String getId_pod() { return id_pod; }
    public void setId_pod(String id_pod) { this.id_pod = id_pod; }

    public String getNome_Bolletta() { return Nome_Bolletta; }
    public void setNome_Bolletta(String nome_Bolletta) { Nome_Bolletta = nome_Bolletta; }

    public Double getTOT_Attiva() { return TOT_Attiva; }
    public void setTOT_Attiva(Double TOT_Attiva) { this.TOT_Attiva = TOT_Attiva; }

    public Double getBudget_Energia() { return Budget_Energia; }
    public void setBudget_Energia(Double budget_Energia) { Budget_Energia = budget_Energia; }

    public Double getBudget_Trasporto() { return Budget_Trasporto; }
    public void setBudget_Trasporto(Double budget_Trasporto) { Budget_Trasporto = budget_Trasporto; }

    public Double getBudget_Oneri() { return Budget_Oneri; }
    public void setBudget_Oneri(Double budget_Oneri) { Budget_Oneri = budget_Oneri; }

    public Double getBudget_Imposte() { return Budget_Imposte; }
    public void setBudget_Imposte(Double budget_Imposte) { Budget_Imposte = budget_Imposte; }

    public Double getBudget_Totale() { return Budget_Totale; }
    public void setBudget_Totale(Double budget_Totale) { Budget_Totale = budget_Totale; }

    public Double getBudget_Penali() { return Budget_Penali; }
    public void setBudget_Penali(Double budget_Penali) { Budget_Penali = budget_Penali; }

    public Double getBudget_Altro() { return Budget_Altro; }
    public void setBudget_Altro(Double budget_Altro) { Budget_Altro = budget_Altro; }

    public Integer getAnno() { return Anno; }
    public void setAnno(Integer anno) { Anno = anno; }

    public Integer getMese() { return Mese; }
    public void setMese(Integer mese) { Mese = mese; }

    public String getAnnoMese() { return AnnoMese; }
    public void setAnnoMese(String annoMese) { AnnoMese = annoMese; }
}
