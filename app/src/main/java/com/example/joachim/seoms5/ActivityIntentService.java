package com.example.joachim.seoms5;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.lang.reflect.Type;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;

import com.google.gson.Gson;

import android.content.Intent;
import android.app.IntentService;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.content.res.Resources;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityIntentService extends IntentService {
    protected static final String TAG = ActivitiesAdapter.class.getName();

    private PrintWriter logger;
    private File file;

    //Call the super IntentService constructor with the name for the worker thread//
    public ActivityIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //setUpLog();
    }

    //Define an onHandleIntent() method, which will be called whenever an activity detection update is available//
    @Override
    protected void onHandleIntent(Intent intent) {
        //Check whether the Intent contains activity recognition data//
        if (ActivityRecognitionResult.hasResult(intent)) {

            //If data is available, then extract the ActivityRecognitionResult from the Intent//
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            Intent broadcastintent = new Intent();
            broadcastintent.setAction(SEOMS5.ResponseReciever.ACTIVITYRESULTACTION);
            broadcastintent.putExtra("result", result);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.sendBroadcast(broadcastintent);

           // logActivity(result, this.getApplicationContext());
            //Get an array of DetectedActivity objects//
            ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(SEOMS5.DETECTED_ACTIVITY,
                            detectedActivitiesToJson(detectedActivities))
                    .apply();

        }
    }

    private void logActivity(ActivityRecognitionResult result, Context context) {
        String activityType = getActivityString(context, result.getMostProbableActivity().getType());
        int confidence = result.getMostProbableActivity().getConfidence();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        log(date + ";" + activityType + ";" + confidence);
        log("/n");
    }

    //Convert the code for the detected activity type, into the corresponding string//
    @SuppressLint("StringFormatInvalid")
    static String getActivityString(Context context, int detectedActivityType) {
        Resources resources = context.getResources();
        switch (detectedActivityType) {
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.vehicle);
            default:
                return resources.getString(R.string.unknown_activity, detectedActivityType);
        }
    }

    static final int[] POSSIBLE_ACTIVITIES = {
            DetectedActivity.STILL,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.TILTING,
            DetectedActivity.UNKNOWN
    };

    static String detectedActivitiesToJson(ArrayList<DetectedActivity> detectedActivitiesList) {
        Type type = new TypeToken<ArrayList<DetectedActivity>>() {
        }.getType();
        return new Gson().toJson(detectedActivitiesList, type);
    }

    static ArrayList<DetectedActivity> detectedActivitiesFromJson(String jsonArray) {
        Type listType = new TypeToken<ArrayList<DetectedActivity>>() {
        }.getType();
        ArrayList<DetectedActivity> detectedActivities = new Gson().fromJson(jsonArray, listType);
        if (detectedActivities == null) {
            detectedActivities = new ArrayList<>();
        }
        return detectedActivities;
    }

    private void setUpLog() {
        String name = String.format("Activity-Recognition-%d.txt", System.currentTimeMillis());
        Log.d("TAG", "Log: " + name);

        String dirname = "";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            dirname = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        }

        file = new File(dirname + File.separator + name);
        Log.d("TAG", "Log File: " + file);

        try {
            logger = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        log(String.format("Logging stuff", System.currentTimeMillis()));
        log("Service Stop");
        logger.close();
        logger = null;

    }

    private void log(String data) {
        Log.d(TAG, data);

        logger.println(data);
        logger.toString();
    }

    @Override
    public void onDestroy() {
        //log(String.format("Service, stop"));
        //logger.close();
        //logger = null;
        super.onDestroy();
    }
}
