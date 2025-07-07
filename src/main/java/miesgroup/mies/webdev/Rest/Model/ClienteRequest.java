package miesgroup.mies.webdev.Rest.Model;

public class ClienteRequest {
    private String username;
    private String email;
    private String pIva;
    private String sedeLegale;
    private String telefono;
    private String stato;
    private String tipologia;
    private String classeAgevolazione;
    private String codiceAteco;
    private String codiceAtecoSecondario;
    private Float consumoAnnuoEnergia;
    private Float fatturatoAnnuo;
    private String password;

    public ClienteRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getpIva() {
        return pIva;
    }

    public void setpIva(String pIva) {
        this.pIva = pIva;
    }

    public String getSedeLegale() {
        return sedeLegale;
    }

    public void setSedeLegale(String sedeLegale) {
        this.sedeLegale = sedeLegale;
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

    public Float getConsumoAnnuoEnergia() { return consumoAnnuoEnergia; }
    public void setConsumoAnnuoEnergia(Float consumoAnnuoEnergia) { this.consumoAnnuoEnergia = consumoAnnuoEnergia; }

    public Float getFatturatoAnnuo() { return fatturatoAnnuo; }
    public void setFatturatoAnnuo(Float fatturatoAnnuo) { this.fatturatoAnnuo = fatturatoAnnuo; }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
