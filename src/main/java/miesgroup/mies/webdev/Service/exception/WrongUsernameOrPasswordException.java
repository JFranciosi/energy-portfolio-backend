package miesgroup.mies.webdev.Service.exception;

public class WrongUsernameOrPasswordException extends Exception {
    public WrongUsernameOrPasswordException() {
        super("Username o password errati");
    }
}