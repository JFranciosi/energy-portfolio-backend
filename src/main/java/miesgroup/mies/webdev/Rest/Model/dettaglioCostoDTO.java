package miesgroup.mies.webdev.Rest.Model;

import miesgroup.mies.webdev.Model.bolletta.dettaglioCosto;

public class dettaglioCostoDTO {
    public Integer id;
    public String item;
    public String unitaMisura;
    public Integer modality;
    public Integer checkModality;
    public Double costo;
    public String categoria;
    public String intervalloPotenza;
    public String classeAgevolazione;
    public String annoRiferimento;
    public String itemDescription;

    public dettaglioCostoDTO(dettaglioCosto entity) {
        this.id = entity.getId();
        this.item = entity.getItem();
        this.unitaMisura = entity.getUnitaMisura();
        this.modality = entity.getModality();
        this.checkModality = entity.getCheckModality();
        this.costo = entity.getCosto();
        this.categoria = entity.getCategoria();
        this.intervalloPotenza = entity.getIntervalloPotenza();
        this.classeAgevolazione = entity.getClasseAgevolazione();
        this.annoRiferimento = entity.getAnnoRiferimento();
        this.itemDescription = entity.getItemDescription();
    }
}
