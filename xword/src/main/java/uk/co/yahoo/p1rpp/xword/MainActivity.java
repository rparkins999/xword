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
import android.widget.ArrayAdapter;
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
    ArrayAdapter<String> mAdapter;
    ListView mResults;
    Button mActionButton;
    Boolean mActionButtonPressed;
    static final String ACTION_BUTTON_PRESSED = "ActionButtonPressed";

    public void doActionButton() {
        if (mActionButtonPressed) {
            mEditor.setText("");
            mEditor.setVisibility(View.VISIBLE);
            mAdapter.clear();
            mResults.setVisibility(View.GONE);
            setActionButton();
        } else {
            mEditor.setVisibility(View.GONE);
            mResults.setVisibility(View.VISIBLE);
            mActionButton.setText(getString(R.string.reset));
            // do the search here
        }
        mActionButtonPressed = !mActionButtonPressed;
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
        String versionInfo = getString(R.string.app_name) +
            " version " + BuildConfig.VERSION_NAME +
            " built " + getString(R.string.build_time);
        TextView buildStamp = new TextView(this);
        buildStamp.setText(versionInfo);
        container.addView(buildStamp);
        String gitinfo1 = getString(R.string.build_git1);
        if ((gitinfo1 != null) && !gitinfo1.isEmpty()) {
            TextView gitstamp = new TextView(this);
            gitstamp.setText(
                gitinfo1
                + "\n" + getString(R.string.build_git2)
                + "\n" + getString(R.string.build_git3)
            );
            container.addView(gitstamp);
        }
        mEditor = new MatchTextEditor(this);
        mEditor.setHint(R.string.texttomatch);
        container.addView(mEditor);
        mAdapter = new ArrayAdapter<String>(this, R.layout.resultitem);
        mResults = new ListView(this);
        mResults.setAdapter(mAdapter);
        container .addView(mResults);
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
        } else {
            mResults.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mActionButtonPressed) {
            mEditor.setVisibility(View.VISIBLE);
            mResults.setVisibility(View.GONE);
            mActionButtonPressed = false;
            setActionButton();
        } else {
            super.onBackPressed();
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
