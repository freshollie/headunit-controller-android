Type=Service
Version=5.02
ModulesStructureVersion=1
B4A=true
@EndOfDesignText@
#Region  Service Attributes 
	#StartAtBoot: True
#End Region

Sub Process_Globals
	Dim BroadcastReceiver As BroadCastReceiver
	Dim mediaplay As JavaObject
	Dim AudioFocusListener As AudioFocus
	Dim ServiceRunning As Boolean
	Dim PreferenceManager As PreferenceManager
	Dim PackageManager As PackageManager
	Dim UsbManager As UsbManager
	Dim NumVariables As Int = 8
	Dim PowerOn As Boolean
	Dim MediaPlayer As MediaPlayer
	Dim Su As SuCommand
	Dim Notification As Notification
	
	'These global variables will be declared once when the application starts.
	'These variables can be accessed from all modules.

End Sub

Sub Wait(Seconds As Int)
   Dim Ti As Long
   Ti = DateTime.Now + (Seconds * 1000)
   Do While DateTime.Now < Ti
   DoEvents
   Loop
End Sub

Sub SetUpVariables
	PreferenceManager.SetBoolean("OpenRadio", False)
	PreferenceManager.SetBoolean("PlayMusic", False)
	PreferenceManager.SetBoolean("PlayPodcast", False)
	PreferenceManager.SetBoolean("PlayRadio", False)
	PreferenceManager.SetBoolean("LaunchMaps", False)
	PreferenceManager.SetBoolean("DrivingMode", False)
	PreferenceManager.SetBoolean("DebugMode", False)
	PreferenceManager.SetString("NumDevices", "3")
End Sub

Sub Service_Create
	PowerOn = True
	mediaplay.InitializeContext	
	MediaPlayer.Initialize
	ServiceRunning = False
	If PreferenceManager.GetAll.Size < NumVariables Then
		SetUpVariables
	End If
	UsbManager.Initialize
	AudioFocusListener.Initialize("AudioFocusListener")
	BroadcastReceiver.Initialize("BroadCastReceiver")
	
	
	BroadcastReceiver.addAction("android.intent.action.ACTION_POWER_CONNECTED")
	BroadcastReceiver.addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
    BroadcastReceiver.addAction("com.android.music.metachanged")
    BroadcastReceiver.addAction("com.android.music.playstatechanged")
    BroadcastReceiver.addAction("com.android.music.playbackcomplete")
    BroadcastReceiver.addAction("com.android.music.queuechanged")
	BroadcastReceiver.addAction("com.freshollie.radioapp.RUNNING")	
	BroadcastReceiver.addAction("com.freshollie.radioapp.STOPPED")
	BroadcastReceiver.registerReceiver("")

End Sub

Sub Service_Start (StartingIntent As Intent)
	ServiceRunning = True
	Notification.Initialize
	Notification.Sound = False
	Notification.Vibrate = False
	'Notification.
	Notification.Icon = "ic_tablet_black_48dp"
	Notification.SetInfo("Tablet Controller is running", "TEST", Main) 
	Notification.OnGoingEvent = True
	'Notification.Notify(1)
	Service.StartForeground(1, Notification)
	StartRoutine
	
End Sub

Sub StartApps

	If PreferenceManager.GetBoolean("DrivingMode") = True And PreferenceManager.GetBoolean("LaunchMaps") = True Then
		Log("Staring driving mode")
		Dim in As Intent
		in.Initialize(in.ACTION_VIEW, "google.navigation:/?free=1&mode=d&entry=fnls")
        If in.IsInitialized Then
           in.SetComponent("com.google.android.apps.maps/com.google.android.maps.MapsActivity")
           StartActivity(in)
       	End If
	End If
	
	If PreferenceManager.GetBoolean("PlayPodcast") Then
		Log("Playing Podcast")
		Wait(4)
		If PowerOn Then PlayPodcasts
		
	Else If PreferenceManager.GetBoolean("PlayRadio") Then
		Log("Playing radio")
		Dim loopTime As Int = DateTime.Now
		
		Do While UsbManager.GetDevices().Length < 3 And PreferenceManager.GetBoolean("DebugMode") = False And (DateTime.Now - loopTime < 10)
			DoEvents
		Loop
		Su.SuCommand("am startservice -a android.intent.action.MAIN -n com.freshollie.radioapp/.radioservice")
		

	Else If PreferenceManager.GetBoolean("PlayMusic") Then
		Log("Playing music")
		Wait(1)
		If PowerOn Then PlayMusic
	Else
		Wait(4)
	End If
End Sub


Sub StartTwilight
	Su.SuCommand("am startservice -a android.intent.action.MAIN -n com.autobright.kevinforeman.autobright/.AutoBrightService")
	Su.SuCommand("am start -a android.intent.action.MAIN -n com.autobright.kevinforeman.autobright/.AutoBright")
End Sub

Sub StartGPS
	Su.SuCommand("am startservice -a org.broeuschmeul.android.gps.usb.provider.nmea.intent.action.START_GPS_PROVIDER")
	Log("Starting gps")
End Sub

Sub StartShuttleXpress
	Su.SuCommand("am startservice -a android.intent.action.MAIN -n com.freshollie.shuttlexpress/.shuttlexpressservice")
	Log("Starting ShuttleXpressDriver")
End Sub

Sub StartRoutine
	MediaPlayer.Load(File.DirAssets, "5sec.mp3")
	MediaPlayer.Play  'Remove noise from audio lines
	MediaPlayer.Looping = True
	Log("Starting empty noise")
	
	AudioFocusListener.Volume = 13
	StartTwilight
	Log("Set volume")
	
	If PowerOn Then
		StartApps
	Else
		Wait(4)
	End If
	
	Log("Started apps")
	
	Dim numDevices = PreferenceManager.GetString("NumDevices") As Int
	
	Dim loopTime As Int = DateTime.Now
	
	Do While UsbManager.GetDevices().Length < numDevices And PreferenceManager.GetBoolean("DebugMode") = False And (DateTime.Now - loopTime < 10)
		DoEvents
	Loop
	If PowerOn Then
		StartShuttleXpress
		StartGPS
	End If
End Sub

Sub StopRoutine
	MediaPlayer.Stop
	
	Dim thread1 As Thread
	thread1.Initialise("KillMaps")
	thread1.Start("KillMaps", Null)
	
End Sub


Sub BroadcastReceiver_onReceive(Action As String, i As Object)
	If I <> Null Then
		Dim intent As Intent = i
	End If
	
	Log(Action)
	Select(Action)
	
	Case "android.intent.action.ACTION_POWER_CONNECTED"
		Log("Power connected")
		PowerOn = True
		StartRoutine
		
	Case "android.intent.action.ACTION_POWER_DISCONNECTED"
		Log("Power disconnected")
		Log(mediaplay.RunMethod("getForegroundPackageName", Null))
		If mediaplay.RunMethod("getForegroundPackageName", Null) = "com.google.android.apps.maps" Then
			Log("Maps last application on top")
			PreferenceManager.SetBoolean("LaunchMaps", True)
		Else
			PreferenceManager.SetBoolean("LaunchMaps", False)
		End If
		
		PowerOn = False
		StopRoutine
		
	Case "com.apple.music.client.player.play"
		Log("Wat")
	
	
	End Select
	
	If UsbManager.GetDevices().Length < 3 Then Return
	
	If PowerOn = False Then Return 'Only check for these while power is connected
	
	Select(Action)
	
	Case "com.freshollie.radioapp.RUNNING"
		Log("Radio Player started")
		PreferenceManager.SetBoolean("PlayRadio", True)
		PreferenceManager.SetBoolean("PlayPodcast", False)
		PreferenceManager.SetBoolean("PlayMusic", False)
	
	Case "com.freshollie.radioapp.STOPPED"
		Log("Radio player stopped")
		PreferenceManager.SetBoolean("PlayRadio", False)
	
	End Select
		
	
End Sub


Sub PlayPodcasts
	Dim args(1) As Object
	args(0) = "au.com.shiftyjelly.pocketcasts"
	mediaplay.RunMethod("togglePause", args)
End Sub

Sub PlayMusic
	Dim args(1) As Object
	Su.SuCommand("am startservice -a 'com.apple.music.client.player.play_pause' -n com.apple.android.music/com.apple.android.svmediaplayer.player.MusicService")
End Sub

Sub Service_Destroy
	Service.StopForeground(0)
End Sub

Private Sub KillMaps
	Log("Killing maps")
	Su.SuCommand("am force-stop com.google.android.apps.maps")
End Sub

#if java
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

#end if
