package com.example.joachim.seoms5;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.widget.ListView;
import android.app.PendingIntent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.View;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;


public class SEOMS5 extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    //Define an ActivityRecognitionClient//

    private ActivityRecognitionClient mActivityRecognitionClient;
    private ActivitiesAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seoms5);

        mContext = this;
        acquirePermissions(this);
        //Retrieve the ListView where we’ll display our activity data//
        ListView detectedActivitiesListView = (ListView) findViewById(R.id.activities_listview);

        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        DETECTED_ACTIVITY, ""));

        //Bind the adapter to the ListView//
        mAdapter = new ActivitiesAdapter(this, detectedActivities);
        detectedActivitiesListView.setAdapter(mAdapter);
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
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
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
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
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(DETECTED_ACTIVITY, ""));

        mAdapter.updateActivities(detectedActivities);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }
}
