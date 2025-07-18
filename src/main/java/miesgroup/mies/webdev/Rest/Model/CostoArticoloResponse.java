package miesgroup.mies.webdev.Rest.Model;

import miesgroup.mies.webdev.Model.bolletta.CostoArticolo;

public class CostoArticoloResponse {
    public String nomeArticolo;
    public Double costoUnitario;
    public String mese;
    public String nomeBolletta;
    public String categoriaArticolo;

    public CostoArticoloResponse(CostoArticolo c) {
        this.nomeArticolo = c.getNomeArticolo();
        this.costoUnitario = c.getCostoUnitario();
        this.mese = c.getMese();
        this.nomeBolletta = c.getBolletta().getNomeBolletta(); // o getNome() se hai modificato il nome del metodo
        this.categoriaArticolo = c.getCategoriaArticolo();
    }
}
