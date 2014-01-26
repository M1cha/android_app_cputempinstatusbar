package info.mzimmermann.xposed.cputempstatusbar;

import info.mzimmermann.libxposed.LXTools;
import info.mzimmermann.xposed.cputempstatusbar.widget.CpuTemp;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedInit implements IXposedHookLoadPackage {
	private static TextView tvClock;
	
	public static TextView getClock() {
		return tvClock;
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
				tvClock = (TextView)param.thisObject;
			}
		});
		
		XposedHelpers.findAndHookMethod(
			"com.android.systemui.statusbar.phone.PhoneStatusBar",
			lpparam.classLoader, "makeStatusBarView", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					try {
						LinearLayout mSystemIconArea = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mSystemIconArea");
						LinearLayout mStatusBarContents = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mStatusBarContents");
						Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
						LXTools.removeInvalidPreferences(Utils.prefs, mContext.getSharedPreferences(CpuTemp.PREF_KEY, 0));
						int position = mContext.getSharedPreferences(CpuTemp.PREF_KEY, 0).getInt("position", 0);
						CpuTemp cpuTemp = new CpuTemp(mContext);

						LinearLayout container = new LinearLayout(mContext);
						container.setOrientation(LinearLayout.HORIZONTAL);
						container.setWeightSum(1);
						container.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
						container.setVisibility(View.GONE);
						mStatusBarContents.addView(container, 0);

						cpuTemp.containerLayoutLeft = container;
						cpuTemp.containerLayoutRight = mSystemIconArea;

						if(position==0) {
							mSystemIconArea.addView(cpuTemp, 0);
						}
						else if(position==1) {
							mSystemIconArea.addView(cpuTemp);
						}
						else if(position==2) {
							container.addView(cpuTemp);
							container.setVisibility(View.VISIBLE);
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
