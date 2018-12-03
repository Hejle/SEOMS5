package com.example.joachim.seoms5;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ListView;
import android.app.PendingIntent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.View;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class SEOMS5 extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;
    protected static final String TAG = ActivitiesAdapter.class.getName();
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    //Define an ActivityRecognitionClient//
    private ResponseReciever receiver;


    private PrintWriter logger;
    private File file;

    private ActivityRecognitionClient activityRecognitionClient;
    private ActivitiesAdapter activitiesAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seoms5);

        context = this;
        acquirePermissions(this);
        //Retrieve the ListView where we’ll display our activity data//
        ListView detectedActivitiesListView = (ListView) findViewById(R.id.activities_listview);

        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        DETECTED_ACTIVITY, ""));

        //Bind the adapter to the ListView//
        activitiesAdapter = new ActivitiesAdapter(this, detectedActivities);
        detectedActivitiesListView.setAdapter(activitiesAdapter);
        activityRecognitionClient = new ActivityRecognitionClient(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
        IntentFilter broadcastFilter = new IntentFilter(ResponseReciever.ACTIVITYRESULTACTION);
        receiver = new ResponseReciever();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(receiver, broadcastFilter);
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(receiver);
    }

    public static void acquirePermissions(Activity activity) {
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }

        while (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void requestUpdatesHandler(View view) {
        //Set the activity detection interval. I’m using 3 seconds//
        setUpLog();
        Task<Void> task = activityRecognitionClient.requestActivityUpdates(
                30,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });
    }

    //Get a PendingIntent//
    private PendingIntent getActivityDetectionPendingIntent() {
        //Send the activity data to our DetectedActivitiesIntentService class//
        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    //Process the list of activities//
    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(DETECTED_ACTIVITY, ""));

        activitiesAdapter.updateActivities(detectedActivities);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }

    private void setUpLog() {
        String name = String.format("Activity-Recognition-%d.txt", System.currentTimeMillis());
        Log.d("TAG", "Log: " + name);

        final String dirname;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            dirname = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        } else {
            dirname = context.getApplicationContext().getFilesDir().getAbsolutePath();
        }

        file = new File(dirname + File.separator + name);
        Log.d("TAG", "Log File: " + file);

        try {
            logger = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        log(String.format("Logging stuff", System.currentTimeMillis()));

    }

    private void log(String data) {
        //Log.d(TAG, data);

        logger.println(data);
        logger.toString();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*log(String.format("Service, stop"));
        logger.close();
        logger = null;*/
    }

    public void stopLogging(View v) {
        log(String.format("Service, stop"));
        logger.close();
        logger = null;
    }

    private void logActivity(ActivityRecognitionResult result, Context context) {
        String activityType = ActivityIntentService.getActivityString(context, result.getMostProbableActivity().getType());
        int confidence = result.getMostProbableActivity().getConfidence();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        log(date + ";" + activityType + ";" + confidence);
    }

    public class ResponseReciever extends BroadcastReceiver {

        public static final String ACTIVITYRESULTACTION= "com.example.joachim.seoms5";

        @Override
        public void onReceive(Context context, Intent intent) {
            ActivityRecognitionResult result = null;
            try {
                result = intent.getParcelableExtra("result");
            } catch (ClassCastException e) {
                throw new RuntimeException();
            }

            if (result != null) {
                logActivity(result, context);
            }

        }
    }
}
