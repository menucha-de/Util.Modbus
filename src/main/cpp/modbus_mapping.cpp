#include "modbus_mapping.h"

class ModbusMappingPrivate {
  friend class ModbusMapping;
private:
  int nbBits;
  int nbInputBits;
  int nbInputRegisters;
  int nbRegisters;
  uint8_t* tabBits;
  uint8_t* tabInputBits;
  uint16_t* tabInputRegisters;
  uint16_t* tabRegisters;
};

ModbusMapping::ModbusMapping(int nbBits, int nbInputBits, int nbInputRegisters, int nbRegisters,
    uint8_t* tabBits, uint8_t* tabInputBits, uint16_t* tabInputRegisters, uint16_t* tabRegisters) {
  d = new ModbusMappingPrivate();
  d->nbBits = nbBits;
  d->nbInputBits = nbInputBits;
  d->nbInputRegisters = nbInputRegisters;
  d->nbRegisters = nbRegisters;
  d->tabBits = tabBits;
  d->tabInputBits = tabInputBits;
  d->tabInputRegisters = tabInputRegisters;
  d->tabRegisters = tabRegisters;
}

ModbusMapping::~ModbusMapping() {
  delete d;
}

int ModbusMapping::getNbBits() {
  return d->nbBits;
}

int ModbusMapping::getNbInputBits() {
  return d->nbInputBits;
}

int ModbusMapping::getNbInputRegisters() {
  return d->nbInputRegisters;
}

int ModbusMapping::getNbRegisters() {
  return d->nbRegisters;
}

uint8_t* ModbusMapping::getTabBits() {
  return d->tabBits;
}

uint8_t* ModbusMapping::getTabInputBits() {
  return d->tabInputBits;
}

uint16_t* ModbusMapping::getTabInputRegisters() {
  return d->tabInputRegisters;
}

uint16_t* ModbusMapping::getTabRegisters() {
  return d->tabRegisters;
}

