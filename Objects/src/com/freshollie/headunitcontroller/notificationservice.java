package com.freshollie.headunitcontroller;


import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.objects.ServiceHelper;
import anywheresoftware.b4a.debug.*;

public class notificationservice extends android.app.Service {
	public static class notificationservice_BR extends android.content.BroadcastReceiver {

		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			android.content.Intent in = new android.content.Intent(context, notificationservice.class);
			if (intent != null)
				in.putExtra("b4a_internal_intent", intent);
			context.startService(in);
		}

	}
    static notificationservice mostCurrent;
	public static BA processBA;
    private ServiceHelper _service;
    public static Class<?> getObject() {
		return notificationservice.class;
	}
	@Override
	public void onCreate() {
        mostCurrent = this;
        if (processBA == null) {
		    processBA = new BA(this, null, null, "com.freshollie.headunitcontroller", "com.freshollie.headunitcontroller.notificationservice");
            try {
                Class.forName(BA.applicationContext.getPackageName() + ".main").getMethod("initializeProcessGlobals").invoke(null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            processBA.loadHtSubs(this.getClass());
            ServiceHelper.init();
        }
        _service = new ServiceHelper(this);
        processBA.service = this;
        processBA.setActivityPaused(false);
        if (BA.isShellModeRuntimeCheck(processBA)) {
			processBA.raiseEvent2(null, true, "CREATE", true, "com.freshollie.headunitcontroller.notificationservice", processBA, _service);
		}
        BA.LogInfo("** Service (notificationservice) Create **");
        processBA.raiseEvent(null, "service_create");
        processBA.runHook("oncreate", this, null);
    }
		@Override
	public void onStart(android.content.Intent intent, int startId) {
		handleStart(intent);
    }
    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId) {
    	handleStart(intent);
        processBA.runHook("onstartcommand", this, new Object[] {intent, flags, startId});
		return android.app.Service.START_NOT_STICKY;
    }
    private void handleStart(android.content.Intent intent) {
    	BA.LogInfo("** Service (notificationservice) Start **");
    	java.lang.reflect.Method startEvent = processBA.htSubs.get("service_start");
    	if (startEvent != null) {
    		if (startEvent.getParameterTypes().length > 0) {
    			anywheresoftware.b4a.objects.IntentWrapper iw = new anywheresoftware.b4a.objects.IntentWrapper();
    			if (intent != null) {
    				if (intent.hasExtra("b4a_internal_intent"))
    					iw.setObject((android.content.Intent) intent.getParcelableExtra("b4a_internal_intent"));
    				else
    					iw.setObject(intent);
    			}
    			processBA.raiseEvent(null, "service_start", iw);
    		}
    		else {
    			processBA.raiseEvent(null, "service_start");
    		}
    	}
    }
	@Override
	public android.os.IBinder onBind(android.content.Intent intent) {
		return null;
	}
	@Override
	public void onDestroy() {
        BA.LogInfo("** Service (notificationservice) Destroy **");
		processBA.raiseEvent(null, "service_destroy");
        processBA.service = null;
		mostCurrent = null;
		processBA.setActivityPaused(true);
        processBA.runHook("ondestroy", this, null);
	}
public anywheresoftware.b4a.keywords.Common __c = null;
public static anywheresoftware.b4a.objects.NotificationListenerWrapper.NotificationListener _notificationlistener = null;
public com.freshollie.headunitcontroller.main _main = null;
public com.freshollie.headunitcontroller.controllingservice _controllingservice = null;
public static String  _notificationlistener_notificationposted(anywheresoftware.b4a.objects.NotificationListenerWrapper.StatusBarNotificationWrapper _notification) throws Exception{
 //BA.debugLineNum = 22;BA.debugLine="Sub NotificationListener_NotificationPosted(Notifi";
 //BA.debugLineNum = 24;BA.debugLine="If Notification.IsInitialized = False Then";
if (_notification.IsInitialized()==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 25;BA.debugLine="Return";
if (true) return "";
 };
 //BA.debugLineNum = 28;BA.debugLine="If ControllingService.ServiceRunning = False Then";
if (mostCurrent._controllingservice._servicerunning==anywheresoftware.b4a.keywords.Common.False) { 
if (true) return "";};
 //BA.debugLineNum = 29;BA.debugLine="If ControllingService.PowerOn = False Then Return";
if (mostCurrent._controllingservice._poweron==anywheresoftware.b4a.keywords.Common.False) { 
if (true) return "";};
 //BA.debugLineNum = 31;BA.debugLine="Select(Notification.PackageName)";
switch (BA.switchObjectToInt((_notification.getPackageName()),"au.com.shiftyjelly.pocketcasts","com.apple.android.music","com.google.android.apps.maps")) {
case 0:
 //BA.debugLineNum = 34;BA.debugLine="Log(\"Podcasts playing\")";
anywheresoftware.b4a.keywords.Common.Log("Podcasts playing");
 //BA.debugLineNum = 35;BA.debugLine="ControllingService.PreferenceManager.SetBoolean(";
mostCurrent._controllingservice._preferencemanager.SetBoolean("PlayPodcast",anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 36;BA.debugLine="ControllingService.PreferenceManager.SetBoolean(";
mostCurrent._controllingservice._preferencemanager.SetBoolean("PlayMusic",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 37;BA.debugLine="ControllingService.PreferenceManager.SetBoolean(";
mostCurrent._controllingservice._preferencemanager.SetBoolean("PlayRadio",anywheresoftware.b4a.keywords.Common.False);
 break;
case 1:
 //BA.debugLineNum = 40;BA.debugLine="If ControllingService.AudioFocusListener.isMusic";
if (mostCurrent._controllingservice._audiofocuslistener.isMusicActive()) { 
 //BA.debugLineNum = 41;BA.debugLine="Log(\"Apple music playing\")";
anywheresoftware.b4a.keywords.Common.Log("Apple music playing");
 //BA.debugLineNum = 42;BA.debugLine="ControllingService.PreferenceManager.SetBoolean";
mostCurrent._controllingservice._preferencemanager.SetBoolean("PlayMusic",anywheresoftware.b4a.keywords.Common.True);
 }else {
 //BA.debugLineNum = 44;BA.debugLine="Log(\"Apple music stopped\")";
anywheresoftware.b4a.keywords.Common.Log("Apple music stopped");
 //BA.debugLineNum = 45;BA.debugLine="ControllingService.PreferenceManager.SetBoolean";
mostCurrent._controllingservice._preferencemanager.SetBoolean("PlayMusic",anywheresoftware.b4a.keywords.Common.False);
 };
 break;
case 2:
 //BA.debugLineNum = 49;BA.debugLine="If ControllingService.PreferenceManager.GetBoole";
if (mostCurrent._controllingservice._preferencemanager.GetBoolean("DrivingMode")==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 50;BA.debugLine="Log(\"Driving Mode started\")";
anywheresoftware.b4a.keywords.Common.Log("Driving Mode started");
 //BA.debugLineNum = 51;BA.debugLine="ControllingService.PreferenceManager.SetBoolean";
mostCurrent._controllingservice._preferencemanager.SetBoolean("DrivingMode",anywheresoftware.b4a.keywords.Common.True);
 };
 break;
}
;
 //BA.debugLineNum = 55;BA.debugLine="End Sub";
return "";
}
public static String  _notificationlistener_notificationremoved(anywheresoftware.b4a.objects.NotificationListenerWrapper.StatusBarNotificationWrapper _notification) throws Exception{
 //BA.debugLineNum = 65;BA.debugLine="Sub NotificationListener_NotificationRemoved(Notif";
 //BA.debugLineNum = 66;BA.debugLine="If Notification.IsInitialized = False Then";
if (_notification.IsInitialized()==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 67;BA.debugLine="Return";
if (true) return "";
 };
 //BA.debugLineNum = 70;BA.debugLine="If ControllingService.PowerOn = False Or Controll";
if (mostCurrent._controllingservice._poweron==anywheresoftware.b4a.keywords.Common.False || mostCurrent._controllingservice._usbmanager.GetDevices().length==0) { 
 //BA.debugLineNum = 71;BA.debugLine="Return";
if (true) return "";
 };
 //BA.debugLineNum = 74;BA.debugLine="Select(Notification.PackageName)";
switch (BA.switchObjectToInt((_notification.getPackageName()),"au.com.shiftyjelly.pocketcasts","com.apple.android.music","com.google.android.apps.maps")) {
case 0:
 //BA.debugLineNum = 77;BA.debugLine="Log(\"Podcasts stopped\")";
anywheresoftware.b4a.keywords.Common.Log("Podcasts stopped");
 //BA.debugLineNum = 78;BA.debugLine="ControllingService.PreferenceManager.SetBoolean(";
mostCurrent._controllingservice._preferencemanager.SetBoolean("PlayPodcast",anywheresoftware.b4a.keywords.Common.False);
 break;
case 1:
 //BA.debugLineNum = 81;BA.debugLine="If ControllingService.AudioFocusListener.isMusic";
if (mostCurrent._controllingservice._audiofocuslistener.isMusicActive()==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 82;BA.debugLine="Log(\"Apple music stopped\")";
anywheresoftware.b4a.keywords.Common.Log("Apple music stopped");
 //BA.debugLineNum = 83;BA.debugLine="ControllingService.PreferenceManager.SetBoolean";
mostCurrent._controllingservice._preferencemanager.SetBoolean("PlayMusic",anywheresoftware.b4a.keywords.Common.False);
 };
 break;
case 2:
 //BA.debugLineNum = 87;BA.debugLine="Log(\"Driving Mode stopped\")";
anywheresoftware.b4a.keywords.Common.Log("Driving Mode stopped");
 //BA.debugLineNum = 88;BA.debugLine="ControllingService.PreferenceManager.SetBoolean(";
mostCurrent._controllingservice._preferencemanager.SetBoolean("DrivingMode",anywheresoftware.b4a.keywords.Common.False);
 break;
}
;
 //BA.debugLineNum = 93;BA.debugLine="End Sub";
return "";
}
public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 5;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 6;BA.debugLine="Dim NotificationListener As NotificationListener";
_notificationlistener = new anywheresoftware.b4a.objects.NotificationListenerWrapper.NotificationListener();
 //BA.debugLineNum = 11;BA.debugLine="End Sub";
return "";
}
public static String  _service_create() throws Exception{
 //BA.debugLineNum = 14;BA.debugLine="Sub Service_Create";
 //BA.debugLineNum = 15;BA.debugLine="NotificationListener.Initialize(\"NotificationList";
_notificationlistener.Initialize(processBA,"NotificationListener");
 //BA.debugLineNum = 16;BA.debugLine="End Sub";
return "";
}
public static String  _service_destroy() throws Exception{
 //BA.debugLineNum = 95;BA.debugLine="Sub Service_Destroy";
 //BA.debugLineNum = 97;BA.debugLine="End Sub";
return "";
}
public static String  _service_start(anywheresoftware.b4a.objects.IntentWrapper _startingintent) throws Exception{
 //BA.debugLineNum = 18;BA.debugLine="Sub Service_Start (StartingIntent As Intent)";
 //BA.debugLineNum = 19;BA.debugLine="If NotificationListener.HandleIntent(StartingInte";
if (_notificationlistener.HandleIntent(_startingintent)) { 
if (true) return "";};
 //BA.debugLineNum = 20;BA.debugLine="End Sub";
return "";
}
public static String  _wait(int _seconds) throws Exception{
long _ti = 0L;
 //BA.debugLineNum = 57;BA.debugLine="Sub Wait(Seconds As Int)";
 //BA.debugLineNum = 58;BA.debugLine="Dim Ti As Long";
_ti = 0L;
 //BA.debugLineNum = 59;BA.debugLine="Ti = DateTime.Now + (Seconds * 1000)";
_ti = (long) (anywheresoftware.b4a.keywords.Common.DateTime.getNow()+(_seconds*1000));
 //BA.debugLineNum = 60;BA.debugLine="Do While DateTime.Now < Ti";
while (anywheresoftware.b4a.keywords.Common.DateTime.getNow()<_ti) {
 //BA.debugLineNum = 61;BA.debugLine="DoEvents";
anywheresoftware.b4a.keywords.Common.DoEvents();
 }
;
 //BA.debugLineNum = 63;BA.debugLine="End Sub";
return "";
}
}
