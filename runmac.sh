#!/bin/bash
NATIVE_DIR=./jnilib
DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$NATIVE_DIR java -classpath ".:jogl.jar:jython.jar" workcraft/JavaFrontend