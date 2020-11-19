package hu.xcc.track

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class TrackBufferDB(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        val DATABASE_NAME="TrackLog"
        val DATABASE_VERSION=1
        val TABLE_LOG="LogTable"
        val KEY_ID="id"
        val KEY_TIMESTAMP="timestamp"
        val KEY_SENT="sent"
        val KEY_ENTRY="entry"
        public val FALSE=0
        public val TRUE=1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE = ("CREATE TABLE " + TABLE_LOG + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TIMESTAMP + " INTEGER,"
                + KEY_SENT + " INTEGER,"
                + KEY_ENTRY + " TEXT" + ")")
        db?.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        // Drop older books table if existed
        var old = true

        db!!.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG + "_OLD")
        try {
            db.execSQL("ALTER TABLE " + TABLE_LOG + " RENAME TO " + TABLE_LOG + "_OLD")
        } catch (e: Exception) {
            old = false
        }

        // create fresh table
        onCreate(db)

        // move records
        if (old) db.execSQL("INSERT INTO " + TABLE_LOG + " SELECT * FROM " + TABLE_LOG + "_OLD")
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG + "_OLD")
    }


    public fun addRecord(location: Location, foundBT: LinkedList<String>) :Boolean {

        val db=this.writableDatabase
        val values=ContentValues()

        var entry=JSONObject()
        entry.put("date",(System.currentTimeMillis()/1000).toString())
        entry.put("tracker_name","gergo")
        entry.put("own_btid","Gergo Note 10")
        entry.put("fixpoint_name","SP1")
        entry.put("latitude", location.latitude.toString())
        entry.put("longitude", location.longitude.toString())
        entry.put("discovery_list", JSONArray(foundBT))

        values.put(KEY_TIMESTAMP, System.currentTimeMillis())
        values.put(KEY_SENT, FALSE)
        values.put(KEY_ENTRY, entry.toString())
        val success=db.insert(TABLE_LOG, null, values)

        db.close()

        return success!=-1L
    }

    val unsent: List<LogEntry>
    get() = getLogList("SELECT * FROM $TABLE_LOG WHERE $KEY_SENT=$FALSE ORDER BY $KEY_TIMESTAMP")


    val all : List<LogEntry>
    get() = getLogList("SELECT * FROM $TABLE_LOG ORDER BY $KEY_TIMESTAMP DESC")

    private fun getLogList(sql: String): LinkedList<LogEntry> {
        val list = LinkedList<LogEntry>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    LogEntry(
                        cursor.getLong(cursor.getColumnIndex(KEY_ID)),
                        cursor.getLong(cursor.getColumnIndex(KEY_TIMESTAMP)),
                        cursor.getString(cursor.getColumnIndex(KEY_ENTRY)),
                        cursor.getInt(cursor.getColumnIndex(KEY_SENT)) == TRUE
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun sent(id: Long) : Boolean {
        val db=this.writableDatabase
        val values=ContentValues()

        values.put(KEY_SENT, TRUE)
        val success=db.update(TABLE_LOG, values, "$KEY_ID=?", arrayOf(id.toString()))

        if(success==1) {
            var old=System.currentTimeMillis()-AppConstants.PURGE_TIME       // 1 hour
            db.delete(TABLE_LOG,"$KEY_SENT=$TRUE AND $KEY_TIMESTAMP<?",arrayOf((old).toString()))
        }

        db.close()

        return success==1
    }

}