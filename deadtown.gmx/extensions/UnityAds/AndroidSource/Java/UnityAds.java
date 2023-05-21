package ${YYAndroidPackageName};

import com.unity3d.ads.android.IUnityAdsListener;

import com.yoyogames.runner.RunnerJNILib;

public class UnityAds implements IUnityAdsListener {
	
	private static final int EVENT_OTHER_SOCIAL = 70;

	public double unity_ads_init(final String game_id) {
		RunnerActivity.ViewHandler.post(new Runnable() {
			public void run() {
				com.unity3d.ads.android.UnityAds.init(RunnerActivity.CurrentActivity, game_id, (IUnityAdsListener) UnityAds.this);
			}
		});
		return -1;
	}

	public double unity_ads_set_test_mode(final double is_test_mode) {
		RunnerActivity.ViewHandler.post(new Runnable() {
			public void run() {
				if (is_test_mode == 1) {
					com.unity3d.ads.android.UnityAds.setTestMode(true);
				} else {
					com.unity3d.ads.android.UnityAds.setTestMode(false);
				}
			}
		});
		return -1;
	}

	public double unity_ads_check_is_can_show() {
		if (com.unity3d.ads.android.UnityAds.canShow() == true) {
			return 1;
		} else {
			return 0;
		}
	}

	public double unity_ads_show() {
		RunnerActivity.ViewHandler.post(new Runnable() {
			public void run() {
				if (com.unity3d.ads.android.UnityAds.canShow() == true) {
					com.unity3d.ads.android.UnityAds.setZone("rewardedVideo");
					com.unity3d.ads.android.UnityAds.show();
				}
			}
		});
		return -1;
	}

	@Override
	public void onVideoCompleted(String itemKey, boolean skipped) {
		int dsMapIndex = RunnerJNILib.jCreateDsMap(null, null, null);
		if (skipped != true) {
			RunnerJNILib.DsMapAddString(dsMapIndex, "type", "unity_ads_video_completed");
		}
		else{
			RunnerJNILib.DsMapAddString(dsMapIndex, "type", "unity_ads_video_skipped");
		}
		RunnerJNILib.CreateAsynEventWithDSMap(dsMapIndex, EVENT_OTHER_SOCIAL);
	}

	@Override
	public void onHide() {}

	@Override
	public void onShow() {}

	@Override
	public void onVideoStarted() {}

	@Override
	public void onFetchCompleted() {}

	@Override
	public void onFetchFailed() {}
}
