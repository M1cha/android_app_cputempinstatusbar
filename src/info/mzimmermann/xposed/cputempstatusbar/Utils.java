package info.mzimmermann.xposed.cputempstatusbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.os.Environment;

public class Utils {
	public static void log(String s) {
		try {
			File file = new File(Environment.getExternalStorageDirectory(), "cpufreq_statusbar.log");
			FileOutputStream out = new FileOutputStream(file, true);
			out.write((s+"\n").getBytes());
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void clearLog() {
		File file = new File(Environment.getExternalStorageDirectory(), "cpufreq_statusbar.log");
		file.delete();
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
	
	public static String readLog() {
		try {
			File file = new File(Environment.getExternalStorageDirectory(), "cpufreq_statusbar.log");
			FileInputStream in = new FileInputStream(file);
			String ret = convertStreamToString(in);
		    in.close();        
		    return ret;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Log cannot be read.";
	}
}
