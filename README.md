# JetPack Compose版 Android LLMアプリ実装ガイド

このガイドでは、LlamaモデルをJetPack Composeを使用したAndroidアプリケーションに統合するための完全な手順を提供します。

## 1. プロジェクトのセットアップ

1. Android Studioを開き、新しいプロジェクトを作成します。
2. "Empty Compose Activity"を選択します。
3. プロジェクト名を設定します（例: "LLMChatApp"）。
4. 言語として"Kotlin"を選択します。
5. 最小SDKをAPI 21以上に設定します。

## 2. 依存関係の追加

`app/build.gradle.kts` ファイルを開き、以下の依存関係を追加します：

```kotlin
android {
    // 他の設定...

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

## 3. パーミッションの追加

`AndroidManifest.xml` ファイルを開き、以下のパーミッションを追加します：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## 4. JNIの設定（LLM推論用）

1. `app/src/main/cpp/` ディレクトリを作成します。

2. `native-lib.cpp` ファイルを作成し、以下のコードを追加します：

```cpp
#include <jni.h>
#include <string>
#include "llama.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_llmchatapp_MainActivity_performLLMInference(
        JNIEnv* env,
        jobject /* this */,
        jstring input) {
    const char* inputStr = env->GetStringUTFChars(input, 0);

    // TODO: Implement LLM inference using Llama.cpp
    // This is a placeholder implementation
    std::string result = "LLM response for: ";
    result += inputStr;

    env->ReleaseStringUTFChars(input, inputStr);
    return env->NewStringUTF(result.c_str());
}
```

3. `app/src/main/cpp/CMakeLists.txt` ファイルを作成し、以下の内容を追加します：

```cmake
cmake_minimum_required(VERSION 3.4.1)

add_library(native-lib SHARED
            native-lib.cpp)

find_library(log-lib
             log)

# llama.cppのヘッダーファイルのパスを指定
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

target_link_libraries(native-lib
                      ${log-lib}
                      # llamaライブラリをリンク
                      ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllama.so)
```

4. `app/build.gradle.kts` ファイルに以下を追加します：

```kotlin
android {
    // ...
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}
```

## 5. Llama.cppライブラリの追加

1. ビルド済みの `libllama.so` ファイルを `app/src/main/jniLibs/arm64-v8a/` ディレクトリに配置します。

2. Llama.cppのヘッダーファイル（`llama.h`）を `app/src/main/cpp/llama.cpp/` ディレクトリに配置します。

## 6. GGUFファイルの準備

アプリケーションがGGUFファイルをダウンロードできるようにしますが、開発中はファイルを手動でデバイスに配置することもできます。ファイルは通常、`/sdcard/Download/` ディレクトリに配置します。

## 7. MainActivity.kt の実装

`MainActivity.kt` ファイルを以下の内容で更新します：

```kotlin
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
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), GGUF_FILENAME)
        return file.exists()
    }
}
```

## 8. アプリのビルドと実行

1. Android Studioでプロジェクトを同期します。
2. プロジェクトをビルドします。
3. エミュレータまたは実機（Pixel 8など）でアプリを実行します。

## 注意点

- このガイドは基本的な実装を示しています。実際のアプリケーションでは、エラーハンドリング、ユーザー体験の向上、セキュリティ対策などを追加する必要があります。
- Llama.cppの統合には、さらなる調整やオプティマイゼーションが必要になる可能性があります。
- 大きなモデルファイルの扱いには注意が必要です。ユーザーのデバイスのストレージ容量を考慮してください。
- LLM推論は計算負荷が高いため、バッテリー消費や端末の発熱に注意してください。
- Jetpack Composeを使用することで、UIの構築がより宣言的になり、状態管理が簡単になります。ただし、Composeに慣れていない場合は、学習曲線があるかもしれません。
