# Copyright © 2023. Richard P. Parkins, M. A.
# Released under GPL V3 or later

# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("xword")

set(NATIVEDIR "/home/rparkins/Documents/Android/xword/xword/src/main/cpp")

add_custom_command(OUTPUT ${NATIVEDIR}/makedict
		COMMAND gcc ${NATIVEDIR}/makedict.c -o ${NATIVEDIR}/makedict
		DEPENDS ${NATIVEDIR}/makedict.c)

add_custom_command(OUTPUT ${NATIVEDIR}/xworddict.h
		WORKING_DIRECTORY ${NATIVEDIR}
		COMMAND ${NATIVEDIR}/makedict
		DEPENDS ${NATIVEDIR}/makedict ${NATIVEDIR}/words)

add_library(xwordsearch-jni SHARED
		${NATIVEDIR}/xwordsearch-jni.c
		${NATIVEDIR}/xworddict.h)

# include libraries needed
target_link_libraries(xwordsearch-jni)
