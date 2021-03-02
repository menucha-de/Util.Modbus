package havis.util.modbus.slave;

public class ModbusSlaveException extends Exception {

	private static final long serialVersionUID = -1L;

	public ModbusSlaveException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModbusSlaveException(String message) {
		super(message);
	}
}