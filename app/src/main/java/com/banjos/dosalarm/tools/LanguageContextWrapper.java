package com.banjos.dosalarm.tools;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LanguageContextWrapper extends ContextWrapper {

    public LanguageContextWrapper(Context base) {
        super(base);
    }

    public static ContextWrapper wrap(Context context, String language) {
        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();
        Locale newLocale;
        if (language.equals("iw")) {
            newLocale = new Locale("iw", "IL");
        } else if (language.equals("fr")) {
            newLocale = Locale.FRENCH;
        } else if (language.equals("es")) {
            newLocale = new Locale("es", "ES");
        } else {
            newLocale = Locale.ENGLISH;
        }
        Locale.setDefault(newLocale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(newLocale);
            LocaleList localeList = new LocaleList(newLocale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
            context = context.createConfigurationContext(configuration);
        } else {
            configuration.setLocale(newLocale);
            context = context.createConfigurationContext(configuration);
        }

        return new LanguageContextWrapper(context);
    }
}
