package hu.xcc.track

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.framework.PrecacheManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    companion object {
        var action_menu:Menu?=null
        val LOCATION_REQ_CODE=1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        var sharedPref=PreferenceManager.getDefaultSharedPreferences(this)
        if(aC.defTrackerName.equals(sharedPref.getString(aC.trackerName,aC.defTrackerName))) {
            var trackerName="NoBluetooth!"
            try {
                var BTAdapter = BluetoothAdapter.getDefaultAdapter()
                if(BTAdapter!=null) {
                    trackerName=BTAdapter.name
                }
            } catch (e: Exception) {
            }
            sharedPref.edit().putString(aC.trackerName,trackerName).apply()
        }

        findViewById<FloatingActionButton>(R.id.fab).apply {
            setImageResource(if (ScanService.running) R.drawable.ic_media_pause else R.drawable.ic_media_play)

            setOnClickListener(object : View.OnClickListener{
                override fun onClick(v: View) {
                    if(!ScanService.running) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED

                        ) {

                            ActivityCompat.requestPermissions(this@MainActivity,
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                else
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                ,
                                LOCATION_REQ_CODE)
                            return
                        }
                        if(BluetoothAdapter.getDefaultAdapter()==null) {
                            Snackbar.make(v, R.string.no_bluetooth, Snackbar.LENGTH_LONG).show()
                            return
                        }
                    }

                    setImageResource(if (!ScanService.running) R.drawable.ic_media_pause else R.drawable.ic_media_play)

                    val intent = Intent(applicationContext, ScanService::class.java)
                    if (ScanService.running) stopService(intent) else startForegroundService(intent)
                    Log.i("activity","start:"+ScanService.running)
                    Snackbar.make(v, if(ScanService.running) R.string.stopped else R.string.started, Snackbar.LENGTH_SHORT).show()
                }

            })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== LOCATION_REQ_CODE) {
            for(result in grantResults) if(result!=PackageManager.PERMISSION_GRANTED) return
            findViewById<FloatingActionButton>(R.id.fab).callOnClick()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        action_menu=menu
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_log_to_settings)
                true
            }
            R.id.action_track_log -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_settings_to_log)
                true
            }
            R.id.action_defaults -> {
                Snackbar.make(findViewById(R.id.nav_host_fragment), getString(R.string.default_confirm), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.restore_defaults), View.OnClickListener {
                            PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply()
                            findNavController(R.id.nav_host_fragment).navigate(R.id.action_settings_to_log)
                        })
                        .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}