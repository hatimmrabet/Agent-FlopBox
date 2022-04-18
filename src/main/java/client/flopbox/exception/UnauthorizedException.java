package client.flopbox.exception;

/**
 * Classe représentant une exception lorsque l'utilisateur n'est pas authentifié
 */
public class UnauthorizedException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }

}
