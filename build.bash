#!/usr/bin/env bash
# The java-modbus libraries are created in directory 
#   target/build-<DEB_HOST_GNU_TYPE>-<CMAKE_BUILD_TYPE>

CMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE:-Release}
DEB_HOST_GNU_TYPE=${DEB_HOST_GNU_TYPE:-x86_64-linux-gnu}
if [ "$DEB_HOST_GNU_TYPE" == "x86_64-linux-gnu" ]; then
  TOOLCHAIN=
else
  TOOLCHAIN=-DCMAKE_TOOLCHAIN_FILE=CMakeCross.txt
fi

OUTPUT_DIR=target

# call swig
SWIG_OUTPUT_DIR=$OUTPUT_DIR/swig
SWIG_CPP_OUTPUT_DIR=$SWIG_OUTPUT_DIR/cpp
SWIG_JAVA_OUTPUT_DIR=$SWIG_OUTPUT_DIR/java
SWIG_JAVA_PACKAGE=havis.util.modbus
SWIG_JAVA_PACKAGE_OUTPUT_DIR=$SWIG_JAVA_OUTPUT_DIR/`echo -n $SWIG_JAVA_PACKAGE | tr . /`
mkdir -p $SWIG_CPP_OUTPUT_DIR $SWIG_JAVA_PACKAGE_OUTPUT_DIR
echo "Calling swig..."
CMD="swig3.0 -c++ -java -package $SWIG_JAVA_PACKAGE -outdir $SWIG_JAVA_PACKAGE_OUTPUT_DIR \
  -o $SWIG_CPP_OUTPUT_DIR/java-modbus.cpp src/main/resources/java-modbus.i"
eval $CMD

# create build directory for cmake
BUILD_DIR=$OUTPUT_DIR/build-$DEB_HOST_GNU_TYPE-$CMAKE_BUILD_TYPE
mkdir -p $BUILD_DIR
cd $BUILD_DIR

CMD="DEB_HOST_GNU_TYPE=$DEB_HOST_GNU_TYPE cmake ../.. $TOOLCHAIN \
  -DCMAKE_BUILD_TYPE=$CMAKE_BUILD_TYPE -DCMAKE_INSTALL_PREFIX=.."
echo $CMD && eval $CMD

# compile and install the java-modbus library
if [ $CMAKE_BUILD_TYPE = "Release" ]; then
  CMD="make install/strip"
  echo $CMD && eval $CMD
else
  CMD="make install"
#  CMD="make install VERBOSE=1"
  echo $CMD && eval $CMD
fi

cd -