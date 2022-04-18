package client.flopbox.exception;

/**
 * Classe représentant une exception lorsque le serveur n'est pas trouvé dans le
 * fichier de config
 */
public class ServeurNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServeurNotFoundException(String message) {
        super(message);
    }

}
