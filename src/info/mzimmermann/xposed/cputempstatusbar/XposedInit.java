package info.mzimmermann.xposed.cputempstatusbar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import info.mzimmermann.libxposed.LXTools;
import info.mzimmermann.xposed.cputempstatusbar.widget.CpuTemp;
import info.mzimmermann.xposed.cputempstatusbar.widget.LegacyPositionCallbackImpl;
import info.mzimmermann.xposed.cputempstatusbar.widget.PositionCallback;
import info.mzimmermann.xposed.cputempstatusbar.widget.PositionCallbackImpl;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedInit implements IXposedHookLoadPackage {
	private static ArrayList<TextView> tvClocks = new ArrayList<TextView>();
	private static PositionCallback mPositionCallback = null;
	private CpuTemp mCpuTemp = null;
	
	public static TextView getClock() {
		if(mPositionCallback==null) 
			return null;

		for(TextView tvClock : tvClocks) {
			if(mPositionCallback.getClockParent().findViewById(tvClock.getId())!=null) {
				return tvClock;
			}
		}

		return null;
	}
	
	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		if (!lpparam.packageName.equals("com.android.systemui"))
			return;
		
		XposedBridge.hookAllConstructors(XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader), new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);
				if(tvClocks.indexOf(param.thisObject)==-1) {
					tvClocks.add((TextView)param.thisObject);
				}
			}
		});
		
		// we hook this method to follow alpha changes in kitkat
		Method setAlpha = XposedHelpers.findMethodBestMatch(XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader), "setAlpha", Float.class);
		XposedBridge.hookMethod(setAlpha, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);

				TextView mClock = XposedInit.getClock();
				if(param.thisObject!=mClock)
					return;

				if(mCpuTemp!=null && mClock!=null) {
					mCpuTemp.setAlpha(mClock.getAlpha());
				}
			}
		});

		XposedHelpers.findAndHookMethod(
			"com.android.systemui.statusbar.phone.PhoneStatusBar",
			lpparam.classLoader, "makeStatusBarView", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					try {
						// create cputemp view
						Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
						LXTools.removeInvalidPreferences(Utils.prefs, mContext.getSharedPreferences(CpuTemp.PREF_KEY, 0));
						mCpuTemp = new CpuTemp(mContext);

						// identify legacy mode
						boolean legacy = false;
						try {
							XposedHelpers.getObjectField(param.thisObject, "mSystemIconArea");
						}
						catch(NoSuchFieldError e) {
							legacy = true;
						}
						
						// create callback
						if(legacy)
							mCpuTemp.mPositionCallback = new LegacyPositionCallbackImpl();
						else
							mCpuTemp.mPositionCallback = new PositionCallbackImpl();
						
						// initial setup
						mPositionCallback = mCpuTemp.mPositionCallback;
						mCpuTemp.mPositionCallback.setup(param, mCpuTemp);
						
						// set position
						LXTools.removeInvalidPreferences(Utils.prefs, mContext.getSharedPreferences(CpuTemp.PREF_KEY, 0));
						int position = Integer.parseInt(mContext.getSharedPreferences(CpuTemp.PREF_KEY, 0).getString("position", "0"));
						if(position==0) {
							mCpuTemp.mPositionCallback.setLeft();
						}
						else if(position==1) {
							mCpuTemp.mPositionCallback.setRight();
						}
						else if(position==2) {
							mCpuTemp.mPositionCallback.setAbsoluteLeft();
						}
					}
					catch(Exception e) {
						Utils.log(Log.getStackTraceString(e));
					}
				}
			}
		);
	}
}
