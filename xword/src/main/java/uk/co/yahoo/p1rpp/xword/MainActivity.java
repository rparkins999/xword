/*
 * Copyright © 2023. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is a little dictionary search app. The UI is in Java,
 * but the actual search is done in native code to make it faster.
 *
 * It can find anagrams or words which match a pattern,
 * and incidentally it can tell if a word is in its dictionary.
 *
 * I originally wrote it to help me solve crosswords,
 * but it can also be used to help set crosswords.
 *
 * It has the British and American Scrabble™ dictionaries,
 * so it can also be used to check if a word is allowed in that game.
 */
package uk.co.yahoo.p1rpp.xword;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class MainActivity extends Activity
    implements AdapterView.OnItemSelectedListener,
    View.OnAttachStateChangeListener {
    // Used to load the 'xword' library on application startup.
    static {
        System.loadLibrary("xwordsearch-jni");
    }

    // this declares the native code dictionary search function
    native void search(String s, int i);

    LinearLayout m_container;

    // this spinner allows selection of which distionary to use
    Spinner mDictSelector;
    int mSelectedDictionary;
    static final String SELECTEDDICTIONARY = "SelectedDictionary";
    static final String SHOWING_HINT = "ShowingHint";
    LinearLayout mVersionInfo;
    int mOrientation;

    // button to do actions
    // if we are displaying the list of matches,
    // this button is labelled RESET and goes back to
    // an empty test string
    // if we are displaying a test string containing ?'s
    // this button is labelled MATCH and searches for pattern matches
    // with the usual convention that ? matches any letter
    // if we are displaying a test string containing only letters
    // this button is labelled ANAGRAM and searches for anagrams
    // if we are displaying an mepty test string
    // this button isn't shown
    Button mActionButton;
    boolean mShowingResults;
    static final String ACTION_BUTTON_PRESSED = "ActionButtonPressed";

    // this is the test string editor, subclassed from EditText
    MatchTextEditor mEditor;
    static final String EDITOR_CONTENT = "EditorContent";
    static final String SELECTION_START = "SelectionStart";
    static final String SELECTION_END = "SelectionEnd";

    // this is the adapter for the ListView
    // which displays the list of matches
    ArrayAdapter<String> mAdapter;
    ListView mResults;

    // prevent redundant Enter key actions
    boolean enterKeyDown;

    // this is called when a dictionary is selected from the spinner
    @Override
    public void onItemSelected(
            AdapterView<?> parent, View view, int position, long id) {
        mSelectedDictionary = position;
    }

    // this is called when nothing is selected from the spinner
    // it should never happen, but the interface requires it
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void focusEditor() {
        mResults.setVisibility(View.GONE);
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mVersionInfo.setVisibility(View.GONE);
        }
        mEditor.setVisibility(View.VISIBLE);
        if (mEditor.requestFocus()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mEditor.getWindowInsetsController().show(WindowInsets.Type.ime());
            } else {
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mEditor, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    // callback from JNI code, called for each match found
    // we just append it to the list of matches
    public void AddItem(String s) {
        mAdapter.add(s);
    }

    // this is for a display with rounded corners
    // we don't want a bit of the scrolling ListView to be cut off
    // so if the display has rounded corners
    // we set the padding of the ListView to inset it a bit
    private int getCornerAllowance() {
        // return 0 if this Android version doesn't do rounded corners
        int i = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            WindowInsets insets = m_container.getRootWindowInsets();
            if (insets != null) {
                // the corner radii are normally all the same
                // but we find the largest one just in case
                RoundedCorner rc = insets.getRoundedCorner(
                        RoundedCorner.POSITION_BOTTOM_LEFT);
                if (rc != null) {
                    int j = rc.getRadius();
                    if (j > i) {
                        i = j;
                    }
                }
                rc = insets.getRoundedCorner(
                        RoundedCorner.POSITION_BOTTOM_RIGHT);
                if (rc != null) {
                    int j = rc.getRadius();
                    if (j > i) {
                        i = j;
                    }
                }
                rc = insets.getRoundedCorner(
                        RoundedCorner.POSITION_TOP_LEFT);
                if (rc != null) {
                    int j = rc.getRadius();
                    if (j > i) {
                        i = j;
                    }
                }
                rc = insets.getRoundedCorner(
                        RoundedCorner.POSITION_TOP_RIGHT);
                if (rc != null) {
                    int j = rc.getRadius();
                    if (j > i) {
                        i = j;
                    }
                }
            }
        }
        return i;
    }

    // this is called when the action button is pressed
    // and also during initialisation to set its state
    public void doActionButton(boolean showingResults) {
        if (showingResults) {
            mEditor.setVisibility(View.GONE);
            mActionButton.setText(getString(R.string.reset));
            mAdapter.clear();
            // This is needed if the screen was rotated
            // to prevent Android from setting the focus on the button
            findViewById(R.id.container).requestFocus();
            search(mEditor.getText().toString(), mSelectedDictionary);
            mResults.setVisibility(View.VISIBLE);
        } else {
            mEditor.setText("");
            focusEditor();
            setActionButton();
        }
        mShowingResults = showingResults;
    }

    // this is called to set the label on the action button
    public void setActionButton() {
        // get the test sttring
        String editorContent = mEditor.getText().toString();
        if ((!mEditor.mShowingHint)
                && (editorContent.length() > 0)) {
            if (editorContent.contains("?")) {
                mActionButton.setText(getString(R.string.match));
            } else {
                mActionButton.setText(getString(R.string.anagram));
            }
            mActionButton.setEnabled(true);
            mActionButton.setVisibility(View.VISIBLE);
        } else {
            mActionButton.setEnabled(false);
            mActionButton.setVisibility(View.GONE);
        }
    }

    // called when this class is created
    // if savedInstanceState is not null, we are being restarted
    // either because the screen was rotated
    // or because another app has been running in front of this one
    // and has now exited
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_container = findViewById(R.id.container);
        m_container.addOnAttachStateChangeListener(this);
        mVersionInfo = new LinearLayout(this);
        mVersionInfo.setOrientation(LinearLayout.VERTICAL);
        // display version onformation
        String versionInfo = getString(R.string.app_name) +
                " version " + BuildConfig.VERSION_NAME +
                " built " + getString(R.string.build_time);
        TextView buildStamp = new TextView(this);
        buildStamp.setText(versionInfo);
        mVersionInfo.addView(buildStamp);
        String gitinfo1 = getString(R.string.build_git1);
        if ((gitinfo1 != null) && !gitinfo1.isEmpty()) {
            TextView gitstamp = new TextView(this);
            gitstamp.setText(
                    gitinfo1
                            + "\n" + getString(R.string.build_git2)
                            + "\n" + getString(R.string.build_git3)
            );
            mVersionInfo.addView(gitstamp);
        }
        m_container.addView(mVersionInfo);
        mActionButton = new Button(this);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pressing it flips between the test string input mode
                // and the match list diplay mode
                doActionButton(!mShowingResults);
            }
        });
        ViewGroup.LayoutParams ablp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        // display the dictionary choice spinner
        // default layout direction is horizontal
        LinearLayout ll = new LinearLayout(this);
        // label
        TextView tv = new TextView(this);
        tv.setText(R.string.choosedict);
        ll.addView(tv);
        // the spinner itself
        mDictSelector = new Spinner(this);
        ArrayAdapter<CharSequence> ad = ArrayAdapter.createFromResource(
                this, R.array.dictionaries, R.layout.resultitem);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDictSelector.setAdapter(ad);
        mDictSelector.setFocusable(false);
        ll.addView(mDictSelector);
        mOrientation = getResources().getConfiguration().orientation;
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            ll.addView(mActionButton, ablp);
            m_container.addView(ll);
        } else {
            m_container.addView(ll);
            m_container.addView(mActionButton, ablp);
        }
        // display the action button
        // display the test string editor
        // it will later be made invisible if we are in match list diplay mode
        mEditor = new MatchTextEditor(this);
        if (savedInstanceState == null) {
            // we just atarted the app
            mShowingResults = false; // in test string input mode
            mSelectedDictionary = 0; // initial dictionary is "words"
        } else {
            // recover state from savedInstanceState
            mSelectedDictionary =
                    savedInstanceState.getInt(SELECTEDDICTIONARY, 0);
            String editorContent = savedInstanceState.getString(EDITOR_CONTENT);
            if (editorContent != null) {
                mEditor.setText(editorContent);
                int i = savedInstanceState.getInt(SELECTION_START);
                int j = savedInstanceState.getInt(SELECTION_END);
                mEditor.setSelection(i, j);
            }
            mShowingResults =
                    savedInstanceState.getBoolean(ACTION_BUTTON_PRESSED);
            mEditor.mShowingHint =
                    savedInstanceState.getBoolean(SHOWING_HINT);
        }
        mEditor.createExtras();
        m_container.addView(mEditor);
        // display the match list
        // it will later be made invisible if se are in test string input mode
        mAdapter = new ArrayAdapter<>(this, R.layout.resultitem);
        mResults = new ListView(this);
        mResults.setAdapter(mAdapter);
        mResults.setFocusable(false);
        m_container.addView(mResults);
        mDictSelector.setSelection(mSelectedDictionary);
        mDictSelector.setOnItemSelectedListener(this);
        setActionButton();
        if (mShowingResults) {
            // the match list can be big
            // so we recreate it instead of saving and restoring it
            doActionButton(true);
            mVersionInfo.setVisibility(View.VISIBLE);
        } else {
            focusEditor();
        }
        enterKeyDown = false;
    }

    // handle back button pressed
    // if we are showing the resulst list
    // we go back to an empty test string
    // if we are showing the test string
    // we do the mormal action which is to exit
    @Override
    public void onBackPressed() {
        if (mShowingResults) {
            mShowingResults = false;
            setActionButton();
            focusEditor();
        } else {
            super.onBackPressed();
        }
    }

    // this gets called if the back button is pressed
    // and if any key is pressed on the hardware keyboard
    // and if the done or enter button is pressed on the software keyboard
    // but not if any other key is pressed on the software keyboard
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int c = event.getUnicodeChar();
        int keycode = event.getKeyCode();
        if (keycode == KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event);
        }
        if (keycode == KeyEvent.KEYCODE_ENTER) {
            // we get KEYCODE_ENTER from both hardware and software keyboards
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                // key down, remember we've seen it and wait for key up
                enterKeyDown = true;
                // ignore a redundant key up
            } else if (enterKeyDown
                    && (event.getAction() == KeyEvent.ACTION_UP)) {
                // key up after key doen
                enterKeyDown = false;
                if (mShowingResults) {
                    // go back to text entry
                    doActionButton(false);
                } else {
                    if (mEditor.mShowingHint) {
                        // if no text has been entered, quit
                        finish();
                    } else {
                        // search for match
                        doActionButton(true);
                    }
                }
            }
            return mEditor.dispatchKeyEvent(event);
        } else if (mShowingResults) {
            // if we are displaying the match list
            // and we get a valid character from a hardware keyboard
            // (on-screen keyboard can't be active)
            // go back to the test string input mode
            // and pass the character to it
            if (Character.isLetter(c)
                    || (c == ' ')
                    || (c == '?')
                    || (c == '/')
                    || (keycode == KeyEvent.KEYCODE_DEL)
                    || (keycode == KeyEvent.KEYCODE_FORWARD_DEL)) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    mShowingResults = false;
                    setActionButton();
                    focusEditor();
                }
            }
            // pass it directly to the MatchTextEditor
            // (avoids problem if screen has been rotated)
            return mEditor.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }


    // save state if this class gets put to sleep
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTEDDICTIONARY, mSelectedDictionary);
        outState.putBoolean(SHOWING_HINT, mEditor.mShowingHint);
        outState.putBoolean(ACTION_BUTTON_PRESSED, mShowingResults);
        outState.putString(EDITOR_CONTENT, mEditor.getText().toString());
        outState.putInt(SELECTION_START, mEditor.getSelectionStart());
        outState.putInt(SELECTION_END, mEditor.getSelectionEnd());
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View view) {
        int p = getCornerAllowance() * 3 / 10;
        mResults.setPadding(p,0,p,p);
    }
    @Override
    public void onViewDetachedFromWindow(@NonNull View view) {
    }
}
