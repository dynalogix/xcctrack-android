package hu.xcc.track

class LogEntry {
    var id: Long = 0
    var timeStamp: Long = System.currentTimeMillis()
    var content: String? = null
    var sent = false

    constructor(id:Long, timeStamp:Long, content: String?, sent:Boolean) {
        this.id = id
        this.timeStamp=timeStamp
        this.content=content
        this.sent=sent
    }
}