// ConsumoDto.java
package miesgroup.mies.webdev.Rest.Model;

import java.math.BigDecimal;

public class ConsumoDto {
    private int mese;
    private BigDecimal prezzoEnergia; // media fasce

    public ConsumoDto() {}

    public ConsumoDto(int mese, BigDecimal prezzoEnergia) {
        this.mese = mese;
        this.prezzoEnergia = prezzoEnergia;
    }

    public int getMese() { return mese; }
    public void setMese(int mese) { this.mese = mese; }

    public BigDecimal getPrezzoEnergia() { return prezzoEnergia; }
    public void setPrezzoEnergia(BigDecimal prezzoEnergia) { this.prezzoEnergia = prezzoEnergia; }
}
