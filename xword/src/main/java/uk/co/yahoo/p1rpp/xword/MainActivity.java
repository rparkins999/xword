/*
 * Copyright © 2023. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 */
package uk.co.yahoo.p1rpp.xword;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import uk.co.yahoo.p1rpp.xword.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

    // Used to load the 'xword' library on application startup.
    static {
        System.loadLibrary("xword");
    }

    MatchTextEditor mEditor;
    static final String EDITOR_CONTENT = "EditorContent";
    Button mActionButton;
    Boolean mActionButtonPressed;
    static final String ACTION_BUTTON_PRESSED = "ActionButtonPressed";

    public void doActionButton() {
    }

    public void setActionButton() {
        String editorContent = mEditor.getText().toString();
        if (editorContent.length() > 0)
        {
            if (editorContent.contains("?"))
            {
                mActionButton.setText(getString(R.string.match));
            }
            else
            {
                mActionButton.setText(getString(R.string.anagram));
            }
            mActionButton.setEnabled(true);
            mActionButton.setVisibility(View.VISIBLE);
        }
        else
        {
            mActionButton.setEnabled(false);
            mActionButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout container = findViewById(R.id.container);
        TextView buildStamp = new TextView(this);
        String versionInfo = getString(R.string.app_name) +
            " version " +
            BuildConfig.VERSION_NAME +
            " built " +
            getString(R.string.build_time);
        buildStamp.setText(versionInfo);
        container.addView(buildStamp);
        mEditor = new MatchTextEditor(this);
        mEditor.setHint(R.string.texttomatch);
        container.addView(mEditor);
        mActionButton = new Button(this);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doActionButton();
            }
        });
        container.addView(mActionButton, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        ListView results = new ListView(this);
        container.addView(results);
        if (savedInstanceState == null) {
            mActionButtonPressed = false;
        } else {
            String editorContent = savedInstanceState.getString(EDITOR_CONTENT);
            if (editorContent != null) {
                mEditor.setText(editorContent);
            }
            mActionButtonPressed =
                savedInstanceState.getBoolean(ACTION_BUTTON_PRESSED);
        }
        setActionButton();
        if (mActionButtonPressed) {
            doActionButton();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EDITOR_CONTENT, mEditor.getText().toString());
        outState.putBoolean(ACTION_BUTTON_PRESSED, mActionButtonPressed);
    }

    /**
     * A native method that is implemented by the 'xword' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
