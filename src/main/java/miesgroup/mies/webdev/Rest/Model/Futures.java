package miesgroup.mies.webdev.Rest.Model;

public class Futures {
    private String year, quarter, month;
    private Double settlementPrice;

    public Futures() {
        this.year = "";
        this.quarter = "";
        this.month = "";
        this.settlementPrice = 0.00;
    }

    public Futures(String year, String quarter, String month, Double settlementPrice) {
        this.year = year;
        this.quarter = quarter;
        this.month = month;
        this.settlementPrice = settlementPrice;
    }

    public static Futures yearlyFutures(String year, Double settlementPrice) {
        return new Futures(year, null, null, settlementPrice);
    }

    public static Futures quarterlyFutures(String quarter, String year, Double settlementPrice) {
        return new Futures(year, quarter, null, settlementPrice);
    }

    public static Futures monthlyFutures(String month, String year, Double settlementPrice) {
        return new Futures(year, null, month, settlementPrice);
    }

    // Getter e Setter
    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getQuarter() {
        return quarter;
    }

    public void setQuarter(String quarter) {
        this.quarter = quarter;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Double getSettlementPrice() {
        return settlementPrice;
    }

    public void setSettlementPrice(Double settlementPrice) {
        this.settlementPrice = settlementPrice;
    }

    @Override
    public String toString() {
        return "Futures{" +
                "year='" + year + '\'' +
                ", quarter='" + quarter + '\'' +
                ", month='" + month + '\'' +
                ", settlementPrice=" + settlementPrice +
                '}';
    }
}