package miesgroup.mies.webdev.Model.bolletta;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ver_bolletta_pod")
public class verBollettaPod extends PanacheEntityBase {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // FK → bolletta_pod
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bolletta_id", nullable = false)
    private BollettaPod bollettaId;

    // Identificativi (copiati)
    @Column(name = "id_pod")
    private String idPod;

    @Column(name = "Nome_Bolletta", nullable = false)
    private String nomeBolletta;

    // Attiva per fascia
    @Column(name = "F1_Att") private Double f1Att;
    @Column(name = "F2_Att") private Double f2Att;
    @Column(name = "F3_Att") private Double f3Att;

    // Reattiva per fascia
    @Column(name = "F1_R") private Double f1R;
    @Column(name = "F2_R") private Double f2R;
    @Column(name = "F3_R") private Double f3R;

    // Reattiva CAPACITIVA IMMESSA
    @Column(name = "F1_RCapI") private Double f1RCapI;
    @Column(name = "F2_RCapI") private Double f2RCapI;
    @Column(name = "F3_RCapI") private Double f3RCapI;

    // Reattiva INDUTTIVA IMMESSA
    @Column(name = "F1_RIndI") private Double f1RIndI;
    @Column(name = "F2_RIndI") private Double f2RIndI;
    @Column(name = "F3_RIndI") private Double f3RIndI;

    // Potenza per fascia
    @Column(name = "F1_Pot") private Double f1Pot;
    @Column(name = "F2_Pot") private Double f2Pot;
    @Column(name = "F3_Pot") private Double f3Pot;

    // Spese macro
    @Column(name = "Spese_Ene")   private Double speseEne;
    @Column(name = "Spese_Trasp") private Double speseTrasp;
    @Column(name = "Oneri")       private Double oneri;
    @Column(name = "Imposte")     private Double imposte;

    // Totali
    @Column(name = "TOT_Att")    private Double totAtt;
    @Column(name = "TOT_R")      private Double totR;
    @Column(name = "TOT_RCapI")  private Double totRCapI;
    @Column(name = "TOT_RIndI")  private Double totRIndI;

    // Altri importi
    @Column(name = "Generation")     private Double generation;
    @Column(name = "Dispacciamento") private Double dispacciamento;

    // Penali
    @Column(name = "F1Penale33") private Double f1Pen33;
    @Column(name = "F1Penale75") private Double f1Pen75;
    @Column(name = "F2Penale33") private Double f2Pen33;
    @Column(name = "F2Penale75") private Double f2Pen75;

    // Penalità reattiva capacitiva immessa
    @Column(name = "Pen_RCapI") private Double penRCapI;

    // Picco / fuori picco
    @Column(name = "Picco_kwh")       private Double piccoKwh;
    @Column(name = "FuoriPicco_kwh")  private Double fuoriPiccoKwh;
    @Column(name = "Euro_Picco")      private Double euroPicco;
    @Column(name = "Euro_FuoriPicco") private Double euroFuoriPicco;

    // Perdite / breakdown fasce
    @Column(name = "TOT_Att_Perd") private Double totAttPerd;

    @Column(name = "F0_Euro") private Double f0Euro;
    @Column(name = "F0_kwh")  private Double f0Kwh;

    @Column(name = "F1_Euro") private Double f1Euro;
    @Column(name = "F1_kwh")  private Double f1Kwh;
    @Column(name = "F1_Perd_Euro")   private Double f1PerdEuro;
    @Column(name = "F1_Perdite_kwh") private Double f1PerdKwh;

    @Column(name = "F2_Euro") private Double f2Euro;
    @Column(name = "F2_kwh")  private Double f2Kwh;
    @Column(name = "F2_Perd_Euro")   private Double f2PerdEuro;
    @Column(name = "F2_Perdite_kwh") private Double f2PerdKwh;

    @Column(name = "F3_Euro") private Double f3Euro;
    @Column(name = "F3_kwh")  private Double f3Kwh;
    @Column(name = "F3_Perd_Euro")   private Double f3PerdEuro;
    @Column(name = "F3_Perdite_kwh") private Double f3PerdKwh;

    // Quote trasporti
    @Column(name = "QFix_Trasp") private Double qFixTrasp;
    @Column(name = "QPot_Trasp") private Double qPotTrasp;
    @Column(name = "QVar_Trasp") private Double qVarTrasp;

    // Quote oneri ASOS/ARIM
    @Column(name = "QEnOn_ASOS")  private Double qEnOnASOS;
    @Column(name = "QEnOn_ARIM")  private Double qEnOnARIM;
    @Column(name = "QFixOn_ASOS") private Double qFixOnASOS;
    @Column(name = "QFixOn_ARIM") private Double qFixOnARIM;
    @Column(name = "QPotOn_ASOS") private Double qPotOnASOS;
    @Column(name = "QPotOn_ARIM") private Double qPotOnARIM;

    // EXTRA: componenti dispacciamento
    @Column(name = "Art25bis") private Double art25bis;
    @Column(name = "Art44_3")  private Double art44_3;
    @Column(name = "Art44bis") private Double art44bis;
    @Column(name = "Art46")    private Double art46;
    @Column(name = "Art48")    private Double art48;
    @Column(name = "Art73")    private Double art73;
    @Column(name = "Art45Ann") private Double art45Ann;
    @Column(name = "Art45Tri") private Double art45Tri;

    // EXTRA: componenti trasporti
    @Column(name = "UC3_UC6")   private Double uc3Uc6;
    @Column(name = "Trasp_QEne") private Double traspQEne;
    @Column(name = "Distr_QEne") private Double distrQEne;
    @Column(name = "Distr_QPot") private Double distrQPot;
    @Column(name = "Mis_QFix")   private Double misQFix;
    @Column(name = "Distr_QFix") private Double distrQFix;
    //Energia Verde
    @Column(name = "En_Ve_Kwh") private Double enVeKwh;
    @Column(name = "En_Ve_Euro") private Double enVeEuro;
    // Audit
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    /* =========================
       Getters & Setters
       ========================= */

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public BollettaPod getBollettaId() { return bollettaId; }
    public void setBollettaId(BollettaPod bollettaId) { this.bollettaId = bollettaId; }

    public String getIdPod() { return idPod; }
    public void setIdPod(String idPod) { this.idPod = idPod; }

    public String getNomeBolletta() { return nomeBolletta; }
    public void setNomeBolletta(String nomeBolletta) { this.nomeBolletta = nomeBolletta; }

    public Double getF1Att() { return f1Att; }
    public void setF1Att(Double f1Att) { this.f1Att = f1Att; }
    public Double getF2Att() { return f2Att; }
    public void setF2Att(Double f2Att) { this.f2Att = f2Att; }
    public Double getF3Att() { return f3Att; }
    public void setF3Att(Double f3Att) { this.f3Att = f3Att; }

    public Double getF1R() { return f1R; }
    public void setF1R(Double f1R) { this.f1R = f1R; }
    public Double getF2R() { return f2R; }
    public void setF2R(Double f2R) { this.f2R = f2R; }
    public Double getF3R() { return f3R; }
    public void setF3R(Double f3R) { this.f3R = f3R; }

    public Double getF1RCapI() { return f1RCapI; }
    public void setF1RCapI(Double f1RCapI) { this.f1RCapI = f1RCapI; }
    public Double getF2RCapI() { return f2RCapI; }
    public void setF2RCapI(Double f2RCapI) { this.f2RCapI = f2RCapI; }
    public Double getF3RCapI() { return f3RCapI; }
    public void setF3RCapI(Double f3RCapI) { this.f3RCapI = f3RCapI; }

    public Double getF1RIndI() { return f1RIndI; }
    public void setF1RIndI(Double f1RIndI) { this.f1RIndI = f1RIndI; }
    public Double getF2RIndI() { return f2RIndI; }
    public void setF2RIndI(Double f2RIndI) { this.f2RIndI = f2RIndI; }
    public Double getF3RIndI() { return f3RIndI; }
    public void setF3RIndI(Double f3RIndI) { this.f3RIndI = f3RIndI; }

    public Double getF1Pot() { return f1Pot; }
    public void setF1Pot(Double f1Pot) { this.f1Pot = f1Pot; }
    public Double getF2Pot() { return f2Pot; }
    public void setF2Pot(Double f2Pot) { this.f2Pot = f2Pot; }
    public Double getF3Pot() { return f3Pot; }
    public void setF3Pot(Double f3Pot) { this.f3Pot = f3Pot; }

    public Double getSpeseEne() { return speseEne; }
    public void setSpeseEne(Double speseEne) { this.speseEne = speseEne; }
    public Double getSpeseTrasp() { return speseTrasp; }
    public void setSpeseTrasp(Double speseTrasp) { this.speseTrasp = speseTrasp; }
    public Double getOneri() { return oneri; }
    public void setOneri(Double oneri) { this.oneri = oneri; }
    public Double getImposte() { return imposte; }
    public void setImposte(Double imposte) { this.imposte = imposte; }

    public Double getTotAtt() { return totAtt; }
    public void setTotAtt(Double totAtt) { this.totAtt = totAtt; }
    public Double getTotR() { return totR; }
    public void setTotR(Double totR) { this.totR = totR; }
    public Double getTotRCapI() { return totRCapI; }
    public void setTotRCapI(Double totRCapI) { this.totRCapI = totRCapI; }
    public Double getTotRIndI() { return totRIndI; }
    public void setTotRIndI(Double totRIndI) { this.totRIndI = totRIndI; }

    public Double getGeneration() { return generation; }
    public void setGeneration(Double generation) { this.generation = generation; }
    public Double getDispacciamento() { return dispacciamento; }
    public void setDispacciamento(Double dispacciamento) { this.dispacciamento = dispacciamento; }

    public Double getF1Pen33() { return f1Pen33; }
    public void setF1Pen33(Double f1Pen33) { this.f1Pen33 = f1Pen33; }
    public Double getF1Pen75() { return f1Pen75; }
    public void setF1Pen75(Double f1Pen75) { this.f1Pen75 = f1Pen75; }
    public Double getF2Pen33() { return f2Pen33; }
    public void setF2Pen33(Double f2Pen33) { this.f2Pen33 = f2Pen33; }
    public Double getF2Pen75() { return f2Pen75; }
    public void setF2Pen75(Double f2Pen75) { this.f2Pen75 = f2Pen75; }

    public Double getPenRCapI() { return penRCapI; }
    public void setPenRCapI(Double penRCapI) { this.penRCapI = penRCapI; }

    public Double getPiccoKwh() { return piccoKwh; }
    public void setPiccoKwh(Double piccoKwh) { this.piccoKwh = piccoKwh; }
    public Double getFuoriPiccoKwh() { return fuoriPiccoKwh; }
    public void setFuoriPiccoKwh(Double fuoriPiccoKwh) { this.fuoriPiccoKwh = fuoriPiccoKwh; }
    public Double getEuroPicco() { return euroPicco; }
    public void setEuroPicco(Double euroPicco) { this.euroPicco = euroPicco; }
    public Double getEuroFuoriPicco() { return euroFuoriPicco; }
    public void setEuroFuoriPicco(Double euroFuoriPicco) { this.euroFuoriPicco = euroFuoriPicco; }

    public Double getTotAttPerd() { return totAttPerd; }
    public void setTotAttPerd(Double totAttPerd) { this.totAttPerd = totAttPerd; }

    public Double getF0Euro() { return f0Euro; }
    public void setF0Euro(Double f0Euro) { this.f0Euro = f0Euro; }
    public Double getF0Kwh() { return f0Kwh; }
    public void setF0Kwh(Double f0Kwh) { this.f0Kwh = f0Kwh; }

    public Double getF1Euro() { return f1Euro; }
    public void setF1Euro(Double f1Euro) { this.f1Euro = f1Euro; }
    public Double getF1Kwh() { return f1Kwh; }
    public void setF1Kwh(Double f1Kwh) { this.f1Kwh = f1Kwh; }
    public Double getF1PerdEuro() { return f1PerdEuro; }
    public void setF1PerdEuro(Double f1PerdEuro) { this.f1PerdEuro = f1PerdEuro; }
    public Double getF1PerdKwh() { return f1PerdKwh; }
    public void setF1PerdKwh(Double f1PerdKwh) { this.f1PerdKwh = f1PerdKwh; }

    public Double getF2Euro() { return f2Euro; }
    public void setF2Euro(Double f2Euro) { this.f2Euro = f2Euro; }
    public Double getF2Kwh() { return f2Kwh; }
    public void setF2Kwh(Double f2Kwh) { this.f2Kwh = f2Kwh; }
    public Double getF2PerdEuro() { return f2PerdEuro; }
    public void setF2PerdEuro(Double f2PerdEuro) { this.f2PerdEuro = f2PerdEuro; }
    public Double getF2PerdKwh() { return f2PerdKwh; }
    public void setF2PerdKwh(Double f2PerdKwh) { this.f2PerdKwh = f2PerdKwh; }

    public Double getF3Euro() { return f3Euro; }
    public void setF3Euro(Double f3Euro) { this.f3Euro = f3Euro; }
    public Double getF3Kwh() { return f3Kwh; }
    public void setF3Kwh(Double f3Kwh) { this.f3Kwh = f3Kwh; }
    public Double getF3PerdEuro() { return f3PerdEuro; }
    public void setF3PerdEuro(Double f3PerdEuro) { this.f3PerdEuro = f3PerdEuro; }
    public Double getF3PerdKwh() { return f3PerdKwh; }
    public void setF3PerdKwh(Double f3PerdKwh) { this.f3PerdKwh = f3PerdKwh; }

    public Double getQFixTrasp() { return qFixTrasp; }
    public void setQFixTrasp(Double qFixTrasp) { this.qFixTrasp = qFixTrasp; }
    public Double getQPotTrasp() { return qPotTrasp; }
    public void setQPotTrasp(Double qPotTrasp) { this.qPotTrasp = qPotTrasp; }
    public Double getQVarTrasp() { return qVarTrasp; }
    public void setQVarTrasp(Double qVarTrasp) { this.qVarTrasp = qVarTrasp; }

    public Double getQEnOnASOS() { return qEnOnASOS; }
    public void setQEnOnASOS(Double qEnOnASOS) { this.qEnOnASOS = qEnOnASOS; }
    public Double getQEnOnARIM() { return qEnOnARIM; }
    public void setQEnOnARIM(Double qEnOnARIM) { this.qEnOnARIM = qEnOnARIM; }
    public Double getQFixOnASOS() { return qFixOnASOS; }
    public void setQFixOnASOS(Double qFixOnASOS) { this.qFixOnASOS = qFixOnASOS; }
    public Double getQFixOnARIM() { return qFixOnARIM; }
    public void setQFixOnARIM(Double qFixOnARIM) { this.qFixOnARIM = qFixOnARIM; }
    public Double getQPotOnASOS() { return qPotOnASOS; }
    public void setQPotOnASOS(Double qPotOnASOS) { this.qPotOnASOS = qPotOnASOS; }
    public Double getQPotOnARIM() { return qPotOnARIM; }
    public void setQPotOnARIM(Double qPotOnARIM) { this.qPotOnARIM = qPotOnARIM; }

    public Double getArt25bis() { return art25bis; }
    public void setArt25bis(Double art25bis) { this.art25bis = art25bis; }
    public Double getArt44_3() { return art44_3; }
    public void setArt44_3(Double art44_3) { this.art44_3 = art44_3; }
    public Double getArt44bis() { return art44bis; }
    public void setArt44bis(Double art44bis) { this.art44bis = art44bis; }
    public Double getArt46() { return art46; }
    public void setArt46(Double art46) { this.art46 = art46; }
    public Double getArt48() { return art48; }
    public void setArt48(Double art48) { this.art48 = art48; }
    public Double getArt73() { return art73; }
    public void setArt73(Double art73) { this.art73 = art73; }
    public Double getArt45Ann() { return art45Ann; }
    public void setArt45Ann(Double art45Ann) { this.art45Ann = art45Ann; }
    public Double getArt45Tri() { return art45Tri; }
    public void setArt45Tri(Double art45Tri) { this.art45Tri = art45Tri; }

    public Double getUc3Uc6() { return uc3Uc6; }
    public void setUc3Uc6(Double uc3Uc6) { this.uc3Uc6 = uc3Uc6; }
    public Double getTraspQEne() { return traspQEne; }
    public void setTraspQEne(Double traspQEne) { this.traspQEne = traspQEne; }
    public Double getDistrQEne() { return distrQEne; }
    public void setDistrQEne(Double distrQEne) { this.distrQEne = distrQEne; }
    public Double getDistrQPot() { return distrQPot; }
    public void setDistrQPot(Double distrQPot) { this.distrQPot = distrQPot; }
    public Double getMisQFix() { return misQFix; }
    public void setMisQFix(Double misQFix) { this.misQFix = misQFix; }
    public Double getDistrQFix() { return distrQFix; }
    public void setDistrQFix(Double distrQFix) { this.distrQFix = distrQFix; }

    public Double getEnVeKwh() {
        return enVeKwh;
    }
    public void setEnVeKwh(Double enVeKwh) {
        this.enVeKwh = enVeKwh;
    }
    public Double getEnVeEuro() {
        return enVeEuro;
    }
    public void setEnVeEuro(Double enVeEuro) {
        this.enVeEuro = enVeEuro;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Alias comodo (se mai serve simile a getIdFile)
    public Integer getIdRow() { return id; }
}
