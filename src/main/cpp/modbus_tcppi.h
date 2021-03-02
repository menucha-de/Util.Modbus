#ifndef MODBUS_TCPPI_H
#define MODBUS_TCPPI_H

#include "modbus_base.h"
#include <stdint.h>

/*
 * The connection and server methods + "getErrNo", "strError" are thread safe.
 */
class ModbusTcpPiPrivate;
class ModbusTcpPi : public ModbusBase {
public:
  static const int MODBUS_TCP_MAX_ADU_LENGTH = 256;
 
  ModbusTcpPi();
  virtual ~ModbusTcpPi();
 
  virtual int newTcpPi(const char* node, const char* service);
  
  // server
  virtual int tcpPiListen(int nbConnections);
  virtual int tcpPiAccept(int socket);

  virtual int selectRead(int* readFds);

  virtual void close();
  virtual void close(int socket);
  virtual void closeClients();
private:
  ModbusTcpPiPrivate* d;
};

#endif // MODBUS_TCPPI_H
