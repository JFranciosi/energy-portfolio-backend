package miesgroup.mies.webdev.Rest.Model;

import miesgroup.mies.webdev.Model.bolletta.Pod;

public class PodResponse {

    private String id;
    private Double tensioneAlimentazione;
    private Double potenzaImpegnata;
    private Double potenzaDisponibile;
    private String sede;
    private String nazione;
    private String cap;
    private String tipoTensione;
    private String fornitore;
    private Double spread;

    // Costruttore no-arg
    public PodResponse() {
    }

    // Costruttore che inizializza i campi da un oggetto Pod
    public PodResponse(Pod pod) {
        this.id = pod.getId();
        this.tensioneAlimentazione = pod.getTensioneAlimentazione();
        this.potenzaImpegnata = pod.getPotenzaImpegnata();
        this.potenzaDisponibile = pod.getPotenzaDisponibile();
        this.sede = pod.getSede();
        this.nazione = pod.getNazione();
        this.cap = pod.getCap();
        this.tipoTensione = pod.getTipoTensione();
        this.fornitore = pod.getFornitore();
        this.spread = pod.getSpread();
    }

    // GETTER e SETTER

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getTensioneAlimentazione() {
        return tensioneAlimentazione;
    }

    public void setTensioneAlimentazione(Double tensioneAlimentazione) {
        this.tensioneAlimentazione = tensioneAlimentazione;
    }

    public Double getPotenzaImpegnata() {
        return potenzaImpegnata;
    }

    public void setPotenzaImpegnata(Double potenzaImpegnata) {
        this.potenzaImpegnata = potenzaImpegnata;
    }

    public Double getPotenzaDisponibile() {
        return potenzaDisponibile;
    }

    public void setPotenzaDisponibile(Double potenzaDisponibile) {
        this.potenzaDisponibile = potenzaDisponibile;
    }

    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }

    public String getNazione() {
        return nazione;
    }

    public void setNazione(String nazione) {
        this.nazione = nazione;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public String getTipoTensione() {
        return tipoTensione;
    }

    public void setTipoTensione(String tipoTensione) {
        this.tipoTensione = tipoTensione;
    }

    public String getFornitore() {
        return fornitore;
    }

    public void setFornitore(String fornitore) {
        this.fornitore = fornitore;
    }

    public Double getSpread() {
        return spread;
    }

    public void setSpread(Double spread) {
        this.spread = spread;
    }
}
