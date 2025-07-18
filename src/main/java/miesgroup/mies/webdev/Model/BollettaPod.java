package miesgroup.mies.webdev.Model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.sql.Date;
import java.util.List;

@Entity
@Table(name = "bolletta_pod") // Nome corretto della tabella nel database
public class BollettaPod extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment su MySQL
    @Column(name = "Id_Bolletta") // Nome corretto della colonna ID
    private Integer id;


    // Mappatura OneToMany: una bolletta ha più articoli
    @OneToMany(mappedBy = "bolletta", cascade = CascadeType.ALL)
    private List<CostoArticolo> costiArticolo;

    // AGGIUNGI QUI:
    @Column(name = "id_user", nullable = false)
    private int idUser;

    @Column(name = "id_pod", nullable = false)
    private String idPod;

    @Column(name = "nome_bolletta", nullable = false)
    private String nomeBolletta;

    @Column(name = "f1_attiva")
    private Double f1A;

    @Column(name = "f2_attiva")
    private Double f2A;

    @Column(name = "f3_attiva")
    private Double f3A;

    @Column(name = "f1_reattiva")
    private Double f1R;

    @Column(name = "f2_reattiva")
    private Double f2R;

    @Column(name = "f3_reattiva")
    private Double f3R;

    @Column(name = "f1_potenza")
    private Double f1P;

    @Column(name = "f2_potenza")
    private Double f2P;

    @Column(name = "f3_potenza")
    private Double f3P;

    @Column(name = "f0_€")
    private Double f0Euro;

    @Column(name = "f1_€")
    private Double f1Euro;

    @Column(name = "f2_€")
    private Double f2Euro;

    @Column(name = "f3_€")
    private Double f3Euro;

    @Column(name = "f1_perdite_€")
    private Double f1PerditeEuro;

    @Column(name = "f2_perdite_€")
    private Double f2PerditeEuro;

    @Column(name = "f3_perdite_€")
    private Double f3PerditeEuro;

    @Column(name = "f0_kwh")
    private Double f0Kwh;

    @Column(name = "f1_kwh")
    private Double f1Kwh;

    @Column(name = "f2_kwh")
    private Double f2Kwh;

    @Column(name = "f3_kwh")
    private Double f3Kwh;

    @Column(name = "f1_perdite_kwh")
    private Double f1PerditeKwh;

    @Column(name = "f2_perdite_kwh")
    private Double f2PerditeKwh;

    @Column(name = "f3_perdite_kwh")
    private Double f3PerditeKwh;

    @Column(name = "verifica_spesa_materia_energia")
    private Double verificaSpesaMateriaEnergia;

    @Column(name = "verifica_f1")
    private Double verificaF1;

    @Column(name = "verifica_f2")
    private Double verificaF2;

    @Column(name = "verifica_f3")
    private Double verificaF3;

    @Column(name = "verifica_f1_perdite")
    private Double verificaF1Perdite;

    @Column(name = "verifica_f2_perdite")
    private Double verificaF2Perdite;

    @Column(name = "verifica_f3_perdite")
    private Double verificaF3Perdite;

    @Column(name = "verifica_f0")
    private Double verificaF0;

    @Column(name = "tot_attiva")
    private Double totAttiva;

    @Column(name = "tot_reattiva")
    private Double totReattiva;

    @Column(name = "spese_energia")
    private Double speseEnergia;

    @Column(name = "spese_trasporto")
    private Double trasporti;

    @Column(name = "oneri")
    private Double oneri;

    @Column(name = "imposte")
    private Double imposte;

    @Column(name = "generation")
    private Double generation;

    @Column(name = "dispacciamento")
    private Double dispacciamento;

    @Column(name = "verifica_dispacciamento")
    private Double verificaDispacciamento;

    @Column(name = "F1Penale33")
    private Double F1Penale33;

    @Column(name = "F1Penale75")
    private Double F1Penale75;

    @Column(name = "F2Penale33")
    private Double F2Penale33;

    @Column(name = "F2Penale75")
    private Double F2Penale75;

    @Column(name = "Altro")
    private Double altro;

    @Column(name = "periodo_inizio", nullable = false)
    private Date periodoInizio;

    @Column(name = "periodo_fine", nullable = false)
    private Date periodoFine;

    @Column(name = "anno", nullable = false)
    private String anno;

    @Column(name = "mese")
    private String mese;

    @Column(name = "verifica_oneri")
    private Double verificaOneri;

    @Column(name = "verifica_trasporti")
    private Double verificaTrasporti;

    @Column(name = "verifica_imposte")
    private Double verificaImposte;

    @Column(name = "picco_kwh")
    private Double piccoKwh;

    @Column(name = "fuori_picco_kwh")
    private Double fuoriPiccoKwh;

    @Column(name = "€_picco")
    private Double costoPicco;

    @Column(name = "€_fuori_picco")
    private Double costoFuoriPicco;

    @Column(name = "verifica_picco")
    private Double verificaPicco;

    @Column(name = "verifica_fuori_picco")
    private Double verificaFuoriPicco;

    @Column(name = "tot_attiva_perdite")
    private Double totAttivaPerdite;

    @Column(name = "quota_variabile_trasporti")
    private Double quotaVariabileTrasporti;

    @Column(name = "quota_fissa_trasporti")
    private Double quotaFissaTrasporti;

    @Column(name = "quota_potenza_trasporti")
    private Double quotaPotenzaTrasporti;

    @Column(name = "quota_energia_oneri")
    private Double quotaEnergiaOneri;

    @Column(name = "quota_fissa_oneri")
    private Double quotaFissaOneri;

    @Column(name = "quota_potenza_oneri")
    private Double quotaPotenzaOneri;

    @Column(name = "mese_anno")
    private String meseAnno;

    // GETTER e SETTER

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

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

    public void setVerificaDispacciamento(Double dispacciamento) {
        this.verificaDispacciamento = dispacciamento;
    }

    public Double getF1Penale33() {
        return F1Penale33;
    }

    public void setF1Penale33(Double f1Penale33) {
        this.F1Penale33 = f1Penale33;
    }

    public Double getF2Penale33() {
        return F2Penale33;
    }

    public void setF2Penale33(Double f2Penale33) {
        this.F2Penale33 = f2Penale33;
    }

    public Double getF1Penale75() {
        return F1Penale75;
    }

    public void setF1Penale75(Double f1Penale75) {
        F1Penale75 = f1Penale75;
    }

    public Double getF2Penale75() {
        return F2Penale75;
    }

    public void setF2Penale75(Double f2Penale75) {
        F2Penale75 = f2Penale75;
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

    public void setVerificaOneri(Double verificaOnneri) {
        this.verificaOneri = verificaOnneri;
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

    public void setPiccoKwh(Double picco) {
        this.piccoKwh = picco;
    }

    public Double getFuoriPiccoKwh() {
        return fuoriPiccoKwh;
    }

    public void setFuoriPiccoKwh(Double fuoriPicco) {
        this.fuoriPiccoKwh = fuoriPicco;
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

    public List<CostoArticolo> getCostiArticolo() {
        return costiArticolo;
    }

    public void setCostiArticolo(List<CostoArticolo> costiArticolo) {
        this.costiArticolo = costiArticolo;
    }

}