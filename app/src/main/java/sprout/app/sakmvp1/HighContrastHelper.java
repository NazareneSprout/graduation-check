package sprout.app.sakmvp1;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.Activity;
import android.content.res.Configuration;

public class HighContrastHelper {

    private static final String PREF_NAME = "high_contrast_settings";
    private static final String KEY_HIGH_CONTRAST_ENABLED = "high_contrast_enabled";

    public static boolean isHighContrastEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_HIGH_CONTRAST_ENABLED, false);
    }

    public static void setHighContrastEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST_ENABLED, enabled).apply();
    }

    public static void toggleHighContrast(Context context) {
        boolean currentState = isHighContrastEnabled(context);
        setHighContrastEnabled(context, !currentState);
    }

    public static void applyHighContrastTheme(Activity activity) {
        if (isHighContrastEnabled(activity)) {
            activity.setTheme(R.style.Theme_SakMvp1_HighContrast);
        } else {
            activity.setTheme(R.style.Theme_SakMvp1);
        }
    }
}