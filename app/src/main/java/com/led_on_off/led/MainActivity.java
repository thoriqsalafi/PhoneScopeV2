package com.led_on_off.led;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    ImageView imageview5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageview5 = (ImageView)findViewById(R.id.imageView5);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavView_Bar);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(0);
        menuItem.setChecked(true);
        imageview5.setImageResource(R.drawable.instruction);



        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.ic_arrow:

                        break;

                    case R.id.ic_android:
                        Intent intent2 = new Intent(MainActivity.this, actionBar.class);
                        startActivity(intent2);

                        break;

                    case R.id.ic_books:

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
}
