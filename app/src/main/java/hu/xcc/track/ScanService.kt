package hu.xcc.track

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.preference.PreferenceManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.*


/**
 * Starts location updates on background and publish LocationUpdateEvent upon
 * each new location result.
 */
class ScanService : Service() {
    private lateinit var locationCallback: LocationCallback
    private lateinit var handler: Handler
    private var foundBT: LinkedList<String> = LinkedList()
    private var lastBT: LinkedList<String> = LinkedList()
    var BTAdapter: BluetoothAdapter? =null

    lateinit var notification_status : String
    lateinit var notification_no_beacons_nearby: String
    lateinit var notification_no_change: String
    lateinit var notification_no_location: String
    lateinit var notification_could_not_send: String
    lateinit var notification_server_resp: String
    lateinit var notification_inaccurate: String

    lateinit var sharedPref : SharedPreferences

    companion object {
        var running=false
    }

    override fun onCreate() {
        super.onCreate()
        handler=Handler()
        Log.i("Service", "created")

        notification_status=getText(R.string.notification_status) as String
        notification_no_beacons_nearby=getString(R.string.notification_no_beacons_nearby)
        notification_no_change=getString(R.string.notification_no_change)
        notification_no_location=getString(R.string.notification_no_location)
        notification_could_not_send=getString(R.string.notification_could_not_send)
        notification_server_resp=getText(R.string.notification_server_resp) as String
        notification_inaccurate= getText(R.string.notification_inaccurate) as String

        sharedPref= PreferenceManager.getDefaultSharedPreferences(this)
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        Log.i("Service", "started")

        running=true
        sharedPref.edit().putBoolean(aC.running,true).apply()

        prepareForegroundNotification()

        BTAdapter = BluetoothAdapter.getDefaultAdapter()
        if(BTAdapter==null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!BTAdapter!!.isEnabled) BTAdapter?.enable()

        val intentFilter=IntentFilter(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        registerReceiver(BTreceiver, intentFilter)

        BTAdapter?.startDiscovery()

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i("Service", "stopped")
        super.onDestroy()

        try {
            unregisterReceiver(BTreceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopLastScan()
        stopForeground(true)
        sharedPref.edit().putBoolean(aC.running,true).apply()
        running=false
    }

    private fun stopLastScan() {
        try {
            var fusedLoc = LocationServices.getFusedLocationProviderClient(this)
            fusedLoc.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            //e.printStackTrace()
        }

        BTAdapter?.cancelDiscovery()
        handler.removeCallbacksAndMessages(null)
    }


    val BTreceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when(intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i("discovery", "start")
                    foundBT.clear()
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                    if (name != null && !foundBT.contains(name)) foundBT.add(name)
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i("discovery", "finish")

                    var msg=notification_no_beacons_nearby;

                    var log = !foundBT.isEmpty()
                    if (log) {
                        log = lastBT.isEmpty()
                        if(!log) for (item in lastBT) if (!foundBT.contains(item)) {
                            log = true; break
                        }
                        if (!log) for (item in foundBT) if (!lastBT.contains(item)) {
                            log = true; break
                        }
                        msg=notification_no_change
                    }
                    if (!log) {
                        var db = TrackBufferDB(context)
                        updateServer(db)
                        db.close()
                        schedule_next_scan()
                        updateNotification(msg)
                        return
                    }
                    lastBT.clear()
                    lastBT.addAll(foundBT)

                    var fusedLoc = LocationServices.getFusedLocationProviderClient(context)

                    val locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                        .setInterval(sharedPref.getInt(aC.GpsInterval,aC.defGpsInterval)*1000L)
                        .setFastestInterval(sharedPref.getInt(aC.GpsFastInterval,aC.defGpsFastInterval)*1000L)

                    val ownBTID= BTAdapter!!.name

                    locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            for (location in locationResult.locations) if (location != null) {

                                if(location.accuracy>sharedPref.getInt(aC.GpsAccuracy,aC.defMinAccuracy)) {
                                    updateNotification(String.format(notification_inaccurate,location.accuracy))
                                    return
                                }

                                // stop location request and schedule next scan
                                fusedLoc.removeLocationUpdates(locationCallback)

                                // store location and BT list

                                if(foundBT.isEmpty()) {

                                    updateNotification(notification_no_beacons_nearby)
                                    schedule_next_scan()

                                } else {

                                    var db = TrackBufferDB(context)
                                    var success = db.addRecord(location, foundBT,ownBTID,sharedPref)

                                    if (success) {

                                        updateUI()
                                        updateServer(db)
                                        updateNotification(String.format(notification_status, location.accuracy, foundBT.size))
                                        schedule_next_scan()

                                    } else {

                                        stopSelf()      // kill service on DB error

                                    }

                                    db.close()
                                    Log.i("location", "found, stored:" + success)
                                }

                                return
                            }

                            updateNotification(notification_no_location)
                        }
                    }
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        stopSelf()
                        return
                    }
                    fusedLoc.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.myLooper()
                    )
                }
            }
        }
    }

    private fun updateNotification(message:String?) {

        if(!running) return;

        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager.notify(aC.LOCATION_SERVICE_NOTIF_ID, generateNotification(message))
    }

    private fun updateServer(db: TrackBufferDB) {
        var unsent=db.unsent

        if(unsent.isEmpty()) return

        Thread {
            var db=TrackBufferDB(applicationContext)
            for (item in unsent) if (send(item.content)) db.sent(item.id,sharedPref)
            db.close()
            updateUI()
        }.start()

    }

    private fun send(item: String?) : Boolean {
        try {
            var connection = URL(sharedPref.getString(aC.url,aC.defURL)).openConnection() as HttpURLConnection

            connection.setRequestMethod("POST")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setDoInput(true)
            connection.setDoOutput(true)
            connection.setUseCaches(false)
            var outputPost = DataOutputStream(connection.getOutputStream())
            outputPost.write(
                item?.toByteArray(Charset.forName("UTF-8"))
            ) // Charset.forName("UTF-8")
            outputPost.flush()
            outputPost.close()


            connection.getInputStream()
            var input = InputStreamReader(connection.getInputStream())
            var res = BufferedReader(input)
            var msg = res.readLine()
            Log.i("Server: ",msg)
            res.close()
            input.close()

            var success=msg.equals("{\"success\": true}")

            if(!success) updateNotification(String.format(notification_server_resp,msg))

            return success

        } catch (e: Exception) {
            updateNotification(notification_could_not_send)
            //e.printStackTrace()
            return false
        }
    }

    val updateUI=kotlinx.coroutines.Runnable {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("update"))
    }

    private fun updateUI() {
        handler.removeCallbacks(updateUI)
        handler.postDelayed(updateUI,1000)
    }

    private fun schedule_next_scan() {
        Log.i("scan", "schedule")
        handler.postDelayed(kotlinx.coroutines.Runnable {
            if (!BTAdapter!!.isEnabled) BTAdapter?.enable()

            stopLastScan()

            BTAdapter?.startDiscovery()
            Log.i("scan", "start")
        }, sharedPref.getInt(aC.trackingInterval,aC.defTrackingInterval)*1000L)
    }

    private fun prepareForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                aC.CHANNEL_ID,
                getString(R.string.scan_service_active),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
        startForeground(aC.LOCATION_SERVICE_NOTIF_ID, generateNotification(null))
    }

    private fun generateNotification(message: String?): Notification? {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            aC.SERVICE_LOCATION_REQUEST_CODE,
            notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, aC.CHANNEL_ID)
            .setContentTitle(getString(R.string.scan_service_active))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_scanner_active)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
        return notification
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


}