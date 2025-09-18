package miesgroup.mies.webdev.Rest.bolletta.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import miesgroup.mies.webdev.Model.bolletta.CostiEnergia;
import miesgroup.mies.webdev.Model.bolletta.CostiPeriodi;

import java.math.BigDecimal;
import java.util.List;

public class CostiEnergiaRequest {

    @NotNull
    private Integer clientId;

    @NotNull
    @Min(2020)
    @Max(2050)
    private Integer year;

    private String tipoPrezzo;
    private String tipoTariffa;
    private BigDecimal percentageVariable;
    private BigDecimal costF0;
    private BigDecimal costF1;
    private BigDecimal costF2;
    private BigDecimal costF3;
    private BigDecimal spreadF1;
    private BigDecimal spreadF2;
    private BigDecimal spreadF3;
    private List<CostiPeriodi> costiPeriodi;

    // Costruttori
    public CostiEnergiaRequest() {}

    // Getter e Setter
    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getTipoPrezzo() { return tipoPrezzo; }
    public void setTipoPrezzo(String tipoPrezzo) { this.tipoPrezzo = tipoPrezzo; }

    public CostiEnergia toEntity() {
        CostiEnergia entity = new CostiEnergia();
        entity.setClientId(this.clientId);
        entity.setYear(this.year);
        entity.setTipoPrezzo(this.tipoPrezzo);
        entity.setTipoTariffa(this.tipoTariffa);
        entity.setPercentageVariable(this.percentageVariable);
        entity.setCostF0(this.costF0);
        entity.setCostF1(this.costF1);
        entity.setCostF2(this.costF2);
        entity.setCostF3(this.costF3);
        entity.setSpreadF1(this.spreadF1);
        entity.setSpreadF2(this.spreadF2);
        entity.setSpreadF3(this.spreadF3);
        entity.setCostiPeriodi(this.costiPeriodi);
        return entity;
    }

    public BigDecimal getCostF3() {
        return costF3;
    }

    public void setCostF3(BigDecimal costF3) {
        this.costF3 = costF3;
    }

    public String getTipoTariffa() {
        return tipoTariffa;
    }

    public void setTipoTariffa(String tipoTariffa) {
        this.tipoTariffa = tipoTariffa;
    }

    public BigDecimal getPercentageVariable() {
        return percentageVariable;
    }

    public void setPercentageVariable(BigDecimal percentageVariable) {
        this.percentageVariable = percentageVariable;
    }

    public BigDecimal getCostF0() {
        return costF0;
    }

    public void setCostF0(BigDecimal costF0) {
        this.costF0 = costF0;
    }

    public BigDecimal getCostF1() {
        return costF1;
    }

    public void setCostF1(BigDecimal costF1) {
        this.costF1 = costF1;
    }

    public BigDecimal getCostF2() {
        return costF2;
    }

    public void setCostF2(BigDecimal costF2) {
        this.costF2 = costF2;
    }

    public BigDecimal getSpreadF1() {
        return spreadF1;
    }

    public void setSpreadF1(BigDecimal spreadF1) {
        this.spreadF1 = spreadF1;
    }

    public BigDecimal getSpreadF2() {
        return spreadF2;
    }

    public void setSpreadF2(BigDecimal spreadF2) {
        this.spreadF2 = spreadF2;
    }

    public BigDecimal getSpreadF3() {
        return spreadF3;
    }

    public void setSpreadF3(BigDecimal spreadF3) {
        this.spreadF3 = spreadF3;
    }

    public List<CostiPeriodi> getCostiPeriodi() {
        return costiPeriodi;
    }

    public void setCostiPeriodi(List<CostiPeriodi> costiPeriodi) {
        this.costiPeriodi = costiPeriodi;
    }
}

