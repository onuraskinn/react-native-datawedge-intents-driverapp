import {
  registerBroadcastReceiver,
  sendBroadcastWithExtras,
} from 'react-native-datawedge-intents';

const config_scanner = {
  name: 'Example',
  package: 'datawedgeintents.example',
};

let ean8checked = true;
let ean13checked = true;
let code39checked = true;
let code128checked = true;
let isSendResult = false;

export function ScannerInit() {
  registerBroadcastReceiver({
    filterActions: [
      'com.zebra.reactnativedemo.ACTION',
      'com.symbol.datawedge.api.RESULT_ACTION',
    ],
    filterCategories: ['android.intent.category.DEFAULT'],
  });
  sendCommand('com.symbol.datawedge.api.GET_VERSION_INFO', '');
  ScannerDecoder();
}

export function ScannerTrigger() {
  sendCommand('com.symbol.datawedge.api.SOFT_SCAN_TRIGGER', 'TOGGLE_SCANNING');
}

export function ScannerDecoder() {
  // NOTE: Set the new configuration
  const body = {
    PROFILE_NAME: config_scanner.name,
    PROFILE_ENABLED: 'true',
    CONFIG_MODE: 'UPDATE',
    PLUGIN_CONFIG: {
      PLUGIN_NAME: 'BARCODE',
      PARAM_LIST: {
        scanner_selection: 'auto',
        decoder_ean8: '' + ean8checked,
        decoder_ean13: '' + ean13checked,
        decoder_code128: '' + code128checked,
        decoder_code39: '' + code39checked,
      },
    },
  };
  sendCommand('com.symbol.datawedge.api.SET_CONFIG', body);
}

function sendCommand(extraName: string, extraValue: any) {
  console.log(
    'Sending Command: ' + extraName + ', ' + JSON.stringify(extraValue)
  );
  var broadcastExtras: any = {};
  broadcastExtras[extraName] = extraValue;
  broadcastExtras.SEND_RESULT = '' + isSendResult;
  sendBroadcastWithExtras({
    action: 'com.symbol.datawedge.api.ACTION',
    extras: broadcastExtras,
  });
}

export function ScannerReceiver(intent: any): any {
  console.log('Received Intent: ' + JSON.stringify(intent));
  if (
    intent.hasOwnProperty('com.symbol.datawedge.api.RESULT_GET_VERSION_INFO')
  ) {
    // NOTE: The version has been returned (DW 6.3 or higher).  Includes the DW version along with other subsystem versions e.g MX
    const versionInfo =
      intent['com.symbol.datawedge.api.RESULT_GET_VERSION_INFO'];
    console.log('Version Info: ' + JSON.stringify(versionInfo));
    const datawedgeVersion = versionInfo.DATAWEDGE;
    console.log('Datawedge version: ' + datawedgeVersion);

    // NOTE: Fire events sequentially so the application can gracefully degrade the functionality available on earlier DW versions
    if (datawedgeVersion >= '06.3') {
      datawedge63();
    }
    if (datawedgeVersion >= '06.4') {
      datawedge64();
    }
    if (datawedgeVersion >= '06.5') {
      datawedge65();
    }
  } else if (
    intent.hasOwnProperty('com.symbol.datawedge.api.RESULT_ENUMERATE_SCANNERS')
  ) {
    // NOTE: Return from our request to enumerate the available scanners
    const enumeratedScannersObj =
      intent['com.symbol.datawedge.api.RESULT_ENUMERATE_SCANNERS'];
    enumerateScanners(enumeratedScannersObj);
  } else if (!intent.hasOwnProperty('RESULT_INFO')) {
    // NOTE: A barcode has been scanned
    const scannedData = intent['com.symbol.datawedge.data_string'];
    const scannedType = intent['com.symbol.datawedge.label_type'];
    if (scannedData) {
      return {
        data: scannedData,
        decoder: scannedType,
        timeAtDecode: new Date().toISOString(),
      };
    } else {
      return null;
    }
  }
}

function datawedge63() {
  sendCommand('com.symbol.datawedge.api.CREATE_PROFILE', config_scanner.name);
  sendCommand('com.symbol.datawedge.api.GET_ACTIVE_PROFILE', ''); // NOTE: Although we created the profile we can only configure it with DW 6.4.
  sendCommand('com.symbol.datawedge.api.ENUMERATE_SCANNERS', ''); // NOTE: Enumerate the available scanners on the device
}

function datawedge64() {
  // NOTE: Configure the created profile (associated app and keyboard plugin)
  const bodyAssociate = {
    PROFILE_NAME: config_scanner.name,
    PROFILE_ENABLED: 'true',
    CONFIG_MODE: 'UPDATE',
    PLUGIN_CONFIG: {
      PLUGIN_NAME: 'BARCODE',
      RESET_CONFIG: 'true',
      PARAM_LIST: {},
    },
    APP_LIST: [
      {
        PACKAGE_NAME: config_scanner.package,
        ACTIVITY_LIST: ['*'],
      },
    ],
  };
  sendCommand('com.symbol.datawedge.api.SET_CONFIG', bodyAssociate);

  // NOTE: Configure the created profile (intent plugin)
  const bodyIntent = {
    PROFILE_NAME: config_scanner.name,
    PROFILE_ENABLED: 'true',
    CONFIG_MODE: 'UPDATE',
    PLUGIN_CONFIG: {
      PLUGIN_NAME: 'INTENT',
      RESET_CONFIG: 'true',
      PARAM_LIST: {
        intent_output_enabled: 'true',
        intent_action: 'com.zebra.reactnativedemo.ACTION',
        intent_delivery: '2',
      },
    },
  };
  sendCommand('com.symbol.datawedge.api.SET_CONFIG', bodyIntent);

  // NOTE: Give some time for the profile to settle then query its value
  setTimeout(() => {
    sendCommand('com.symbol.datawedge.api.GET_ACTIVE_PROFILE', '');
  }, 1000);
}

function datawedge65() {
  // NOTE: Instruct the API to send
  isSendResult = true;
}

function enumerateScanners(enumeratedScanners: any) {
  let humanReadableScannerList = '';
  for (let i = 0; i < enumeratedScanners.length; i++) {
    console.log(
      'Scanner found: name= ' +
        enumeratedScanners[i].SCANNER_NAME +
        ', id=' +
        enumeratedScanners[i].SCANNER_INDEX +
        ', connected=' +
        enumeratedScanners[i].SCANNER_CONNECTION_STATE
    );
    humanReadableScannerList += enumeratedScanners[i].SCANNER_NAME;
    if (i < enumeratedScanners.length - 1) {
      humanReadableScannerList += ', ';
    }
  }
  return humanReadableScannerList;
}
