package miesgroup.mies.webdev.Model;

import java.util.Date;

public class Periodo {
    private Date inizio;
    private Date fine;
    private String anno;

    public Periodo(Date inizio, Date fine, String anno) {
        this.inizio = inizio;
        this.fine = fine;
        this.anno = anno;
    }

    // Getter e Setter
    public Date getInizio() {
        return inizio;
    }

    public void setInizio(Date inizio) {
        this.inizio = inizio;
    }

    public Date getFine() {
        return fine;
    }

    public void setFine(Date fine) {
        this.fine = fine;
    }

    public String getAnno() {
        return anno;
    }

    public void setAnno(String anno) {
        this.anno = anno;
    }

    public Date getDataFine() {
        return fine;
    }
}

