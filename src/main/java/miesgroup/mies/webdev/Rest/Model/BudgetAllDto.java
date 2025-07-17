package miesgroup.mies.webdev.Rest.Model;

public class BudgetAllDto {
    private long utenteId;
    private Integer anno;
    private Integer mese;

    private Double prezzoEnergiaBase;
    private Double consumiBase;
    private Double oneriBase;

    private Double prezzoEnergiaPerc;
    private Double consumiPerc;
    private Double oneriPerc;

    public BudgetAllDto() {}

    public Long getUtenteId() {
        return utenteId;
    }

    public void setUtenteId(Long utenteId) {
        this.utenteId = utenteId;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    public Integer getMese() {
        return mese;
    }

    public void setMese(Integer mese) {
        this.mese = mese;
    }

    public Double getPrezzoEnergiaBase() {
        return prezzoEnergiaBase;
    }

    public void setPrezzoEnergiaBase(Double prezzoEnergiaBase) {
        this.prezzoEnergiaBase = prezzoEnergiaBase;
    }

    public Double getConsumiBase() {
        return consumiBase;
    }

    public void setConsumiBase(Double consumiBase) {
        this.consumiBase = consumiBase;
    }

    public Double getOneriBase() {
        return oneriBase;
    }

    public void setOneriBase(Double oneriBase) {
        this.oneriBase = oneriBase;
    }

    public Double getPrezzoEnergiaPerc() {
        return prezzoEnergiaPerc;
    }

    public void setPrezzoEnergiaPerc(Double prezzoEnergiaPerc) {
        this.prezzoEnergiaPerc = prezzoEnergiaPerc;
    }

    public Double getConsumiPerc() {
        return consumiPerc;
    }

    public void setConsumiPerc(Double consumiPerc) {
        this.consumiPerc = consumiPerc;
    }

    public Double getOneriPerc() {
        return oneriPerc;
    }

    public void setOneriPerc(Double oneriPerc) {
        this.oneriPerc = oneriPerc;
    }
}
