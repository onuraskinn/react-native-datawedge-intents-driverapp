import { useEffect } from 'react';
import { NativeEventEmitter, StyleSheet, Text, View } from 'react-native';
import { ScannerInit, ScannerReceiver } from './libs/scanner';

const eventEmitter = new NativeEventEmitter();
let isSuccessScan = false;

export default function App() {
  useEffect(() => {
    isSuccessScan = false;
    ScannerInit();
    const subscription = eventEmitter.addListener(
      'datawedge_broadcast_intent',
      _broadcastReceiverHandler
    );
    return () => {
      subscription.remove();
    };
  }, []);

  const _broadcastReceiverHandler = (intent: any) => {
    const result = ScannerReceiver(intent);
    if (result && !isSuccessScan) {
      isSuccessScan = true;
      console.log('result', result);
      setTimeout(() => {
        isSuccessScan = false;
      }, 500);
    }
  };

  return (
    <View style={styles.container}>
      <Text>App</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'red',
  },
});
