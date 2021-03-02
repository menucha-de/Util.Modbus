package havis.util.modbus.slave;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.util.modbus.Floater;
import havis.util.modbus.IntArray;
import havis.util.modbus.ModbusBase;
import havis.util.modbus.ModbusMapping;
import havis.util.modbus.ModbusTcpPi;
import havis.util.modbus.UInt16Array;
import havis.util.modbus.UInt8Array;

public class ModbusSlave implements Floater {

	private static final Logger log = Logger.getLogger(ModbusSlave.class.getName());

	private ModbusSlaveProcessor slaveProcessor;
	private int maxConnectionCount;
	private int serverSocket = -1;
	private ModbusTcpPi ctx = null;

	private Lock lock = new ReentrantLock();
	private Condition stopped = lock.newCondition();
	private int stopState;

	public ModbusSlave(ModbusSlaveProcessor slaveProcessor, int maxConnectionCount) {
		this.slaveProcessor = slaveProcessor;
		this.maxConnectionCount = maxConnectionCount;
	}

	public void open(int port) throws ModbusSlaveException {
		ctx = new ModbusTcpPi();
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Opening server socket on port " + port);
		}
		if (ctx.newTcpPi("::0", Integer.toString(port)) < 0) {
			// delete class instance
			ctx.delete();
			ctx = null;
			throw new ModbusSlaveException("Unable to create a TCP context");
		}
		// set debug mode
		ctx.setDebug(log.isLoggable(Level.FINE));
		// open slave
		serverSocket = ctx.tcpPiListen(maxConnectionCount);
		if (serverSocket < 0) {
			String msg = "Unable to open slave: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo());
			// destroy context
			ctx.free();
			// delete class instance
			ctx.delete();
			ctx = null;
			throw new ModbusSlaveException(msg);
		}
	}

	public void close(int openCloseTimeout) throws ModbusSlaveException {
		if (ctx == null) {
			return;
		}
		if (serverSocket >= 0) {
			lock.lock();
			try {
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Closing server socket");
				}
				stopState = 1;
				// close the network connection and socket (tcpPiAccept /
				// receive call is aborted)
				ctx.close();
				try {
					while (stopState != 2) {
						if (!stopped.await(openCloseTimeout, TimeUnit.MILLISECONDS)) {
							throw new ModbusSlaveException("Cannot close back end within " + openCloseTimeout + "ms");
						}
					}
				} catch (ModbusSlaveException e) {
					throw e;
				} catch (Exception e) {
					throw new ModbusSlaveException("Closing failed", e);
				}
				stopState = 0;
			} finally {
				lock.unlock();
			}
			serverSocket = -1;
		}
		// destroy context
		ctx.free();
		// delete class instance
		ctx.delete();
		ctx = null;
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Modbus slave closed");
		}
	}

	private boolean isClosing() {
		lock.lock();
		try {
			if (stopState == 1) {
				stopState = 2;
				stopped.signalAll();
				return true;
			}
			return false;
		} finally {
			lock.unlock();
		}
	}

	private ModbusMapping createMapping(int functionCode, int address, int addressQuantity) {
		// increase size of mapping for requested addresses
		int maxAddress = address + addressQuantity;
		int nbBits, nbInputBits, nbRegisters, nbInputRegisters;
		nbBits = nbInputBits = nbRegisters = nbInputRegisters = 0;
		if ((functionCode == ModbusBase.MODBUS_FC_READ_COILS //
				|| functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL || functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS)
				&& nbBits < maxAddress) {
			nbBits = maxAddress;
		} else if (functionCode == ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS && nbInputBits < maxAddress) {
			nbInputBits = maxAddress;
		} else if ((functionCode == ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS //
				|| functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_REGISTER //
				|| functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS) && nbRegisters < maxAddress) {
			nbRegisters = maxAddress;
		} else if (functionCode == ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS && nbInputRegisters < maxAddress) {
			nbInputRegisters = maxAddress;
		}
		return ctx.mappingNew(nbBits /* coils */,
				nbInputBits /* discreteInputs */,
				nbRegisters /* holdingRegisters */,
				nbInputRegisters/* inputRegisters */);
	}

	public void run() {
		int connectionCount = 0;
		boolean isSlaveProcessorConnected = false;
		UInt8Array request = new UInt8Array(ModbusTcpPi.MODBUS_TCP_MAX_ADU_LENGTH);
		IntArray readFds = new IntArray(1 /* serverSocket */ + maxConnectionCount);
		int readFdsCount = 0;
		try {
			while (true) {
				if (readFdsCount == 0) {
					boolean isIncomingConnection;
					do {
						log.log(Level.FINE, "Waiting for data...");
						do {
							readFdsCount = ctx.selectRead(readFds.cast());
							// if slave is being closed
							if (isClosing()) {
								if (isSlaveProcessorConnected) {
									// disconnect slave processor
									try {
										slaveProcessor.disconnect();
									} catch (ModbusSlaveException e) {
										log.log(Level.SEVERE, "Cannot clean up backend", e);
									}
								}
								return;
							}
							if (readFdsCount < 0) {
								log.severe("Waiting for data failed: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
							}
						} while (readFdsCount <= 0);
						isIncomingConnection = readFds.getitem(0) == serverSocket;
						if (isIncomingConnection) {
							// accept the connection
							int clientSocket = ctx.tcpPiAccept(serverSocket);
							if (clientSocket < 0) {
								log.severe("Unable to accept a connection: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
							} else {
								if (log.isLoggable(Level.FINE))
									log.fine("Connection established: " + clientSocket);
								connectionCount++;
								if (!isSlaveProcessorConnected) {
									try {
										// connect slave processor
										slaveProcessor.connect();
										isSlaveProcessorConnected = true;
									} catch (ModbusSlaveException e) {
										log.log(Level.SEVERE, "Cannot initialize backend", e);
									}
								}
							}
						}
					} while (isIncomingConnection);
				}
				if (log.isLoggable(Level.FINE))
					log.fine("Processing request from connection " + readFds.getitem(readFdsCount - 1));
				// set client socket
				ctx.setSocket(readFds.getitem(readFdsCount - 1));
				readFdsCount--;
				// wait for a request
				int requestLength;
				Date timeStamp;
				do {
					requestLength = ctx.receive(request.cast());
					timeStamp = new Date();
					// filtered requests return 0
				} while (requestLength == 0);
				// if an error has occurred
				if (requestLength < 0) {
					// if "Connection reset by peer"
					if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
						if (log.isLoggable(Level.FINE))
							log.fine("Failed to receive message: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
						// close client
						ctx.close(ctx.getSocket());
						connectionCount--;
						if (connectionCount == 0 && isSlaveProcessorConnected) {
							// disconnect slave processor
							try {
								slaveProcessor.disconnect();
								isSlaveProcessorConnected = false;
							} catch (ModbusSlaveException e) {
								log.log(Level.SEVERE, "Cannot clean up backend", e);
							}
						}
					} else {
						log.severe("Failed to receive message: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
					}
					// wait for next connection/request
					continue;
				}
				// if initialization of backend failed
				if (!isSlaveProcessorConnected) {
					log.severe("Discarding request due to failed initialization of backend");
					// send exception response
					if (ctx.replyException(request.cast(), ModbusBase.MODBUS_EXCEPTION_SLAVE_OR_SERVER_FAILURE) < 0) {
						// if "Connection reset by peer"
						if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
							if (log.isLoggable(Level.FINE))
								log.fine("Failed to receive message: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
							// close client
							ctx.close(ctx.getSocket());
							connectionCount--;
						} else {
							log.severe("Failed to send exception response: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
						}
					}
					// wait for next connection/request
					continue;
				}
				ModbusMapping mapping = null;
				try {
					int headerLength = ctx.getHeaderLength();
					// get unitId
					int unitId = request.getitem(headerLength - 1);
					// get function code
					short functionCode = request.getitem(headerLength);
					boolean isRead = functionCode == ModbusBase.MODBUS_FC_READ_COILS || functionCode == ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS
							|| functionCode == ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS || functionCode == ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS;
					boolean isWrite = functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL || functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS
							|| functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_REGISTER || functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS;
					// get address
					int address = ctx.getInt16FromInt8(request.cast(), headerLength + 1);
					// get quantity
					int addressQuantity = 1;
					if (isRead || functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS || functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS) {
						addressQuantity = ctx.getInt16FromInt8(request.cast(), headerLength + 3);
					}
					mapping = createMapping(functionCode, address, addressQuantity);

					// if data shall be read
					if (isRead) {
						if (log.isLoggable(Level.FINE))
							log.fine("Reading data for functionCode=" + functionCode + ", address=" + address + ", addressQuantity=" + addressQuantity);
						// update data
						try {
							slaveProcessor.read(unitId, functionCode, address, addressQuantity, mapping);
						} catch (ModbusSlaveException e) {
							log.log(Level.SEVERE, "Cannot read data for functionCode=" + functionCode + ",address=" + address + ",quantity=" + addressQuantity,
									e);
									long err;
									switch(e.getMessage()){
										case "ILLEGAL FUNCTION":
										err=ModbusBase.MODBUS_EXCEPTION_ILLEGAL_FUNCTION;
										break;
										case "ILLEGAL DATA ADDRESS":
										err=ModbusBase.MODBUS_EXCEPTION_ILLEGAL_DATA_ADDRESS;
										break;
										default:
										err=ModbusBase.MODBUS_EXCEPTION_SLAVE_OR_SERVER_FAILURE;
										break;
									}
						
							if (ctx.replyException(request.cast(), err) < 0) {
								// if "Connection reset by peer"
								if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
									if (log.isLoggable(Level.FINE))
										log.fine("Failed to send exception response: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
									// close client
									ctx.close(ctx.getSocket());
									connectionCount--;
									if (connectionCount == 0) {
										// disconnect slave processor
										try {
											slaveProcessor.disconnect();
											isSlaveProcessorConnected = false;
										} catch (ModbusSlaveException e1) {
											log.log(Level.SEVERE, "Cannot clean up backend", e1);
										}
									}
								} else {
									log.severe("Failed to send exception response: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
								}
							}
							// wait for next connection/request
							continue;
						}
					}
					// send response
					if (ctx.reply(request.cast(), requestLength, mapping) < 0) {
						// if "Connection reset by peer"
						if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
							if (log.isLoggable(Level.FINE))
								log.fine("Failed to send response: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
							// close client
							ctx.close(ctx.getSocket());
							connectionCount--;
							if (connectionCount == 0) {
								// disconnect slave processor
								try {
									slaveProcessor.disconnect();
									isSlaveProcessorConnected = false;
								} catch (ModbusSlaveException e) {
									log.log(Level.SEVERE, "Cannot clean up backend", e);
								}
							}
						} else {
							log.log(Level.SEVERE, "Failed to send response: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
						}
						// wait for next connection/request
						continue;
					}
					if (log.isLoggable(Level.FINE)) {
						String requestDescr = null;
						if (log.isLoggable(Level.FINE)) {
							if (functionCode == ModbusBase.MODBUS_FC_READ_COILS) {
								requestDescr = "READ_COILS";
							} else if (functionCode == ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS) {
								requestDescr = "READ_DISCRETE_INPUTS";
							} else if (functionCode == ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS) {
								requestDescr = "READ_HOLDING_REGISTERS";
							} else if (functionCode == ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS) {
								requestDescr = "READ_INPUT_REGISTERS";
							} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL) {
								requestDescr = "WRITE_SINGLE_COIL";
							} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS) {
								requestDescr = "WRITE_MULTIPLE_COILS";
							} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_REGISTER) {
								requestDescr = "WRITE_SINGLE_REGISTER";
							} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS) {
								requestDescr = "WRITE_MULTIPLE_REGISTERS";
							}
							log.fine("Received " + requestDescr);
						}
						log.fine("Sent " + requestDescr);
					}
					// if data has been written
					if (isWrite) {
						if (log.isLoggable(Level.FINE))
							log.fine("Writing data for functionCode=" + functionCode + ", address=" + address + ", addressQuantity=" + addressQuantity);
						// update data
						try {
							slaveProcessor.write(unitId, functionCode, address, addressQuantity, timeStamp, mapping);
						} catch (ModbusSlaveException e) {
							log.log(Level.SEVERE, "Cannot write data for functionCode=" + functionCode + ",address=" + address + ",quantity=" + addressQuantity,
									e);
						}
					}
				} finally {
					if (mapping != null) {
						destroyMapping(mapping);
						mapping = null;
					}
				}
			}
		} finally {
			// destroy request structure
			request.delete();
			// destroy structure for read fds
			readFds.delete();
		}
	}

	private void destroyMapping(ModbusMapping mapping) {
		// destroy mapping structure
		ctx.mappingFree(mapping);
		// destroy class instance
		mapping.delete();
	}

	public void setFloat(float value, UInt16Array destRegisters) {
		ctx.setFloat(value, destRegisters.cast());
	}

	public float getFloat(UInt16Array destRegisters) {
		return ctx.getFloat(destRegisters.cast());
	}
}