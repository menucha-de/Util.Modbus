package havis.util.modbus;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.util.modbus.UInt16Array;
import havis.util.modbus.UInt8Array;
import havis.util.modbus.slave.ModbusSlaveException;

public class Mapper {

	private static final Logger log = Logger.getLogger(Mapper.class.getName());
	private Floater floater;

	public Mapper(Floater floater) {
		this.floater = floater;
	}

	private Object getBools(UInt8Array srcBits, int srcBitsSize, int address, int addressQuantity, String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcBitsSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcBitsSize);
		}
		boolean[] ret = new boolean[addressQuantity];
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			short shortValue = srcBits.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read" + description + " " + key + ": 0x" + String.format("%02X", shortValue));
			}
			ret[i] = (shortValue == 1);
		}
		return ret;
	}

	public Object get(UInt8Array destBits, int destBitsSize, Field field, String description) throws ModbusSlaveException {
		switch (field.getDataType()) {
		case BOOLEAN:
			return getBools(destBits, destBitsSize, field.getAddress(), field.getAddressQuantity(), description);
		default:
			throw new ModbusSlaveException(
					"Unknown data type for bits at address " + field.getAddress() + ": " + field.getDataType() + " (supported: BOOLEAN)");
		}
	}

	private Object getBytes(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity, String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		byte[] ret = new byte[addressQuantity * 2];
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			short shortValue = (short) srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": 0x" + String.format("%04X", shortValue));
			}
			ret[i * 2] = (byte) (shortValue >> 8);
			ret[i * 2 + 1] = (byte) shortValue;
		}
		return ret;
	}

	private Object getShorts(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity, String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		short[] ret = new short[addressQuantity];
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			short shortValue = (short) srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": 0x" + String.format("%04X", shortValue));
			}
			ret[i] = shortValue;
		}
		return ret;
	}

	private Object getUShorts(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity, String description)
			throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		int[] ret = new int[addressQuantity];
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			int intValue = srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": " + intValue);
			}
			ret[i] = intValue;
		}
		return ret;
	}

	private Object getFloats(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity, String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		float[] ret = new float[addressQuantity];
		UInt16Array floatRegisters = new UInt16Array(2);
		try {
			// for each float value
			for (int i = 0; i < addressQuantity / 2; i++) {
				// for each integer value
				for (int j = 0; j < 2; j++) {
					int key = address + i * 2 + j;
					short shortValue = (short) srcRegisters.getitem(key);
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "Read " + description + " " + key + ": 0x" + String.format("%04X", shortValue));
					}
					floatRegisters.setitem(j, shortValue);
				}
				// set float value to register array
				float floatValue = floater.getFloat(floatRegisters);
				ret[i] = floatValue;
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "  -> " + floatValue);
				}
			}
		} finally {
			floatRegisters.delete();
		}
		return ret;
	}

	private Object getStrings(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity, Charset encoding, String description)
			throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}

		List<Byte> byteList = new ArrayList<>();
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			short shortValue = (short) srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": 0x" + String.format("%04X", shortValue));
			}
			byteList.add((byte) (shortValue >> 8));
			byteList.add((byte) shortValue);
		}
		byte[] bytes = new byte[byteList.size()];
		for (int i = 0; i < byteList.size(); i++) {
			bytes[i] = byteList.get(i);
		}
		String stringValue = new String(bytes, StandardCharsets.UTF_8);
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "  -> " + stringValue);
		}
		return new String[] { stringValue };
	}

	public Object get(UInt16Array destRegisters, int destRegistersSize, Field field, String description) throws ModbusSlaveException {
		switch (field.getDataType()) {
		case BYTE:
			return getBytes(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), description);
		case SHORT:
			return getShorts(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), description);
		case USHORT:
			return getUShorts(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), description);
		case FLOAT:
			return getFloats(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), description);
		case STRING:
			return getStrings(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), StandardCharsets.UTF_8, description);
		default:
			throw new ModbusSlaveException("Unknown data type for registers at address " + field.getAddress() + ": " + field.getDataType()
					+ " (supported: BYTE, SHORT, USHORT, FLOAT, STRING)");
		}
	}

	private void setBoolValues(UInt8Array destBits, int destBitsSize, int address, int addressQuantity, boolean[] values, String description)
			throws ModbusSlaveException {
		if (address + addressQuantity > destBitsSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destBitsSize);
		}
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			short shortValue = (values != null && i < values.length && values[i]) ? (short) 1 : (short) 0;
			// set value to registers
			int key = address + i;
			destBits.setitem(key, shortValue);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Wrote " + description + " " + key + ": 0x" + String.format("%02X", shortValue));
			}
		}
	}

	public void set(UInt8Array destBits, int destBitsSize, Field field, Object value, String description) throws ModbusSlaveException {
		if (value == null) {
			setBoolValues(destBits, destBitsSize, field.getAddress(), field.getAddressQuantity(),
					(boolean[]) null /* value */, description);
			return;
		}
		switch (field.getDataType()) {
		case BOOLEAN:
			setBoolValues(destBits, destBitsSize, field.getAddress(), field.getAddressQuantity(), (boolean[]) value, description);
			break;
		default:
			throw new ModbusSlaveException(
					"Unknown data type for bits at address " + field.getAddress() + ": " + field.getDataType() + " (supported: BOOLEAN)");
		}
	}

	private void setBytes(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity, byte[] values, String description)
			throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		for (int i = 0; i < addressQuantity * 2; i += 2) {
			byte b1 = (values != null && i < values.length) ? values[i] : 0;
			byte b2 = (values != null && i + 1 < values.length) ? values[i + 1] : 0;
			int intValue = (b1 << 8 | b2 & 0x00FF) & 0xFFFF;
			// set value to registers
			int key = address + i / 2;
			destRegisters.setitem(key, intValue);
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Wrote " + description + " " + key + ": 0x" + String.format("%04X", intValue));
			}
		}
	}

	private void setShorts(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity, short[] values, String description)
			throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			short v = (values != null && i < values.length) ? values[i] : 0;
			int intValue = v & 0xFFFF;
			// set value to registers
			int key = address + i;
			destRegisters.setitem(key, intValue);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Wrote " + description + " " + key + ": 0x" + String.format("%04X", intValue));
			}
		}
	}

	private void setUShorts(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity, int[] values, String description)
			throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			int intValue = (values != null && i < values.length) ? values[i] : 0;
			// set value to registers
			int key = address + i;
			destRegisters.setitem(key, intValue);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Wrote " + description + " " + key + ": " + intValue);
			}
		}
	}

	private void setFloats(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity, float[] values, String description)
			throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		UInt16Array floatRegisters = new UInt16Array(2);
		try {
			// for each value
			for (int i = 0; i < addressQuantity / 2; i++) {
				float floatValue = (values != null && i < values.length) ? values[i] : 0;
				// set float value to register array
				floater.setFloat(floatValue, floatRegisters);
				// for each register value
				for (int j = 0; j < 2; j++) {
					int intValue = floatRegisters.getitem(j);
					// set value to registers
					int key = address + i * 2 + j;
					destRegisters.setitem(key, intValue);
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "Wrote " + description + " " + key + ": 0x" + String.format("%04X", intValue));
					}
				}
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, " -> " + floatValue);
				}
			}
		} finally {
			floatRegisters.delete();
		}
	}

	private void setStrings(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity, String[] values, Charset encoding,
			String description) throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		// convert string array to byte array
		byte[] bytes = null;
		String stringValue = null;
		if (values != null) {
			StringBuilder strB = new StringBuilder();
			for (String value : values) {
				if (value != null) {
					strB.append(value);
				}
			}
			stringValue = strB.toString();
			bytes = stringValue.getBytes(encoding);
		}
		// for each value
		for (int i = 0; i < addressQuantity * 2; i += 2) {
			byte b1 = (bytes != null && i < bytes.length) ? bytes[i] : 0;
			byte b2 = (bytes != null && i + 1 < bytes.length) ? bytes[i + 1] : 0;
			int intValue = (b1 << 8 | b2 & 0x00FF) & 0xFFFF;
			// set value to registers
			int key = address + i / 2;
			destRegisters.setitem(key, intValue);
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Wrote " + description + " " + key + ": 0x" + String.format("%04X", intValue));
			}
		}
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "  -> " + stringValue);
		}
	}

	public void set(UInt16Array destRegisters, int destRegistersSize, Field field, Object value, String description) throws ModbusSlaveException {
		if (value == null) {
			setBytes(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(),
					(byte[]) null /* value */, description);
			return;
		}
		switch (field.getDataType()) {
		case BYTE:
			setBytes(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), (byte[]) value, description);
			break;
		case SHORT:
			setShorts(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), (short[]) value, description);
			break;
		case USHORT:
			setUShorts(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), (int[]) value, description);
			break;
		case FLOAT:
			setFloats(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), (float[]) value, description);
			break;
		case STRING:
			setStrings(destRegisters, destRegistersSize, field.getAddress(), field.getAddressQuantity(), (String[]) value, StandardCharsets.UTF_8, description);
			break;
		default:
			throw new ModbusSlaveException("Unknown data type for registers at address " + field.getAddress() + ": " + field.getDataType()
					+ " (supported: BYTE, SHORT, USHORT, FLOAT, STRING)");
		}
	}
}
