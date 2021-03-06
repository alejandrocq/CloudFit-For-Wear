package com.alejandro_castilla.cloudfitforwear.activities.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alejandro_castilla.cloudfitforwear.R;
import com.alejandro_castilla.cloudfitforwear.activities.adapters.RequestsFragmentAdapter;
import com.alejandro_castilla.cloudfitforwear.asynctask.GetRequestsTask;
import com.alejandro_castilla.cloudfitforwear.cloudfit.models.RequestTrainer;
import com.alejandro_castilla.cloudfitforwear.interfaces.CloudFitDataHandler;
import com.blunderer.materialdesignlibrary.fragments.ScrollViewFragment;

import java.util.ArrayList;

/**
 * Created by alejandrocq on 17/05/16.
 */
public class RequestsFragment extends ScrollViewFragment {

    private CloudFitDataHandler cloudFitDataHandler;

    private View view;
    private RecyclerView recyclerView;
    private RequestsFragmentAdapter requestsFragmentAdapter;

    private ArrayList<RequestTrainer> requests = new ArrayList<>();

    public void setRequests(ArrayList<RequestTrainer> requests) {
        setRefreshing(false);
        this.requests = requests;
        if (requestsFragmentAdapter != null) requestsFragmentAdapter.setRequests(requests);
        checkNumberOfRequestsAndUpdateLayout();
    }

    public void checkNumberOfRequestsAndUpdateLayout() {
        if (view != null) {
            ImageView img = (ImageView) view.findViewById(R.id.imgInfo);
            TextView txt = (TextView) view.findViewById(R.id.textNoTrainings);

            if (requests.size()>0) {
                img.setVisibility(View.GONE);
                txt.setVisibility(View.GONE);
            } else {
                img.setVisibility(View.VISIBLE);
                txt.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        this.view = view;

        requestsFragmentAdapter =
                new RequestsFragmentAdapter(getActivity(), requests);
        recyclerView = (RecyclerView) view.findViewById(R.id.reqRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(requestsFragmentAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);

        checkNumberOfRequestsAndUpdateLayout();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        cloudFitDataHandler = (CloudFitDataHandler) activity;
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        requestsFragmentAdapter.setRequests(requests);
    }

    @Override
    public int getContentView() {
        return R.layout.fragment_requests;
    }

    @Override
    public boolean pullToRefreshEnabled() {
        return true;
    }

    @Override
    public int[] getPullToRefreshColorResources() {
        return new int[]{R.color.mdl_color_primary};
    }

    @Override
    public void onRefresh() {
        new GetRequestsTask(getActivity(), cloudFitDataHandler.getCloudFitService()).execute();
    }
}
