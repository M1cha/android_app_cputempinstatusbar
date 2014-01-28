package info.mzimmermann.xposed.cputempstatusbar.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class LegacyPositionCallbackImpl implements PositionCallback {

	private LinearLayout mStatusIcons;
	private LinearLayout mIcons;
	private View cputemp;
	private LinearLayout mNotificationIconArea;

	@Override
	public void setup(MethodHookParam param, View v) {
		 cputemp = v;
		 mStatusIcons = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mStatusIcons");
		 mIcons = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mIcons");
		 mNotificationIconArea = (LinearLayout)mIcons.getChildAt(0);
	}

	@Override
	public void setAbsoluteLeft() {
		removeFromParent();
		mNotificationIconArea.addView(cputemp, 0);
	}

	@Override
	public void setLeft() {
		removeFromParent();
		mIcons.addView(cputemp, mIcons.indexOfChild(mStatusIcons));
	}

	@Override
	public void setRight() {
		removeFromParent();
		mIcons.addView(cputemp);
	}
	
	private void removeFromParent() {
		if(cputemp.getParent()!=null)
			((ViewGroup)cputemp.getParent()).removeView(cputemp);
	}

	@Override
	public LinearLayout getClockParent() {
		return mIcons;
	}
}
