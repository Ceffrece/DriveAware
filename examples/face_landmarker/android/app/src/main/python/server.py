import requests

def serverFunction(imagepath):
    # === Config ===
    SERVER_IP = '172.27.82.13'  # Your Flask server's IP
    PORT = 80                   # Flask server port (not 5001 anymore)
    IMAGE_PATH = imagepath  # Path to a test image

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