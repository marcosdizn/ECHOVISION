package com.example.echovision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;

import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {
    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }

        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int i) {
            }

            @Override
            public void onResults(Bundle bundle) { //El que se lanza cuando acabamos de hablar
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String string = "";
                if (matches != null) {
                    string = matches.get(0);

                    if (string.equalsIgnoreCase("gafas")) {
                        openEspBluetooth(findViewById(android.R.id.content)); // Llama a openActivity2() pasando una vista como parámetro
                    }

                    if (string.equalsIgnoreCase("Texto")) {
                        openActivity2(findViewById(android.R.id.content)); // Llama a openActivity2() pasando una vista como parámetro
                    }
                    if (string.equalsIgnoreCase("en torno") || string.equalsIgnoreCase("Entorno")) {
                        openActivity3(findViewById(android.R.id.content)); // Llama a openActivity2() pasando una vista como parámetro
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        });
    }

    public void StartButton(View view) {
        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);
        speechRecognizer.startListening(intentRecognizer);
    }

    public void openActivity2(View view) {
        startActivity(new Intent(this,Activity2.class));
    }

    public void openActivity3(View view) {
        startActivity(new Intent(this,Activity3.class));
    }

    public void openEspBluetooth(View view) {
        startActivity(new Intent(this,EspBluetooth.class));
    }

}