package miesgroup.mies.webdev.Rest.bolletta.DTO;

import java.math.BigDecimal;

public class UpdatePeriodoRequest {
    private BigDecimal costF1;
    private BigDecimal costF2;
    private BigDecimal costF3;
    private BigDecimal percentageVariable;

    // Costruttori
    public UpdatePeriodoRequest() {}

    // Getter e Setter
    public BigDecimal getCostF1() { return costF1; }
    public void setCostF1(BigDecimal costF1) { this.costF1 = costF1; }

    public BigDecimal getCostF2() { return costF2; }
    public void setCostF2(BigDecimal costF2) { this.costF2 = costF2; }

    public BigDecimal getCostF3() { return costF3; }
    public void setCostF3(BigDecimal costF3) { this.costF3 = costF3; }

    public BigDecimal getPercentageVariable() {
        return percentageVariable;
    }
    public void setPercentageVariable(BigDecimal percentageVariable) {
        this.percentageVariable = percentageVariable;
    }
}

