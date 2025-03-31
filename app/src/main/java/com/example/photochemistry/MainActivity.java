package com.example.photochemistry;


import static android.content.ContentValues.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.content.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_SAMPLERATE = 16000; //must be 16k for the model
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private File outputFile = null;

    private static final int REQUEST_IMAGE_CAPTURE = 1888;
    private static final int REQUEST_IMAGE_IMPORT = 103;
    private ImageView resultImageView;
    private EditText textView;
    private TextView resultView, textView2;
    private ImageButton menuButton;
    private ImageButton importButton;
    private  ImageButton captureButton;
    private ImageButton voiceButton;
    private Button resolveButton;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int MY_GALLERY_PERMISSION_CODE = 200;
    private static final int MY_MICROPHONE_PERMISSION_CODE = 300;
    private static final int RESULT_LOAD_IMAGE = 1;
    private static final int RESULT_RESOLVE = 2;
    private static final String[] notSupported = {"",""};
    private static final String check = "+-=";
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    private Context mycontext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //If dell'OpenCV
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        //create temp file for recording
        try{
            outputFile = File.createTempFile("voice16kmono", ".pcm", getApplicationContext().getFilesDir());
            outputFile.deleteOnExit();
        }catch(IOException e){
            Log.e("TEMPFILE", e.toString());
        }

        this.mycontext = this;

        setContentView(R.layout.activity_main);

        textView = (EditText) findViewById(R.id.reactionText);
        textView2 = (TextView) findViewById(R.id.recordingText);
        resultView = (TextView) findViewById(R.id.resultText);

        captureButton = (ImageButton) findViewById(R.id.captureButton);
        importButton = (ImageButton) findViewById(R.id.importButton);
        voiceButton = (ImageButton) findViewById(R.id.voiceButton);
        menuButton = (ImageButton) findViewById(R.id.menuButton);
        resolveButton = (Button) findViewById(R.id.risolviButton);

        resultImageView = (ImageView) findViewById(R.id.resultImageView);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.CAMERA},MY_CAMERA_PERMISSION_CODE);
                }else{
                    dispatchTakePictureIntent();
                }
            }
        });

        importButton.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, MY_GALLERY_PERMISSION_CODE);
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_GALLERY_PERMISSION_CODE);
                }
                else if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, MY_GALLERY_PERMISSION_CODE);
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_GALLERY_PERMISSION_CODE);

                }else openImageImport();
            }
        });

        voiceButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MY_MICROPHONE_PERMISSION_CODE);
                }
                else if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MY_MICROPHONE_PERMISSION_CODE);
                }
                else toggleRecording();
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openMenu(); }
        });

        resolveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //solve the input reaction
                String str = ((EditText) findViewById(R.id.reactionText)).getText().toString();

                try{
                    Parser myp = new Parser(mycontext);
                    String res="";
                    myp.setTk(str);

                    myp.isEquation();
                    myp.getEquation().divide();
                    myp.getEquation().createMatrix();

                    //res+=myp.getEquation().getMatrix().toString()+"\n";
                    res= SolveReaction.solve(myp.getEquation().getMatrix());
                    res = myp.printResult(res);
                    resultView.setText(res);
                }catch(Exception e){

                    resultView.setText("Equazione non scritta correttamente.");
                }

            }
        });



    }

    // Handle the results from the camera.
    ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                    Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                    try {
                        //uso dell'immagine catturata

                        Segmentation st = new Segmentation(mycontext, imageBitmap);
                        Model mym = new Model(st.segment(), mycontext);
                        textView.setText(mym.getStringPredicted());
                        resultImageView.setImageBitmap(imageBitmap);

                    } catch (IOException e) {
                        resultView.setText("Errore!");
                    }

                }
            }
    );


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);

    }

    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {

                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                    // URI
                    Uri imageUri = result.getData().getData();
                    //uso dell'immagine catturata
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(mycontext.getContentResolver(), imageUri);

                        Segmentation st = new Segmentation(mycontext, bitmap);
                        Model mym = new Model(st.segment(), mycontext);
                        textView.setText(mym.getStringPredicted());
                        resultImageView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        resultView.setText("Errore nell'apertura del file!");
                    }
                }
                else{
                    resultView.setText("errore!");
                }
            }
    );

    private void openImageImport() {
        Intent importIntent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(importIntent);

    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }

    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Toast.makeText(this,"Recording",Toast.LENGTH_SHORT).show(); creates exception

        int minBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, minBufferSize);
        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "audiorecorder thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        int bufferElements2Rec = 1024;
        byte[] bData = new byte[bufferElements2Rec];
        FileOutputStream os = null;

        try{
            os = new FileOutputStream(outputFile);
        }catch(FileNotFoundException e){
            Log.e("ERRSCRITTURA", e.toString());
        }

        while(isRecording){
            // gets the voice output from mic to byte format
            recorder.read(bData, 0, bufferElements2Rec);
            try{
                os.write(bData, 0, bufferElements2Rec);
            }catch(IOException e){
                Log.e("ERRSCRITTURAAUDIO", e.toString());
            }
        }

        try{
            os.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private String getOutputFilePath() {
        //File directory = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AudioRecordings");
        File directory = getCacheDir();
        if(!directory.exists()){
            directory.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "AUDIO_" + timeStamp + ".3gp";
        return directory.getAbsolutePath()+"/"+fileName;
    }

    private void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            //Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();

            List<Float> myAudio = readAudioAndConvertToFloat();

            try{
                AudioModel mymodel = new AudioModel(getApplicationContext());
                String res = mymodel.getTranscription(myAudio);
                textView.setText(res);
            }catch(IOException e){
                throw new RuntimeException(e);
            }

        }
    }

    private List<Float> readAudioAndConvertToFloat(){
        FileInputStream fis = null;

        try{
            fis = new FileInputStream(outputFile);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }

        byte[] data = new byte[2]; //16bit is 1 sample
        List<Float> samples_float = new ArrayList<>();

        try {
            while(fis.read(data) != -1){
                int low = data[0] & 0xFF;
                int high = data[1] << 8;
                short sample = (short) (high | low);

                samples_float.add(sample/32768.0f);
            }
        }catch(IOException e){
            e.printStackTrace();
        }

        return samples_float;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        } else if (requestCode == MY_GALLERY_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImageImport();
            }
        } else if (requestCode == MY_MICROPHONE_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleRecording();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void processAudioFile(File audioFile) {
        // TODO INSERIRE CODICE LAVORAZIONE AUDIO

        textView.setText("Audio analysis complete!");
        if (audioFile.exists()) {
            if (audioFile.delete()) {
                Log.i("File", "File deleted!");
            } else {
                Log.e("File", "File not deleted!");
            }
        }
    }


    private void openMenu() {
        PopupMenu popupMenu = new PopupMenu(this,menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu/*FILE MENU*/,popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == R.id.menu_settings) {
                    //azione per le impostazioni
                    return true;
                }else if(item.getItemId() == R.id.menu_guide){
                    //azione per la Guida
                    return true;
                }

                else if (item.getItemId() == R.id.menu_WAW) {
                    // azione per il "Chi siamo"
                    return true;

                }else return false;

            }
        });
        popupMenu.show();
    }

    private void resolveReaction(){
        String reaction = textView.getText().toString();
        if(MainActivity.notCorrect(reaction)) {
            resultView.setText("Wrong syntax!");
        }
        else{
            String result = "WIP";
            resultView.setText("Balanced reaction: "+result);
        }
    }



    private static Boolean notCorrect (String reaction){
        boolean res = false;
        if (!reaction.contains("=")){
            res = true;
        }

        return res;
    }

    public static String assetFilePath(Context context, String assetName){
        File file = new File(context.getFilesDir(), assetName);
        if(file.exists() && file.length() > 0)
            return file.getAbsolutePath();

        try (InputStream is = context.getAssets().open(assetName)){
            try (OutputStream os = new FileOutputStream(file)){
                byte[] buffer = new byte[4 * 1024];
                int read;
                while((read=is.read(buffer)) != -1)
                    os.write(buffer, 0, read);
                os.flush();

            } return file.getAbsolutePath();

        }catch(IOException e){
            Log.e("errorFileAudioModel", "impossibile processare il file");
        }
        return null;
    }


}




