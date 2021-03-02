#ifndef MODBUS_RTU_H
#define MODBUS_RTU_H

#include "modbus_base.h"
#include <stdint.h>

/*
 * All method excl. client methods are thread safe.
 */
class ModbusRtu : public ModbusBase {
public:
  static const int MODBUS_RTU_MAX_ADU_LENGTH = 256;
  // serial modes
  static const int MODBUS_RTU_RS485 = 1;
  static const int MODBUS_RTU_RS232 = 0;
 
  ModbusRtu();
  virtual ~ModbusRtu();
 
  virtual int newRtu(const char *device, int baudrate, char parity,int dataBits, int stopBits);
  virtual int setSerialMode(int serialMode);
};

#endif // MODBUS_RTU_H
