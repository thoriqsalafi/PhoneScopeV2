package com.led_on_off.led;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;



/**
 * A login screen that offers login via email/password.
 */
public class AreaSelectActivity extends ActionBarActivity{
    //Declaire fields
    private final static String TAG = "Activity::AreaSelect";

    ImageView imageView;
    Uri videoUri;

    private final static int IMG_UPDATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_select);

        //Get URI
        Intent prevIntent = getIntent();
        videoUri = prevIntent.getParcelableExtra("videoPath");

        //Set views
        imageView = (ImageView) findViewById(R.id.selectScreenshot);

        //Start fragment
        getFragmentManager().beginTransaction().replace(R.id.coordFragment,new CoordsFragment()).commit();
    }

    //Handler to update screenshot ImageView
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Update image view with a bmp
            if(msg.what == IMG_UPDATE) {
                //Get bitmap from message
                Bitmap bitmap = (Bitmap) msg.obj;

                //Set bitmap to image view
                imageView.setImageBitmap(bitmap);
                Log.d(TAG,"Updated screenshot ImageView");
            }
        }
    };



    public Context getThisContext(){
        return this;
    }

    public void nextActivity(View view){
        Intent intent = new Intent(this,flowCountActivity.class);
        intent.putExtra("videoPath",videoUri);
        startActivity(intent);
    }
}

