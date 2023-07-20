#!/usr/bin/python3

from dynamixel_sdk import *                    # Uses Dynamixel SDK library
from paho.mqtt import client as mqtt_client
import json
import os
import time
import random

broker = '127.0.0.1'
port = 1883
topic = 'amr/data'
client_id = 'test'

def connect_mqtt() -> mqtt_client:
   def on_connect(client, userdata, flag, rc):
      if rc == 0:
        print('super geschafft!')
      else:
        print('verdammt!')
   client = mqtt_client.Client(client_id)
   client.on_connect = on_connect
   client.connect(broker, port)
   return client

def subscribe(client: mqtt_client):
   def on_message(client, userdate, msg):
     #print(f'Nachricht: `{msg.payload.decode()}`')
     message = json.loads(msg.payload.decode())
     #todo pruefen ob key vorhanden
     #print("tawda")
     #test = "vertical" in message
     #print(test)
     if "vertical" in message:
        if message['vertical'] < -140 :
            message['vertical'] = -140
        if message['vertical'] > -40 :
            message['vertical'] = -40
        print(message['vertical'])
        move(abs(1024+(int(message['vertical'] * 5.69))),2)  #5.69 = 1024/180 muss noch abs machen
        print(abs(1024+(int(message['vertical'] * 5.69))))
     if "horizontal" in message:
        print('message:  ' + str(message['horizontal']))
        if message['horizontal'] > 180 and message['horizontal'] < 270:
            message['horizontal'] = 180
        elif message['horizontal'] > 270:
            message['horizontal'] = 0
        
        move(abs((int(message['horizontal'] *7.60))),3)  #7.60 = 1024/140 muss noch abs machen
        #print(abs((int(message['horizontal'] * 5.69))))
   client.subscribe(topic)
   client.on_message = on_message

def run():
  client = connect_mqtt()
  subscribe(client)
  client.loop_forever()
  
  

        
def move(position,ID):
    # Write goal position
    dxl_comm_result, dxl_error = packetHandler.write2ByteTxRx(portHandler, ID, ADDR_MX_GOAL_POSITION, position)
    if dxl_comm_result != COMM_SUCCESS:
        print("%s" % packetHandler.getTxRxResult(dxl_comm_result))
    elif dxl_error != 0:
        print("%s" % packetHandler.getRxPacketError(dxl_error))
    while 1:
      #  test = random.randint(0,200)
        # Read present position
        dxl_present_position, dxl_comm_result, dxl_error = packetHandler.read2ByteTxRx(portHandler, ID, ADDR_MX_PRESENT_POSITION)
        if dxl_comm_result != COMM_SUCCESS:
            print("%s" % packetHandler.getTxRxResult(dxl_comm_result))
        elif dxl_error != 0:
            print("%s" % packetHandler.getRxPacketError(dxl_error))

        print("[ID:%03d] GoalPos:%03d  PresPos:%03d" % (ID, position, dxl_present_position))
        break

        if not abs(position - dxl_present_position) > DXL_MOVING_STATUS_THRESHOLD:
            break
        



# Control table address
ADDR_MX_TORQUE_ENABLE      = 24 #64               # Control table address is different in Dynamixel model
ADDR_MX_GOAL_POSITION      = 30 #116
ADDR_MX_PRESENT_POSITION   = 36 #132

# Protocol version
PROTOCOL_VERSION            = 1.0               # See which protocol version is used in the Dynamixel

# Default setting
DXL_ID                      = 3                 # Dynamixel ID : 1
BAUDRATE                    = 57600             # Dynamixel default baudrate : 57600
DEVICENAME                  = '/dev/ttyUSB0'    # Check which port is being used on your controller
                                                # ex) Windows: "COM1"   Linux: "/dev/ttyUSB0" Mac: "/dev/tty.usbserial-*"

TORQUE_ENABLE               = 1                 # Value for enabling the torque
TORQUE_DISABLE              = 0                 # Value for disabling the torque
DXL_MINIMUM_POSITION_VALUE  = 0           # Dynamixel will rotate between this value
DXL_MAXIMUM_POSITION_VALUE  = 0            # and this value (note that the Dynamixel would not move when the position value is out of movable range. Check e-manual about the range of the Dynamixel you use.)
DXL_MOVING_STATUS_THRESHOLD =  50                # Dynamixel moving status threshold

index = 0
dxl_goal_position = [DXL_MINIMUM_POSITION_VALUE, DXL_MAXIMUM_POSITION_VALUE]         # Goal position


# Initialize PortHandler instance
# Set the port path
# Get methods and members of PortHandlerLinux or PortHandlerWindows
portHandler = PortHandler(DEVICENAME)

# Initialize PacketHandler instance
# Set the protocol version
# Get methods and members of Protocol1PacketHandler or Protocol2PacketHandler
packetHandler = PacketHandler(PROTOCOL_VERSION)


if __name__ == '__main__':
    if os.name == 'nt':
        import msvcrt
        def getch():
            return msvcrt.getch().decode()
    else:
        import sys, tty, termios
        fd = sys.stdin.fileno()
        old_settings = termios.tcgetattr(fd)
        def getch():
            try:
                tty.setraw(sys.stdin.fileno())
                ch = sys.stdin.read(1)
            finally:
                termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
            return ch
    
    # Open port
    if portHandler.openPort():
        print("Succeeded to open the port")
    else:
        print("Failed to open the port")
        print("Press any key to terminate...")
        getch()
        quit()

    # Set port baudrate
    if portHandler.setBaudRate(BAUDRATE):
        print("Succeeded to change the baudrate")
    else:
        print("Failed to change the baudrate")
        print("Press any key to terminate...")
        getch()
        quit()

    # Enable Dynamixel Torque
    dxl_comm_result, dxl_error = packetHandler.write1ByteTxRx(portHandler, DXL_ID, ADDR_MX_TORQUE_ENABLE, TORQUE_ENABLE)
    if dxl_comm_result != COMM_SUCCESS:
        print("%s" % packetHandler.getTxRxResult(dxl_comm_result))
    elif dxl_error != 0:
        print("%s" % packetHandler.getRxPacketError(dxl_error))
    else:
        print("Dynamixel has been successfully connected")
# Close port
    run()
    portHandler.closePort()
