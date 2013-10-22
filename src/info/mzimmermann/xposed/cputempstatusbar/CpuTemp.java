package info.mzimmermann.xposed.cputempstatusbar;

import java.io.File;
import java.io.FileInputStream;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class CpuTemp extends TextView implements OnSharedPreferenceChangeListener {
	final public static String INTENT_ACTION_UPDATE = "cputemp_update_timer";
	final private Context mContext;
	final public static String PREF_KEY = "cputemp_preferences";
	private PendingIntent pi = null;

	public CpuTemp(Context context) {
		this(context, null);
	}

	public CpuTemp(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CpuTemp(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		
		// style
		setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		setTextColor(context.getResources().getColor(
				android.R.color.holo_blue_light));
		setSingleLine(true);
		setPadding(6, 0, 0, 0);
		setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		
		// start update interval
		int updateInterval = mContext.getSharedPreferences(PREF_KEY, 0).getInt("update_interval", 1000);
		setAlarm(updateInterval);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		IntentFilter filter = new IntentFilter();
		filter.addAction(INTENT_ACTION_UPDATE);
		filter.addAction(SettingsActivity.ACTION_SETTINGS_UPDATE);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		mContext.registerReceiver(mBroadcastReceiver, filter);
		mContext.getSharedPreferences(PREF_KEY, 0).registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		mContext.unregisterReceiver(mBroadcastReceiver);
		mContext.getSharedPreferences(PREF_KEY, 0).unregisterOnSharedPreferenceChangeListener(this);
		super.onDetachedFromWindow();
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		private boolean isScreenOn = true;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				isScreenOn = true;
			}
			else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				isScreenOn = false;
			}
			else if(intent.getAction().equals(INTENT_ACTION_UPDATE) && isScreenOn) {
				updateFrequency();
			}
			else if(intent.getAction().equals(SettingsActivity.ACTION_SETTINGS_UPDATE)) {
				if(mContext!=null) {
					SharedPreferences sp = mContext.getSharedPreferences(PREF_KEY, 0);
					Editor editor = sp.edit();
					if(intent.hasExtra("update_interval")) {
						editor.putInt("update_interval", intent.getIntExtra("update_interval", 1000));
					}
					if(intent.hasExtra("position")) {
						editor.putInt("position", intent.getIntExtra("position", 0));
					}
					editor.commit();
				}
			}
		}
	};

	public void setAlarm(int interval) {
		AlarmManager am = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(INTENT_ACTION_UPDATE);
		pi = PendingIntent.getBroadcast(mContext, 0, intent, 0);
		am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), interval, pi);
	}
	
	public void cancelAlarm() {
		AlarmManager am = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);
		if(pi!=null)  {
			am.cancel(pi);
			pi.cancel();
		}
	}

	private void updateFrequency() {
		try {
			int mode = 0;
			File f = new File("/sys/devices/platform/omap/omap_temp_sensor.0/temperature");
			if(!f.exists()) {
				f = new File("/sys/kernel/debug/tegra_thermal/temp_tj");
				mode = 0;
			}
			if(!f.exists()) {
				f = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp");
				mode = 0;
			}
			if(!f.exists()) {
				f = new File("/sys/class/thermal/thermal_zone0/temp");
				mode = 0;
			}
			if(!f.exists()) {
				f = new File("/sys/class/thermal/thermal_zone1/temp");
				mode = 0;
			}
			if(!f.exists()) {
				f = new File("/sys/devices/platform/s5p-tmu/curr_temp");
				mode = 1;
			}
			
			FileInputStream fis = new FileInputStream(f);
			StringBuffer sFreq = new StringBuffer("");

			byte[] buffer = new byte[1024];
			while (fis.read(buffer) != -1) {
				sFreq.append(new String(buffer));
			}
			fis.close();

			String text = "";
			int freq = Integer.parseInt(sFreq.toString().replaceAll("[^0-9]+", ""));
			if(mode == 0) {
				text=String.valueOf(freq);
			}
			else if(mode==1) {
				text = String.valueOf(freq/10f);
			}
			
			setText(text + "°C");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		if(key.equals("position")) {
			int position = pref.getInt("position", 0);
			if(position==0) {
				LinearLayout mSystemIconArea = (LinearLayout)getParent();
				mSystemIconArea.removeView(this);
				mSystemIconArea.addView(this, 0);
			}
			else if(position==1) {
				LinearLayout mSystemIconArea = (LinearLayout)getParent();
				mSystemIconArea.removeView(this);
				mSystemIconArea.addView(this);
			}
		}
		
		else if(key.equals("update_interval")) {
			Log.e("DEBUG", "new update_interval");
			int updateInterval = pref.getInt("update_interval", 1000);
			cancelAlarm();
			setAlarm(updateInterval);
		}
	}
}
