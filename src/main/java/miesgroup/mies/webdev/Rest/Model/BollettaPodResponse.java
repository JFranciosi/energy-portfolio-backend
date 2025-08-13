package miesgroup.mies.webdev.Rest.Model;

import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import java.sql.Date;

public class BollettaPodResponse {

    private Integer id;
    private String idPod;
    private String nomeBolletta;

    // Attiva
    private Double f1Att, f2Att, f3Att;

    // Reattiva
    private Double f1R, f2R, f3R;

    // Reattiva Capacitiva Immessa
    private Double f1RCapI, f2RCapI, f3RCapI;

    // Reattiva Induttiva Immessa
    private Double f1RIndI, f2RIndI, f3RIndI;

    // Potenza
    private Double f1Pot, f2Pot, f3Pot;

    // Euro/kWh
    private Double f0Euro, f1Euro, f2Euro, f3Euro;
    private Double f1PerdEuro, f2PerdEuro, f3PerdEuro;
    private Double f0Kwh, f1Kwh, f2Kwh, f3Kwh;
    private Double f1PerdKwh, f2PerdKwh, f3PerdKwh;

    // Totali
    private Double totAtt, totR, totRCapI, totRIndI;

    // Spese macro
    private Double speseEne, speseTrasp, oneri, imposte;

    // Altri costi
    private Double generation, dispacciamento;

    // Penali
    private Double f1Pen33, f2Pen33, f1Pen75, f2Pen75;
    private Double penRCapI;

    // Picco / fuori picco
    private Double piccoKwh, fuoriPiccoKwh, euroPicco, euroFuoriPicco;

    // Perdite attiva totale
    private Double totAttPerd;

    // Quote trasporti
    private Double qFixTrasp, qPotTrasp, qVarTrasp;

    // Quote oneri ASOS/ARIM
    private Double qEnOnASOS, qEnOnARIM;
    private Double qFixOnASOS, qFixOnARIM;
    private Double qPotOnASOS, qPotOnARIM;

    // Periodo / tempo
    private Date periodoInizio, periodoFine;
    private String anno, mese, meseAnno;

    public BollettaPodResponse(BollettaPod b) {
        this.id = b.getId();
        this.idPod = b.getIdPod();
        this.nomeBolletta = b.getNomeBolletta();

        this.f1Att = b.getF1Att();
        this.f2Att = b.getF2Att();
        this.f3Att = b.getF3Att();

        this.f1R = b.getF1R();
        this.f2R = b.getF2R();
        this.f3R = b.getF3R();

        this.f1RCapI = b.getF1RCapI();
        this.f2RCapI = b.getF2RCapI();
        this.f3RCapI = b.getF3RCapI();

        this.f1RIndI = b.getF1RIndI();
        this.f2RIndI = b.getF2RIndI();
        this.f3RIndI = b.getF3RIndI();

        this.f1Pot = b.getF1Pot();
        this.f2Pot = b.getF2Pot();
        this.f3Pot = b.getF3Pot();

        this.f0Euro = b.getF0Euro();
        this.f1Euro = b.getF1Euro();
        this.f2Euro = b.getF2Euro();
        this.f3Euro = b.getF3Euro();

        this.f1PerdEuro = b.getF1PerdEuro();
        this.f2PerdEuro = b.getF2PerdEuro();
        this.f3PerdEuro = b.getF3PerdEuro();

        this.f0Kwh = b.getF0Kwh();
        this.f1Kwh = b.getF1Kwh();
        this.f2Kwh = b.getF2Kwh();
        this.f3Kwh = b.getF3Kwh();

        this.f1PerdKwh = b.getF1PerdKwh();
        this.f2PerdKwh = b.getF2PerdKwh();
        this.f3PerdKwh = b.getF3PerdKwh();

        this.totAtt = b.getTotAtt();
        this.totR = b.getTotR();
        this.totRCapI = b.getTotRCapI();
        this.totRIndI = b.getTotRIndI();

        this.speseEne = b.getSpeseEne();
        this.speseTrasp = b.getSpeseTrasp();
        this.oneri = b.getOneri();
        this.imposte = b.getImposte();

        this.generation = b.getGeneration();
        this.dispacciamento = b.getDispacciamento();

        this.f1Pen33 = b.getF1Pen33();
        this.f2Pen33 = b.getF2Pen33();
        this.f1Pen75 = b.getF1Pen75();
        this.f2Pen75 = b.getF2Pen75();

        this.penRCapI = b.getPenRCapI();

        this.piccoKwh = b.getPiccoKwh();
        this.fuoriPiccoKwh = b.getFuoriPiccoKwh();
        this.euroPicco = b.getEuroPicco();
        this.euroFuoriPicco = b.getEuroFuoriPicco();

        this.totAttPerd = b.getTotAttPerd();

        this.qFixTrasp = b.getQFixTrasp();
        this.qPotTrasp = b.getQPotTrasp();
        this.qVarTrasp = b.getQVarTrasp();

        this.qEnOnASOS = b.getQEnOnASOS();
        this.qEnOnARIM = b.getQEnOnARIM();
        this.qFixOnASOS = b.getQFixOnASOS();
        this.qFixOnARIM = b.getQFixOnARIM();
        this.qPotOnASOS = b.getQPotOnASOS();
        this.qPotOnARIM = b.getQPotOnARIM();

        this.periodoInizio = b.getPeriodoInizio();
        this.periodoFine = b.getPeriodoFine();
        this.anno = b.getAnno();
        this.mese = b.getMese();
        this.meseAnno = b.getMeseAnno();
    }

    public Double getF3R() {
        return f3R;
    }

    public void setF3R(Double f3R) {
        this.f3R = f3R;
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

    public Double getF1Att() {
        return f1Att;
    }

    public void setF1Att(Double f1Att) {
        this.f1Att = f1Att;
    }

    public Double getF2Att() {
        return f2Att;
    }

    public void setF2Att(Double f2Att) {
        this.f2Att = f2Att;
    }

    public Double getF3Att() {
        return f3Att;
    }

    public void setF3Att(Double f3Att) {
        this.f3Att = f3Att;
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

    public Double getF1RCapI() {
        return f1RCapI;
    }

    public void setF1RCapI(Double f1RCapI) {
        this.f1RCapI = f1RCapI;
    }

    public Double getF2RCapI() {
        return f2RCapI;
    }

    public void setF2RCapI(Double f2RCapI) {
        this.f2RCapI = f2RCapI;
    }

    public Double getF3RCapI() {
        return f3RCapI;
    }

    public void setF3RCapI(Double f3RCapI) {
        this.f3RCapI = f3RCapI;
    }

    public Double getF1RIndI() {
        return f1RIndI;
    }

    public void setF1RIndI(Double f1RIndI) {
        this.f1RIndI = f1RIndI;
    }

    public Double getF2RIndI() {
        return f2RIndI;
    }

    public void setF2RIndI(Double f2RIndI) {
        this.f2RIndI = f2RIndI;
    }

    public Double getF3RIndI() {
        return f3RIndI;
    }

    public void setF3RIndI(Double f3RIndI) {
        this.f3RIndI = f3RIndI;
    }

    public Double getF1Pot() {
        return f1Pot;
    }

    public void setF1Pot(Double f1Pot) {
        this.f1Pot = f1Pot;
    }

    public Double getF2Pot() {
        return f2Pot;
    }

    public void setF2Pot(Double f2Pot) {
        this.f2Pot = f2Pot;
    }

    public Double getF3Pot() {
        return f3Pot;
    }

    public void setF3Pot(Double f3Pot) {
        this.f3Pot = f3Pot;
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

    public Double getF1PerdEuro() {
        return f1PerdEuro;
    }

    public void setF1PerdEuro(Double f1PerdEuro) {
        this.f1PerdEuro = f1PerdEuro;
    }

    public Double getF2PerdEuro() {
        return f2PerdEuro;
    }

    public void setF2PerdEuro(Double f2PerdEuro) {
        this.f2PerdEuro = f2PerdEuro;
    }

    public Double getF3PerdEuro() {
        return f3PerdEuro;
    }

    public void setF3PerdEuro(Double f3PerdEuro) {
        this.f3PerdEuro = f3PerdEuro;
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

    public Double getF1PerdKwh() {
        return f1PerdKwh;
    }

    public void setF1PerdKwh(Double f1PerdKwh) {
        this.f1PerdKwh = f1PerdKwh;
    }

    public Double getF2PerdKwh() {
        return f2PerdKwh;
    }

    public void setF2PerdKwh(Double f2PerdKwh) {
        this.f2PerdKwh = f2PerdKwh;
    }

    public Double getF3PerdKwh() {
        return f3PerdKwh;
    }

    public void setF3PerdKwh(Double f3PerdKwh) {
        this.f3PerdKwh = f3PerdKwh;
    }

    public Double getTotAtt() {
        return totAtt;
    }

    public void setTotAtt(Double totAtt) {
        this.totAtt = totAtt;
    }

    public Double getTotR() {
        return totR;
    }

    public void setTotR(Double totR) {
        this.totR = totR;
    }

    public Double getTotRCapI() {
        return totRCapI;
    }

    public void setTotRCapI(Double totRCapI) {
        this.totRCapI = totRCapI;
    }

    public Double getTotRIndI() {
        return totRIndI;
    }

    public void setTotRIndI(Double totRIndI) {
        this.totRIndI = totRIndI;
    }

    public Double getSpeseEne() {
        return speseEne;
    }

    public void setSpeseEne(Double speseEne) {
        this.speseEne = speseEne;
    }

    public Double getSpeseTrasp() {
        return speseTrasp;
    }

    public void setSpeseTrasp(Double speseTrasp) {
        this.speseTrasp = speseTrasp;
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

    public Double getF1Pen33() {
        return f1Pen33;
    }

    public void setF1Pen33(Double f1Pen33) {
        this.f1Pen33 = f1Pen33;
    }

    public Double getF2Pen33() {
        return f2Pen33;
    }

    public void setF2Pen33(Double f2Pen33) {
        this.f2Pen33 = f2Pen33;
    }

    public Double getF1Pen75() {
        return f1Pen75;
    }

    public void setF1Pen75(Double f1Pen75) {
        this.f1Pen75 = f1Pen75;
    }

    public Double getF2Pen75() {
        return f2Pen75;
    }

    public void setF2Pen75(Double f2Pen75) {
        this.f2Pen75 = f2Pen75;
    }

    public Double getPenRCapI() {
        return penRCapI;
    }

    public void setPenRCapI(Double penRCapI) {
        this.penRCapI = penRCapI;
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

    public Double getEuroPicco() {
        return euroPicco;
    }

    public void setEuroPicco(Double euroPicco) {
        this.euroPicco = euroPicco;
    }

    public Double getEuroFuoriPicco() {
        return euroFuoriPicco;
    }

    public void setEuroFuoriPicco(Double euroFuoriPicco) {
        this.euroFuoriPicco = euroFuoriPicco;
    }

    public Double getTotAttPerd() {
        return totAttPerd;
    }

    public void setTotAttPerd(Double totAttPerd) {
        this.totAttPerd = totAttPerd;
    }

    public Double getqFixTrasp() {
        return qFixTrasp;
    }

    public void setqFixTrasp(Double qFixTrasp) {
        this.qFixTrasp = qFixTrasp;
    }

    public Double getqPotTrasp() {
        return qPotTrasp;
    }

    public void setqPotTrasp(Double qPotTrasp) {
        this.qPotTrasp = qPotTrasp;
    }

    public Double getqVarTrasp() {
        return qVarTrasp;
    }

    public void setqVarTrasp(Double qVarTrasp) {
        this.qVarTrasp = qVarTrasp;
    }

    public Double getqEnOnASOS() {
        return qEnOnASOS;
    }

    public void setqEnOnASOS(Double qEnOnASOS) {
        this.qEnOnASOS = qEnOnASOS;
    }

    public Double getqEnOnARIM() {
        return qEnOnARIM;
    }

    public void setqEnOnARIM(Double qEnOnARIM) {
        this.qEnOnARIM = qEnOnARIM;
    }

    public Double getqFixOnASOS() {
        return qFixOnASOS;
    }

    public void setqFixOnASOS(Double qFixOnASOS) {
        this.qFixOnASOS = qFixOnASOS;
    }

    public Double getqFixOnARIM() {
        return qFixOnARIM;
    }

    public void setqFixOnARIM(Double qFixOnARIM) {
        this.qFixOnARIM = qFixOnARIM;
    }

    public Double getqPotOnASOS() {
        return qPotOnASOS;
    }

    public void setqPotOnASOS(Double qPotOnASOS) {
        this.qPotOnASOS = qPotOnASOS;
    }

    public Double getqPotOnARIM() {
        return qPotOnARIM;
    }

    public void setqPotOnARIM(Double qPotOnARIM) {
        this.qPotOnARIM = qPotOnARIM;
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

    public String getMeseAnno() {
        return meseAnno;
    }

    public void setMeseAnno(String meseAnno) {
        this.meseAnno = meseAnno;
    }
}
