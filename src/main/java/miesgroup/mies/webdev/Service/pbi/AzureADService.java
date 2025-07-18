package miesgroup.mies.webdev.Service.pbi;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import miesgroup.mies.webdev.Model.pbi.Secret;
import miesgroup.mies.webdev.Repository.pbi.SecretRepo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class AzureADService {

    @Inject
    SecretRepo secretRepo;

    @ConfigProperty(name = "azure.client-id")
    String clientId;

    @ConfigProperty(name = "azure.tenant-id")
    String tenantId;

    private Secret clientSecret;

    // Cache del token per evitare chiamate inutili
    private final AtomicReference<AccessToken> cachedToken = new AtomicReference<>();

    // Lock per evitare race condition durante l'acquisizione di nuovi token
    private final ReentrantLock tokenLock = new ReentrantLock();

    // Configura il numero di tentativi e il ritardo
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    // Tempo di buffer prima della scadenza (5 minuti)
    private static final Duration TOKEN_EXPIRY_BUFFER = Duration.ofMinutes(5);

    // Scope per Power BI
    private static final String POWER_BI_SCOPE = "https://analysis.windows.net/powerbi/api/.default";

    @PostConstruct
    public void init() {
        if (secretRepo == null) {
            throw new RuntimeException("SecretRepo is not injected! Check your dependency injection.");
        }

        this.clientSecret = secretRepo.findAll().stream().findFirst().orElse(null);
        if (clientSecret == null) {
            throw new RuntimeException("Secret not found in database");
        }
    }

    /**
     * Ottiene un token di accesso per Power BI, sfruttando la cache quando possibile.
     * Implementa retry pattern e gestisce correttamente le interruzioni.
     */
    public String getPowerBIAccessToken() {
        // Verifica se abbiamo un token in cache ancora valido
        AccessToken currentToken = cachedToken.get();
        if (currentToken != null && isTokenValid(currentToken)) {
            return currentToken.getToken();
        }

        // Token non valido o non presente, ne acquisisco uno nuovo
        try {
            // Utilizzare il lock per evitare che più thread richiedano contemporaneamente un token
            if (tokenLock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    // Controlla nuovamente dopo aver acquisito il lock (potrebbe essere stato aggiornato)
                    currentToken = cachedToken.get();
                    if (currentToken != null && isTokenValid(currentToken)) {
                        return currentToken.getToken();
                    }

                    // Ottieni un nuovo token con tentativi multipli
                    AccessToken newToken = getTokenWithRetry();
                    cachedToken.set(newToken);
                    return newToken.getToken();
                } finally {
                    tokenLock.unlock();
                }
            } else {
                throw new RuntimeException("Timeout during token acquisition lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Ripristina lo stato di interruzione
            throw new RuntimeException("Thread was interrupted while waiting for token acquisition", e);
        } catch (Exception e) {
            throw new RuntimeException("Error getting Power BI token: " + e.getMessage(), e);
        }
    }

    /**
     * Tenta di ottenere un token con retry in caso di fallimento.
     */
    private AccessToken getTokenWithRetry() {
        if (clientSecret == null) {
            throw new RuntimeException("Client secret is null. Check database records.");
        }

        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                        .clientId(clientId)
                        .clientSecret(clientSecret.getSecret())
                        .tenantId(tenantId)
                        .build();

                // Non usare block() direttamente, ma con timeout
               return credential.getToken(new TokenRequestContext().addScopes(POWER_BI_SCOPE))
                                .block(Duration.ofSeconds(30));

            } catch (Exception e) {
                lastException = e;
                System.err.println("Token acquisition attempt " + (attempt + 1) +
                        " failed: " + e.getMessage());

                try {
                    // Aspetta prima di riprovare
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", ie);
                }
            }
        }

        throw new RuntimeException("Failed to acquire token after " +
                MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }

    /**
     * Controlla se un token è ancora valido, considerando un buffer di sicurezza.
     */
    private boolean isTokenValid(AccessToken token) {
        if (token == null || token.getExpiresAt() == null) {
            return false;
        }

        // Verifica se il token scade a breve (considerando il buffer)
        return OffsetDateTime.now().plus(TOKEN_EXPIRY_BUFFER).isBefore(token.getExpiresAt());
    }
}