package info.mzimmermann.xposed.cputempstatusbar.activities;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import info.mzimmermann.libxposed.LXTools;
import info.mzimmermann.libxposed.apps.LXMyApp;
import info.mzimmermann.xposed.cputempstatusbar.R;
import info.mzimmermann.xposed.cputempstatusbar.Utils;
import info.mzimmermann.xposed.cputempstatusbar.widget.CpuTemp;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;
	public static final String ACTION_SETTINGS_UPDATE = "cputemp-statusbar-settings-update";
	private static Context mContext = null;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mContext = getApplicationContext();
		LXTools.removeInvalidPreferences(Utils.prefs, mContext.getSharedPreferences(CpuTemp.PREF_KEY, 0));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'general' preferences.
		addPreferencesFromResource(R.xml.pref_general);

		
		bindPreferenceSummaryToValue(findPreference("update_interval"));
		bindPreferenceSummaryToValue(findPreference("position"));
		
		ListPreference temperature_file = (ListPreference)findPreference("temperature_file");
		bindPreferenceSummaryToValue(findPreference("temperature_divider"));
		String[] files = Utils.getTemperatureFiles();
		temperature_file.setEntries(files);
		temperature_file.setEntryValues(files);
		bindPreferenceSummaryToValue(findPreference("temperature_file"));
		bindPreferenceSummaryToValue(findPreference("measurement"));
		bindPreferenceSummaryToValue(findPreference("color_mode"));
		bindPreferenceSummaryToValue(findPreference("configured_color"));
		bindPreferenceSummaryToValue(findPreference("color_low"));
		bindPreferenceSummaryToValue(findPreference("color_middle"));
		bindPreferenceSummaryToValue(findPreference("color_high"));
		bindPreferenceSummaryToValue(findPreference("temp_middle"));
		bindPreferenceSummaryToValue(findPreference("temp_high"));
//		LXMyApp.setTransferOnPreferenceChangeListener(ACTION_SETTINGS_UPDATE, findPreference("manual_color"));
//		LXMyApp.setTransferOnPreferenceChangeListener(ACTION_SETTINGS_UPDATE, findPreference("configured_color"));
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isXLargeTablet(context);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();
			PreferenceManager pm = preference.getPreferenceManager();
			SharedPreferences sp = pm.getSharedPreferences();
			
			// get color_mode
			String sColorMode="0";
			if(preference.getKey().equals("color_mode"))
				sColorMode = value.toString();
			else
				sColorMode = PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString("color_mode", "0");
			int color_mode = Integer.parseInt(sColorMode);
			
			switch(color_mode) {
			case 0:
				pm.findPreference("configured_color").setEnabled(false);
				pm.findPreference("color_low").setEnabled(false);
				pm.findPreference("color_middle").setEnabled(false);
				pm.findPreference("color_high").setEnabled(false);
				pm.findPreference("temp_middle").setEnabled(false);
				pm.findPreference("temp_high").setEnabled(false);
				break;
			case 1:
				pm.findPreference("configured_color").setEnabled(true);
				pm.findPreference("color_low").setEnabled(false);
				pm.findPreference("color_middle").setEnabled(false);
				pm.findPreference("color_high").setEnabled(false);
				pm.findPreference("temp_middle").setEnabled(false);
				pm.findPreference("temp_high").setEnabled(false);
				break;
			case 2:
				pm.findPreference("configured_color").setEnabled(false);
				pm.findPreference("color_low").setEnabled(true);
				pm.findPreference("color_middle").setEnabled(true);
				pm.findPreference("color_high").setEnabled(true);
				pm.findPreference("temp_middle").setEnabled(true);
				pm.findPreference("temp_high").setEnabled(true);
				break;
			}
			
			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference
						.setSummary(index >= 0 ? listPreference.getEntries()[index]
								: null);

			} else if (preference instanceof RingtonePreference) {
				// For ringtone preferences, look up the correct display value
				// using RingtoneManager.
				if (TextUtils.isEmpty(stringValue)) {
					// Empty values correspond to 'silent' (no ringtone).
					// preference.setSummary(R.string.pref_ringtone_silent);

				} else {
					Ringtone ringtone = RingtoneManager.getRingtone(
							preference.getContext(), Uri.parse(stringValue));

					if (ringtone == null) {
						// Clear the summary if there was a lookup error.
						preference.setSummary(null);
					} else {
						// Set the summary to reflect the new ringtone display
						// name.
						String name = ringtone
								.getTitle(preference.getContext());
						preference.setSummary(name);
					}
				}

			} else if(!(preference instanceof ColorPickerPreference)) {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		LXMyApp.setTransferOnPreferenceChangeListener(ACTION_SETTINGS_UPDATE, preference, sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getAll().get(preference.getKey()));
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("example_text"));
			bindPreferenceSummaryToValue(findPreference("example_list"));
		}
	}
}
