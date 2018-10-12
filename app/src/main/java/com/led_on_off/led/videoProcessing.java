package com.led_on_off.led;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;


import org.opencv.android.OpenCVLoader;
import org.w3c.dom.Text;

public class videoProcessing extends AppCompatActivity {

    //Declare fields
    private static int SELECT_IMAGE = 1;
    private static int OPTIMISE_IMAGE = 2;
    private static int FLOW_COUNT = 3;
    private static int VIDEO_TEST = 4;
    private final static String TAG = "Activity::Main";
    private Button mRGBpicker, flowCountbutton;
    private static int RESULT_LOAD_IMAGE = 1;
    private ImageView imageViewRGB;
    private TextView textView, textViewCol;


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
        setContentView(R.layout.activity_video_processing);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavView_Bar);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(3);
        menuItem.setChecked(true);

        mRGBpicker = (Button) findViewById(R.id.RGBanalyzer);
        textView = (TextView) findViewById(R.id.pixel);
        textViewCol = (TextView)findViewById(R.id.pixel2);
        flowCountbutton = (Button) findViewById(R.id.flowcount_button);


        mRGBpicker.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        flowCountbutton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, FLOW_COUNT);
            }
        });



        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.ic_arrow:
                        Intent intent1 = new Intent(videoProcessing.this, MainActivity.class);
                        startActivity(intent1);

                        break;

                    case R.id.ic_android:
                        Intent intent2 = new Intent(videoProcessing.this, actionBar.class);
                        startActivity(intent2);

                        break;

                    case R.id.ic_books:
                        Intent intent3 = new Intent(videoProcessing.this, videoBar.class);
                        startActivity(intent3);

                        break;

                    case R.id.ic_center_focus:

                        break;

                    case R.id.ic_backup:
                        Intent intent5 = new Intent(videoProcessing.this, aboutInformation.class);
                        startActivity(intent5);

                        break;
                }


                return false;
            }
        });

    }

    //Choose video to initiate flowcount
    public void flowCount(View view){
        //Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
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

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            imageViewRGB = (ImageView) findViewById(R.id.imageView5);
            imageViewRGB.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            imageViewRGB.setOnTouchListener(new ImageView.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // TODO Auto-generated method stub
                    int x=0;
                    int y=0;
                    textView.setText("Touch coordinates : " +
                            String.valueOf(Math.round(event.getX())) + "x" + String.valueOf(Math.round(event.getY())));
                    ImageView img = ((ImageView)v);
                    img.setDrawingCacheEnabled(true);
                    Bitmap bitmap = Bitmap.createBitmap(img.getDrawingCache());
                    img.setDrawingCacheEnabled(false);
                    int pixel = bitmap.getPixel(Math.round(event.getX()),Math.round(event.getY()));
                    int redValue = Color.red(pixel);
                    int blueValue = Color.blue(pixel);
                    int greenValue = Color.green(pixel);
                    textViewCol.setText("Red:" + String.valueOf(redValue) + ", Green:" + String.valueOf(greenValue) + ", Blue:" + blueValue);

                    return true;    }
            });

        }
        //If video is selected and everything is ok

        else if (requestCode == FLOW_COUNT && resultCode == RESULT_OK ){
            //Get image data
            Uri selectedImage = data.getData();

            //Start new activity to process image
            //Intent intent = new Intent(this,FlowCountActivity.class);
            Intent intent = new Intent(this,AreaSelectActivity.class);
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
