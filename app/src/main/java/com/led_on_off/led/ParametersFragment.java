package com.led_on_off.led;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by thoriqsalafi on 11/3/17.
 */

public class ParametersFragment extends PreferenceFragment {

    private OnFragmentInteractionListener mListener;

    public ParametersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.params_pref);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
