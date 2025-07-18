package miesgroup.mies.webdev.Service.cliente;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.sql.SQLException;


@ApplicationScoped
public class LoggerService {

    private static final Logger LOGGER = Logger.getLogger(LoggerService.class);

    public void info(String message) {
        LOGGER.info(message);
    }

    public void warn(String message, SQLException e) {
        LOGGER.warn(message);
    }

    public void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    public void debug(String message) {
        LOGGER.debug(message);
    }

    public void trace(String message) {
        LOGGER.trace(message);
    }
}
