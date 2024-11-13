import { useEffect, useState } from 'react';
import { NativeEventEmitter, StyleSheet, Text, View } from 'react-native';
import {
  ScannerInit,
  ScannerReceiver,
  type ProfileConfigType,
} from 'react-native-datawedge-intents';
import DeviceInfo from 'react-native-device-info';

const eventEmitter = new NativeEventEmitter();
let isSuccessScan = false;

export default function App() {
  const [result, setResult] = useState(null);
  const [isZebraDevice, setIsZebraDevice] = useState(false);

  useEffect(() => {
    async function _loadManufacture() {
      const device = await DeviceInfo.getManufacturer();
      if (device === 'Zebra Technologies' || device === 'Motorola Solutions') {
        setIsZebraDevice(true);
      } else {
        setIsZebraDevice(false);
      }
    }
    _loadManufacture();
  }, []);

  useEffect(() => {
    if (isZebraDevice) {
      isSuccessScan = false;
      const profileConfig: ProfileConfigType = {
        name: 'Example',
        package: 'datawedgeintents.example',
      };
      ScannerInit(profileConfig);
      const subscription = eventEmitter.addListener(
        'datawedge_broadcast_intent',
        _broadcastReceiverHandler
      );
      return () => {
        subscription.remove();
      };
    }
    return () => {};
  }, [isZebraDevice]);

  const _broadcastReceiverHandler = (intent: any) => {
    const objResult = ScannerReceiver(intent);
    if (objResult && !isSuccessScan) {
      isSuccessScan = true;
      setResult(objResult.data);
      setTimeout(() => {
        isSuccessScan = false;
      }, 500);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.textInfo}>
        Press Scan on Zebra Scanner, and see result below:
      </Text>
      <Text style={styles.textResult}>{JSON.stringify(result)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'cyan',
    padding: 16,
  },
  textInfo: {
    fontSize: 16,
    lineHeight: 24,
  },
  textResult: {
    color: 'red',
  },
});
