package com.example.signlanguagedetector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import com.example.signlanguagedetector.databinding.ActivitySpeechBinding
import java.util.Locale
import java.util.Objects

class SpeechActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySpeechBinding

    private val REQUEST_CODE_SPEECH_INPUT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeechBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.butn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.idIVMic.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to translation")

            try{
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            }catch (e: Exception){
                Toast.makeText(this, " " + e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_CODE_SPEECH_INPUT){
            if(resultCode == RESULT_OK && data != null){
                val res: ArrayList <String> = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                binding.idTVOutput.text = Objects.requireNonNull(res)[0]
            }
        }
    }
}