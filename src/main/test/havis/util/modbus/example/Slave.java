package havis.util.modbus.example;

import havis.util.modbus.ModbusMapping;
import havis.util.modbus.ModbusBase;
import havis.util.modbus.ModbusRtu;
import havis.util.modbus.ModbusTcpPi;
import havis.util.modbus.UInt8Array;
import havis.util.modbus.UInt16Array;
import havis.util.modbus.IntArray;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// RTU: socat -d -d PTY,link=/dev/ttyS10,group=dialout,mode=660 PTY,link=/dev/ttyS11,group=dialout,mode=660
public class Slave {

  public static void main(String argv[]) {
    System.loadLibrary("modbus");

    int mode = 1; // 0: RTU, 1: TCP PI
    
    // RTU
    String device = "/dev/ttyS10";
    int baudrate = 115200;
    char parity = 'N';
    int dataBits = 8;
    int stopBits = 2;
    int serialMode = ModbusRtu.MODBUS_RTU_RS485;

    // TCP PI
    String node = "0.0.0.0";
    String service = "1502";
    int maxConnectionCount = 2;

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
        System.err.println("Unable to create the modbus RTU context");
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
        System.err.println("Unable to create the modbus TCP PI context: "
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
        System.err.println("Unable to set slaveId " + slaveId + ": " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
        return;
      }
      // create mappings
      ModbusMapping map = createMappings(ctx, addr, wordCount);
      if (map == null) {
        return;
      }
      try {
        // open slave
        UInt8Array request = null;
        int serverSocket = -1;
        IntArray readFds = null;
        int readFdsCount = -1;
        switch (mode) {
        case 0: // RTU
          if (ctx.connect() < 0) {
            System.err.println("Unable to open slave: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
            return;
          } 
          request = new UInt8Array(ModbusRtu.MODBUS_RTU_MAX_ADU_LENGTH);
          break;
        case 1: // TCP PI
          ModbusTcpPi ctxTcpPi = (ModbusTcpPi) ctx;
          serverSocket = ctxTcpPi.tcpPiListen(maxConnectionCount);
          if (serverSocket < 0) {
            System.err.println("Unable to open slave: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
            return;
          }
          request = new UInt8Array(ModbusTcpPi.MODBUS_TCP_MAX_ADU_LENGTH);
          readFds = new IntArray(1 /* serverSocket */ + maxConnectionCount);
          readFdsCount = 0;
          break;
        }

        // test code for closing the sockets while waiting for a connection or request
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        Future<Object> future = threadPool.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Thread.sleep(5000);
                //ctxFinal.close();
                return null;
            }
        });

        try {
          int headerLength = ctx.getHeaderLength();
          for (;;) {
             // if TCP PI
             if (mode == 1) {
               if (readFdsCount == 0) {
                 boolean isIncomingConnection;
                 do {
                   System.out.println("Waiting for data...");
                   ModbusTcpPi ctxTcpPi = (ModbusTcpPi) ctx;
                   do {
                     readFdsCount = ctxTcpPi.selectRead(readFds.cast());
                     if (readFdsCount < 0) {
                       System.err.println("Waiting for data failed: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
                     }
                   } while (readFdsCount <= 0);
                   isIncomingConnection = readFds.getitem(0) == serverSocket;
                   if (isIncomingConnection) {
                     // accept the connection
                     int clientSocket = ctxTcpPi.tcpPiAccept(serverSocket);
                     if (clientSocket < 0) {
                       System.err.println("Unable to accept a connection: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
                     } else {
                       System.out.println("Connection established: " + clientSocket);
                     }
                   }
                 } while (isIncomingConnection);
               }
               System.out.println("Processing request from connection " + readFds.getitem(readFdsCount - 1));
               // set client socket
               ctx.setSocket(readFds.getitem(readFdsCount - 1));
               readFdsCount--;
             }
             // wait for request
             int requestLength;
             do {
               requestLength = ctx.receive(request.cast());
               // filtered requests return 0
             } while (requestLength == 0);
             // if error has occurred
             if (requestLength < 0) {
               // if "Connection reset by peer"
               if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
                 // if TCP PI
                 if (mode == 1) {
                   ModbusTcpPi ctxTcpPi = (ModbusTcpPi) ctx;
                   ctxTcpPi.close(ctx.getSocket());
                 }
               }
               System.err.println("Failed to receive message: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
               // continue with processing of next request
               continue;
             }
             short functionCode = request.getitem(headerLength);
             int maxAddr;
             if (functionCode == ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS ||
                 functionCode == ModbusBase.MODBUS_FC_READ_COILS ||
                 functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL ||
                 functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS) {
               maxAddr = addr + wordCount * 16 - 1;
             } else {
               maxAddr = addr + wordCount - 1;
             }
             int reqAddr = ctx.getInt16FromInt8(request.cast(), headerLength + 1);
             // if valid address
             if (reqAddr >= addr && reqAddr <= maxAddr) {
               // send response
               if (ctx.reply(request.cast(), requestLength, map) < 0) {
                 // if "Connection reset by peer"
                 if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
                   // if TCP PI
                   if (mode == 1) {
                     ModbusTcpPi ctxTcpPi = (ModbusTcpPi) ctx;
                     ctxTcpPi.close(ctx.getSocket());
                   }
                 }
                 System.err.println("Failed to send response: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
               }
             } else {
               // send exception response
               if (ctx.replyException(request.cast(), ModbusBase.MODBUS_EXCEPTION_ILLEGAL_DATA_ADDRESS) < 0) {
                 // if "Connection reset by peer"
                 if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
                   // if TCP PI
                   if (mode == 1) {
                     ModbusTcpPi ctxTcpPi = (ModbusTcpPi) ctx;
                     ctxTcpPi.close(ctx.getSocket());
                   }
                 }
                 System.err.println("Failed to send exception response: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
               }
             }
          }
        } finally {
          if (request != null) {
            // destroy request structure
            request.delete();
          }
          if (readFds != null) {
            // destroy structure for read fds
            readFds.delete();
          }

          // test code clean up
          try {
            future.get();
          } catch (Exception e) {
            e.printStackTrace();
          }
          threadPool.shutdown();

          // close slave
          ctx.close();
        }
      } finally {
        // destroy mapping structure
        ctx.mappingFree(map);
        // destroy class instance
        map.delete();
      }
    } finally {
      // destroy context
      ctx.free();
      // destroy class instance
      ctx.delete();
    }
  }


  static ModbusMapping createMappings(ModbusBase ctx, int addr, int wordCount) {
    ModbusMapping map = ctx.mappingNew(addr + wordCount * 16 /*nb_bits*/, 
      addr + wordCount * 16 /*nb_input_bits*/,
      addr + wordCount /*nb_registers*/, addr + wordCount /*nb_input_registers*/);
    if (map == null) {
      System.err.println("Failed to create mapping: " +  ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
      return null;
    }

    // initialize mapping for coils
    UInt8Array tabBitsBytes = new UInt8Array(wordCount);
    // float 49.996532
    tabBitsBytes.setitem(0, (short) 0x42);
    tabBitsBytes.setitem(1, (short) 0x47);
    tabBitsBytes.setitem(2, (short) 0xFC);
    tabBitsBytes.setitem(3, (short) 0x73);
    UInt8Array tabBits = UInt8Array.frompointer(map.getTabBits());
    for (int i = 0; i < 4; i++) {
      ctx.setBitsFromByte(tabBits.cast(), i * 8, tabBitsBytes.getitem(i));
      for (int j = 0; j < 8; j++) {
        int key = addr + i * 8 + j;
        short value = tabBits.getitem(i * 8 + j);
        tabBits.setitem(key, value);
        System.out.println("Coils " + key + ": 0x" + String.format("%02X", value));
      }
    }
    tabBits.delete();
    tabBitsBytes.delete();

    // initialize mapping for discrete inputs
    UInt8Array tabInputBitsBytes = new UInt8Array(wordCount);
    // float 50.246536
    tabInputBitsBytes.setitem(0, (short) 0x42);
    tabInputBitsBytes.setitem(1, (short) 0x48);
    tabInputBitsBytes.setitem(2, (short) 0xFC);
    tabInputBitsBytes.setitem(3, (short) 0x74);
    UInt8Array tabInputBits = UInt8Array.frompointer(map.getTabInputBits());
    for (int i = 0; i < 4; i++) {
      ctx.setBitsFromByte(tabInputBits.cast(), i * 8, tabInputBitsBytes.getitem(i));
      for (int j = 0; j < 8; j++) {
        int key = addr + i * 8 + j;
        short value = tabInputBits.getitem(i * 8 + j);
        tabInputBits.setitem(key, value);
        System.out.println("Discrete input " + key + ": 0x" + String.format("%02X", value));
      }
    }
    tabInputBits.delete();
    tabInputBitsBytes.delete();

    // initialize mapping for holding registers   
    UInt16Array tabRegistersBytes = new UInt16Array(wordCount);
    // float 49.996532
    tabRegistersBytes.setitem(0, 0x4247);
    tabRegistersBytes.setitem(1, 0xFC73);
    UInt16Array tabRegisters = UInt16Array.frompointer(map.getTabRegisters());
    for (int i=0; i < wordCount; i++) {
      int key = addr + i;
      int value = tabRegistersBytes.getitem(i);
      tabRegisters.setitem(key, value);
      System.out.println("Holding register " + key + ": 0x" + String.format("%04X", value));
    }
    tabRegisters.delete();
    tabRegistersBytes.delete();

    // initialize mapping for input registers   
    UInt16Array tabInputRegistersBytes = new UInt16Array(wordCount);
    // float 50.246536
    tabInputRegistersBytes.setitem(0, 0x4248);
    tabInputRegistersBytes.setitem(1, 0xFC74);
    UInt16Array tabInputRegisters = UInt16Array.frompointer(map.getTabInputRegisters());
    for (int i=0; i < wordCount; i++) {
      int key = addr + i;
      int value = tabInputRegistersBytes.getitem(i);
      tabInputRegisters.setitem(key, value);
      System.out.println("Input register " + key + ": 0x" + String.format("%04X", value));
    }
    tabInputRegisters.delete();
    tabInputRegistersBytes.delete();
    
    return map;
  }
}
