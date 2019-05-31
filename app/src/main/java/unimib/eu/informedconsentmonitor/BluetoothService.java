package unimib.eu.informedconsentmonitor;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.ShimmerDevice;
import com.shimmerresearch.exceptions.ShimmerException;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import unimib.eu.informedconsentmonitor.datamodel.SQLiteDbHelper;

public class BluetoothService extends Service {
    public static String SHIMMER_STATE = "ShimmerBluetoothRadioState";
    // WARN: If SENSORS is changed, be sure to check the shimmer handler and the dbHelper also
    public static String[] SENSORS = {"GSR_Skin_Conductance", "GSR_Skin_Resistance", "PPG_A13", "Temperature_BMP280"};
    public static String BROADCAST_ACTION = "unimib.eu.informedconsentmonitor.BluetoothService";

    String LOG_TAG = "BluetoothService";
    LocalBroadcastManager localBroadcastManager;
    ShimmerBluetoothManagerAndroid btManager;
    ShimmerDevice shimmerDevice;
    public SQLiteDbHelper dbHelper;

    // FIXME refactor this var
    int baseline = 0;

    public class LocalBinder extends Binder {
        /**
         * Return enclosing BinderService instance
         */
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "Binding...");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(LOG_TAG, "Service started.");
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Open SQLite Db
        //dbHelper = new SQLiteDbHelper(getApplicationContext());
        // FIX: changing dbHelper to public and setting it from MainActivity

        sendBroadcast(new HashMap<String, String>());

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy(){
        Log.d(LOG_TAG, "Service destroyed.");
        if (shimmerDevice != null){
            if(shimmerDevice.mBluetoothRadioState == ShimmerBluetooth.BT_STATE.STREAMING) {
                shimmerDevice.stopStreaming();
            }
            try {
                shimmerDevice.disconnect();
            } catch (ShimmerException e) {
                e.printStackTrace();
            }
        }
    }

    public void connectDevice(String macAddress) {
        if(btManager == null){
            try {
                btManager = new ShimmerBluetoothManagerAndroid(this, mHandler);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Couldn't create ShimmerBluetoothManagerAndroid. Error: " + e);
            }
        }
        if(macAddress != null) {
            //Disconnect all devices first
            btManager.disconnectAllDevices();
            //Connect to the selected device
            btManager.connectShimmerThroughBTAddress(macAddress);
        }
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
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;

                        HashMap<String, String> hm = new HashMap<>();
                        for(int i = 0; i < SENSORS.length; i++){
                            Collection<FormatCluster> allFormats = objectCluster.getCollectionOfFormatClusters(SENSORS[i]);
                            FormatCluster formatCluster = ((FormatCluster) ObjectCluster.returnFormatCluster(allFormats, "CAL"));
                            if (formatCluster != null) {
                                hm.put(SENSORS[i], String.valueOf(formatCluster.mData));
                            }
                        }

                        double gsrConductance = Double.parseDouble(hm.get(SENSORS[0]));
                        double gsrResistance = Double.parseDouble(hm.get(SENSORS[1]));
                        double ppg = Double.parseDouble(hm.get(SENSORS[2]));
                        double temperature = Double.parseDouble(hm.get(SENSORS[3]));
                        dbHelper.insertShimmerDataEntry(new Date().getTime(), baseline, gsrConductance, gsrResistance, ppg, temperature);
                        sendBroadcast(hm);
                        //Log.d(LOG_TAG, "handleMessage: " + Arrays.toString(hm.entrySet().toArray()));

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
                            shimmerDevice = btManager.getShimmerDeviceBtConnectedFromMac(macAddress);
                            if(shimmerDevice != null) {
                                Log.i(LOG_TAG, "Got the ShimmerDevice!");

                                /*
                                for (SensorDetails sensorsDetails : shimmerDevice.getListOfEnabledSensors()) {
                                    for (String sensors : sensorsDetails.mSensorDetailsRef.mListOfChannelsRef) {
                                        Log.d("Enabled Sensors", sensors);
                                    }
                                }
                                */
                            }
                            else { Log.i(LOG_TAG, "ShimmerDevice returned is NULL!"); }
                            sendBroadcast(new HashMap<String, String>());
                            break;
                        case CONNECTING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is CONNECTING");
                            sendBroadcast(new HashMap<String, String>());
                            break;
                        case STREAMING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING");
                            sendBroadcast(new HashMap<String, String>());
                            break;
                        case STREAMING_AND_SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING AND LOGGING");
                            sendBroadcast(new HashMap<String, String>());
                            break;
                        case SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now SDLOGGING");
                            sendBroadcast(new HashMap<String, String>());
                            break;
                        case DISCONNECTED:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] has been DISCONNECTED");
                            sendBroadcast(new HashMap<String, String>());
                            Toast.makeText(getApplicationContext(), "Device " + macAddress + " is NOT Ready", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
            }

            super.handleMessage(msg);
        }
    };

    public void stopStreaming() {
        if (shimmerDevice != null) {
            Log.d(LOG_TAG, "Stop Streaming");
            shimmerDevice.stopStreaming();
        }
    }

    public void startStreaming() {
        if (shimmerDevice != null) {
            Log.d(LOG_TAG, "Start Streaming");
            shimmerDevice.startStreaming();
        }
    }

    public double getShimmerSamplingRate(){
        if (shimmerDevice != null) {
            return shimmerDevice.getSamplingRateShimmer();
        }
        return 0;
    }

    public String getShimmerState(){
        if(shimmerDevice != null){
            return shimmerDevice.getBluetoothRadioStateString();
        }
        return "DISCONNECTED";
    }

    public void setBaseline(int value){
        baseline = value;
    }

    /**
     * This method is responsible to send broadCast to specific Action
     * */
    private void sendBroadcast(HashMap<String, String> map)
    {
        try
        {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(BROADCAST_ACTION);

            broadCastIntent.putExtra(SHIMMER_STATE, shimmerDevice == null ? "Disconnected" : shimmerDevice.getBluetoothRadioStateString());
            for (Map.Entry<String, String> pair: map.entrySet()) {
                broadCastIntent.putExtra(pair.getKey(), pair.getValue());
            }
            localBroadcastManager.sendBroadcast(broadCastIntent);
            Log.d(LOG_TAG, "broadcast sent.");
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG, "sendBroadcast: " + ex.getMessage());
        }
    }
}
