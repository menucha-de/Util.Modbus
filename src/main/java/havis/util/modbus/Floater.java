package havis.util.modbus;

import havis.util.modbus.UInt16Array;

public interface Floater {

	void setFloat(float value, UInt16Array destRegisters);

	float getFloat(UInt16Array destRegisters);
}