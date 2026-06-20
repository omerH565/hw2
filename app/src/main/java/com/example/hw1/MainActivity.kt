package com.example.hw1 // ודא ששם החבילה תואם

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    enum class Item { EMPTY, OBSTACLE, COIN }

    private lateinit var playerUI: Array<ImageView>
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var layoutControls: LinearLayout

    private lateinit var imgHeart1: ImageView
    private lateinit var imgHeart2: ImageView
    private lateinit var imgHeart3: ImageView

    private lateinit var tvDistance: TextView
    private lateinit var tvCoins: TextView

    private var currentLane = 2
    private var lives = 3
    private var distance = 0
    private var coins = 0

    private lateinit var obstaclesUI: Array<Array<ImageView>>
    private var obstaclesLogic = Array(5) { Array(10) { Item.EMPTY } }

    private var gameScope = CoroutineScope(Dispatchers.Main)
    private var isGameRunning = true

    // סנסורים ומצבים
    private var isSensorMode = false
    private var isFastMode = false
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastSensorUpdateTime: Long = 0
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        isSensorMode = intent.getBooleanExtra("SENSOR_MODE", false)
        isFastMode = intent.getBooleanExtra("FAST_MODE", false)

        initViews()
        if (isSensorMode) {
            layoutControls.visibility = View.GONE
            initSensors()
        }
        setupListeners()
        updatePlayerUI()

        startGameLoop()
    }

    private fun initViews() {
        playerUI = arrayOf(
            findViewById(R.id.imgPlayer0), findViewById(R.id.imgPlayer1),
            findViewById(R.id.imgPlayer2), findViewById(R.id.imgPlayer3),
            findViewById(R.id.imgPlayer4)
        )

        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        layoutControls = findViewById(R.id.layoutControls)

        imgHeart1 = findViewById(R.id.imgHeart1)
        imgHeart2 = findViewById(R.id.imgHeart2)
        imgHeart3 = findViewById(R.id.imgHeart3)

        tvDistance = findViewById(R.id.tvDistance)
        tvCoins = findViewById(R.id.tvCoins)

        obstaclesUI = arrayOf(
            arrayOf(findViewById(R.id.obs_0_0), findViewById(R.id.obs_0_1), findViewById(R.id.obs_0_2), findViewById(R.id.obs_0_3), findViewById(R.id.obs_0_4), findViewById(R.id.obs_0_5), findViewById(R.id.obs_0_6), findViewById(R.id.obs_0_7), findViewById(R.id.obs_0_8), findViewById(R.id.obs_0_9)),
            arrayOf(findViewById(R.id.obs_1_0), findViewById(R.id.obs_1_1), findViewById(R.id.obs_1_2), findViewById(R.id.obs_1_3), findViewById(R.id.obs_1_4), findViewById(R.id.obs_1_5), findViewById(R.id.obs_1_6), findViewById(R.id.obs_1_7), findViewById(R.id.obs_1_8), findViewById(R.id.obs_1_9)),
            arrayOf(findViewById(R.id.obs_2_0), findViewById(R.id.obs_2_1), findViewById(R.id.obs_2_2), findViewById(R.id.obs_2_3), findViewById(R.id.obs_2_4), findViewById(R.id.obs_2_5), findViewById(R.id.obs_2_6), findViewById(R.id.obs_2_7), findViewById(R.id.obs_2_8), findViewById(R.id.obs_2_9)),
            arrayOf(findViewById(R.id.obs_3_0), findViewById(R.id.obs_3_1), findViewById(R.id.obs_3_2), findViewById(R.id.obs_3_3), findViewById(R.id.obs_3_4), findViewById(R.id.obs_3_5), findViewById(R.id.obs_3_6), findViewById(R.id.obs_3_7), findViewById(R.id.obs_3_8), findViewById(R.id.obs_3_9)),
            arrayOf(findViewById(R.id.obs_4_0), findViewById(R.id.obs_4_1), findViewById(R.id.obs_4_2), findViewById(R.id.obs_4_3), findViewById(R.id.obs_4_4), findViewById(R.id.obs_4_5), findViewById(R.id.obs_4_6), findViewById(R.id.obs_4_7), findViewById(R.id.obs_4_8), findViewById(R.id.obs_4_9))
        )
    }

    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || !isGameRunning) return

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSensorUpdateTime > 250) {
                val x = event.values[0]
                if (x > 2.5f && currentLane > 0) {
                    currentLane--
                    updatePlayerUI()
                    checkCollisionImmediately()
                    lastSensorUpdateTime = currentTime
                } else if (x < -2.5f && currentLane < 4) {
                    currentLane++
                    updatePlayerUI()
                    checkCollisionImmediately()
                    lastSensorUpdateTime = currentTime
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onResume() {
        super.onResume()
        if (isSensorMode) {
            accelerometer?.let {
                sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isSensorMode) {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    private fun setupListeners() {
        btnLeft.setOnClickListener {
            if (currentLane > 0) {
                currentLane--
                updatePlayerUI()
                checkCollisionImmediately()
            }
        }

        btnRight.setOnClickListener {
            if (currentLane < 4) {
                currentLane++
                updatePlayerUI()
                checkCollisionImmediately()
            }
        }
    }

    private fun updatePlayerUI() {
        for (i in 0..4) {
            playerUI[i].visibility = if (i == currentLane) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun startGameLoop() {
        val speedDelay = if (isFastMode) 150L else 300L
        gameScope.launch {
            while (isGameRunning) {
                delay(speedDelay)
                distance += 10
                tvDistance.text = "Distance: ${distance}m"

                moveObstaclesDown()
                checkCollision()
                spawnNewObstacle()
                updateObstaclesUI()
            }
        }
    }

    private fun moveObstaclesDown() {
        for (lane in 0..4) {
            for (row in 9 downTo 1) {
                obstaclesLogic[lane][row] = obstaclesLogic[lane][row - 1]
            }
            obstaclesLogic[lane][0] = Item.EMPTY
        }
    }

    private fun spawnNewObstacle() {
        val shouldSpawn = (0..2).random()
        if (shouldSpawn == 1) {
            val randomLane = (0..4).random()
            val isCoin = (1..10).random() > 8
            obstaclesLogic[randomLane][0] = if (isCoin) Item.COIN else Item.OBSTACLE
        }
    }

    private fun updateObstaclesUI() {
        for (lane in 0..4) {
            for (row in 0..9) {
                val view = obstaclesUI[lane][row]
                when (obstaclesLogic[lane][row]) {
                    Item.EMPTY -> view.visibility = View.INVISIBLE
                    Item.OBSTACLE -> {
                        view.setImageResource(R.drawable.ic_obstacle)
                        view.visibility = View.VISIBLE
                    }
                    Item.COIN -> {
                        view.setImageResource(R.drawable.ic_coin)
                        view.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun checkCollision() {
        when (obstaclesLogic[currentLane][9]) {
            Item.OBSTACLE -> handleCrash()
            Item.COIN -> collectCoin()
            Item.EMPTY -> {}
        }
    }

    private fun checkCollisionImmediately() {
        when (obstaclesLogic[currentLane][9]) {
            Item.OBSTACLE -> handleCrash()
            Item.COIN -> collectCoin()
            Item.EMPTY -> {}
        }
    }

    private fun collectCoin() {
        coins++
        tvCoins.text = "Coins: $coins"
        obstaclesLogic[currentLane][9] = Item.EMPTY
        updateObstaclesUI()
    }

    private fun playCrashSound() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.crash)
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleCrash() {
        lives--
        vibrateDevice()
        playCrashSound()
        Toast.makeText(this, "Crash! 💥", Toast.LENGTH_SHORT).show()
        updateHeartsUI()
        obstaclesLogic[currentLane][9] = Item.EMPTY

        if (lives == 0) {
            isGameRunning = false
            saveScore()
            Toast.makeText(this, "Game Over! Saving Score...", Toast.LENGTH_LONG).show()

            // חזרה לתפריט הראשי אחרי שנגמר
            val intent = Intent(this, MenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun saveScore() {
        val prefs = getSharedPreferences("GameData", Context.MODE_PRIVATE)
        val current = prefs.getString("scores", "") ?: ""

        // יצירת נקודת ציון רנדומלית בתל אביב עבור המפה
        val lat = 32.0853 + (Math.random() - 0.5) * 0.05
        val lng = 34.7818 + (Math.random() - 0.5) * 0.05

        val newEntry = "$distance,$lat,$lng"
        prefs.edit().putString("scores", if (current.isEmpty()) newEntry else "$current;$newEntry").apply()
    }

    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(400)
        }
    }

    private fun updateHeartsUI() {
        imgHeart1.visibility = if (lives >= 1) View.VISIBLE else View.INVISIBLE
        imgHeart2.visibility = if (lives >= 2) View.VISIBLE else View.INVISIBLE
        imgHeart3.visibility = if (lives >= 3) View.VISIBLE else View.INVISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        gameScope.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}