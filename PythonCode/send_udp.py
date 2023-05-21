#!/usr/bin/python3
import socket
import cv2

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
        if index > 10:
            break
    return index if found else None


def send_video(index, address, port):
    capture = cv2.VideoCapture(index, cv2.CAP_V4L)  
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    chunk_size = 1024 

    delimiter = b'###' 

    while True:
        ret, frame = capture.read() 
        if ret:
            _, jpeg = cv2.imencode('.jpg', frame)  
            frame_bytes = jpeg.tobytes()  
            frame_bytes += delimiter

            for i in range(0, len(frame_bytes), chunk_size):
                chunk = frame_bytes[i:i+chunk_size]
                sock.sendto(chunk, (address, port))
            print('frame send')
        else:
             cv2.destroyAllWindows()
             capture.release()
             capture = cv2.VideoCapture(index, cv2.CAP_V4L)
             print('frame wurde nicht erfasst')
          
    sock.close()
    capture.release()

if __name__ == '__main__':
    index = find_camera_index()
    print('camera: ' + str(index))
    server_address = '192.168.1.126' 
    server_port = 5000
    send_video('/dev/video'+str(index), server_address, server_port)
