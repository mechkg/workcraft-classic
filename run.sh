#!/bin/bash
SO_X86=so.x86
SO_AMD64=so.amd64
SO_DIR=
ARCH=`uname -m`

echo -n "Architecture "$ARCH" "
case $ARCH
in
  x86|i386|i486|i586|i686)
    echo "uses "$SO_X86
    SO_DIR=./$SO_X86
  ;;
  x86_64|amd64)
    echo "uses "$SO_AMD64
    SO_DIR=./$SO_AMD64
  ;;
  *)
    echo "is not supported." 
  ;;
esac

if [ SO_DIR ];
then
  LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SO_DIR java -classpath ".:jogl.jar:jython.jar:gluegen-rt.jar" workcraft/JavaFrontend
fi
