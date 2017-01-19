package com.led_on_off.led;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;

public class videoActivity extends ActionBarActivity {
    //Declare fields
    private final static String TAG = "Activity::VideoActivity";
    private File imgFile = new File(Environment.getExternalStorageDirectory(), "CellCount");
    private String imgPath = imgFile.getPath();

    ImageView imageView;

    private static int PROCESS_INCOMPLETE = 0;
    private static int IMG_UPDATE = 1;
    private static int TEXT_UPDATE = 2;
    private static int COMBINE_FRAME = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        //Get image URI from previous intent
        Intent prevIntent = getIntent();
        Uri videoUri = prevIntent.getParcelableExtra("videoPath");

        //Set ImageView and TextView
        imageView = (ImageView) findViewById(R.id.videoImage);

        decodeVideo(videoUri);
    }

    //Create handler to update imageView and textView
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //If message status is complete, run the updateImage method
            if(msg.what == COMBINE_FRAME) {
                if(msg.obj instanceof Mat){
                    //Get matrix from message
                    Mat img = (Mat) msg.obj;

                    //Convert mat into bitmap
                    Bitmap bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(img, bitmap);

                    //Set bitmap to image view
                    imageView.setImageBitmap(bitmap);

                    //Add entry into log
                    Log.i(TAG, "Updated image view with matrix");
                } else if (msg.obj instanceof Bitmap){
                    //Get bitmap from message
                    Bitmap bitmap = (Bitmap) msg.obj;

                    //Set bitmap to image view
                    imageView.setImageBitmap(bitmap);

                    //Add entry into log
                    Log.i(TAG, "Updated image view with bitmap");
                }
            }
        }
    };

    //Function to decode video file
    public void decodeVideo(final Uri videoUri){
        Runnable decodeRunnable = new Runnable() {
            @Override
            public void run() {
                //Create DecodeVideo and its wrapper
                DecodeVideo decodeVideo = new DecodeVideo(imgFile,videoUri,getBaseContext(),handler);
                decodeVideo.callWrapper();
            }
        };

        Thread thread = new Thread(decodeRunnable);
        thread.start();
    }
}
