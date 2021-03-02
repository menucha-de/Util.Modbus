package havis.util.modbus;

public class Field {

	private RegisterType registerType;
	private DataType dataType;
	private int address;
	private int addressQuantity;

	public Field() {
	}

	public Field(RegisterType registerType, DataType dataType, int address, int addressQuantity) {
		this.registerType = registerType;
		this.dataType = dataType;
		this.address = address;
		this.addressQuantity = addressQuantity;
	}

	public RegisterType getRegisterType() {
		return registerType;
	}

	public void setRegisterType(RegisterType type) {
		this.registerType = type;
	}

	public DataType getDataType() {
		return dataType;
	}

	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public int getAddressQuantity() {
		return addressQuantity;
	}

	public void setAddressQuantity(int addressQuantity) {
		this.addressQuantity = addressQuantity;
	}
}