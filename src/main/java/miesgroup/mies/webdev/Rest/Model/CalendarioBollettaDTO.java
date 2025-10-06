package miesgroup.mies.webdev.Rest.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CalendarioBollettaDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("mese_numero")
    private String meseNumeroAnno;

    @JsonProperty("anno")
    private String anno;

    @JsonProperty("nome_mese")
    private String nomeMese;

    @JsonProperty("nome_mese_abbreviato")
    private String nomeMeseAbbreviato;

    @JsonProperty("data_completa")
    private String dataCompleta;

    @JsonProperty("meseAnno")
    private String meseAnno;

    public CalendarioBollettaDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMeseNumeroAnno() {
        return meseNumeroAnno;
    }

    public void setMeseNumeroAnno(String meseNumeroAnno) {
        this.meseNumeroAnno = meseNumeroAnno;
    }

    public String getNomeMese() {
        return nomeMese;
    }

    public void setNomeMese(String nomeMese) {
        this.nomeMese = nomeMese;
    }

    public String getNomeMeseAbbreviato() {
        return nomeMeseAbbreviato;
    }

    public String getAnno() {
        return anno;
    }

    public void setAnno(String anno) {
        this.anno = anno;
    }

    public void setNomeMeseAbbreviato(String nomeMeseAbbreviato) {
        this.nomeMeseAbbreviato = nomeMeseAbbreviato;
    }

    public String getDataCompleta() {
        return dataCompleta;
    }

    public void setDataCompleta(String dataCompleta) {
        this.dataCompleta = dataCompleta;
    }

    public String getMeseAnno() {
        return meseAnno;
    }

    public void setMeseAnno(String meseAnno) {
        this.meseAnno = meseAnno;
    }

    @Override
    public String toString() {
        return "CalendarioBollettaDTO{" +
                "id=" + id +
                ", meseNumeroAnno='" + meseNumeroAnno + '\'' +
                ", nomeMese='" + nomeMese + '\'' +
                ", nomeMeseAbbreviato='" + nomeMeseAbbreviato + '\'' +
                ", dataCompleta='" + dataCompleta + '\'' +
                ", meseAnno='" + meseAnno + '\'' +
                '}';
    }
}
