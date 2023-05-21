#!/usr/bin/python3

import socket
import cv2
import numpy as np

def receive_video(address, port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((address, port))

    chunk_size = 1024 
    frame_bytes = b'' 

    delimiter = b'###' 
    
    print('listening for data:')
    while True:
        try:
           chunk, _ = sock.recvfrom(chunk_size) 

           frame_bytes += chunk

           if delimiter in frame_bytes:
               frame_chunks = frame_bytes.split(delimiter)
               #print(frame_chunks[0])
               frame = cv2.imdecode(np.frombuffer(frame_chunks[0], dtype=np.uint8), cv2.IMREAD_COLOR)
               cv2.imshow('Received Frame', frame)
               print('hat geklappt')
               if cv2.waitKey(1) & 0xFF == ord('q'):
                  break

               frame_bytes = b'' #frame_chunks[1]
        except Exception as e:
           frame_bytes = b''
           print('failed to read block')
    sock.close()
    cv2.destroyAllWindows()

if __name__ == '__main__':
    server_address = '0.0.0.0' #ip sender 
    server_port = 5000
    receive_video(server_address, server_port)

