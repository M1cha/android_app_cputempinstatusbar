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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class CpuTemp extends TextView implements OnSharedPreferenceChangeListener {
	final public static String INTENT_ACTION_UPDATE = "cputemp_update_timer";
	final private Context mContext;
	final public static String PREF_KEY = "cputemp_preferences";
	private PendingIntent pi = null;
	private File freqFile = null;
	private int freqMode = 0;
	private boolean celsius;
	public LinearLayout containerLayoutLeft = null;
	public LinearLayout containerLayoutRight = null;

	public CpuTemp(Context context) {
		this(context, null);
	}

	public CpuTemp(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CpuTemp(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		
		// init
		initFreqFile();

		// style
		setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		setTextColor(context.getResources().getColor(
				android.R.color.holo_blue_light));
		setSingleLine(true);
		setPadding(6, 0, 0, 0);
		setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	}
	
	private void initFreqFile() {
		freqFile = new File("/sys/devices/platform/omap/omap_temp_sensor.0/temperature");
		if(!freqFile.exists()) {
			freqFile = new File("/sys/kernel/debug/tegra_thermal/temp_tj");
			freqMode = 0;
		}
		else return;

		if(!freqFile.exists()) {
			freqFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp");
			freqMode = 0;
		}
		else return;

		if(!freqFile.exists()) {
			freqFile = new File("/sys/class/thermal/thermal_zone0/temp");
			freqMode = 0;
		}
		else return;

		if(!freqFile.exists()) {
			freqFile = new File("/sys/class/thermal/thermal_zone1/temp");
			freqMode = 0;
		}
		else return;

		if(!freqFile.exists()) {
			freqFile = new File("/sys/devices/platform/s5p-tmu/curr_temp");
			freqMode = 1;
		}
		else return;

		if(!freqFile.exists()) {
			freqFile = new File("/sys/devices/virtual/thermal/thermal_zone0/temp");
			freqMode = 1;
		}
		else return;

		if(!freqFile.exists()) {
			freqFile = new File("/sys/devices/virtual/thermal/thermal_zone1/temp");
			freqMode = 1;
		}
		else return;

		if(!freqFile.exists()) {
			freqFile = null;
		}
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

		// start update interval
		int updateInterval = mContext.getSharedPreferences(PREF_KEY, 0).getInt("update_interval", 1000);
		String measurement = mContext.getSharedPreferences(PREF_KEY, 0).getString("measurement", "C");
		if(measurement.equals("F")){
			celsius = false;
		}
		setAlarm(updateInterval);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		mContext.unregisterReceiver(mBroadcastReceiver);
		mContext.getSharedPreferences(PREF_KEY, 0).unregisterOnSharedPreferenceChangeListener(this);
		cancelAlarm();
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
			FileInputStream fis = new FileInputStream(freqFile);
			StringBuffer sFreq = new StringBuffer("");

			byte[] buffer = new byte[1024];
			while (fis.read(buffer) != -1) {
				sFreq.append(new String(buffer));
			}
			fis.close();

			String text = "";
			int freq = Integer.parseInt(sFreq.toString().replaceAll("[^0-9]+", ""));
			
			if(!celsius){ ///Convert to Fahrenheit
				freq = (freq * 9/5) + 32;
			}
			if(freqMode == 0) {
				text=String.valueOf(freq);
			}
			else if(freqMode==1) {
				text = String.valueOf(freq/10f);
			}
			
			if(!celsius){ 
				setText(text + "°F");
			} else {
				setText(text + "°C");
			}

			

		} catch (Exception e) {
			e.printStackTrace();
			setText("-");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		if(key.equals("position")) {
			int position = pref.getInt("position", 0);
			if(position==0) {
				containerLayoutRight.removeView(this);
				containerLayoutLeft.removeView(this);

				containerLayoutRight.addView(this, 0);
				containerLayoutLeft.setVisibility(View.GONE);
			}
			else if(position==1) {
				containerLayoutRight.removeView(this);
				containerLayoutLeft.removeView(this);

				containerLayoutRight.addView(this);
				containerLayoutLeft.setVisibility(View.GONE);
			}
			else if(position==2) {
				containerLayoutRight.removeView(this);
				containerLayoutLeft.removeView(this);

				containerLayoutLeft.addView(this);
				containerLayoutLeft.setVisibility(View.VISIBLE);
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
