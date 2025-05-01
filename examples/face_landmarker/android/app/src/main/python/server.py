import socket
import struct
import cv2
import requests
import json

def serverFunction(imgpath):
    # === Config ===
    SERVER_IP = '172.27.82.13'  # Change to your server's IP if remote
    PORT = 80
    IMAGE_PATH = imgpath # Path to a test image

    # === Read and encode image ===
    img = cv2.imread(IMAGE_PATH)
    _, img_encoded = cv2.imencode('.jpg', img)
    img_bytes = img_encoded.tobytes()
    img_len = len(img_bytes)

    # === Send image ===
    url = f'http://{SERVER_IP}:{PORT}/predict'

    with open(IMAGE_PATH, 'rb') as img_file:
        files = {'image': img_file}
        response = requests.post(url, files=files)

    # === Print server response ===
    if response.ok:
        print("Server response:", response.json())
        return response.json().get("label")
    else:
        print("Error:", response.status_code, response.text)