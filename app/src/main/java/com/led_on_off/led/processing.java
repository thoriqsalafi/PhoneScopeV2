package com.led_on_off.led;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;

import org.opencv.android.OpenCVLoader;

public class processing extends ActionBarActivity {

    //Declare fields
    private static int SELECT_IMAGE = 1;
    private static int OPTIMISE_IMAGE = 2;
    private static int FLOW_COUNT = 3;
    private static int VIDEO_TEST = 4;
    private final static String TAG = "Activity::Main";

    //Initiate OpenCV
    static{
        if(!OpenCVLoader.initDebug()) {
            Log.d(TAG,"OpenCV not loaded");
        } else {
            Log.d(TAG,"OpenCV loaded");
        }

        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);
    }

    //Choose video to initiate flow count
    public void flowCount(View view){
        //Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setType("video/*");
        startActivityForResult(intent,FLOW_COUNT);
    }

    //Temporary code to run video test
    public void videoTest(View view){
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setType("video/*");
        startActivityForResult(intent,VIDEO_TEST);
    }

    //Runs after an image is selected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        //If image is selected and everything is ok
         if (requestCode == FLOW_COUNT && resultCode == RESULT_OK){
            //Get image data
            Uri selectedImage = data.getData();

            //Start new activity to process image
            Intent intent = new Intent(this,flowCountActivity.class);
            intent.putExtra("videoPath",selectedImage);
            startActivity(intent);

        }  else if (requestCode == VIDEO_TEST && resultCode == RESULT_OK){
            //Get image data
            Uri selectedImage = data.getData();

            //Start new activity to process image
            Intent intent = new Intent(this,videoActivity.class);
            intent.putExtra("videoPath",selectedImage);
            startActivity(intent);

        }

    }
}
