package miesgroup.mies.webdev.Rest.Model;

import miesgroup.mies.webdev.Model.BollettaPod;

import java.sql.Date;

public class BollettaPodResponse {

    private Integer id;
    private String idPod;
    private String nomeBolletta;
    private Double f1A, f2A, f3A;
    private Double f1R, f2R, f3R;
    private Double f1P, f2P, f3P;
    private Double f0Euro, f1Euro, f2Euro, f3Euro;
    private Double f1PerditeEuro, f2PerditeEuro, f3PerditeEuro;
    private Double f0Kwh, f1Kwh, f2Kwh, f3Kwh;
    private Double f1PerditeKwh, f2PerditeKwh, f3PerditeKwh;
    private Double verificaSpesaMateriaEnergia;
    private Double verificaF1, verificaF2, verificaF3;
    private Double verificaF1Perdite, verificaF2Perdite, verificaF3Perdite, verificaF0;
    private Double totAttiva, totReattiva;
    private Double speseEnergia, trasporti, oneri, imposte, generation, dispacciamento;
    private Double verificaDispacciamento, penali33, penali75, altro;
    private Date periodoInizio, periodoFine;
    private String anno, mese;
    private Double verificaOneri, verificaTrasporti, verificaImposte;
    private Double piccoKwh, fuoriPiccoKwh, costoPicco, costoFuoriPicco;
    private Double verificaPicco, verificaFuoriPicco, totAttivaPerdite;
    private Double quotaVariabileTrasporti, quotaFissaTrasporti, quotaPotenzaTrasporti;
    private Double quotaEnergiaOneri, quotaFissaOneri, quotaPotenzaOneri;
    private String meseAnno;

    // Costruttori, getter e setter generati automaticamente oppure con Lombok


    public BollettaPodResponse(BollettaPod b) {
        this.id = b.getId();
        this.idPod = b.getIdPod();
        this.nomeBolletta = b.getNomeBolletta();
        this.f1A = b.getF1A();
        this.f2A = b.getF2A();
        this.f3A = b.getF3A();
        this.f1R = b.getF1R();
        this.f2R = b.getF2R();
        this.f3R = b.getF3R();
        this.f1P = b.getF1P();
        this.f2P = b.getF2P();
        this.f3P = b.getF3P();
        this.f0Euro = b.getF0Euro();
        this.f1Euro = b.getF1Euro();
        this.f2Euro = b.getF2Euro();
        this.f3Euro = b.getF3Euro();
        this.f1PerditeEuro = b.getF1PerditeEuro();
        this.f2PerditeEuro = b.getF2PerditeEuro();
        this.f3PerditeEuro = b.getF3PerditeEuro();
        this.f0Kwh = b.getF0Kwh();
        this.f1Kwh = b.getF1Kwh();
        this.f2Kwh = b.getF2Kwh();
        this.f3Kwh = b.getF3Kwh();
        this.f1PerditeKwh = b.getF1PerditeKwh();
        this.f2PerditeKwh = b.getF2PerditeKwh();
        this.f3PerditeKwh = b.getF3PerditeKwh();
        this.verificaSpesaMateriaEnergia = b.getVerificaSpesaMateriaEnergia();
        this.verificaF1 = b.getVerificaF1();
        this.verificaF2 = b.getVerificaF2();
        this.verificaF3 = b.getVerificaF3();
        this.verificaF1Perdite = b.getVerificaF1Perdite();
        this.verificaF2Perdite = b.getVerificaF2Perdite();
        this.verificaF3Perdite = b.getVerificaF3Perdite();
        this.verificaF0 = b.getVerificaF0();
        this.totAttiva = b.getTotAttiva();
        this.totReattiva = b.getTotReattiva();
        this.speseEnergia = b.getSpeseEnergia();
        this.trasporti = b.getTrasporti();
        this.oneri = b.getOneri();
        this.imposte = b.getImposte();
        this.generation = b.getGeneration();
        this.dispacciamento = b.getDispacciamento();
        this.verificaDispacciamento = b.getVerificaDispacciamento();
        this.penali33 = b.getF1Penale33();
        this.penali75 = b.getF2Penale33();
        this.altro = b.getAltro();
        this.periodoInizio = b.getPeriodoInizio();
        this.periodoFine = b.getPeriodoFine();
        this.anno = b.getAnno();
        this.mese = b.getMese();
        this.verificaOneri = b.getVerificaOneri();
        this.verificaTrasporti = b.getVerificaTrasporti();
        this.verificaImposte = b.getVerificaImposte();
        this.piccoKwh = b.getPiccoKwh();
        this.fuoriPiccoKwh = b.getFuoriPiccoKwh();
        this.costoPicco = b.getCostoPicco();
        this.costoFuoriPicco = b.getCostoFuoriPicco();
        this.verificaPicco = b.getVerificaPicco();
        this.verificaFuoriPicco = b.getVerificaFuoriPicco();
        this.totAttivaPerdite = b.getTotAttivaPerdite();
        this.quotaVariabileTrasporti = b.getQuotaVariabileTrasporti();
        this.quotaFissaTrasporti = b.getQuotaFissaTrasporti();
        this.quotaPotenzaTrasporti = b.getQuotaPotenzaTrasporti();
        this.quotaEnergiaOneri = b.getQuotaEnergiaOneri();
        this.quotaFissaOneri = b.getQuotaFissaOneri();
        this.quotaPotenzaOneri = b.getQuotaPotenzaOneri();
        this.meseAnno = b.getMeseAnno();
    }


    public String getMeseAnno() {
        return meseAnno;
    }

    public void setMeseAnno(String meseAnno) {
        this.meseAnno = meseAnno;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdPod() {
        return idPod;
    }

    public void setIdPod(String idPod) {
        this.idPod = idPod;
    }

    public String getNomeBolletta() {
        return nomeBolletta;
    }

    public void setNomeBolletta(String nomeBolletta) {
        this.nomeBolletta = nomeBolletta;
    }

    public Double getF1A() {
        return f1A;
    }

    public void setF1A(Double f1A) {
        this.f1A = f1A;
    }

    public Double getF2A() {
        return f2A;
    }

    public void setF2A(Double f2A) {
        this.f2A = f2A;
    }

    public Double getF3A() {
        return f3A;
    }

    public void setF3A(Double f3A) {
        this.f3A = f3A;
    }

    public Double getF1R() {
        return f1R;
    }

    public void setF1R(Double f1R) {
        this.f1R = f1R;
    }

    public Double getF2R() {
        return f2R;
    }

    public void setF2R(Double f2R) {
        this.f2R = f2R;
    }

    public Double getF3R() {
        return f3R;
    }

    public void setF3R(Double f3R) {
        this.f3R = f3R;
    }

    public Double getF1P() {
        return f1P;
    }

    public void setF1P(Double f1P) {
        this.f1P = f1P;
    }

    public Double getF2P() {
        return f2P;
    }

    public void setF2P(Double f2P) {
        this.f2P = f2P;
    }

    public Double getF3P() {
        return f3P;
    }

    public void setF3P(Double f3P) {
        this.f3P = f3P;
    }

    public Double getF0Euro() {
        return f0Euro;
    }

    public void setF0Euro(Double f0Euro) {
        this.f0Euro = f0Euro;
    }

    public Double getF1Euro() {
        return f1Euro;
    }

    public void setF1Euro(Double f1Euro) {
        this.f1Euro = f1Euro;
    }

    public Double getF2Euro() {
        return f2Euro;
    }

    public void setF2Euro(Double f2Euro) {
        this.f2Euro = f2Euro;
    }

    public Double getF3Euro() {
        return f3Euro;
    }

    public void setF3Euro(Double f3Euro) {
        this.f3Euro = f3Euro;
    }

    public Double getF1PerditeEuro() {
        return f1PerditeEuro;
    }

    public void setF1PerditeEuro(Double f1PerditeEuro) {
        this.f1PerditeEuro = f1PerditeEuro;
    }

    public Double getF2PerditeEuro() {
        return f2PerditeEuro;
    }

    public void setF2PerditeEuro(Double f2PerditeEuro) {
        this.f2PerditeEuro = f2PerditeEuro;
    }

    public Double getF3PerditeEuro() {
        return f3PerditeEuro;
    }

    public void setF3PerditeEuro(Double f3PerditeEuro) {
        this.f3PerditeEuro = f3PerditeEuro;
    }

    public Double getF0Kwh() {
        return f0Kwh;
    }

    public void setF0Kwh(Double f0Kwh) {
        this.f0Kwh = f0Kwh;
    }

    public Double getF1Kwh() {
        return f1Kwh;
    }

    public void setF1Kwh(Double f1Kwh) {
        this.f1Kwh = f1Kwh;
    }

    public Double getF2Kwh() {
        return f2Kwh;
    }

    public void setF2Kwh(Double f2Kwh) {
        this.f2Kwh = f2Kwh;
    }

    public Double getF3Kwh() {
        return f3Kwh;
    }

    public void setF3Kwh(Double f3Kwh) {
        this.f3Kwh = f3Kwh;
    }

    public Double getF1PerditeKwh() {
        return f1PerditeKwh;
    }

    public void setF1PerditeKwh(Double f1PerditeKwh) {
        this.f1PerditeKwh = f1PerditeKwh;
    }

    public Double getF2PerditeKwh() {
        return f2PerditeKwh;
    }

    public void setF2PerditeKwh(Double f2PerditeKwh) {
        this.f2PerditeKwh = f2PerditeKwh;
    }

    public Double getF3PerditeKwh() {
        return f3PerditeKwh;
    }

    public void setF3PerditeKwh(Double f3PerditeKwh) {
        this.f3PerditeKwh = f3PerditeKwh;
    }

    public Double getVerificaSpesaMateriaEnergia() {
        return verificaSpesaMateriaEnergia;
    }

    public void setVerificaSpesaMateriaEnergia(Double verificaSpesaMateriaEnergia) {
        this.verificaSpesaMateriaEnergia = verificaSpesaMateriaEnergia;
    }

    public Double getVerificaF1() {
        return verificaF1;
    }

    public void setVerificaF1(Double verificaF1) {
        this.verificaF1 = verificaF1;
    }

    public Double getVerificaF2() {
        return verificaF2;
    }

    public void setVerificaF2(Double verificaF2) {
        this.verificaF2 = verificaF2;
    }

    public Double getVerificaF3() {
        return verificaF3;
    }

    public void setVerificaF3(Double verificaF3) {
        this.verificaF3 = verificaF3;
    }

    public Double getVerificaF1Perdite() {
        return verificaF1Perdite;
    }

    public void setVerificaF1Perdite(Double verificaF1Perdite) {
        this.verificaF1Perdite = verificaF1Perdite;
    }

    public Double getVerificaF2Perdite() {
        return verificaF2Perdite;
    }

    public void setVerificaF2Perdite(Double verificaF2Perdite) {
        this.verificaF2Perdite = verificaF2Perdite;
    }

    public Double getVerificaF3Perdite() {
        return verificaF3Perdite;
    }

    public void setVerificaF3Perdite(Double verificaF3Perdite) {
        this.verificaF3Perdite = verificaF3Perdite;
    }

    public Double getVerificaF0() {
        return verificaF0;
    }

    public void setVerificaF0(Double verificaF0) {
        this.verificaF0 = verificaF0;
    }

    public Double getTotAttiva() {
        return totAttiva;
    }

    public void setTotAttiva(Double totAttiva) {
        this.totAttiva = totAttiva;
    }

    public Double getTotReattiva() {
        return totReattiva;
    }

    public void setTotReattiva(Double totReattiva) {
        this.totReattiva = totReattiva;
    }

    public Double getSpeseEnergia() {
        return speseEnergia;
    }

    public void setSpeseEnergia(Double speseEnergia) {
        this.speseEnergia = speseEnergia;
    }

    public Double getTrasporti() {
        return trasporti;
    }

    public void setTrasporti(Double trasporti) {
        this.trasporti = trasporti;
    }

    public Double getOneri() {
        return oneri;
    }

    public void setOneri(Double oneri) {
        this.oneri = oneri;
    }

    public Double getImposte() {
        return imposte;
    }

    public void setImposte(Double imposte) {
        this.imposte = imposte;
    }

    public Double getGeneration() {
        return generation;
    }

    public void setGeneration(Double generation) {
        this.generation = generation;
    }

    public Double getDispacciamento() {
        return dispacciamento;
    }

    public void setDispacciamento(Double dispacciamento) {
        this.dispacciamento = dispacciamento;
    }

    public Double getVerificaDispacciamento() {
        return verificaDispacciamento;
    }

    public void setVerificaDispacciamento(Double verificaDispacciamento) {
        this.verificaDispacciamento = verificaDispacciamento;
    }

    public Double getPenali33() {
        return penali33;
    }

    public void setPenali33(Double penali33) {
        this.penali33 = penali33;
    }

    public Double getPenali75() {
        return penali75;
    }

    public void setPenali75(Double penali75) {
        this.penali75 = penali75;
    }

    public Double getAltro() {
        return altro;
    }

    public void setAltro(Double altro) {
        this.altro = altro;
    }

    public Date getPeriodoInizio() {
        return periodoInizio;
    }

    public void setPeriodoInizio(Date periodoInizio) {
        this.periodoInizio = periodoInizio;
    }

    public Date getPeriodoFine() {
        return periodoFine;
    }

    public void setPeriodoFine(Date periodoFine) {
        this.periodoFine = periodoFine;
    }

    public String getAnno() {
        return anno;
    }

    public void setAnno(String anno) {
        this.anno = anno;
    }

    public String getMese() {
        return mese;
    }

    public void setMese(String mese) {
        this.mese = mese;
    }

    public Double getVerificaOneri() {
        return verificaOneri;
    }

    public void setVerificaOneri(Double verificaOneri) {
        this.verificaOneri = verificaOneri;
    }

    public Double getVerificaTrasporti() {
        return verificaTrasporti;
    }

    public void setVerificaTrasporti(Double verificaTrasporti) {
        this.verificaTrasporti = verificaTrasporti;
    }

    public Double getVerificaImposte() {
        return verificaImposte;
    }

    public void setVerificaImposte(Double verificaImposte) {
        this.verificaImposte = verificaImposte;
    }

    public Double getPiccoKwh() {
        return piccoKwh;
    }

    public void setPiccoKwh(Double piccoKwh) {
        this.piccoKwh = piccoKwh;
    }

    public Double getFuoriPiccoKwh() {
        return fuoriPiccoKwh;
    }

    public void setFuoriPiccoKwh(Double fuoriPiccoKwh) {
        this.fuoriPiccoKwh = fuoriPiccoKwh;
    }

    public Double getCostoPicco() {
        return costoPicco;
    }

    public void setCostoPicco(Double costoPicco) {
        this.costoPicco = costoPicco;
    }

    public Double getCostoFuoriPicco() {
        return costoFuoriPicco;
    }

    public void setCostoFuoriPicco(Double costoFuoriPicco) {
        this.costoFuoriPicco = costoFuoriPicco;
    }

    public Double getVerificaPicco() {
        return verificaPicco;
    }

    public void setVerificaPicco(Double verificaPicco) {
        this.verificaPicco = verificaPicco;
    }

    public Double getVerificaFuoriPicco() {
        return verificaFuoriPicco;
    }

    public void setVerificaFuoriPicco(Double verificaFuoriPicco) {
        this.verificaFuoriPicco = verificaFuoriPicco;
    }

    public Double getTotAttivaPerdite() {
        return totAttivaPerdite;
    }

    public void setTotAttivaPerdite(Double totAttivaPerdite) {
        this.totAttivaPerdite = totAttivaPerdite;
    }

    public Double getQuotaVariabileTrasporti() {
        return quotaVariabileTrasporti;
    }

    public void setQuotaVariabileTrasporti(Double quotaVariabileTrasporti) {
        this.quotaVariabileTrasporti = quotaVariabileTrasporti;
    }

    public Double getQuotaFissaTrasporti() {
        return quotaFissaTrasporti;
    }

    public void setQuotaFissaTrasporti(Double quotaFissaTrasporti) {
        this.quotaFissaTrasporti = quotaFissaTrasporti;
    }

    public Double getQuotaPotenzaTrasporti() {
        return quotaPotenzaTrasporti;
    }

    public void setQuotaPotenzaTrasporti(Double quotaPotenzaTrasporti) {
        this.quotaPotenzaTrasporti = quotaPotenzaTrasporti;
    }

    public Double getQuotaEnergiaOneri() {
        return quotaEnergiaOneri;
    }

    public void setQuotaEnergiaOneri(Double quotaEnergiaOneri) {
        this.quotaEnergiaOneri = quotaEnergiaOneri;
    }

    public Double getQuotaFissaOneri() {
        return quotaFissaOneri;
    }

    public void setQuotaFissaOneri(Double quotaFissaOneri) {
        this.quotaFissaOneri = quotaFissaOneri;
    }

    public Double getQuotaPotenzaOneri() {
        return quotaPotenzaOneri;
    }

    public void setQuotaPotenzaOneri(Double quotaPotenzaOneri) {
        this.quotaPotenzaOneri = quotaPotenzaOneri;
    }
}