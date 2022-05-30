package it.uniba.sms2122.tourexperience;

import static it.uniba.sms2122.tourexperience.utility.filesystem.LocalFileManager.createLocalDirectoryIfNotExists;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import it.uniba.sms2122.tourexperience.holders.UserHolder;
import it.uniba.sms2122.tourexperience.main.MainActivity;
import it.uniba.sms2122.tourexperience.utility.connection.NetworkConnectivity;
import it.uniba.sms2122.tourexperience.welcome.WelcomeActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private UserHolder userHolder;
    private ActivityOptions options;
    private final String mainDirectory = "Museums";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        createLocalDirectoryIfNotExists(getFilesDir(), mainDirectory);
        options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, R.anim.slide_out_left);
        checkConnectivity();
    }

    public void checkConnectivity() {
        if (!NetworkConnectivity.check(getApplicationContext())) {
            Toast.makeText(SplashScreenActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, FirstActivity.class));
            finish();
            return;
        }
        userHolder = UserHolder.getInstance();
        userHolder.getUser(
            //Caso: Utente è loggato
            (user) -> {
                LoginActivity.addNewSessionUid(getApplicationContext());
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish(); // Non si può tornare indietro con il pulsane Back
            },
            //Caso: Utente non è loggato
            (String errorMsg) -> {
                // Verifica se si tratta della prima apertura o no
                SharedPreferences prefs = getSharedPreferences(BuildConfig.SHARED_PREFS, MODE_PRIVATE);
                if(!prefs.contains(BuildConfig.SP_FIRST_OPENING)) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(BuildConfig.SP_FIRST_OPENING, true);
                    editor.apply();
                }

                if(prefs.getBoolean(BuildConfig.SP_FIRST_OPENING, true)) {
                    startActivity(new Intent(this, WelcomeActivity.class));
                } else {
                    startActivity(new Intent(this, FirstActivity.class));
                }
                finish();
            }
        );
    }
}