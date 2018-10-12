package com.led_on_off.led;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;

public class videoBar extends AppCompatActivity {

    private Button mRecordview, mPlayView;
    private VideoView mVideoView;
    private int ACTIVITY_START_CAMERA_APP = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_bar);

        mRecordview = (Button)findViewById(R.id.recordButton);
        mPlayView = (Button)findViewById(R.id.playButton);
        mVideoView = (VideoView)findViewById(R.id.videoView);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavView_Bar);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(2);
        menuItem.setChecked(true);

        mRecordview.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Intent callVideoAppIntent = new Intent();
                callVideoAppIntent.setAction(MediaStore.ACTION_VIDEO_CAPTURE);
                startActivityForResult(callVideoAppIntent, ACTIVITY_START_CAMERA_APP);
            }
        });

        mPlayView.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                mVideoView.start();
            }
        });


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.ic_arrow:
                        Intent intent1 = new Intent(videoBar.this, MainActivity.class);
                        startActivity(intent1);

                        break;

                    case R.id.ic_android:
                        Intent intent2 = new Intent(videoBar.this, actionBar.class);
                        startActivity(intent2);

                        break;

                    case R.id.ic_books:

                        break;

                    case R.id.ic_center_focus:
                        Intent intent4 = new Intent(videoBar.this, videoProcessing.class);
                        startActivity(intent4);

                        break;

                    case R.id.ic_backup:
                        Intent intent5 = new Intent(videoBar.this, aboutInformation.class);
                        startActivity(intent5);

                        break;
                }


                return false;
            }
        });




    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ACTIVITY_START_CAMERA_APP && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
            mVideoView.setVideoURI(videoUri);
            mVideoView.seekTo(100);

        }
    }
}

