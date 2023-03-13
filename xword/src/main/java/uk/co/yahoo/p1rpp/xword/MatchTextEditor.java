/*
 * Copyright © 2023. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 */
package uk.co.yahoo.p1rpp.xword;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ArrowKeyMovementMethod;
import android.util.AttributeSet;
import android.widget.EditText;

@SuppressLint("AppCompatCustomView")
public class MatchTextEditor extends EditText {
    Context mContext;

    private class MatchFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(source);
            for (int i = start; i < end; i++) {
                char c = ssb.charAt(i);
                if (Character.isLowerCase(c)) {
                    // OK as is
                }
                else if (Character.isUpperCase(c)) {
                    StringBuilder s = new StringBuilder(Character.toLowerCase(c));
                    ssb.replace(i, i + 1, s);
                }
                else if ((c == '?') || (c == '/') || (c == ' ')) {
                    ssb.replace(i, i + 1, "?");
                }
                else if (c == '\n')
                {
                    ssb.replace(i, end, "");
                    hitReturn();
                }
                else
                {
                    // not wanted, remove it
                    ssb.replace(i, i + 1, "");
                }
            }
            return ssb;
        }
    }

    private void createExtras()
    {
        setFilters(new InputFilter[] { new MatchFilter() });
        setRawInputType(InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setMovementMethod (ArrowKeyMovementMethod.getInstance());
    }

    public MatchTextEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        createExtras();
    }

    public MatchTextEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        createExtras();
    }

    public MatchTextEditor(Context context) {
        super(context);
        mContext = context;
        createExtras();
    }

    protected void hitReturn() {
        if (length() > 0) {
            ((MainActivity) mContext).doActionButton(true);
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start,
                                 int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (mContext != null) {
            ((MainActivity)mContext).setActionButton();
        }
    }
}
