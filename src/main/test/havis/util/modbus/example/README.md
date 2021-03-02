create two devices which are connected internally (slave emulation: /dev/ttyS10, master: /dev/ttyS11)

    socat -d -d PTY,link=/dev/ttyS10,group=dialout,mode=660 PTY,link=/dev/ttyS11,group=dialout,mode=660

    OUTPUT_DIR=../../../../target
    java -Djava.library.path=$OUTPUT_DIR -cp $OUTPUT_DIR/havis.util.modbus.jar havis.util.modbus.example.Slave
    java -Djava.library.path=$OUTPUT_DIR -cp $OUTPUT_DIR/havis.util.modbus.jar havis.util.modbus.example.Master
