package unimib.eu.informedconsentmonitor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
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

import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pl.flex_it.androidplot.XYSeriesShimmer;
import unimib.eu.informedconsentmonitor.datamodel.SQLiteDbHelper;

import static com.shimmerresearch.android.guiUtilities.PlotFragment.X_AXIS_LENGTH;
import static com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog.EXTRA_DEVICE_ADDRESS;

public class MainActivity extends Activity {

    private final static String LOG_TAG = "Shimmer";


    protected Button connectBtn;
    protected ToggleButton streamBtn;
    protected ToggleButton debugBtn;
    protected TableLayout statsTable;
    protected WebView webView;
    protected XYPlot dynamicPlot;

    public static HashMap<String, LineAndPointFormatter> sensorMap = new HashMap<String, LineAndPointFormatter>() {{
        put(Configuration.Shimmer3.ObjectClusterSensorName.GSR_RESISTANCE, // opposite of GSR_CONDUCTANCE
                new LineAndPointFormatter(Color.rgb(0, 255, 0), null, null));
        put(Configuration.Shimmer3.ObjectClusterSensorName.ECG_TO_HR_FW,
                new LineAndPointFormatter(Color.rgb(255, 0, 0), null, null));
    }};
    public static HashMap<String, List<Number>> mPlotDataMap = new HashMap<String, List<Number>>(1);
    public static HashMap<String, XYSeriesShimmer> mPlotSeriesMap = new HashMap<String, XYSeriesShimmer>(1);
    SQLiteDbHelper dbHelper;
    ShimmerBluetoothManagerAndroid btManager;
    ShimmerDevice shimmerDevice;
    String shimmerBtAdd;

    protected String webApp_BaseUrl = "http://ericab12.altervista.org/new-informed-consent";
    protected String webApp_LoginUrl = webApp_BaseUrl + "/login.php";
    protected String webApp_DocumentUrl = webApp_BaseUrl + "/builder.php";


    @Override
    protected void onStart() {
        //Connect the Shimmer using Bluetooth
        connectDevice(null);
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 0);

        // Layout elements
        connectBtn = findViewById(R.id.connect_btn);
        webView = findViewById(R.id.webview);
        statsTable = findViewById(R.id.debug_stats);
        dynamicPlot = findViewById(R.id.dynamicPlot);

        // Initialize plot
        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));
        dynamicPlot.setDomainStepMode(XYStepMode.SUBDIVIDE);
        dynamicPlot.getLegendWidget().setSize(new SizeMetrics(0, SizeLayoutType.ABSOLUTE, 0, SizeLayoutType.ABSOLUTE));

        try {
            btManager = new ShimmerBluetoothManagerAndroid(this, mHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Couldn't create ShimmerBluetoothManagerAndroid. Error: " + e);
        }

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDevice(statsTable);
            }
        });

        webView.clearCache(false);
        // Added only to be able to debug the application through chrome://inspect
        WebView.setWebContentsDebuggingEnabled(true);

        // Open SQLite Db
        dbHelper = new SQLiteDbHelper(getApplicationContext());

        // We inject the needed javascript on every loaded page
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView webView, String url) {
                super.onPageFinished(webView, url);

                if(url.contains(webApp_DocumentUrl)) {
                injectScriptFile(webView, "timeme.js");
                injectScriptFile(webView, "scrolldetect.js");
                injectScriptFile(webView, "webgazer.js");
                injectScriptFile(webView, "inject.js");
                }
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
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webView.addJavascriptInterface(new CustomJavaScriptInterface(this, webView), "Native");
        webView.loadUrl(webApp_LoginUrl);
        // TODO remove. testing on localhost due to this
        // "https://sites.google.com/a/chromium.org/dev/Home/chromium-security/deprecating-powerful-features-on-insecure-origins"
        // camera remote activation is allowed only on localhost or https served websites
        //webView.loadUrl("file:///android_asset/www/calibration.html");
        //webView.loadUrl("https://stackoverflow.com");

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
        if (shimmerDevice != null && shimmerDevice.mBluetoothRadioState == ShimmerBluetooth.BT_STATE.STREAMING) {
            shimmerDevice.stopStreaming();
        }
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

        // We check if the bluetooth is ON
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.e(LOG_TAG, "Error. This device does not support Bluetooth");
            Toast.makeText(this, "Error. This device does not support Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enabled
                Log.e(LOG_TAG, "Error. Shimmer device not paired or Bluetooth is not enabled");
                Toast.makeText(this, "Error. Shimmer device not paired or Bluetooth is not enabled. " +
                        "Please close the app and pair or enable Bluetooth", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(getApplicationContext(), ShimmerBluetoothDialog.class);
                startActivityForResult(intent, ShimmerBluetoothDialog.REQUEST_CONNECT_SHIMMER);
            }
        }
    }

    public void startSDLogging(View v) {
        ((ShimmerBluetooth) shimmerDevice).writeConfigTime(System.currentTimeMillis());
        shimmerDevice.startSDLogging();
    }

    public void stopSDLogging(View v) {
        shimmerDevice.stopSDLogging();
    }

    public void stopStreaming(View v) {
        if (shimmerDevice != null)
            shimmerDevice.stopStreaming();
    }

    public void startStreaming(View v) {
        if (shimmerDevice != null)
            shimmerDevice.startStreaming();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // Open front facing camera
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    try {
                        manager.openCamera("1", new CameraDevice.StateCallback() {
                            @Override
                            public void onOpened(@NonNull CameraDevice camera) {

                            }

                            @Override
                            public void onDisconnected(@NonNull CameraDevice camera) {

                            }

                            @Override
                            public void onError(@NonNull CameraDevice camera, int error) {

                            }
                        }, new Handler());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                else Camera.open(1);
            }
        }
        // other 'case' lines to check for other
        // permissions this app might request.
    }

    /**
     * Messages from the Shimmer device including sensor data are received here
     */
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET:
                    if ((msg.obj instanceof ObjectCluster)) {

                        //Print data to Logcat
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;

                        for (String sensor : sensorMap.keySet()) {
                            //Retrieve all possible formats for the current sensor device:
                            Collection<FormatCluster> allFormats = objectCluster.getCollectionOfFormatClusters(sensor);
                            FormatCluster formatCluster = ((FormatCluster) ObjectCluster.returnFormatCluster(allFormats, "CAL"));
                            if (formatCluster != null) {
                                double data = formatCluster.mData;
                                Log.i(LOG_TAG, sensor + ": " + data);
                            }

                            // Plot streamed data
                            List<Number> data;
                            if (mPlotDataMap.get(sensor)!=null){
                                data = mPlotDataMap.get(sensor);
                            } else {
                                data = new ArrayList<Number>();
                            }
                            if (data.size()>X_AXIS_LENGTH){
                                //data.clear();
                                data.remove(0);
                            }
                            data.add(formatCluster.mData);
                            mPlotDataMap.put(sensor, data);

                            //next check if the series exist
                            if (mPlotSeriesMap.get(sensor)!=null){
                                //if the series exist get the line format
                                mPlotSeriesMap.get(sensor).updateData(data);
                            } else {
                                XYSeriesShimmer series = new XYSeriesShimmer(data, 0, sensor);
                                mPlotSeriesMap.put(sensor, series);
                                dynamicPlot.addSeries(mPlotSeriesMap.get(sensor), sensorMap.get(sensor));
                            }
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
                            if(shimmerDevice != null) {
                                Log.i(LOG_TAG, "Got the ShimmerDevice!");
                                shimmerDevice.startStreaming();
                            }
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
        if(requestCode == ShimmerBluetoothDialog.REQUEST_CONNECT_SHIMMER) {
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
