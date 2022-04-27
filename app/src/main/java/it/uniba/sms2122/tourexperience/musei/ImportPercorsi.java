package it.uniba.sms2122.tourexperience.musei;

import static it.uniba.sms2122.tourexperience.cache.CacheMuseums.*;

import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import it.uniba.sms2122.tourexperience.model.DTO.MuseoLocalStorageDTO;
import it.uniba.sms2122.tourexperience.model.Museo;
import it.uniba.sms2122.tourexperience.utility.LocalFileMuseoManager;

public class ImportPercorsi {

    private final static FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private static LocalFileMuseoManager localFileManager = null;
    private final File filesDir;
    private final String filesDirStr;

    public ImportPercorsi(final File filesDir) {
        this.filesDir = filesDir;
        this.filesDirStr = filesDir.toString();
        if (localFileManager == null) {
            localFileManager = new LocalFileMuseoManager(this.filesDirStr);
        }
    }

    /**
     * Esegue il download e salvataggio in locale del percorso selezionato e,
     * nel caso il museo corrispondente non fosse presente in locale, scarica
     * e salva in locale anche tutte le altre informazioni del museo.
     * @param nomePercorso
     * @param nomeMuseo
     */
    public void downloadMuseoPercorso(final String nomePercorso, final String nomeMuseo) {
        if (cacheMuseums.get(nomeMuseo) == null &&
                cacheMuseums.get(nomeMuseo.toLowerCase()) == null) {
            downloadAll(nomePercorso, nomeMuseo);
        } else {
            downloadPercorso(nomePercorso, nomeMuseo);
        }
    }

    /**
     * Esegue il download e salvataggio in locale di un museo e tutte
     * le informazioni ad esso associato, tranne i file json dei percorsi.
     * @param nomeMuseo
     */
    public void downloadAll(final String nomePercorso, final String nomeMuseo) {
        StorageReference storage = firebaseStorage.getReference("Museums/" + nomeMuseo);
        storage.listAll().addOnSuccessListener(listResult -> {
            if (!listResult.getPrefixes().isEmpty() && !listResult.getItems().isEmpty()) {
                Log.v("DOWNLOAD_MUSEO", "Non Vuoto");

                // Creo i riferimenti alle cartelle e i files principali
                MuseoLocalStorageDTO dto = localFileManager
                        .createMuseoDirWithFiles(filesDir, nomeMuseo);

                // Scarico l'immagine principale del museo
                dto.getImmaginePrincipale().ifPresent(immagine ->
                storage.child(nomeMuseo + ".png").getFile(immagine)
                    .addOnFailureListener(e -> Log.v("ERROR_immagine_principale", e.getMessage()))
                    .addOnSuccessListener(taskSnapImage -> {
                        // Scarico il json di info del museo
                        // La creazione del file Info.json del museo, avviene solo dopo
                        // la creazione dell'immagine principale
                        dto.getInfo().ifPresent(info -> storage.child("Info.json").getFile(info)
                        .addOnFailureListener(e -> Log.v("ERROR_info", e.getMessage()))
                        .addOnSuccessListener(taskSnapshot -> {
                            try ( Reader reader = new FileReader(info) ) {
                                Museo museo = new Gson().fromJson(reader , Museo.class);
                                museo.setFileUri(filesDirStr
                                        + "/Museums/" + museo.getNome()
                                        + "/" + museo.getNome()
                                        + ".png");
                                cacheMuseums.put(nomeMuseo, museo);
                                Log.v("CACHE", nomeMuseo + " scaricato e cachato correttamente");
                            } catch (IOException | JsonSyntaxException | JsonIOException e) {
                                e.printStackTrace();
                            }
                        }));
                    }));

                // TODO per ora scarico solo i file json delle varie stanze
                // Scarico tutte le cartelle delle stanze con i file json delle stanze
                dto.getStanzeDir().ifPresent(stanzeDir -> {
                    storage.child("Stanze").listAll().addOnSuccessListener(listStanze -> {
                        for (StorageReference dirStanza : listStanze.getPrefixes()) {
                            File dirStanzaLocale = localFileManager
                                    .createLocalDirectoryIfNotExists(stanzeDir, dirStanza.getName());
                            File jsonStanza = new File(
                                    dirStanzaLocale, "Info_stanza.json");
                            dirStanza.child("Info_stanza.json").getFile(jsonStanza)
                                    .addOnFailureListener(e -> Log.e("ERROR_Info_stanza.json", e.getMessage()));
                        }
                    }).addOnFailureListener(e -> Log.e("ERROR_stanze", e.getMessage()));
                });

                // Scarica infine il percorso scelto
                downloadPercorso(nomePercorso, nomeMuseo);
            } else {
                Log.e("DOWNLOAD_MUSEO",
                        String.format("museo %s non esistente nello storage in cloud", nomeMuseo));
            }
        }).addOnFailureListener(error -> Log.e("DOWNLOAD_MUSEO", error.getMessage()));
    }

    /**
     * Scarica il percorso scelto da firebase e lo salva in locale
     * @param nomePercorso
     * @param nomeMuseo
     */
    public void downloadPercorso(final String nomePercorso, final String nomeMuseo) {
        final String prefix = "Museums/" + nomeMuseo + "/Percorsi/";
        Log.v("PERCORSO", nomePercorso);
        StorageReference filePercorso = firebaseStorage.getReference(prefix + nomePercorso + ".json");
        File dirPercorsi = localFileManager.createLocalDirectoryIfNotExists(filesDir, prefix);
        File jsonPercorso = new File(dirPercorsi, nomePercorso+".json");
        filePercorso.getFile(jsonPercorso)
        .addOnFailureListener(e -> Log.e("DOWNLOAD_PERCORSO", e.getMessage()))
        .addOnSuccessListener(taskSnapshot ->
            Log.v("DOWNLOAD_PERCORSO",
                String.format("Download del percorso %s eseguito correttamente", nomePercorso)));
    }
}
