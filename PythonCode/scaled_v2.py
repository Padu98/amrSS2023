#!/usr/bin/python3
import socket
import cv2
from wand.image import Image

def find_camera_index():
    index = -2
    found = False
    while not found:
        capture = cv2.VideoCapture('/dev/video'+str(index), cv2.CAP_V4L)
        if capture.isOpened():
            found = True
            capture.release()
        else:
            print('index is closed' + str(index))
            index += 1
        if index > 20:
            break
    return index if found else None

def rescale_frame(frame, percent=75):
    width = int(frame.shape[1] * percent/ 100)
    height = int(frame.shape[0] * percent/ 100)
    dim = (width, height)
    return cv2.resize(frame, dim, interpolation =cv2.INTER_AREA)

def send_video(index, address, port):
    capture = cv2.VideoCapture(index, cv2.CAP_V4L)  
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    chunk_size = 1024 

    delimiter = b'###' 

    while True:
        ret, frame = capture.read() 
        if ret:
            frame = rescale_frame(frame, 55)
            _, jpeg = cv2.imencode('.jpg', frame)  
            frame_bytes = jpeg.tobytes()
            with Image(blob=frame_bytes) as img:
                #print('width =', img.width)
                #print('height =', img.height)
                img.virtual_pixel = 'transparent'
                img.distort('barrel', (0.1, 0.0, 0.0, 1.0)) #(0.2 0.0 0.0 1.0
                bytes_from_blob = bytearray(img.make_blob())
               
            #bytes_from_blob = bytearray(img.make_blob())
            #print(img)  
            frame_bytes = bytes_from_blob + delimiter
            #frame_bytes += delimiter

            for i in range(0, len(frame_bytes), chunk_size):
                chunk = frame_bytes[i:i+chunk_size]
                sock.sendto(chunk, (address, port))
            print(len(frame_bytes))
        else:
             #cv2.destroyAllWindows()
             capture.release()
             cv2.destroyAllWindows()
             capture = cv2.VideoCapture(index, cv2.CAP_V4L)
             #print('frame wurde nicht erfasst')
          
    sock.close()
    capture.release()

if __name__ == '__main__':
    index = find_camera_index()
   # index = 0
    print('camera: ' + str(index))
    server_address = '192.168.146.66' 
    server_port = 5000
    send_video('/dev/video'+str(index), server_address, server_port)
    #send_video('/dev/video'+str(1), server_address, server_port)
