#import <UnityAds/UnityAds.h>
#import "UnityAdsExt.h"

@implementation UnityAdsExt

const int EVENT_OTHER_SOCIAL = 70;
extern int CreateDsMap( int _num, ... );
extern void CreateAsynEventWithDSMap(int dsmapindex, int event_index);

- (double) unity_ads_init:(char *)game_id
{
    UIViewController * activeController = [UIApplication sharedApplication].keyWindow.rootViewController;
    if ([activeController isKindOfClass:[UINavigationController class]]) {
        activeController = [(UINavigationController *) activeController visibleViewController];
    }

    // Initialize Unity Ads
    [[UnityAds sharedInstance] startWithGameId:[NSString stringWithUTF8String:game_id] andViewController:activeController];
    [[UnityAds sharedInstance] setDelegate:self];

    return (double)-1;
}

- (double) unity_ads_set_test_mode:(double)is_test_mode
{
    if (is_test_mode == 1) {
        [[UnityAds sharedInstance] setTestMode:YES];
    } else {
        [[UnityAds sharedInstance] setTestMode:NO];
    }
    return (double)-1;
}

- (double) unity_ads_check_is_can_show
{
    if ([[UnityAds sharedInstance] canShow]) {
        return (double)1;
    } else {
        return (double)0;
    }
    return (double)-1;
}

- (double) unity_ads_show
{
    // Use the canShow method to check for zone readiness,
    //  then use the canShowAds method to check for ad readiness.
    if ([[UnityAds sharedInstance] canShow])
    {
        [[UnityAds sharedInstance] setZone:@"rewardedVideo"];
        // If both are ready, show the ad.
        [[UnityAds sharedInstance] show];
    }

    return (double)-1;
}

- (void) unityAdsVideoCompleted:(NSString *)rewardItemKey skipped:(BOOL)skipped
{
    if (skipped) {
        CreateAsynEventWithDSMap(CreateDsMap(1,
            "type", 0.0, "unity_ads_video_skipped", (void *)NULL
        ), EVENT_OTHER_SOCIAL);
    } else {
        CreateAsynEventWithDSMap(CreateDsMap(1,
            "type", 0.0, "unity_ads_video_completed", (void *)NULL
        ), EVENT_OTHER_SOCIAL);
    }
}

@end
