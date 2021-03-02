package havis.util.modbus.slave;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.util.modbus.ModbusBase;
import havis.util.modbus.ModbusMapping;
import havis.util.modbus.UInt16Array;
import havis.util.modbus.UInt8Array;
import havis.util.modbus.slave.ModbusSlave;
import havis.util.modbus.slave.ModbusSlaveException;
import havis.util.modbus.slave.ModbusSlaveProcessor;

public class RtuModbusSlaveProcessor implements ModbusSlaveProcessor {
	private static final Logger log = Logger.getLogger(RtuModbusSlaveProcessor.class.getName());

	private ModbusSlave slave;

	private ExecutorService threadPool;
	private Future<?> future;
	private Lock lock = new ReentrantLock();

	private ModbusBase context;
	private int maxConnectionCount;

	public RtuModbusSlaveProcessor(ModbusBase context, int maxConnectionCount) {
		this.context = context;
		this.maxConnectionCount = maxConnectionCount;
	}

	public void start(final int port) {

		slave = new ModbusSlave(this, maxConnectionCount);
		threadPool = Executors.newFixedThreadPool(1);
		future = threadPool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					slave.open(port);
					slave.run();
				} catch (Throwable e) {
					log.log(Level.SEVERE, "Execution of modbus slave failed", e);
				}
			}
		});
	}

	public void stop(int openCloseTimeout) throws ModbusSlaveException {
		try {
			lock.lock();

			if (future != null) {
				try {
					slave.close(openCloseTimeout);
					future.get();
					future = null;
				} catch (Exception e) {
					log.log(Level.SEVERE, "Cannot close modbus slave", e);
				}
				threadPool.shutdown();
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void connect() throws ModbusSlaveException {

	}

	@Override
	public void disconnect() throws ModbusSlaveException {

	}

	@Override
	public void read(int slaveId, short functionCode, int address, int addressQuantity, ModbusMapping mapping)
			throws ModbusSlaveException {

		synchronized (context) {
			lock.lock();

			try {
				if (future != null && !threadPool.isShutdown()) {
					// set slaveId
					if (slaveId >= 0 && context.setSlave(slaveId) < 0) {
						String msg = "Unable to set slaveId " + slaveId + " : " + context.getErrNo() + " "
								+ context.strError(context.getErrNo());
						throw new ModbusSlaveException(msg);
					}

					UInt8Array bits = null;
					UInt8Array bitsTmp = null;
					UInt16Array registers = null;
					UInt16Array registersTmp = null;
					try {
						// if bits shall be read
						if (functionCode == ModbusBase.MODBUS_FC_READ_COILS // 1
								|| functionCode == ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS) { // 2
							// read values
							bitsTmp = new UInt8Array(addressQuantity);
							int bitCount;
							if (functionCode == ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS) {
								bits = UInt8Array.frompointer(mapping.getTabInputBits());
								bitCount = context.readInputBits(address, addressQuantity, bitsTmp.cast());
							} else {
								bits = UInt8Array.frompointer(mapping.getTabBits());
								bitCount = context.readBits(address, addressQuantity, bitsTmp.cast());
							}
							if (bitCount < 1) {
								throw new ModbusSlaveException(
										"Unable to read " + addressQuantity + " bits at " + address + ": "
												+ context.getErrNo() + " " + context.strError(context.getErrNo()));
							}
							if (log.isLoggable(Level.FINE)) {
								for (int i = 0; i < bitCount; i++) {
									log.log(Level.FINE, "Read " + (address + i) + ": " + bitsTmp.getitem(i));
								}
							}
							// write values to mapping
							for (int i = 0; i < addressQuantity; i++) {
								bits.setitem(address + i, bitsTmp.getitem(i));
							}
						} else if (functionCode == ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS // 3
								|| functionCode == ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS) { // 4
							// read values
							registersTmp = new UInt16Array(addressQuantity);
							int registerCount;
							if (functionCode == ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS) {
								registers = UInt16Array.frompointer(mapping.getTabInputRegisters());
								registerCount = context.readInputRegisters(address, addressQuantity,
										registersTmp.cast());
							} else {
								registers = UInt16Array.frompointer(mapping.getTabRegisters());
								registerCount = context.readRegisters(address, addressQuantity, registersTmp.cast());
							}
							if (registerCount < 1) {
								throw new ModbusSlaveException(
										"Unable to read " + addressQuantity + " registers at " + address + ": "
												+ context.getErrNo() + " " + context.strError(context.getErrNo()));
							}
							if (log.isLoggable(Level.FINE)) {
								for (int i = 0; i < registerCount; i++) {
									log.log(Level.FINE, "Read " + (address + i) + ": 0x"
											+ String.format("%04X", registersTmp.getitem(i)));
								}
							}
							// write values to mapping
							for (int i = 0; i < addressQuantity; i++) {
								registers.setitem(address + i, registersTmp.getitem(i));
							}
						} else {
							throw new ModbusSlaveException("Invalid function code for reading values: " + functionCode
									+ " (supported: 1, 2, 3, 4");
						}
					} finally {
						if (bitsTmp != null) {
							bitsTmp.delete();
							if (bits != null) {
								bits.delete();
							}
						}
						if (registersTmp != null) {
							registersTmp.delete();
							if (registers != null) {
								registers.delete();
							}
						}
					}
				}
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public void write(int slaveId, short functionCode, int address, int addressQuantity, Date timeStamp,
			ModbusMapping mapping) throws ModbusSlaveException {
		synchronized (context) {
			// set slaveId
			if (slaveId >= 0 && context.setSlave(slaveId) < 0) {
				String msg = "Unable to set slaveId " + slaveId + " : " + context.getErrNo() + " "
						+ context.strError(context.getErrNo());
				throw new ModbusSlaveException(msg);
			}
			UInt8Array bits = null;
			UInt8Array bitsTmp = null;
			UInt16Array registers = null;
			UInt16Array registersTmp = null;
			try {
				if (functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL // 5
						|| functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS) { // 15
					bits = UInt8Array.frompointer(mapping.getTabBits());
					if (log.isLoggable(Level.FINE)) {
						for (int i = 0; i < addressQuantity; i++) {
							log.log(Level.FINE, "Writing " + (address + i) + ": " + bits.getitem(address + i));
						}
					}
					// write values
					if (functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL) {
						for (int i = 0; i < addressQuantity; i++) {
							context.writeBit(address + i, bits.getitem(address + i));
						}
					} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS) {
						bitsTmp = new UInt8Array(addressQuantity);
						for (int i = 0; i < addressQuantity; i++) {
							bitsTmp.setitem(i, bits.getitem(address + i));
						}
						context.writeBits(address, addressQuantity, bitsTmp.cast());
					}
				} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_REGISTER // 6
						|| functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS) { // 16
					registers = UInt16Array.frompointer(mapping.getTabRegisters());
					if (log.isLoggable(Level.FINE)) {
						for (int i = 0; i < addressQuantity; i++) {
							log.log(Level.FINE, "Writing " + (address + i) + ": 0x"
									+ String.format("%04X", registers.getitem(address + i)));
						}
					}
					// write values
					if (functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_REGISTER) {
						for (int i = 0; i < addressQuantity; i++) {
							context.writeRegister(address + i, registers.getitem(address + i));
						}
					} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS) {
						registersTmp = new UInt16Array(addressQuantity);
						for (int i = 0; i < addressQuantity; i++) {
							registersTmp.setitem(i, registers.getitem(address + i));
						}
						context.writeRegisters(address, addressQuantity, registersTmp.cast());
					}
				} else {
					throw new ModbusSlaveException(
							"Invalid function code for writing values: " + functionCode + " (supported: 5, 6, 15, 16)");
				}
			} finally {
				if (bits != null) {
					bits.delete();
					if (bitsTmp != null) {
						bitsTmp.delete();
					}
				}
				if (registers != null) {
					registers.delete();
					if (registersTmp != null) {
						registersTmp.delete();
					}
				}
			}
		}
	}
}