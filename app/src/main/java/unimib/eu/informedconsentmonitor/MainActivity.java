package unimib.eu.informedconsentmonitor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

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
import java.util.Date;

import unimib.eu.informedconsentmonitor.datamodel.SQLiteDbHelper;

import static com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog.EXTRA_DEVICE_ADDRESS;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";

    protected WebView webView;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    //FIXME refactor this a dbHandler Singleton?)
    SQLiteDbHelper dbHelper;
    public boolean isBaseline = false; // data streamed is used for baseline calculation

    ShimmerBluetoothManagerAndroid btManager;
    ShimmerDevice shimmerDevice;
    String shimmerBtAdd;

    /**
     * Reference to our bound service.
     */
    BluetoothService mService = null;
    boolean mServiceConnected = false;
    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d("BinderActivity", "Connected to service.");
            mService = ((BluetoothService.LocalBinder) binder).getService();
            mServiceConnected = true;
        }
        /**
         * Connection dropped.
         */
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d("BinderActivity", "Disconnected from service.");
            mService = null;
            mServiceConnected = false;
        }
    };

    // configuration.properties
    boolean isDebug;
    String webApp_BaseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 0);

        // Layout elements
        webView = findViewById(R.id.webview);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        // Read configuration properties
        isDebug = BuildConfig.DEBUG;
        webApp_BaseUrl = BuildConfig.WEBAPP_URL;
        // Open SQLite Db
        dbHelper = new SQLiteDbHelper(getApplicationContext());

        Intent intent = new Intent(getApplicationContext(), BluetoothService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        startService(intent);

        if (isDebug) {
            // Added only to be able to debug the application through chrome://inspect
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // Initialize toolbar
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        // Initialize navigation menu handler
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // Handle item selection
                        menuItem.setChecked(false);
                        drawerLayout.closeDrawers();
                        switch (menuItem.getItemId()) {
                            case R.id.nav_home:
                                if(webView != null) webView.loadUrl(webApp_BaseUrl );
                                return false;
                            case R.id.nav_calibrate:
                                if(webView != null) webView.loadUrl(webApp_BaseUrl + "/calibration.php");
                                return false;
                            case R.id.nav_connect_shimmer:
                                connectDevice(null);
                                return false;
                            case R.id.nav_export_database:
                                exportCsv(null);
                                return false;
                            case R.id.nav_info:
                                Intent i = new Intent(getApplicationContext(), InfoActivity.class);
                                startActivity(i);
                                return false;
                            default:
                                return false;
                        }
                    }
                });


        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                webView.loadUrl(url);
                return true;
            }
            // TODO remove this once the webapp is deployed with a proper certificate
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //super.onReceivedSslError(view, handler, error);
                Log.d("SSL", error.toString());
                handler.proceed();
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
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webView.clearCache(false);
        webView.addJavascriptInterface(new CustomJavaScriptInterface(this, webView), "AndroidBridge");
        if(savedInstanceState == null) {
            webView.loadUrl(webApp_BaseUrl);
        }
        Log.d("DEBUG", "webapp url is " + webApp_BaseUrl);
        // IMPORTANT !!!
        // "https://sites.google.com/a/chromium.org/dev/Home/chromium-security/deprecating-powerful-features-on-insecure-origins"
        // camera remote activation is allowed only on localhost or https served websites
    }

    @Override
    protected void onSaveInstanceState(Bundle outState )
    {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shimmerDevice != null){
            if(shimmerDevice.mBluetoothRadioState == ShimmerBluetooth.BT_STATE.STREAMING) {
                shimmerDevice.stopStreaming();
            }
            btManager.disconnectAllDevices();
        }

        if (mServiceConnected) {
            unbindService(mConn);
            stopService(new Intent(this, BluetoothService.class));
            mServiceConnected = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Deprecated
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

    public void startStreaming(View v) {
        if (mService != null) {
            //FIXME refactor this a dbHandler Singleton?)
            mService.setIsBaseline(isBaseline);
            mService.startStreaming();
        }
    }

    public void stopStreaming(View v) {
        if (mService != null) {
            //FIXME refactor this (a dbHandler Singleton?)
            mService.setIsBaseline(isBaseline);
            mService.stopStreaming();
        }
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

                        double gsrConductance = 0;
                        double gsrResistance = 0;
                        double ppg = 0;
                        double temperature = 0;

                        Collection<FormatCluster> allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE);
                        FormatCluster formatCluster = ((FormatCluster) ObjectCluster.returnFormatCluster(allFormats, "CAL"));
                        if (formatCluster != null) {
                            gsrConductance = formatCluster.mData;
                        }
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_RESISTANCE);
                        formatCluster = ((FormatCluster) ObjectCluster.returnFormatCluster(allFormats, "CAL"));
                        if (formatCluster != null) {
                            gsrResistance = formatCluster.mData;
                        }
                        allFormats = objectCluster.getCollectionOfFormatClusters("PPG_A13");
                        formatCluster = ((FormatCluster) ObjectCluster.returnFormatCluster(allFormats, "CAL"));
                        if (formatCluster != null) {
                            ppg = formatCluster.mData;
                        }
                        allFormats = objectCluster.getCollectionOfFormatClusters("Temperature_BMP280");
                        formatCluster = ((FormatCluster) ObjectCluster.returnFormatCluster(allFormats, "CAL"));
                        if (formatCluster != null) {
                            temperature = formatCluster.mData;
                        }

                        Log.d(LOG_TAG, "DATA_PACKET: " +
                                "\n GSR CONDUCTANCE: " + gsrConductance +
                                "\n GSR RESISTANCE: " + gsrResistance +
                                "\n PPG: " + ppg +
                                "\n TEMPERATURE: " + temperature);
                        dbHelper.insertShimmerDataEntry(new Date().getTime(), isBaseline, gsrConductance, gsrResistance, ppg, temperature);

                    }
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
                            shimmerDevice = btManager.getShimmerDeviceBtConnectedFromMac(shimmerBtAdd);
                            if(shimmerDevice != null) {
                                Log.i(LOG_TAG, "Got the ShimmerDevice!");

                                /*
                                for (SensorDetails sensorsDetsils : shimmerDevice.getListOfEnabledSensors()) {
                                    for (String sensors : sensorsDetsils.mSensorDetailsRef.mListOfChannelsRef) {
                                        Log.d("Enabled Sensors", sensors);
                                    }
                                }
                                */
                            }
                            else { Log.i(LOG_TAG, "ShimmerDevice returned is NULL!"); }
                            break;
                        case CONNECTING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is CONNECTING");
                            break;
                        case STREAMING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING");
                            break;
                        case STREAMING_AND_SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING AND LOGGING");
                            break;
                        case SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now SDLOGGING");
                            break;
                        case DISCONNECTED:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] has been DISCONNECTED");
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
                //Get the Bluetooth mac address of the selected device:
                String macAdd = data.getStringExtra(EXTRA_DEVICE_ADDRESS);
                mService.connectDevice(macAdd);
                /*
                if(btManager == null){
                    try {
                        btManager = new ShimmerBluetoothManagerAndroid(this, mHandler);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Couldn't create ShimmerBluetoothManagerAndroid. Error: " + e);
                    }
                }
                //Disconnect all devices first
                btManager.disconnectAllDevices();
                //Connect to the selected device
                btManager.connectShimmerThroughBTAddress(macAdd);
                */
                shimmerBtAdd = macAdd;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void exportCsv(View v){
        String message = "Tables exported successfully on the local storage.";
        dbHelper.exportDataBaseToCsv();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}
