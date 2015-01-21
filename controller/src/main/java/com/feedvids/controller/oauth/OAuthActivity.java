package com.feedvids.controller.oauth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.feedvids.controller.Notificable;
import com.feedvids.controller.R;
import com.feedvids.controller.service.PocketService;
import com.feedvids.controller.service.PocketServiceImpl;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class OAuthActivity extends Activity implements View.OnClickListener, Notificable {

    public static final String FIRST_NAME = "firstName";
    static final String SUMMARY = "summary";
    private ProgressDialog progDailog;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauth_welcome);
        View nextButton = findViewById(R.id.next_button);
        View nextButtonWithOAuth = findViewById(R.id.next_button_with_oauth);
        nextButton.setOnClickListener(this);
        nextButtonWithOAuth.setOnClickListener(this);
//        doNextAction();
    }

    private void showProfile() {
        Log.v(OAuthActivity.class.getSimpleName(), "Fetching profileForCurrentUser for current user.");
        new ShowResultAsynkTaskTask().execute();
    }

    private void showProfile(Object profile) {
        System.out.println("profile = " + profile);
        HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        stringStringHashMap.put(FIRST_NAME, "one");
        stringStringHashMap.put(SUMMARY, "two");

        Toast.makeText(this, "showProfile", Toast.LENGTH_LONG).show();
/*
        Intent intent = new Intent(this, ShowUserActivity.class);
        intent.putExtras(getBundleFromMap(stringStringHashMap));
        startActivity(intent);
*/
    }

    public void onClick(View v) {
        if((v.getId()) == R.id.next_button_with_oauth) {
            doOAuth();
        } else {
            doNextAction();
        }
    }

    private void doNextAction() {
        //Button next
        try {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            //If not exists a token on prefs we launch the oauth li process
            if ("".equals(defaultSharedPreferences.getString(getString(R.string.pocket_access_token_pref), ""))) {

                doOAuth();
                //see postexecute
            } else {
                doShowProfile();
            }

        } catch (Exception e) {
            Log.e(OAuthActivity.class.getSimpleName(), "Error", e);
        }
    }

    private void doShowProfile() {
        SharedPreferences defaultSharedPreferences;
        //           "429a204a-....",//token
        //           "9f37a401-...");//tokenSecret

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = defaultSharedPreferences.getString(getString(R.string.pocket_access_token_pref), "");
/*
        String tokenSecret = defaultSharedPreferences.getString(getString(R.string.li_token_secret_pref), "");
        pocketService.setAccessToken(token, tokenSecret);
        //TODO if empty error
        showProfile();
*/
    }

    private void doOAuth() {
        new OauthRequestAsynkTaskTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //From launchOAuthProccess
        super.onActivityResult(requestCode, resultCode, data);

        System.out.println("return from webview");
        setResult(RESULT_OK);
        finish();

    }

    //TODO utils
    public Bundle getBundleFromMap(HashMap<String, String> map) {
        Bundle bundle = new Bundle();
        for (String key : map.keySet()) {
            bundle.putString(key, map.get(key));
        }
        return bundle;
    }

    private void saveAccessToken(Object accessToken) {
        //Save accessToken to preferences
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = defaultSharedPreferences.edit();
/*
        editor.putString(getString(R.string.pocket_access_token_pref), ...);
        editor.putString(getString(R.string.li_token_secret_pref), ...);
*/
        editor.apply();
    }

    //TODO do request (rest -> https://getpocket.com/v3/oauth/request)
    private void launchOAuthProccess(String authUrl) {
        String data = new PocketServiceImpl(this, null, null).getRequestToken("url");

    }

    private void startProggressDialog() {
/*
        progDailog = ProgressDialog.show(this,
                getString(R.string.wait_title),
                getString(R.string.wait_message), false);
*/
        progDailog = ProgressDialog.show(this,
                "wait titl",
                "eait mesage", false);
    }

    @Override
    public void notifyActionFinish(String action, Object data) {
        System.out.println("requestToken = " + data);

        String code = null;
        try {
            JSONObject jObject = new JSONObject((String) data);
            code = (String) jObject.get("code");
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(this, PocketOAuthWebViewActivity.class);
        String url = "https://getpocket.com/auth/authorize?request_token=" + code + "&redirect_uri=pocket1234://authFinished";
        intent.putExtra(PocketService.REQUEST_TOKEN, code);
        intent.putExtra(PocketService.KEY_AUTHORIZATION_URL, url);
        //Open web browser to authenticate
        startActivityForResult(intent, 1);

/*
        Intent intent = new Intent(this, PocketOAuthWebViewActivity.class);
        intent.putExtra(PocketService.KEY_AUTHORIZATION_URL, authUrl);
        //Open web browser to authenticate
        startActivityForResult(intent, 1);
*/
    }



    abstract class OauthAsynkGenericTask extends AsyncTask {
        protected Exception exception;
        @Override
        protected void onPreExecute() {
            startProggressDialog();
        }
    }

    class OauthRequestAsynkTaskTask extends OauthAsynkGenericTask {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Object result) {
            launchOAuthProccess((String) result);
       }

        @Override
        protected Object doInBackground(Object... /*not used*/params) {
            try {
                return "url";
//                return pocketService.getOAuthUrl();
            } catch (Exception e) {
                Log.e(OAuthActivity.class.getSimpleName(), e.getMessage(), e);
                this.exception = e;
                return null;
            }
        }
    }

    class OauthAsynkForAccessTokenTask extends OauthAsynkGenericTask {

        @Override
        protected Object doInBackground(Object... params) {
            try {
                //TODO launch webActivity
                String pinCode = (String) params[0];
                Toast.makeText(getApplicationContext(), "OauthAsynkForAccessTokenTask", Toast.LENGTH_LONG).show();
                return "";
/*                return pocketService.getAccessTokenWithPinCode(pinCode);*/
            } catch (Exception e) {
                Log.e(OAuthActivity.class.getSimpleName(), e.getMessage(), e);
                this.exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            progDailog.dismiss();
            Toast.makeText(getApplicationContext(), "saveAccessToken", Toast.LENGTH_LONG).show();
            showProfile();
        }
    }

   class ShowResultAsynkTaskTask extends OauthAsynkGenericTask {

        @Override
        protected Object doInBackground(Object... /*not used*/params) {
            try {
                /*return pocketService.getProfile();*/
                Toast.makeText(getApplicationContext(), "getProfile", Toast.LENGTH_LONG).show();
                return "";
            } catch (Exception e) {
                Log.e(OAuthActivity.class.getSimpleName(), e.getMessage(), e);
                this.exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            progDailog.dismiss();
            showProfile(result);
        }
    }


}