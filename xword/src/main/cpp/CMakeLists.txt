# Copyright © 2023. Richard P. Parkins, M. A.
# Released under GPL V3 or later

# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("xword")

SET(CMAKE_INCLUDE_CURRENT_DIR ON)

add_custom_command(OUTPUT makedict
		COMMAND gcc ${CMAKE_CURRENT_SOURCE_DIR}/makedict.c -o makedict
		DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/makedict.c)

add_custom_command(OUTPUT ./xworddict.h
		COMMAND ./makedict ${CMAKE_CURRENT_SOURCE_DIR}/words dict ${CMAKE_CURRENT_SOURCE_DIR}/sowpods-ScrabbleUKwords scrabbleUKwords ${CMAKE_CURRENT_SOURCE_DIR}/twl06-ScrabbleUSwords scrabbleUSwords
		DEPENDS ./makedict ${CMAKE_CURRENT_SOURCE_DIR}/words ${CMAKE_CURRENT_SOURCE_DIR}/sowpods-ScrabbleUKwords ${CMAKE_CURRENT_SOURCE_DIR}/twl06-ScrabbleUSwords )

add_library(xwordsearch-jni SHARED
		xwordsearch-jni.c
		./xworddict.h)

# include libraries needed
target_link_libraries(xwordsearch-jni)
