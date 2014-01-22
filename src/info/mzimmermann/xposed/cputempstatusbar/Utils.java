package info.mzimmermann.xposed.cputempstatusbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import android.content.Context;
import de.robv.android.xposed.XposedBridge;

public class Utils {
	public static void log(String s) {
		XposedBridge.log(s);
	}
	
	public static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line).append("\n");
	    }
	    return sb.toString();
	}
	
	public static String[] getTemperatureFiles() {
		ArrayList<String> result = new ArrayList<String>();
		result.add("AUTO");
		
		try {
			InputStream in = Runtime.getRuntime()
					.exec("busybox find /sys -type f -name *temp*")
					.getInputStream();
			BufferedReader inBuffered = new BufferedReader(
					new InputStreamReader(in));

			String line = null;
			while ((line = inBuffered.readLine()) != null) {
				result.add(line.trim());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result.toArray(new String[]{});
	}
	
	private static String[] freqFiles = {
		"/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
		"/sys/kernel/debug/tegra_thermal/temp_tj",
		"/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
		"/sys/class/thermal/thermal_zone0/temp",
		"/sys/class/thermal/thermal_zone1/temp",
		"/sys/devices/platform/s5p-tmu/curr_temp",
		"/sys/devices/virtual/thermal/thermal_zone0/temp",
		"/sys/devices/virtual/thermal/thermal_zone1/temp",
		"/sys/devices/system/cpu/cpufreq/cput_attributes/cur_temp",
	};
	
	public static File getFreqFile(Context context, String fileName) {
		File ret = null;
		
		if(fileName!=null) { 
			ret = new File(fileName);
			if(!ret.exists() || !ret.canRead())
				ret = null;
		}
		
		if(ret==null || fileName.equals("AUTO")) {
			for(String freqFileName : freqFiles) {
				ret = new File(freqFileName);
				if(!ret.exists() || !ret.canRead()) {
					ret = null;
					continue;
				}
				else break;
			}
		}
		
		if(ret==null) {
			Utils.log("Couldn't find any freq files!");
		}
		
		return ret;
	}
}
