package com.feedvids.controller.oauth;

import android.app.Activity;
import android.os.Bundle;

import com.feedvids.controller.R;

/**
 * @author luismoramedina
 */
public class OAuthLauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauth_launch_activity);
        System.out.println("setting token");
        getSharedPreferences("com.feedvids", MODE_PRIVATE).edit().putString("OAUTH_ACCESS_TOKEN", "something").commit();
        setResult(RESULT_OK);
        finish();
    }
}
