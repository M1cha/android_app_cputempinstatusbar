package info.mzimmermann.xposed.cputempstatusbar;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedInit implements IXposedHookLoadPackage {
	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		if (!lpparam.packageName.equals("com.android.systemui"))
			return;

		XposedHelpers.findAndHookMethod(
				"com.android.systemui.statusbar.phone.PhoneStatusBar",
				lpparam.classLoader, "makeStatusBarView", new XC_MethodHook() {

					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						LinearLayout mSystemIconArea = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mSystemIconArea");
						Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
						TextView mClock = (TextView)XposedHelpers.getObjectField(param.thisObject, "mClock");
						int position = mContext.getSharedPreferences(CpuTemp.PREF_KEY, 0).getInt("position", 0);
						
						CpuTemp cpuFreq = new CpuTemp(mContext);
						cpuFreq.setTextColor(mClock.getCurrentTextColor());

						if(position==0) {
							mSystemIconArea.addView(cpuFreq, 0);
						}
						else if(position==1) {
							mSystemIconArea.addView(cpuFreq);
						}
					}
				});
	}

}
