package com.sb.p1;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ClockColorChanger implements IXposedHookLoadPackage {
	int inetCondition=0;
	private Context mContext = null;
	int origcolor=0; int newcolor=Color.GREEN;
	
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
	if (!lpparam.packageName.equals("com.android.systemui"))
	return;

	XposedBridge.log("ClockColorChanger started!");
	final Class<?> clock = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);

	
	XposedHelpers.findAndHookMethod(clock, "updateClock", new XC_MethodHook() {
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
	TextView tv = (TextView) param.thisObject;
	if (origcolor==0) origcolor = tv.getTextColors().getDefaultColor(); //store original color
	String text = tv.getText().toString();
	tv.setText(text);
	if (inetCondition>50) tv.setTextColor(newcolor); else tv.setTextColor(origcolor);
	}
	});

	XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.policy.NetworkController", lpparam.classLoader, "updateConnectivity", Intent.class, new XC_MethodHook() {
 @Override
 protected void afterHookedMethod(MethodHookParam param) throws Throwable {
 Intent intent = (Intent) param.args[0];
 inetCondition = intent.getIntExtra("inetCondition", 0);
 if (mContext == null) mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
 //XposedBridge.log(new SimpleDateFormat("HH:mm:SS").format(new Date())+" inetCondition: " + inetCondition);
 mContext.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED)); //force updates clock
 }
 });
	}

}