package client.flopbox.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.ClientProtocolException;

import client.flopbox.exception.ConfigFileException;
import client.flopbox.exception.ServeurNotFoundException;
import client.flopbox.model.Serveur;
import client.flopbox.model.User;

/**
 * Classe pour la gestion des fichiers locaux
 * @author Hatim  Mrabet el khomssi   Nouria Aitkheddache 		
 */
public class FileManager {

    /**
     * Fonction pour extraire les donnees du fichier "config.json"
     * @return un objet Json contenue les information du fichier "config.json" 
     */
    public static JsonObject getJsonFileContent() {
        Gson gson = new Gson();
        try {
            JsonElement json = gson.fromJson(new FileReader("config.json"), JsonElement.class);
            return json.getAsJsonObject();
        } catch (IOException e) {
            throw new ConfigFileException("Config File Not Found");
        }
    }

    /**
     * permet la creation du dossier racine de la synchronisation
     * avec la creation de tous ces parents s'ils n'existent pas
     * @throws IOException
     */
    public static void initRacine() throws IOException {
        User u = User.getInstance();
       if(!u.getRacine().exists())
            Files.createDirectories(u.getRacinePath());
    }
    

    /**
     * permet la creation d'un dossier racine pour chaque serveur disponible sur FlopBox
     * @throws ClientProtocolException
     * @throws IOException
     * @throws ParseException
     */
    public static void initServeurs() throws ClientProtocolException, IOException, ParseException {
        System.out.println("Initialisation des serveurs");
        User u = User.getInstance();
        // recuperation des serveurs disponible sur FlopBox
        JsonObject je = AgentFlopBox.getServeursFromFlopBox().getAsJsonObject();
        // creation d'un repertoire pour chaque serveur
        for(Entry<String, JsonElement> key : je.entrySet()) {
            File f = new File(u.getRacine().getAbsolutePath() + "/" + key.getKey());
            // supprimer tout le contenu existant
            if(f.exists())
                FileManager.DeleteFilesDirectories(f);
            // creer le repertoire
            f.mkdir();
            // si un serveur qui existe sur FlopBox et n'est pas enregistrer dans le fichier config.json
            // on leve une exception
            Serveur s = u.getServeur(key.getKey());
            if( s == null)
                throw new ServeurNotFoundException("Serveur " + key.getKey() + " Not Found in config.json");
            s.setRacine(f);
            // telecharger le contenu du repertoire racine du serveur 
            AgentFlopBox.downloadDir(s, s.getRacine().getParentFile(), "");
            // mettre à jour les date de modification des fichiers
            updateLastModificationDate("");
            // supprimer le dossier "deleted" du local
            File deleted = new File(s.getRacine().getAbsolutePath() + "/deleted");
            if(deleted.exists())
                FileManager.DeleteFilesDirectories(deleted);
        }
    }

    /**
     * permet le changement de la date de derniere mise a jour de chaque fichier
     * @param path du fichier sur le serveur
     * @throws ClientProtocolException
     * @throws IOException
     * @throws ParseException
     */
    private static void updateLastModificationDate(String path) throws ClientProtocolException, IOException, ParseException {
        User u = User.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for(Serveur s : u.getServeurs().values()) {
            JsonObject je = AgentFlopBox.infoRemoteFiles(s, path).getAsJsonObject();
            for(Entry<String, JsonElement> key : je.entrySet()) {
                File f = new File(s.getRacine().getAbsolutePath() + "/" + path +"/" + key.getKey());
                if(f.exists()) {
                    f.setLastModified(df.parse(key.getValue().getAsJsonObject().get("date").getAsString()).getTime());
                }
                // modification des dates du contenue du repertoire
                if(f.isDirectory()) {
                    updateLastModificationDate(path + "/" + key.getKey());
                }
            }
        }
    }

    /**
     * cete methode permet de créer un repertoire dans un repertoire passer en parametre avec le nom d'un répertoire zipé 
     * @param destinationDir nom de chemin de répertoire.
     * @param zipEntry repertoire ZIP. 
     * @return un fichier 
     */
    public static File newFile(File destinationDir, ZipEntry zipEntry) {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath, destFilePath;
        try {
            destDirPath = destinationDir.getCanonicalPath();
            destFilePath = destFile.getCanonicalPath();
            if (!destFilePath.startsWith(destDirPath + File.separator)) {
                throw new RuntimeException("Entry is outside of the target dir: " + zipEntry.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException: " + e.getMessage());
        }
        return destFile;
    }

    /**
     * fonctiorn permet de décompresser un reperetoire zip 
     * @param is InputStream du fichier zip
     * @param destDir un repertoire ou desiper le fichier
     * @throws IOException
     */
    public static void unzip(InputStream is, File destDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = FileManager.newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new RuntimeException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new RuntimeException("Failed to create directory " + parent);
                }
                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

	/**
	 * permet la preparation pour ziper un fichier 
	 * @param sourceFile chemin de repertoire à zipé
	 * @param destDirectory le nom de repertoire où enregistrer le fichier zipé
	 * @throws IOException
	 */
	public static void zip(String sourceFile, String destDirectory) throws IOException {
		FileOutputStream fos = new FileOutputStream(destDirectory);
		ZipOutputStream zipOut = new ZipOutputStream(fos);
		File fileToZip = new File(sourceFile);
		zipFile(fileToZip, fileToZip.getName(), zipOut);
		zipOut.close();
		fos.close();
	}

    /**
     * permet de ziper un repertoire ou un fichier
     * @param fileToZip : fichier à zipper
     * @param fileName : nom du fichier
     * @param zipOut : flux de sortie
     * @throws IOException
     */
    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    /**
     * permet de supprimer un repertoire ou fichier et son contenu 
     * @param file repertoire ou fichier 
     */
    public static void DeleteFilesDirectories(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    throw new RuntimeException("Delete Files Exception : " + e.getMessage());
                }
            }
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    /**
     * Avoir le details de tous les fichiers locaux de la racine du serveur
     * @return JsonElement contenant les details des fichiers locaux
     */
    public static JsonElement infoLocalFiles()
    {
        return infoLocalFiles(User.getInstance().getRacine().getAbsolutePath());
    }


    /**
     * Avoir le details de tous les fichiers locaux d'un repertoire donné en parametre
     * @param path : le path où chercher les données
     * @return JsonElement contenant les details des fichiers locaux dans le path
     */
    public static JsonElement infoLocalFiles(String path)
    {
        // recuperer le fichier dans le path
        File currentFile = new File(path);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonArray = new JsonObject();
        DateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // parcourir tous les fichiers dans ce path
        for(File f : currentFile.listFiles())
        {
            // creation d'un objet avec le detils de chaque fichier
            JsonObject obj = new JsonObject();
            obj.addProperty("name", f.getName());
            try {
                obj.addProperty("size", Files.size(f.toPath()));
            } catch (IOException e) {
                throw new RuntimeException("Exception: "+e.getMessage());
            }
            obj.addProperty("date", dateFormater.format(f.lastModified()));
            obj.addProperty("path", f.getAbsolutePath());
            obj.addProperty("parentPath", f.getParentFile().getAbsolutePath());
            if(f.isDirectory())
            {
                obj.addProperty("type","directory");
                obj.add("content", infoLocalFiles(f.getAbsolutePath()));
            }
            else
                obj.addProperty("type","file");
            // ajouter au objet du retour
            jsonArray.add(f.getName(), obj);
        }
        JsonElement jsonResponse = gson.fromJson(jsonArray, JsonElement.class);
        return jsonResponse;
    }
}
