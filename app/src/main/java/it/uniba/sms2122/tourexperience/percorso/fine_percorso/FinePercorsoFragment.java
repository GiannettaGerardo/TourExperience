package it.uniba.sms2122.tourexperience.percorso.fine_percorso;

import static it.uniba.sms2122.tourexperience.cache.CacheMuseums.getMuseoByName;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import it.uniba.sms2122.tourexperience.R;
import it.uniba.sms2122.tourexperience.cache.CacheMuseums;
import it.uniba.sms2122.tourexperience.percorso.PercorsoActivity;
import it.uniba.sms2122.tourexperience.utility.connection.NetworkConnectivity;
import it.uniba.sms2122.tourexperience.utility.filesystem.LocalFileManager;
import it.uniba.sms2122.tourexperience.utility.ranking.FileShare;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FinePercorsoFragment} factory method to
 * create an instance of this fragment.
 */
public class FinePercorsoFragment extends Fragment {

    private Bundle savedInstanceState;
    private ImageView imageView;
    private TextView textView;

    private RatingBar ratingBar;
    private Button buttonVote;
    private Button buttonSkip;
    private RadioGroup radioGroupQuestion2;
    private RadioGroup radioGroupQuestion3;
    private CheckBox checkBox1;
    private CheckBox checkBox2;
    private CheckBox checkBox3;
    private CheckBox checkBox4;
    private CheckBox checkBox5;

    private FileShare fileShare;
    private PercorsoActivity parent;
    private String nomeMuseo;
    private String nomePercorso;

    private DatabaseReference db;
    private Task<DataSnapshot> snapshotVoti;
    private Task<DataSnapshot> snapshotNumStarts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fine_percorso, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        parent = (PercorsoActivity) requireActivity();
        assert parent != null;
        Objects.requireNonNull(parent.getSupportActionBar()).setDisplayHomeAsUpEnabled(false);

        radioGroupQuestion2 = view.findViewById(R.id.radio_group_question_2);
        radioGroupQuestion3 = view.findViewById(R.id.radio_group_question_3);

        checkBox1 = view.findViewById(R.id.checkbox_question4_id1);
        checkBox2 = view.findViewById(R.id.checkbox_question4_id2);
        checkBox3 = view.findViewById(R.id.checkbox_question4_id3);
        checkBox4 = view.findViewById(R.id.checkbox_question4_id4);
        checkBox5 = view.findViewById(R.id.checkbox_question4_id5);

        textView = view.findViewById(R.id.nome_item_museo_end);
        imageView = view.findViewById(R.id.icona_item_museo_end);

        ratingBar = view.findViewById(R.id.ratingBar);
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        stars.getDrawable(1).setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);

        buttonVote = view.findViewById(R.id.end_quiz);
        buttonSkip = view.findViewById(R.id.skip_quiz);

        if (savedInstanceState == null) {
            nomeMuseo = parent.getNomeMuseo();
            nomePercorso = parent.getPath().getNomePercorso();
        } else {
            this.nomeMuseo = savedInstanceState.getString("nomeMuseo");
            this.nomePercorso = savedInstanceState.getString("nomePercorso");
            //lo stato non è nullo ma il fragment è stato riaperto attraverso onBackPressed per cui comunque viene ricreato da 0 e non ha valori inzializzati
            if (this.nomeMuseo == null) {
                nomeMuseo = parent.getNomeMuseo();
            }
            if (this.nomePercorso == null) {
                nomePercorso = parent.getPath().getNomePercorso();
            }
        }

        textView.setText(getString(R.string.museum, nomeMuseo) + "\n" + getString(R.string.path_end));
        try{
            imageView.setImageURI(Uri.parse(getMuseoByName(nomeMuseo, view.getContext()).getFileUri()));
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        if(checkConnectivity()){
            buttonVoteSetOnClickListener();
        }
        buttonSkipSetOnClickListener();
    }

    private boolean isQuizComplete() {
        boolean isQuizComplete = true;

        if(radioGroupQuestion2.getCheckedRadioButtonId() == -1){
            isQuizComplete = false;
        } else if(radioGroupQuestion3.getCheckedRadioButtonId() == -1){
            isQuizComplete = false;
        } else if(!checkBox1.isChecked() && !checkBox2.isChecked() && !checkBox3.isChecked() && !checkBox4.isChecked() && !checkBox5.isChecked()){
            isQuizComplete = false;
        }

        return isQuizComplete;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.share, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.shareItem:
                if(isQuizComplete()){
                    writeFileShare();
                    Uri uri = FileProvider.getUriForFile(requireContext(), "it.uniba.sms2122.tourexperience.fileprovider", fileShare.getTxt());
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    sendIntent.setType("text/plain");

                    Intent shareIntent = Intent.createChooser(sendIntent, null);
                    startActivity(shareIntent);
                } else {
                    Toast.makeText(getContext(), requireContext().getString(R.string.quiz_non_completato), Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        File txtsFolder = LocalFileManager.createLocalDirectoryIfNotExists(requireActivity().getFilesDir(), "txts");
        File txt = new File (txtsFolder, "form.txt");

        if(txt.exists())
            txt.delete();

        try {
            txt.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileShare = new FileShare(txt);

        if(savedInstanceState != null){
            try{
                imageView.setImageURI(Uri.parse(Objects.requireNonNull(CacheMuseums.cacheMuseums.get(nomeMuseo)).getFileUri()));
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }

    private void writeFileShare() {
        String answer;
        fileShare.writeToFile(getString(R.string.museum, nomeMuseo) + " , " + getString(R.string.path, nomePercorso) + "\n\n");
        fileShare.writeToFile(getString(R.string.question_1) + "\n" + Math.round(ratingBar.getRating() * 100.0) / 100.0 + "\n\n");
        if(radioGroupQuestion2.getCheckedRadioButtonId() == R.id.radio_question2_id1)
            answer = getString(R.string.yes);
        else
            answer = getString(R.string.no);

        fileShare.writeToFile(getString(R.string.question_2) + "\n" + answer + "\n\n");

        if(radioGroupQuestion2.getCheckedRadioButtonId() == R.id.radio_question3_id1)
            answer = getString(R.string.yes);
        else
            answer = getString(R.string.no);
        fileShare.writeToFile(getString(R.string.question_3) + "\n" + answer + "\n\n");

        answer = null;
        if(checkBox1.isChecked())
            answer = getString(R.string.fun) + ", ";
        if(checkBox2.isChecked())
            answer += getString(R.string.boring) + ", ";
        if(checkBox3.isChecked())
            answer += getString(R.string.original) + ", ";
        if(checkBox4.isChecked())
            answer += getString(R.string.lively) + ", ";
        if(checkBox5.isChecked())
            answer += getString(R.string.monotonous);

        fileShare.writeToFile(getString(R.string.question_4) + "\n" + answer + "\n\n");
    }

    @Override
    public void onStop() {
        super.onStop();
        Objects.requireNonNull(parent.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    public Bundle getSavedInstanceState() {
        return savedInstanceState;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("nomeMuseo", this.nomeMuseo);
        outState.putString("nomePercorso", this.nomePercorso);
    }

    private void buttonSkipSetOnClickListener() {
        buttonSkip.setOnClickListener(view -> {
            parent.endPath();
            if(checkConnectivity())
                increaseNumeroStarts();
        });
    }
    
    private void buttonVoteSetOnClickListener() {
        buttonVote.setOnClickListener(view -> NetworkConnectivity.check(isConnected -> {
            if (!isConnected) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(R.string.msg_attention);
                builder.setTitle(R.string.attention);
                builder.setIcon(R.drawable.ic_baseline_error_24);

                builder.setCancelable(false);
                builder.setPositiveButton("Ok", (dialogInterface, i) -> parent.endPath());

                builder.setNegativeButton(R.string.try_again, (dialogInterface, i) -> dialogInterface.cancel());

                AlertDialog alertDialog = builder.create();
                alertDialog.show();

            }else {
                if (isQuizComplete()){
                    String result = String.valueOf(ratingBar.getRating());
                    snapshotVoti.addOnSuccessListener(dataSnapshot -> {
                        String voti = dataSnapshot.getValue(String.class);

                        assert voti != null;
                        if (voti.equals("-1"))
                            voti = result;
                        else
                            voti = voti.concat(";" + result);
                        db.child("Voti").setValue(voti).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), R.string.path_end_success, Toast.LENGTH_LONG).show();
                                parent.endPath();
                            } else {
                                Toast.makeText(getContext(), R.string.path_end_fail, Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                    increaseNumeroStarts();
                } else {
                    Toast.makeText(getContext(), requireContext().getString(R.string.quiz_non_completato), Toast.LENGTH_SHORT).show();
                }
            }
        }));
    }

    private void increaseNumeroStarts() {
        snapshotNumStarts.addOnSuccessListener(dataSnapshot -> {
            Integer numStarts = dataSnapshot.getValue(Integer.class);
            numStarts++;
            db.child("Numero_starts").setValue(numStarts);
        });
    }

    public boolean checkConnectivity() {
        if (NetworkConnectivity.check(requireContext())) {
            db = FirebaseDatabase.getInstance().getReference("Museums").child(nomeMuseo).child(nomePercorso);
            snapshotVoti = db.child("Voti").get();
            snapshotNumStarts = db.child("Numero_starts").get();
            return true;
        } else {
            Toast.makeText(getContext(), getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}