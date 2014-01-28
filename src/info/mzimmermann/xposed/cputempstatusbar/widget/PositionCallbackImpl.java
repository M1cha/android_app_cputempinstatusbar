package info.mzimmermann.xposed.cputempstatusbar.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class PositionCallbackImpl implements PositionCallback {
	
	private LinearLayout mSystemIconArea;
	private LinearLayout mStatusBarContents;
	private LinearLayout container;
	private View cputemp;

	@Override
	public void setup(MethodHookParam param, View v) {
		 cputemp = v;
		 mSystemIconArea = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mSystemIconArea");
         mStatusBarContents = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mStatusBarContents");
         Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");

         container = new LinearLayout(mContext);
         container.setOrientation(LinearLayout.HORIZONTAL);
         container.setWeightSum(1);
         container.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
         container.setVisibility(View.GONE);
         mStatusBarContents.addView(container, 0);
	}

	@Override
	public void setAbsoluteLeft() {
		mSystemIconArea.removeView(cputemp);
		container.removeView(cputemp);

		container.addView(cputemp);
		container.setVisibility(View.VISIBLE);
	}

	@Override
	public void setLeft() {
		mSystemIconArea.removeView(cputemp);
		container.removeView(cputemp);

        mSystemIconArea.addView(cputemp, 0);
        container.setVisibility(View.GONE);
	}

	@Override
	public void setRight() {
		mSystemIconArea.removeView(cputemp);
		container.removeView(cputemp);

        mSystemIconArea.addView(cputemp);
        container.setVisibility(View.GONE);
	}

	@Override
	public LinearLayout getClockParent() {
		return mStatusBarContents;
		
	}
}
