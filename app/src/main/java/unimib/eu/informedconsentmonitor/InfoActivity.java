package unimib.eu.informedconsentmonitor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

                String[] from = {"item_text", "item_value"};
                int[] to = {R.id.item_text, R.id.item_value};

                SimpleAdapter simpleAdapter = new SimpleAdapter(getBaseContext(), aList, R.layout.list_view_items, from, to);
                ListView androidListView = (ListView) findViewById(R.id.list_view);
                androidListView.setAdapter(simpleAdapter);
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
    }
}
