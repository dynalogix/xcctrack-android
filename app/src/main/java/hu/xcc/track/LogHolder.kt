package hu.xcc.track

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import hu.xcc.track.databinding.ListItemBinding
import java.text.SimpleDateFormat
import java.util.*

class LogHolder(inflater: LayoutInflater, parent: ViewGroup) : RecyclerView.ViewHolder(
    inflater.inflate(
        R.layout.list_item,
        parent,
        false
    )
) {

    fun bind(entry: LogEntry) {
        val binding=ListItemBinding.bind(itemView)
        binding.timestamp.apply {
            text=getDate(entry.timeStamp,"MM/dd hh:mm.ss")
            setCompoundDrawablesWithIntrinsicBounds(if(entry.sent) R.drawable.ic_sent else R.drawable.ic_not_sent,0,0,0)
        }
        binding.content.text=entry.content
    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        val formatter = SimpleDateFormat(dateFormat)
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(milliSeconds)
        return formatter.format(calendar.getTime())
    }
}
