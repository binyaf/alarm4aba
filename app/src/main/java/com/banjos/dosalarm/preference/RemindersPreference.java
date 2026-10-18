package com.banjos.dosalarm.preference;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.DialogPreference;
import com.banjos.dosalarm.R;

public class RemindersPreference extends DialogPreference {

    public RemindersPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RemindersPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RemindersPreference(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.preference.R.attr.dialogPreferenceStyle);
    }

    public RemindersPreference(Context context) {
        this(context, null);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.dialog_reminders;
    }
}
