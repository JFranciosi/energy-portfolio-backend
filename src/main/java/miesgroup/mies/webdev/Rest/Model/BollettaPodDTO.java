package miesgroup.mies.webdev.Rest.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BollettaPodDTO {

    @JsonProperty("IdBolletta")
    private Double IdBolletta;

    @JsonProperty("idpod")
    private String idpod;

    @JsonProperty("NomeBolletta")
    private Double NomeBolletta;

    @JsonProperty("F1Attiva")
    private Double F1Attiva;

    @JsonProperty("F2Attiva")
    private Double F2Attiva;

    @JsonProperty("F3Attiva")
    private Double F3Attiva;

    @JsonProperty("F1Reattiva")
    private Double F1Reattiva;

    @JsonProperty("F2Reattiva")
    private Double F2Reattiva;

    @JsonProperty("F3Reattiva")
    private Double F3Reattiva;

    @JsonProperty("F1Potenza")
    private Double F1Potenza;

    @JsonProperty("F2Potenza")
    private Double F2Potenza;

    @JsonProperty("F3Potenza")
    private Double F3Potenza;

    @JsonProperty("SpeseEnergia")
    private Double SpeseEnergia;

    @JsonProperty("SpeseTrasporto")
    private Double SpeseTrasporto;

    @JsonProperty("Oneri")
    private Double Oneri;

    @JsonProperty("Imposte")
    private Double Imposte;

    @JsonProperty("PeriodoInizio")
    private String PeriodoInizio;

    @JsonProperty("PeriodoFine")
    private String PeriodoFine;

    @JsonProperty("Mese")
    private String Mese;

    @JsonProperty("TOTAttiva")
    private Double TOTAttiva;

    @JsonProperty("TOTReattiva")
    private Double TOTReattiva;

    @JsonProperty("Generation")
    private Double Generation;

    @JsonProperty("Dispacciamento")
    private Double Dispacciamento;

    @JsonProperty("VerificaTrasporti")
    private Double VerificaTrasporti;

    @JsonProperty("Penali33")
    private Double Penali33;

    @JsonProperty("Penali75")
    private Double Penali75;

    @JsonProperty("VerificaOneri")
    private Double VerificaOneri;

    @JsonProperty("VerificaImposte")
    private Double VerificaImposte;

    @JsonProperty("Altro")
    private Double Altro;

    @JsonProperty("Anno")
    private Double Anno;

    @JsonProperty("piccokwh")
    private Double piccokwh;

    @JsonProperty("fuoripiccokwh")
    private Double fuoripiccokwh;

    @JsonProperty("picco")
    private Double picco;

    @JsonProperty("fuoripicco")
    private Double fuoripicco;

    @JsonProperty("verificapicco")
    private Double verificapicco;

    @JsonProperty("verificafuoripicco")
    private Double verificafuoripicco;

    @JsonProperty("Anno-Mese")
    private String AnnoMese;

    // Constructor
    public BollettaPodDTO() {}

    // Getters and Setters
    public Double getIdBolletta() {
        return IdBolletta;
    }

    public void setIdBolletta(Double idBolletta) {
        this.IdBolletta = idBolletta;
    }

    public String getIdpod() {
        return idpod;
    }

    public void setIdpod(String idpod) {
        this.idpod = idpod;
    }

    public Double getNomeBolletta() {
        return NomeBolletta;
    }

    public void setNomeBolletta(Double nomeBolletta) {
        this.NomeBolletta = nomeBolletta;
    }

    public Double getF1Attiva() {
        return F1Attiva;
    }

    public void setF1Attiva(Double f1Attiva) {
        this.F1Attiva = f1Attiva;
    }

    public Double getF2Attiva() {
        return F2Attiva;
    }

    public void setF2Attiva(Double f2Attiva) {
        this.F2Attiva = f2Attiva;
    }

    public Double getF3Attiva() {
        return F3Attiva;
    }

    public void setF3Attiva(Double f3Attiva) {
        this.F3Attiva = f3Attiva;
    }

    public Double getF1Reattiva() {
        return F1Reattiva;
    }

    public void setF1Reattiva(Double f1Reattiva) {
        this.F1Reattiva = f1Reattiva;
    }

    public Double getF2Reattiva() {
        return F2Reattiva;
    }

    public void setF2Reattiva(Double f2Reattiva) {
        this.F2Reattiva = f2Reattiva;
    }

    public Double getF3Reattiva() {
        return F3Reattiva;
    }

    public void setF3Reattiva(Double f3Reattiva) {
        this.F3Reattiva = f3Reattiva;
    }

    public Double getF1Potenza() {
        return F1Potenza;
    }

    public void setF1Potenza(Double f1Potenza) {
        this.F1Potenza = f1Potenza;
    }

    public Double getF2Potenza() {
        return F2Potenza;
    }

    public void setF2Potenza(Double f2Potenza) {
        this.F2Potenza = f2Potenza;
    }

    public Double getF3Potenza() {
        return F3Potenza;
    }

    public void setF3Potenza(Double f3Potenza) {
        this.F3Potenza = f3Potenza;
    }

    public Double getSpeseEnergia() {
        return SpeseEnergia;
    }

    public void setSpeseEnergia(Double speseEnergia) {
        this.SpeseEnergia = speseEnergia;
    }

    public Double getSpeseTrasporto() {
        return SpeseTrasporto;
    }

    public void setSpeseTrasporto(Double speseTrasporto) {
        this.SpeseTrasporto = speseTrasporto;
    }

    public Double getOneri() {
        return Oneri;
    }

    public void setOneri(Double oneri) {
        this.Oneri = oneri;
    }

    public Double getImposte() {
        return Imposte;
    }

    public void setImposte(Double imposte) {
        this.Imposte = imposte;
    }

    public String getPeriodoInizio() {
        return PeriodoInizio;
    }

    public void setPeriodoInizio(String periodoInizio) {
        this.PeriodoInizio = periodoInizio;
    }

    public String getPeriodoFine() {
        return PeriodoFine;
    }

    public void setPeriodoFine(String periodoFine) {
        this.PeriodoFine = periodoFine;
    }

    public String getMese() {
        return Mese;
    }

    public void setMese(String mese) {
        this.Mese = mese;
    }

    public Double getTOTAttiva() {
        return TOTAttiva;
    }

    public void setTOTAttiva(Double TOTAttiva) {
        this.TOTAttiva = TOTAttiva;
    }

    public Double getTOTReattiva() {
        return TOTReattiva;
    }

    public void setTOTReattiva(Double TOTReattiva) {
        this.TOTReattiva = TOTReattiva;
    }

    public Double getGeneration() {
        return Generation;
    }

    public void setGeneration(Double generation) {
        this.Generation = generation;
    }

    public Double getDispackciamento() {
        return Dispacciamento;
    }

    public void setDispackciamento(Double dispacciamento) {
        this.Dispacciamento = dispacciamento;
    }

    public Double getVerificaTrasporti() {
        return VerificaTrasporti;
    }

    public void setVerificaTrasporti(Double verificaTrasporti) {
        this.VerificaTrasporti = verificaTrasporti;
    }

    public Double getPenali33() {
        return Penali33;
    }

    public void setPenali33(Double penali33) {
        this.Penali33 = penali33;
    }

    public Double getPenali75() {
        return Penali75;
    }

    public void setPenali75(Double penali75) {
        this.Penali75 = penali75;
    }

    public Double getVerificaOneri() {
        return VerificaOneri;
    }

    public void setVerificaOneri(Double verificaOneri) {
        this.VerificaOneri = verificaOneri;
    }

    public Double getVerificaImposte() {
        return VerificaImposte;
    }

    public void setVerificaImposte(Double verificaImposte) {
        this.VerificaImposte = verificaImposte;
    }

    public Double getAltro() {
        return Altro;
    }

    public void setAltro(Double altro) {
        this.Altro = altro;
    }

    public Double getAnno() {
        return Anno;
    }

    public void setAnno(Double anno) {
        this.Anno = anno;
    }

    public Double getPiccokwh() {
        return piccokwh;
    }

    public void setPiccokwh(Double piccokwh) {
        this.piccokwh = piccokwh;
    }

    public Double getFuoripiccokwh() {
        return fuoripiccokwh;
    }

    public void setFuoripiccokwh(Double fuoripiccokwh) {
        this.fuoripiccokwh = fuoripiccokwh;
    }

    public Double getPicco() {
        return picco;
    }

    public void setPicco(Double picco) {
        this.picco = picco;
    }

    public Double getFuoripicco() {
        return fuoripicco;
    }

    public void setFuoripicco(Double fuoripicco) {
        this.fuoripicco = fuoripicco;
    }

    public Double getVerificapicco() {
        return verificapicco;
    }

    public void setVerificapicco(Double verificapicco) {
        this.verificapicco = verificapicco;
    }

    public Double getVerificafuoripicco() {
        return verificafuoripicco;
    }

    public void setVerificafuoripicco(Double verificafuoripicco) {
        this.verificafuoripicco = verificafuoripicco;
    }

    public String getAnnoMese() {
        return AnnoMese;
    }

    public void setAnnoMese(String annoMese) {
        this.AnnoMese = annoMese;
    }

    @Override
    public String toString() {
        return "BollettaPodDTO{" +
                "IdBolletta=" + IdBolletta +
                ", idpod='" + idpod + '\'' +
                ", NomeBolletta=" + NomeBolletta +
                ", F1Attiva=" + F1Attiva +
                ", F2Attiva=" + F2Attiva +
                ", F3Attiva=" + F3Attiva +
                ", F1Reattiva=" + F1Reattiva +
                ", F2Reattiva=" + F2Reattiva +
                ", F3Reattiva=" + F3Reattiva +
                ", F1Potenza=" + F1Potenza +
                ", F2Potenza=" + F2Potenza +
                ", F3Potenza=" + F3Potenza +
                ", SpeseEnergia=" + SpeseEnergia +
                ", SpeseTrasporto=" + SpeseTrasporto +
                ", Oneri=" + Oneri +
                ", Imposte=" + Imposte +
                ", PeriodoInizio='" + PeriodoInizio + '\'' +
                ", PeriodoFine='" + PeriodoFine + '\'' +
                ", Mese='" + Mese + '\'' +
                ", TOTAttiva=" + TOTAttiva +
                ", TOTReattiva=" + TOTReattiva +
                ", Generation=" + Generation +
                ", Dispacciamento=" + Dispacciamento +
                ", VerificaTrasporti=" + VerificaTrasporti +
                ", Penali33=" + Penali33 +
                ", Penali75=" + Penali75 +
                ", VerificaOneri=" + VerificaOneri +
                ", VerificaImposte=" + VerificaImposte +
                ", Altro=" + Altro +
                ", Anno=" + Anno +
                ", piccokwh=" + piccokwh +
                ", fuoripiccokwh=" + fuoripiccokwh +
                ", picco=" + picco +
                ", fuoripicco=" + fuoripicco +
                ", verificapicco=" + verificapicco +
                ", verificafuoripicco=" + verificafuoripicco +
                ", AnnoMese='" + AnnoMese + '\'' +
                '}';
    }
}
