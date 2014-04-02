package com.sb.p1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ClockColorChanger implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	int inetCondition=0;
	private Context mContext = null;
	int origcolor=0; int newcolor=Color.GREEN;
	String clockClass, networkClass;
	private static XSharedPreferences preference = null;
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
	preference = new XSharedPreferences(ClockColorChanger.class.getPackage().getName());
	XposedBridge.log("ClockColorChanger v1.1 started!");
	origcolor = Integer.parseInt(preference.getString("InetNotPresentColor", "0"));
	newcolor  = Integer.parseInt(preference.getString("InetPresentColor", "-16711936"));
	}
	
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		try {
			if (!lpparam.packageName.equals("com.android.systemui"))
				return;

			XposedBridge.log("ClockColorChanger v1.1 started!");
			XposedBridge.log("Android SDK: " + Build.VERSION.SDK_INT);

			if (Build.VERSION.SDK_INT>=11) {// ver 4.0+ 
				clockClass="com.android.systemui.statusbar.policy.Clock";
				networkClass="com.android.systemui.statusbar.policy.NetworkController";
			} else { // ver 2.3
				XposedBridge.log("Classes for Android 2.3 will be used");
				clockClass="com.android.systemui.statusbar.Clock";
				networkClass="com.android.systemui.statusbar.policy.StatusBarPolicy";
			}

			final Class<?> clock = XposedHelpers.findClass(clockClass, lpparam.classLoader);

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

			XposedHelpers.findAndHookMethod(networkClass, lpparam.classLoader, "updateConnectivity", Intent.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Intent intent = (Intent) param.args[0];
					inetCondition = intent.getIntExtra("inetCondition", 0);
					if (mContext == null) mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
					//XposedBridge.log(new SimpleDateFormat("HH:mm:SS").format(new Date())+" inetCondition: " + inetCondition);
					mContext.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED)); //force updates clock
				}
			});

		} catch (Exception e){
			XposedBridge.log("\nException="+e.toString());
			XposedBridge.log("\nMessage="+e.getMessage());
			XposedBridge.log("\nStackTrace="+e.getStackTrace().toString());
            String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();            
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();
            XposedBridge.log(className + "." + methodName + "():" + lineNumber);
            throw(e);
		}

	} //handleLoadPackage

}