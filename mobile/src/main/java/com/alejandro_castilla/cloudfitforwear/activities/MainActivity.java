package com.alejandro_castilla.cloudfitforwear.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.alejandro_castilla.cloudfitforwear.R;
import com.alejandro_castilla.cloudfitforwear.asynctask.GetAllTrainingsTask;
import com.alejandro_castilla.cloudfitforwear.asynctask.GetUserInfoTask;
import com.alejandro_castilla.cloudfitforwear.cloudfit.models.RequestTrainer;
import com.alejandro_castilla.cloudfitforwear.cloudfit.models.User;
import com.alejandro_castilla.cloudfitforwear.cloudfit.services.CloudFitService;
import com.alejandro_castilla.cloudfitforwear.cloudfit.utilities.StaticReferences;
import com.alejandro_castilla.cloudfitforwear.cloudfit.utilities.zDBFunctions;
import com.alejandro_castilla.cloudfitforwear.interfaces.GetUserInfoInterface;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GetUserInfoInterface {

    private final String TAG = MainActivity.class.getSimpleName();

    private Button downloadButton;

    private CloudFitService cloudFitService;
    private User cloudFitUser;
    private ArrayList<RequestTrainer> requests;

    /**
     * ServiceConnection to connect to CloudFit service.
     */
    private ServiceConnection cloudFitServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to CloudFit service");
            CloudFitService.MyBinder cloudFitServiceBinder = (CloudFitService.MyBinder) service;
            cloudFitService = cloudFitServiceBinder.getService();
            new GetUserInfoTask(MainActivity.this, cloudFitService, MainActivity.this).execute();
            Log.d(TAG, "Getting user data");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from CloudFit service");
        }
    };

    /**
     * Saves user info obtained from GetUserInfoTask on this activity.
     * @param cloudFitUser User data from CloudFit platform.
     * @param requests Requests from trainers sent from the platform.
     */
    @Override
    public void saveUserInfo(User cloudFitUser, ArrayList<RequestTrainer> requests) {
        this.cloudFitUser = cloudFitUser;

        if (cloudFitUser.getRol() == StaticReferences.ROL_USER) {
            Log.d(TAG, "ROL USER OK");
            this.cloudFitUser.setUsername(cloudFitService.getFit().getSetting().getUsername());
//            Log.d(TAG, "ROLE: " + cloudFitService.getFit().getSetting().getRole());
            cloudFitService.getFit().getSetting().setUserID(cloudFitUser.getId()+"");

            if (requests != null && requests.size()>0) {
                this.requests = requests;
                Log.d(TAG, "Request length: " + this.requests.size());
                Log.d(TAG, "Request trainer ID: " + this.requests.get(0).getTrainerid());
                Toast.makeText(this, "Número de solicitudes: " + this.requests.size(),
                        Toast.LENGTH_SHORT).show();
                zDBFunctions.saveSetting(cloudFitService.getDB(),
                        cloudFitService.getFit().getSetting());
//                new ReplyToRequestTask(this, cloudFitService,
//                        Long.parseLong(cloudFitService.getFit().getSetting().getUserID()),
//                        this.requests.get(0).getTrainerid(), StaticReferences.REQUEST_ACCEPT)
//                        .execute();
            } else {
                Toast.makeText(this, "No hay solicitudes.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadButton = (Button) findViewById(R.id.downloadButton);

        Intent cloudFitServiceIntent = new Intent(MainActivity.this, CloudFitService.class);
        bindService(cloudFitServiceIntent, cloudFitServiceConnection, Context.BIND_AUTO_CREATE);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO Get all trainings
//                new ReplyToRequestTask(MainActivity.this, cloudFitService,
//                        Long.parseLong(cloudFitService.getFit().getSetting().getUserID()),
//                        requests.get(0).getTrainerid(), StaticReferences.REQUEST_ACCEPT)
//                        .execute();
                new GetAllTrainingsTask(MainActivity.this, cloudFitService).execute();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbindService(cloudFitServiceConnection);
        super.onDestroy();
    }
}