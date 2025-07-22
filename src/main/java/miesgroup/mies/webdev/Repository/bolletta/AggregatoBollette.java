package miesgroup.mies.webdev.Repository.bolletta;

public class AggregatoBollette {
    private Double consumoTotale;      // deve corrispondere a SUM(totAttiva)
    private Double spesaEnergiaTotale; // deve corrispondere a SUM(speseEnergia)
    private Double oneriTotale;        // opzionale

    public AggregatoBollette(Double consumoTotale, Double spesaEnergiaTotale, Double oneriTotale) {
        this.consumoTotale = consumoTotale;
        this.spesaEnergiaTotale = spesaEnergiaTotale;
        this.oneriTotale = oneriTotale;
    }

    public Double getConsumoTotale() {
        return consumoTotale;
    }

    public Double getSpesaEnergiaTotale() {
        return spesaEnergiaTotale;
    }

    public Double getOneriTotale() {
        return oneriTotale;
    }
}
