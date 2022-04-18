package client.flopbox.exception;

/**
 * Exception levée lorsqu'un fichier de configuration est introuvable
 */
public class ConfigFileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigFileException(String message) {
        super(message);
    }

}