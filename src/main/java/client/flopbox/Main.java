package client.flopbox;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;

import client.flopbox.exception.ServeurNotFoundException;
import client.flopbox.util.AgentFlopBox;
import client.flopbox.util.FileManager;

public class Main {

    /** Adresse API utilisé */
    public static final String apiURL = "http://localhost:8080/flopbox/v4/";

    /** dernier date de synchronisation */
    public static Date dateLastSynchro = new Date();

    /**
     * Fonction main du programme
     * 
     * @param args
     * @throws IOException
     * @throws ServeurNotFoundException : si le serveur utiliser n'est pas trouvé
     *                                  dans le config file
     * @throws ParseException
     * @throws URISyntaxException
     */
    public static void main(String[] args)
            throws IOException, ServeurNotFoundException, ParseException, URISyntaxException {
        FileManager.initRacine();
        FileManager.initServeurs();
        while (true) {
            if (new Date().getTime() - dateLastSynchro.getTime() > 60 * 1000) {
                AgentFlopBox.synchronisation();
                dateLastSynchro = new Date();
            }
        }
    }
}
