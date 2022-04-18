package client.flopbox.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import client.flopbox.Main;
import client.flopbox.exception.BadRequestException;
import client.flopbox.exception.ServeurNotFoundException;
import client.flopbox.exception.UnauthorizedException;
import client.flopbox.model.Serveur;
import client.flopbox.model.User;

/***
 * Classe cliente utilitaire pour les requêtes HTTP vers le serveur FlopBox
 */
public class AgentFlopBox {

    /**
     * permet de télecharger un fichier
     * 
     * @param serveur       le serveur sur lequel le fichier est stocké
     * @param fileDownloded fichier telechargé
     * @param remotePath    le chemin de fichier distant
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void downloadFile(Serveur serveur, File fileDownloded, String remotePath)
            throws ClientProtocolException, IOException {
        System.out.println("Download File : " + remotePath + " to " + fileDownloded.getAbsolutePath());
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet((Main.apiURL + serveur.getAlias() + "/file/" + remotePath.replace(" ", "+")));
        request.addHeader("Authorization", "Basic " + User.getInstance().getToken());
        request.addHeader("username", serveur.getUsername());
        request.addHeader("password", serveur.getPassword());

        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        checkHttpResponseCode(response, entity);
        InputStream is = entity.getContent();
        FileOutputStream fos = new FileOutputStream(fileDownloded);
        int inByte;
        while ((inByte = is.read()) != -1) {
            fos.write(inByte);
        }
        is.close();
        fos.close();
    }

    /**
     * permet de upload un fichier sur un serveur distant
     * 
     * @param serveur      serveur sur lequel le fichier va etre stocker
     * @param fileToUpload fichier à uploader
     * @param filePath     chemin de fichier distant
     * @throws ClientProtocolException
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void uploadFile(Serveur serveur, File fileToUpload, String filePath)
            throws ClientProtocolException, IOException, java.text.ParseException {
        System.out.println("Upload File : " + fileToUpload.getAbsolutePath() + " to " + filePath);
        System.out.println(filePath);
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost((Main.apiURL + serveur.getAlias() + "/file/" + filePath.replace(" ", "+")));
        request.addHeader("Authorization", "Basic " + User.getInstance().getToken());
        request.addHeader("username", serveur.getUsername());
        request.addHeader("password", serveur.getPassword());

        HttpEntity requestEntity = MultipartEntityBuilder.create()
                .addBinaryBody("file", fileToUpload)
                .build();
        request.setEntity(requestEntity);
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        checkHttpResponseCode(response, entity);
    }

    /**
     * permet de télecharger un repertoire
     * 
     * @param serveur    serveur sur lequel le repertoire est stocké
     * @param destDir    repertoire de destination
     * @param remotePath chemin de repertoire distant
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void downloadDir(Serveur serveur, File destDir, String remotePath)
            throws ClientProtocolException, IOException {
        System.out.println("Telechargement du dossier " + remotePath + " vers " + destDir.getAbsolutePath());
        User u = User.getInstance();
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(Main.apiURL + serveur.getAlias() + "/directory/" + remotePath.replace(" ", "+"));
        request.addHeader("Authorization", "Basic " + u.getToken());
        request.addHeader("username", serveur.getUsername());
        request.addHeader("password", serveur.getPassword());
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        checkHttpResponseCode(response, entity);
        InputStream is = entity.getContent();
        // unzipper le fichier recu
        FileManager.unzip(is, destDir);
    }

    /**
     * permet de upload un repertoire
     * 
     * @param serveur     serveur
     * @param dirToUpload repertoire
     * @param dirPath     chemin de repertoire
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void uploadDir(Serveur serveur, File dirToUpload, String dirPath)
            throws ClientProtocolException, IOException {
        System.out.println("Upload du dossier " + dirToUpload.getAbsolutePath() + " vers " + dirPath);
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(Main.apiURL + serveur.getAlias() + "/directory/" + dirPath.replace(" ", "+"));
        request.addHeader("Authorization", "Basic " + User.getInstance().getToken());
        request.addHeader("username", serveur.getUsername());
        request.addHeader("password", serveur.getPassword());

        // zipper le repertoire
        File zipFolder = new File("downloads/" + dirToUpload.getName() + ".zip");
        FileManager.zip(dirToUpload.getAbsolutePath(), zipFolder.getAbsolutePath());
        // envoyer le zip
        HttpEntity requestEntity = MultipartEntityBuilder.create()
                .addBinaryBody("file", zipFolder)
                .build();
        request.setEntity(requestEntity);

        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        checkHttpResponseCode(response, entity);
        // suppression du zip apres le upload
        FileManager.DeleteFilesDirectories(zipFolder);
    }

    /**
     * permet de supprimer un fichier ou un repertoire en remote (le deplacer vers
     * le repertoire "deleted")
     * 
     * @param serveur  serveur distant ou le fichier est stocké
     * @param filename nom du fichier ou du repertoire
     * @param oldPath  chemin du fichier ou du repertoire
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void deleteFile(Serveur serveur, String filename, String oldPath)
            throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPut request = new HttpPut(Main.apiURL + serveur.getAlias() + "/rename/");
        request.addHeader("Authorization", "Basic " + User.getInstance().getToken());
        request.addHeader("username", serveur.getUsername());
        request.addHeader("password", serveur.getPassword());
        // formulaire du put
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("oldname", oldPath));
        params.add(new BasicNameValuePair("newname", "/deleted/" + filename));
        request.setEntity(new UrlEncodedFormEntity(params));

        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        checkHttpResponseCode(response, entity);
    }

    /**
     * @param serveur    serveur
     * @param remotePath chemin de fichier distant
     * @return JsonElement
     * @throws ClientProtocolException
     * @throws IOException
     */
    /**
     * permet de recuperer le details des fichiers en remote
     * 
     * @param serveur    serveur distant ou le fichier est stocké
     * @param remotePath chemin de fichier distant
     * @return JsonElement avec tous les donnees
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static JsonElement infoRemoteFiles(Serveur serveur, String remotePath)
            throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(
                Main.apiURL + serveur.getAlias() + "/files-details/" + remotePath.replace(" ", "+"));
        request.addHeader("Authorization", "Basic " + User.getInstance().getToken());
        request.addHeader("username", serveur.getUsername());
        request.addHeader("password", serveur.getPassword());
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        checkHttpResponseCode(response, entity);
        String responseString = EntityUtils.toString(entity, "UTF-8");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonResponse = gson.fromJson(responseString, JsonElement.class);
        return jsonResponse;
    }

    /**
     * permet de recuperer la liste des serveurs
     * 
     * @return liste des serveurs
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static JsonElement getServeursFromFlopBox() throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        User u = User.getInstance();
        HttpGet request = new HttpGet((Main.apiURL + "/alias"));
        request.addHeader("Authorization", "Basic " + u.getToken());
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        checkHttpResponseCode(response, entity);
        String responseString = EntityUtils.toString(entity, "UTF-8");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonResponse = gson.fromJson(responseString, JsonElement.class);
        return jsonResponse;
    }

    /**
     * Lever des exception selon le code de retour de la requete http
     * 
     * @param response reponse http
     * @param entity   entite de la reponse
     * @throws ParseException
     * @throws IOException
     */
    private static void checkHttpResponseCode(HttpResponse response, HttpEntity entity)
            throws ParseException, IOException {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode == 400)
            throw new BadRequestException("BadRequestException : " + EntityUtils.toString(entity, "UTF-8"));
        if (responseCode == 401)
            throw new UnauthorizedException("UnauthorizedException : " + EntityUtils.toString(entity, "UTF-8"));
        if (responseCode == 404)
            throw new ServeurNotFoundException("ServeurNotFoundException : " + EntityUtils.toString(entity, "UTF-8"));
    }

    /**
     * permet de synchroniser serveur<->local
     * 
     * @throws ClientProtocolException
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void synchronisation() throws ClientProtocolException, IOException, java.text.ParseException {
        User u = User.getInstance();
        for (Serveur s : u.getServeurs().values()) {
            System.out.println("Synchronisation du serveur " + s.getAlias());
            JsonObject remote = AgentFlopBox.infoRemoteFiles(s, "").getAsJsonObject();
            JsonObject local = FileManager.infoLocalFiles(s.getRacine().getAbsolutePath()).getAsJsonObject();
            compareFiles(s, local, remote);
        }
    }

    /**
     * permet de comparer les fichiers entre serveur et local
     * 
     * @param s          serveur distant
     * @param localJson  JsonElement des fichiers du local
     * @param remoteJson JsonElement des fichiers du serveur
     * @throws ClientProtocolException
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void compareFiles(Serveur s, JsonObject localJson, JsonObject remoteJson)
            throws ClientProtocolException, IOException, java.text.ParseException {
        compareWithLocalFiles(s, localJson, remoteJson);
        compareWithRemoteFiles(s, localJson, remoteJson);
    }

    /**
     * permet de comparer le local avec le distant si il n'existe pas dans le
     * distant on opload
     * si c'est un repertoire on compare leurs contenus et on change la date de
     * modification du reperoire
     * et si la date de modification est differente et si un fichier on telecharge
     * ou on upload en comparent la date
     * 
     * @param s          serveur
     * @param localJson  JsonObject de fichiers local
     * @param remoteJson JsonObject de fichiers distant
     * @throws ClientProtocolException
     * @throws IOException
     * @throws java.text.ParseException
     */
    public static void compareWithLocalFiles(Serveur s, JsonObject localJson, JsonObject remoteJson)
            throws ClientProtocolException, IOException, java.text.ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // parcourir les fichiers locaux
        for (Map.Entry<String, JsonElement> entry : localJson.entrySet()) {

            JsonObject localObj = entry.getValue().getAsJsonObject();

            // si le fichier n'existe pas en remote
            if (!remoteJson.has(entry.getKey())) {
                System.out.println("Le fichier " + entry.getKey() + " n'existe pas en remote");
                File fileDownload = new File(localObj.get("path").getAsString());
                // on upload le fichier ou dossier
                if (localObj.get("type").getAsString().equals("file")) {
                    String dirPath = localObj.get("parentPath").getAsString()
                            .substring(s.getRacine().getAbsolutePath().length()).replace("\\", "/");
                    AgentFlopBox.uploadFile(s, fileDownload, dirPath);
                } else // on upload le dossier
                {
                    String dirPath = fileDownload.getParentFile().getAbsolutePath()
                            .substring(s.getRacine().getAbsolutePath().length()).replace("\\", "/");
                    AgentFlopBox.uploadDir(s, fileDownload, dirPath);
                }
                remoteJson.add(fileDownload.getName(), localObj);
            } else // existe dans le local et remote
            {
                JsonObject remoteObj = remoteJson.get(entry.getKey()).getAsJsonObject();

                // si c'est un repertoire
                if (localObj.get("type").getAsString().equals("directory")) {
                    // on compare leurs contenus
                    compareFiles(s, localObj.get("content").getAsJsonObject(),
                            remoteObj.get("content").getAsJsonObject());

                    Date dateremote = formatter.parse(remoteObj.get("date").getAsString());
                    // on change la date de modification du reperoire
                    File repertoire = new File(localObj.get("path").getAsString());
                    repertoire.setLastModified(dateremote.getTime());
                }

                Date datelocal = formatter.parse(localObj.get("date").getAsString());
                Date dateremote = formatter.parse(remoteObj.get("date").getAsString());
                // si la date de modification est differente
                if (!datelocal.equals(dateremote)) {
                    System.out.println(entry.getKey() + "|| local : " + datelocal + "| remote : " + dateremote);

                    // si c'est un fichier
                    if (localObj.get("type").getAsString().equals("file")) {
                        // si le local est plus recent que le remote
                        if (datelocal.after(dateremote)) {
                            System.out.println(
                                    "local is newer, upload : " + localObj.get("path").getAsString());
                            // on upload le fichier au serveur
                            AgentFlopBox.uploadFile(s, new File(localObj.get("path").getAsString()),
                                    remoteObj.get("parentPath").getAsString());
                        } else // si le remote est plus recent que le remote
                        {
                            System.out.println(
                                    "remote is newer, download : " + remoteObj.get("path").getAsString());
                            // on telecharge le fichier
                            File fileDownload = new File(localObj.get("path").getAsString());
                            AgentFlopBox.downloadFile(s, fileDownload, remoteObj.get("path").getAsString());
                            fileDownload.setLastModified(dateremote.getTime());
                        }
                    }
                }
            }
        }
    }

    /**
     * permet de comparer le distant avec le local si il n'existe pas dans le local
     * on download,
     * et traite aussi le cas du suppression en local
     * 
     * @param s          serveur distant
     * @param localJson  JsonObject de fichiers local
     * @param remoteJson JsonObject de fichiers distant
     * @throws java.text.ParseException
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static void compareWithRemoteFiles(Serveur s, JsonObject localJson, JsonObject remoteJson)
            throws java.text.ParseException, ClientProtocolException, IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // on compare le remote avec le locale
        for (Map.Entry<String, JsonElement> entry : remoteJson.entrySet()) {
            JsonObject remoteObj = entry.getValue().getAsJsonObject();
            Date dateremote = formatter.parse(remoteObj.get("date").getAsString());
            String type = remoteObj.get("type").getAsString();

            // fichier existe dans le remote mais pas en local
            // et non pas dans le dossier deleted
            if (!remoteObj.get("path").getAsString().startsWith("/deleted") && !localJson.has(entry.getKey())) {
                System.out
                        .println("Fichier existe en remote mais pas en local : " + remoteObj.get("path").getAsString());
                File fileDownload = new File(
                        s.getRacine().getAbsolutePath() + "/" + remoteObj.get("path").getAsString());

                // delete file from remote (put it in deleted folder)
                if (dateremote.before(Main.dateLastSynchro)) {
                    System.out.println(
                            "Fichier trop vieux il faut le supprimer : " + remoteObj.get("path").getAsString());
                    AgentFlopBox.deleteFile(s, remoteObj.get("name").getAsString(),
                            remoteObj.get("path").getAsString());
                    remoteJson.remove(remoteObj.get("name").getAsString());
                    break;
                } else {
                    // si c'est un dossier on le telecharge
                    if (type.equals("directory")) {
                        AgentFlopBox.downloadDir(s, fileDownload.getParentFile(), remoteObj.get("path").getAsString());
                    } else // si c'est un fichier on le telecharge
                    {
                        AgentFlopBox.downloadFile(s, fileDownload, remoteObj.get("path").getAsString());
                    }
                    fileDownload.setLastModified(dateremote.getTime());
                }
            } else if (localJson.has(entry.getKey())) // le fichier existe en local aussi
            {
                JsonObject localObj = localJson.get(entry.getKey()).getAsJsonObject();

                if (type.equals("directory")) {
                    // on compare leurs contenus
                    compareFiles(s, localObj.get("content").getAsJsonObject(),
                            remoteObj.get("content").getAsJsonObject());

                    // on change la fate de modification du reperoire
                    File repertoire = new File(localObj.get("path").getAsString());
                    repertoire.setLastModified(dateremote.getTime());
                }
            }
        }
    }
}
