package com.feedvids.controller;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.feedvids.controller.data.FakeData;
import com.feedvids.controller.data.VidItem;
import com.feedvids.controller.oauth.OAuthActivity;
import com.feedvids.controller.service.PocketService;
import com.feedvids.controller.service.PocketServiceImpl;
import com.feedvids.controller.util.ui.RetainedFragment;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author luismoramedina
 */
public class PocketListActivity extends ActionBarActivity implements Notificable {

    private ProgressDialog dialog;

    private static final String TAG = PocketListActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 1;

    private MediaRouter mediaRouter;
    private MediaRouteSelector mediaRouteSelector;
    private MediaRouter.Callback mediaRouterCallback;
    private CastDevice selectedDevice;
    private GoogleApiClient apiClient;
    private FeedvidsMessageReceiver feedvidsMessageReceiver;
    private boolean applicationStarted;
    private boolean waitingForReconnect;
    private String sessionId;
    private VidItem currentVideoItem;
    private List<VidItem> videoList;
    private VidArrayAdapter adapter;
    private boolean debug = false;
    private PocketService pocketService;
    private RetainedFragment dataFragment;
    private int itemForDelete;

    public PocketListActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vid_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(
                android.R.color.transparent));



        // find the retained fragment on activity restarts
        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag("data");

        // create the fragment and data the first time
        if (dataFragment == null) {
            // add the fragment
            dataFragment = new RetainedFragment();
            fm.beginTransaction().add(dataFragment, "data").commit();

            // Configure Cast device discovery
            mediaRouter = MediaRouter.getInstance(getApplicationContext());
            mediaRouteSelector = new MediaRouteSelector.Builder()
                    .addControlCategory(CastMediaControlIntent.categoryForCast(getString(R.string.cast_ap_pid))).build();
            mediaRouterCallback = new MediaRouterCallback();

        } else {
            videoList = dataFragment.data;
            mediaRouter = dataFragment.router;
            mediaRouteSelector = dataFragment.mediaRouteSelector;
            mediaRouterCallback = dataFragment.mediaRouterCallback;
            apiClient = dataFragment.apiClient;
            feedvidsMessageReceiver = dataFragment.feedvidsMessageReceiver;
            currentVideoItem = dataFragment.currentVideoItem;

        }

        String oauthAccessToken = getSharedPreferences("com.feedvids", MODE_PRIVATE).getString("OAUTH_ACCESS_TOKEN", null);
        if (oauthAccessToken != null) {
            feedList(videoList == null);
        } else {
            launchOathProcess();
        }



    }

    private void launchOathProcess() {
        Intent intent = new Intent(this, OAuthActivity.class);
        startActivityForResult(intent, 24);
    }

    private void feedList(boolean goToRepository) {
        if (goToRepository) {

            dialog = new ProgressDialog(this);
            dialog.setMessage("Getting list...");
            dialog.setCancelable(false);
            dialog.show();

            pocketService = new PocketServiceImpl(
                    this,
                    getString(R.string.consumer_key),
                    getSharedPreferences("com.feedvids", MODE_PRIVATE)
                            .getString("OAUTH_ACCESS_TOKEN", null));

            if (!debug) {
                try {
                    pocketService.retrieve();
                } catch (Throwable e) {
                    Toast.makeText(PocketListActivity.this, "Error retrieving vids, try again", Toast.LENGTH_SHORT).show();
                }

                //Then callback when finish notifyActionFinish
            } else {
                fillListView(FakeData.DATA);
                dialog.dismiss();
            }
        } else {
            loadListViewWithVideos();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_ctx_item, menu);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        View targetView = info.targetView;
        String itemId = videoList.get(info.position).itemId;
        String videoId = videoList.get(info.position).id;

        switch (item.getItemId()) {
            case R.id.ctx_option_delete:
                delete(itemId);
                Toast.makeText(targetView.getContext(), "Delete option", Toast.LENGTH_LONG).show();
                markForDelete(info.position);
                return true;
            case R.id.ctx_option_archive:
                Toast.makeText(targetView.getContext(), "Archive option", Toast.LENGTH_LONG).show();
                //TODO call archive
                return true;
            case R.id.ctx_option_play:
                sendPlayVidById(itemId);
                Toast.makeText(targetView.getContext(), "Play option", Toast.LENGTH_LONG).show();
                return true;
            case R.id.ctx_option_external_play:
//                String url = "http://www.youtube.com/?v=" + videoId;
                playOut(videoId);
                Toast.makeText(targetView.getContext(), "Play out option", Toast.LENGTH_LONG).show();
                return true;

            case R.id.ctx_option_share:

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
                i.putExtra(Intent.EXTRA_TEXT, "http://www.youtube.com/watch?v=" + videoId);
                startActivity(Intent.createChooser(i, "Share URL"));

                Toast.makeText(targetView.getContext(), "Send option", Toast.LENGTH_LONG).show();
                return true;


            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * play on youtube
     * @param videoId the id
     */
    private void playOut(String videoId) {
        String url = "vnd.youtube:" + videoId;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void markForDelete(int position) {
        itemForDelete = position;
    }

    private void fillListView(Object data) {
        if (data != null) {
            videoList = new ArrayList<VidItem>();
            JSONObject jObject;
            try {
                jObject = new JSONObject((String) data);
                JSONObject list = (JSONObject) jObject.get("list");
                Iterator keys = list.keys();

                while (keys.hasNext()) {
                    Object vidItemId = keys.next();
                    JSONObject item = (JSONObject) list.get((String) vidItemId);
                    JSONObject videos = (JSONObject) item.get("videos");
                    String resolvedTitle = item.getString("resolved_title");
                    String timeAdded = item.getString("time_added");
                    Iterator videoKeys = videos.keys();
                    while (videoKeys.hasNext()) {
                        String videoKey = (String) videoKeys.next();
                        JSONObject videoItem = (JSONObject) videos.get(videoKey);
                        String videoUrl = videoItem.getString("src");
                        //TODO use
                        String videoId = videoItem.getString("vid");
                        String itemId = videoItem.getString("item_id");
                        String videoOrder = videoItem.getString("video_id");
                        String type = videoItem.getString("type");

                        //1 = youtube
                        if (type.equals("1")) {
                            VidItem vidItem = new VidItem();
                            vidItem.itemId = itemId;
                            vidItem.id = videoId;
                            vidItem.description = resolvedTitle;
                            vidItem.dateAdded = Long.valueOf(timeAdded);
                            vidItem.order = Integer.valueOf(videoOrder);
                            videoList.add(vidItem);
                        }

                    }
                    //654539651

                }

                Collections.sort(videoList);

                loadListViewWithVideos();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Null data", Toast.LENGTH_LONG).show();
        }
    }

    private void loadListViewWithVideos() {
        final ListView listview = (ListView) findViewById(R.id.listview);
        registerForContextMenu(listview);

        adapter = new VidArrayAdapter(this,
                android.R.layout.simple_list_item_1, videoList);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final VidItem item = (VidItem) parent.getItemAtPosition(position);
                view.animate().setDuration(200).alpha(0).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        //videoList.remove(item);
                        adapter.notifyDataSetChanged();
                        view.setAlpha(1);

                        currentVideoItem = item;
                        playOut(item.id);
                    }
                });
                adapter.notifyDataSetChanged();
            }

        });

        new YoutubeVideoResolverAsyncTask().execute(videoList);
    }

    private void sendPlayVidById(String id) {
        sendMessage("{\"command\":\"" + "playVidById" + "\" , \"data\" : \"" + id + "\"}");
    }

    private void sendLoadVideos(String[] videos) {
        String message = "{\"command\":\"" + "load" + "\" , \"data\" : " + new JSONArray(Arrays.asList(videos)).toString() + "}";
        sendMessage(message);
    }

    @Override
    public void notifyActionFinish(String action, Object data) {
        if (action.equals("retrieve")) {
            fillListView(data);
            dialog.dismiss();
        }
        if (action.equals("delete")) {
            VidItem vidItem = videoList.get(itemForDelete);
            List<VidItem> itemsFromItemId = findItemsFromItemId(vidItem.itemId);
            for (VidItem item : itemsFromItemId) {
                adapter.remove(item);
                videoList.remove(item);
            }
        }
        //TODO successfully deleted... etc. actions_results: [true] is good
    }

    public void sendAllOnClick(MenuItem item) {
        sendLoadVideos();
    }

    public void updateOnClick(MenuItem item) {
        feedList(true);
    }

    private class VidArrayAdapter extends ArrayAdapter<VidItem> {

        HashMap<VidItem, Integer> mIdMap = new HashMap<VidItem, Integer>();

        public VidArrayAdapter(Context context, int textViewResourceId,
                               List<VidItem> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            return mIdMap.get(getItem(position));
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.vid_element, null);
            }

            VidItem item = getItem(position);
            if (item != null) {
                // My layout has only one TextView

                TextView itemView = (TextView) view.findViewById(R.id.first_line);
                if (itemView != null) {
                    // do whatever you want with your string and long
                    itemView.setText(item.description);
                }

                TextView itemViewSecondLine = (TextView) view.findViewById(R.id.second_line);
                if (itemViewSecondLine != null) {
                    // do whatever you want with your string and long

                    itemViewSecondLine.setText(item.videoDescription != null ?
                            item.videoDescription : item.id);
                }

                if (currentVideoItem != null && item.id.equals(currentVideoItem.id)) {
                    view.setBackgroundColor(Color.GRAY);
                } else {
                    view.setBackgroundColor(Color.WHITE);
                }
            }


            return view;
        }
    }


    /*
     * Handle the voice recognition response
     *
     * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches.size() > 0) {
                Log.d(TAG, matches.get(0));
                sendMessage(matches.get(0));
            }
        }
        if (requestCode == 24 && resultCode == RESULT_OK) {
            feedList(true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start media router discovery
        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            // End media router discovery
            mediaRouter.removeCallback(mediaRouterCallback);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
//        teardown();
        // store the data in the fragment
        //TODO store router and las viewed...
        dataFragment.data = videoList;
        dataFragment.router = mediaRouter;
        dataFragment.mediaRouteSelector= mediaRouteSelector;
        dataFragment.mediaRouterCallback = mediaRouterCallback;
        dataFragment.currentVideoItem = currentVideoItem;
        dataFragment.feedvidsMessageReceiver = feedvidsMessageReceiver;
        dataFragment.apiClient = apiClient;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);
        return true;
    }

    public void next(View view) {
        sendMessage(getCommand("next"));
    }

    public void pause(View view) {
        sendMessage(getCommand("pause"));
    }

    private String getCommand(final String command) {
        return "{\"command\":\"" + command + "\"}";
    }

    public void prev(View view) {
        sendMessage(getCommand("prev"));
    }

    public void play(View view) {
        sendMessage(getCommand("play"));
    }

    public void playPause(View view) {
        sendMessage(getCommand("playPause"));
    }
    public void rewind(View view) {
        sendMessage(getCommand("rewind"));
    }
    public void forward(View view) {
        sendMessage(getCommand("forward"));
    }

    public void removeOAuthToken(MenuItem item) {
        getSharedPreferences("com.feedvids", MODE_PRIVATE).edit().remove("OAUTH_ACCESS_TOKEN").commit();
    }

    public void delete(View view) {
        if (currentVideoItem != null && currentVideoItem.itemId != null) {
            delete(currentVideoItem.itemId);
        } else {
            Toast.makeText(PocketListActivity.this, "No vid selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void delete(String itemId) {
        if (itemId != null) {
            String deleteResult = null;
            try {
                deleteResult = pocketService.delete(itemId);
            } catch (Throwable e) {
                Toast.makeText(PocketListActivity.this, "Error deleting vid", Toast.LENGTH_SHORT).show();
            }
            System.out.println("deleteResult = " + deleteResult);
        }
    }

    /**
     * Callback for MediaRouter events
     */
    private class MediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRouteSelected");
            // Handle the user route selection.
            selectedDevice = CastDevice.getFromBundle(info.getExtras());

            launchReceiver();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: info=" + info);
            teardown();
            selectedDevice = null;
        }
    }

    /**
     * Start the receiver app
     */
    private void launchReceiver() {
        try {
            Cast.Listener mCastListener = new Cast.Listener() {

                @Override
                public void onApplicationDisconnected(int errorCode) {
                    Log.d(TAG, "application has stopped");
                    teardown();
                }

            };
            // Connect to Google Play services
            ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
            ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(selectedDevice, mCastListener);
            apiClient = new GoogleApiClient.Builder(this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();

            apiClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Failed launchReceiver", e);
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected");

            if (apiClient == null) {
                // We got disconnected while this runnable was pending
                // execution.
                return;
            }

            try {
                if (waitingForReconnect) {
                    waitingForReconnect = false;

                    // Check if the receiver app is still running
                    if ((connectionHint != null)
                            && connectionHint
                            .getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                        Log.d(TAG, "App  is no longer running");
                        teardown();
                    } else {
                        // Re-create the custom message channel
                        try {
                            Cast.CastApi.setMessageReceivedCallbacks(
                                    apiClient,
                                    feedvidsMessageReceiver.getNamespace(),
                                    feedvidsMessageReceiver);
                        } catch (IOException e) {
                            Log.e(TAG, "Exception while creating channel", e);
                        }
                    }
                } else {
                    // Launch the receiver app
                    Cast.CastApi.launchApplication(apiClient, (getString(R.string.cast_ap_pid)), false).setResultCallback(
                            new ResultCallback<Cast.ApplicationConnectionResult>() {
                                @Override
                                public void onResult(
                                        ApplicationConnectionResult result) {
                                    Status status = result.getStatus();
                                    Log.d(TAG,
                                            "ApplicationConnectionResultCallback.onResult: statusCode"
                                                    + status.getStatusCode()
                                    );
                                    if (status.isSuccess()) {
                                        ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                                        sessionId = result.getSessionId();
                                        String applicationStatus = result.getApplicationStatus();
                                        boolean wasLaunched = result
                                                .getWasLaunched();
                                        Log.d(TAG, "application name: " + applicationMetadata.getName()
                                                        + ", status: " + applicationStatus + ", sessionId: "
                                                        + sessionId + ", wasLaunched: " + wasLaunched
                                        );
                                        applicationStarted = true;

                                        // Create the custom message
                                        // channel
                                        feedvidsMessageReceiver = new FeedvidsMessageReceiver();
                                        try {
                                            Cast.CastApi.setMessageReceivedCallbacks(apiClient,
                                                    feedvidsMessageReceiver.getNamespace(), feedvidsMessageReceiver);
                                        } catch (IOException e) {
                                            Log.e(TAG, "Exception while creating channel", e);
                                        }

                                        //TODO Initial command!
                                    } else {
                                        Log.e(TAG, "application could not launch");
                                        teardown();
                                    }
                                }
                            }
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch application", e);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended");
            waitingForReconnect = true;
        }
    }

    private void sendLoadVideos() {
        String[] vidIds = new String[videoList.size()];
        for (int i = 0; i < videoList.size(); i++) {
            VidItem vidItem = videoList.get(i);
            vidIds[i] = vidItem.id;
        }

        // set the initial instructions
        // on the receiver
        sendLoadVideos(vidIds);
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "onConnectionFailed ");

            teardown();
        }
    }

    /**
     * Tear down the connection to the receiver
     */
    private void teardown() {
        Log.d(TAG, "teardown");
        if (apiClient != null) {
            if (applicationStarted) {
                if (apiClient.isConnected()) {
                    try {
                        //TODO not stop app, always next vid!
                        //Cast.CastApi.stopApplication(apiClient, sessionId);
                        if (feedvidsMessageReceiver != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(
                                    apiClient,
                                    feedvidsMessageReceiver.getNamespace());
                            feedvidsMessageReceiver = null;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while removing channel", e);
                    }
                    apiClient.disconnect();
                }
                applicationStarted = false;
            }
            apiClient = null;
        }
        selectedDevice = null;
        waitingForReconnect = false;
        sessionId = null;
    }

    /**
     * Send a text message to the receiver
     */
    private void sendMessage(String message) {
        if (apiClient != null && feedvidsMessageReceiver != null) {
            try {
                Cast.CastApi.sendMessage(apiClient,
                        feedvidsMessageReceiver.getNamespace(), message)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status result) {
                                if (!result.isSuccess()) {
                                    Log.e(TAG, "Sending message failed");
                                }
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Exception while sending message", e);
            }
        } else {
            Toast.makeText(PocketListActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private VidItem findItemFromVideoId(String videoId) {
        for (VidItem vidItem : videoList) {
            if (vidItem.id.equals(videoId)) {
                return vidItem;
            }
        }
        return null;
    }

    private List<VidItem> findItemsFromItemId(String itemId) {
        List<VidItem> list = new ArrayList<VidItem>();
        for (VidItem vidItem : videoList) {
            if (vidItem.itemId.equals(itemId)) {
                list.add(vidItem);
            }
        }
        return list;
    }


    /**
     * Custom message channel
     */
    public class FeedvidsMessageReceiver implements MessageReceivedCallback {

        /**
         * @return custom namespace
         */
        public String getNamespace() {
            return "urn:x-cast:com.google.cast.sample.helloworld";
        }

        /*
         * Receive message from the receiver app
         */
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace,
                                      String message) {
            Log.d(TAG, "onMessageReceived: " + message);
            if (message != null) {
                try {
                    //{"playing":"video"}
                    JSONObject json = new JSONObject(message);
                    if (json.has("playing")) {
                        String playing = (String) json.get("playing");
                        currentVideoItem = findItemFromVideoId(playing);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter.notifyDataSetChanged();

            }
        }

    }


    private class YoutubeVideoResolverAsyncTask extends AsyncTask {
        @Override
        protected void onPostExecute(Object o) {
            adapter.notifyDataSetChanged();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            List<VidItem> vidList = (List<VidItem>) params[0];
            doYoutubeRequest(vidList);
//            adapter.notifyDataSetChanged();
            return vidList;
        }

        private List<VidItem> doYoutubeRequest(List<VidItem> videoIds) {
            //String videos = "nvr7T4sfads,7lCDEYXw3mM,AxvPIJj38jI";
            String videos = "";
            int limit = 40;
            for(int i = 0; i < limit; i++) {
                VidItem video = videoIds.get(i);
                videos = videos.concat(video.id + ",");
            }
            videos = videos.substring(0, videos.length() - 1);
//https://www.googleapis.com/youtube/v3/videos?id=K96sMVa7Mug,uuM4yBFI03E,hHe0nckLiYU,4rtT7nOdBtk,100785455,101266603,82408340,hu4U8XKmJGA,fH0FukXyoi0,rcXh7SIB76A,iA24gVGnIxQ,tWpjJimRz1k,oeuuAcY1tAw,pQU3vmo-d-k,vr_SbrFIuKA,jJ9XaBanomQ,byVAVKPyoXE,BGHddKYkUjU,9buIAYnpUNM,VCA689lvfpI,e87NBOLnynw,Fu7a3WBDGHk,dhHKfSFGdUI,ewcAQvJkbM4,yziKqvPpqZ4,rS2G8P6C9fk,5ZFncj8IB_I,cseTX_rW3uM,DerdemZEloY,qHVnebR7s5A,vBZ5SLJmfdw,Xe6W1PeuomU,-tgTPSu4PeM,vWKDaOAwm34,nW9jRWZ8chg,qHVnebR7s5A,LE4HiUky9fE,TGbNjPJAMhg,q2bwKAPgsyA,_Jv9IoXUp-U,93932132,OC-MR-SIslg,sOPLieLQQQs,v5RMDeStF_M,aUY7fW7z4uI,t0tAdNj_Q0Q,FzZEO_-gBxU,Qi0RIowmrQU,RUUAfhbdx2Y,-HnBYO8k5pM,efnsrLg03e4,J8Z549GKkeM,83728153,z3mM5-t2w68,VSFGAl1BekY,,pQb6Qrao4VU,vUdlbcX_Gqo,p9MMJgFKv24,7d7cr-zdm1Y,jPUGyTalpvQ,taaEzHI9xyY,QW7OVd8YBr0,vyniPM2Kch0,p8kjbjx2EUw,YLBQi3ERh14,G5zPqgQ67yo,9YO4pVBtWxs,HzqThXINmN0,XmUs2v8XAVU,vJZOMyxgF7Q,M8lL0TbItBc,Zl77rUgiDYA,eoZ66ktG4gk,17d6WRnQeuo,S_YDZw2utZM,du4JbfK5okA,eQq4FH81jd0,VHNV3LHSvx4,rRoy6I4gKWU,81822628,5JU3W9itQXY,8O6sChAi9Tk,0HWv7YJtYCA,2FmFXQSIzCo,E5fccx15b2k,Q04ILDXe3QE,oEqV5TqqogM,lNOMk9d6NI8,cN2OCcTe8Bo,MlK8jCJrsH4,80832646,39391702,zByhbZV79S0,78289223,6QHkv-bSlds,I4vX-twze9I,M7lc1UVf-VE,QnkiKrruJiE,zhszwkcay2A,HS-UsJwnG-g,v5u_Owtbfew,SfbdQYmyEnI,72162108,nsZK12AG1nw,d6XXgeAkBfQ,9edp1ch__0s,NkCmcTnyNoc,iUtS3YpWt5U,70920316,yxW29JVXCqc,70920316,61402101,1eudJzUP1Oo,QJp6hmASstQ,JuaBy3e6fd4,cmZq7Yok6Ns,kT019zkUMF0,Qt1_atU_Qsg,p65fODkI3hA,65914235,uoSrI6MOy10,QuorKiKYey8,aF-zt73Zm5I,N5V203T95P0,61481733,T_BQevqRp44,efdiUjZLzwM,57010472,FmjMc-9PKRI,_jFHrpknS4c,dYBDCsNDIT8,59464655,2FGNvbc2MRk&key=AIzaSyDvmmt8JxytxYgeWCOnmYPIySaG9tGBPNc%20&part=snippet,contentDetails,statistics,status
            String videoGetInfoUrl = "https://www.googleapis.com/youtube/v3/videos?id=" +
                    videos + "&key=AIzaSyDvmmt8JxytxYgeWCOnmYPIySaG9tGBPNc%20&part=snippet,contentDetails,statistics,status";
            HttpGet httpGet = new HttpGet(videoGetInfoUrl);
            httpGet.setHeader("Content-Type", "application/json");
            httpGet.setHeader("X-Accept", "application/json");

            HttpClient httpClient = new DefaultHttpClient();

            HttpResponse response;
//            HashMap<String, String> map = new HashMap<String, String>();
            try {
                response = httpClient.execute(httpGet);
                System.out.println(response);
                String responseData = getResponseData(response);
                System.out.println("responseData = " + responseData);
                //items[x].snippet.title
                JSONObject jObject = new JSONObject(responseData);
                JSONArray list = (JSONArray) jObject.get("items");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject jsonVidItem = (JSONObject) list.get(i);
                    VidItem vidItem = videoIds.get(i);
                    String id = (String) jsonVidItem.get("id");
                    String title = (String) ((JSONObject) jsonVidItem.get("snippet")).get("title");

                    if (id != null && title != null) {
                        VidItem itemFromVideoId = findItemFromVideoId(id);
                        if (itemFromVideoId != null) {
                            itemFromVideoId.videoDescription = title;
                        }
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return videoIds;
        }

        private String getResponseData(HttpResponse response) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder sb = new StringBuilder("");
            String line;
            String lineSeparator = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line).append(lineSeparator);
            }
            in.close();
            return sb.toString();
        }

    }
}