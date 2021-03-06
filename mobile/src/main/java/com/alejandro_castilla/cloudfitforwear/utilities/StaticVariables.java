package com.alejandro_castilla.cloudfitforwear.utilities;

import android.os.Environment;

import com.alejandro_castilla.cloudfitforwear.activities.adapters.TrainingsFragmentAdapter;
import com.alejandro_castilla.cloudfitforwear.asynctask.GetTrainingsTask;

public class StaticVariables {

    public static final String APP_NAME = "CloudFitForWear";
    public static final String APP_PATH = Environment.getExternalStorageDirectory() + "/"
            + StaticVariables.APP_NAME + "/";

    /**
     * Variables for {@link GetTrainingsTask}
     */

    public static final short GET_ALL_TRAININGS = 1;
    public static final short GET_SINGLE_TRAINING = 2;
    public static final short GET_TRAINING_NOT_DONE = 3;
    public static final short GET_TRAINING_DONE = 4;

    /**
     * Variables for click listener on {@link TrainingsFragmentAdapter}
     */

    public static final short NORMAL_BUTTON = 1;
    public static final short HIGHLIGHT_BUTTON = 2;

    /**
     * Message types
     */

    public static final short MSG_WEARABLESERVICE_MESSENGER = 1;

    public static final short MSG_REQUEST_WEARABLE_STATE = 2;
    public static final short MSG_WEARABLE_STATE = 3;

    public static final short MSG_SEND_TRAINING_TO_WEARABLE = 4;
    public static final short MSG_SEND_TRAINING_TO_WEARABLE_ACK = 5;

    public static final short MSG_TRAINING_RECEIVED_FROM_WEARABLE = 6;
    public static final short MSG_TRAINING_RECEIVED_FROM_WEARABLE_ACK = 7;

    /**
     * Bundle strings
     */

    public static final String BUNDLE_WEARABLE_STATE = "wearablestate";
    public static final String BUNDLE_WEARABLE_TRAINING_DONE = "wearabletrainingdone";

    /**
     * Data Map Request Types
     */

    public static final String TRAINING_FROM_HANDHELD = "/traininghandheld";
    public static final String ACK_FROM_WEARABLE = "/ackwearable";
    public static final String TRAINING_DONE_FROM_WEARABLE = "/trainingdone";
    public static final String ACK_FROM_HANDHELD = "/ackhandheld";

    /**
     * Data Map Types
     */

    public static final String WEARABLE_TRAINING = "wearabletraining";
    public static final String WEARABLE_TRAINING_ACK = "wearabletrainingack";
    public static final String WEARABLE_TRAINING_DONE = "wearabletrainingdone";
    public static final String WEARABLE_TRAINING_DONE_ACK = "wearabletrainingdoneack";

    /**
     * Preference keys
     */

    public static final String KEY_PREF_CLOUDFIT_URL = "cloudfit_url";



}
