package com.ck.audiorecplay;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.ContentValues.TAG;

public class RecordActivity extends AppCompatActivity {

    public static MediaRecorder _recorder;
    
    double dB = 0.0, averageFreq;
    List<Double> frequency;
    ProgressDialog loading = null;
    private MediaPlayer _player;
    private Timer _timer;
    private String _path;
    private boolean firstTime = true;
    CardView cardRecord;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
//        loading = new ProgressDialog(RecordActivity.this);
//        loading.setMessage("Please wait.....");
//        loading.show();
        _timer = new Timer();
        prepareEvents();
        frequency = new ArrayList<Double>();
        prepareEvents();
    }

    public void prepareEvents() {

        cardRecord = findViewById(R.id.cardRecord);
        ImageView recordButton = findViewById(R.id.buttonRecord);
        ImageView stopButton = findViewById(R.id.buttonStop);
        Button playButton = findViewById(R.id.buttonPlay);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                //File write logic here
            }
        }

        cardRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MediaPlayer mp = MediaPlayer.create(RecordActivity.this, R.raw.sevilla);

                try {
                    if (CheckPermissions()) {
                        if (_recorder == null) {
                            File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            _path = downloadFolder.getAbsolutePath() + "/test1.aac";
                            Log.d(TAG, "onClick: absolute path is: " + _path);

                            _recorder = new MediaRecorder();
                            _recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            _recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
                            _recorder.setAudioSamplingRate(48000);
                            _recorder.setAudioEncodingBitRate(96000);
                            _recorder.setAudioChannels(2);
                            _recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                            _recorder.setOutputFile(_path);
                            try {
                                _recorder.prepare();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mp.start();
                            _recorder.start();
                            _timer = new Timer();
                            long startTime = System.currentTimeMillis();
                            _timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    RecordActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                dB = 20.0 * Math.log10(_recorder.getMaxAmplitude());
                                                dB = Double.parseDouble(String.format("%.2f", dB));
                                            } catch (NullPointerException e) {
                                                Log.d(TAG, "run: Getting null pointer here");
                                            }
                                            ((TextView) findViewById(R.id.textViewDecibelNumber)).setText(String.valueOf(dB));
                                            if (!String.valueOf(dB).equals("Infinity") && !String.valueOf(dB).equals("-Infinity")) {
                                                frequency.add(dB);
                                            }
                                            Log.d(TAG, "run: frequency " + frequency);
                                            Log.d(TAG, "run: System.currentTimeMillis() " + System.currentTimeMillis());
                                            Log.d(TAG, "run: startTime " + startTime);
                                            Log.d(TAG, "run: System.currentTimeMillis() - startTime " + (System.currentTimeMillis() - startTime));
                                            if (System.currentTimeMillis() - startTime < 10000) {
                                                Log.d(TAG, "less than 30 sec ");
                                            } else {
                                                Log.d(TAG, "more than 30 sec ");
                                                stopButton.performClick();
                                            }
                                        }
                                    });
                                }
                            }, 1000, 1000);
                        }
                    }
                    else {
                        // if audio recording permissions are
                        // not granted by user below method will
                        // ask for runtime permission for mic and storage.
                        RequestPermissions();
                    }
                } catch (Exception exception) {
                    Log.d(TAG, "onClick: exception comes here cardRecord " + exception.getMessage());
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss aaa z");
                String dateTime = simpleDateFormat.format(calendar.getTime()).toString();
//                    format3.setText(dateTime);
                double sumFreq, sumLatitude, sumLongitude;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sumFreq = frequency.stream().mapToDouble(Double::doubleValue).sum();
                    int count = frequency.size();
                    Log.d(TAG, "onClick: average of all values " + sumFreq / count);
                    averageFreq = sumFreq / count;
                }

                try {
                    if (_recorder != null) {
                        _recorder.stop();
                        _recorder.release();
                        _recorder = null;
                        _timer.cancel();
                        ((TextView) findViewById(R.id.textViewDecibelNumber)).setText("0");
                        Toast.makeText(RecordActivity.this, "Your recording has been saved successfully in Downloads/test1.aac.", Toast.LENGTH_SHORT).show();
                    } else if (_player != null) {
                        _player.stop();
                        _player.release();
                        _player = null;
                        Toast.makeText(RecordActivity.this, "Your recording has been saved successfully in Downloads/test1.aac.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception exception) {
                    Log.d(TAG, "onClick: stop button catch block " + exception.getMessage().toString());
                    Toast.makeText(RecordActivity.this, "Something went wrong please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (_recorder == null && _path != "") {
                        _player = new MediaPlayer();
                        _player.setDataSource(_path);
                        _player.prepare();
                        _player.start();
                    }
                } catch (Exception exception) {
                    System.out.println(exception.getMessage());
                }
            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: onStop");
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onStop: onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // this method is called when user will
        // grant the permission for audio recording.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean CheckPermissions() {
        // this method is used to check permission
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        // this method is used to request the
        // permission for audio recording and storage.
        ActivityCompat.requestPermissions(RecordActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }




}