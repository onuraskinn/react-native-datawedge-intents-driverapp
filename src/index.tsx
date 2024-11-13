import { NativeModules, Platform } from 'react-native';
import {
  ScannerInit,
  ScannerReceiver,
  type ProfileConfigType,
} from './scanner';

const LINKING_ERROR =
  `The package 'react-native-datawedge-intents' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const DatawedgeIntents = NativeModules.DatawedgeIntents
  ? NativeModules.DatawedgeIntents
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export { ScannerInit, ScannerReceiver, type ProfileConfigType };

export default {
  ACTION_SOFTSCANTRIGGER: DatawedgeIntents.ACTION_SOFTSCANTRIGGER,
  ACTION_SCANNERINPUTPLUGIN: DatawedgeIntents.ACTION_SCANNERINPUTPLUGIN,
  ACTION_ENUMERATESCANNERS: DatawedgeIntents.ACTION_ENUMERATESCANNERS,
  ACTION_SETDEFAULTPROFILE: DatawedgeIntents.ACTION_SETDEFAULTPROFILE,
  ACTION_RESETDEFAULTPROFILE: DatawedgeIntents.ACTION_RESETDEFAULTPROFILE,
  ACTION_SWITCHTOPROFILE: DatawedgeIntents.ACTION_SWITCHTOPROFILE,
  START_SCANNING: DatawedgeIntents.START_SCANNING,
  STOP_SCANNING: DatawedgeIntents.STOP_SCANNING,
  TOGGLE_SCANNING: DatawedgeIntents.TOGGLE_SCANNING,
  ENABLE_PLUGIN: DatawedgeIntents.ENABLE_PLUGIN,
  DISABLE_PLUGIN: DatawedgeIntents.DISABLE_PLUGIN,
};

export function sendIntent(action: string, parameterValue: any) {
  //  THIS METHOD IS DEPRECATED, use SendBroadcastWithExtras
  DatawedgeIntents.sendIntent(action, parameterValue);
}

export function sendBroadcastWithExtras(extrasObject: any) {
  DatawedgeIntents.sendBroadcastWithExtras(extrasObject);
}

export function registerBroadcastReceiver(filter: any) {
  DatawedgeIntents.registerBroadcastReceiver(filter);
}

export function registerReceiver(action: string, category: any) {
  //  THIS METHOD IS DEPRECATED, use registerBroadcastReceiver
  DatawedgeIntents.registerReceiver(action, category);
}
