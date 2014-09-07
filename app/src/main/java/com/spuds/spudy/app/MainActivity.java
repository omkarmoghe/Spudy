package com.spuds.spudy.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends Activity {
    Bitmap bm;
    String finalText = "";

    private String apiKey = "d7wSyPRbD8";
    private String langCode = "en";

    static final int REQUEST_IMAGE_CAPTURE = 1;
    String mCurrentPhotoPath = "";

    public boolean playing = false;
    ImageView playpauseButton;

    TextView wpmDisplay;
    SeekBar wpmSlider;

    ArrayList<String> finalReadingList;
    TextView mainDisplay;

    Thread thread;
    public boolean running = false;
    private long SLEEP_TIME;
    public boolean firstRun = true;

    Firebase myFirebaseRef = new Firebase("https://spudy.firebaseio.com/");

    public Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().hide();
        setContentView(R.layout.activity_main);

        playpauseButton = (ImageView) findViewById(R.id.playpauseButton);
        wpmSlider = (SeekBar) findViewById(R.id.wpmSlider);
        wpmDisplay = (TextView) findViewById(R.id.wpmDisplay);
        mainDisplay = (TextView) findViewById(R.id.mainDisplay);

        startCameraIntent();

        handler = new Handler();
    }

    private Runnable timerTask = new Runnable() {
        public void run() {
            if (running && finalReadingList.size() != 0) {
                setMainDisplay(finalReadingList.get(0));
                finalReadingList.remove(0);
            } else if (finalReadingList.size() == 0) {
                firstRun = true;
            }
            //run again with delay
            handler.postDelayed(timerTask, SLEEP_TIME);
        }
    };

    public void sendPost() {
        getBitmap();

        HttpPost httppost = new HttpPost("http://api.ocrapiservice.com/1.0/rest/ocr");
        MultipartEntityBuilder entity = MultipartEntityBuilder.create();
        entity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        HttpClient httpClient = new DefaultHttpClient();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 60, bos);

        byte[] data = bos.toByteArray();
        ByteArrayBody bab = new ByteArrayBody(data, mCurrentPhotoPath);
        try {
            entity.addPart("image", bab);
            entity.addPart("language", new StringBody(langCode, ContentType.TEXT_PLAIN));
            entity.addPart("apikey", new StringBody(apiKey, ContentType.TEXT_PLAIN));
        } catch (Exception e) {
            e.printStackTrace();
        }

        httppost.setEntity(entity.build());

        enableStrictMode();
        try {
            HttpResponse response = httpClient.execute(httppost);
            HttpEntity entity1 = response.getEntity();
            finalText = EntityUtils.toString(entity1);

            //System.out.println(finalText);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO make more conditions for throwing an alert at the user.
        if(finalText.length() < 3){
            makeAlert();
        } else {
            initializeList();
            pushString(finalText);
            Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void enableStrictMode()
    {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
    }

    public void startCameraIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            sendPost();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    public void getBitmap(){
        Toast.makeText(this, "Spuding...", Toast.LENGTH_SHORT).show();

        try {
            bm = BitmapFactory.decodeStream((InputStream)new URL(mCurrentPhotoPath).getContent());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Scaling :(
        int width = Math.round((float).7 * bm.getWidth());
        int height = Math.round((float).7 * bm.getHeight());

        bm = Bitmap.createScaledBitmap(bm, width,
                height, true);

        /*
        ImageView img = (ImageView)findViewById(R.id.bitmapTest);
        img.setImageBitmap(bm);
        */
    }

    public void playpause(View v){
        if(playing)
            pause();
        else
            play();
    }

    public void play(){
        if(firstRun){
            handler.post(timerTask);
            firstRun = false;
        }
        playpauseButton.setImageResource(R.drawable.pause);
        playing = true;
        running = true;
    }

    public void pause(){
        playpauseButton.setImageResource(R.drawable.play);
        playing = false;
        running = false;
    }

    public void setDefault(View v){
    }

    public void initializeList(){
        StringMaster sm = new StringMaster(finalText);
        sm.parseString();
        finalReadingList = sm.getList();
        SLEEP_TIME = (long)getSleepTime();
    }

    public void setMainDisplay(String word){
        mainDisplay.setText(word);
    }

    public double getSleepTime(){
        int wpm;
        if(wpmSlider.getProgress() != 0) {
            wpm = wpmSlider.getProgress();
        } else {
            wpm = 10;
        }
        System.out.println(wpm);
        return (60000.0 / wpm);
    }

    public void makeAlert(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Uh-oh!");
        alertDialog.setMessage("There doesn't seem to be anything here. Please try again.");
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startCameraIntent();
            }
        });
        alertDialog.show();
    }

    public void pushString(String stringToPush) {
        myFirebaseRef.child("message").setValue(stringToPush);
    }

    //TODO get the snapshot object out and do some useful shit with it.
    public void pullString(String pullReq){
        myFirebaseRef.child(pullReq).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                snapshot.getValue();
            }

            @Override
            public void onCancelled(FirebaseError error) {
            }
        });
    }
}