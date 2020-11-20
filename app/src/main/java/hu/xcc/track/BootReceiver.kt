package hu.xcc.track

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        var sharedPref= PreferenceManager.getDefaultSharedPreferences(context)
        Log.i("boot","running:"+sharedPref.getBoolean(aC.running,false))
        if(sharedPref.getBoolean(aC.running,false)) {
            val intent = Intent(context, ScanService::class.java)
            context?.startForegroundService(intent)
        }
    }
}