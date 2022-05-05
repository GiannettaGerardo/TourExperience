package it.uniba.sms2122.tourexperience.percorso.OverviewPath;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import java.util.Optional;

import java.util.Optional;

import it.uniba.sms2122.tourexperience.R;
import it.uniba.sms2122.tourexperience.graph.Percorso;
import it.uniba.sms2122.tourexperience.percorso.PercorsoActivity;
import it.uniba.sms2122.tourexperience.utility.filesystem.LocalFilePercorsoManager;

public class OverviewPathFragment extends Fragment {

    View inflater;

    String museoumName;
    String pathName;
    TextView pathNameTextView;
    String pathDescription;
    TextView pathDescriptionTextView;
    Button startPathButton;
    PercorsoActivity parent;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.inflater = inflater.inflate(R.layout.overview_path_fragment, container, false);

        parent = (PercorsoActivity) getActivity();

        museoumName = parent.getNomeMuseo();
        pathName = parent.getNomePercorso();

        return this.inflater;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        setDynamicValuesOnView();
        triggerStartPathButton();
    }

    /**
     * funzione per triggerare il click sul pulsante per far partire la guida
     */
    private void triggerStartPathButton() {

        startPathButton = inflater.findViewById(R.id.startPathButton);

        startPathButton.setOnClickListener(view -> parent.nextStanzeFragment());
    }


    /**
     * Funzione che si occupa di settare i reali valori dinamici delle viste che formano questa fragment
     */
    private void setDynamicValuesOnView() {

        pathNameTextView = inflater.findViewById(R.id.pathName);
        pathNameTextView.setText(pathName);

        pathDescriptionTextView = inflater.findViewById(R.id.pathDescription);
        pathDescriptionTextView.setText(pathDescription);


    }

}
