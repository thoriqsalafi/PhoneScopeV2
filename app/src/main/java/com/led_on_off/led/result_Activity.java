package com.led_on_off.led;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.util.ArrayList;


public class result_Activity extends Activity {

        private CustomSeekBar seekbar;

        private float totalSpan = 1500;
        private float redSpan = 200;
        private float blueSpan = 300;
        private float greenSpan = 400;
        private float yellowSpan = 300;
        private float darkGreySpan;

        private ArrayList<ProgressItem> progressItemList;
        private ProgressItem mProgressItem;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_result_);
            seekbar = ((CustomSeekBar) findViewById(R.id.seekBar0));
            initDataToSeekbar();

        }

        private void initDataToSeekbar() {
            progressItemList = new ArrayList<ProgressItem>();
            // red span
            mProgressItem = new ProgressItem();
            mProgressItem.progressItemPercentage = ((redSpan / totalSpan) * 100);
            Log.i("result activity", mProgressItem.progressItemPercentage + "");
            mProgressItem.color = R.color.red;
            progressItemList.add(mProgressItem);
            // blue span
            mProgressItem = new ProgressItem();
            mProgressItem.progressItemPercentage = (blueSpan / totalSpan) * 100;
            mProgressItem.color = R.color.yellow;
            progressItemList.add(mProgressItem);
            // green span
            mProgressItem = new ProgressItem();
            mProgressItem.progressItemPercentage = (greenSpan / totalSpan) * 100;
            mProgressItem.color = R.color.green;
            progressItemList.add(mProgressItem);
            // yellow span
            mProgressItem = new ProgressItem();
            mProgressItem.progressItemPercentage = (yellowSpan / totalSpan) * 100;
            mProgressItem.color = R.color.yellow;
            progressItemList.add(mProgressItem);
            // greyspan
            mProgressItem = new ProgressItem();
            mProgressItem.progressItemPercentage = (darkGreySpan / totalSpan) * 100;
            mProgressItem.color = R.color.red;
            progressItemList.add(mProgressItem);

            seekbar.initData(progressItemList);
            seekbar.invalidate();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }

    }

