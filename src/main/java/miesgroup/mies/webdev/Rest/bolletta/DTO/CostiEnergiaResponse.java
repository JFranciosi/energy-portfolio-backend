package miesgroup.mies.webdev.Rest.bolletta.DTO;

import miesgroup.mies.webdev.Model.bolletta.CostiEnergia;
import miesgroup.mies.webdev.Model.bolletta.CostiPeriodi;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

public class CostiEnergiaResponse {

    private Integer id;
    private Integer clientId;
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
    private List<CostiPeriodoDTO> costiPeriodi;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public CostiEnergiaResponse() {}

    public CostiEnergiaResponse(CostiEnergia entity) {
        this.id = entity.getId();
        this.clientId = entity.getClientId();
        this.year = entity.getYear();
        this.tipoPrezzo = entity.getTipoPrezzo();
        this.tipoTariffa = entity.getTipoTariffa();
        this.percentageVariable = entity.getPercentageVariable();
        this.costF0 = entity.getCostF0();
        this.costF1 = entity.getCostF1();
        this.costF2 = entity.getCostF2();
        this.costF3 = entity.getCostF3();
        this.spreadF1 = entity.getSpreadF1();
        this.spreadF2 = entity.getSpreadF2();
        this.spreadF3 = entity.getSpreadF3();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();

        if (entity.getCostiPeriodi() != null) {
            this.costiPeriodi = entity.getCostiPeriodi()
                    .stream()
                    .map(CostiPeriodoDTO::new)
                    .collect(Collectors.toList());
        }
    }

    // -------------------- Getter e Setter --------------------

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getTipoPrezzo() { return tipoPrezzo; }
    public void setTipoPrezzo(String tipoPrezzo) { this.tipoPrezzo = tipoPrezzo; }

    public String getTipoTariffa() { return tipoTariffa; }
    public void setTipoTariffa(String tipoTariffa) { this.tipoTariffa = tipoTariffa; }

    public BigDecimal getPercentageVariable() { return percentageVariable; }
    public void setPercentageVariable(BigDecimal percentageVariable) { this.percentageVariable = percentageVariable; }

    public BigDecimal getCostF0() { return costF0; }
    public void setCostF0(BigDecimal costF0) { this.costF0 = costF0; }

    public BigDecimal getCostF1() { return costF1; }
    public void setCostF1(BigDecimal costF1) { this.costF1 = costF1; }

    public BigDecimal getCostF2() { return costF2; }
    public void setCostF2(BigDecimal costF2) { this.costF2 = costF2; }

    public BigDecimal getCostF3() { return costF3; }
    public void setCostF3(BigDecimal costF3) { this.costF3 = costF3; }

    public BigDecimal getSpreadF1() { return spreadF1; }
    public void setSpreadF1(BigDecimal spreadF1) { this.spreadF1 = spreadF1; }

    public BigDecimal getSpreadF2() { return spreadF2; }
    public void setSpreadF2(BigDecimal spreadF2) { this.spreadF2 = spreadF2; }

    public BigDecimal getSpreadF3() { return spreadF3; }
    public void setSpreadF3(BigDecimal spreadF3) { this.spreadF3 = spreadF3; }

    public List<CostiPeriodoDTO> getCostiPeriodi() { return costiPeriodi; }
    public void setCostiPeriodi(List<CostiPeriodoDTO> costiPeriodi) { this.costiPeriodi = costiPeriodi; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // -------------------- Inner DTO --------------------

    public static class CostiPeriodoDTO {
        private Integer id;
        private Integer monthStart;
        private BigDecimal costF1;
        private BigDecimal costF2;
        private BigDecimal costF3;
        private Timestamp createdAt;

        public CostiPeriodoDTO(CostiPeriodi p) {
            this.id = p.getId();
            this.monthStart = p.getMonthStart();
            this.costF1 = p.getCostF1();
            this.costF2 = p.getCostF2();
            this.costF3 = p.getCostF3();
            this.createdAt = p.getCreatedAt();
        }

        // Getter e Setter
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public Integer getMonthStart() { return monthStart; }
        public void setMonthStart(Integer monthStart) { this.monthStart = monthStart; }

        public BigDecimal getCostF1() { return costF1; }
        public void setCostF1(BigDecimal costF1) { this.costF1 = costF1; }

        public BigDecimal getCostF2() { return costF2; }
        public void setCostF2(BigDecimal costF2) { this.costF2 = costF2; }

        public BigDecimal getCostF3() { return costF3; }
        public void setCostF3(BigDecimal costF3) { this.costF3 = costF3; }

        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    }
}
