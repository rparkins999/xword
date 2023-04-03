/*
 * Copyright © 2023. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 */
package uk.co.yahoo.p1rpp.xword;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.util.AttributeSet;
import android.widget.EditText;

// test string editor
@SuppressLint("AppCompatCustomView")
public class MatchTextEditor extends EditText implements InputFilter {
    final Context mContext;
    SpannableStringBuilder mFilteredChange;
    boolean mInAfterTextChanged = false;
    public boolean mShowingHint = true;
    ColorStateList mTextColours;

    /* This can be called with longer CharSequences than needed.
       In particular if the on-screen keyboard is being used and the insertion
       point is at the end of a word, the destination Spanned is the whole word
       and the source CharSequence is the word with the user's input appended.
       This causes a problem if the user is overtyping the hint because we want
       just the user's input in this case.
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend)
    {
        // Don't override afterTextChanged's actions
        if (mInAfterTextChanged) { return null; }
        mFilteredChange = new SpannableStringBuilder();
        int i = start;
        int j = dstart;
        if (mShowingHint) {
            // remove initial matching characters
            while (   (i < end) && (j < dend)
                   && (source.charAt(i) == dest.charAt(j)))
            { ++i; ++j; }
        }
        // now copy any useful chars from user's input, which may be empty
        while (i < end) {
            char c = source.charAt(i++);
            if (Character.isUpperCase(c)) {
                c = Character.toLowerCase(c);
                mFilteredChange.append(Character.toLowerCase(c));
            } else if ((c == '?') || (c == '/') || (c == ' ')) {
                mFilteredChange.append('?');
            } else if (Character.isLowerCase(c)) {  /* OK as is */
                mFilteredChange.append(c);
            }
            // other characters are ignored
        }
        /* If mFilteredChange is empty and we are showing the hint,
         *      afterTextChanged will reinstate the hint.
         * If mFilteredChange is nonempty and we are showing the hint,
         *      afterTextChanged will replace the hint with mFilteredChange.
         * If we are not showing the hint, normal editing occurs.
         * If the result of normal editing is an empty string,
         *      afterTextChanged will reinstate the hint.
         */
        return mFilteredChange;
    }

    // initialisation now called from MainActivity after creation
    public void createExtras()
    {
        mTextColours = getTextColors();
        mFilteredChange = new SpannableStringBuilder();
        // create an input character filter
        setFilters(new InputFilter[] { this });
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            { /* nothing */ }
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            { /* nothing */ }
            // this gets called after a text change
            @Override
            public void afterTextChanged(Editable s) {
                // prevent recursion
                if (!mInAfterTextChanged) {
                    mInAfterTextChanged = true;
                    int len = mFilteredChange.length();
                    if (mShowingHint) {
                        if (len == 0) {
                            // user didn't type anything useful
                            // but might have deleted some of the hint
                            // so reinstate it
                            s.replace(0, s.length(),
                                mContext.getString(R.string.texttomatch));
                            setTextColor(0xFF808080);
                        } else {
                            // replace the hint by what the user typed
                            mShowingHint = false;
                            s.replace(0, s.length(), mFilteredChange);
                            setSelection(len, len);
                            setTextColor(mTextColours);
                        }
                    } else if (s.length() == 0) {
                        // text has been deleted, show the hint instead
                        s.replace(0, s.length(),
                            mContext.getString(R.string.texttomatch));
                        setTextColor(0xFF808080);
                        mShowingHint = true;
                    }
                    // the action button may need to be updated
                    // so we call the MainActivity to do that
                    if (mContext != null) {
                        ((MainActivity)mContext).setActionButton();
                    }
                    mInAfterTextChanged = false;
                }
            }
        });
        if (mShowingHint) {
            setText(""); // calls afterTextChanged()
        }
        setInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setRawInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setMovementMethod (ArrowKeyMovementMethod.getInstance());
    }

    public MatchTextEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setSingleLine();
    }

    public MatchTextEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setSingleLine();
    }

    public MatchTextEditor(Context context) {
        super(context);
        mContext = context;
        setSingleLine();
    }
}
