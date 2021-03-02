#include "modbus_base.h"
#include <modbus.h>
#include <stddef.h> // NULL
#include <errno.h> // errno
#include <map>
namespace UnistdNamespace {
#include <unistd.h> // pipe
}
#include <fcntl.h> // fcntl

using namespace CommonNamespace;

class ModbusBasePrivate {
  friend class ModbusBase;
private:
  std::map<ModbusMapping*, modbus_mapping_t*> mappings;
};

ModbusBase::ModbusBase() {
  d = new ModbusBasePrivate();
  sockets = NULL;
  socketCount = 0;
  maxSocketCount = 0;
  // self pipe trick:
  // - create an internal pipe
  // - make read + write end nonblocking
  // - add read end of pipe to read fd set for select call
  // - write anything to pipe to abort select call
  UnistdNamespace::pipe(pipeFds);
  fcntl(pipeFds[0], F_SETFL, O_NONBLOCK);
  fcntl(pipeFds[1], F_SETFL, O_NONBLOCK);

  selectMask = SELECT_MASK_NONE;
  mutex = new Mutex(); // MutexException
}

ModbusBase::~ModbusBase() {
  delete mutex;
  // clear pipe and close it
  char ch;
  while (0 < UnistdNamespace::read(pipeFds[0], &ch, 1)) {
  }
  UnistdNamespace::close(pipeFds[1]);
  UnistdNamespace::close(pipeFds[0]);

  delete d;
}

void ModbusBase::free() {
  MutexLock lock(*mutex);
  if (context != NULL) {
    modbus_free(static_cast<modbus_t*>(context));
    context = NULL;
  }
}

void ModbusBase::setDebug(bool debug) {
  MutexLock lock(*mutex);
  modbus_set_debug(static_cast<modbus_t*>(context), debug);
}

int ModbusBase::setSlave(int slaveId) {
  MutexLock lock(*mutex);
  return modbus_set_slave(static_cast<modbus_t*>(context), slaveId);
}

void ModbusBase::setSocket(int socket) {
  MutexLock lock(*mutex);
  modbus_set_socket(static_cast<modbus_t*>(context), socket);
}

int ModbusBase::getSocket() {
  MutexLock lock(*mutex);
  return modbus_get_socket(static_cast<modbus_t*>(context));
}

void ModbusBase::setResponseTimeout(long timeout) {
  timeval tv;
  tv.tv_sec = timeout / 1000;
  tv.tv_usec = (timeout % 1000) * 1000;
  MutexLock lock(*mutex);
  modbus_set_response_timeout(static_cast<modbus_t*>(context), &tv);
}

uint8_t ModbusBase::getByteFromBits(const uint8_t* src, int index, unsigned int nb) {
  return modbus_get_byte_from_bits(src, index, nb);
}

void ModbusBase::setBitsFromByte(uint8_t* dest, int index, const uint8_t value) {
  modbus_set_bits_from_byte(dest, index, value);
}

uint16_t ModbusBase::getInt16FromInt8(const uint8_t* tabInt8, int index) {
  return MODBUS_GET_INT16_FROM_INT8(tabInt8, index);
}

uint32_t ModbusBase::getInt32FromInt16(const uint16_t* tabInt16, int index) {
  return MODBUS_GET_INT32_FROM_INT16(tabInt16, index);
}

float ModbusBase::getFloat(const uint16_t* src) {
#if __FLOAT_WORD_ORDER__ != __ORDER_BIG_ENDIAN__
  // reverse the word order
  uint16_t srcReverse[2] = { src[1], src[0] };
  src = srcReverse;
#endif
  return modbus_get_float(src);
}

void ModbusBase::setFloat(float f, uint16_t* dest) {
#if __FLOAT_WORD_ORDER__ != __ORDER_BIG_ENDIAN__
  // reverse the word order
  float fReverse;
  uint16_t* f2 = (uint16_t*) &f;
  uint16_t* fReverse2 = (uint16_t*) &fReverse;
  fReverse2[0] = f2[1];
  fReverse2[1] = f2[0];
  f = fReverse;
#endif
  modbus_set_float(f, dest);
}

int ModbusBase::getHeaderLength() {
  MutexLock lock(*mutex);
  return modbus_get_header_length(static_cast<modbus_t*>(context));
}

int ModbusBase::getErrNo() {
  MutexLock lock(*mutex);
  return errno;
}

const char* ModbusBase::strError(int errNo) {
  MutexLock lock(*mutex);
  return modbus_strerror(errNo);
}

int ModbusBase::connect() {
  MutexLock lock(*mutex);
  // connect
  int ret = modbus_connect(static_cast<modbus_t*>(context));
  if (ret == 0) {
    // get socket
    delete[] sockets;
    sockets = new int[1];
    sockets[0] = modbus_get_socket(static_cast<modbus_t*>(context));
    socketCount = 1;
    maxSocketCount = 1;
  }
  return ret;
}

void ModbusBase::close() {
  MutexLock lock(*mutex);
  if (socketCount <= 0) {
    return;
  }
  if (selectMask & SELECT_MASK_RECEIVE != 0) {
    // write anything to pipe to abort a blocking select call
    UnistdNamespace::write(pipeFds[1], "x", 1);
  }
  // close sockets
  for (int i = 0; i < socketCount; i++) {
    modbus_set_socket(static_cast<modbus_t*>(context), sockets[i]);
    modbus_close(static_cast<modbus_t*>(context));
  }
  socketCount = 0;
}

int ModbusBase::readBits(int addr, int nb, uint8_t* returnDest) {
  MutexLock lock(*mutex);
  return modbus_read_bits(static_cast<modbus_t*>(context), addr, nb, returnDest);
}

int ModbusBase::readInputBits(int addr, int nb, uint8_t* returnDest) {
  MutexLock lock(*mutex);
  return modbus_read_input_bits(static_cast<modbus_t*>(context), addr, nb, returnDest);
}

int ModbusBase::readRegisters(int addr, int nb, uint16_t* returnDest) {
  MutexLock lock(*mutex);
  return modbus_read_registers(static_cast<modbus_t*>(context), addr, nb, returnDest);
}

int ModbusBase::readInputRegisters(int addr, int nb, uint16_t* returnDest) {
  MutexLock lock(*mutex);
  return modbus_read_input_registers(static_cast<modbus_t*>(context), addr, nb, returnDest);
}

int ModbusBase::writeBit(int addr, int status) {
  MutexLock lock(*mutex);
  return modbus_write_bit(static_cast<modbus_t*>(context), addr, status);
}

int ModbusBase::writeBits(int addr, int nb, const uint8_t* src) {
  MutexLock lock(*mutex);
  return modbus_write_bits(static_cast<modbus_t*>(context), addr, nb, src);
}

int ModbusBase::writeRegister(int addr, int value) {
  MutexLock lock(*mutex);
  return modbus_write_register(static_cast<modbus_t*>(context), addr, value);
}

int ModbusBase::writeRegisters(int addr, int nb, const uint16_t* src) {
  MutexLock lock(*mutex);
  return modbus_write_registers(static_cast<modbus_t*>(context), addr, nb, src);
}

int ModbusBase::writeAndReadRegisters(int writeAddr, int writeNb, const uint16_t* src,
                                    int readAddr, int readNb, uint16_t* dest) {
  MutexLock lock(*mutex);
  return modbus_write_and_read_registers(static_cast<modbus_t*>(context), writeAddr, writeNb, src,
    readAddr, readNb, dest);
}

ModbusMapping* ModbusBase::mappingNew(int nbCoilStatus, int nbInputStatus, int nbHoldingRegisters,
    int nbInputRegisters) {
  MutexLock lock(*mutex);
  modbus_mapping_t* map = modbus_mapping_new(nbCoilStatus, nbInputStatus, nbHoldingRegisters,
                                             nbInputRegisters);
  if (map == NULL) {
    return NULL;
  }
  // set the references to the original data arrays to the return value
  // The method "reply" modifies the content of the data arrays on write requests.
  ModbusMapping* mbMapping = new ModbusMapping(
    map->nb_bits, map->nb_input_bits, map->nb_input_registers, map->nb_registers, 
    map->tab_bits, map->tab_input_bits, map->tab_input_registers, map->tab_registers);
  d->mappings[mbMapping] = map;
  return mbMapping;
}

void ModbusBase::mappingFree(ModbusMapping* mbMapping) {
  MutexLock lock(*mutex);
  std::map<ModbusMapping*, modbus_mapping_t*>::iterator it = d->mappings.find(mbMapping);
  if (it != d->mappings.end()) {
    modbus_mapping_free(it->second);
    d->mappings.erase(it);
  }
  delete mbMapping;
}

int ModbusBase::receive(uint8_t* returnRequest) {
  MutexLock lock(*mutex);
  // get current socket
  int socket = modbus_get_socket(static_cast<modbus_t*>(context));
  bool found = false;
  for (int i = 0; i < socketCount; i++) {
    if (sockets[i] == socket) {
       found = true;
       break;
    }
  }
  if (!found) {
    return -1;
  }
  // wait for reading
  if (wait4read(socket, SELECT_MASK_RECEIVE, lock) < 0) {
    return -1;
  }
  // receive data
  return modbus_receive(static_cast<modbus_t*>(context), returnRequest);
}

int ModbusBase::reply(const uint8_t* request, int requestLength, ModbusMapping* mbMapping) {
  MutexLock lock(*mutex);
  // get libmodbus mapping
  std::map<ModbusMapping*, modbus_mapping_t*>::iterator it = d->mappings.find(mbMapping);
  if (it != d->mappings.end()) {
    // send response
    return modbus_reply(static_cast<modbus_t*>(context), request, requestLength, it->second);
  }
  return -1;
}

int ModbusBase::replyException(const uint8_t* request, unsigned int exceptionCode) {
  MutexLock lock(*mutex);
  return modbus_reply_exception(static_cast<modbus_t*>(context), request, exceptionCode);
}

int ModbusBase::wait4read(int fd, int selectMask, MutexLock& lock) {
  // add fd to a set
  fd_set readFds;
  FD_ZERO(&readFds);
  FD_SET(fd, &readFds);
  // wait for reading
  return wait4read(&readFds, fd /* fdMax */, selectMask, lock);
}

int ModbusBase::wait4read(fd_set* readFds, int fdMax, int selectMask, MutexLock& lock) {
  // add read end of pipe to fd set
  int pipeReadFd = pipeFds[0];
  FD_SET(pipeReadFd, readFds);
  if (pipeReadFd > fdMax) {
      fdMax = pipeReadFd;
  }
  // select
  this->selectMask = selectMask;
  lock.unlock();
  int count = select(fdMax + 1, readFds, NULL /* writeFds */, 
                     NULL /* exceptFds */, NULL /* timeout */);
  lock.lock();
  this->selectMask = SELECT_MASK_NONE;
  // if data were sent to the pipe
  if (FD_ISSET(pipeReadFd, readFds)) {
    // return error status
    count = -1;
  }
  // always clear pipe (data may be send to the pipe between "select" and "lock" call)
  char ch;
  while (0 < UnistdNamespace::read(pipeFds[0], &ch, 1)) {
  }
  // remove read end of pipe from fd set
  FD_CLR(pipeReadFd, readFds);
  return count;
}

void ModbusBase::close(int socket, MutexLock& lock) {
  // get current socket
  int s = modbus_get_socket(static_cast<modbus_t*>(context));
  // remove socket from list
  bool found = false;
  for (int i = 0; i < socketCount; i++) {
    if (found) {
      sockets[i - 1] = sockets[i];
    } else if (sockets[i] == socket) {
      found = true;
    }
  }
  if (found) {
    socketCount--;
    if (socket == s && selectMask != SELECT_MASK_ALL && selectMask & SELECT_MASK_RECEIVE != 0) {
      // write anything to pipe to abort a blocking select call
      UnistdNamespace::write(pipeFds[1], "x", 1);
    }
    // close socket
    if (socket != s) {
      modbus_set_socket(static_cast<modbus_t*>(context), socket);
    }
    modbus_close(static_cast<modbus_t*>(context));
    if (socket != s) {
      modbus_set_socket(static_cast<modbus_t*>(context), s);
    }
  }
}

