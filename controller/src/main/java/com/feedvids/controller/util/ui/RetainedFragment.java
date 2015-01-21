package com.feedvids.controller.util.ui;

import android.support.v7.media.MediaRouter;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.media.MediaRouteSelector;

import com.feedvids.controller.PocketListActivity;
import com.feedvids.controller.data.VidItem;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.List;

import static android.support.v7.media.MediaRouter.Callback;

/**
 * @author luismoramedina
 */
public class RetainedFragment extends Fragment {

    // data object we want to retain
    public List<VidItem> data;
    public MediaRouter router;
    public MediaRouteSelector mediaRouteSelector;
    public Callback mediaRouterCallback;
    public VidItem currentVideoItem;
    public GoogleApiClient apiClient;
    public PocketListActivity.FeedvidsMessageReceiver feedvidsMessageReceiver;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }
}
