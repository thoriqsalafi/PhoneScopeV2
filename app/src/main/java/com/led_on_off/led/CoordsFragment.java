package com.led_on_off.led;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Created by thoriqsalafi on 11/3/17.
 */

public class CoordsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Declare fields
    MediaMetadataRetriever dataRetriever;
    Uri videoUri;
    Context context;
    Handler handler;
    Message message;
    SharedPreferences sharedPref;
    Bitmap screenshot;
    Mat img;

    private final static String TAG = "CoordsFragment";
    private final static int IMG_UPDATE = 1;

    public CoordsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Add preferences from xml file
        addPreferencesFromResource(R.xml.coords_pref);

        //Get Uri and context
        videoUri = ((AreaSelectActivity)getActivity()).videoUri;
        context = ((AreaSelectActivity)getActivity()).getThisContext();

        //Get handler from base activity
        handler = ((AreaSelectActivity)getActivity()).handler;

        //Create and set MediaMetadataRetriever
        dataRetriever = new MediaMetadataRetriever();
        dataRetriever.setDataSource(context,videoUri);
        Log.d(TAG,"Updated MediaMetadataRetriever video source");

        //Get shared preferences
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        //Update screenshot in main UI
        updateImg();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //Update the image upon change
        updateImg();
    }

    public void updateImg(){
        //Get new screenshot from dataRetriever
        screenshot = dataRetriever.getFrameAtTime(0);   //0 seconds

        //Convert from bitmap to mat
        img = new Mat();
        Utils.bitmapToMat(screenshot,img);

        //Get coordinates
        int x1 = Integer.parseInt(sharedPref.getString("s_x1","498"));
        int x2 = Integer.parseInt(sharedPref.getString("s_x2","540"));
        int y = Integer.parseInt(sharedPref.getString("s_y","339"));
        Point rect1 = new Point(x1,y);
        Point rect2 = new Point(x2,y);

        //Draw rectangle on image
        Imgproc.rectangle(img,rect1,rect2,new Scalar(0, 0, 255),5); //Blue, thickness = 5

        //Convert back to bmp
        Utils.matToBitmap(img,screenshot);

        //Send message to handler
        message = handler.obtainMessage(IMG_UPDATE,screenshot);
        message.sendToTarget();
    }


}

