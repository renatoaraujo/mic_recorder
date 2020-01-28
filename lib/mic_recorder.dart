import 'dart:async';

import 'package:flutter/services.dart';

class MicRecorder {
  static const MethodChannel _channel =
      const MethodChannel('mic_recorder');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
