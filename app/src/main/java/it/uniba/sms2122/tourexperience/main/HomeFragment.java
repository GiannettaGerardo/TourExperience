package it.uniba.sms2122.tourexperience.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

import it.uniba.sms2122.tourexperience.R;
import it.uniba.sms2122.tourexperience.holders.UserHolder;
import it.uniba.sms2122.tourexperience.utility.connection.NetworkConnectivity;

/**
 * Fragment per la schermata Home
 * @author Catignano Francesco
 */
public class HomeFragment extends Fragment {

    private AutoCompleteTextView autoCompleteTextView;
    private static DatabaseReference reference;
    private static final String TABLE_NAME = "Museums";
    private CardView classificaVoti;
    private CardView classificaVisitati;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO Qui bisogna prendere i dati sui percorsi preferiti
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        UserHolder.getInstance().getUser(
                (user) -> {
                    // Imposto il titolo del fragment col nome dell'utente e lo faccio
                    // ogni volta che torno su questo fragment
                    Objects.requireNonNull(((MainActivity)requireActivity()).getSupportActionBar()).setTitle(getString(R.string.hello, user.getName()));
                },
                (String errorMsg) -> {
                    Objects.requireNonNull(((MainActivity)requireActivity()).getSupportActionBar()).setTitle(getString(R.string.hello, "guest"));
                }
        );

        autoCompleteTextView = view.findViewById(R.id.autocomplete);
        autoCompleteTextView.setThreshold(1);
        reference = FirebaseDatabase.getInstance().getReference(TABLE_NAME);

        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                museumsSearch(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        reference.addListenerForSingleValueEvent(eventListener);

        classificaVoti = view.findViewById(R.id.classifica_voti);
        classificaVisitati = view.findViewById(R.id.classifica_visitai);

        classificaVoti.setOnClickListener(view1 -> {
            if(NetworkConnectivity.check(requireContext())){
                Bundle bundle = new Bundle();
                bundle.putInt("ranking", 1);
                ((MainActivity) requireActivity()).replaceRankingFragment(bundle);
            }
        });

        classificaVisitati.setOnClickListener(view12 -> {
            if(NetworkConnectivity.check(requireContext())) {
                Bundle bundle = new Bundle();
                bundle.putInt("ranking", 2);
                ((MainActivity) requireActivity()).replaceRankingFragment(bundle);
            }
        });


    }

    /**
     * Funzione che gestione la funzionalità di ricerca dei musei in base
     * al nome, città e tipologia del museo
     * @param snapshot, risultato ottenuto una volta intepellato firebase per ottenere la
     *                  lista di nomi, città e tipologie dei musei presenti
     */
    private void museumsSearch(DataSnapshot snapshot) {
        ArrayList<String> arrayList = new ArrayList<>();
        if(snapshot.exists()){
            //valorizzazione di arrayList
            for(DataSnapshot ds : snapshot.getChildren()){
                String nome = ds.child("nome").getValue(String.class);
                arrayList.add(nome);
                String citta = ds.child("citta").getValue(String.class);
                arrayList.add(citta);
                String tipologia = ds.child("tipologia").getValue(String.class);
                arrayList.add(tipologia);
            }

            SearchAdapter adapter = new SearchAdapter(requireContext(), arrayList);
            autoCompleteTextView.setAdapter(adapter);

            autoCompleteTextView.setOnItemClickListener((adapterView, view, i, l) -> {
                String string = autoCompleteTextView.getText().toString();

                if(!string.equals(requireContext().getResources().getString(R.string.no_result_1)
                        + "\n" + requireContext().getResources().getString(R.string.no_result_2))){
                    Bundle bundle = new Bundle();
                    bundle.putString("search", string);
                    ((MainActivity) requireActivity()).replaceSceltaMuseiFragment(bundle);
                    autoCompleteTextView.setText(R.string.empty_phrase);
                } else {
                    autoCompleteTextView.setText(R.string.empty_phrase);
                }
            });
        }
    }
}