package it.uniba.sms2122.tourexperience.utility;

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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.uniba.sms2122.tourexperience.model.DTO.MuseoLocalStorageDTO;
import it.uniba.sms2122.tourexperience.model.Museo;
import static it.uniba.sms2122.tourexperience.cache.CacheMuseums.cachePercorsiInLocale;

/**
 * Classe che gestisce tutti salvati nel filesystem locale relativi ai musei.
 */
public class LocalFileMuseoManager extends LocalFileManager {

    public LocalFileMuseoManager(String generalPath) {
        super(generalPath);
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
                try ( Reader reader = new FileReader(path + "/Info.json") )
                {
                    Museo museo = gson.fromJson(reader , Museo.class);
                    museo.setFileUri(generalPath + museo.getNome() + "/" + museo.getNome() + ".png");
                    listaMusei.add(museo);
                }
                catch (IOException | JsonSyntaxException | JsonIOException e) {
                    e.printStackTrace();
                }
            }
        }
        return listaMusei;
    }


    public MuseoLocalStorageDTO createMuseoDirWithFiles(final File filesDir, final String nomeMuseo) {
        final String prefix = "Museums/" + nomeMuseo + "/";
        File museoDir = createLocalDirectoryIfNotExists(filesDir, prefix);
        File stanzeDir = createLocalDirectoryIfNotExists(filesDir,prefix + "Stanze");
        File info = new File(filesDir, prefix + "Info.json");
        File immagine = new File(filesDir, prefix + nomeMuseo + ".png");

        return MuseoLocalStorageDTO.newBuilder()
                .setMuseoDir(museoDir)
                .setStanzeDir(stanzeDir)
                .setInfo(info)
                .setImmaginePrincipale(immagine);
    }

    public void getPercorsiInLocale() throws IOException {
        try (
                DirectoryStream<Path> stream =
                    Files.newDirectoryStream(Paths.get(generalPath))
        ) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) continue;
                String pathMuseo = path + "/Percorsi";
                cachePercorsiInLocale.addAll(Stream.of(new File(pathMuseo).listFiles())
                    .filter(file -> !file.isDirectory())
                    .map(file -> {
                        String name = file.getName();
                        return String.format("%s_%s", path.getFileName(), name.substring(0, name.length()-5));
                    })
                    .collect(Collectors.toList()));
            }
        }
    }

}
