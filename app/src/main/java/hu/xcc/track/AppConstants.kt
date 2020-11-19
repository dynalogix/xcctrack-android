package hu.xcc.track

class AppConstants {
    companion object {
        val PURGE_TIME=1000L*60*60*24*4          // remove sent item older than 4 days
        val URL="https://data.4mhu.com/upload_data/"
        val LOCATION_SERVICE_NOTIF_ID=1
        val SERVICE_LOCATION_REQUEST_CODE=1
        val CHANNEL_ID="xcc.scan.service"
    }

}
