/*
 * Copyright © 2023. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 */
package uk.co.yahoo.p1rpp.xword;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class MainActivity extends Activity
    implements AdapterView.OnItemSelectedListener
{

    // Used to load the 'xword' library on application startup.
    static {
        System.loadLibrary("xwordsearch-jni");
    }

    native void search(String s, int i);

    Spinner mDictSelector;
    int mSelectedDictionary;
    static final String SELECTEDDICTIONARY = "SelectedDictionary";
    Button mActionButton;
    boolean mActionButtonPressed;
    static final String ACTION_BUTTON_PRESSED = "ActionButtonPressed";
    MatchTextEditor mEditor;
    static final String EDITOR_CONTENT = "EditorContent";
    static final String SELECTION_START = "SelectionStart";
    static final String SELECTION_END = "SelectionEnd";
    ArrayAdapter<String> mAdapter;
    ListView mResults;

    @Override
    public void onItemSelected(
        AdapterView<?> parent, View view, int position, long id)
    {
        mSelectedDictionary = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // Callback from JNI code, called for each match found
    public void AddItem(String s) {
        mAdapter.add(s);
    }

    // Find out how much space to reserve for rounded corners
    private int getCornerAllowance() {
        int i = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Display disp = getDisplay();
            RoundedCorner rc = disp.getRoundedCorner(
                RoundedCorner.POSITION_BOTTOM_LEFT);
            int j = rc.getRadius();
            if (j > i) { i = j; }
            rc = disp.getRoundedCorner(
                RoundedCorner.POSITION_BOTTOM_RIGHT);
            j = rc.getRadius();
            if (j > i) { i = j; }
            rc = disp.getRoundedCorner(
                RoundedCorner.POSITION_TOP_LEFT);
            j = rc.getRadius();
            if (j > i) { i = j; }
            rc = disp.getRoundedCorner(
                RoundedCorner.POSITION_TOP_RIGHT);
            j = rc.getRadius();
            if (j > i) { i = j; }
        }
        return i;
    }

    public void doActionButton(boolean isNowPressed) {
        if (isNowPressed) {
            mEditor.setVisibility(View.GONE);
            mResults.setVisibility(View.VISIBLE);
            mActionButton.setText(getString(R.string.reset));
            mAdapter.clear();
            search(mEditor.getText().toString(), mSelectedDictionary);
        } else{
            mEditor.setText("");
            mEditor.setVisibility(View.VISIBLE);
            mEditor.requestFocus();
            mResults.setVisibility(View.GONE);
            setActionButton();
        }
        mActionButtonPressed = isNowPressed;
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

    @SuppressLint("SetTextI18n")
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
        LinearLayout ll = new LinearLayout(this);
        TextView tv = new TextView(this);
        tv.setText(R.string.choosedict);
        ll.addView(tv);
        mDictSelector = new Spinner(this);
        ArrayAdapter<CharSequence> ad = ArrayAdapter.createFromResource(
            this, R.array.dictionaries, R.layout.resultitem);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDictSelector.setAdapter(ad);
        ll.addView(mDictSelector);
        container.addView(ll);
        mActionButton = new Button(this);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setVisibility(View.VISIBLE);
                mEditor.requestFocus();
                mResults.setVisibility(View.GONE);
                setActionButton();
                doActionButton(!mActionButtonPressed);
            }
        });
        container.addView(mActionButton, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        mEditor = new MatchTextEditor(this);
        mEditor.setHint(R.string.texttomatch);
        mEditor.setHintTextColor(0x808080);
        container.addView(mEditor);
        mAdapter = new ArrayAdapter<>(this, R.layout.resultitem);
        mResults = new ListView(this);
        int p = getCornerAllowance() * 3 / 10;
        mResults.setPadding(p, 0, p, p);
        mResults.setAdapter(mAdapter);
        container .addView(mResults);
        if (savedInstanceState == null) {
            mActionButtonPressed = false;
            mSelectedDictionary = 0;
        } else {
            mSelectedDictionary =
                savedInstanceState.getInt(SELECTEDDICTIONARY, 0);
            String editorContent = savedInstanceState.getString(EDITOR_CONTENT);
            if (editorContent != null) {
                mEditor.setText(editorContent);
                int i = savedInstanceState.getInt(SELECTION_START);
                int j = savedInstanceState.getInt(SELECTION_END);
                mEditor.setSelection(i, j);
            }
            mActionButtonPressed =
                savedInstanceState.getBoolean(ACTION_BUTTON_PRESSED);
        }
        mDictSelector.setSelection(mSelectedDictionary);
        mDictSelector.setOnItemSelectedListener(this);
        setActionButton();
        if (mActionButtonPressed) {
            doActionButton(mActionButtonPressed);
        } else {
            mResults.setVisibility(View.GONE);
            mEditor.requestFocus();
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
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (   mActionButtonPressed
               && (event.getAction() == KeyEvent.ACTION_DOWN)) {
            int c = event.getUnicodeChar();
            if (Character.isLetter(c)
                || (c == ' ')
                || (c == '?')
                || (c == '/'))
            {
                mEditor.setVisibility(View.VISIBLE);
                mEditor.requestFocus();
                mResults.setVisibility(View.GONE);
                mActionButtonPressed = false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTEDDICTIONARY, mSelectedDictionary);
        outState.putString(EDITOR_CONTENT, mEditor.getText().toString());
        outState.putInt(SELECTION_START, mEditor.getSelectionStart());
        outState.putInt(SELECTION_END, mEditor.getSelectionEnd());
        outState.putBoolean(ACTION_BUTTON_PRESSED, mActionButtonPressed);
    }
}
