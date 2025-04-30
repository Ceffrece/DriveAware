import os
import cv2
import numpy as np
import mediapipe as mp

def preprocess_eye(image_path, output_path="processed_eye.png"):
    image = cv2.imread(image_path)
    if image is None:
        print("Error: Could not load image.")
        return None

    mp_face_mesh = mp.solutions.face_mesh
    face_mesh = mp_face_mesh.FaceMesh(
        static_image_mode=True,
        max_num_faces=1,
        min_detection_confidence=0.5
    )

    LEFT_EYE = [33, 160, 158, 133, 153, 144, 362, 385, 386, 387, 388]
    rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    results = face_mesh.process(rgb_image)

    if results.multi_face_landmarks:
        for face_landmarks in results.multi_face_landmarks:
            h, w, _ = image.shape
            left_eye_coords = [(int(face_landmarks.landmark[i].x * w), int(face_landmarks.landmark[i].y * h)) for i in LEFT_EYE]
            left_eye_rect = cv2.boundingRect(np.array(left_eye_coords))
            left_eye_crop = image[left_eye_rect[1]:left_eye_rect[1] + left_eye_rect[3], left_eye_rect[0]:left_eye_rect[0] + left_eye_rect[2]]

            if left_eye_crop.size > 0:
                left_eye_resized = cv2.resize(left_eye_crop, (64, 64))
                left_eye_normalized = (left_eye_resized / 255.0 * 255).astype(np.uint8)

                #Save the array to a txt file
                #Open txt file in write mode
                context = Python.getPlatform().getApplication()
                directory = context.getFilesDir().toString()
                file_path = os.path.join(directory, "faceimg.txt")
                f = open(file_path, "w")
                #Write each piece of data to each line
                for row in left_eye_normalized:
                    for col in row:
                        for pixel in col:
                            f.write(' '.join(map(str,pixel)) + '\n')
                            #Return side of array
                            return left_eye_input.size

    print("No left eye detected.")
    return None