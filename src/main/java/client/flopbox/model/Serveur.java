package client.flopbox.model;

import java.io.File;

/**
 * Classe représentant un serveur
 */
public class Serveur {
    private String alias;
    /** username du serveur FTP */
    private String username;
    /** mot de passe du serveur FTP */
    private String password;
    private File racine;

    public Serveur(String alias, String name, String mdp)
    {
        this.alias = alias;
        this.username = name;
        this.password = mdp;
    }

    /**
     * @return le nom du serveur
     */
    public String getAlias() {
        return this.alias;
    }

    /**
     * @return le nom de l'utilisateur du serveur
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @return le mot de passe de l'utilisateur du serveur
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * @return le chemin de la racine du serveur
     */
    public File getRacine() {
        return this.racine;
    }

    /**
     * @param racine le chemin de la racine du serveur
     */
    public void setRacine(File racine) {
        this.racine = racine;
    }
}
