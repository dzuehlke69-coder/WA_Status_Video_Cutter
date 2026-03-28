package com.dzuehlke69.wa_status_video_cutter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var selectedVideoFile: File? = null
    private val lastCutUris = ArrayList<Uri>()

    private val selectVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val videoUri = result.data?.data
            videoUri?.let { uri ->
                selectedVideoFile = copyUriToTempFile(uri)
                findViewById<Button>(R.id.btnCutOnly).isEnabled = true
                Toast.makeText(this, "Video geladen!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSelect = findViewById<Button>(R.id.btnStart)
        val btnCutOnly = findViewById<Button>(R.id.btnCutOnly)
        val btnShareOnly = findViewById<Button>(R.id.btnShareOnly)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnDonation = findViewById<Button>(R.id.btnDonation) // Der neue Spenden-Button

        btnCutOnly.isEnabled = false
        btnShareOnly.isEnabled = false

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "video/*" }
            selectVideoLauncher.launch(intent)
            btnCutOnly.text = "2. Nur Schneiden"
        }

        btnCutOnly.setOnClickListener {
            selectedVideoFile?.let { file ->
                btnCutOnly.isEnabled = false
                startCuttingProcess(file)
            }
        }

        btnShareOnly.setOnClickListener {
            if (lastCutUris.isNotEmpty()) shareToWhatsApp(lastCutUris)
            btnCutOnly.text = "2. Nur Schneiden"
        }

        btnClear.setOnClickListener {
            clearCutterFolder()
        }

        // Öffnet das Spenden-Fenster (Dialog)
        btnDonation.setOnClickListener {
            showDonationDialog()
        }

        // Versionsnummer im Footer anzeigen
        val tvVersion = findViewById<TextView>(R.id.tvVersionFooter)
        try {
            val version = packageManager.getPackageInfo(packageName, 0).versionName
            tvVersion.text = "v$version"
        } catch (e: Exception) {
            tvVersion.text = "v?.?.?"
        }
    }

    // Funktion, die das Spenden-Fenster (QR-Code + Adresse) anzeigt
    private fun showDonationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_donation, null)
        val tvBtcAddressDialog = dialogView.findViewById<TextView>(R.id.tv_btc_address_dialog)

        // Kopier-Funktion für die Adresse innerhalb des Fensters
        tvBtcAddressDialog.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("BTC Address", tvBtcAddressDialog.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Bitcoin-Adresse kopiert!", Toast.LENGTH_SHORT).show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Schließen", null)
            .show()
    }

    private fun copyUriToTempFile(uri: Uri): File {
        val tempFile = File(cacheDir, "input_video.mp4")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    private fun startCuttingProcess(inputFile: File) {
        lastCutUris.clear()
        val internalDir = getExternalFilesDir(null)?.absolutePath ?: cacheDir.absolutePath
        val info = FFprobeKit.getMediaInformation(inputFile.absolutePath)
        val duration = info.mediaInformation?.duration?.toDouble() ?: 0.0
        val timestamp = System.currentTimeMillis()

        Thread {
            var startTime = 0.0
            var part = 1
            val segmentStep = 60.0
            val cutDuration = 61.0

            while (startTime < duration) {
                runOnUiThread { findViewById<Button>(R.id.btnCutOnly).text = "Teil $part..." }

                val tempOutPath = "$internalDir/temp_$part.mp4"
                val cmd = "-y -ss $startTime -t $cutDuration -i ${inputFile.absolutePath} -c copy $tempOutPath"

                val session = FFmpegKit.execute(cmd)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    val fileUri = saveToGalleryAndGetUri(tempOutPath, "WA_${timestamp}_Part_$part.mp4")
                    fileUri?.let { lastCutUris.add(it) }
                }

                startTime += segmentStep
                part++
                if (startTime >= duration) break
            }

            runOnUiThread {
                val btn = findViewById<Button>(R.id.btnCutOnly)
                btn.text = "Schneiden fertig"
                btn.isEnabled = true
                findViewById<Button>(R.id.btnShareOnly).isEnabled = true
                Toast.makeText(this, "Schnitt beendet!", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun saveToGalleryAndGetUri(sourcePath: String, fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/WA_Cutter")
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let { targetUri ->
            contentResolver.openOutputStream(targetUri)?.use { output ->
                File(sourcePath).inputStream().use { input -> input.copyTo(output) }
            }
            File(sourcePath).delete()
        }
        return uri
    }

    private fun shareToWhatsApp(uris: ArrayList<Uri>) {
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/mp4"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }
        try {
            startActivity(Intent.createChooser(shareIntent, "Teile ${uris.size} Videos"))
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp Fehler!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearCutterFolder() {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Movies/WA_Cutter%")
        try {
            val deletedRows = contentResolver.delete(collection, selection, selectionArgs)
            lastCutUris.clear()
            findViewById<Button>(R.id.btnShareOnly).isEnabled = false
            Toast.makeText(this, "Ordner leer! $deletedRows Dateien gelöscht.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Löschen fehlgeschlagen.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Einstellungen folgen in Kürze...", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_contact -> {
                showContactDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) { "?.?.?" }

        AlertDialog.Builder(this)
            .setTitle("Statuscutter")
            .setMessage("Status: Einsatzbereit\nv$version\n\nSchneidet Videos automatisch in 60-Sekunden-Parts.\n\nSupport den Entwickler über den Spenden-Button!")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showContactDialog() {
        val message = "Name: Dirk Zühlke\n\neMail: dzuehlke69@gmail.com\n\nTel.: +49 (0)157 826 709 32"
        val dialog = AlertDialog.Builder(this)
            .setTitle("Kontakt")
            .setMessage(message)
            .setPositiveButton("Schließen", null)
            .create()
        dialog.show()
        Linkify.addLinks(dialog.findViewById<TextView>(android.R.id.message)!!, Linkify.ALL)
    }
}