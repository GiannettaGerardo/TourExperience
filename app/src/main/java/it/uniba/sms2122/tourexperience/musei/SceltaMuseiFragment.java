package it.uniba.sms2122.tourexperience.musei;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
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
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.uniba.sms2122.tourexperience.R;
import it.uniba.sms2122.tourexperience.main.MainActivity;
import it.uniba.sms2122.tourexperience.model.Museo;
import it.uniba.sms2122.tourexperience.utility.connection.NetworkConnectivity;
import it.uniba.sms2122.tourexperience.utility.filesystem.LocalFileMuseoManager;
import it.uniba.sms2122.tourexperience.utility.filesystem.zip.DTO.OpenFileAndroidStorageDTO;
import it.uniba.sms2122.tourexperience.utility.filesystem.zip.OpenFile;

import static it.uniba.sms2122.tourexperience.cache.CacheMuseums.*;


public class SceltaMuseiFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private List<Museo> listaMusei;
    private LocalFileMuseoManager localFileManager;
    private final int requestCodeGC = 10;
    private MuseiAdapter generalAdapter;

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
        } else Log.v("CACHE_MUSEI", "musei già presenti in memoria");
    }


    /**
     * Collega un listener alla barra di ricerca. In particolare
     * il listener che collega permette di filtrare la lista
     * presente nella recyclerView.
     */
    public void attachQueryTextListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {return false;}
            @Override
            public boolean onQueryTextChange(String newText) {
                generalAdapter.getFilter().filter(newText);
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

        File filesDir = view.getContext().getFilesDir();
        localFileManager = new LocalFileMuseoManager(filesDir.toString());

        searchView = view.findViewById(R.id.searchviewMusei);

        recyclerView = view.findViewById(R.id.recyclerViewMusei);
        progressBar = view.findViewById(R.id.idPBLoading);
        // Setting the layout as linear layout for vertical orientation
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        setAllTheReference(view);

        mAddFab.setOnClickListener(this::listenerFabMusei);

        localStorageFab.setOnClickListener(view2 -> {
            final Runnable openFileExplorer = () -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, requestCodeGC);
                hideFabOptions();
            };
            final SharedPreferences sp = requireActivity().getPreferences(Context.MODE_PRIVATE);
            final String spKey = getString(R.string.do_not_show_again_local_import);
            final boolean doNotShowAgain = sp.getBoolean(spKey, false);
            if (!doNotShowAgain) {
                new AlertDialog.Builder(view2.getContext())
                .setTitle(getString(R.string.local_import_dialog_title))
                .setMessage(getString(R.string.local_import_message))
                .setPositiveButton("OK", (dialog, whichButton) -> openFileExplorer.run())
                .setNeutralButton(getString(R.string.do_not_show_again), (dialog, whichButton) -> {
                    dialog.dismiss();
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putBoolean(spKey, true);
                    editor.apply();
                    openFileExplorer.run();
                })
                .show();
            } else openFileExplorer.run();
        });

        cloudFab.setOnClickListener(view2 -> NetworkConnectivity.check(isConnected -> {
            if (!isConnected) {
                Toast.makeText(getContext(), getContext().getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
                return;
            }
            //progressBar.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(null);
            hideFabOptions();
            mAddFab.setImageResource(R.drawable.ic_baseline_close_24);
            MainActivity activity = (MainActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.museums_cloud_import);
            Back backToMuseumsList = new BackToMuseumsList(this, mAddFab);
            // Il FAB torna allo stato iniziale e la lista di musei torna a contenere i musei presenti in cache
            mAddFab.setOnClickListener((view3) -> {
                searchView.setQueryHint(getString(R.string.search_museums));
                backToMuseumsList.back(view3);
            });

            // Impostando questo oggetto in ImportPercorsi, potrò evocare il suo metodo back
            // per tornare allo stato precedente come se avessi cliccato il pulsante
            ImportPercorsi.setBackToMuseumsList(backToMuseumsList);
            // Ottiene da firebase tutti i percorsi
            getListaPercorsiFromCloudStorage();
            searchView.setQueryHint(getString(R.string.search_paths));
        }));
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
            listaMusei.add(Museo.getMuseoVuoto(getResources()));
        }

        if (recyclerView.getAdapter() == null) {
            attachNewAdapter(new MuseiAdapter(this, listaMusei, true));
        }
    }

    /**
     * Permette di aprire e chiudere i FAB opzionali.
     * @param view
     */
    public void listenerFabMusei(View view) {
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


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Gestisce solo l'ottenimento del file .zip / .json, avvenuto tramite Intent implicito.
        if (requestCode == requestCodeGC) {
            if (resultCode == MainActivity.RESULT_OK) {
                Uri returnUri = data.getData();
                String mimeType = getActivity().getContentResolver().getType(returnUri);
                String fileName = DocumentFile.fromSingleUri(getContext(), returnUri).getName();
                OpenFile dto = new OpenFileAndroidStorageDTO(getContext(), returnUri);
                String resultMessage = localFileManager.saveImport(fileName, mimeType, dto, this);
                Toast.makeText(getContext(), resultMessage, Toast.LENGTH_SHORT).show();
            } else {
                Log.e("SceltaMuseiFragment.onActivityResult", "resultCode " + resultCode);
            }
        }
    }

    /**
     * Ritorna la lista dei percorsi presenti in cloud su Firebase.
     */
    private void getListaPercorsiFromCloudStorage() {
        generalAdapter = new MuseiAdapter(this, new ArrayList<>(), false);
        Log.v("IMPORT_CLOUD", "start download...");
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("Museums_v2");
        ValueEventListener listener = new
            ListaPercorsiFromCloud(this, progressBar, recyclerView);
        db.addValueEventListener(listener);
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
        if (listaMusei != null && !isListaMuseiEmpty()) {
            ArrayList<String> nomiMusei = new ArrayList<>();
            ArrayList<String> cittaMusei = new ArrayList<>();
            ArrayList<String> descrizioneMusei = new ArrayList<>();
            ArrayList<String> tipologieMusei = new ArrayList<>();
            ArrayList<String> uriImmagini = new ArrayList<>();
            for (int i = 0; i < listaMusei.size(); i++) {
                nomiMusei.add(listaMusei.get(i).getNome());
                cittaMusei.add(listaMusei.get(i).getCitta());
                descrizioneMusei.add(listaMusei.get(i).getDescrizione());
                tipologieMusei.add(listaMusei.get(i).getTipologia());
                uriImmagini.add(listaMusei.get(i).getFileUri());
            }
            outState.putStringArrayList("nomi_musei", nomiMusei);
            outState.putStringArrayList("citta_musei", cittaMusei);
            outState.putStringArrayList("descrizione_musei", descrizioneMusei);
            outState.putStringArrayList("tipologie_musei", tipologieMusei);
            outState.putStringArrayList("immagini_musei", uriImmagini);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onViewStateRestored(@NonNull Bundle savedInstanceState) {
        Log.v("SceltaMuseiFragment", "chiamato onViewStateRestored()");
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null)
                actionBar.setTitle(R.string.museums);
        }
        if(savedInstanceState != null && !savedInstanceState.isEmpty()) {
            listaMusei = new ArrayList<>();
            Log.v("SceltaMuseiFragment", "ripristino stato precedente della lista musei");
            ArrayList<String> nomiMusei = savedInstanceState.getStringArrayList("nomi_musei");
            if (nomiMusei == null || nomiMusei.isEmpty()) {
                super.onViewStateRestored(savedInstanceState);
                return;
            }
            ArrayList<String> cittaMusei = savedInstanceState.getStringArrayList("citta_musei");
            ArrayList<String> descrizioneMusei = savedInstanceState.getStringArrayList("descrizione_musei");
            ArrayList<String> tipologieMusei = savedInstanceState.getStringArrayList("tipologie_musei");
            ArrayList<String> uriImmagini = savedInstanceState.getStringArrayList("immagini_musei");
            for (int i = 0; i < nomiMusei.size(); i++) {
                listaMusei.add(new Museo(
                        nomiMusei.get(i),
                        cittaMusei.get(i),
                        descrizioneMusei.get(i),
                        tipologieMusei.get(i),
                        uriImmagini.get(i)
                ));
            }
        }
        super.onViewStateRestored(savedInstanceState);
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

    /**
     * Ricarica la lista musei nel caso sia vuota o nulla e ricrea l'adapter.
     */
    public void refreshListaMusei() {
        try {
            createListMuseums();
        } catch (IOException e) {
            Log.e("SceltaMuseiFragment", "SCELTA_MUSEI_ERROR: Lista musei non caricata.");
            listaMusei = new ArrayList<>();
            e.printStackTrace();
        }

        if (listaMusei.isEmpty()) {
            listaMusei.add(Museo.getMuseoVuoto(getResources()));
        }

        attachNewAdapter(new MuseiAdapter(this, listaMusei, true));
    }

    /**
     * La lista è considerata vuota se è presente un solo museo ed
     * esso è il Museo vuoto.
     * @return True se nella lista è presente un solo museo ed è
     * il museo vuoto, False altrimenti.
     */
    public boolean isListaMuseiEmpty() {
        return listaMusei.size() == 1 && listaMusei.get(0)
            .equals(Museo.getMuseoVuoto(getResources()));
    }

    public void setListaMusei(List<Museo> listaMusei) {
        this.listaMusei = listaMusei;
    }

    public List<Museo> getListaMusei() {
        return listaMusei;
    }

    public MuseiAdapter getGeneralAdapter() {
        return generalAdapter;
    }

    public void attachNewAdapter(@NonNull MuseiAdapter adapter) {
        generalAdapter = adapter;
        recyclerView.setAdapter(generalAdapter);
        attachQueryTextListener();
    }
}