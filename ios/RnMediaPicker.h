
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNRnMediaPickerSpec.h"

@interface RnMediaPicker : NSObject <NativeRnMediaPickerSpec>
#else
#import <React/RCTBridgeModule.h>

@interface RnMediaPicker : NSObject <RCTBridgeModule>
#endif

@end
