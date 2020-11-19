package hu.xcc.track

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TrackLogAdapter(private val list:List<LogEntry>) : RecyclerView.Adapter<LogHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogHolder {
        return LogHolder(LayoutInflater.from(parent.context),parent)
    }

    override fun onBindViewHolder(holder: LogHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

}