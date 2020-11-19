package hu.xcc.track

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import hu.xcc.track.databinding.FragmentTrackLogBinding


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class TrackLogFragment : Fragment() {

    private var _binding: FragmentTrackLogBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MainActivity.action_menu?.findItem(R.id.action_settings)?.setVisible(true)
        MainActivity.action_menu?.findItem(R.id.action_track_log)?.setVisible(false)

        return inflater.inflate(R.layout.fragment_track_log, container, false)
    }

    val receiver:BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i("F1","update")
            val db= context?.let { TrackBufferDB(it) }
            if (db != null) {
                Log.i("F1","refresh")
                binding.listTrack.swapAdapter(TrackLogAdapter(db.all),true)
                db.close()
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding=FragmentTrackLogBinding.bind(view)

        binding.listTrack.apply {
            layoutManager=LinearLayoutManager(context)

            val db=TrackBufferDB(context)
            adapter=TrackLogAdapter(db.all)
            db.close()

            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter("update"))
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
        Log.i("F1", "exit")

        run {
            context?.let { LocalBroadcastManager.getInstance(it).unregisterReceiver(receiver) }
        }
    }
}