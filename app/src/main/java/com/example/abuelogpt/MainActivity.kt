package com.example.abuelogpt

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private val chatHistory: List<Content> = emptyList()
    private var reconocervoz = true
    private val model = GenerativeModel(
        "gemini-1.5-flash",
        apiKey = BuildConfig.API_KEY
    )
    private val chat = model.startChat(chatHistory)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tts = TextToSpeech(this, this)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(p0: String?) {
                // No action needed
            }

            override fun onDone(p0: String?) {
                if (reconocervoz)
                    startVoiceRecognition()
            }

            override fun onError(p0: String?) {
                // Handle TTS error if needed
            }
        })

        // Inicializa el ActivityResultLauncher
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val resultData = result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (resultData != null && resultData.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val response2 = chat.sendMessage(resultData[0])
                        val textToRead2 = response2.text.toString()
                        readText(textToRead2)
                    }
                } else {
                    Toast.makeText(this, "Resultado vacío", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale("es", "LAT")
            val result = tts.setLanguage(locale)
        } else {
            // Handle TTS initialization error
            Toast.makeText(this, "Error en la inicialización de TextToSpeech", Toast.LENGTH_SHORT).show()
        }
    }

    fun accionBotonPresionado(view: View) {
        val boton: Button = findViewById(R.id.button)
        if (boton.text == "HABLAR") {
            reconocervoz = true
            boton.text = "COLGAR"

            CoroutineScope(Dispatchers.IO).launch {
                val response = chat.sendMessage("hola")
                val textToRead = response.text.toString()
                readText(textToRead)
            }
        } else {
            boton.text = "HABLAR"
            reconocervoz = false
            tts.stop()
        }
    }

    private fun readText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        // Especificamos el idioma español latino en el Intent del reconocimiento de voz
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "es-419"
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Di algo...")

        try {
            startForResult.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Tu dispositivo no soporta reconocimiento de voz",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}