#include "modbus_rtu.h"
#include "common/MutexLock.h"
#include <modbus.h> // modbus_new_rtu
#include <stddef.h> // NULL

using namespace CommonNamespace;

ModbusRtu::ModbusRtu() {
}

ModbusRtu::~ModbusRtu() {
}

int ModbusRtu::newRtu(const char *device, int baudrate, char parity,int dataBits, int stopBits) {
  MutexLock lock(*mutex);
  context = modbus_new_rtu(device, baudrate, parity, dataBits, stopBits);
  sockets = NULL;
  socketCount = 0;
  maxSocketCount = 0;
  return context == NULL ? -1 : 0;
}

int ModbusRtu::setSerialMode(int serialMode) {
  MutexLock lock(*mutex);
  modbus_rtu_set_serial_mode(static_cast<modbus_t*>(context), serialMode);
}

