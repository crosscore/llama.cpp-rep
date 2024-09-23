# Complete Kotlin Android LLM App Implementation Guide

このガイドでは、LlamaモデルをAndroidアプリケーションに統合するための完全な手順を提供します。

## 1. プロジェクトのセットアップ

1. Android Studioを開き、新しいプロジェクトを作成します。
2. "Empty Activity"を選択します。
3. プロジェクト名を設定します（例: "LLMChatApp"）。
4. 言語として"Kotlin"を選択します。
5. 最小SDKをAPI 21以上に設定します。

## 2. 依存関係の追加

`app/build.gradle` ファイルを開き、以下の依存関係を追加します：

```gradle
android {
    // 他の設定...

    buildFeatures {
        viewBinding true
    }

    kotlinOptions {
        jvmTarget = '1.8'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.7.0'
    implementation 'androidx.appcompat:appcompat:1.4.1'
    implementation 'com.google.android.material:material:1.5.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.4.0'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
}
```

## 3. パーミッションの追加

`AndroidManifest.xml` ファイルを開き、以下のパーミッションを追加します：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## 4. レイアウトの作成

`app/src/main/res/layout/activity_main.xml` ファイルを以下の内容で作成または更新します：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <Button
        android:id="@+id/downloadButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Download GGUF" />

    <EditText
        android:id="@+id/questionInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Enter your question" />

    <Button
        android:id="@+id/askButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Ask" />

    <TextView
        android:id="@+id/answerOutput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp" />

</LinearLayout>
```

## 5. JNIの設定（LLM推論用）

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

4. `app/build.gradle` ファイルに以下を追加します：

```gradle
android {
    // ...
    externalNativeBuild {
        cmake {
            path file('src/main/cpp/CMakeLists.txt')
        }
    }
}
```

## 6. Llama.cppライブラリの追加

1. ビルド済みの `libllama.so` ファイルを `app/src/main/jniLibs/arm64-v8a/` ディレクトリに配置します。

2. Llama.cppのヘッダーファイル（`llama.h`）を `app/src/main/cpp/llama.cpp/` ディレクトリに配置します。

## 7. GGUFファイルの準備

アプリケーションがGGUFファイルをダウンロードできるようにしますが、開発中はファイルを手動でデバイスに配置することもできます。ファイルは通常、`/sdcard/Download/` ディレクトリに配置します。

## 8. アプリのビルドと実行

1. Android Studioでプロジェクトを同期します。
2. プロジェクトをビルドします。
3. エミュレータまたは実機でアプリを実行します。

## 注意点

- このガイドは基本的な実装を示しています。実際のアプリケーションでは、エラーハンドリング、ユーザー体験の向上、セキュリティ対策などを追加する必要があります。
- Llama.cppの統合には、さらなる調整やオプティマイゼーションが必要になる可能性があります。
- 大きなモデルファイルの扱いには注意が必要です。ユーザーのデバイスのストレージ容量を考慮してください。
- LLM推論は計算負荷が高いため、バッテリー消費や端末の発熱に注意してください。
