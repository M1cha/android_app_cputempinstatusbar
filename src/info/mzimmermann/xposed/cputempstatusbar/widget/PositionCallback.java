package info.mzimmermann.xposed.cputempstatusbar.widget;

import android.view.View;
import android.widget.LinearLayout;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public interface PositionCallback {
	void setup(MethodHookParam param, View v);
	void setAbsoluteLeft();
	void setLeft();
	void setRight();
	LinearLayout getClockParent();
}
