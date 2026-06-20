package com.example.hw1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class HighScoresActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val listFragment = ScoreListFragment()
        listFragment.onScoreClicked = { lat, lng ->
            mMap?.clear()
            val pos = LatLng(lat, lng)
            mMap?.addMarker(MarkerOptions().position(pos).title("Record Location"))
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.listFragmentContainer, listFragment)
            .commit()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }
}

// הפרגמנט של הטבלה - יושב באותו קובץ בשביל הסדר
class ScoreListFragment : Fragment() {
    var onScoreClicked: ((Double, Double) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scrollView = ScrollView(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(layout)

        val prefs = requireActivity().getSharedPreferences("GameData", Context.MODE_PRIVATE)
        val scoresStr = prefs.getString("scores", "") ?: ""

        if (scoresStr.isEmpty()) return scrollView

        // חיתוך וסידור 10 התוצאות הגבוהות
        val scoresList = scoresStr.split(";")
            .filter { it.isNotEmpty() }
            .map {
                val parts = it.split(",")
                Triple(parts[0].toInt(), parts[1].toDouble(), parts[2].toDouble())
            }
            .sortedByDescending { it.first }
            .take(10)

        scoresList.forEachIndexed { index, score ->
            val btn = Button(requireContext()).apply {
                text = "#${index + 1} - Distance: ${score.first}m"
                setOnClickListener { onScoreClicked?.invoke(score.second, score.third) }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
            }
            layout.addView(btn)
        }
        return scrollView
    }
}