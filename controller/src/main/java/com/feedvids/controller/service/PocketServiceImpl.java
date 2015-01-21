package com.feedvids.controller.service;

import android.os.AsyncTask;

import com.feedvids.controller.Notificable;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author luismoramedina
 */
public class PocketServiceImpl implements PocketService {

    private String consumerKey;
    private String accessToken;
    Notificable notificable;

    public PocketServiceImpl() {
    }

    public PocketServiceImpl (Notificable notifcable, String consumerKey, String access_token) {
        this.notificable = notifcable;
        this.consumerKey = consumerKey;
        this.accessToken = access_token;
    }

    @Override
    public String retrieve() {
/*
    post_data = {'consumerKey': consumerKey,
            'accessToken': accessToken,
            'contentType': 'video',
            'sort': 'newest',
            'detailType': 'complete'}

    #TODO
    post_data['count'] = 100

            logging.info('Before doPost retrieve')
    data = doPost('https://getpocket.com/v3/get', post_data)
    logging.info('After doPost retrieve')
            return data
*/
        String data = "{\"consumer_key\": \"" + consumerKey
                + "\", \"accessToken\": \"" + accessToken
                + "\", \"contentType\": \"video\""
                + ", \"sort\": \"newest\""
                + ", \"detailType\": \"complete\""
                + ", \"count\": \"100\"" +
                "}";

        String uri = "https://getpocket.com/v3/get";

        HttpTask httpTask = new HttpTask();
        httpTask.execute(uri, data, "retrieve");
        data = httpTask.getResponse();

        return data;
    }

    private String getResponseData(HttpResponse response) throws IOException {
        String data = null;

        if (response.getStatusLine().getStatusCode() == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder sb = new StringBuilder("");
            String line;
            String lineSeparator = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line).append(lineSeparator);
            }
            in.close();
            data = sb.toString();
        } else {
            System.out.println(response.getStatusLine().getStatusCode());
        }
        return data;
    }

    @Override
    public String modify(String action, String item) {
        return null;
    }

    @Override
    public String archive(String item) {
        return null;
    }

    @Override
    public String delete(String item) {
        String action = "delete";
        String message = "[{\"action\": \"" + action + "\", \"item_id\": \"" + item + "\"}]";

        String data = "{\"consumer_key\": \"" + consumerKey
                + "\", \"accessToken\": \"" + accessToken
                + "\", \"actions\": " + message +
                "}";

        String uri = "https://getpocket.com/v3/send";

        HttpTask httpTask = new HttpTask();
        httpTask.execute(uri, data, action);
        data = httpTask.getResponse();

        return data;
    }

    @Override
    public String getRequestToken(String redirectUrl) {
        String data = "{\"consumer_key\": \"" + consumerKey
                + "\", \"redirect_uri\": \"" + redirectUrl
                + "\"}";

        String uri = "https://getpocket.com/v3/oauth/request";

        HttpTask httpTask = new HttpTask();
        httpTask.execute(uri, data, "getRequestToken");
        data = httpTask.getResponse();

        return data;
    }

    @Override
    public String authorize(String code) {
        String data = "{\"consumer_key\": \"" + consumerKey
                + "\", \"code\": \"" + code
                + "\"}";

        String uri = "https://getpocket.com/v3/oauth/authorize";

        HttpTask httpTask = new HttpTask();
        httpTask.execute(uri, data);
        data = httpTask.getResponse();

        return data;

    }

    private class HttpTask extends AsyncTask<String, Void, String[]> {
        private String data;

        public String getResponse () {
            return data;
        }
        // Do the long-running work in here
        protected String[] doInBackground(String... uriAndParams) {
            String action = null;
            try {
                HttpPost httpPost = new HttpPost(uriAndParams[0]);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("X-Accept", "application/json");

                httpPost.setEntity(new StringEntity(uriAndParams[1]));
                if (uriAndParams.length > 2) {
                    action = uriAndParams[2];
                }
                HttpClient httpClient = new DefaultHttpClient();


                HttpResponse response;
                response = httpClient.execute(httpPost);
                data = getResponseData(response);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return new String[]{data, action};
        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(String... dataAndAction) {
            System.out.println("data = " + data);
            if (notificable != null) {
                notificable.notifyActionFinish( dataAndAction[1], dataAndAction[0]);
            }
        }

    }
}
