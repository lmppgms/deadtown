
package ${YYAndroidPackageName};

import ${YYAndroidPackageName}.RunnerActivity;
import com.yoyogames.runner.RunnerJNILib;

import android.util.Log;
import android.os.Handler;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Activity;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.app.DialogFragment;
import android.os.Bundle;
import android.content.Intent;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import android.os.Build;
import ${YYAndroidPackageName}.R;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.View;
import android.widget.AbsoluteLayout;
import com.yoyogames.runner.RunnerJNILib;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;
import java.util.Calendar;

import android.annotation.TargetApi;
import com.google.android.gms.*;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.plus.Plus;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.*;
import com.google.android.gms.appstate.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.content.IntentSender.SendIntentException;
//Can't extend BaseGameUtils as we already extend RunnerSocial

public class GooglePlayServicesExtension extends RunnerSocial implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener 
{
	private GoogleApiClient mGoogleApiClient;
	public GcmPush mGcmPush;	//Push notifications
	private int LastGSId=-1;
	public static GooglePlayServicesExtension CurrentGoogleExtension;

	private String mCurrentSaveName = "DefaultSave";
	private byte [] mSaveGameData = null;
	
	
	public void GooglePlayServices_Init()
	{
		//This will start all the login flow etc
		
		if(mGoogleApiClient!=null)
		{
			Log.i("yoyo","Attempting to initialise Google Play Services when they have already been initialised.");
			return;
		}
		
		String appid = RunnerActivity.CurrentActivity.mYYPrefs.getString("com.google.android.gms.games.APP_ID");
		
		if(appid == null|| appid.isEmpty())
		{
			Log.i("yoyo", "Failed to find appid, not initialising GoogleApiClient");
			return;
			
		}
		else
			Log.i("yoyo", "Found google play app id = " + appid);
		
		
		GoogleApiClient.Builder builder = new GoogleApiClient.Builder(RunnerActivity.CurrentActivity);
		
        builder.addConnectionCallbacks(this);
		builder.addOnConnectionFailedListener(this);
		builder.addApi(Games.API);
		builder.addScope(Games.SCOPE_GAMES);
		builder.addApi(Plus.API);
		builder.addScope(Plus.SCOPE_PLUS_LOGIN);
		
		boolean bCloudEnable = RunnerActivity.CurrentActivity.mYYPrefs.getBoolean("YYGoogleCloudSavingEnabled");
		if( bCloudEnable )
		{
			//These are needed if you are using the cloud saving functionality, if you have them in & do not have save games enabled in your google developer console for your game, you won't be able to log
			// in to google play services...
			Log.i("yoyo","Adding Google Drive API for cloud saving");
			builder.addApi(Drive.API);
			builder.addScope(Drive.SCOPE_APPFOLDER);
		}
		
		
		mGoogleApiClient = builder.build();
		
	}
	
	@Override
	public void Init()
	{
		CurrentGoogleExtension = this;

		//If they aren't available don't trigger the prompt for install flow as this may be a fire device etc - leave that up to the dev to decide if\when to do this.
		if(GooglePlayServices_Status()==ConnectionResult.SUCCESS)
		{
			Log.i("yoyo", "Google Play Services extension initialising" );
	
			GooglePlayServices_Init();
		}
		else
		{
			Log.i("yoyo", "Google Play Services extension not initialising as AreGooglePlayServicesAvailable returns false" );
		}
	
	}
	
	
	@Override
	public void onStart() 
	{
		Log.i("yoyo","googleplayservices extension onStart called");
		//super.onStart();
		if (mGoogleApiClient != null) 
			mGoogleApiClient.connect();
	}

	@Override
	public void onStop() 
	{
		Log.i("yoyo","googleplayservices extension onStop called");
		//super.onStop();
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) 
			mGoogleApiClient.disconnect();
	}
	
	@Override
	public void onResume()
	{
		if(mGcmPush !=null )
		{
			mGcmPush.deliverStoredMessages();
		}
	}
	
	
	@Override
	public void onConnected(Bundle connectionHint) 
	{
		// The player is signed in. Hide the sign-in button and allow the
		// player to proceed.
		Log.i("yoyo","Sign In Succeeded");
		
		
		Player p = Games.Players.getCurrentPlayer(getApiClient());
        String displayName;
        String id;
        if (p == null) {
            Log.i("yoyo", "mGamesClient.getCurrentPlayer() is NULL!");
            displayName = "???";
            id="-1";
        }
        else {
            displayName = p.getDisplayName();
            id = p.getPlayerId();
        }
        
        Log.i("yoyo","Found displayname " + displayName + " with id " + id);
		
		RunnerJNILib.OnLoginSuccess(displayName,id,"","","","","");
		
		//Call back to generate the social event 
	}
	
 // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

	


	public double GooglePlayServices_Status()
	{
		return (double)GooglePlayServicesUtil.isGooglePlayServicesAvailable(RunnerJNILib.ms_context);
	}
  

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode,int requestCode) 
    {    
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode,RunnerActivity.CurrentActivity, requestCode);
        if (dialog != null) 
        {
            dialog.show();
        }
        else 
        {
            // no built-in dialog: show the fallback error message
        //    showAlert(RunnerActivity.CurrentActivity, );
			Log.i("yoyo","Google Play Services Error and unable to initialise GooglePlayServicesUtil error dialog");
        }
    }



@Override
    public void onConnectionFailed(ConnectionResult result) {
		
		Log.i("yoyo","onConnectionFailed called with result:"+result);
		
        if (mResolvingError) {
            // Already attempting to resolve an error.
			Log.i("yoyo","onConnectionFailed called with resolving error already set to "+ mResolvingError);
            return;
        } else if (result.hasResolution()) {
            try {
				Log.i("yoyo","startResolutionForResult completed");
                mResolvingError = true;
                result.startResolutionForResult(RunnerActivity.CurrentActivity, REQUEST_RESOLVE_ERROR);
				Log.i("yoyo","startResolutionForResult completed");
            } catch (SendIntentException e) {
                // There was an error with the resolution intent. Try again.
				Log.i("yoyo","SendIntentException detected");
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
			Log.i("yoyo","Attempting to showErrorDialog");
            showErrorDialog(result.getErrorCode(),RC_UNUSED);
            Log.i("yoyo","About to call onloginsuccess");
			RunnerJNILib.OnLoginSuccess("Not logged in","-1","","","","","");
			Log.i("yoyo","Post call onloginsuccess");
            mResolvingError = true;
        }
    }


	
	@Override
	public void onConnectionSuspended(int i) 
	{
		// Attempt to reconnect
		
		Log.i("yoyo","onConnectionSuspended call with " + i);
		
		if (mGoogleApiClient != null) 
			mGoogleApiClient.connect();
		
		
	}
	
	public void SendSocialEvent(String type,double value)
	{
	
		int dsMapIndex = RunnerJNILib.jCreateDsMap(null, null, null);
		
		RunnerJNILib.DsMapAddString( dsMapIndex, "type", type );
		RunnerJNILib.DsMapAddDouble( dsMapIndex, "value", value);
		RunnerJNILib.DsMapAddDouble( dsMapIndex, "id", 9817);
		RunnerJNILib.CreateAsynEventWithDSMap(dsMapIndex,EVENT_OTHER_SOCIAL);
	}
	
	@Override
	public	void onActivityResult(int requestCode, int resultCode,Intent intent) 
	{
		Log.i("yoyo","gps onActivityResult called with requestCode "+requestCode );
		
	//	if
		
		 switch (requestCode) 
		 {
			case RC_GPS_ACTIVITY:
			if(resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED)
			{
				Log.i("yoyo","Logging out due to result of GooglePlayServices activity");
	//			Logout();
				if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) 
				{
					mGoogleApiClient.disconnect();
					Log.i("yoyo","Logged out of Google Play Services");
					RunnerJNILib.OnLoginSuccess("Not logged in","-1","","","","","");
				}
			}
			break;
			
            case REQUEST_RESOLVE_ERROR:
				Log.i("yoyo","onActivityResult called with REQUEST_RESOLVE_ERROR" );
                if (resultCode == Activity.RESULT_OK) 
				{
					Log.i("yoyo","onActivityResult called with REQUEST_RESOLVE_ERROR and resultCode (result_ok)" + resultCode);
			
					if (mGoogleApiClient != null) 
                    if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) 
					{
						mResolvingError = false;
						mGoogleApiClient.connect();
						 
                    }
                }
				else
				{
					Log.i("yoyo","onActivityResult called with REQUEST_RESOLVE_ERROR and resultCode " + resultCode);
					mResolvingError = false;
					
					switch(resultCode)
					{
						case GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED:
							Log.i("yoyo","gps OnActivityResult with: GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED");
							break;
						case GamesActivityResultCodes.RESULT_NETWORK_FAILURE:
							Log.i("yoyo","gps OnActivityResult with: GamesActivityResultCodes.RESULT_NETWORK_FAILURE");
							RunnerJNILib.OnLoginSuccess("Not logged in","-1","","","","","");
							break;
						case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED:
							Log.i("yoyo","gps OnActivityResult with: GamesActivityResultCodes.RESULT_SIGN_IN_FAILED");
							RunnerJNILib.OnLoginSuccess("Not logged in","-1","","","","","");
						break;
						case GamesActivityResultCodes.RESULT_LICENSE_FAILED:
							Log.i("yoyo","gps OnActivityResult with: GamesActivityResultCodes.RESULT_LICENSE_FAILED");
							RunnerJNILib.OnLoginSuccess("Not logged in","-1","","","","","");
						break;

						case GamesActivityResultCodes.RESULT_APP_MISCONFIGURED:
							Log.i("yoyo","gps OnActivityResult with: GamesActivityResultCodes.RESULT_APP_MISCONFIGURED");
							RunnerJNILib.OnLoginSuccess("Not logged in","-1","","","","","");
						break;						
							
						case GamesActivityResultCodes.RESULT_INVALID_ROOM:
							Log.i("yoyo","gps OnActivityResult with: GamesActivityResultCodes.RESULT_INVALID_ROOM");
						break;	
						case GamesActivityResultCodes.RESULT_LEFT_ROOM:
							Log.i("yoyo","gps OnActivityResult with: GamesActivityResultCodes.RESULT_LEFT_ROOM");
						break;	
						case GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED:
							Log.i("yoyo","gps OnActivityResult with: GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED");
						break;	
						
					}
					
					SendSocialEvent("GPSGamesActivityResult",(double)resultCode);
					
					//going to stop putting up the gps error as it's not at all helpful
					 //showErrorDialog(resultCode,requestCode);
					//We have an error and haven't been able to resolve it...
				}
                break;
            default:
				
				Log.i("yoyo","onActivityResult called with " + requestCode);
			
				mResolvingError = false; 
                break;
        }
	}
	
	RunnerBilling mRunnerBilling = null;
	
	public RunnerBilling InitRunnerBilling()
	{
		if(mRunnerBilling==null)
		{
			mRunnerBilling = new RunnerBilling(RunnerActivity.CurrentActivity);
		}
		
		return mRunnerBilling;
	}
	
	
	public GoogleApiClient getApiClient()
	{
		return mGoogleApiClient;
	}
	
	
	public boolean isSignedIn()
	{
	//	Log.i("yoyo","isSignedIn called" );
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) 
			return true;
		
		return false;
	}
	
	@Override
	public void Login()
	{
		if(isSignedIn())
		{
			Log.i("yoyo","Called achievement login when already logged in");
			return;
		}
		Log.i("yoyo","Calling connect on googleapiclient");
		if (mGoogleApiClient != null) 
			mGoogleApiClient.connect();
	}
	
	
	@Override
	public void Logout()
	{
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) 
		{
			Games.signOut(mGoogleApiClient);
			mGoogleApiClient.disconnect();
			Log.i("yoyo","Logged out of Google Play Services");
			RunnerJNILib.OnLoginSuccess("Not logged in","-1","","","","","");
		}
	}
	

	
	//Push notifications bit
	public void setupPushNotifications(String pushID)
	{
		
		mGcmPush = new GcmPush( RunnerActivity.CurrentActivity );
		mGcmPush.Init( pushID );
		//check for stored messages on startup- also done in onResume ( in case of messages received while in background),
		//but this is called after onResume on startup
		mGcmPush.deliverStoredMessages();

	}
	
	public static void QueuePushNotification( String msgData, Integer type, Boolean bSuccess )
	{
		if( CurrentGoogleExtension.mGcmPush != null ) 
		{
			RunnerJNILib.GCMPushResult(msgData, type, true );
		}
	}
	
	public int PushGetLocalNotification(Integer iIndex, Integer dsMap)
	{
		if( mGcmPush != null )
		{
			return mGcmPush.pushGetLocalNotification( iIndex, dsMap);
		}
		
		return -1;
	}
	public int pushCancelLocalNotification(Integer iIndex)
	{
		
		if( mGcmPush != null )
		{
			return mGcmPush.pushCancelLocalNotification( iIndex);
		}
		
		return -1;
		
	}
	
	
	public static void PushLocalNotification( Float fireTime, String title, String message, String data )
	{
		if( CurrentGoogleExtension.mGcmPush != null )
		{
			CurrentGoogleExtension.mGcmPush.pushLocalNotification( fireTime, title, message, data );
		}
		else 
		{
			Log.i("yoyo", "Push notifications not enabled");
		}
	}
	
	
	
	/// Cloud save stuff
	public void onStateConflict( AppStateManager.StateConflictResult result )
	{
		int StateKey = result.getStateKey();
		String resolvedVersion = result.getResolvedVersion();
		byte[] localData = result.getLocalData();
		byte[] serverData = result.getServerData();
		
		Log.i("yoyo","onStateConflict called " + StateKey+ " " + resolvedVersion);
		
	//	getGameHelper().getAppStateClient().resolveState(this, StateKey, resolvedVersion, serverData);
	   AppStateManager.resolve(getApiClient(), StateKey,     resolvedVersion, serverData);
		
		if(serverData!=null)
		{
			byte[] mySaveGameByteArray = new byte[ serverData.length + 1];	//Need to put in a null terminator for desc string....
			mySaveGameByteArray[0] = '\0';
			System.arraycopy(serverData, 0, mySaveGameByteArray, 1, serverData.length);
		
			RunnerJNILib.CloudResultData( mySaveGameByteArray, 0/*STATUS_NEW_GAME_DATA*/, LastGSId );
			mySaveGameByteArray = null;
		}
		else
		{
			RunnerJNILib.CloudResultData( null, 0/*STATUS_NEW_GAME_DATA*/, LastGSId );
		}
	}

	//@Override
	//public void onStateLoaded(int StatusCode, int StateKey, byte[] localData)
	public void onStateLoaded(AppStateManager.StateLoadedResult result )
	{
		int StatusCode = result.getStatus().getStatusCode();
		int StateKey = result.getStateKey();
		byte[] localData = result.getLocalData();
		
		Log.i("yoyo","onStateLoadedCalled with " + StatusCode + " Key " + StateKey);
		if(localData!=null)
		{
			byte[] mySaveGameByteArray = new byte[ localData.length + 1];	//Need to put in a null terminator for desc string....
			mySaveGameByteArray[0] = '\0';
			System.arraycopy(localData, 0, mySaveGameByteArray, 1, localData.length);
		
			RunnerJNILib.CloudResultData( mySaveGameByteArray, 0/*STATUS_NEW_GAME_DATA*/, LastGSId );
			mySaveGameByteArray = null;
		}
		else
			RunnerJNILib.CloudResultData( null, 0/*STATUS_NEW_GAME_DATA*/, LastGSId );
	}
	
	ResultCallback<AppStateManager.StateResult> mResultCallback = new ResultCallback<AppStateManager.StateResult>() 
    {
		@Override
		public void onResult(AppStateManager.StateResult result) {
			
			Log.i("yoyo","Cloud Sync result callback called with " + result + " status:" + result.getStatus());
			
			
			AppStateManager.StateConflictResult conflictResult = result.getConflictResult();
			AppStateManager.StateLoadedResult loadedResult = result.getLoadedResult();
			if (loadedResult != null) {
				Log.i("yoyo", "Calling onStateLoaded with " + loadedResult);
				onStateLoaded(loadedResult);
			} else if (conflictResult != null) {
				Log.i("yoyo","Calling onStateConflict with " + conflictResult);
				onStateConflict(conflictResult);
			}
			else
			{
				Log.i("yoyo","both conflict and loaded results are null");
			}
		}
	};
	
	
	private static final int MAX_SNAPSHOT_RESOLVE_RETRIES = 3;
	
    /**
     * Conflict resolution for when Snapshots are opened.  Must be run in an AsyncTask or in a
     * background thread,
     */
    Snapshot processSnapshotOpenResult(Snapshots.OpenSnapshotResult result, int retryCount) 
	{
        Snapshot mResolvedSnapshot = null;
        retryCount++;

        int status = result.getStatus().getStatusCode();
        Log.i("yoyo", "Save Result status: " + status);

        if (status == GamesStatusCodes.STATUS_OK) {
            return result.getSnapshot();
        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
            return result.getSnapshot();
        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
            Snapshot snapshot = result.getSnapshot();
            Snapshot conflictSnapshot = result.getConflictingSnapshot();

            // Resolve between conflicts by selecting the newest of the conflicting snapshots.
            mResolvedSnapshot = snapshot;

            if (snapshot.getMetadata().getLastModifiedTimestamp() <
                    conflictSnapshot.getMetadata().getLastModifiedTimestamp()) {
                mResolvedSnapshot = conflictSnapshot;
            }

            Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots.resolveConflict(
                    mGoogleApiClient, result.getConflictId(), mResolvedSnapshot).await();

            if (retryCount < MAX_SNAPSHOT_RESOLVE_RETRIES) {
                // Recursively attempt again
                return processSnapshotOpenResult(resolveResult, retryCount);
            } else {
                // Failed, log error and show Toast to the user
                String message = "Could not resolve snapshot conflicts";
                Log.e("yoyo", message);
          //      Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
            }

        }

        // Fail, return null.
        return null;
    }
	
	
	
	
	public void onGSCloudSync(java.lang.Integer id)
	{
		final Integer fid = id;
		Log.i("yoyo","onGSCloudSync called with id=" + id);
		
		AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                // Open the saved game using its name.
                Snapshots.OpenSnapshotResult result = Games.Snapshots.open(mGoogleApiClient,mCurrentSaveName, true).await();
						
				int status = result.getStatus().getStatusCode();
						
				Log.i("yoyo","get save game data result :"+ result.getStatus() + " status=" +status);	
                // Check the result of the open operation
                if ((status == GamesStatusCodes.STATUS_OK) ||(status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) ||( status==GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE )) 
				{
                    Snapshot snapshot = processSnapshotOpenResult(result,0);
					
					//result.getSnapshot();
                    // Read the byte content of the saved game.
                    try 
					{
                        mSaveGameData = snapshot.getSnapshotContents().readFully();

						String s = new String(mSaveGameData);
						Log.i("yoyo","got save game data:"+ s);

						RunnerJNILib.CloudResultData( s.getBytes(), 0/*STATUS_NEW_GAME_DATA*/, fid );
						return result.getStatus().getStatusCode();
                    }
					catch (IOException e) 
					{
                        Log.e("yoyo", "Error while reading Snapshot.", e);
                    }
                } 
				else
				{
                    Log.e("yoyo", "Error while loading: " + result.getStatus().getStatusCode());
                }
				RunnerJNILib.CloudResultData( null, 0/*STATUS_NEW_GAME_DATA*/, fid );
                return result.getStatus().getStatusCode();
            }

            @Override
            protected void onPostExecute(Integer status) {
                // Dismiss progress dialog and reflect the changes in the UI.
                // ...
				
				Log.i("yoyo","New cloud save system onPostExecute called with status" + status);
				
            }
        };

        task.execute();
		
		
		

	}
	
	
	public void onGSStringSave(String data,String desc,Integer id)
	{
			final String _data=data;
			final String _desc=desc;
			final Integer _id=id;
			
			Log.i("yoyo","Google onGSStringSave called");
			
			AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() 
			{
            @Override
            protected Integer doInBackground(Void... params) {
		
		
				boolean createIfMissing = true;
				byte[] databytes = _data.getBytes();
				
                // Open the snapshot, creating if necessary
                Snapshots.OpenSnapshotResult open = Games.Snapshots.open(
                        mGoogleApiClient, mCurrentSaveName, createIfMissing).await();

                if (!open.getStatus().isSuccess()) 
				{
                    Log.i("yoyo", "Could not open Snapshot for saving.");
                    return -1;
                }

                // Write the new data to the snapshot
                Snapshot snapshot = open.getSnapshot();
                snapshot.getSnapshotContents().writeBytes(databytes);
				
				String s = new String(databytes);
				Log.i("yoyo","saving snapshot:" + s);

                // Change metadata
                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                        .fromMetadata(snapshot.getMetadata())
						.setDescription("Modified data at: " + Calendar.getInstance().getTime())
                //        .setCoverImage(bitmap)
                //        .setDescription(description)
                //        .setPlayedTimeMillis(playedTimeMillis)
                        .build();

                Snapshots.CommitSnapshotResult commit = Games.Snapshots.commitAndClose(
                        mGoogleApiClient, snapshot, metadataChange).await();

                if (!commit.getStatus().isSuccess()) {
                    Log.i("yoyo", "Failed to commit Snapshot.");
                    return -1;
                }

				Log.i("yoyo", "Commit Snapshot success");
				return 0;
            }

            @Override
            protected void onPostExecute(Integer status) {
                // Dismiss progress dialog and reflect the changes in the UI.
                // ...
				
				Log.i("yoyo","onGSStringSaves completed with " + status);
				
            }
        };

        task.execute();
	}
	
	//Achievement & Leaderboards stuff
	
	@Override
	public void PostScore(String lb, int score)
	{
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) 
		{
			Games.Leaderboards.submitScore(getApiClient(),lb, score);
		}
	}
   
	public void onIncrementAchievement(String id,java.lang.Float Increment)
    {
		
		float increment = (float)Increment;
		
		if ( increment < 1 ) // as per: https://developer.android.com/reference/com/google/android/gms/games/achievement/Achievements.html#increment(com.google.android.gms.common.api.GoogleApiClient, java.lang.String, int)
			Log.i("yoyo","achievement_increment must be called with a positive value above 1");
		else
		//	Log.i("yoyo","awaiting fix");
			if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) 
			{
				Games.Achievements.increment(getApiClient(),id,(int)increment);
			}
		//	getApiClient().incrementAchievement(id,(int)increment);
    
    }
    
    public void onPostAchievement(String id,java.lang.Float percentdone)
    {
		
		if(percentdone<=0)
			Log.i("yoyo","achievement_post must be called with a positive value");
		else if(percentdone >=100.0)
		{
			if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) 
			{
				Games.Achievements.unlock(getApiClient(),id);
			}
			//getApiClient().unlockAchievement(id);
		}
		else
			Log.i("yoyo","GPS does not currently support posting partially complete achievements. Either call achievement_post() when the achievement is complete or use achievement_increment()");
    
    }
    
	  final int RC_RESOLVE = 5000, RC_UNUSED = 5001,RC_GPS_ACTIVITY = 5011;
    public void onShowGSAchievements()
    {
		if (isSignedIn()) {
            RunnerActivity.CurrentActivity.startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), RC_GPS_ACTIVITY);
        }
        else {
            (new AlertDialog.Builder(RunnerActivity.CurrentActivity))
               .setTitle("Achievement Not Available")
               .setMessage("You can't view achievements because you are not signed in.")
                  .setNeutralButton("OK", null)
                  .show();
        }
    }
    public void onShowGSLeaderboards()
    {
		if (isSignedIn()) {
			RunnerActivity.CurrentActivity.startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(getApiClient()), RC_GPS_ACTIVITY);
		}
		else {
			(new AlertDialog.Builder(RunnerActivity.CurrentActivity))
			   .setTitle("Leaderboards Not Available")
			   .setMessage("You can't view leaderboards because you are not signed in.")
				  .setNeutralButton("OK", null)
				  .show();
		}
	}
	private AdView adView=null;
	private InterstitialAd interstitialAd=null;
	//private String BannerId;
	private String InterstitialId;
	private String InterstitialStatus = "Not Ready";
	private String TestDeviceId;
	private boolean bUseTestAds=false;
	private AdSize BannerSize;
	private int BannerXPos;
	private int BannerYPos;
	
	private AdListener adlistener = new AdListener(){
		@Override
		 public void onAdLoaded() {
			Log.i("yoyo","onAdLoaded called");
			sendInterstitialLoadedEvent( true );
		 }

		@Override
		public void onAdFailedToLoad(int errorCode) {
			Log.i("yoyo","onAdFailedToLoad called");
			sendInterstitialLoadedEvent( false );
		}

	};
	
	
	private static final int EVENT_OTHER_SOCIAL = 70;
	
	public void GoogleMobileAds_Init(String _Arg1)
	{
	
		InterstitialId = _Arg1;	
	}
	
	public void GoogleMobileAds_ShowInterstitial()
	{
		if(interstitialAd!=null)
		{
	
			RunnerActivity.ViewHandler.post( new Runnable() {
    		public void run() 
    		{
				Log.i("yoyo","showinterstitial called");
				if (interstitialAd.isLoaded()) 
				{
					RunnerActivity.CurrentActivity.runOnUiThread(new Runnable() {
						public void run() {
							interstitialAd.show();
						}
					});
				} 
				else
				{
					Log.i("yoyo", "Interstitial ad was not ready to be shown.");
				}
    		}});
		}
    	
	}
	
	private void initInterstitial()
	{
	
		interstitialAd = new InterstitialAd(RunnerActivity.CurrentActivity);
		interstitialAd.setAdUnitId(InterstitialId);
			
		interstitialAd.setAdListener(adlistener);
	}
	
	public void GoogleMobileAds_LoadInterstitial()
	{
		RunnerActivity.ViewHandler.post( new Runnable() {
    	public void run() 
    	{
			if(interstitialAd==null)
				initInterstitial();
				
			AdRequest.Builder builder = new AdRequest.Builder();
			builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
			if( bUseTestAds)
				builder.addTestDevice(TestDeviceId);
			AdRequest adRequest = builder.build();

			// Load the interstitial ad.
			interstitialAd.loadAd(adRequest);
		}});
	}
	
	private void sendBannerLoadedEvent( boolean _bLoaded )
	{
		int dsMapIndex = RunnerJNILib.jCreateDsMap(null, null, null);
		
		RunnerJNILib.DsMapAddString( dsMapIndex, "type", "banner_load" );
		double loaded = (_bLoaded) ? 1 : 0;
		RunnerJNILib.DsMapAddDouble( dsMapIndex, "loaded", loaded);
		
		if( _bLoaded)
		{
			GoogleMobileAds_MoveBanner(BannerXPos, BannerYPos);
		
			RunnerJNILib.DsMapAddDouble( dsMapIndex, "width",  GoogleMobileAds_BannerGetWidth());
			RunnerJNILib.DsMapAddDouble( dsMapIndex, "height",  GoogleMobileAds_BannerGetHeight());
		}
		
		RunnerJNILib.CreateAsynEventWithDSMap(dsMapIndex,EVENT_OTHER_SOCIAL);
	}
	
	private void sendInterstitialLoadedEvent( boolean _bLoaded )
	{
		int dsMapIndex = RunnerJNILib.jCreateDsMap(null, null, null);
		RunnerJNILib.DsMapAddString( dsMapIndex, "type", "interstitial_load" );
		double loaded = (_bLoaded) ? 1 : 0;
		RunnerJNILib.DsMapAddDouble( dsMapIndex, "loaded", loaded);
		RunnerJNILib.CreateAsynEventWithDSMap(dsMapIndex,EVENT_OTHER_SOCIAL);
	}
	
	public void GoogleMobileAds_AddBanner(String _bannerId, double _sizeType)
	{
		GoogleMobileAds_AddBannerAt( _bannerId, _sizeType, 0, 0);
	}
	
	
	@TargetApi(11)
	void SetLayerType()
	{
		int sdkVersion =Build.VERSION.SDK_INT;
		if (sdkVersion > 10)
			adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}
	
	
	public void GoogleMobileAds_AddBannerAt(String _bannerId, double _sizeType, double _x, double _y)
	{
		final String bannerId = _bannerId;
		BannerXPos = (int)_x;
		BannerYPos = (int)_y;
		int type = (int)(_sizeType+0.5);
		
		switch(type)
		{
		case 1: BannerSize = AdSize.BANNER; break;
		case 2: BannerSize = AdSize.MEDIUM_RECTANGLE;break;
		case 3: BannerSize = AdSize.FULL_BANNER;break;
		case 4: BannerSize = AdSize.LEADERBOARD;break;
		case 5: BannerSize = AdSize.WIDE_SKYSCRAPER; break;
		default: Log.i("yoyo", "AddBanner illegal banner size type:" + _sizeType); return;
		}
		
		RunnerActivity.ViewHandler.post( new Runnable() {
    	public void run() 
    	{
    		AbsoluteLayout layout = (AbsoluteLayout)RunnerActivity.CurrentActivity.findViewById(R.id.ad);
			ViewGroup vg = (ViewGroup)layout;
			
			//remove existing banner
			if( adView != null )
			{
				if(vg!=null)
				{
					vg.removeView( adView );
				}
				adView.destroy();
				adView = null;
			}
			
    		//create new banner
			adView = new AdView(RunnerActivity.CurrentActivity);
			//adView.setAdListener(GoogleMobileAdsExt.this);
			adView.setAdListener( new AdListener() {
				/** Called when an ad is loaded. */
			    @Override
			    public void onAdLoaded() 
			    {
			    	Log.i("yoyo", "Banner Ad onAdLoaded");
			    	sendBannerLoadedEvent(true);
			    }
			    
			    @Override
			    public void onAdFailedToLoad(int errorCode)
			    {
			    	Log.i("yoyo", "Banner Ad onAdFailedToLoad");
			    	sendBannerLoadedEvent(false);
			    }
			    
			});    
		
			SetLayerType();
			
		
			//adView.setAdSize(AdSize.BANNER);	
			adView.setAdSize( BannerSize );
			adView.setAdUnitId(bannerId);
			
			if(vg != null)
			{
				vg.addView((View)adView);
			
				AdRequest.Builder builder = new AdRequest.Builder();
				builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
				if( bUseTestAds)
					builder.addTestDevice(TestDeviceId);
				AdRequest adRequest = builder.build();

				// Start loading the ad in the background.
				adView.setVisibility(View.INVISIBLE);
				adView.loadAd(adRequest);
			}
		}});
	}
	
	public void GoogleMobileAds_RemoveBanner()
	{
		if( adView != null )
		{
			RunnerActivity.ViewHandler.post( new Runnable() {
			public void run() 
		    {
				AbsoluteLayout layout = (AbsoluteLayout)RunnerActivity.CurrentActivity.findViewById(R.id.ad);
				ViewGroup vg = (ViewGroup)layout;
				if(vg!=null)
				{
					vg.removeView( adView );
				}
				adView.destroy();
				adView = null;
				
		    }});
		}
	}
	
	public double GoogleMobileAds_BannerGetWidth()
	{
		if( BannerSize !=null )
		{
			int w =BannerSize.getWidthInPixels(RunnerJNILib.ms_context); 
			return w;
		}
		return 0;
	}
	
	public double GoogleMobileAds_BannerGetHeight()
	{
		if( BannerSize !=null )
		{
			int h = BannerSize.getHeightInPixels(RunnerJNILib.ms_context);
			return h;
		}
		return 0;
	}
	
	public void GoogleMobileAds_MoveBanner( double _x, double _y )
	{
		Log.i("yoyo", "MoveBanner:" + _x + "," + _y);
		final int x = (int)_x;
		final int y = (int)_y;
		BannerXPos = x;
		BannerYPos = y;

		if( adView != null )
		{
			RunnerActivity.ViewHandler.post( new Runnable() {
			public void run()
			{
				if( x < 0 || y < 0) {
					adView.setVisibility(View.INVISIBLE);
				}
				else
				{
					AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, x,y );
					adView.setLayoutParams( params);
					adView.requestLayout();
					adView.setVisibility(View.VISIBLE);
				}
			}});
		}
	}
	
	public void GoogleMobileAds_UseTestAds( double _useTest, String _deviceId )
	{
		bUseTestAds = (_useTest >=0.5);
		TestDeviceId = _deviceId;
	}
	
	public String GoogleMobileAds_InterstitialStatus()
	{
		if(interstitialAd!=null)
		{
			RunnerActivity.ViewHandler.post( new Runnable() {
    		public void run() 
    		{
				if(interstitialAd.isLoaded())
					InterstitialStatus="Ready";
				else
					InterstitialStatus = "Not Ready";
			}});
		}

		return InterstitialStatus;
	}
	
	
}