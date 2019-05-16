package unimib.eu.informedconsentmonitor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InfoActivity extends Activity {
    String LOG_TAG = "InfoActivity";
    LocalBroadcastManager localBroadcastManager;
    BroadCastReceiver broadCastReceiver;

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

    /**
     * This method is responsible to register an action to BroadCastReceiver
     * */
    private void registerReceiver() {
        try
        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothService.BROADCAST_ACTION);
            localBroadcastManager.registerReceiver(broadCastReceiver, intentFilter);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    private void adaptToListView(List<HashMap<String, String>> aList){
        String[] from = {"item_text", "item_value"};
        int[] to = {R.id.item_text, R.id.item_value};

        SimpleAdapter simpleAdapter = new SimpleAdapter(getBaseContext(), aList, R.layout.list_view_items, from, to);
        ListView androidListView = (ListView) findViewById(R.id.list_view);
        androidListView.setAdapter(simpleAdapter);
    }

    /**
     * MyBroadCastReceiver is responsible to receive broadCast from register action
     * */
    class BroadCastReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent) {

            try
            {
                Log.d(LOG_TAG, "onReceive() called");

                List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("item_text", BluetoothService.SHIMMER_STATE);
                map.put("item_value", intent.getStringExtra(BluetoothService.SHIMMER_STATE));
                aList.add(map);
                for (int i = 0; i < BluetoothService.SENSORS.length; i++) {
                    HashMap<String, String> hm = new HashMap<String, String>();
                    hm.put("item_text", BluetoothService.SENSORS[i]);
                    hm.put("item_value", intent.getStringExtra(BluetoothService.SENSORS[i]));
                    aList.add(hm);
                }

                adaptToListView(aList);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        broadCastReceiver = new BroadCastReceiver();
        registerReceiver();

        Intent intent = new Intent(getApplicationContext(), BluetoothService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        startService(intent);

        if(mService != null) {
            List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("item_text", BluetoothService.SHIMMER_STATE);
            map.put("item_value", mService.getShimmerState());
            aList.add(map);

            for (int i = 0; i < BluetoothService.SENSORS.length; i++) {
                HashMap<String, String> hm = new HashMap<String, String>();
                hm.put("item_text", BluetoothService.SENSORS[i]);
                hm.put("item_value", "");
                aList.add(hm);
            }

            adaptToListView(aList);
        }
    }

    /**
     * This method called when this Activity finished
     * Override this method to unregister MyBroadCastReceiver
     * */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // make sure to unregister your receiver after finishing of this activity
        localBroadcastManager.unregisterReceiver(broadCastReceiver);

        if (mServiceConnected) {
            unbindService(mConn);
            stopService(new Intent(this, BluetoothService.class));
            mServiceConnected = false;
        }
    }
}
