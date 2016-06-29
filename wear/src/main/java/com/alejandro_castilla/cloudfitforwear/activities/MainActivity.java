package com.alejandro_castilla.cloudfitforwear.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.alejandro_castilla.cloudfitforwear.R;
import com.alejandro_castilla.cloudfitforwear.activities.adapters.MainActivityGridPagerAdapter;
import com.alejandro_castilla.cloudfitforwear.services.WearableService;

public class MainActivity extends WearableActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private Intent wearableServiceIntent;
    private WearableService wearableService;

    /* Layout Views */
    private ImageView startActionImgView;
    private ImageView settingsImgView;
    private GridViewPager gridViewPager;

    private ServiceConnection wearableServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to Wearable Service");
            WearableService.WearableServiceBinder wearableServiceBinder =
                    (WearableService.WearableServiceBinder) service;
            wearableService = wearableServiceBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from Wearable Service");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                startActionImgView = (ImageView) stub.findViewById(R.id.startActionImg);
                startActionImgView.setOnClickListener(new ActionButtonsClickListener());
                settingsImgView = (ImageView) stub.findViewById(R.id.settingsImg);
                settingsImgView.setOnClickListener(new ActionButtonsClickListener());

                gridViewPager = (GridViewPager) stub.findViewById(R.id.pager);
                gridViewPager.setAdapter(new MainActivityGridPagerAdapter(MainActivity.this));
                gridViewPager.setOffscreenPageCount(2);
                DotsPageIndicator dotsPageIndicator = (DotsPageIndicator)
                        findViewById(R.id.page_indicator);
                dotsPageIndicator.setPager(gridViewPager);
            }
        });

        setAmbientEnabled();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        wearableServiceIntent = new Intent(MainActivity.this, WearableService.class);
        bindService(wearableServiceIntent, wearableServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(wearableServiceConnection);
        super.onDestroy();
    }

    private class ActionButtonsClickListener implements View.OnClickListener {

        private int resId;

        @Override
        public void onClick(View v) {
            resId = v.getId();
            switch (resId) {
                case R.id.startActionImg:
                    Intent startPracticeActivityIntent = new Intent(MainActivity.this,
                            TrainingActivity.class);
                    startActivity(startPracticeActivityIntent);
                    break;
                case R.id.infoImg:
                    //TODO something here
                    break;
                case R.id.settingsImg:
                    Intent startSettingsActivityIntent = new Intent (MainActivity.this,
                            SettingsActivity.class);
                    startActivity(startSettingsActivityIntent);
                    break;
            }
        }
    }

}
