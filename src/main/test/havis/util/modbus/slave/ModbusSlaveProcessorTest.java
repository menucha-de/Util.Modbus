package havis.util.modbus.slave;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Test;

import havis.util.modbus.DataType;
import havis.util.modbus.Field;
import havis.util.modbus.RegisterType;

public class ModbusSlaveProcessorTest {
	private static final Logger log = Logger.getLogger(ModbusSlaveProcessorTest.class.getName());

	class TestModbusSlaveProcessor extends FieldModbusSlaveProcessor {

		Map<Field, Object> fields = new HashMap<>();

		public TestModbusSlaveProcessor(List<Field> fields) {
			super(fields);
		}

		@Override
		public void connect() throws ModbusSlaveException {

		}

		@Override
		public void disconnect() throws ModbusSlaveException {

		}

		@Override
		public Object read(Field field) throws ModbusSlaveException {
			Object value = fields.get(field);
			log.info("READ: " + field + " = " + value);
			return value;
		}

		@Override
		public void write(Field field, Object value) throws ModbusSlaveException {
			log.info("WRITE: " + field + " = " + value);
			fields.put(field, value);

		}
	}

	@Test
	public void test() throws Exception {
		List<Field> fields = Arrays.asList( //
				new Field[] { //
						new Field(RegisterType.HOLDING_REGISTERS, DataType.BYTE, 0, 1), //
						new Field(RegisterType.HOLDING_REGISTERS, DataType.BYTE, 1, 1) //
				} //
		);
		FieldModbusSlaveProcessor processor = new TestModbusSlaveProcessor(fields);
		processor.start(4711);
		System.in.read();
		processor.stop(5000);
	}
}