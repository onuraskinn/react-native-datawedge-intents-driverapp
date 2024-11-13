# react-native-datawedge-intents

React Native Android module to interface with Zebra's DataWedge using Android Intents to control the barcode scanner and retrieve scanned data

## Installation

```sh
npm install @edritech93/react-native-datawedge-intents
```

## Usage

init scanner and listen

```ts
import {
  ScannerInit,
  ScannerReceiver,
} from '@edritech93/react-native-datawedge-intents';

// ...

const eventEmitter = new NativeEventEmitter();

// ...

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

// ...

const _broadcastReceiverHandler = (intent: any) => {
  const objResult = ScannerReceiver(intent);
  // get data here
  console.log(objResult.data);
};

// ...
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
