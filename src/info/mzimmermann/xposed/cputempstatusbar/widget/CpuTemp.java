package info.mzimmermann.xposed.cputempstatusbar.widget;

import info.mzimmermann.libxposed.LXTarget;
import info.mzimmermann.xposed.cputempstatusbar.Utils;
import info.mzimmermann.xposed.cputempstatusbar.XposedInit;
import info.mzimmermann.xposed.cputempstatusbar.activities.SettingsActivity;
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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
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
	private File tempFile = null;
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
		initTempFile();
		try {
			Utils.log("tempFile="+tempFile==null?"null":tempFile.getPath());
		} catch (Exception e) {
			Utils.log(Log.getStackTraceString(e));
		}

		// style
		setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		setTextColor(context.getResources().getColor(
				android.R.color.holo_blue_light));
		setSingleLine(true);
		setPadding(6, 0, 0, 0);
		setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	}
	
	private void initTempFile() {
		String temperature_file = mContext.getSharedPreferences(PREF_KEY, 0).getString("temperature_file", null);
		tempFile = Utils.getTempFile(mContext, temperature_file);
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
		int updateInterval = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("update_interval", "1000"));
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
				updateTemperature();
			}
			else if(intent.getAction().equals(SettingsActivity.ACTION_SETTINGS_UPDATE)) {
				LXTarget.receivePreferences(context.getSharedPreferences(PREF_KEY, 0), intent);
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

	private void updateTemperature() {
		try {
			FileInputStream fis = new FileInputStream(tempFile);
			StringBuffer sbTemp = new StringBuffer("");

			// read temperature
			byte[] buffer = new byte[1024];
			while (fis.read(buffer) != -1) {
				sbTemp.append(new String(buffer));
			}
			fis.close();

			// parse temp
			String sTemp = sbTemp.toString().replaceAll("[^0-9]+", "");
			float temp = Float.valueOf(sTemp);

			// measure system
			String measurement = mContext.getSharedPreferences(PREF_KEY, 0).getString("measurement", "C");
			if(measurement.equals("F")){
				temp = (temp * 9/5) + 32;
			}
			
			// apply divider
			int divider = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("temperature_divider", "1"));
			if(divider!=0)
				temp = temp/divider;
			
			// set text
			setText((int)temp + "°"+measurement);
			
			// set text color
			int color_mode = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("color_mode", "0"));
			TextView mClock = XposedInit.getClock();
			switch(color_mode) {
			// auto
			case 0:
				if(mClock!=null) {
					setTextColor(mClock.getCurrentTextColor());
				}
				break;
			//manual
			case 1:
				int configured_color = mContext.getSharedPreferences(PREF_KEY, 0).getInt("configured_color", Color.BLACK);
				setTextColor(configured_color);
				break;
			// temp
			case 2:
				int color_low = mContext.getSharedPreferences(PREF_KEY, 0).getInt("color_low", Color.BLUE);
				int color_middle = mContext.getSharedPreferences(PREF_KEY, 0).getInt("color_middle", Color.GREEN);
				int color_high = mContext.getSharedPreferences(PREF_KEY, 0).getInt("color_high", Color.RED);
				int temp_middle = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("temp_middle", "20"));
				int temp_high = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("temp_high", "50"));
				
				if(temp>=temp_high)
					setTextColor(color_high);
				if(temp>=temp_middle)
					setTextColor(color_middle);
				else
					setTextColor(color_low);
				
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Utils.log(Log.getStackTraceString(e));
			setText("-");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		if(key.equals("position")) {
			int position = Integer.parseInt(pref.getString("position", "0"));
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
			int updateInterval = Integer.parseInt(pref.getString("update_interval", "1000"));
			cancelAlarm();
			setAlarm(updateInterval);
		}
		
		else if(key.equals("temperature_file")) {
			String temperature_file = pref.getString("temperature_file", null);
			tempFile = Utils.getTempFile(mContext, temperature_file);
		}

		updateTemperature();
	}
}
