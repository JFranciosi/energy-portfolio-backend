package miesgroup.mies.webdev.Rest.Model;

import java.math.BigDecimal;

public class BudgetAllDto {
    private Long utenteId;
    private Integer anno;
    private Integer mese;

    private BigDecimal prezzoEnergiaBase;
    private BigDecimal consumiBase;
    private BigDecimal oneriBase;

    private BigDecimal prezzoEnergiaPerc;
    private BigDecimal consumiPerc;
    private BigDecimal oneriPerc;

    public BudgetAllDto() {}

    public Long getUtenteId() { return utenteId; }
    public void setUtenteId(Long utenteId) { this.utenteId = utenteId; }

    public Integer getAnno() { return anno; }
    public void setAnno(Integer anno) { this.anno = anno; }

    public Integer getMese() { return mese; }
    public void setMese(Integer mese) { this.mese = mese; }

    public BigDecimal getPrezzoEnergiaBase() { return prezzoEnergiaBase; }
    public void setPrezzoEnergiaBase(BigDecimal prezzoEnergiaBase) { this.prezzoEnergiaBase = prezzoEnergiaBase; }

    public BigDecimal getConsumiBase() { return consumiBase; }
    public void setConsumiBase(BigDecimal consumiBase) { this.consumiBase = consumiBase; }

    public BigDecimal getOneriBase() { return oneriBase; }
    public void setOneriBase(BigDecimal oneriBase) { this.oneriBase = oneriBase; }

    public BigDecimal getPrezzoEnergiaPerc() { return prezzoEnergiaPerc; }
    public void setPrezzoEnergiaPerc(BigDecimal prezzoEnergiaPerc) { this.prezzoEnergiaPerc = prezzoEnergiaPerc; }

    public BigDecimal getConsumiPerc() { return consumiPerc; }
    public void setConsumiPerc(BigDecimal consumiPerc) { this.consumiPerc = consumiPerc; }

    public BigDecimal getOneriPerc() { return oneriPerc; }
    public void setOneriPerc(BigDecimal oneriPerc) { this.oneriPerc = oneriPerc; }
}