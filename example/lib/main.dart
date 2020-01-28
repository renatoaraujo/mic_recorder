import 'dart:io';
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:mic_recorder/mic_recorder.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  MicRecorder micRecorder = new MicRecorder();
  double recordPosition = 0.0;
  bool isRecording = false;

  String file;

  @override
  initState() {
    super.initState();
    micRecorder.setCallBack((dynamic data) {
      _onEvent(data);
    });
    _validatePermissions();
  }

  Future _validatePermissions() async {
    PermissionStatus microphonePermissionStatus = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.microphone);

    if ('PermissionStatus.granted' != microphonePermissionStatus.toString()) {
      final microphoneRequestResult = await PermissionHandler()
          .requestPermissions([PermissionGroup.microphone]);
      print(microphoneRequestResult.toString());
    }

    PermissionStatus storagePermissionStatus = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.microphone);

    if ('PermissionStatus.granted' != storagePermissionStatus.toString()) {
      final storageRequestResult = await PermissionHandler()
          .requestPermissions([PermissionGroup.storage]);
      print(storageRequestResult.toString());
    }

    return;
  }

  Future<String> get _localFile async {
    DateTime time = new DateTime.now();
    Directory appDocDirectory = await getExternalStorageDirectory();

    String directory = appDocDirectory.path + "/audio/";
    bool dirExists = await Directory(directory).exists();

    if (!dirExists) {
      await new Directory(directory)
          .create(recursive: true)
          .then((Directory dir) {
        print('New directory created: ' + dir.path);
      });
    }

    final filename = directory + time.millisecondsSinceEpoch.toString();
    return filename;
  }

  Future _startRecording(String filename) async {
    PermissionStatus permissionStatus = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.microphone);

    if ('PermissionStatus.granted' != permissionStatus.toString()) {
      print('Check permission for microphone');
      return;
    }

    String localFile = await _localFile;

    setState(() {
      file = localFile;
    });

    await micRecorder.startRecording(file);
    setState(() {
      isRecording = true;
    });
  }

  Future _stopRecording() async {
    await micRecorder.stopRecording();
    setState(() {
      isRecording = false;
    });

    String outputFile = await micRecorder.getOutputFile();
    String audioFrequency = await micRecorder.getAudioFrequency();

    print(audioFrequency);
    print(outputFile);
  }

  void _onEvent(dynamic event) {
    if (event['code'] == 'recording') {
      setState(() {
        recordPosition = event['current_time'];
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: new Text('Mic recorder demo'),
        ),
        body: new Center(
          child: new Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              new Text(recordPosition.toStringAsFixed(2),
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 50)),
              SizedBox(height: 30),
              new FloatingActionButton(
                child: isRecording
                    ? new Icon(Icons.mic, color: Colors.red)
                    : new Icon(Icons.mic, color: Colors.white),
                onPressed: () {
                  if (isRecording) {
                    _stopRecording();
                  } else {
                    _startRecording(null);
                  }
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
