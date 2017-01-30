package org.ttnmapper.ttnmapperv2;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class LogInToTTN extends AppCompatActivity {

    final String TAG = "LogInToTTN";
    final String secretState = "secret" + new Random().nextInt(999_999);

    private String clientId;
    private String clientSecret;
    private String redirectURI;

    private OAuth2AccessToken accessToken;

    private OAuth20Service service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in_to_ttn);

        clientId = getString(R.string.oauth_client_id);
        clientSecret = getString(R.string.oauth_client_secret);
        redirectURI = getString(R.string.oauth_redirect_url);

        service = new ServiceBuilder()
                .apiKey(clientId)
                .apiSecret(clientSecret)
                .state(secretState)
                .callback(redirectURI)
                .build(TheThingsNetworkOathApi.instance());

        MyApplication mApplication = (MyApplication)getApplicationContext();
        mApplication.ttnApplications = new ArrayList<>();
        mApplication.chosenTtnApplication = null;

        loadLoginPage();
    }

    public void setStatusMessage(final String status)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.textViewStatus);
                textView.setText(status);
                Log.d(TAG, status);
            }
        });
    }

    public void loadLoginPage()
    {
        final String authorizationUrl = service.getAuthorizationUrl();

        WebView webview = (WebView) findViewById(R.id.webViewTTNlogin);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                if(url.startsWith(redirectURI))
                {
                    checkCallbackURL(url);
                }
                else {
                    view.loadUrl(url);
                }
                return false; // then it is not handled by default action
            }
        });

        webview.loadUrl(authorizationUrl);
    }

    public void checkCallbackURL(String url)
    {
        //hide web view, show progress norification
        Log.d(TAG, "Redirected: "+url);
        WebView webview = (WebView) findViewById(R.id.webViewTTNlogin);
        webview.setVisibility(View.GONE);

        TextView textView = (TextView) findViewById(R.id.textViewStatus);
        textView.setVisibility(View.VISIBLE);
        textView.setText("Redirected back from TTN login page");

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBarLogIn);
        progressBar.setVisibility(View.VISIBLE);

        Button button = (Button) findViewById(R.id.buttonRetry);

        Map<String, String> callbackData = getQueryParameter(Uri.parse(url));
        if(!callbackData.containsKey("state"))
        {
            Log.d(TAG, "Response does not contain a secret state. Please try again.");
            textView.setText("Response does not contain a secret state. Please try again.");
            progressBar.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
        }
        else if(!callbackData.get("state").equals(secretState))
        {
            Log.d(TAG, "Secret state does not match. "+callbackData.get("state")+" != "+secretState);
            textView.setText("Secret state does not match. Please try again.");
            progressBar.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
        }
        else if(!callbackData.containsKey("code"))
        {
            Log.d(TAG, "Response does not contain a code. Please try again.");
            textView.setText("Response does not contain a code. Please try again.");
            progressBar.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
        }
        else if(callbackData.get("code").equals(""))
        {
            Log.d(TAG, "Invalid code in response. Please try again.");
            textView.setText("Invalid code in response. Please try again.");
            progressBar.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
        }
        else
        {
            Log.d(TAG, "Log in successful. Loading list of application.");
            textView.setText("Logged in successfully. Getting token for key.");
            final String code = callbackData.get("code");
            new getTokenWithCode().execute(code);
        }
    }

    public void onButtonRetryClicked(View v)
    {
        WebView webview = (WebView) findViewById(R.id.webViewTTNlogin);
        webview.setVisibility(View.VISIBLE);

        TextView textView = (TextView) findViewById(R.id.textViewStatus);
        textView.setVisibility(View.GONE);

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBarLogIn);
        progressBar.setVisibility(View.GONE);

        Button button = (Button) findViewById(R.id.buttonRetry);
        button.setVisibility(View.GONE);

        loadLoginPage();
    }

    private class getTokenWithCode extends AsyncTask<String, String, OAuth2AccessToken> {

        protected OAuth2AccessToken doInBackground(String... code) {
            try {
                return service.getAccessToken(code[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(OAuth2AccessToken result) {
            if(result!=null)
            {
                accessToken = result;
                new getApplications().execute("");
                setStatusMessage("Token received");
            }

        }
    }

    private class getApplications extends AsyncTask<String, String, String> {

        protected String doInBackground(String... strings) {
            OAuthRequest request = new OAuthRequest(Verb.GET, "https://account.thethingsnetwork.org/applications", service);

            service.signRequest(accessToken, request);
            request.addHeader("Accept", "application/json");
            final Response response = request.send();

            setStatusMessage("Application GET response code="+response.getCode());

            try {
                if (response.getCode() == 401) {
                    setStatusMessage("Not authorized. Try again.");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBarLogIn);
                            Button button = (Button) findViewById(R.id.buttonRetry);
                            progressBar.setVisibility(View.GONE);
                            button.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    setStatusMessage("Getting list of applications");
                    /*
                    [
                        {"id":"sodaq_testing",
                         "name":"van alles",
                         "euis":["70B3D57EF0000187"],
                         "created":"2016-11-24T11:09:51.799Z",
                         "rights":["settings","devices","collaborators"],
                         "collaborators":[{"username":"jpmeijers","email":"ttn@jpmeijers.com","rights":["settings","devices","collaborators"]},{"username":"laurens","email":"laurens@thethingsnetwork.org","rights":["settings","devices"]},{"username":"paulb","email":"paulb@dds.nl","rights":["settings","delete","collaborators","devices"]}],
                         "access_keys":[{"name":"default key","rights":["messages:up:r","messages:down:w"],"key":"ttn-account-v2.r4sNAmTRbF-QRRbrS2l6sT-u9U00RtaDo307UmnlGB4"},
                                        {"name":"sesame","rights":["messages:up:r","messages:down:w","settings","devices"],"key":"ttn-account-v2.xp0e7CJgvdsfDjY2U2IjL-CDT4GeFwQqTPIPHylfGIs"}
                                       ]
                        },
                        {"id":"jpm_mapping_nodes","name":"joris binary format mapping nodes","euis":["70B3D57EF0001D09"],"created":"2016-12-23T15:13:33.199Z",
                         "rights":["settings","delete","collaborators","devices"],
                         "collaborators":[{"username":"jpmeijers","email":"ttn@jpmeijers.com","rights":["settings","delete","collaborators","devices"]}],
                         "access_keys":[{"name":"default key","key":"ttn-account-v2.UsPtrxlB7BFjaZoTY8HKPOQxSv8ax3tBnih7hJEZInA","_id":"585d3f1def783e0032f3a904","rights":["messages:up:r","messages:down:w"]}]},
                        {"id":"jpm_testing","name":"general testing nodes","euis":["70B3D57EF0003304"],"created":"2017-01-17T14:47:09.740Z","rights":["settings","delete","collaborators","devices"],"collaborators":[{"username":"jpmeijers","email":"ttn@jpmeijers.com","rights":["settings","delete","collaborators","devices"]}],"access_keys":[{"name":"default key","key":"ttn-account-v2.76hcK6z-BE2aZwjjB9AcotZq2p9JDtN-MmMGB2jZe1w","_id":"587e2e6dbc438d00317f1544","rights":["messages:up:r","messages:down:w"]}]}
                    ]
                    */
                    JSONArray receivedData = new JSONArray(response.getBody());
                    for (int i = 0; i < receivedData.length(); i++) {
                        JSONObject applicationData = receivedData.getJSONObject(i);
                        TTNApplication application = new TTNApplication(applicationData.getString("id"));
                        application.setName(applicationData.getString("name"));
                        Log.d(TAG, "Processing application id="+application.getId());

                        JSONArray accessKeys = applicationData.getJSONArray("access_keys");
                        for(int j=0; j<accessKeys.length(); j++)
                        {
                            JSONObject accessKey = accessKeys.getJSONObject(j);
                            Log.d(TAG, "Checking rights for key name="+accessKey.getString("name"));

                            JSONArray rights = accessKey.getJSONArray("rights");
                            for(int k=0; k<rights.length(); k++)
                            {
                                if(rights.getString(k).equals("messages:up:r"))
                                {
                                    application.setAccessKey(accessKey.getString("key"));
                                    Log.d(TAG, "AccessKey set to "+application.getAccessKey());
                                }
                                if(rights.getString(k).equals("devices"))
                                {
                                    application.setDevicesKey(accessKey.getString("key"));
                                    Log.d(TAG, "DevicesKey set to "+application.getDevicesKey());
                                }
                            }
                        }

                        MyApplication mApplication = (MyApplication)getApplicationContext();
                        mApplication.ttnApplications.add(application);

                    }
                }
                return null;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            new getHandlers().execute();
        }
    }

    private class getHandlers extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            setStatusMessage("Discovering handlers");

            MyApplication mApplication = (MyApplication)getApplicationContext();
            OAuthRequest request = new OAuthRequest(Verb.GET, "http://discovery.thethingsnetwork.org:8080/announcements/handler", service);

            service.signRequest(accessToken, request);
            request.addHeader("Accept", "application/json");
            final Response response = request.send();

            try {
                JSONObject resultData = new JSONObject(response.getBody());
                JSONArray handlers = resultData.getJSONArray("services");

                for(int i=0; i<handlers.length(); i++)
                {
                    JSONObject currentHandler = handlers.getJSONObject(i);
                    String handlerID = currentHandler.getString("id");

                    String mqttAddress = null;
                    if(currentHandler.has("mqtt_address")) {
                        mqttAddress = currentHandler.getString("mqtt_address");
                    }
                    else
                    {
                        Log.d(TAG, "Handler="+handlerID+" does not have a MQTT address");
                    }

                    String netAddress = null;
                    if(currentHandler.has("net_address")) {
                        netAddress = currentHandler.getString("net_address");
                    }
                    else
                    {
                        Log.d(TAG, "Handler="+handlerID+" does not have a Net address");
                    }

                    String apiAddress = null;
                    if(currentHandler.has("api_address")) {
                        apiAddress = currentHandler.getString("api_address");
                    }
                    else
                    {
                        Log.d(TAG, "Handler="+handlerID+" does not have an API address");
                    }

                    JSONArray apps = currentHandler.getJSONArray("metadata");

                    for(int j=0; j<apps.length(); j++)
                    {
                        String appID = apps.getJSONObject(j).getString("app_id");

                        for(TTNApplication localApp : mApplication.ttnApplications)
                        {
                            if(localApp.getId().equals(appID))
                            {
                                localApp.setHandler(handlerID);
                                localApp.setMqttAddress(mqttAddress);
                                localApp.setApiAddress(apiAddress);
                                localApp.setNetAddress(netAddress);
                            }
                        }
                    }
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            new getDevices().execute();
        }
    }

    //applications/{app_id}/devices
    //http://eu.thethings.network:8084/applications/jpm_testing/devices
    private class getDevices extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            MyApplication mApplication = (MyApplication)getApplicationContext();

            for (TTNApplication currentApp : mApplication.ttnApplications)
            {
                setStatusMessage("Getting list of devices for "+currentApp.getId());

                if(currentApp.getApiAddress()==null || currentApp.getApiAddress().equals(""))
                {
                    //this handler does not have a api
                    Log.d(TAG, "Application does not have an api address");
                    continue;
                }


                //get restricted token
                OAuthRequest request = new OAuthRequest(Verb.POST, "https://account.thethingsnetwork.org/users/restrict-token", service);
                service.signRequest(accessToken, request);
                request.addHeader("Accept", "application/json");
                request.addHeader("Content-Type", "application/json;charset=UTF-8");
                request.addPayload("{\"scope\": [ \"apps:"+currentApp.getId()+"\" ]}");
                //request.addBodyParameter("scope", "apps:"+currentApp.getId());

                String restrictedToken = "";

                final Response response = request.send();

                try {
                    restrictedToken = new JSONObject(response.getBody()).getString("access_token");

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

                String URL = currentApp.getApiAddress()+"/applications/"+currentApp.getId()+"/devices";
                Log.d(TAG, "Fetching "+URL);
                request = new OAuthRequest(Verb.GET, URL, service);

                //service.signRequest(accessToken, request);
                request.addHeader("Accept", "application/json");
                request.addHeader("Authorization", "Bearer "+restrictedToken);

                final Response devicesResponse = request.send();

                try {
                    /* {"devices":
                       [
                         {"app_id":"jpm_mapping_nodes",
                          "dev_id":"pink_kosblik",
                          "lorawan_device":
                          {
                            "app_eui":"70B3D57EF0001D09",
                            "dev_eui":"00CF85F175DD45CD",
                            "app_id":"jpm_mapping_nodes",
                            "dev_id":"pink_kosblik",
                            "dev_addr":"260110CC",
                            "nwk_s_key":"A998A0E5645663A9AAF58B58B36A9BA1",
                            "app_s_key":"41FA07B9FC5E6367DCDB0ACBC0155776",
                            "app_key":""
                          }
                         },
                         {"app_id":"jpm_mapping_nodes","dev_id":"rfm_teensy3","lorawan_device":{"app_eui":"70B3D57EF0001D09","dev_eui":"00CC186F83CD4492","app_id":"jpm_mapping_nodes","dev_id":"rfm_teensy3","dev_addr":"26011256","nwk_s_key":"F227888F5F74AA2DAC372BA8D53D20FC","app_s_key":"0AF44FB198F7B3DA1204F6A2EC2080D7","app_key":""}},
                         {"app_id":"jpm_mapping_nodes","dev_id":"thingsuno_gpsshield","lorawan_device":{"app_eui":"70B3D57EF0001D09","dev_eui":"006259C6A72A76AA","app_id":"jpm_mapping_nodes","dev_id":"thingsuno_gpsshield","dev_addr":"260115A6","nwk_s_key":"32551FF1817DDDD7BD2FF4307F51D408","app_s_key":"18DAA70AE22D0B9B100140E2766EBE7D","app_key":""}},
                         {"app_id":"jpm_mapping_nodes","dev_id":"tnt_sodaq_one","lorawan_device":{"app_eui":"70B3D57EF0001D09","dev_eui":"007DBBB13067B880","app_id":"jpm_mapping_nodes","dev_id":"tnt_sodaq_one","dev_addr":"260112B2","nwk_s_key":"ADDF9C3BDA818AAD7922F3A2C38FA3ED","app_s_key":"20B8EE18721453C6D04F564D5329FC94","app_key":""}}
                       ]
                       }
                    */
                    JSONArray devices = new JSONObject(devicesResponse.getBody()).getJSONArray("devices");
                    for(int i=0; i<devices.length(); i++)
                    {
                        currentApp.addDevice(devices.getJSONObject(i).getString("dev_id"));
                        Log.d(TAG, devices.getJSONObject(i).getString("dev_id"));
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

            }

            return null;
        }

        protected void onPostExecute(String result) {
            Intent intent = new Intent(getApplicationContext(), ApplicationList.class);
            startActivity(intent);
            finish();
        }
    }


    /**
     * Return a map of argument->value from a query in a URI
     * @param uri The URI
     */
    private Map<String,String> getQueryParameter(Uri uri) {
        if (uri.isOpaque()) {
            return Collections.emptyMap();
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptyMap();
        }

        Map<String,String> parameters = new LinkedHashMap<String,String>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);
            String value;
            if (separator < end)
                value = query.substring(separator + 1, end);
            else
                value = "";

            parameters.put(Uri.decode(name), Uri.decode(value));

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableMap(parameters);
    }
}
