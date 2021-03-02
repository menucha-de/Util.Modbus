package havis.util.modbus.slave;

import java.util.Date;

import havis.util.modbus.ModbusMapping;

public interface ModbusSlaveProcessor {

	void connect() throws ModbusSlaveException;

	void disconnect() throws ModbusSlaveException;

	void read(int slaveId, short functionCode, int address, int addressQuantity, ModbusMapping mapping) throws ModbusSlaveException;

	void write(int slaveId, short functionCode, int address, int addressQuantity, Date timeStamp, ModbusMapping mapping) throws ModbusSlaveException;
}