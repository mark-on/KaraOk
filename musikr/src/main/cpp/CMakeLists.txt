# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("tagJNI") # becomes "libtagJNI.so"

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
set(taglib_location "${CMAKE_CURRENT_SOURCE_DIR}/taglib")

# Build TagLib as part of the Android native build. Keeping it in this CMake
# graph makes project sync and builds work on Windows without a Unix shell or
# pre-generated archives.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build shared libraries" FORCE)
set(VISIBILITY_HIDDEN ON CACHE BOOL "Hide TagLib symbols" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "Build TagLib tests" FORCE)
set(BUILD_EXAMPLES OFF CACHE BOOL "Build TagLib examples" FORCE)
set(BUILD_BINDINGS OFF CACHE BOOL "Build TagLib bindings" FORCE)
set(WITH_ZLIB OFF CACHE BOOL "Build TagLib with zlib" FORCE)
set(WITH_APE OFF CACHE BOOL "Build TagLib with APE support" FORCE)
set(WITH_ASF OFF CACHE BOOL "Build TagLib with ASF support" FORCE)
set(WITH_MOD OFF CACHE BOOL "Build TagLib with tracker support" FORCE)
set(WITH_SHORTEN OFF CACHE BOOL "Build TagLib with Shorten support" FORCE)
set(WITH_TRUEAUDIO OFF CACHE BOOL "Build TagLib with TrueAudio support" FORCE)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_subdirectory(
        "${taglib_location}"
        "${CMAKE_CURRENT_BINARY_DIR}/taglib-build"
        EXCLUDE_FROM_ALL)

# TagLib installs its public headers into one flat `taglib` directory, which
# is the include layout used by the JNI bridge. Recreate that layout inside
# CMake's generated directory so no files need to be written into the source
# checkout and generated headers such as taglib_config.h are included too.
get_target_property(taglib_public_headers tag PUBLIC_HEADER)
set(taglib_include "${CMAKE_CURRENT_BINARY_DIR}/taglib-include")
file(MAKE_DIRECTORY "${taglib_include}/taglib")
foreach(taglib_header IN LISTS taglib_public_headers)
    if(IS_ABSOLUTE "${taglib_header}")
        set(taglib_header_source "${taglib_header}")
    else()
        set(taglib_header_source "${taglib_location}/taglib/${taglib_header}")
    endif()
    get_filename_component(taglib_header_name "${taglib_header}" NAME)
    configure_file(
            "${taglib_header_source}"
            "${taglib_include}/taglib/${taglib_header_name}"
            COPYONLY)
endforeach()

add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        taglib_jni.cpp
        JInputStream.cpp
        JTagMap.cpp
        JMetadataBuilder.cpp
        JClassRef.cpp
        JObjectRef.cpp
        JStringRef.cpp
        JByteArrayRef.cpp
)
target_include_directories(${CMAKE_PROJECT_NAME}
        PRIVATE "${taglib_include}")
target_link_options(${CMAKE_PROJECT_NAME}
        # @Tolriq found that these flags can reduce the size of the linked
        # taglib + jni shim shared library. Kudos to them.
        # https://github.com/taglib/taglib/issues/1212#issuecomment-2326456903
        # Additionally, enable 16kb page size. I believe taglib can support this fine,
        # as a cursory glance indicates that it doesn't hardcode any page sizes.
        PRIVATE "-Wl,--exclude-libs,ALL,-z,max-page-size=16384")

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        PRIVATE android
        PRIVATE log
        PRIVATE tag)
