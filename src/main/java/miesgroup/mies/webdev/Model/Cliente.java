package miesgroup.mies.webdev.Model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "utente")
public class Cliente extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_Utente")
    private Integer id;

    @Column(name = "Username", nullable = false, unique = true)
    private String username;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "Sede_Legale")
    private String sedeLegale;

    @Column(name = "Piva", length = 11)
    private String pIva;

    @Column(name = "Email")
    private String email;

    @Column(name = "Telefono")
    private String telefono;

    @Column(name = "Stato")
    private String stato;

    @Column(name = "Tipologia", nullable = false)
    private String tipologia = "Cliente";

    @Column(name = "Classe_Agevolazione")
    private String classeAgevolazione;

    @Column(name = "codice_ateco")
    private String codiceAteco;

    @Column(name = "codice_ateco_secondario")
    private String codiceAtecoSecondario;

    @Column(name = "energivori")
    private Boolean energivori = false;

    @Column(name = "gassivori")
    private Boolean gassivori = false;

    @Column(name = "consumo_annuo")
    private Float consumoAnnuoEnergia;

    @Column(name = "fatturato_annuo")
    private Float fatturatoAnnuo;

    @Column(name = "checkEmail")
    private Boolean checkEmail;

    public Cliente() {
    }

    // Getter e Setter

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSedeLegale() {
        return sedeLegale;
    }

    public void setSedeLegale(String sedeLegale) {
        this.sedeLegale = sedeLegale;
    }

    public String getpIva() {
        return pIva;
    }

    public void setpIva(String pIva) {
        this.pIva = pIva;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public String getTipologia() {
        return tipologia;
    }

    public void setTipologia(String tipologia) {
        this.tipologia = tipologia;
    }

    public String getClasseAgevolazione() {
        return classeAgevolazione;
    }

    public void setClasseAgevolazione(String classeAgevolazione) {
        this.classeAgevolazione = classeAgevolazione;
    }

    public String getCodiceAteco() {
        return codiceAteco;
    }

    public void setCodiceAteco(String codiceAteco) {
        this.codiceAteco = codiceAteco;
    }

    public String getCodiceAtecoSecondario() {
        return codiceAtecoSecondario;
    }

    public void setCodiceAtecoSecondario(String codiceAtecoSecondario) {
        this.codiceAtecoSecondario = codiceAtecoSecondario;
    }

    public Boolean getEnergivori() {
        return energivori;
    }

    public void setEnergivori(Boolean energivori) {
        this.energivori = energivori;
    }

    public Boolean getGassivori() {
        return gassivori;
    }

    public void setGassivori(Boolean gassivori) {
        this.gassivori = gassivori;
    }

    public Float getConsumoAnnuoEnergia() {
        return consumoAnnuoEnergia;
    }

    public void setConsumoAnnuoEnergia(Float consumoAnnuoEnergia) {
        this.consumoAnnuoEnergia = consumoAnnuoEnergia;
    }

    public Float getFatturatoAnnuo() {
        return fatturatoAnnuo;
    }

    public void setFatturatoAnnuo(Float fatturatoAnnuo) {
        this.fatturatoAnnuo = fatturatoAnnuo;
    }


    public Boolean getCheckEmail() {
        return checkEmail;
    }

    public void setCheckEmail(Boolean checkEmail) {
        this.checkEmail = checkEmail;
    }
}
