package it.uniba.sms2122.tourexperience.utility.filesystem;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.uniba.sms2122.tourexperience.R;
import it.uniba.sms2122.tourexperience.model.Museo;
import it.uniba.sms2122.tourexperience.musei.SceltaMuseiFragment;
import it.uniba.sms2122.tourexperience.musei.checkzip.CheckJsonPercorso;
import it.uniba.sms2122.tourexperience.musei.checkzip.CheckZipMuseum;
import it.uniba.sms2122.tourexperience.utility.filesystem.zip.OpenFile;
import it.uniba.sms2122.tourexperience.utility.filesystem.zip.Zip;

import static it.uniba.sms2122.tourexperience.utility.filesystem.zip.MimeType.*;

import static it.uniba.sms2122.tourexperience.cache.CacheMuseums.*;


/**
 * Classe che gestisce tutti salvati nel filesystem locale relativi ai musei.
 */
public class LocalFileMuseoManager extends LocalFileManager {

    public LocalFileMuseoManager(String generalPath) {
        super(generalPath);
    }

    /**
     * Controlla che la directory di un museo esista o no.
     * @param nomeMuseo nome della directory del museo di cui controllare l'esistenza.
     * @return true se la directory esiste, false altrimenti.
     */
    public boolean existsMuseo(final String nomeMuseo) {
        final File f = buildGeneralPath(generalPath, new String[] {nomeMuseo}).toFile();
        return f.exists() && f.isDirectory();
    }

    /**
     * Ottiene il file di info json del museo scelto e ne ritorna un oggetto Museo.
     * @param nomeMuseo nome del museo da ottenere.
     * @return oggetto Museo scelto.
     * @throws IOException
     * @throws JsonSyntaxException
     * @throws JsonIOException
     */
    public Museo getMuseoByName(final String nomeMuseo)
            throws IOException, JsonSyntaxException, JsonIOException {
        try (
                Reader reader = new
                    FileReader(buildGeneralPath(generalPath, new String[] {nomeMuseo, "Info.json"}).toFile())
        ) {
            Museo museo = new Gson().fromJson(reader, Museo.class);
            museo.setFileUri(
                buildGeneralPath(generalPath, new String[] {nomeMuseo, nomeMuseo + IMG_EXTENSION}).toString()
            );
            return museo;
        }
    }


    /**
     * Ottiene la lista di immagini del museo.
     * @param nomeMuseo nome del museo da ottenere.
     * @return lista immagini del museo.
     */
    public List<String> getMuseoImages(String nomeMuseo)
    {
        String img1 = buildGeneralPath(generalPath, new String[] {nomeMuseo, "Immagine_1" + IMG_EXTENSION}).toString();
        String img2 = buildGeneralPath(generalPath, new String[] {nomeMuseo, "Immagine_2" + IMG_EXTENSION}).toString();
        String img3 = buildGeneralPath(generalPath, new String[] {nomeMuseo, "Immagine_3" + IMG_EXTENSION}).toString();

        return Arrays.asList(img1,img2,img3);
    }

    /**
     * Ritorna la lista di musei prelevata dal filesystem locale.
     * Recupera i file di ogni museo e crea gli oggetti corrispondenti
     * inserendoli in una lista.
     * @return lista dei musei presenti nel filesystem.
     * @throws IOException
     */
    public List<Museo> getListMusei() throws IOException {
        Log.v("LocalFileMuseoManager", "chiamato getListMusei()");
        List<Museo> listaMusei = new ArrayList<>();
        Gson gson = new Gson();
        try (
                DirectoryStream<Path> stream =
                        Files.newDirectoryStream(Paths.get(generalPath))
        ) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) continue;
                try ( Reader reader = new FileReader(Paths.get(path.toString(), "Info.json").toString()) )
                {
                    Museo museo = gson.fromJson(reader , Museo.class);
                    museo.setFileUri(buildGeneralPath(generalPath, new String[] {museo.getNome(), museo.getNome()+IMG_EXTENSION}).toString());
                    listaMusei.add(museo);
                }
                catch (IOException | JsonSyntaxException | JsonIOException e) {
                    e.printStackTrace();
                }
            }
        }
        return listaMusei;
    }

    /**
     * Riempie la cache dei percorsi in locale, ma solo con i nomi dei percorsi.
     * @throws IOException
     */
    public void getPercorsiInLocale() throws IOException {
        try (
                DirectoryStream<Path> stream =
                    Files.newDirectoryStream(Paths.get(generalPath))
        ) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) continue;
                String pathMuseo = Paths.get(path.toString(), "Percorsi").toString();
                try {
                    File[] files = new File(pathMuseo).listFiles();
                    if (files == null) continue;
                    addNewPercorsoToCache(path.getFileName().toString(), Stream.of(files)
                        .filter(file -> !file.isDirectory())
                        .map(file -> {
                            String name = file.getName();
                            return name.substring(0, name.length()-5);
                        })
                        .collect(Collectors.toList()));
                }
                catch (NullPointerException e) {
                    Log.e("NullPointerException",
                        "catturata in LocalFileMuseoManager.getPercorsiInLocale()\n" + e.getMessage());
                }
            }
        }
    }

    /**
     * Tenta di salvare il file ottenuto dalla selezione dell'utente.
     * @param fileName nome del file selezionato dall'utente.
     * @param mimeType mime type del file selezionato dall'utente (valgono solo zip e json).
     * @param dto Data Transfer Object contenente i dati utili all'apertura del file.
     * @param frag fragment dalla quale è chiamato questo metodo
     * @return True se il file scelto è corretto, accettabile ed è stato salvato in locale,
     *         False altrimenti.
     */
    public String saveImport(final String fileName, final String mimeType,
                             final OpenFile dto, final SceltaMuseiFragment frag) {
        try {
            Context context = frag.requireContext();
            String resultMessage = context.getString(R.string.mime_type_error);

            if (mimeType.equals(JSON.mimeType())) {
                CheckJsonPercorso cjp = new CheckJsonPercorso(dto, this, context);
                resultMessage = (cjp.check())
                        ? context.getString(R.string.json_import_success, fileName)
                        : context.getString(R.string.json_import_error, fileName);
                if (resultMessage.equals(context.getString(R.string.json_import_success, fileName))) {
                    cjp.updateFirebase();
                }
            }
            else if (mimeType.equals(ZIP.mimeType())) {
                final String nomeMuseo = fileName.substring(0, fileName.length()-4);
                final Zip zip = new Zip(this);
                if (!zip.startUnzip(fileName, dto))
                    return context.getString(R.string.zip_import_error, fileName);
                final List<Object> bool0_hashSet1 = CheckZipMuseum.checkAllJson(generalPath, nomeMuseo);
                if (!((Boolean)bool0_hashSet1.get(0))) {
                    deleteMuseo(nomeMuseo);
                    return context.getString(R.string.zip_import_error, fileName);
                }
                Zip.updateUI(nomeMuseo, (Set<String>) bool0_hashSet1.get(1), frag, this);
                resultMessage = context.getString(R.string.zip_import_success, fileName);
            } else {
                Log.e("LOCAL_IMPORT", "ALTRO NON PREVISTO");
            }
            return resultMessage;
        }
        catch (NullPointerException | IllegalStateException | IOException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    /**
     * Elimina un la cartella di un museo, anche ricorsivamente se non vuota.
     * @param nomeMuseo nom del museo da eliminare.
     */
    public void deleteMuseo(final String nomeMuseo) {
        try {
            deleteDir(buildGeneralPath(generalPath, new String[] {nomeMuseo}).toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
