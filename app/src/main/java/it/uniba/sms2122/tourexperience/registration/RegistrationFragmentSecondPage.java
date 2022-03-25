package it.uniba.sms2122.tourexperience.registration;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.material.textfield.TextInputEditText;

import it.uniba.sms2122.tourexperience.R;

public class RegistrationFragmentSecondPage extends Fragment {

    private TextInputEditText name;
    private TextInputEditText surname;
    private TextInputEditText date;
    private ImageView imgDate;
    private Button btnEnd;
    private ProgressBar progressBar;
    private RegistrationActivity mainActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration_second_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        name = view.findViewById(R.id.idEdtRegName);
        surname = view.findViewById(R.id.idEdtRegSurname);
        imgDate = view.findViewById(R.id.idImgSetDate);
        progressBar = view.findViewById(R.id.idProgressBarReg);
        btnEnd = view.findViewById(R.id.idBtnRegSecondPage);
        mainActivity = (RegistrationActivity) getActivity();

        imgDate.setOnClickListener(view1 -> {
            DialogFragment dialogFragment = new DatePickerDialogTheme();
            dialogFragment.show(getChildFragmentManager(), "MyTheme");
        });

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registration(view);
            }
        });
    }

    private void registration(View view) {
        String txtName = name.getText().toString();
        String txtSurname = surname.getText().toString();
        String txtDate = date.getText().toString();

        CheckRegistration checker = mainActivity.getChecker();
        if (!checker.checkGenericStringGeneral("name", name,30,txtName,mainActivity)) return;

        Bundle bundle = getArguments();
        bundle.putString("name", txtName);
        bundle.putString("surname", txtSurname);
        bundle.putString("dateBirth", txtDate);
        progressBar.setVisibility(View.VISIBLE);
        mainActivity.registration(bundle);

    }
}