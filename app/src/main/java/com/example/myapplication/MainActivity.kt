package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : AppCompatActivity(), SensorEventListener, TextToSpeech.OnInitListener {
    private val RQ_Speech = 102
    private lateinit var language: String
    private lateinit var sensorManager: SensorManager
    private var mAccelerometer: Sensor? = null
    private val SHAKE_THRESHOLD = 5
    private var isOpeningMap = false
    private lateinit var tts: TextToSpeech





    private val greetings = mapOf(
        "Korean" to "안녕하세요",
        "Chinese" to "你好",
        "Japanese" to "こんにちは"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        tts = TextToSpeech(this, this)

        val languages = listOf("Korean", "Chinese", "Japanese")
        val languageSpinner: Spinner = findViewById(R.id.language_spinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        languageSpinner.adapter = adapter

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                language = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        findViewById<Button>(R.id.recordBtn).setOnClickListener {
            startRecording()
        }
    }

    private fun startRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something in $language")
        }
        startActivityForResult(intent, RQ_Speech)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]

                val z = event.values[2]
//                Log.d("shake_detect",x.toString() + " " + y.toString() + " "+ z.toString())

                val acceleration = sqrt((x * x + z * z).toDouble())
//                Log.d("shake_detect",acceleration.toString())
                if (acceleration > SHAKE_THRESHOLD) {
                    // Shake detected!
                    Log.d("shake_detect", "Shake detected2!")
                    jumpToMapAtRandomLocation()
                }
            }


        }
    }

        private fun jumpToMapAtRandomLocation() {

              if (isOpeningMap) return

               speakOut(greetings[language])
               isOpeningMap=true
               val randomNumber = Random.nextInt(2)
               val location: String
               Thread.sleep(500L)
                when (randomNumber) {
                0 -> location = when (this.language) {
                    "Chinese" -> "31.22031, 121.46239" // sh
                     "Korean" -> "37.5519, 126.9918" //soeul
                      "Japanese" -> "35.6764, 139.6500" //tokyo
                    else -> "42.36026, -71.05728" // Boston
                }

                else -> location = when (this.language) {
                    "Chinese" -> "39.9075, 116.39723" // beijing
                    "Korean" -> "35.2100°, 129.0689" //busan
                    "Japanese" -> "34.6937, 135.5023" //osaka
                    else -> "51.50335, -0.07940" // London
                }
            }
            val geoUri = Uri.parse("geo:$location")
            Log.d("MapIntent", "Geo URI: $geoUri")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            startActivity(mapIntent)
        }

        private fun speakOut(text: String?) {
            text?.let {
                tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onInit(status: Int) {
            if (!::language.isInitialized) {

                return
            }
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale(language)
                val result = tts.setLanguage(locale)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This Language is not supported")
                }
            } else {
                Log.e("TTS", "Initialization Failed!")
            }
        }

        override fun onDestroy() {
            if (tts.isSpeaking) {
                tts.stop()
            }
            tts.shutdown()
            super.onDestroy()
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == RQ_Speech && resultCode == Activity.RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val speechText: EditText = findViewById(R.id.speech_text)
                speechText.setText(result?.get(0) ?: "")
            }
        }

        override fun onResume() {
            super.onResume()
            isOpeningMap=false

        }

        override fun onPause() {
            super.onPause()
            sensorManager.unregisterListener(this)
        }
    }
