package miesgroup.mies.webdev.Rest.Model;

public class ClienteResponse {

    private int id;

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

    private Boolean energivori;
    private Boolean gassivori;
    private Boolean checkEmail;

    public ClienteResponse(int id, String username, String email, String pIva, String sedeLegale, String telefono, String stato, String tipologia,
                           String classeAgevolazione, String codiceAteco, String codiceAtecoSecondario,
                           Float consumoAnnuoEnergia, Float fatturatoAnnuo,
                           Boolean energivori, Boolean gassivori, Boolean checkEmail) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.pIva = pIva;
        this.sedeLegale = sedeLegale;
        this.telefono = telefono;
        this.stato = stato;
        this.tipologia = tipologia;
        this.classeAgevolazione = classeAgevolazione;
        this.codiceAteco = codiceAteco;
        this.codiceAtecoSecondario = codiceAtecoSecondario;
        this.consumoAnnuoEnergia = consumoAnnuoEnergia;
        this.fatturatoAnnuo = fatturatoAnnuo;
        this.energivori = energivori;
        this.gassivori = gassivori;
        this.checkEmail = checkEmail;
    }

    // getter e setter per id
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    // getters per tutti gli altri campi

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getpIva() { return pIva; }
    public String getSedeLegale() { return sedeLegale; }
    public String getTelefono() { return telefono; }
    public String getStato() { return stato; }
    public String getTipologia() { return tipologia; }
    public String getClasseAgevolazione() { return classeAgevolazione; }
    public String getCodiceAteco() { return codiceAteco; }
    public String getCodiceAtecoSecondario() { return codiceAtecoSecondario; }
    public Float getConsumoAnnuoEnergia() { return consumoAnnuoEnergia; }
    public Float getFatturatoAnnuo() { return fatturatoAnnuo; }
    public Boolean getEnergivori() { return energivori; }
    public Boolean getGassivori() { return gassivori; }
    public Boolean getCheckEmail() { return checkEmail; }
}
