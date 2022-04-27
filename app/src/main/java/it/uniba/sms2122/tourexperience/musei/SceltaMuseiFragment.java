package it.uniba.sms2122.tourexperience.musei;

import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.uniba.sms2122.tourexperience.R;
import it.uniba.sms2122.tourexperience.main.MainActivity;
import it.uniba.sms2122.tourexperience.model.Museo;
import it.uniba.sms2122.tourexperience.utility.LocalFileMuseoManager;
import static it.uniba.sms2122.tourexperience.cache.CacheMuseums.*;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SceltaMuseiFragment} factory method to
 * create an instance of this fragment.
 */
public class SceltaMuseiFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private List<Museo> listaMusei;
    private LocalFileMuseoManager localFileManager;
    private FirebaseStorage firebaseStorage;

    // Make sure to use the FloatingActionButton for all the FABs
    private FloatingActionButton mAddFab, localStorageFab, cloudFab;
    // These are taken to make visible and invisible along with FABs
    private TextView localStorageTxtView, cloudTxtView;
    // to check whether sub FAB buttons are visible or not.
    private Boolean isAllFabsVisible;

    /**
     * Istanzia la lista dei musei, recuperando i musei dal filesystem
     * locale o dalla cache. Se recuperati dal filesystem, vengono
     * inseriti nella cache.
     * @throws IOException
     */
    private void createListMuseums() throws IOException {
        if (listaMusei == null || listaMusei.isEmpty()) {
            if (cacheMuseums.isEmpty()) {
                listaMusei = localFileManager.getListMusei();
                if (listaMusei.isEmpty()) {
                    Log.v("CACHE_MUSEI", "cache e lista musei vuoti");
                    listaMusei = new ArrayList<>();
                }
                else {
                    Log.v("CACHE_MUSEI", "musei recuperati da locale e inseriti nella cache");
                    replaceMuseumsInCache(listaMusei);
                }
            } else {
                Log.v("CACHE_MUSEI", "musei recuperati dalla cache");
                listaMusei = getAllCachedMuseums();
            }
        } else Log.v("CACHE_MUSEI", "musei giò presenti in memoria");
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            createListMuseums();

            Bundle bundle = this.getArguments();
            if (bundle != null) {
                listaMusei = searchData(listaMusei, bundle.getString("search"));
            }
        } catch (IOException e) {
            Log.e("SceltaMuseiFragment", "SCELTA_MUSEI_ERROR: Lista musei non caricata.");
            listaMusei = new ArrayList<>();
            e.printStackTrace();
        }

        if (listaMusei.isEmpty()) {
            listaMusei.add(new Museo(getContext().getResources().getString(R.string.no_result)));
        }

        // Sending reference and data to Adapter
        MuseiAdapter adapter = new MuseiAdapter((MainActivity) getActivity(), listaMusei, true);
        // Setting Adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        attachQueryTextListener(adapter);
    }

    /**
     * Collega un listener alla barra di ricerca. In particolare
     * il listener che collega permette di filtrare la lista
     * presente nella recyclerView.
     * @param adapter adapter da utilizzate per il filtraggio.
     */
    private void attachQueryTextListener(MuseiAdapter adapter) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {return false;}
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.hideKeyboard(getContext());

        return inflater.inflate(R.layout.fragment_scelta_musei, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localFileManager = new LocalFileMuseoManager(getContext().getFilesDir().toString());
        localFileManager.createLocalDirectoryIfNotExists(getContext().getFilesDir(), "Museums");

        // METODO DI TEST, USARE SOLO UNA VOLTA E POI ELIMINARE
        //test_downloadImageAndSaveInLocalStorage();

        firebaseStorage = FirebaseStorage.getInstance();
        searchView = view.findViewById(R.id.searchviewMusei);

        recyclerView = view.findViewById(R.id.recyclerViewMusei);
        progressBar = view.findViewById(R.id.idPBLoading);
        // Setting the layout as linear layout for vertical orientation
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        setAllTheReference(view);

        // We will make all the FABs and action name texts
        // visible only when Parent FAB button is clicked So
        // we have to handle the Parent FAB button first, by
        // using setOnClickListener you can see below
        mAddFab.setOnClickListener(this::listenerFabMusei);

        localStorageFab.setOnClickListener(view2 -> {
            Log.v("FAB", "cliccato LOCAL");
        });

        cloudFab.setOnClickListener(view2 -> {
            Log.v("FAB", "cliccato CLOUD");
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(null);
            hideFabOptions();
            mAddFab.setImageResource(R.drawable.ic_baseline_close_24);
            mAddFab.setBackgroundTintList(
                    ColorStateList.valueOf(
                            getResources().getColor(R.color.main_red, null))
            );
            MainActivity activity = (MainActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.museums_cloud_import);

            // Ottiene da firebase tutti i percorsi
            getListaPercorsiFromCloudStorage();

            // Il FAB torna allo stato iniziale e la lista di musei torna a contenere i musei presenti in cache
            mAddFab.setOnClickListener(view3 -> {
                listaMusei = getAllCachedMuseums();
                MuseiAdapter adapterMusei = new MuseiAdapter((MainActivity) getActivity(), listaMusei, true);
                recyclerView.setAdapter(adapterMusei);
                attachQueryTextListener(adapterMusei);
                mAddFab.setImageResource(R.drawable.ic_baseline_add_24);
                mAddFab.setOnClickListener(this::listenerFabMusei);
                mAddFab.setBackgroundTintList(
                    ColorStateList.valueOf(
                        getResources().getColor(R.color.mtrl_fab_button_default, null))
                );
                activity.getSupportActionBar().setTitle(R.string.museums);
            });
        });
    }

    /**
     * Permette di aprire e chiudere gli altri FAB a partire da quello principale.
     * @param view
     */
    private void listenerFabMusei(View view) {
        if (!isAllFabsVisible) {
            // when isAllFabsVisible becomes true make all the action name
            // texts and FABs VISIBLE.
            localStorageFab.show();
            cloudFab.show();
            cloudTxtView.setVisibility(View.VISIBLE);
            localStorageTxtView.setVisibility(View.VISIBLE);
            // make the boolean variable true as we have set the sub FABs
            // visibility to GONE
            isAllFabsVisible = true;
        } else {
            hideFabOptions();
        }
    }

    /**
     * Nasconde i pulsanti FAB opzionali.
     */
    private void hideFabOptions() {
        localStorageFab.hide();
        cloudFab.hide();
        cloudTxtView.setVisibility(View.GONE);
        localStorageTxtView.setVisibility(View.GONE);
        isAllFabsVisible = false;
    }


    private void getListaPercorsiFromCloudStorage() {
        if (!cachePercorsi.isEmpty()) {
            Log.v("IMPORT_CLOUD", "with cache cachePercorsi.");
            MuseiAdapter adapterPercorsi = new MuseiAdapter(
                    null,
                    cachePercorsi,
                    false
            );
            recyclerView.setAdapter(adapterPercorsi);
            progressBar.setVisibility(View.GONE);
            attachQueryTextListener(adapterPercorsi);
            return;
        }
        StorageReference listRef = firebaseStorage.getReference().child("Museums");
        listRef.listAll().addOnSuccessListener(listResult -> {
            Log.v("IMPORT_CLOUD", "start download...");
            MuseiAdapter adapterPercorsi = new MuseiAdapter(
                    null,
                    new ArrayList<>(),
                    false
            );
            recyclerView.setAdapter(adapterPercorsi);
            progressBar.setVisibility(View.GONE);
            for (StorageReference folder : listResult.getPrefixes()) {
                String nomeMuseo = folder.getName();
                Task<ListResult> task = folder.child("Percorsi").listAll();
                while (!task.isComplete());
                if (task.isSuccessful()) {
                    for (StorageReference fileJson : task.getResult().getItems()) {
                        String nomePercorso = fileJson.getName();
                        Museo museo = new Museo(
                                nomePercorso.substring(0, nomePercorso.length()-5),
                                nomeMuseo
                        );
                        adapterPercorsi.addMuseum(museo);
                    }
                } else Log.e("IMPORT_CLOUD", "Task is not succesfull");
            }
            attachQueryTextListener(adapterPercorsi);
            replacePercorsiInCache(adapterPercorsi.getListaMusei());
            Log.v("IMPORT_CLOUD", "finish download...");
        }).addOnFailureListener(error -> Log.e("IMPORT_CLOUD", error.toString()));
    }


    /**
     * Imposta solo i riferimenti per il fragment.
     * @param view
     */
    private void setAllTheReference(View view) {
        // Register all the FABs with their IDs
        // This FAB button is the Parent
        mAddFab = view.findViewById(R.id.add_fab);
        // FAB button
        localStorageFab = view.findViewById(R.id.fab_import_from_localstorage);
        cloudFab = view.findViewById(R.id.fab_import_from_cloud);

        // Also register the action name text, of all the FABs.
        localStorageTxtView = view.findViewById(R.id.txtview_import_from_localstorage);
        cloudTxtView = view.findViewById(R.id.txtview_download_from_cloud);

        // Now set all the FABs and all the action name texts as GONE
        localStorageFab.setVisibility(View.GONE);
        cloudFab.setVisibility(View.GONE);
        localStorageTxtView.setVisibility(View.GONE);
        cloudTxtView.setVisibility(View.GONE);

        // make the boolean variable as false, as all the
        // action name texts and all the sub FABs are invisible
        isAllFabsVisible = false;
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.v("SceltaMuseiFragment", "chiamato onSaveInstanceState()");
        ArrayList<String> nomiMusei = new ArrayList<>();
        ArrayList<String> cittaMusei = new ArrayList<>();
        ArrayList<String> tipologieMusei = new ArrayList<>();
        ArrayList<String> uriImmagini = new ArrayList<>();
        for (int i = 0; i < listaMusei.size(); i++) {
            nomiMusei.add(listaMusei.get(i).getNome());
            cittaMusei.add(listaMusei.get(i).getCitta());
            tipologieMusei.add(listaMusei.get(i).getTipologia());
            uriImmagini.add(listaMusei.get(i).getFileUri());
        }
        outState.putStringArrayList("nomi_musei", nomiMusei);
        outState.putStringArrayList("citta_musei", cittaMusei);
        outState.putStringArrayList("tipologie_musei", tipologieMusei);
        outState.putStringArrayList("immagini_musei", uriImmagini);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@NonNull Bundle savedInstanceState) {
        Log.v("SceltaMuseiFragment", "chiamato onViewStateRestored()");
        listaMusei = new ArrayList<>();
        if(savedInstanceState != null && !savedInstanceState.isEmpty())
        {
            Log.v("SceltaMuseiFragment", "chiamato onViewStateRestored() -> savedInstanceState != null");
            ArrayList<String> nomiMusei = savedInstanceState.getStringArrayList("nomi_musei");
            ArrayList<String> cittaMusei = savedInstanceState.getStringArrayList("citta_musei");
            ArrayList<String> tipologieMusei = savedInstanceState.getStringArrayList("tipologie_musei");
            ArrayList<String> uriImmagini = savedInstanceState.getStringArrayList("immagini_musei");
            for (int i = 0; i < nomiMusei.size(); i++) {
                listaMusei.add(new Museo(
                    nomiMusei.get(i),
                    cittaMusei.get(i),
                    tipologieMusei.get(i),
                    uriImmagini.get(i)
                ));
            }
        }
        super.onViewStateRestored(savedInstanceState);
    }

    /**
     * Metodo di test per effettuare il download la prima volta di
     * due musei da firebase e salvarli in locale.
     */
    private void test_downloadImageAndSaveInLocalStorage() {
        ArrayList<String> musei = new ArrayList<>();
        musei.add("Louvre");
        musei.add("Hermitage");
        FirebaseStorage storage = FirebaseStorage.getInstance();
        File fullPath = new File(getContext().getFilesDir() + "/Museums");

        for (String museo : musei) {
            String filePathImmagine = museo+"/"+museo+".png";
            String filePathInfo = museo+"/"+"Info.json";

            // Ottengo il riferimento su Firebase dell'immagine del museo e del file Info.json
            StorageReference rifImmagine = storage.getReference("Museums").child(filePathImmagine);
            StorageReference rifInfo = storage.getReference("Museums").child(filePathInfo);

            File dir = new File(fullPath, museo);
            if (!dir.exists())
                dir.mkdir();

            File localFileImmagine = new File(fullPath, filePathImmagine);
            File localFileInfo = new File(fullPath, filePathInfo);

            rifImmagine.getFile(localFileImmagine).addOnSuccessListener(taskSnapshot -> {
                Log.v("CARICAMENTO IMMAGINE", filePathImmagine+" caricato!");
            }).addOnFailureListener(exception -> {
                Log.e("CARICAMENTO IMMAGINE", filePathImmagine+" NON caricato.");
            });

            rifInfo.getFile(localFileInfo).addOnSuccessListener(taskSnapshot -> {
                Log.v("CARICAMENTO INFO", filePathInfo+" caricato!");
            }).addOnFailureListener(exception -> {
                Log.e("CARICAMENTO INFO", filePathInfo+" NON caricato.");
            });
        }
    }

    private List<Museo> searchData(List<Museo> museums, String string) {
        List<Museo> returnList = new ArrayList<>();

        for(Museo museum : museums){
            if(museum.getNome().equals(string) || museum.getCitta().equals(string) || museum.getTipologia().equals(string)){
                returnList.add(museum);
            }
        }

        return returnList;
    }
}