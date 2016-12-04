Type=Service
Version=5.02
ModulesStructureVersion=1
B4A=true
@EndOfDesignText@
#Region  Service Attributes 
	#StartAtBoot: False
#End Region

Sub Process_Globals
	Dim NotificationListener As NotificationListener
	'Dim MusicPlayChecker As Timer
	'These global variables will be declared once when the application starts.
	'These variables can be accessed from all modules.

End Sub


Sub Service_Create
	NotificationListener.Initialize("NotificationListener")
End Sub

Sub Service_Start (StartingIntent As Intent)
	If NotificationListener.HandleIntent(StartingIntent) Then Return
End Sub

Sub NotificationListener_NotificationPosted(Notification As StatusBarNotification)

	If Notification.IsInitialized = False Then
		Return
	End If

	If ControllingService.ServiceRunning = False Then Return
	If ControllingService.PowerOn = False Then Return
		
	Select(Notification.PackageName)

	Case "au.com.shiftyjelly.pocketcasts"
		Log("Podcasts playing")
		ControllingService.PreferenceManager.SetBoolean("PlayPodcast", True)
		ControllingService.PreferenceManager.SetBoolean("PlayMusic", False)
		ControllingService.PreferenceManager.SetBoolean("PlayRadio", False)
		
	Case "com.apple.android.music" 'Apple music notification appeared
		If ControllingService.AudioFocusListener.isMusicActive() Then
			Log("Apple music playing")
			ControllingService.PreferenceManager.SetBoolean("PlayMusic", True)
		Else
			Log("Apple music stopped")
			ControllingService.PreferenceManager.SetBoolean("PlayMusic", False)
		End If
	
	Case "com.google.android.apps.maps"
		If ControllingService.PreferenceManager.GetBoolean("DrivingMode") = False Then
			Log("Driving Mode started")
			ControllingService.PreferenceManager.SetBoolean("DrivingMode", True)
		End If
	
	End Select
End Sub

Sub Wait(Seconds As Int)
   Dim Ti As Long
   Ti = DateTime.Now + (Seconds * 1000)
   Do While DateTime.Now < Ti
   DoEvents
   Loop
End Sub

Sub NotificationListener_NotificationRemoved(Notification As StatusBarNotification)
	If Notification.IsInitialized = False Then
		Return
	End If
	
	If ControllingService.PowerOn = False Or ControllingService.UsbManager.GetDevices().Length = 0 Then
		Return
	End If
	
	Select(Notification.PackageName)
		
	Case "au.com.shiftyjelly.pocketcasts"
		Log("Podcasts stopped")
		ControllingService.PreferenceManager.SetBoolean("PlayPodcast", False)
	
	Case "com.apple.android.music" 'Apple music notification disappeared
		If ControllingService.AudioFocusListener.isMusicActive() = False Then
			Log("Apple music stopped")
			ControllingService.PreferenceManager.SetBoolean("PlayMusic", False)
		End If
		
	Case "com.google.android.apps.maps"
		Log("Driving Mode stopped")
		ControllingService.PreferenceManager.SetBoolean("DrivingMode", False)
		
	End Select
	
	
End Sub

Sub Service_Destroy

End Sub
