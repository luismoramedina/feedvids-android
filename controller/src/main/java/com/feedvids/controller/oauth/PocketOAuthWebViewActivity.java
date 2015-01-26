package com.feedvids.controller.oauth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.feedvids.controller.Notificable;
import com.feedvids.controller.R;
import com.feedvids.controller.service.PocketService;
import com.feedvids.controller.service.PocketServiceImpl;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 29/10/12
 * Time: 22:26
 * To change this template use File | Settings | File Templates.
 */
public class PocketOAuthWebViewActivity extends Activity {

    private String code;
    private boolean isWorking;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.oauthwebview);
        final WebView webView = (WebView) findViewById(R.id.oauthwebview);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // Activities and WebViews measure progress with different scales.
                // The progress meter will automatically disappear when we reach 100%
                setProgress(newProgress * 100);
            }

        });

        webView.setWebViewClient(new WebViewClient() {



            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.startsWith("pocket") && !isWorking) {
                    isWorking = true;
                    new PocketServiceImpl(new Notificable() {
                        @Override
                        public void notifyActionFinish(String action, Object data) {
                            //Toast.makeText(getApplicationContext(), (CharSequence) data, Toast.LENGTH_LONG).show();
                            try {
                                JSONObject jsonObject = new JSONObject(data.toString());
                                String accessToken = (String) jsonObject.get("access_token");
                                System.out.println("setting token");
                                getSharedPreferences("com.feedvids", MODE_PRIVATE).edit().putString("OAUTH_ACCESS_TOKEN", accessToken).commit();
                                setResult(RESULT_OK);
                                finish();

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, getString(R.string.consumer_key), null).authorize(code);
                }
            }
        });


        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setAppCacheEnabled(true); // the important change
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        code = this.getIntent().getExtras().getString(PocketService.REQUEST_TOKEN);
        webView.loadUrl(this.getIntent().getExtras().getString(PocketService.KEY_AUTHORIZATION_URL));

    }

    //TODO not working from inside WebView
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Object uri = intent.getData();
        System.out.println("URI=" + uri);
    }
}
