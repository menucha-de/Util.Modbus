#ifndef MODBUS_BASE_H
#define MODBUS_BASE_H

#include "modbus_mapping.h"
#include "common/Mutex.h"
#include "common/MutexLock.h"
#include <stdint.h>
#include <errno.h>
#include <sys/select.h> // fd_set

/*
 * All method excl. client methods are thread safe.
 */
class ModbusBasePrivate;
class ModbusBase {
public:
  // function codes
  static const int MODBUS_FC_READ_COILS = 1;
  static const int MODBUS_FC_READ_DISCRETE_INPUTS = 2;
  static const int MODBUS_FC_READ_HOLDING_REGISTERS = 3;
  static const int MODBUS_FC_READ_INPUT_REGISTERS = 4;
  static const int MODBUS_FC_WRITE_SINGLE_COIL = 5;
  static const int MODBUS_FC_WRITE_SINGLE_REGISTER = 6;
  static const int MODBUS_FC_WRITE_MULTIPLE_COILS = 15;
  static const int MODBUS_FC_WRITE_MULTIPLE_REGISTERS = 16;
  static const int MODBUS_FC_WRITE_AND_READ_REGISTERS = 23;

  // error numbers (getErrNo)
  static const unsigned int ERRNO_EAGAIN = EAGAIN; // 11: Try again
  static const unsigned int ERRNO_EPIPE = EPIPE; // 32: Broken pipe
  static const unsigned int ERRNO_ECONNRESET = ECONNRESET; // 104: Connection reset by peer
  static const unsigned int ERRNO_ETIMEDOUT = ETIMEDOUT; // 110: Connection timed out
  static const unsigned int ERRNO_ECONNREFUSED = ECONNREFUSED; // 111: Connection refused

  // exception codes (replyException)
  static const unsigned int MODBUS_EXCEPTION_ILLEGAL_FUNCTION = 1;
  static const unsigned int MODBUS_EXCEPTION_ILLEGAL_DATA_ADDRESS = 2;
  static const unsigned int MODBUS_EXCEPTION_ILLEGAL_DATA_VALUE = 3;
  static const unsigned int MODBUS_EXCEPTION_SLAVE_OR_SERVER_FAILURE = 4;
  static const unsigned int MODBUS_EXCEPTION_ACKNOWLEDGE = 5;
  static const unsigned int MODBUS_EXCEPTION_SLAVE_OR_SERVER_BUSY = 6;
  static const unsigned int MODBUS_EXCEPTION_NEGATIVE_ACKNOWLEDGE = 7;
  static const unsigned int MODBUS_EXCEPTION_MEMORY_PARITY = 8;
  static const unsigned int MODBUS_EXCEPTION_NOT_DEFINED = 9;
  static const unsigned int MODBUS_EXCEPTION_GATEWAY_PATH = 10;
  static const unsigned int MODBUS_EXCEPTION_GATEWAY_TARGET = 11;

  // modbus specific error numbers
  static const unsigned int ERRNO_MODBUS_ENOBASE = 112345678;
  static const unsigned int ERRNO_EMBXGTAR = (ERRNO_MODBUS_ENOBASE + MODBUS_EXCEPTION_GATEWAY_TARGET);
  static const unsigned int ERRNO_EMBBADDATA = (ERRNO_EMBXGTAR + 2); // 112345691: Invalid transaction or protocol ID

  // common
  virtual void free();
  virtual void setDebug(bool debug);
  virtual int setSlave(int slaveId);
  virtual void setSocket(int socket);
  virtual int getSocket();
  virtual void setResponseTimeout(long timeout);
  virtual uint8_t getByteFromBits(const uint8_t* src, int index, unsigned int nb);
  virtual void setBitsFromByte(uint8_t* dest, int index, const uint8_t value);
  virtual uint16_t getInt16FromInt8(const uint8_t* tabInt8, int index);
  virtual uint32_t getInt32FromInt16(const uint16_t* tabInt16, int index);
  virtual float getFloat(const uint16_t* src);
  virtual void setFloat(float f, uint16_t* dest);
  virtual int getHeaderLength();
  virtual int getErrNo();
  virtual const char* strError(int errNo);

  // connection
  virtual int connect();
  virtual void close();

  // client
  virtual int readBits(int addr, int nb, uint8_t* returnDest);
  virtual int readInputBits(int addr, int nb, uint8_t* returnDest);
  virtual int readRegisters(int addr, int nb, uint16_t* returnDest);
  virtual int readInputRegisters(int addr, int nb, uint16_t* returnDest);
  virtual int writeBit(int addr, int status);
  virtual int writeBits(int addr, int nb, const uint8_t* src);
  virtual int writeRegister(int addr, int value);
  virtual int writeRegisters(int addr, int nb, const uint16_t* src);
  virtual int writeAndReadRegisters(int writeAddr, int writeNb, const uint16_t* src,
                                    int readAddr, int readNb, uint16_t* dest);

  // server
  virtual ModbusMapping* mappingNew(int nbCoilStatus, int nbInputStatus, 
                                    int nbHoldingRegisters, int nbInputRegisters);
  virtual void mappingFree(ModbusMapping* mbMapping);
  virtual int receive(uint8_t* returnRequest);
  virtual int reply(const uint8_t* request, int requestLength, ModbusMapping* mbMapping);
  virtual int replyException(const uint8_t* request, unsigned int exceptionCode);

protected:
  static const unsigned int SELECT_MASK_NONE = 0;
  static const unsigned int SELECT_MASK_RECEIVE = 1;
  static const unsigned int SELECT_MASK_ACCEPT = 4;
  static const unsigned int SELECT_MASK_ALL = 5;

  void* context; // set by "newRtu" or "newTcpPi", destroyed by "free"
  int* sockets; // destroyed + initialized by "connect"/"tcpPiListen", 
                // filled by "connect"/"tcpPiAccept"
  int socketCount;
  int maxSocketCount;
  int pipeFds[2];
  int selectMask;
  CommonNamespace::Mutex* mutex;

  ModbusBase();
  virtual ~ModbusBase();
  virtual int wait4read(int fd, int selectMask, CommonNamespace::MutexLock& lock);
  virtual int wait4read(fd_set* readFds, int fdMax, int selectMask, CommonNamespace::MutexLock& lock);
  virtual void close(int socket, CommonNamespace::MutexLock& lock);
private:
  ModbusBasePrivate* d;
};

#endif // MODBUS_BASE_H
