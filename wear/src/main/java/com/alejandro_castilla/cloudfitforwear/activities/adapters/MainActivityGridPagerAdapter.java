package com.alejandro_castilla.cloudfitforwear.activities.adapters;

import android.content.Context;
import android.support.wearable.view.GridPagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.alejandro_castilla.cloudfitforwear.R;

/**
 * Created by alejandrocq on 21/04/16.
 */
public class MainActivityGridPagerAdapter extends GridPagerAdapter {

    private final String TAG = MainActivityGridPagerAdapter.class.getSimpleName();

    private final int[] actionLayouts = {R.id.mainLayout, R.id.startActionLayout,
            R.id.previousPracticesLayout};
    private final int NUMBER_OF_ACTIONS = actionLayouts.length;

    private Context context;

    public MainActivityGridPagerAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount(int i) {
        return NUMBER_OF_ACTIONS;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int row, int column) {
        return container.findViewById(actionLayouts[column]);
    }

    @Override
    public void destroyItem(ViewGroup container, int row, int column, Object view) {
//        container.removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}