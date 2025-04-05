package com.example.echovision;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Activity3 extends AppCompatActivity {
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;

    private static final String API_URL = "http://echo-vision.ddns.net:4000/processImage"; //El de Llava para entorno

    ImageView selectedImage;
    Button cameraBtn,galleryBtn;
    String currentPhotoPath;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_3);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color));
        }

        selectedImage = findViewById(R.id.displayImageView);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);

        cameraBtn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                askCameraPermissions();
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });

    }

    private void askCameraPermissions() { //Para ver si ya se le ha dado permiso a la app para usar la cámara o no
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);//Pedimos permiso
        }else {
            dispatchTakePictureIntent();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Se requiere permiso de cámara para usar la cámara.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File f = new File(currentPhotoPath);//Tenemos la imagen guardada aquí
                selectedImage.setImageURI(Uri.fromFile(f));
                Log.d("tag", "ABsolute Url of Image is " + Uri.fromFile(f));

                ImageRequestTask imageRequestTask = new ImageRequestTask();
                imageRequestTask.execute(f);

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);


            }
        }

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);//Creamos el nombre del archivo
                Log.d("tag", "onActivityResult: Gallery Image Uri:  " + imageFileName);

                // Aquí obtenemos el file de la imagen
                File f = new File(getRealPathFromURI(contentUri)); // Llamamos a una función para obtener la ruta real del archivo

                ImageRequestTask imageRequestTask = new ImageRequestTask();
                imageRequestTask.execute(f);

                selectedImage.setImageURI(contentUri);
            }
        }
    }

    private void deleteImageFile(File photoFile) {
        if (photoFile != null && photoFile.exists()) { // Comprueba si el archivo existe
            if (photoFile.delete()) { // Intenta eliminar el archivo
                Log.d("ImageDeleted", "Image deleted successfully");
            } else {
                Log.d("ImageDeleted", "Failed to delete image");
            }
        }
    }

    public String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String encodeImageToBase64(File imageFile) {
        try (FileInputStream imageInFile = new FileInputStream(imageFile)) {
            // Lee la imagen en un array de bytes
            byte[] imageData = new byte[(int) imageFile.length()];
            imageInFile.read(imageData);
            // Codifica la imagen a Base64 y retorna la cadena resultante
            return Base64.getEncoder().encodeToString(imageData);
        } catch (IOException e) {
            Log.e("tag", "Error al leer la imagen: " + e.getMessage());
            return null;
        }
    }

    public class ImageRequestTask extends AsyncTask<File, Void, String> {

        private static final String TAG = "ImageRequestTask";

        private final OkHttpClient httpClient = new OkHttpClient();

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(File... files) {
            try {
                if (files.length > 0) {
                    File imageFile = files[0];

                    String encodedImage = encodeImageToBase64(imageFile);

                    // Build JSON with encoded image
                    String json = "{ \"image\": \"" + encodedImage + "\" }";
                    postRequest(API_URL, json);

                    // Eliminar la imagen después de procesarla
                    deleteImageFile(imageFile);

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

                        // Obtener el valor del campo "audio"
                        audioBase64 = jsonResponse.getString("audio");

                        // Imprimir el audio en formato Base64
                        System.out.println("Audio en Base64: " + audioBase64);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        try {
                            // Decode Base64 audio
                            byte[] decodedAudio = Base64.getDecoder().decode(audioBase64);

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


                        /*

                        // Decodificar la cadena base64 en un array de bytes
                        byte[] decodedBytes = Base64.getDecoder().decode(responseString);

                        // Guardar los bytes decodificados en un archivo de audio
                        try (FileOutputStream fos = new FileOutputStream("audio.wav")) {
                            fos.write(decodedBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Inicializar MediaPlayer
                        MediaPlayer mediaPlayer = new MediaPlayer();

                        // Reproducir el archivo de audio
                        String audioPath = "audio.wav";

                        try {
                            // Establecer el archivo de audio a reproducir
                            mediaPlayer.setDataSource(audioPath);

                            // Preparar el MediaPlayer
                            mediaPlayer.prepare();

                            // Reproducir el audio
                            mediaPlayer.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
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


        private String sendGetRequest(String urlStr) throws Exception {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        }


        private String sendPostRequest(String urlStr, ImageDataRequest data) throws Exception {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);

           conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String jsonInputString = data.toJson();

           try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
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


    private class PostRequestSender extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            String data = params[1];

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    wr.writeBytes(data);
                    wr.flush();
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Manejar el resultado de la solicitud aquí
            Log.d("SendImageTask", "Result: " + result);
        }
    }

    private class SendImageTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String json = strings[0];

            try {
                // Configurar y enviar la solicitud POST
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                //RequestBody body = RequestBody.create(json, JSON);
                RequestBody body = RequestBody.create(JSON, json);
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();

                // Manejar la respuesta
                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    return "Error al enviar la imagen: " + response.code() + " " + response.message();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Manejar el resultado de la solicitud aquí
            Log.d("SendImageTask", "Result: " + result);
        }
    }

    /*private void uploadImageToFirebase(String name, Uri contentUri) {
        final StorageReference image = storageReference.child("pictures/" + name);
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());
                    }
                });

                Toast.makeText(MainActivity.this, "Image Is Uploaded.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Upload Failled.", Toast.LENGTH_SHORT).show();
            }
        });

    }*/



    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();//Aquí tenemos guardada la imagen
            } catch (IOException ex) {

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "net.smallacademy.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE, null);
            }
        }
    }
}

