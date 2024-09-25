import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

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
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LLMChatApp()
                }
            }
        }
    }

    @Composable
    fun LLMChatApp() {
        val context = LocalContext.current
        var question by remember { mutableStateOf("") }
        var answer by remember { mutableStateOf("") }
        var isDownloaded by remember { mutableStateOf(checkIfGGUFDownloaded(context)) }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (!isDownloaded) {
                        downloadGGUF(context)
                    }
                },
                enabled = !isDownloaded
            ) {
                Text(if (isDownloaded) "Already Downloaded" else "Download GGUF")
            }

            TextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Enter your question") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.Default) {
                        val result = performLLMInference(question)
                        answer = result
                    }
                },
                enabled = isDownloaded
            ) {
                Text("Ask")
            }

            Text(
                text = answer,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    private fun downloadGGUF(context: Context) {
        val request = DownloadManager.Request(Uri.parse(GGUF_URL))
            .setTitle("Downloading GGUF file")
            .setDescription("Downloading $GGUF_FILENAME")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, GGUF_FILENAME)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
    
    private fun checkIfGGUFDownloaded(context: Context): Boolean {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), GGUF_FILENAME)
        return file.exists()
    }
}
