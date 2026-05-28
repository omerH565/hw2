package com.example.hw1

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    // רכיבי שחקן
    private lateinit var imgPlayerLeft: ImageView
    private lateinit var imgPlayerCenter: ImageView
    private lateinit var imgPlayerRight: ImageView
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button

    //לבבות
    private lateinit var imgHeart1: ImageView
    private lateinit var imgHeart2: ImageView
    private lateinit var imgHeart3: ImageView

    // משתני מצב
    private var currentLane = 1
    private var lives = 3

    // מערך דו מימדי - רשת
    private lateinit var obstaclesUI: Array<Array<ImageView>>
    private var obstaclesLogic = Array(3) { BooleanArray(8) }

    private var gameScope = CoroutineScope(Dispatchers.Main)
    private var isGameRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        updatePlayerUI()

        startGameLoop()
    }

    private fun initViews() {
        imgPlayerLeft = findViewById(R.id.imgPlayerLeft)
        imgPlayerCenter = findViewById(R.id.imgPlayerCenter)
        imgPlayerRight = findViewById(R.id.imgPlayerRight)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)

        // קישור הלבבות מה-XML
        imgHeart1 = findViewById(R.id.imgHeart1)
        imgHeart2 = findViewById(R.id.imgHeart2)
        imgHeart3 = findViewById(R.id.imgHeart3)

        // מיפוי מטריצת המכשולים
        obstaclesUI = arrayOf(
            arrayOf(findViewById(R.id.obs_0_0), findViewById(R.id.obs_0_1), findViewById(R.id.obs_0_2), findViewById(R.id.obs_0_3), findViewById(R.id.obs_0_4), findViewById(R.id.obs_0_5), findViewById(R.id.obs_0_6), findViewById(R.id.obs_0_7)),
            arrayOf(findViewById(R.id.obs_1_0), findViewById(R.id.obs_1_1), findViewById(R.id.obs_1_2), findViewById(R.id.obs_1_3), findViewById(R.id.obs_1_4), findViewById(R.id.obs_1_5), findViewById(R.id.obs_1_6), findViewById(R.id.obs_1_7)),
            arrayOf(findViewById(R.id.obs_2_0), findViewById(R.id.obs_2_1), findViewById(R.id.obs_2_2), findViewById(R.id.obs_2_3), findViewById(R.id.obs_2_4), findViewById(R.id.obs_2_5), findViewById(R.id.obs_2_6), findViewById(R.id.obs_2_7))
        )
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
            if (currentLane < 2) {
                currentLane++
                updatePlayerUI()
                checkCollisionImmediately()
            }
        }
    }

    private fun updatePlayerUI() {
        imgPlayerLeft.visibility = if (currentLane == 0) View.VISIBLE else View.INVISIBLE
        imgPlayerCenter.visibility = if (currentLane == 1) View.VISIBLE else View.INVISIBLE
        imgPlayerRight.visibility = if (currentLane == 2) View.VISIBLE else View.INVISIBLE
    }

    private fun startGameLoop() {
        gameScope.launch {
            while (isGameRunning) {
                delay(300)
                moveObstaclesDown()
                checkCollision()
                spawnNewObstacle()
                updateObstaclesUI()
            }
        }
    }

    private fun moveObstaclesDown() {
        for (lane in 0..2) {
            for (row in 7 downTo 1) {
                obstaclesLogic[lane][row] = obstaclesLogic[lane][row - 1]
            }
            obstaclesLogic[lane][0] = false
        }
    }

    private fun spawnNewObstacle() {
        val shouldSpawn = (0..3).random()
        if (shouldSpawn == 1) {
            val randomLane = (0..2).random()
            obstaclesLogic[randomLane][0] = true
        }
    }

    private fun updateObstaclesUI() {
        for (lane in 0..2) {
            for (row in 0..7) {
                obstaclesUI[lane][row].visibility =
                    if (obstaclesLogic[lane][row]) View.VISIBLE else View.INVISIBLE
            }
        }
    }

    private fun checkCollision() {
        if (obstaclesLogic[currentLane][7]) {
            handleCrash()
        }
    }
    private fun checkCollisionImmediately() {
        if (obstaclesLogic[currentLane][7]) {
            handleCrash()
        }
    }

    private fun handleCrash() {
        lives--
        vibrateDevice()
        Toast.makeText(this, "Crash! 💥", Toast.LENGTH_SHORT).show()
        updateHeartsUI()
        obstaclesLogic[currentLane][7] = false

        if (lives == 0) {
            Toast.makeText(this, "Game Over! Restarting...", Toast.LENGTH_LONG).show()
            resetGame()
        }
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

    private fun resetGame() {
        lives = 3
        updateHeartsUI()
        for (lane in 0..2) {
            for (row in 0..7) {
                obstaclesLogic[lane][row] = false//ניקוי אבנים לריסארט
            }
        }
        currentLane = 1 // מחזירים את השחקן לאמצע
        updatePlayerUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameScope.cancel()
    }
}