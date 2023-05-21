#import <UnityAds/UnityAds.h>

@interface UnityAdsExt : NSObject <UnityAdsDelegate>
{
}

- (double) unity_ads_init:(char *)game_id;
- (double) unity_ads_set_test_mode:(double)is_test_mode;
- (double) unity_ads_check_is_can_show;
- (double) unity_ads_show;
- (void) unityAdsVideoCompleted:(NSString *)rewardItemKey skipped:(BOOL)skipped;

@end
