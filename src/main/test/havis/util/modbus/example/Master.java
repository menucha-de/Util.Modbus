package havis.util.modbus.example;

import havis.util.modbus.ModbusBase;
import havis.util.modbus.ModbusRtu;
import havis.util.modbus.ModbusTcpPi;
import havis.util.modbus.UInt8Array;
import havis.util.modbus.UInt16Array;

public class Master {

  public static void main(String argv[]) {
    int mode = 1; // 0: RTU, 1: TCP PI

    // RTU
    String device = "/dev/ttyS11"; // local pseudo device
//    String device = "/dev/ttyACM0"; // mica
//    String device = "/dev/ttyUSB0"; // local USB adapter
    int baudrate = 115200;
    char parity = 'N';
    int dataBits = 8;
    int stopBits = 2;
    int serialMode = ModbusRtu.MODBUS_RTU_RS485;

    // TCP PI
    String node = "127.0.0.1";
    String service = "1502";

    int slaveId = 1;
    int addr = 19050;
    int wordCount = 2;
    int responseTimeout = 2500; // in ms
    boolean debug = false;

    ModbusBase ctx = null;
    switch (mode) {
    case 0: // RTU
      // create RTU context
      ModbusRtu ctxRtu = new ModbusRtu();
      if (ctxRtu.newRtu(device, baudrate, parity, dataBits, stopBits) < 0) {
        System.err.println("Unable to create the modbus context");
        return;
      }
      // set serial mode
      if (ctxRtu.setSerialMode(serialMode) < 0) {
        System.err.println("Unable to set serial mode " + 
          (serialMode == ModbusRtu.MODBUS_RTU_RS232 ? "MODBUS_RTU_RS232" : "MODBUS_RTU_RS485") + ": "
          + ctxRtu.getErrNo() + " " + ctxRtu.strError(ctxRtu.getErrNo()));
      }
      ctx = ctxRtu;
      break;
    case 1: // TCP PI
      // create TCP PI context
      ModbusTcpPi ctxTcpPi = new ModbusTcpPi();
      if (ctxTcpPi.newTcpPi(node, service) < 0) {
        System.err.println("Unable to create the TCP PI context: "
          + ctxTcpPi.getErrNo() + " " + ctxTcpPi.strError(ctxTcpPi.getErrNo()));
        return;
      }
      ctx = ctxTcpPi;
      break;
    }
    try {
      // set debug mode
      ctx.setDebug(debug);
      // set response timeout
      ctx.setResponseTimeout(responseTimeout);
      // set slaveId
      if (ctx.setSlave(slaveId) < 0) {
        System.err.println("Unable to set slaveId " + slaveId 
          + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
        return;
      }
      // connect to slave
      if (ctx.connect() < 0) {
        System.err.println("Unable to connect: " 
          + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
        return;
      }
      try {
        // read coils (invalid address)
        UInt8Array coils = readCoils(ctx, addr - 1, wordCount);

        // read coils: 0x42 0x47 0xFC 0x73
        coils = readCoils(ctx, addr, wordCount);
        if (coils == null) {
          return;
        }
        coils.delete();
     
        // write single coils: 0x73 -> 0x76
        writeCoil(ctx, addr + 24, 0);
        writeCoil(ctx, addr + 25, 1);
        writeCoil(ctx, addr + 26, 1);
        coils = readCoils(ctx, addr, wordCount);
        if (coils == null) {
          return;
        }
        coils.delete();
     
        // write coils: 0x76 -> 0x73
        // new class instances are created in cpp => "delete" must be called
        coils = new UInt8Array(8);
        ctx.setBitsFromByte(coils.cast(), 0 /*index*/, (short) 3);
        writeCoils(ctx, addr + 24, 3 /*nb*/, coils);
        coils.delete();
        coils = readCoils(ctx, addr, wordCount);
        if (coils == null) {
          return;
        }
        coils.delete();
     
        // read discrete inputs
        UInt8Array discreteInputs = readDiscreteInputs(ctx, addr, wordCount);
        if (discreteInputs == null) {
          return;
        }
        discreteInputs.delete();
        
        // read holding registers: 0x4247 0xFC73
        UInt16Array holdingRegisters = readHoldingRegisters(ctx, addr, wordCount);
        if (holdingRegisters == null) {
          return;
        }
        holdingRegisters.delete();
     
        // write single holding registers: 0x4248 0xFC74
        writeHoldingRegister(ctx, addr, 0x4248);
        writeHoldingRegister(ctx, addr + 1, 0xFC74);
        holdingRegisters = readHoldingRegisters(ctx, addr, wordCount);
        if (holdingRegisters == null) {
          return;
        }
        holdingRegisters.delete();
     
        // write holding registers: 0x4247 0xFC73
        // new class instances are created in cpp => "delete" must be called
        UInt16Array holdingRegistersValues = new UInt16Array(2);
        holdingRegistersValues.setitem(0, 0x4247); 
        holdingRegistersValues.setitem(1, 0xFC73); 
        writeHoldingRegisters(ctx, addr, 2, holdingRegistersValues);
        holdingRegistersValues.delete();
        holdingRegisters = readHoldingRegisters(ctx, addr, wordCount);
        if (holdingRegisters == null) {
          return;
        }
        holdingRegisters.delete();
     
        // write and read holding registers: 0x4248 0xFC74
        holdingRegistersValues = new UInt16Array(2);
        holdingRegistersValues.setitem(0, 0x4248); 
        holdingRegistersValues.setitem(1, 0xFC74); 
        holdingRegisters = writeAndReadHoldingRegisters(ctx, addr, 2, holdingRegistersValues, addr, 2);
        holdingRegistersValues.delete();
        if (holdingRegisters == null) {
          return;
        }
        holdingRegisters.delete();
        // write and read holding registers: 0x4247 0xFC73
        holdingRegistersValues = new UInt16Array(2);
        holdingRegistersValues.setitem(0, 0x4247); 
        holdingRegistersValues.setitem(1, 0xFC73); 
        holdingRegisters = writeAndReadHoldingRegisters(ctx, addr, 2, holdingRegistersValues, addr, 2);
        holdingRegistersValues.delete();
        if (holdingRegisters == null) {
          return;
        }
        holdingRegisters.delete();

        // read input registers: 0x4248 0xFC74
        UInt16Array inputRegisters = readInputRegisters(ctx, addr, wordCount);
        if (inputRegisters == null) {
          return;
        }
        inputRegisters.delete();
      } finally {
        // close connection
        ctx.close();
      } 
    } finally {
      // destroy context
      ctx.free();
      // destroy class instance
      ctx.delete();
    }
  }


  private static UInt8Array readCoils(ModbusBase ctx, int addr, int wordCount) {
    // new class instances are created in cpp => "delete" must be called
    UInt8Array bits = new UInt8Array(wordCount * 16); 
    int rc = ctx.readBits(addr, wordCount * 16, bits.cast());
    if (rc < 0) {
      System.err.println("Unable to read " + (wordCount * 16) + " coils at " + addr 
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return null;
    }
    // for each byte
    for (int i = 0; i < rc / 8; i++) {
      System.out.println("Coils " + (addr + i * 8 + 7) + "-" + (addr + i * 8) 
        + ": 0x" + String.format("%02X", ctx.getByteFromBits(bits.cast(), i * 8, 8)));
      // for each bit
      for (int j = 0; j < 8; j++) {
        System.out.println("Coil " + (addr + i * 8 + j) 
          + ": " + bits.getitem(i * 8 + j));
      }
    }
    return bits;
  }
 
  private static void writeCoil(ModbusBase ctx, int addr, int status) {
    int rc = ctx.writeBit(addr, status);
    if (rc < 0) {
      System.err.println("Unable to write coil at " + addr
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return;
    }
  }

  private static void writeCoils(ModbusBase ctx, int addr, int nb, UInt8Array bits) {
    // new class instances are created in cpp => "delete" must be called
    int rc = ctx.writeBits(addr, nb, bits.cast());
    if (rc < 0) {
      System.err.println("Unable to write " + nb + " coils at " + addr
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return;
    }
  }

  private static UInt8Array readDiscreteInputs(ModbusBase ctx, int addr, int wordCount) {
    // new class instances are created in cpp => "delete" must be called
    UInt8Array bits = new UInt8Array(wordCount * 16); 
    int rc = ctx.readInputBits(addr, wordCount * 16, bits.cast());
    if (rc < 0) {
      System.err.println("Unable to read " + (wordCount * 16) + " discrete inputs at " + addr
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return null;
    }
    // for each byte
    for (int i = 0; i < rc / 8; i++) {
      System.out.println("Discrete inputs " + (addr + i * 8 + 7) + "-" + (addr + i * 8) 
        + ": 0x" + String.format("%02X", ctx.getByteFromBits(bits.cast(), i * 8, 8)));
      // for each bit
      for (int j = 0; j < 8; j++) {
        System.out.println("Discrete input " + (addr + i * 8 + j) 
          + ": " + bits.getitem(i * 8 + j));
      }
    }
    return bits;
  }

  private static UInt16Array readHoldingRegisters(ModbusBase ctx, int addr, int wordCount) {
    // new class instances are created in cpp => "delete" must be called
    UInt16Array registers = new UInt16Array(wordCount); 
    int rc = ctx.readRegisters(addr, wordCount, registers.cast());
    if (rc < 0) {
      System.err.println("Unable to read " + wordCount + " holding registers at " + addr
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return null;
    }
    for (int i=0; i < rc; i++) {
      System.out.println("Holding register " + (addr + i) 
        + ": 0x" + String.format("%04X", registers.getitem(i)));
    }
    float value = ctx.getFloat(registers.cast());
    UInt16Array floatRegisters = new UInt16Array(2);     
    ctx.setFloat(value, floatRegisters.cast());
    float value2 = ctx.getFloat(floatRegisters.cast());
    floatRegisters.delete();
    System.err.println("float value: " + value2);
    return registers;
  }

  private static void writeHoldingRegister(ModbusBase ctx, int addr, int value) {
    int rc = ctx.writeRegister(addr, value);
    if (rc < 0) {
      System.err.println("Unable to write holding register at " + addr
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return;
    }
  }

  private static void writeHoldingRegisters(ModbusBase ctx, int addr, int nb, UInt16Array src) {
    int rc = ctx.writeRegisters(addr, nb, src.cast());
    if (rc < 0) {
      System.err.println("Unable to write " + nb + " holding registers at " + addr
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return;
    }
  }

  private static UInt16Array readInputRegisters(ModbusBase ctx, int addr, int wordCount) {
    // new class instances are created in cpp => "delete" must be called
    UInt16Array registers = new UInt16Array(wordCount); 
    int rc = ctx.readInputRegisters(addr, wordCount, registers.cast());
    if (rc < 0) {
      System.err.println("Unable to read " + wordCount + " input registers at " + addr
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return null;
    }
    for (int i=0; i < rc; i++) {
      System.out.println("Input register " + (addr + i) 
        + ": 0x" + String.format("%04X", registers.getitem(i)));
    }
    float value = ctx.getFloat(registers.cast());
    System.err.println("float value: " + value);
    return registers;
  }

  
  private static UInt16Array writeAndReadHoldingRegisters(ModbusBase ctx, int writeAddr, int writeNb, UInt16Array src,
      int readAddr, int readNb) {
    UInt16Array registers = new UInt16Array(readNb);
    int rc = ctx.writeAndReadRegisters(writeAddr, writeNb, src.cast(), readAddr, readNb, registers.cast());
    if (rc < 0) {
      System.err.println("Unable to write " + writeNb + " holding registers to " + writeAddr 
        + " and read " + readNb + " holding registers at " + readAddr
        + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return null;
    }
    for (int i=0; i < rc; i++) {
      System.out.println("Holding register " + (readAddr + i) 
        + ": 0x" + String.format("%04X", registers.getitem(i)));
    }
    float value = ctx.getFloat(registers.cast());
    System.err.println("float value: " + value);
    return registers;
  }
}
