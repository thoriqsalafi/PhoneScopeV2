package com.led_on_off.led;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class actionBar extends ActionBarActivity {
    Button button;
    ImageView imageview4;
    static final int CAM_REQUEST = 0;
    private String mImageFileLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_bar);

        button = (Button) findViewById(R.id.button3);
        imageview4 = (ImageView)findViewById(R.id.imageView4);
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavView_Bar);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);

        button.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)
            {
                Intent callCameraApplicationIntent = new Intent();
                callCameraApplicationIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch(IOException e) {
                    e.printStackTrace();
                }
                callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(callCameraApplicationIntent, CAM_REQUEST);

            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.ic_arrow:

                        break;

                    case R.id.ic_android:
                        break;

                    case R.id.ic_books:
                        Intent intent3 = new Intent(actionBar.this, videoBar.class);
                        startActivity(intent3);

                        break;

                    case R.id.ic_center_focus:

                        break;

                    case R.id.ic_backup:

                        break;
                }


                return false;
            }
        });

    }


    File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp + "_";
        File StorageDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/Camera/");
        if (!StorageDirectory.exists())
            StorageDirectory.mkdirs();
        File image = File.createTempFile(imageFileName,".jpg", StorageDirectory);
        mImageFileLocation = image.getAbsolutePath();

        return image;
    }

    private Bitmap setReducedImageSize() {
        int targetImageViewWidth = imageview4.getWidth();
        int targetImageViewHeight = imageview4.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mImageFileLocation,bmOptions);
        int cameraImageWidth = bmOptions.outWidth;
        int cameraImageHeight = bmOptions.outHeight;

        int scaleFactor = Math.min(cameraImageWidth/targetImageViewWidth, cameraImageHeight/targetImageViewHeight);
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inJustDecodeBounds = false;

        //Bitmap photoReducedSizeBitmap = BitmapFactory.decodeFile(mImageFileLocation,bmOptions);
        //imageview4.setImageBitmap(photoReducedSizeBitmap);
        return BitmapFactory.decodeFile(mImageFileLocation,bmOptions);


    }

    private void rotateImage(Bitmap bitmap){
        ExifInterface exifInterface =null;
        try{
            exifInterface = new ExifInterface(mImageFileLocation);
        } catch(IOException e) {
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            default:
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),bitmap.getHeight(),matrix,true );
        imageview4.setImageBitmap(rotatedBitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Bitmap photoCapturedBitmap = BitmapFactory.decodeFile(mImageFileLocation);
        //imageview4.setImageBitmap(photoCapturedBitmap);
        if(requestCode == CAM_REQUEST && resultCode == RESULT_OK) {
            addPicToGallery(this,mImageFileLocation);
            rotateImage(setReducedImageSize());
        }
    }

    public static void addPicToGallery(Context context, String photoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(photoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }
}
