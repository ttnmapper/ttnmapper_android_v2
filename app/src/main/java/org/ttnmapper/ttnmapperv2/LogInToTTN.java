package org.ttnmapper.ttnmapperv2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class LogInToTTN extends AppCompatActivity {

    final String TAG = "LogInToTTN";
    final String secretState = "secret" + new Random().nextInt(999_999);

    private String clientId;
    private String clientSecret;
    private String redirectURI;

    private OAuth2AccessToken accessToken;

    private OAuth20Service service;

    @SuppressWarnings("deprecation")
    public static void clearCookies(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

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

        Answers.getInstance().logCustom(new CustomEvent("Device configure").putCustomAttribute("method", "oauth2"));

        loadLoginPage();
    }

    public void setStatusMessage(final String status)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WebView webview = (WebView) findViewById(R.id.webViewTTNlogin);
                webview.setVisibility(View.GONE);

                TextView textView = (TextView) findViewById(R.id.textViewStatus);
                textView.setVisibility(View.VISIBLE);
                textView.setText(status);
                Log.d(TAG, status);
            }
        });
    }

    public void enableRetryButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button = (Button) findViewById(R.id.buttonRetry);
                button.setVisibility(View.VISIBLE);

                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBarLogIn);
                progressBar.setVisibility(View.GONE);

            }
        });
    }

    public void loadLoginPage()
    {
        final String authorizationUrl = service.getAuthorizationUrl();

        clearCookies(this);

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

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d(TAG, "webview error:" + errorCode);
                Log.d(TAG, "webview description:" + description);
                Log.d(TAG, "webview failingurl:" + failingUrl);

                setStatusMessage("Failed to load TTN login page. Check your internet conection.\n\n" +
                        "error code: " + errorCode + "\n" +
                        "description: " + description + "\n" +
                        "failing URL: " + failingUrl);
                enableRetryButton();

                super.onReceivedError(view, errorCode, description, failingUrl);
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

        Map<String, String> callbackData = getQueryParameter(Uri.parse(url));
        if(!callbackData.containsKey("state"))
        {
            Log.d(TAG, "Response does not contain a secret state. Please try again.");
            textView.setText("Response does not contain a secret state. Please try again.");
            progressBar.setVisibility(View.GONE);
            enableRetryButton();
        }
        else if(!callbackData.get("state").equals(secretState))
        {
            Log.d(TAG, "Secret state does not match. "+callbackData.get("state")+" != "+secretState);
            textView.setText("Secret state does not match. Please try again.");
            progressBar.setVisibility(View.GONE);
            enableRetryButton();
        }
        else if(!callbackData.containsKey("code"))
        {
            Log.d(TAG, "Response does not contain a code. Please try again.");
            textView.setText("Response does not contain a code. Please try again.");
            progressBar.setVisibility(View.GONE);
            enableRetryButton();
        }
        else if(callbackData.get("code").equals(""))
        {
            Log.d(TAG, "Invalid code in response. Please try again.");
            textView.setText("Invalid code in response. Please try again.");
            progressBar.setVisibility(View.GONE);
            enableRetryButton();
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

    /**
     * Return a map of argument->value from a query in a URI
     *
     * @param uri The URI
     */
    private Map<String, String> getQueryParameter(Uri uri) {
        if (uri.isOpaque()) {
            return Collections.emptyMap();
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptyMap();
        }

        Map<String, String> parameters = new LinkedHashMap<>();
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

    private class getTokenWithCode extends AsyncTask<String, String, OAuth2AccessToken> {

        protected OAuth2AccessToken doInBackground(String... code) {
            try {
                return service.getAccessToken(code[0]);
            } catch (IOException e) {
                e.printStackTrace();
                setStatusMessage("Failed to exchange code for a token.");
                enableRetryButton();
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

    private class getApplications extends AsyncTask<String, String, Boolean> {

        protected Boolean doInBackground(String... strings) {
            OAuthRequest request = new OAuthRequest(Verb.GET, "https://account.thethingsnetwork.org/applications", service);

            service.signRequest(accessToken, request);
            request.addHeader("Accept", "application/json");

            try {
                final Response response = request.send();
                setStatusMessage("Application GET response code=" + response.getCode());

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
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                setStatusMessage("ERROR while getting list of Applications. Check your internet connection.");
                enableRetryButton();
                return false;
            }
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                new getHandlers().execute();
            }
        }
    }

    private class getHandlers extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            setStatusMessage("Discovering handlers");

            MyApplication mApplication = (MyApplication)getApplicationContext();
            OAuthRequest request = new OAuthRequest(Verb.GET, "http://discovery.thethingsnetwork.org:8080/announcements/handler", service);

            service.signRequest(accessToken, request);
            request.addHeader("Accept", "application/json");

            try {
                final Response response = request.send();

                JSONObject resultData = new JSONObject(response.getBody());
                JSONArray handlers = resultData.getJSONArray("services");

                for(int i=0; i<handlers.length(); i++)
                {
                    JSONObject currentHandler = handlers.getJSONObject(i);
                    String handlerID = currentHandler.getString("id");
                    Log.d(TAG, "Handler: " + handlerID);

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

                    if (currentHandler.has("metadata")) {
                        JSONArray apps = currentHandler.getJSONArray("metadata");

                        for (int j = 0; j < apps.length(); j++) {
                            String appID = apps.getJSONObject(j).getString("app_id");

                            for (TTNApplication localApp : mApplication.ttnApplications) {
                                if (localApp.getId().equals(appID)) {
                                    localApp.setHandler(handlerID);
                                    localApp.setMqttAddress(mqttAddress);
                                    localApp.setApiAddress(apiAddress);
                                    localApp.setNetAddress(netAddress);
                                }
                            }
                        }
                    }
                }

                return true;

            } catch (Exception e) {
                e.printStackTrace();
                setStatusMessage("ERROR while discovering handlers. Maybe the discovery server is offline, or this service is blocked on your internet connection.");
                enableRetryButton();
                return false;
            }
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                new getDevices().execute();
            } else {
                //error
            }
        }
    }

    //applications/{app_id}/devices
    //http://eu.thethings.network:8084/applications/jpm_testing/devices
    private class getDevices extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            MyApplication mApplication = (MyApplication)getApplicationContext();
            Boolean result = true;

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
                Log.d(TAG, "sending request");

                try {
                    final Response response = request.send();
                    Log.d(TAG, "Restricted token received");
                    restrictedToken = new JSONObject(response.getBody()).getString("access_token");

                } catch (Exception e) {
                    e.printStackTrace();
                    setStatusMessage("Failed to get a restricted token for app" + currentApp.getId() + ". Check your internet connection.");
                    result = false;
                }

                String URL = currentApp.getApiAddress()+"/applications/"+currentApp.getId()+"/devices";
                Log.d(TAG, "Fetching "+URL);
                request = new OAuthRequest(Verb.GET, URL, service);

                //service.signRequest(accessToken, request);
                request.addHeader("Accept", "application/json");
                request.addHeader("Authorization", "Bearer "+restrictedToken);

                try {
                    final Response devicesResponse = request.send();

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
                    Log.d(TAG, devicesResponse.getBody());
                    JSONArray devices = new JSONObject(devicesResponse.getBody()).getJSONArray("devices");
                    for(int i=0; i<devices.length(); i++)
                    {
                        currentApp.addDevice(devices.getJSONObject(i).getString("dev_id"));
                        Log.d(TAG, devices.getJSONObject(i).getString("dev_id"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setStatusMessage("ERROR while getting list of devices for app " + currentApp.getId());
                    result = false;
                }

            }

            return result;
        }

        protected void onPostExecute(Boolean result) {
            if (!result) {
                Toast.makeText(getApplicationContext(), "Can not retrieve device list", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(getApplicationContext(), ApplicationList.class);
            startActivity(intent);
            finish();
        }
    }
}
