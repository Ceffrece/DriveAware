import socket
import struct
import cv2

def serverFunction(imgpath):
    # === Config ===
    SERVER_IP = '10.111.118.237'  # Change to your server's IP if remote
    PORT = 5001
    IMAGE_PATH = imgpath # Path to a test image

    # === Read and encode image ===
    img = cv2.imread(IMAGE_PATH)
    _, img_encoded = cv2.imencode('.jpg', img)
    img_bytes = img_encoded.tobytes()
    img_len = len(img_bytes)

    # === Connect to server ===
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((SERVER_IP, PORT))

        # Send 4-byte image length
        s.sendall(struct.pack('!I', img_len))

        # Send image data
        s.sendall(img_bytes)

        # Receive and print response
        response = s.recv(1024).decode('utf-8')
        return response