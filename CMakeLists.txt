cmake_minimum_required(VERSION 3.0)
project(Java-Modbus)

# load properties
include(cmake.properties)

set(CMAKE_CXX_FLAGS "-std=c++98 -static-libstdc++")

set(swigOutputDir ${CMAKE_CURRENT_SOURCE_DIR}/target/swig/cpp)

set(resourcesDir ${CMAKE_CURRENT_SOURCE_DIR}/src/main/resources)
set(installDir ${CMAKE_INSTALL_PREFIX})

#---------- debug ----------
set(CMAKE_DEBUG_POSTFIX d)
 
#---------- system libraries ----------
# libraries: ${CMAKE_THREAD_LIBS_INIT}
find_package(Threads REQUIRED) 
# includes: ${JAVA_INCLUDE_PATH}
find_package(JNI REQUIRED)
find_package(PkgConfig REQUIRED)
# libraries: ${PC_LIBMODBUS_LIBRARIES}
# includes: ${PC_LIBMODBUS_INCLUDE_DIRS}
pkg_check_modules(PC_LIBMODBUS REQUIRED libmodbus)

add_subdirectory(src/main/cpp)
