package com.example.echovision;

import static android.Manifest.permission.RECORD_AUDIO;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EspBluetooth extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;

    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String API_URL = "http://echo-vision.ddns.net:4000/processImageOCRBytes"; //El de google para casa

    Button gafasCasaBtn, gafasEntornoBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.esp_bluetooth);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }

        gafasCasaBtn = findViewById(R.id.gafasCasaBtn);
        gafasEntornoBtn = findViewById(R.id.gafasEntornoBtn);

        //connectToBluetooth();

        gafasCasaBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                String nuevaUrl = "http://echo-vision.ddns.net:4000/processImageOCRBytes"; // OCDR GOOGLE
                setApiUrl(nuevaUrl);
                connectToBluetooth();
            }
        });

        gafasEntornoBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                String nuevaUrl = "http://echo-vision.ddns.net:4000/processImageBytes"; // OCDR GOOGLE
                setApiUrl(nuevaUrl);
                connectToBluetooth();
            }
        });

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

                    if (string.equalsIgnoreCase("Texto")) {
                        String nuevaUrl = "http://echo-vision.ddns.net:4000/processImageOCRBytes"; // OCDR GOOGLE
                        setApiUrl(nuevaUrl);
                        connectToBluetooth();
                    }
                    if (string.equalsIgnoreCase("en torno") || string.equalsIgnoreCase("Entorno")) {
                        String nuevaUrl = "http://echo-vision.ddns.net:4000/processImageBytes"; //LLaVa
                        setApiUrl(nuevaUrl);
                        connectToBluetooth();
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

    private void connectToBluetooth() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice hc05 = btAdapter.getRemoteDevice("10:97:BD:D9:AD:9A");


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("\n\n\nFALTA PERMISO\n\n\n");
            return;
        }
        System.out.println("\n\n\n1:"+btAdapter.getBondedDevices()+ "\n\n\n");
        System.out.println("\n\n\n2:"+hc05.getName()+"\n\n\n");

        BluetoothSocket btSocket = null;
        int counter = 0;
        do {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                  return;
                }
                btSocket = hc05.createRfcommSocketToServiceRecord(mUUID);
                btSocket.connect();
                System.out.println("\n\n\n3:"+btSocket.isConnected()+"\n\n\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            counter++;
        } while (!btSocket.isConnected() && counter < 3);

        /*try {
            OutputStream outputStream = btSocket.getOutputStream();
            outputStream.write(49);
            System.out.println("\n\n\nENVÍO UN 1\n\n\n");

        } catch (IOException e) {
            e.printStackTrace();
        }*/

        InputStream inputStream = null;

        // Declaración de la variable datas
        ArrayDeque<byte[]> datas = new ArrayDeque<>();
        byte[] buf2 = new byte[118400];

        // Creamos un nuevo hilo para leer del InputStream
        BluetoothSocket finalBtSocket = btSocket;
        int bucle = 0;
        while(bucle<2) {
            bucle ++;
            Thread readingThread = new Thread(() -> {

                try {

                    final InputStream finalInputStream = finalBtSocket.getInputStream();

                    byte[] buf = new byte[1024];
                    int bytesRead;

                    // Bucle para leer continuamente del InputStream
                    //int bloque = 0;
                    // Determinar la posición actual en el buffer
                    int currentPosition = 0;
                    //int primero=0;
                    while (true) {
                        try {
                            // Intenta leer del InputStream
                            bytesRead = finalInputStream.read(buf);
                            //System.out.println("\n\nBYTESREAD: "+bytesRead+"\n\n");

                            if (bytesRead > 0) {
                                //primero++;

                                // Se leyeron datos correctamente
                                byte[] receivedBytes = new byte[bytesRead];
                                System.arraycopy(buf, 0, receivedBytes, 0, bytesRead);

                                // Copiar los bytes de receivedBytes al buffer buf2
                                System.arraycopy(receivedBytes, 0, buf2, currentPosition, receivedBytes.length);
                                // Actualizar la posición actual en el buffer
                                currentPosition += receivedBytes.length;

                                // Almacena los bytes recibidos en el ArrayDeque
                                datas.add(receivedBytes);

                                System.out.println("\n\nESPERANDO RECIBIR BYTES. TAMANO: " + bytesRead);
                                System.out.println("\nTAMANO DATAS: " + datas.size() + "\n\n");
                                //System.out.println("\nTHOLAAAA\n\n");

                                /*if ((primero > 1) &&  (bytesRead < 1024)){
                                    // Imprime información sobre los bytes recibidos
                                    // Imprime información sobre los bytes recibidos

                                    System.out.println("\n\nFIN DEL FLUJO\n\n");
                                    break;
                                }*/
                            }
                        } catch (IOException e) {
                            // Error en la lectura, manejar adecuadamente
                            e.printStackTrace();
                            break; // Salir del bucle en caso de error
                        }
                    }
                    //System.out.println("\n\nDATOS TODO JUNTO: " + datas);
                    System.out.println("\n\nFINNNNN\n\n");
               /* int bytesToPrint = 10; // Número de bytes que deseas imprimir
                int startIndex = buf2.length - bytesToPrint;

                System.out.println("\n\nPRIMEROS " + bytesToPrint + " bytes:");

                for (int i = 0; i < bytesToPrint; i++) {
                    System.out.print(String.format("%02X ", buf2[i]));
                }

                System.out.println("\n\nÚltimos " + bytesToPrint + " bytes:");

                for (int i = startIndex; i < buf2.length; i++) {
                    System.out.print(String.format("%02X ", buf2[i]));
                }*/
                    //System.out.println("\n\nTODOS LOS BYTES: "+bytesToHex(buf2)+"\n\n");
                    //System.out.println("\n\nIMAGEN EN HEXADECIMAL ENVIADA\n\n");

                    EspBluetooth.ImageRequestTask imageRequestTask = new EspBluetooth.ImageRequestTask();
                    imageRequestTask.execute(bytesToHex(buf2));
                    //vaciarArreglo(buf2);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            });

// Iniciamos el hilo de lectura
            readingThread.start();

// Esperamos a que el hilo de lectura termine (opcional)
            try {
                readingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /*try {
            btSocket.close();
           // System.out.println("\n\n\n4: SOCKET CONECTADO= "+btSocket.isConnected()+"\n\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }*/

    }

    // Función para imprimir los bytes en formato hexadecimal
   public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public void vaciarArreglo(byte[] arreglo) {
        // Asignar un nuevo arreglo vacío
        arreglo = new byte[0]; // Descomenta esta línea si deseas asignar un arreglo vacío
    }


    public class ImageRequestTask extends AsyncTask<String, Void, String> {

        private static final String TAG = "ImageRequestTask";

        private final OkHttpClient httpClient = new OkHttpClient();

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(String... strings) {
            try {
                if (strings.length > 0) {
                    String imageData = strings[0];

                    // Build JSON with encoded image
                    String json = "{ \"image\": \"" + imageData + "\" }";

                    System.out.println("\n\n\nJSON: " + json+ "\n\n\n");

                    postRequest(API_URL, json);

                    return "";

                } else {
                    Log.e(TAG, "No se proporcionó ningún archivo");
                    return null;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                return null;
            }
        }

        void postRequest(String url, String json) throws IOException {
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), json);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @RequiresApi(api = Build.VERSION_CODES.O)
                public void onResponse(Call call, Response response) throws IOException {
                    String audioBase64 = "";
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);

                    } else {
                        // Parsear la respuesta JSON
                        JSONObject jsonResponse = null;
                        String responseString = response.body().string();
                        try {
                            jsonResponse = new JSONObject(responseString);

                            audioBase64 = jsonResponse.getString("audio");

                            System.out.println("Audio en Base64: " + audioBase64);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        try {
                            // Decode Base64 audio
                            byte[] decodedAudio = java.util.Base64.getDecoder().decode(audioBase64);

                            // Create a temporary file
                            File tempMp3 = File.createTempFile("audio", "mp3", getCacheDir());
                            tempMp3.deleteOnExit();

                            // Write decoded audio to the file
                            FileOutputStream fos = new FileOutputStream(tempMp3);
                            fos.write(decodedAudio);
                            fos.close();

                            // Create a MediaPlayer
                            MediaPlayer mediaPlayer = new MediaPlayer();

                            // Set the data source to the temporary file
                            mediaPlayer.setDataSource(tempMp3.getPath());

                            // Prepare and start the MediaPlayer
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        }

        @Override
        protected void onPostExecute(String response) {
            // Print the response here
            if (response != null) {
                Log.d(TAG, "RESPUESTA: " + response);
                // Aquí puedes hacer cualquier otra cosa con la respuesta, como mostrarla en una vista de texto
            } else {
                Log.e(TAG, "Response is null");
            }
        }

    }
    class ImageDataRequest {
        private String image;
        public ImageDataRequest(String image) {
            this.image = image;
        }
        public String toJson() {
            return "{\"image\":\"" + this.image + "\"}";
        }
    }

    public static void setApiUrl(String apiUrl) {
        API_URL = apiUrl;
    }

    public void StartButton(View view) {
        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);
        speechRecognizer.startListening(intentRecognizer);
    }
}

