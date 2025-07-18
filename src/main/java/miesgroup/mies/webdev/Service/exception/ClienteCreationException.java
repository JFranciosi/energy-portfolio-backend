package miesgroup.mies.webdev.Service.exception;

public class ClienteCreationException extends Exception {
    public ClienteCreationException() {
        super("Utente con questo username esiste già");
    }
}
