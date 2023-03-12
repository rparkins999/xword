/*
 * Copyright © 2023. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 */
package uk.co.yahoo.p1rpp.xword;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import uk.co.yahoo.p1rpp.xword.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

    // Used to load the 'xword' library on application startup.
    static {
        System.loadLibrary("xword");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv =new TextView(this);
        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'xword' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
