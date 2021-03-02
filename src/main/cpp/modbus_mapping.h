#ifndef MODBUS_MAPPING_H
#define MODBUS_MAPPING_H

#include <stdint.h>

class ModbusMappingPrivate;
class ModbusMapping {
public:
  ModbusMapping(int nbBits, int nbInputBits, int nbInputRegisters, int nbRegisters,
    uint8_t* tabBits, uint8_t* tabInputBits, uint16_t* tabInputRegisters, uint16_t* tabRegisters);
  virtual ~ModbusMapping();

  virtual int getNbBits();
  virtual int getNbInputBits();
  virtual int getNbInputRegisters();
  virtual int getNbRegisters();

  virtual uint8_t* getTabBits();
  virtual uint8_t* getTabInputBits();
  virtual uint16_t* getTabInputRegisters();
  virtual uint16_t* getTabRegisters();

private:
  ModbusMappingPrivate* d;
};

#endif // MODBUS_MAPPING_H
