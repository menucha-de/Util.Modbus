%module Modbus
%{
#include "../../../src/main/cpp/modbus_mapping.h"
#include "../../../src/main/cpp/modbus_base.h"
#include "../../../src/main/cpp/modbus_rtu.h"
#include "../../../src/main/cpp/modbus_tcppi.h"
%}
 
%include "carrays.i"
%include "stdint.i"
%array_class(uint8_t, UInt8Array);
%array_class(uint16_t, UInt16Array);
%array_class(int, IntArray);

%include "src/main/cpp/modbus_mapping.h"
%include "src/main/cpp/modbus_base.h"
%include "src/main/cpp/modbus_rtu.h"
%include "src/main/cpp/modbus_tcppi.h"

%pragma(java) jniclasscode=%{
  static {
    System.loadLibrary("java-modbus");
  }
%}
