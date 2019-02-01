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
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import unimib.eu.informedconsentmonitor.datamodel.SQLiteDbHelper;

import static com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog.EXTRA_DEVICE_ADDRESS;

public class MainActivity extends Activity {

    private final static String LOG_TAG_SHIMMER = "Shimmer";
    private final static String LOG_TAG_JAVASCRIPT = "Javascript";
    private final static int REQUEST_CAMERA_PERMISSION = 10;
    private final static int REQUEST_EXTERNAL_STORAGE_PERMISSION = 11;

    Shimmer shimmer;
    SQLiteDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 0);

        // Added the handler for the bluetooth state change event
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter1);

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.clearCache(false);
        // Added only to be able to debug the application through chrome://inspect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // Open front facing camera
        Camera.open(1);

        // Open SQLite Db
        dbHelper = new SQLiteDbHelper(getApplicationContext());

        // We inject the needed javascript on every loaded page
        webView.setWebViewClient(new WebViewClient(){
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onPageFinished(WebView webView, String url) {
                super.onPageFinished(webView, url);
                injectScriptFile(webView, "timeme.js");
                injectScriptFile(webView, "scrolldetect.js");
                injectScriptFile(webView, "webgazer.js");
                injectScriptFile(webView, "inject.js");
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
        // TODO remove. testing on localhost due to this
        // "https://sites.google.com/a/chromium.org/dev/Home/chromium-security/deprecating-powerful-features-on-insecure-origins"
        // camera remote acivation is allowed only on localhost or https served websites
        //webView.loadUrl("file:///android_asset/index.html");

        //shimmer = new Shimmer(mHandler);

        final TableLayout tv = (TableLayout)findViewById(R.id.debug_stats);

        // We set the toggle button to hide or show the statistics screen
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.debug_btn);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (((ToggleButton) v).isChecked())
                    tv.setVisibility(View.VISIBLE);
                else
                    tv.setVisibility(View.GONE);
            }
        });

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
                // TODO decomment
                //connectDevice(null);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver1);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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


            Log.d(LOG_TAG_JAVASCRIPT, "Injected file " + encoded);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void connectDevice(View v) {
        Intent intent = new Intent(getApplicationContext(), ShimmerBluetoothDialog.class);
        startActivityForResult(intent, ShimmerBluetoothDialog.REQUEST_CONNECT_SHIMMER);
    }

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:

                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:

                        break;
                    case BluetoothAdapter.STATE_ON:

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        connectDevice(null);
                        break;
                }

            }
        }
    };

    // API 23 (Marshmellow) requires runtime permission
    public void requestPermission(String permission, int permissionCode){
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation required
                ActivityCompat.requestPermissions(this, new String[]{permission}, permissionCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
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
            case REQUEST_EXTERNAL_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    // Open front facing camera
                    Camera.open(1);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    dbHelper.exportCsv();
                }
                return;
            }



            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void startStreaming(View v) throws InterruptedException, IOException{
        shimmer.startStreaming();
    }

    public void stopStreaming(View v) throws IOException{
        shimmer.stopStreaming();
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

                        //Print data to Logcat
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;

                        //Retrieve all possible formats for the current sensor device:
                        Collection<FormatCluster> allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP);
                        FormatCluster timeStampCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        double timeStampData = timeStampCluster.mData;
                        Log.i(LOG_TAG_SHIMMER, "Time Stamp: " + timeStampData);
                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_X);
                        FormatCluster accelXCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
                        if (accelXCluster!=null) {
                            double accelXData = accelXCluster.mData;
                            Log.i(LOG_TAG_SHIMMER, "Accel LN X: " + accelXData);
                        }
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
                    Log.d(LOG_TAG_SHIMMER, "Shimmer sensor " + state.toString());
                    updateDebugText((TextView) findViewById(R.id.debug_shimmer), state.toString());
                    switch (state) {
                        case CONNECTED:
                            break;
                        case CONNECTING:
                            break;
                        case STREAMING:
                            break;
                        case STREAMING_AND_SDLOGGING:
                            break;
                        case SDLOGGING:
                            break;
                        case DISCONNECTED:
                            break;
                        case CONNECTION_LOST:
                            break;
                        case CONNECTION_FAILED:
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
                //Get the Bluetooth mac address of the selected device:
                String macAdd = data.getStringExtra(EXTRA_DEVICE_ADDRESS);
                shimmer = new Shimmer(mHandler);
                shimmer.connect(macAdd, "default");                  //Connect to the selected device
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
