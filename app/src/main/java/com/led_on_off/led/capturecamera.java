package com.led_on_off.led;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class capturecamera extends Activity {
    Button button;
    ImageView imageview4;
    static final int CAM_REQUEST = 1;
    private String mImageFileLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capturecamera);

        button = (Button) findViewById(R.id.button2);
        imageview4 = (ImageView)findViewById(R.id.imageView4);
        button.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)
            {
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch(IOException e) {
                    e.printStackTrace();
                }
                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(camera_intent, CAM_REQUEST);

            }
        });
    }


    File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp + "_";
        File StorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
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
            rotateImage(setReducedImageSize());
        }
    }
}
