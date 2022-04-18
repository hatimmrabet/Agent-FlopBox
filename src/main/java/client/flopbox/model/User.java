package client.flopbox.model;

import java.io.File;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;

import com.google.gson.JsonObject;

import client.flopbox.util.FileManager;

/**
 * Classe représentant un utilisateur
 */
public class User {
    static User instance = null;
    private String username;
    private String password;
    private String token;
    private File racine;
    private HashMap<String, Serveur> serveurs;

    /**
     * Constructeur de la classe User
     * Recuperation des donnes à partir du fichier de config
     */
    private User() {
        JsonObject data = FileManager.getJsonFileContent();
        this.username = data.get("username").getAsString();
        this.password = data.get("password").getAsString();
        this.token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        this.racine = new File(data.get("localPath").getAsString());
        this.serveurs = new HashMap<>();
        JsonObject sers = data.get("serveurs").getAsJsonObject();
        for (String key : sers.keySet()) {
            JsonObject obj = sers.getAsJsonObject(key);
            Serveur s = new Serveur(key, obj.get("username").getAsString(), obj.get("password").getAsString());
            this.serveurs.put(key, s);
        }
    }

    /**
     * @return instance de la classe User
     */
    public static User getInstance() {
        if (instance == null)
            instance = new User();
        return instance;
    }

    /**
     * @return le nom de client
     */
    public String getUserName() {
        return this.username;
    }

    /**
     * @return le mot de passe de client
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * @return le fichier racine
     */
    public File getRacine() {
        return this.racine;
    }

    public Path getRacinePath() {
        return this.racine.toPath();
    }

    /**
     * @return le token de client
     */
    public String getToken() {
        return this.token;
    }

    /**
     * @return HashMap de serveur
     */
    public HashMap<String, Serveur> getServeurs() {
        return this.serveurs;
    }

    /**
     * @return un serveur ou null s'il n'existe pas
     */
    public Serveur getServeur(String serveurName) {
        return this.getServeurs().get(serveurName);
    }

}
