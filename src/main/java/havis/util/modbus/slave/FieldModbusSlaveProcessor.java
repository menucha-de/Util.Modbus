package havis.util.modbus.slave;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.util.modbus.Field;
import havis.util.modbus.Mapper;
import havis.util.modbus.ModbusMapping;
import havis.util.modbus.UInt16Array;
import havis.util.modbus.UInt8Array;
import havis.util.modbus.ModbusBase;

public abstract class FieldModbusSlaveProcessor implements ModbusSlaveProcessor {

	private static final Logger log = Logger.getLogger(ModbusSlaveProcessor.class.getName());
	private List<Field> fields;
	private ModbusSlave slave;
	private Mapper mapper;
	private ExecutorService threadPool;
	private Future<?> future;

	public FieldModbusSlaveProcessor(List<Field> fields) {
		this.fields = fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

	public void start(final int port) {
		slave = new ModbusSlave(this, 5);
		mapper = new Mapper(slave);
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
	}

	public int size() {
		if (fields.isEmpty())
			return 0;
		Field field = fields.get(fields.size() - 1);
		return field.getAddress() + field.getAddressQuantity();
	}

	@Override
	public void connect() throws ModbusSlaveException {
	}

	@Override
	public void disconnect() throws ModbusSlaveException {
	}

	public abstract Object read(Field field) throws ModbusSlaveException;

	public abstract void write(Field field, Object value) throws ModbusSlaveException;

	@Override
	public void read(int slaveId, short functionCode, int address, int addressQuantity, ModbusMapping mapping)
			throws ModbusSlaveException {
		if (fields != null && !fields.isEmpty()) {
			int nr = fields.size() - 1;
			if (address + addressQuantity > fields.get(nr).getAddress() + fields.get(nr).getAddressQuantity()) {
				throw new ModbusSlaveException("ILLEGAL DATA ADDRESS");
			}
		} else {
			throw new ModbusSlaveException("ILLEGAL DATA ADDRESS");
		}
		for (Field field : fields) {
			if (address <= field.getAddress() + field.getAddressQuantity()) {
				Object value = read(field);
				UInt8Array tabBits = null;
				UInt16Array tabRegisters = null;
				try {
					// set field value to mapping
					switch (field.getRegisterType()) {
					case COILS:
						if (functionCode != ModbusBase.MODBUS_FC_READ_COILS) {
							throw new ModbusSlaveException("ILLEGAL FUNCTION");
						}
						tabBits = UInt8Array.frompointer(mapping.getTabBits());
						mapper.set(tabBits, mapping.getNbBits(), field, value, "coil");
						break;
					case DISCRETE_INPUTS:
						if (functionCode != ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS) {
							throw new ModbusSlaveException("ILLEGAL FUNCTION");
						}
						tabBits = UInt8Array.frompointer(mapping.getTabInputBits());
						mapper.set(tabBits, mapping.getNbInputBits(), field, value, "discrete input");
						break;
					case HOLDING_REGISTERS:
						if (functionCode != ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS) {
							throw new ModbusSlaveException("ILLEGAL FUNCTION");
						}
						tabRegisters = UInt16Array.frompointer(mapping.getTabRegisters());
						mapper.set(tabRegisters, mapping.getNbRegisters(), field, value, "holding register");
						break;
					case INPUT_REGISTERS:
						if (functionCode != ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS) {
							throw new ModbusSlaveException("ILLEGAL FUNCTION");
						}
						tabRegisters = UInt16Array.frompointer(mapping.getTabInputRegisters());
						mapper.set(tabRegisters, mapping.getNbInputRegisters(), field, value, "input register");
						break;
					}

				} catch (ModbusSlaveException ex) {
					if (ex.getMessage().equals("ILLEGAL DATA ADDRESS") || ex.getMessage().equals("ILLEGAL FUNCTION")) {
						throw ex;
					}
				}

				catch (Exception ex) {
					// just ignore (we create the mapiing only for the size of request)
				} finally {
					if (tabBits != null) {
						tabBits.delete();
					} else if (tabRegisters != null) {
						tabRegisters.delete();
					}
				}
			}
		}
	}

	@Override
	public void write(int slaveId, short functionCode, int address, int addressQuantity, Date timeStamp,
			ModbusMapping mapping) throws ModbusSlaveException {

		for (Field field : fields) {
			if (address >= field.getAddress() && address <= field.getAddress() + field.getAddressQuantity()) {
				UInt8Array tabBits = null;
				UInt16Array tabRegisters = null;
				try {
					Object value = null;
					// get field value from mapping
					switch (field.getRegisterType()) {
					case COILS:
						if (functionCode != ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL
								|| functionCode != ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS) {
							throw new ModbusSlaveException("ILLEGAL FUNCTION");
						}
						tabBits = UInt8Array.frompointer(mapping.getTabBits());
						value = mapper.get(tabBits, mapping.getNbBits(), field, "coil");
						break;
					case HOLDING_REGISTERS:
						if (functionCode != ModbusBase.MODBUS_FC_WRITE_SINGLE_REGISTER
								|| functionCode != ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS) {
							throw new ModbusSlaveException("ILLEGAL FUNCTION");
						}

						tabRegisters = UInt16Array.frompointer(mapping.getTabRegisters());
						value = mapper.get(tabRegisters, mapping.getNbRegisters(), field, "holding register");
						break;
					default:
						throw new ModbusSlaveException("ILLEGAL FUNCTION");

					}
					// set field value to module
					write(field, value);
				} finally {
					if (tabBits != null) {
						tabBits.delete();
					} else if (tabRegisters != null) {
						tabRegisters.delete();
					}
				}
			}
		}
	}
}
