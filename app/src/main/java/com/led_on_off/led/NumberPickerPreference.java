package com.led_on_off.led;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;



public class NumberPickerPreference extends DialogPreference {

    //Initialise fields
    private int mValue; //Value of the current preference
    private NumberPicker numPicker; //NumberPicker widget
    private int MIN = 0;    //Minimum number for widget
    private int MAX = 255;   //Maximum number for widget
    private int DEFAULT_VALUE = 33;  //Default value for params_pref (to be replaced)

    //Constructor for NumberPickerPreference
    public NumberPickerPreference (Context context, AttributeSet attribute) {
        //Call super for its usual functionality
        super(context,attribute);

        //Setup dialog box
        setDialogLayoutResource(R.layout.activity_numpicker); //Use the layout_numpicker.xml
        setPositiveButtonText(R.string.ok); //Set button text to OK from string
        setNegativeButtonText(R.string.cancel); //Set button text to Cancel from string
        setDialogIcon(null);    //No dialog icon

        /*
        //Get array of attributes
        TypedArray attrArray = context.obtainStyledAttributes(attribute,R.styleable.NumPickerPref, 0, 0);
        //Get constants from attributes
        MIN = attrArray.getInt(R.styleable.NumPickerPref_prefMin, 0);   //Get minimum, default to 0
        MAX = attrArray.getInt(R.styleable.NumPickerPref_prefMax, 255); //Get maximum, default to 255
        DEFAULT_VALUE = attrArray.getInt(android.R.attr.defaultValue, 1); //
        attrArray.recycle();
        */
    }

    //Function to save the value
    public void setValue (int value) {
        //Update variable with new value
        mValue = value;

        //Save as persistent into SharedPreferences
        persistInt(mValue);
    }

    //Get DefaultValue as integer
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        //Get integer from array, use DEFAULT_VALUE if failed
        return a.getInt(index,DEFAULT_VALUE);
    }

    //Set initial value of the variable
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        //If there is a persistent value
        if (restorePersistedValue) {
            //Restore persistent value to mValue, using DEFAULT_VALUE if failed
            mValue = getPersistedInt(DEFAULT_VALUE);
        } else {
            //If not, set it using the default value
            setValue ((Integer) defaultValue);
        }
    }

    //Called upon linking to the widget
    @Override
    protected void onBindDialogView(View view) {
        //Call super for usual functionality
        super.onBindDialogView(view);

        //Setup the number picker widget
        numPicker = (NumberPicker) view.findViewById(R.id.numPicker);
        numPicker.setMinValue(MIN); //Set minimum value
        numPicker.setMaxValue(MAX); //Set maximum value
        numPicker.setWrapSelectorWheel(false);

        //Set number picker default to saved pref
        numPicker.setValue(mValue);
    }

    //Set the value from dialog after closing it
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        //If the user selected the "Ok" button
        if (positiveResult) {
            setValue(numPicker.getValue());
            //callChangeListener(mValue);
        }
    }
}

