package miesgroup.mies.webdev.Rest.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CalendarioDTO {

    @JsonProperty("Anno")
    private Long Anno;

    @JsonProperty("Mese Numero")
    private Long MeseNumero;

    @JsonProperty("Mese Nome")
    private String MeseNome;

    @JsonProperty("Anno-Mese")
    private String AnnoMese;

    @JsonProperty("Mese Abbreviato")
    private String MeseAbbreviato;

    @JsonProperty("Trimestre")
    private String Trimestre;

    @JsonProperty("Periodo")
    private String Periodo;

    @JsonProperty("PeriodoTrimestre")
    private String PeriodoTrimestre;

    // Constructor
    public CalendarioDTO() {}

    // Getters and Setters
    public Long getAnno() {
        return Anno;
    }

    public void setAnno(Long anno) {
        this.Anno = anno;
    }

    public Long getMeseNumero() {
        return MeseNumero;
    }

    public void setMeseNumero(Long meseNumero) {
        this.MeseNumero = meseNumero;
    }

    public String getMeseNome() {
        return MeseNome;
    }

    public void setMeseNome(String meseNome) {
        this.MeseNome = meseNome;
    }

    public String getAnnoMese() {
        return AnnoMese;
    }

    public void setAnnoMese(String annoMese) {
        this.AnnoMese = annoMese;
    }

    public String getMeseAbbreviato() {
        return MeseAbbreviato;
    }

    public void setMeseAbbreviato(String meseAbbreviato) {
        this.MeseAbbreviato = meseAbbreviato;
    }

    public String getTrimestre() {
        return Trimestre;
    }

    public void setTrimestre(String trimestre) {
        this.Trimestre = trimestre;
    }

    public String getPeriodo() {
        return Periodo;
    }

    public void setPeriodo(String periodo) {
        this.Periodo = periodo;
    }

    public String getPeriodoTrimestre() {
        return PeriodoTrimestre;
    }

    public void setPeriodoTrimestre(String periodoTrimestre) {
        this.PeriodoTrimestre = periodoTrimestre;
    }

    @Override
    public String toString() {
        return "CalendarioDTO{" +
                "Anno=" + Anno +
                ", MeseNumero=" + MeseNumero +
                ", MeseNome='" + MeseNome + '\'' +
                ", AnnoMese='" + AnnoMese + '\'' +
                ", MeseAbbreviato='" + MeseAbbreviato + '\'' +
                ", Trimestre='" + Trimestre + '\'' +
                ", Periodo='" + Periodo + '\'' +
                ", PeriodoTrimestre='" + PeriodoTrimestre + '\'' +
                '}';
    }
}
