package miesgroup.mies.webdev.Rest.Model;

public class KeepLoggedRequest {
    public int idUtente;

    public boolean isKeepLogged() {
        return keepLogged;
    }

    public void setKeepLogged(boolean keepLogged) {
        this.keepLogged = keepLogged;
    }

    public int getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(int idUtente) {
        this.idUtente = idUtente;
    }

    public boolean keepLogged;

}
