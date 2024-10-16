import { NativeModules, Platform } from 'react-native';
import type { IOptions } from './type';

const LINKING_ERROR =
  `The package 'react-native-media-picker' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RnMediaPicker = NativeModules.RnMediaPicker
  ? NativeModules.RnMediaPicker
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function multiply(a: number, b: number): Promise<number> {
  return RnMediaPicker.multiply(a, b);
}

export function launchLibrary(options: IOptions): Promise<string> {
  return RnMediaPicker.launchLibrary(options);
}
