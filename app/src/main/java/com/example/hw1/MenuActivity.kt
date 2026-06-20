package com.example.hw1 // ודא ששם החבילה תואם

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        findViewById<Button>(R.id.btnSlow).setOnClickListener { startGame(sensor = false, fast = false) }
        findViewById<Button>(R.id.btnFast).setOnClickListener { startGame(sensor = false, fast = true) }
        findViewById<Button>(R.id.btnSensor).setOnClickListener { startGame(sensor = true, fast = false) }

        findViewById<Button>(R.id.btnScores).setOnClickListener {
            startActivity(Intent(this, HighScoresActivity::class.java))
        }
    }

    private fun startGame(sensor: Boolean, fast: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SENSOR_MODE", sensor)
            putExtra("FAST_MODE", fast)
        }
        startActivity(intent)
    }
}