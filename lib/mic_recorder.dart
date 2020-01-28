import 'dart:async';
import 'dart:collection';
import 'package:flutter/services.dart';

class MicRecorder {
  dynamic callback;

  static const MethodChannel platform = const MethodChannel('micrecorder_audio');
  static const EventChannel eventChannel =
  const EventChannel('micrecorder_audio_events');

  MicRecorder() {
    eventChannel.receiveBroadcastStream().listen((event) {
      callback(event);
    });
  }

  void setCallBack(dynamic _callback) {
    callback = _callback;
  }

  Future<String> startRecording(String file) async {
    final String result = await platform.invokeMethod('startRecording', file);
    return result;
  }

  Future<String> stopRecording() async {
      final String result = await platform.invokeMethod('stopRecording');
      return result;
  }

  Future<String> getOutputFile() async {
    final String result = await platform.invokeMethod('getCurrentOutputFile');
    return result;
  }

  Future<String> getAudioFrequency() async {
    final String result = await platform.invokeMethod('getAudioFrequency');
    return result;
  }
}
