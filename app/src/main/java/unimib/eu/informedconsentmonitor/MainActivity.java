package unimib.eu.informedconsentmonitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.androidplot.xy.XYPlot;
import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog;
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.ShimmerDevice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog.EXTRA_DEVICE_ADDRESS;

public class MainActivity extends Activity {

    private final static String LOG_TAG = "Shimmer";
    private final static int CAMERA_PERMISSIONS_REQUEST = 10;

    protected Button connectBtn;
    protected ToggleButton streamBtn;
    protected ToggleButton debugBtn;
    protected TableLayout statsTable;
    protected WebView webView;
    protected XYPlot dynamicPlot;

    ShimmerBluetoothManagerAndroid btManager;
    ShimmerDevice shimmerDevice;
    String shimmerBtAdd;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Layout elements
        connectBtn = findViewById(R.id.connect_btn);
        webView = findViewById(R.id.webview);
        statsTable = findViewById(R.id.debug_stats);
        dynamicPlot = findViewById(R.id.dynamicPlot);

        try {
            btManager = new ShimmerBluetoothManagerAndroid(this, mHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Couldn't create ShimmerBluetoothManagerAndroid. Error: " + e);
        }

        connectBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick (View v)
            {
                connectDevice(statsTable);
            }
        });

        webView.clearCache(false);
        // Added only to be able to debug the application through chrome://inspect
        WebView.setWebContentsDebuggingEnabled(true);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        CAMERA_PERMISSIONS_REQUEST);

            }
        } else {
            // Permission has already been granted
            // Open front facing camera
            Camera.open(1);
        }

        // We inject the needed javascript on every loaded page
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView webView, String url) {
                super.onPageFinished(webView, url);
                injectScriptFile(webView, "timeme.js");
                injectScriptFile(webView, "scrolldetect.js");
                //injectScriptFile(webView, "webgazer.js");
                //injectScriptFile(webView, "inject.js");
            }


        });
        webView.setWebChromeClient(new WebChromeClient() {
            // Grant permissions for cam
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new CustomJavaScriptInterface(this, webView), "Native");
        webView.loadUrl("http://ericab12.altervista.org/new-informed-consent/login.php");
        // TODO remove. testing on localhost due to this "https://sites.google.com/a/chromium.org/dev/Home/chromium-security/deprecating-powerful-features-on-insecure-origins"
        //webView.loadUrl("file:///android_asset/index.html");

        // We set the debug button to hide or show the statistics screen
        debugBtn = findViewById(R.id.debug_btn);
        debugBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (((ToggleButton) v).isChecked())
                    statsTable.setVisibility(View.VISIBLE);
                else
                    statsTable.setVisibility(View.GONE);
            }
        });

        // We set the stream button to start and stop streaming functions
        streamBtn = findViewById(R.id.stream_btn);
        streamBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (((ToggleButton) v).isChecked())
                    startStreaming(v);
                else
                    stopStreaming(v);
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void injectScriptFile(WebView view, String scriptFile) {
        InputStream input;
        try {
            input = getAssets().open(scriptFile);
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            input.close();

            // String-ify the script byte-array using BASE64 encoding !!!
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            view.evaluateJavascript("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "script.innerHTML = decodeURIComponent(escape(window.atob('" + encoded + "')));" +
                    "parent.appendChild(script)" +
                    "})()", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    // noop
                }
            });

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void connectDevice(View v) {

        /*
        // We check if the bluetooth is ON
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            updateDebugText((TextView) findViewById(R.id.debug_shimmer), "Bluetooth not supported");
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enabled
                updateDebugText((TextView) findViewById(R.id.debug_shimmer), "Bluetooth turned off");
            }
            else{
                connectDevice(statsTable);
            }
        }
        */

        Intent intent = new Intent(getApplicationContext(), ShimmerBluetoothDialog.class);
        startActivityForResult(intent, ShimmerBluetoothDialog.REQUEST_CONNECT_SHIMMER);
    }

    public void startSDLogging(View v) {
        ((ShimmerBluetooth)shimmerDevice).writeConfigTime(System.currentTimeMillis());
        shimmerDevice.startSDLogging();
    }

    public void stopSDLogging(View v) {
        shimmerDevice.stopSDLogging();
    }

    public void stopStreaming(View v){
        shimmerDevice.stopStreaming();
    }

    public void startStreaming(View v){
        shimmerDevice.startStreaming();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    // Open front facing camera
                    Camera.open(1);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    /**
     * Messages from the Shimmer device including sensor data are received here
     */
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET:
                    if ((msg.obj instanceof ObjectCluster)) {

                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;

                        //Retrieve all possible formats for the current sensor device:
                        Collection<FormatCluster> allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP);
                        FormatCluster timeStampCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        double timeStampData = timeStampCluster.mData;
                        Log.i(LOG_TAG, "TIMESTAMP: " + timeStampData);

                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_RESISTANCE);
                        FormatCluster gsrResistance = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (gsrResistance!=null) {
                            double GSRData = gsrResistance.mData;
                            Log.i(LOG_TAG, "GSR_RESISTANCE: " + GSRData);
                        }

                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE);
                        FormatCluster gsrConductance = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (gsrConductance!=null) {
                            double GSRData = gsrConductance.mData;
                            Log.i(LOG_TAG, "GSR_CONDUCTANCE: " + GSRData);
                        }

                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.ECG_LL_RA_24BIT);
                        FormatCluster ECG_LL_RA = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (ECG_LL_RA!=null) {
                            double ECGData = ECG_LL_RA.mData;
                            Log.i(LOG_TAG, "ECG_LL_RA_24BIT: " + ECGData);
                        }

                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.ECG_LL_LA_24BIT);
                        FormatCluster ECG_LL_LA = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (ECG_LL_LA!=null) {
                            double ECGData = ECG_LL_LA.mData;
                            Log.i(LOG_TAG, "ECG_LL_LA_24BIT: " + ECGData);
                        }
                    }
                    dynamicPlot.redraw();
                    break;
                case Shimmer.MESSAGE_TOAST:
                    /** Toast messages sent from {@link Shimmer} are received here. E.g. device xxxx now streaming.
                     *  Note that display of these Toast messages is done automatically in the Handler in {@link com.shimmerresearch.android.shimmerService.ShimmerService} */
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Shimmer.TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE:
                    ShimmerBluetooth.BT_STATE state = null;
                    String macAddress = "";

                    if (msg.obj instanceof ObjectCluster) {
                        state = ((ObjectCluster) msg.obj).mState;
                        macAddress = ((ObjectCluster) msg.obj).getMacAddress();
                    } else if (msg.obj instanceof CallbackObject) {
                        state = ((CallbackObject) msg.obj).mState;
                        macAddress = ((CallbackObject) msg.obj).mBluetoothAddress;
                    }

                    Log.d(LOG_TAG, "Shimmer state changed! Shimmer = " + macAddress + ", new state = " + state);

                    switch (state) {
                        case CONNECTED:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now CONNECTED");
                            updateDebugText((TextView) findViewById(R.id.debug_shimmer), "CONNECTED");
                            shimmerDevice = btManager.getShimmerDeviceBtConnectedFromMac(shimmerBtAdd);
                            if(shimmerDevice != null) { Log.i(LOG_TAG, "Got the ShimmerDevice!"); }
                            else { Log.i(LOG_TAG, "ShimmerDevice returned is NULL!"); }
                            break;
                        case CONNECTING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is CONNECTING");
                            updateDebugText((TextView) findViewById(R.id.debug_shimmer), "CONNECTING");
                            break;
                        case STREAMING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING");
                            updateDebugText((TextView) findViewById(R.id.debug_shimmer), "STREAMING");
                            break;
                        case STREAMING_AND_SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING AND LOGGING");
                            updateDebugText((TextView) findViewById(R.id.debug_shimmer), "STREAMING AND LOGGING");
                            break;
                        case SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now SDLOGGING");
                            updateDebugText((TextView) findViewById(R.id.debug_shimmer), "SDLOGGING");
                            break;
                        case DISCONNECTED:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] has been DISCONNECTED");
                            updateDebugText((TextView) findViewById(R.id.debug_shimmer), "DISCONNECTED");
                            break;
                    }
                    break;
            }

            super.handleMessage(msg);
        }
    };


    /**
     * Get the result from the paired devices dialog
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    btManager.disconnectAllDevices();   //Disconnect all devices first
                }catch (NullPointerException ex){
                    // .disconnectAllDevices() on null object throws a NPE
                }
                //Get the Bluetooth mac address of the selected device:
                String macAdd = data.getStringExtra(EXTRA_DEVICE_ADDRESS);
                btManager.connectShimmerThroughBTAddress(macAdd);   //Connect to the selected device
                shimmerBtAdd = macAdd;
                /*
                shimmer = new Shimmer(mHandler);
                shimmer.connect(macAdd, "default");                  //Connect to the selected device
                */
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void updateDebugText(final TextView view, final String message){
        // Addressing System.err: Only the original thread that created a view hierarchy can touch its views.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(message);
            }
        });
    }

}
