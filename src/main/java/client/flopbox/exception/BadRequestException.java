package client.flopbox.exception;

/**
 * Classe représentant une exception lorsque la requete n'est pas bien formée
 */
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message);
    }

}
