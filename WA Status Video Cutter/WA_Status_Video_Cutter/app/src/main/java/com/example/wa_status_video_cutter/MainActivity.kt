package com.dzuehlke69.wa_status_video_cutter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Berechtigungen beim Start prüfen
        checkStoragePermission()

        val btnStart = findViewById<Button>(R.id.btnStart)
        btnStart.setOnClickListener {
            openVideoPicker()
        }
    }

    private fun checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    // Öffnet die Galerie/Dateien auf deinem Samsung
    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        videoPickerLauncher.launch(intent)
    }

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri = result.data?.data
            if (videoUri != null) {
                // Video zerschneiden starten
                processVideo(videoUri)
            }
        }
    }

    private fun processVideo(uri: Uri) {
        val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        val outputDir = "$downloadFolder/WA_Status_Parts"

        val folder = File(outputDir)
        if (!folder.exists()) folder.mkdirs()

        // Temporäre Datei erstellen, da FFmpeg direkten URI-Zugriff oft nicht mag
        val inputFile = File(cacheDir, "input_video.mp4")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(inputFile).use { output -> input.copyTo(output) }
        }

        Toast.makeText(this, "Schnitt gestartet...", Toast.LENGTH_SHORT).show()

        // FFmpeg Logik
        val mediaInfo = com.arthenica.ffmpegkit.FFprobeKit.getMediaInformation(inputFile.absolutePath)
        val duration = mediaInfo?.mediaInformation?.duration?.toDouble() ?: 0.0

        var startTime = 0
        var partNumber = 1

        Thread {
            while (startTime < duration) {
                val outputPath = "$outputDir/status_part_$partNumber.mp4"
                val cmd = "-ss $startTime -i ${inputFile.absolutePath} -t 60 -c copy \"$outputPath\""

                val session = FFmpegKit.execute(cmd)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    Log.d("VideoCutter", "Teil $partNumber fertig")
                }

                startTime += 59
                partNumber++
                if (partNumber > 100) break
            }
            runOnUiThread {
                Toast.makeText(this, "Fertig! Ordner: Downloads/WA_Status_Parts", Toast.LENGTH_LONG).show()
            }
        }.start()
    } // <-- Schließt processVideo
} // <-- Schließt die gesamte MainActivity Klasse (Das ist die fehlende Klammer!)