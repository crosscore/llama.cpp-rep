#!/bin/bash

set -e

# Function to print error and exit
function error_exit {
    echo "$1" 1>&2
    exit 1
}

# Check if ANDROID_NDK_ROOT is set
if [ -z "$ANDROID_NDK_ROOT" ]; then
    error_exit "Error: ANDROID_NDK_ROOT environment variable is not set.
Please set it to the path of your Android NDK installation."
fi

# Print the NDK path for verification
echo "Using Android NDK at: $ANDROID_NDK_ROOT"

# Define the source directory (llama.cpp) and build directory
SOURCE_DIR="llama.cpp"
BUILD_DIR="build-android"

# Create build directory
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Run CMake
cmake ../$SOURCE_DIR \
  -DCMAKE_TOOLCHAIN_FILE=../android-toolchain.cmake \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_SHARED_LIBS=ON \
  -DLLAMA_STATIC=OFF \
  -DLLAMA_BUILD_EXAMPLES=OFF \
  -DLLAMA_BUILD_TESTS=OFF

# Determine the number of CPU cores for parallel build
if [[ "$OSTYPE" == "linux-gnu"* || "$OSTYPE" == "darwin"* ]]; then
    NUM_CORES=$(nproc)
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    NUM_CORES=$(nproc) # Ensure nproc is available on Windows or set to a default value
else
    NUM_CORES=1
fi

# Build
make -j"$NUM_CORES"

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build completed successfully."
    # Locate the .so file
    LIBLLAMA_SO=$(find . -name "libllama.so" -print -quit)
    if [ -z "$LIBLLAMA_SO" ]; then
        error_exit "Error: libllama.so was not found after build."
    fi
    echo "Found libllama.so at: $LIBLLAMA_SO"

    # Copy libllama.so to the parent directory (llama.cpp-rep)
    cp "$LIBLLAMA_SO" ../
    echo "Copied libllama.so to ../"

    # Return to the parent directory
    cd ..

    # Remove the build directory
    rm -rf "$BUILD_DIR"
    echo "Removed build directory: $BUILD_DIR"

    echo "libllama.so has been successfully copied to the current directory and build-android has been removed."
else
    error_exit "Build failed. Please check the error messages above."
fi
