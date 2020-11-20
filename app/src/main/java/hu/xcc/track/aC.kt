package hu.xcc.track

class aC {
    companion object {
        val fixPointName="fixpoint_name"
        val defFixPointName="NONE"

        val trackerName="tracker_name"
        val defTrackerName="defTracker"

        val GpsInterval="gps_interval"
        val defGpsInterval=15

        val GpsFastInterval="gps_fast_interval"
        val defGpsFastInterval=1

        val trackingInterval="tracking_interval"
        val defTrackingInterval=30

        val url="url"
        val defURL="https://data.4mhu.com/upload_data/"

        val purge="purge"
        val defPurge=4

        val purgeTime=1000L*60*60*24          // remove sent item older than 4 days
        val LOCATION_SERVICE_NOTIF_ID=1
        val SERVICE_LOCATION_REQUEST_CODE=1
        val CHANNEL_ID="xcc.scan.service"
    }

}
