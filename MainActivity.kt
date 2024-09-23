import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val GGUF_URL = "https://huggingface.co/QuantFactory/TinySlime-1.1B-Chat-v1.0-GGUF/resolve/main/TinySlime-1.1B-Chat-v1.0.Q4_0.gguf"
        private const val GGUF_FILENAME = "TinySlime-1.1B-Chat-v1.0.Q4_0.gguf"

        init {
            System.loadLibrary("native-lib")
        }
    }

    private external fun performLLMInference(input: String): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadButton.setOnClickListener { downloadGGUF() }
        askButton.setOnClickListener { askQuestion() }

        updateDownloadButtonState()
    }

    private fun downloadGGUF() {
        val request = DownloadManager.Request(Uri.parse(GGUF_URL))
            .setTitle("Downloading GGUF file")
            .setDescription("Downloading $GGUF_FILENAME")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, GGUF_FILENAME)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateDownloadButtonState()
                unregisterReceiver(this)
            }
        }

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun updateDownloadButtonState() {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), GGUF_FILENAME)
        if (file.exists()) {
            downloadButton.apply {
                text = "Already Downloaded"
                isEnabled = false
            }
        } else {
            downloadButton.apply {
                text = "Download GGUF"
                isEnabled = true
            }
        }
    }

    private fun askQuestion() {
        val question = questionInput.text.toString()
        if (question.isEmpty()) {
            Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            askButton.isEnabled = false
            val answer = withContext(Dispatchers.Default) {
                performLLMInference(question)
            }
            answerOutput.text = answer
            askButton.isEnabled = true
        }
    }
}
