package com.android.settings.zephyr;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.Utils;
import android.os.SystemProperties;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.SummaryLoader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.android.internal.logging.MetricsProto.MetricsEvent;

public class ZephyrFragment extends SettingsPreferenceFragment {

	private static final String LOG_TAG = "ZephyrFragment";

	private static final String KEY_MOD_BUILD_DATE = "build_date";
	private static final String KEY_ZEPHYR_VERSION = "zephyr_version";
	private static final String KEY_DEVICE_MAINTAINER = "device_maintainer";
	private static final String KEY_ZEPHYR_OTA = "zephyrota";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.zephyr_fragment);

	setValueSummary(KEY_MOD_BUILD_DATE, "ro.build.date");
	setValueSummary(KEY_ZEPHYR_VERSION, "ro.zephyr.version");
	findPreference(KEY_ZEPHYR_VERSION).setEnabled(true);
	setMaintainerSummary(KEY_DEVICE_MAINTAINER, "ro.zephyr.maintainer");

	 // Only the owner should see the Updater settings, if it exists
        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            removePreferenceIfPackageNotInstalled(findPreference(KEY_ZEPHYR_OTA));
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_ZEPHYR_OTA));
        }
 
    }

   private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private void setMaintainerSummary(String preference, String property) {
        try {
            String maintainers = SystemProperties.get(property,
                    getResources().getString(R.string.device_info_default));
            findPreference(preference).setSummary(maintainers);
            if (maintainers.contains(",")) {
                findPreference(preference).setTitle(
                        getResources().getString(R.string.device_maintainers));
            }
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri=((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName=matcher.find()?matcher.group(1):null;
        if(packageName != null) {
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(packageName,
                        PackageManager.GET_ACTIVITIES);
                if (!pi.applicationInfo.enabled) {
                    Log.e(LOG_TAG,"package "+packageName+" is disabled, hiding preference.");
                    getPreferenceScreen().removePreference(preference);
                    return true;
                }
            } catch (NameNotFoundException e) {
               Log.e(LOG_TAG,"package "+packageName+" not installed, hiding preference.");
               getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ABOUT_ZEPHYR;
    }
}
