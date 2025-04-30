import os
import cv2
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import load_model
from com.chaquo.python import Python

def preprocess_eye(image_path):

#     model_path = '/data/user/0/com.google.mediapipe.examples.facelandmarker/files/rl_trained_model.h5'  # Adjust with your actual app's package name
#     model = load_model(model_path)

    image = cv2.imread(image_path)
    if image is None:
        print("Error: Could not load image.")
        return None

    print(f"Processing {image_path}...")

    # Convert to grayscale for better detection
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Load OpenCV's pre-trained face and eye detectors
    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
    eye_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_eye.xml')

    # Detect faces in the image
    faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))

    for (x, y, w, h) in faces:
        face_roi = gray[y:y+h, x:x+w]
        eyes = eye_cascade.detectMultiScale(face_roi, scaleFactor=1.1, minNeighbors=10, minSize=(20, 20))

        if len(eyes) > 0:
            ex, ey, ew, eh = eyes[0]  # Assume the first detected eye is the left eye
            left_eye_crop = image[y+ey:y+ey+eh, x+ex:x+ex+ew]

            if left_eye_crop.size > 0:
                left_eye_resized = cv2.resize(left_eye_crop, (64, 64))
                left_eye_normalized = left_eye_resized / 255.0  # Normalize

                cv2.imwrite("left_eye.jpg", left_eye_resized)
                print("Left eye saved as 'left_eye.jpg'")

                left_eye_input = np.expand_dims(left_eye_normalized, axis=0)

                #Save the array to a txt file
                #Open txt file in write mode
                context = Python.getPlatform().getApplication()
                directory = context.getFilesDir().toString()
                file_path = os.path.join(directory, "faceimg.txt")
                f = open(file_path, "w")
                #Write each piece of data to each line
                for row in left_eye_input:
                    for col in row:
                        for pixel in col:
                            f.write(' '.join(map(str,pixel)) + '\n')

                #Return side of array
                return left_eye_input.size

#                 # Reshape for model input
#                 left_eye_input = np.expand_dims(left_eye_norm, axis=0)
#
#                 # Make prediction
#                 prediction = model.predict(left_eye_input)[0][0]
#                 label = "Distracted" if prediction > 0.5 else "Focused"
#                 confidence = prediction if prediction > 0.5 else 1 - prediction
#
#                 return(f"Prediction: {label}, Confidence: {confidence:.2f}, Raw Prediction: {prediction:.3f}")

    print("No left eye detected.")
    return None