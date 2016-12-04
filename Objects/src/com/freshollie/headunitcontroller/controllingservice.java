package com.freshollie.headunitcontroller;

import android.content.Intent;
import android.util.Log;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.content.ComponentName; 
import android.media.session.MediaSessionManager;
import android.media.session.MediaController;
import android.app.ActivityManager;
import android.app.usage.UsageStatsManager;
import android.app.usage.UsageStats;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;

import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.objects.ServiceHelper;
import anywheresoftware.b4a.debug.*;

public class controllingservice extends android.app.Service {
	public static class controllingservice_BR extends android.content.BroadcastReceiver {

		@Override
		public void onReceive(android.content.Context context, android.content.Intent intent) {
			android.content.Intent in = new android.content.Intent(context, controllingservice.class);
			if (intent != null)
				in.putExtra("b4a_internal_intent", intent);
			context.startService(in);
		}

	}
    static controllingservice mostCurrent;
	public static BA processBA;
    private ServiceHelper _service;
    public static Class<?> getObject() {
		return controllingservice.class;
	}
	@Override
	public void onCreate() {
        mostCurrent = this;
        if (processBA == null) {
		    processBA = new BA(this, null, null, "com.freshollie.headunitcontroller", "com.freshollie.headunitcontroller.controllingservice");
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
			processBA.raiseEvent2(null, true, "CREATE", true, "com.freshollie.headunitcontroller.controllingservice", processBA, _service);
		}
        BA.LogInfo("** Service (controllingservice) Create **");
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
    	BA.LogInfo("** Service (controllingservice) Start **");
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
        BA.LogInfo("** Service (controllingservice) Destroy **");
		processBA.raiseEvent(null, "service_destroy");
        processBA.service = null;
		mostCurrent = null;
		processBA.setActivityPaused(true);
        processBA.runHook("ondestroy", this, null);
	}
public anywheresoftware.b4a.keywords.Common __c = null;
public static com.rootsoft.broadcastreceiver.BroadCastReceiver _broadcastreceiver = null;
public static anywheresoftware.b4j.object.JavaObject _mediaplay = null;
public static com.freshollie.audiofocus.AudioFocus _audiofocuslistener = null;
public static boolean _servicerunning = false;
public static anywheresoftware.b4a.objects.preferenceactivity.PreferenceManager _preferencemanager = null;
public static anywheresoftware.b4a.phone.PackageManagerWrapper _packagemanager = null;
public static anywheresoftware.b4a.objects.usb.UsbManagerWrapper _usbmanager = null;
public static int _numvariables = 0;
public static boolean _poweron = false;
public static anywheresoftware.b4a.objects.MediaPlayerWrapper _mediaplayer = null;
public static corsaro.sucommand.library.SuCommand _su = null;
public static anywheresoftware.b4a.objects.NotificationWrapper _notification = null;
public com.freshollie.headunitcontroller.main _main = null;
public com.freshollie.headunitcontroller.notificationservice _notificationservice = null;
public static String  _broadcastreceiver_onreceive(String _action,Object _i) throws Exception{
anywheresoftware.b4a.objects.IntentWrapper _intent = null;
 //BA.debugLineNum = 172;BA.debugLine="Sub BroadcastReceiver_onReceive(Action As String,";
 //BA.debugLineNum = 173;BA.debugLine="If I <> Null Then";
if (_i!= null) { 
 //BA.debugLineNum = 174;BA.debugLine="Dim intent As Intent = i";
_intent = new anywheresoftware.b4a.objects.IntentWrapper();
_intent.setObject((android.content.Intent)(_i));
 };
 //BA.debugLineNum = 177;BA.debugLine="Log(Action)";
anywheresoftware.b4a.keywords.Common.Log(_action);
 //BA.debugLineNum = 178;BA.debugLine="Select(Action)";
switch (BA.switchObjectToInt((_action),"android.intent.action.ACTION_POWER_CONNECTED","android.intent.action.ACTION_POWER_DISCONNECTED","com.apple.music.client.player.play")) {
case 0:
 //BA.debugLineNum = 181;BA.debugLine="Log(\"Power connected\")";
anywheresoftware.b4a.keywords.Common.Log("Power connected");
 //BA.debugLineNum = 182;BA.debugLine="PowerOn = True";
_poweron = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 183;BA.debugLine="StartRoutine";
_startroutine();
 break;
case 1:
 //BA.debugLineNum = 186;BA.debugLine="Log(\"Power disconnected\")";
anywheresoftware.b4a.keywords.Common.Log("Power disconnected");
 //BA.debugLineNum = 187;BA.debugLine="Log(mediaplay.RunMethod(\"getForegroundPackageNam";
anywheresoftware.b4a.keywords.Common.Log(BA.ObjectToString(_mediaplay.RunMethod("getForegroundPackageName",(Object[])(anywheresoftware.b4a.keywords.Common.Null))));
 //BA.debugLineNum = 188;BA.debugLine="If mediaplay.RunMethod(\"getForegroundPackageName";
if ((_mediaplay.RunMethod("getForegroundPackageName",(Object[])(anywheresoftware.b4a.keywords.Common.Null))).equals((Object)("com.google.android.apps.maps"))) { 
 //BA.debugLineNum = 189;BA.debugLine="Log(\"Maps last application on top\")";
anywheresoftware.b4a.keywords.Common.Log("Maps last application on top");
 //BA.debugLineNum = 190;BA.debugLine="PreferenceManager.SetBoolean(\"LaunchMaps\", True";
_preferencemanager.SetBoolean("LaunchMaps",anywheresoftware.b4a.keywords.Common.True);
 }else {
 //BA.debugLineNum = 192;BA.debugLine="PreferenceManager.SetBoolean(\"LaunchMaps\", Fals";
_preferencemanager.SetBoolean("LaunchMaps",anywheresoftware.b4a.keywords.Common.False);
 };
 //BA.debugLineNum = 195;BA.debugLine="PowerOn = False";
_poweron = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 196;BA.debugLine="StopRoutine";
_stoproutine();
 break;
case 2:
 //BA.debugLineNum = 199;BA.debugLine="Log(\"Wat\")";
anywheresoftware.b4a.keywords.Common.Log("Wat");
 break;
}
;
 //BA.debugLineNum = 204;BA.debugLine="If UsbManager.GetDevices().Length < 3 Then Return";
if (_usbmanager.GetDevices().length<3) { 
if (true) return "";};
 //BA.debugLineNum = 206;BA.debugLine="If PowerOn = False Then Return 'Only check for th";
if (_poweron==anywheresoftware.b4a.keywords.Common.False) { 
if (true) return "";};
 //BA.debugLineNum = 208;BA.debugLine="Select(Action)";
switch (BA.switchObjectToInt((_action),"com.freshollie.radioapp.RUNNING","com.freshollie.radioapp.STOPPED")) {
case 0:
 //BA.debugLineNum = 211;BA.debugLine="Log(\"Radio Player started\")";
anywheresoftware.b4a.keywords.Common.Log("Radio Player started");
 //BA.debugLineNum = 212;BA.debugLine="PreferenceManager.SetBoolean(\"PlayRadio\", True)";
_preferencemanager.SetBoolean("PlayRadio",anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 213;BA.debugLine="PreferenceManager.SetBoolean(\"PlayPodcast\", Fals";
_preferencemanager.SetBoolean("PlayPodcast",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 214;BA.debugLine="PreferenceManager.SetBoolean(\"PlayMusic\", False)";
_preferencemanager.SetBoolean("PlayMusic",anywheresoftware.b4a.keywords.Common.False);
 break;
case 1:
 //BA.debugLineNum = 217;BA.debugLine="Log(\"Radio player stopped\")";
anywheresoftware.b4a.keywords.Common.Log("Radio player stopped");
 //BA.debugLineNum = 218;BA.debugLine="PreferenceManager.SetBoolean(\"PlayRadio\", False)";
_preferencemanager.SetBoolean("PlayRadio",anywheresoftware.b4a.keywords.Common.False);
 break;
}
;
 //BA.debugLineNum = 223;BA.debugLine="End Sub";
return "";
}
public static String  _killmaps() throws Exception{
 //BA.debugLineNum = 241;BA.debugLine="Private Sub KillMaps";
 //BA.debugLineNum = 242;BA.debugLine="Log(\"Killing maps\")";
anywheresoftware.b4a.keywords.Common.Log("Killing maps");
 //BA.debugLineNum = 243;BA.debugLine="Su.SuCommand(\"am force-stop com.google.android.ap";
_su.SuCommand("am force-stop com.google.android.apps.maps");
 //BA.debugLineNum = 244;BA.debugLine="End Sub";
return "";
}
public static String  _playmusic() throws Exception{
Object[] _args = null;
 //BA.debugLineNum = 232;BA.debugLine="Sub PlayMusic";
 //BA.debugLineNum = 233;BA.debugLine="Dim args(1) As Object";
_args = new Object[(int) (1)];
{
int d0 = _args.length;
for (int i0 = 0;i0 < d0;i0++) {
_args[i0] = new Object();
}
}
;
 //BA.debugLineNum = 234;BA.debugLine="Su.SuCommand(\"am startservice -a 'com.apple.music";
_su.SuCommand("am startservice -a 'com.apple.music.client.player.play_pause' -n com.apple.android.music/com.apple.android.svmediaplayer.player.MusicService");
 //BA.debugLineNum = 235;BA.debugLine="End Sub";
return "";
}
public static String  _playpodcasts() throws Exception{
Object[] _args = null;
 //BA.debugLineNum = 226;BA.debugLine="Sub PlayPodcasts";
 //BA.debugLineNum = 227;BA.debugLine="Dim args(1) As Object";
_args = new Object[(int) (1)];
{
int d0 = _args.length;
for (int i0 = 0;i0 < d0;i0++) {
_args[i0] = new Object();
}
}
;
 //BA.debugLineNum = 228;BA.debugLine="args(0) = \"au.com.shiftyjelly.pocketcasts\"";
_args[(int) (0)] = (Object)("au.com.shiftyjelly.pocketcasts");
 //BA.debugLineNum = 229;BA.debugLine="mediaplay.RunMethod(\"togglePause\", args)";
_mediaplay.RunMethod("togglePause",_args);
 //BA.debugLineNum = 230;BA.debugLine="End Sub";
return "";
}
public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 5;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 6;BA.debugLine="Dim BroadcastReceiver As BroadCastReceiver";
_broadcastreceiver = new com.rootsoft.broadcastreceiver.BroadCastReceiver();
 //BA.debugLineNum = 7;BA.debugLine="Dim mediaplay As JavaObject";
_mediaplay = new anywheresoftware.b4j.object.JavaObject();
 //BA.debugLineNum = 8;BA.debugLine="Dim AudioFocusListener As AudioFocus";
_audiofocuslistener = new com.freshollie.audiofocus.AudioFocus();
 //BA.debugLineNum = 9;BA.debugLine="Dim ServiceRunning As Boolean";
_servicerunning = false;
 //BA.debugLineNum = 10;BA.debugLine="Dim PreferenceManager As PreferenceManager";
_preferencemanager = new anywheresoftware.b4a.objects.preferenceactivity.PreferenceManager();
 //BA.debugLineNum = 11;BA.debugLine="Dim PackageManager As PackageManager";
_packagemanager = new anywheresoftware.b4a.phone.PackageManagerWrapper();
 //BA.debugLineNum = 12;BA.debugLine="Dim UsbManager As UsbManager";
_usbmanager = new anywheresoftware.b4a.objects.usb.UsbManagerWrapper();
 //BA.debugLineNum = 13;BA.debugLine="Dim NumVariables As Int = 8";
_numvariables = (int) (8);
 //BA.debugLineNum = 14;BA.debugLine="Dim PowerOn As Boolean";
_poweron = false;
 //BA.debugLineNum = 15;BA.debugLine="Dim MediaPlayer As MediaPlayer";
_mediaplayer = new anywheresoftware.b4a.objects.MediaPlayerWrapper();
 //BA.debugLineNum = 16;BA.debugLine="Dim Su As SuCommand";
_su = new corsaro.sucommand.library.SuCommand();
 //BA.debugLineNum = 17;BA.debugLine="Dim Notification As Notification";
_notification = new anywheresoftware.b4a.objects.NotificationWrapper();
 //BA.debugLineNum = 22;BA.debugLine="End Sub";
return "";
}
public static String  _service_create() throws Exception{
 //BA.debugLineNum = 43;BA.debugLine="Sub Service_Create";
 //BA.debugLineNum = 44;BA.debugLine="PowerOn = True";
_poweron = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 45;BA.debugLine="mediaplay.InitializeContext";
_mediaplay.InitializeContext(processBA);
 //BA.debugLineNum = 46;BA.debugLine="MediaPlayer.Initialize";
_mediaplayer.Initialize();
 //BA.debugLineNum = 47;BA.debugLine="ServiceRunning = False";
_servicerunning = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 48;BA.debugLine="If PreferenceManager.GetAll.Size < NumVariables T";
if (_preferencemanager.GetAll().getSize()<_numvariables) { 
 //BA.debugLineNum = 49;BA.debugLine="SetUpVariables";
_setupvariables();
 };
 //BA.debugLineNum = 51;BA.debugLine="UsbManager.Initialize";
_usbmanager.Initialize();
 //BA.debugLineNum = 52;BA.debugLine="AudioFocusListener.Initialize(\"AudioFocusListener";
_audiofocuslistener.Initialize(processBA,"AudioFocusListener");
 //BA.debugLineNum = 53;BA.debugLine="BroadcastReceiver.Initialize(\"BroadCastReceiver\")";
_broadcastreceiver.Initialize(processBA,"BroadCastReceiver");
 //BA.debugLineNum = 56;BA.debugLine="BroadcastReceiver.addAction(\"android.intent.actio";
_broadcastreceiver.addAction("android.intent.action.ACTION_POWER_CONNECTED");
 //BA.debugLineNum = 57;BA.debugLine="BroadcastReceiver.addAction(\"android.intent.actio";
_broadcastreceiver.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
 //BA.debugLineNum = 58;BA.debugLine="BroadcastReceiver.addAction(\"com.android.music";
_broadcastreceiver.addAction("com.android.music.metachanged");
 //BA.debugLineNum = 59;BA.debugLine="BroadcastReceiver.addAction(\"com.android.music";
_broadcastreceiver.addAction("com.android.music.playstatechanged");
 //BA.debugLineNum = 60;BA.debugLine="BroadcastReceiver.addAction(\"com.android.music";
_broadcastreceiver.addAction("com.android.music.playbackcomplete");
 //BA.debugLineNum = 61;BA.debugLine="BroadcastReceiver.addAction(\"com.android.music";
_broadcastreceiver.addAction("com.android.music.queuechanged");
 //BA.debugLineNum = 62;BA.debugLine="BroadcastReceiver.addAction(\"com.freshollie.radio";
_broadcastreceiver.addAction("com.freshollie.radioapp.RUNNING");
 //BA.debugLineNum = 63;BA.debugLine="BroadcastReceiver.addAction(\"com.freshollie.radio";
_broadcastreceiver.addAction("com.freshollie.radioapp.STOPPED");
 //BA.debugLineNum = 64;BA.debugLine="BroadcastReceiver.registerReceiver(\"\")";
_broadcastreceiver.registerReceiver("");
 //BA.debugLineNum = 66;BA.debugLine="End Sub";
return "";
}
public static String  _service_destroy() throws Exception{
 //BA.debugLineNum = 237;BA.debugLine="Sub Service_Destroy";
 //BA.debugLineNum = 238;BA.debugLine="Service.StopForeground(0)";
mostCurrent._service.StopForeground((int) (0));
 //BA.debugLineNum = 239;BA.debugLine="End Sub";
return "";
}
public static String  _service_start(anywheresoftware.b4a.objects.IntentWrapper _startingintent) throws Exception{
 //BA.debugLineNum = 68;BA.debugLine="Sub Service_Start (StartingIntent As Intent)";
 //BA.debugLineNum = 69;BA.debugLine="ServiceRunning = True";
_servicerunning = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 70;BA.debugLine="Notification.Initialize";
_notification.Initialize();
 //BA.debugLineNum = 71;BA.debugLine="Notification.Sound = False";
_notification.setSound(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 72;BA.debugLine="Notification.Vibrate = False";
_notification.setVibrate(anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 74;BA.debugLine="Notification.Icon = \"ic_tablet_black_48dp\"";
_notification.setIcon("ic_tablet_black_48dp");
 //BA.debugLineNum = 75;BA.debugLine="Notification.SetInfo(\"Tablet Controller is runnin";
_notification.SetInfo(processBA,"Tablet Controller is running","TEST",(Object)(mostCurrent._main.getObject()));
 //BA.debugLineNum = 76;BA.debugLine="Notification.OnGoingEvent = True";
_notification.setOnGoingEvent(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 78;BA.debugLine="Service.StartForeground(1, Notification)";
mostCurrent._service.StartForeground((int) (1),(android.app.Notification)(_notification.getObject()));
 //BA.debugLineNum = 79;BA.debugLine="StartRoutine";
_startroutine();
 //BA.debugLineNum = 81;BA.debugLine="End Sub";
return "";
}
public static String  _setupvariables() throws Exception{
 //BA.debugLineNum = 32;BA.debugLine="Sub SetUpVariables";
 //BA.debugLineNum = 33;BA.debugLine="PreferenceManager.SetBoolean(\"OpenRadio\", False)";
_preferencemanager.SetBoolean("OpenRadio",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 34;BA.debugLine="PreferenceManager.SetBoolean(\"PlayMusic\", False)";
_preferencemanager.SetBoolean("PlayMusic",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 35;BA.debugLine="PreferenceManager.SetBoolean(\"PlayPodcast\", False";
_preferencemanager.SetBoolean("PlayPodcast",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 36;BA.debugLine="PreferenceManager.SetBoolean(\"PlayRadio\", False)";
_preferencemanager.SetBoolean("PlayRadio",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 37;BA.debugLine="PreferenceManager.SetBoolean(\"LaunchMaps\", False)";
_preferencemanager.SetBoolean("LaunchMaps",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 38;BA.debugLine="PreferenceManager.SetBoolean(\"DrivingMode\", False";
_preferencemanager.SetBoolean("DrivingMode",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 39;BA.debugLine="PreferenceManager.SetBoolean(\"DebugMode\", False)";
_preferencemanager.SetBoolean("DebugMode",anywheresoftware.b4a.keywords.Common.False);
 //BA.debugLineNum = 40;BA.debugLine="PreferenceManager.SetString(\"NumDevices\", \"3\")";
_preferencemanager.SetString("NumDevices","3");
 //BA.debugLineNum = 41;BA.debugLine="End Sub";
return "";
}
public static String  _startapps() throws Exception{
anywheresoftware.b4a.objects.IntentWrapper _in = null;
 //BA.debugLineNum = 83;BA.debugLine="Sub StartApps";
 //BA.debugLineNum = 85;BA.debugLine="If PreferenceManager.GetBoolean(\"DrivingMode\") =";
if (_preferencemanager.GetBoolean("DrivingMode")==anywheresoftware.b4a.keywords.Common.True && _preferencemanager.GetBoolean("LaunchMaps")==anywheresoftware.b4a.keywords.Common.True) { 
 //BA.debugLineNum = 86;BA.debugLine="Log(\"Staring driving mode\")";
anywheresoftware.b4a.keywords.Common.Log("Staring driving mode");
 //BA.debugLineNum = 87;BA.debugLine="Dim in As Intent";
_in = new anywheresoftware.b4a.objects.IntentWrapper();
 //BA.debugLineNum = 88;BA.debugLine="in.Initialize(in.ACTION_VIEW, \"google.navigation";
_in.Initialize(_in.ACTION_VIEW,"google.navigation:/?free=1&mode=d&entry=fnls");
 //BA.debugLineNum = 89;BA.debugLine="If in.IsInitialized Then";
if (_in.IsInitialized()) { 
 //BA.debugLineNum = 90;BA.debugLine="in.SetComponent(\"com.google.android.app";
_in.SetComponent("com.google.android.apps.maps/com.google.android.maps.MapsActivity");
 //BA.debugLineNum = 91;BA.debugLine="StartActivity(in)";
anywheresoftware.b4a.keywords.Common.StartActivity(processBA,(Object)(_in.getObject()));
 };
 };
 //BA.debugLineNum = 95;BA.debugLine="If PreferenceManager.GetBoolean(\"PlayPodcast\") Th";
if (_preferencemanager.GetBoolean("PlayPodcast")) { 
 //BA.debugLineNum = 96;BA.debugLine="Log(\"Playing Podcast\")";
anywheresoftware.b4a.keywords.Common.Log("Playing Podcast");
 //BA.debugLineNum = 97;BA.debugLine="Wait(4)";
_wait((int) (4));
 //BA.debugLineNum = 98;BA.debugLine="If PowerOn Then PlayPodcasts";
if (_poweron) { 
_playpodcasts();};
 }else if(_preferencemanager.GetBoolean("PlayRadio")) { 
 //BA.debugLineNum = 101;BA.debugLine="Log(\"Playing radio\")";
anywheresoftware.b4a.keywords.Common.Log("Playing radio");
 //BA.debugLineNum = 102;BA.debugLine="Do While UsbManager.GetDevices().Length < 3 And";
while (_usbmanager.GetDevices().length<3 && _preferencemanager.GetBoolean("DebugMode")==anywheresoftware.b4a.keywords.Common.False) {
 //BA.debugLineNum = 103;BA.debugLine="DoEvents";
anywheresoftware.b4a.keywords.Common.DoEvents();
 }
;
 //BA.debugLineNum = 105;BA.debugLine="Su.SuCommand(\"am startservice -a android.intent.";
_su.SuCommand("am startservice -a android.intent.action.MAIN -n com.freshollie.radioapp/.radioservice");
 }else if(_preferencemanager.GetBoolean("PlayMusic")) { 
 //BA.debugLineNum = 109;BA.debugLine="Log(\"Playing music\")";
anywheresoftware.b4a.keywords.Common.Log("Playing music");
 //BA.debugLineNum = 110;BA.debugLine="Wait(1)";
_wait((int) (1));
 //BA.debugLineNum = 111;BA.debugLine="If PowerOn Then PlayMusic";
if (_poweron) { 
_playmusic();};
 }else {
 //BA.debugLineNum = 113;BA.debugLine="Wait(4)";
_wait((int) (4));
 };
 //BA.debugLineNum = 115;BA.debugLine="End Sub";
return "";
}
public static String  _startgps() throws Exception{
 //BA.debugLineNum = 123;BA.debugLine="Sub StartGPS";
 //BA.debugLineNum = 124;BA.debugLine="Su.SuCommand(\"am startservice -a org.broeuschmeul";
_su.SuCommand("am startservice -a org.broeuschmeul.android.gps.usb.provider.nmea.intent.action.START_GPS_PROVIDER");
 //BA.debugLineNum = 125;BA.debugLine="Log(\"Starting gps\")";
anywheresoftware.b4a.keywords.Common.Log("Starting gps");
 //BA.debugLineNum = 126;BA.debugLine="End Sub";
return "";
}
public static String  _startroutine() throws Exception{
int _numdevices = 0;
 //BA.debugLineNum = 133;BA.debugLine="Sub StartRoutine";
 //BA.debugLineNum = 134;BA.debugLine="MediaPlayer.Load(File.DirAssets, \"5sec.mp3\")";
_mediaplayer.Load(anywheresoftware.b4a.keywords.Common.File.getDirAssets(),"5sec.mp3");
 //BA.debugLineNum = 135;BA.debugLine="MediaPlayer.Play  'Remove noise from audio lines";
_mediaplayer.Play();
 //BA.debugLineNum = 136;BA.debugLine="MediaPlayer.Looping = True";
_mediaplayer.setLooping(anywheresoftware.b4a.keywords.Common.True);
 //BA.debugLineNum = 137;BA.debugLine="Log(\"Starting empty noise\")";
anywheresoftware.b4a.keywords.Common.Log("Starting empty noise");
 //BA.debugLineNum = 139;BA.debugLine="AudioFocusListener.Volume = 13";
_audiofocuslistener.setVolume((int) (13));
 //BA.debugLineNum = 140;BA.debugLine="StartTwilight";
_starttwilight();
 //BA.debugLineNum = 141;BA.debugLine="Log(\"Set volume\")";
anywheresoftware.b4a.keywords.Common.Log("Set volume");
 //BA.debugLineNum = 143;BA.debugLine="If PowerOn Then";
if (_poweron) { 
 //BA.debugLineNum = 144;BA.debugLine="StartApps";
_startapps();
 }else {
 //BA.debugLineNum = 146;BA.debugLine="Wait(4)";
_wait((int) (4));
 };
 //BA.debugLineNum = 149;BA.debugLine="Log(\"Started apps\")";
anywheresoftware.b4a.keywords.Common.Log("Started apps");
 //BA.debugLineNum = 151;BA.debugLine="Dim numDevices = PreferenceManager.GetString(\"Num";
_numdevices = (int)(Double.parseDouble(_preferencemanager.GetString("NumDevices")));
 //BA.debugLineNum = 153;BA.debugLine="Do While UsbManager.GetDevices().Length < numDevi";
while (_usbmanager.GetDevices().length<_numdevices && _preferencemanager.GetBoolean("DebugMode")==anywheresoftware.b4a.keywords.Common.False) {
 //BA.debugLineNum = 154;BA.debugLine="DoEvents";
anywheresoftware.b4a.keywords.Common.DoEvents();
 }
;
 //BA.debugLineNum = 156;BA.debugLine="If PowerOn Then";
if (_poweron) { 
 //BA.debugLineNum = 157;BA.debugLine="StartShuttleXpress";
_startshuttlexpress();
 //BA.debugLineNum = 158;BA.debugLine="StartGPS";
_startgps();
 };
 //BA.debugLineNum = 160;BA.debugLine="End Sub";
return "";
}
public static String  _startshuttlexpress() throws Exception{
 //BA.debugLineNum = 128;BA.debugLine="Sub StartShuttleXpress";
 //BA.debugLineNum = 129;BA.debugLine="Su.SuCommand(\"am startservice -a android.intent.a";
_su.SuCommand("am startservice -a android.intent.action.MAIN -n com.freshollie.shuttlexpress/.shuttlexpressservice");
 //BA.debugLineNum = 130;BA.debugLine="Log(\"Starting ShuttleXpressDriver\")";
anywheresoftware.b4a.keywords.Common.Log("Starting ShuttleXpressDriver");
 //BA.debugLineNum = 131;BA.debugLine="End Sub";
return "";
}
public static String  _starttwilight() throws Exception{
 //BA.debugLineNum = 118;BA.debugLine="Sub StartTwilight";
 //BA.debugLineNum = 119;BA.debugLine="Su.SuCommand(\"am startservice -a android.intent.a";
_su.SuCommand("am startservice -a android.intent.action.MAIN -n com.autobright.kevinforeman.autobright/.AutoBrightService");
 //BA.debugLineNum = 120;BA.debugLine="Su.SuCommand(\"am start -a android.intent.action.M";
_su.SuCommand("am start -a android.intent.action.MAIN -n com.autobright.kevinforeman.autobright/.AutoBright");
 //BA.debugLineNum = 121;BA.debugLine="End Sub";
return "";
}
public static String  _stoproutine() throws Exception{
anywheresoftware.b4a.agraham.threading.Threading _thread1 = null;
 //BA.debugLineNum = 162;BA.debugLine="Sub StopRoutine";
 //BA.debugLineNum = 163;BA.debugLine="MediaPlayer.Stop";
_mediaplayer.Stop();
 //BA.debugLineNum = 165;BA.debugLine="Dim thread1 As Thread";
_thread1 = new anywheresoftware.b4a.agraham.threading.Threading();
 //BA.debugLineNum = 166;BA.debugLine="thread1.Initialise(\"KillMaps\")";
_thread1.Initialise(processBA,"KillMaps");
 //BA.debugLineNum = 167;BA.debugLine="thread1.Start(\"KillMaps\", Null)";
_thread1.Start("KillMaps",(Object[])(anywheresoftware.b4a.keywords.Common.Null));
 //BA.debugLineNum = 169;BA.debugLine="End Sub";
return "";
}
public static String  _wait(int _seconds) throws Exception{
long _ti = 0L;
 //BA.debugLineNum = 24;BA.debugLine="Sub Wait(Seconds As Int)";
 //BA.debugLineNum = 25;BA.debugLine="Dim Ti As Long";
_ti = 0L;
 //BA.debugLineNum = 26;BA.debugLine="Ti = DateTime.Now + (Seconds * 1000)";
_ti = (long) (anywheresoftware.b4a.keywords.Common.DateTime.getNow()+(_seconds*1000));
 //BA.debugLineNum = 27;BA.debugLine="Do While DateTime.Now < Ti";
while (anywheresoftware.b4a.keywords.Common.DateTime.getNow()<_ti) {
 //BA.debugLineNum = 28;BA.debugLine="DoEvents";
anywheresoftware.b4a.keywords.Common.DoEvents();
 }
;
 //BA.debugLineNum = 30;BA.debugLine="End Sub";
return "";
}

public void setUpListener(){
	MediaSessionManager.OnActiveSessionsChangedListener mSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
	    @Override
	    public void onActiveSessionsChanged(List<MediaController> controllers) {
	        for (MediaController controller : controllers) {
	    		processBA.Log(controller.getPackageName());
			};
			
	    };
	};

	MediaSessionManager mMediaSessionManager = (MediaSessionManager) processBA.applicationContext.getSystemService(processBA.applicationContext.MEDIA_SESSION_SERVICE); 
	mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionsChangedListener, new ComponentName(processBA.applicationContext, notificationservice.class));
};

public void togglePause(String packageName){
	long eventtime = SystemClock.uptimeMillis(); 
	Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null); 
	KeyEvent downEvent = new KeyEvent(eventtime, eventtime, 
	KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0); 
	downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent); 
	downIntent.setPackage(packageName);
	processBA.applicationContext.sendOrderedBroadcast(downIntent, null); 

	Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null); 
	KeyEvent upEvent = new KeyEvent(eventtime, eventtime, 
	KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0); 
	upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent); 
	upIntent.setPackage(packageName);
	processBA.applicationContext.sendOrderedBroadcast(upIntent, null);
	
};

public String getForegroundPackageName(){
	String currentApp = "";
	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) processBA.applicationContext.getSystemService("usagestats");
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
														   time - 1000 * 1000, time);
                    if (appList != null && appList.size() > 0) {
                        SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                        for (UsageStats usageStats : appList) {
                            mySortedMap.put(usageStats.getLastTimeUsed(),
                                      		usageStats);
                        }
                        if (mySortedMap != null && !mySortedMap.isEmpty()) {
                            currentApp = mySortedMap.get(
                                    mySortedMap.lastKey()).getPackageName();
                        }
                    }
        } else {
                ActivityManager am = (ActivityManager) getBaseContext().getSystemService(ACTIVITY_SERVICE);
                currentApp = am.getRunningTasks(1).get(0).topActivity .getPackageName(); 

        };
	
	return currentApp;
};

public void StartService(String application, String action){

};

}
