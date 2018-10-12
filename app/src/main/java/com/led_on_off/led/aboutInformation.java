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

import java.io.File;
import java.io.IOException;

public class aboutInformation extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_information);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavView_Bar);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(4);
        menuItem.setChecked(true);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.ic_arrow:
                        Intent intent1 = new Intent(aboutInformation.this, MainActivity.class);
                        startActivity(intent1);

                        break;

                    case R.id.ic_android:
                        Intent intent2 = new Intent(aboutInformation.this, actionBar.class);
                        startActivity(intent2);

                        break;

                    case R.id.ic_books:
                        Intent intent3 = new Intent(aboutInformation.this, videoBar.class);
                        startActivity(intent3);

                        break;

                    case R.id.ic_center_focus:
                        Intent intent4 = new Intent(aboutInformation.this, videoProcessing.class);
                        startActivity(intent4);

                        break;

                    case R.id.ic_backup:

                        break;
                }


                return false;
            }
        });

    }
}
