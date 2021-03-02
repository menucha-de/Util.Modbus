#include "modbus_tcppi.h"
#include "common/MutexLock.h"
#include <modbus.h> // modbus_tcp_pi_listen
#include <stddef.h> // NULL
namespace UnistdNamespace {
#include <unistd.h> // write
}

using namespace CommonNamespace;

class ModbusTcpPiPrivate {
  friend class ModbusTcpPi;
private:
  int serverSocket;
};

ModbusTcpPi::ModbusTcpPi() {
  d = new ModbusTcpPiPrivate();
  d->serverSocket = -1;
}

ModbusTcpPi::~ModbusTcpPi() {
  delete d;
}

int ModbusTcpPi::newTcpPi(const char *node, const char* service) {
  MutexLock lock(*mutex);
  context = modbus_new_tcp_pi(node, service);
  return context == NULL ? -1 : 0;
}

int ModbusTcpPi::tcpPiListen(int nbConnections) {
  MutexLock lock(*mutex);
  d->serverSocket = modbus_tcp_pi_listen(static_cast<modbus_t*>(context), nbConnections);
  if (d->serverSocket < 0) {
    return -1;
  }
  delete[] sockets;
  sockets = new int[nbConnections];
  socketCount = 0;
  maxSocketCount = nbConnections;
  return d->serverSocket;
}

int ModbusTcpPi::tcpPiAccept(int serverSocket) {
  MutexLock lock(*mutex);
  if (d->serverSocket < 0) {
    return -1;
  }
  // wait for reading
  if (wait4read(serverSocket, SELECT_MASK_ACCEPT, lock) < 0) {
    return -1;
  }
  // accept connection
  int socket = modbus_tcp_pi_accept(static_cast<modbus_t*>(context), &serverSocket);
  if (socket >= 0) {
    // if max. socket count has already been reached
    if (socketCount == maxSocketCount) {
      // reject connection by closing the socket
      close(socket);
      return -1;
    }
    sockets[socketCount++] = socket;
  }
  // return the client socket
  return socket;
}

int ModbusTcpPi::selectRead(int* readFds) {
  MutexLock lock(*mutex);
  if (d->serverSocket < 0) {
    return -1;
  }
  // add server socket to fd set
  fd_set readFdSet;
  FD_ZERO(&readFdSet);
  FD_SET(d->serverSocket, &readFdSet);
  int fdMax = d->serverSocket;
  // add client sockets to fd set
  for (int i = 0; i < socketCount; i++) {
    int s = sockets[i];
    FD_SET(s, &readFdSet);
    if (s > fdMax) {
      fdMax = s;
    }
  }
  // wait for reading
  if (wait4read(&readFdSet, fdMax, SELECT_MASK_ALL, lock) < 0) {
    return -1;
  }
  // set sockets to return parameter
  int count = 0;
  if (FD_ISSET(d->serverSocket, &readFdSet)) {
    readFds[count++] = d->serverSocket; 
  }
  for (int i = 0; i < socketCount; i++) {
     int s = sockets[i];
     if (FD_ISSET(s, &readFdSet)) {
       readFds[count++] = s; 
     }
  }
  return count;
}

void ModbusTcpPi::close() {
  MutexLock lock(*mutex);
  // close client sockets
  ModbusBase::close();
  if (d->serverSocket < 0) {
    return;
  }
  if (selectMask & SELECT_MASK_ACCEPT != 0) {
    // write anything to pipe to abort a blocking select call
    UnistdNamespace::write(pipeFds[1], "x", 1);
  }
  // close server socket
  UnistdNamespace::close(d->serverSocket);
  d->serverSocket = -1;
}

void ModbusTcpPi::close(int socket) {
  MutexLock lock(*mutex);
  if (socket == d->serverSocket) {
    // close server socket incl. client sockets
    close();
  } else {
    // close client socket
    ModbusBase::close(socket, lock);
  }
}

void ModbusTcpPi::closeClients() {
  // close client sockets
  ModbusBase::close();
}

