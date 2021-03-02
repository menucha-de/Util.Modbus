
#---------- java-modbus library ----------
add_library(java-modbus SHARED
  modbus_mapping.cpp
  modbus_base.cpp
  modbus_rtu.cpp
  modbus_tcppi.cpp
  common/Exception.cpp
  common/Mutex.cpp
  common/MutexLock.cpp
  common/MutexException.cpp
  ${swigOutputDir}/java-modbus.cpp
)
target_include_directories(java-modbus PRIVATE
  ${PC_LIBMODBUS_INCLUDE_DIRS} # modbus.h
  ${JAVA_INCLUDE_PATH} # jni.h
  ${JAVA_INCLUDE_PATH2} # jni_m.h
)
target_link_libraries (java-modbus PRIVATE
  ${CMAKE_THREAD_LIBS_INIT}
  ${PC_LIBMODBUS_LIBRARIES}
)
install(TARGETS java-modbus DESTINATION ${installDir})
