package io.projectj.mic_recorder;

import android.app.Activity;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import io.flutter.Log;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.PluginRegistry;

public class MicRecorderPlugin implements MethodCallHandler, EventChannel.StreamHandler {
    private static final String TAG = "MicRecorder";

    private EventChannel.EventSink eventSink;

    private Timer recordTimer;

    private String currentOutputFile;

    private MediaRecorder recorder;
    private boolean isRecording = false;
    private double recorderSecondsElapsed = 0.0;

    JSONArray audioFrequency = new JSONArray();
    private boolean isAudioFrequencyAvailable = false;

    private Activity activity;

    private MicRecorderPlugin(Activity _activity) {
        this.activity = _activity;
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        eventSink = null;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (call.method.equals("startRecording")) {
            result.success(startRecording((String) call.arguments) ? "Ok" : "Error");
        } else if (call.method.equals("stopRecording")) {
            result.success(stopRecording() ? "Ok" : "Error");
        } else if (call.method.equals("getCurrentOutputFile")) {
            result.success(getCurrentOutputFile());
        } else if (call.method.equals("getAudioFrequency")) {
            result.success(getAudioFrequency());
        } else {
            result.notImplemented();
        }
    }

    public static void registerWith(PluginRegistry.Registrar registrar) {
        final MicRecorderPlugin plugin = new MicRecorderPlugin(registrar.activity());

        final MethodChannel methodChannel = new MethodChannel(registrar.messenger(), "micrecorder_audio");
        methodChannel.setMethodCallHandler(plugin);

        final EventChannel eventChannel = new EventChannel(registrar.messenger(), "micrecorder_audio_events");
        eventChannel.setStreamHandler(plugin);
    }

    private void sendEvent(final Object o) {
        if (eventSink != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    eventSink.success(o);
                    Log.d(TAG, o.toString());
                }
            });
        }
    }

    private boolean startRecording(String fileName) {
        currentOutputFile = fileName + ".3gp";

        MediaRecorder currentRecorder = new MediaRecorder();
        currentRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        currentRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        currentRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        currentRecorder.setAudioEncodingBitRate(16);
        currentRecorder.setAudioSamplingRate(44100);
        currentRecorder.setOutputFile(currentOutputFile);

        recorder = currentRecorder;

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Start recording fails: " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Start recording fails: " + e.getMessage());
            return false;
        }

        recorder.start();
        startRecordTimer();
        isRecording = true;

        return true;
    }

    private boolean stopRecording() {
        if (!isRecording) {
            return true;
        }

        stopRecordTimer();
        isRecording = false;

        try {
            recorder.stop();
            recorder.reset();
            recorder.release();
        } catch (final RuntimeException e) {
            Log.e(TAG, "Stop recording fails.");
            return false;
        }

        recorder = null;
        return true;
    }

    private void startRecordTimer() {
        stopRecordTimer();
        recordTimer = new Timer();
        recordTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    saveCurrentFrequency();
                } catch (JSONException e) {
                    Log.e(TAG, "Fail to save current frequency");
                }
                sendRecordTimerEvent();
                recorderSecondsElapsed = recorderSecondsElapsed + 0.1;
            }
        }, 0, 100);
    }

    private void stopRecordTimer() {
        recorderSecondsElapsed = 0.0;
        if (recordTimer != null) {
            recordTimer.cancel();
            recordTimer.purge();
            recordTimer = null;
        }
    }

    private void sendRecordTimerEvent() {
        HashMap<String, Object> timeObj = new HashMap<>();
        timeObj.put("code", "recording");
        timeObj.put("current_time", recorderSecondsElapsed);
        sendEvent(timeObj);
    }

    private void saveCurrentFrequency() throws JSONException {
        JSONObject currentFrequency = new JSONObject();
        currentFrequency.put("peak_amplitude", (double) recorder.getMaxAmplitude());
        currentFrequency.put("time", recorderSecondsElapsed);
        audioFrequency.put(currentFrequency);
    }

    private String getAudioFrequency() {
        try {
            if (!isAudioFrequencyAvailable) {
                throw new Exception("Audio frequency is not ready yet.");
            }
        } catch (Exception err) {
            Log.e(TAG, err.getMessage());
        }
        return audioFrequency.toString();
    }

    private String getCurrentOutputFile() {
        try {
            if (currentOutputFile.isEmpty()) {
                throw new Exception("No output file found");
            }
        } catch (Exception err) {
            Log.e(TAG, err.getMessage());
        }
        return currentOutputFile;
    }
}
