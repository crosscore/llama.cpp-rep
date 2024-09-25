# android-toolchain.cmake

set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION 33) # 最新のAndroid API levelに更新
set(CMAKE_ANDROID_ARCH_ABI arm64-v8a)
set(CMAKE_ANDROID_NDK $ENV{ANDROID_NDK_ROOT})
set(CMAKE_ANDROID_STL_TYPE c++_static)

# Detect host OS
if(${CMAKE_HOST_SYSTEM_NAME} STREQUAL "Darwin")
    set(HOST_TAG "darwin-x86_64")
elseif(${CMAKE_HOST_SYSTEM_NAME} STREQUAL "Linux")
    set(HOST_TAG "linux-x86_64")
else()
    message(FATAL_ERROR "Unsupported host system: ${CMAKE_HOST_SYSTEM_NAME}")
endif()

# Ensure ANDROID_NDK_ROOT is set
if(NOT DEFINED ENV{ANDROID_NDK_ROOT})
    message(FATAL_ERROR "ANDROID_NDK_ROOT environment variable is not set.")
endif()

set(CMAKE_ANDROID_NDK $ENV{ANDROID_NDK_ROOT})

# Update compiler to match the API level
set(CMAKE_C_COMPILER "${CMAKE_ANDROID_NDK}/toolchains/llvm/prebuilt/${HOST_TAG}/bin/aarch64-linux-android33-clang")
set(CMAKE_CXX_COMPILER "${CMAKE_ANDROID_NDK}/toolchains/llvm/prebuilt/${HOST_TAG}/bin/aarch64-linux-android33-clang++")

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)
