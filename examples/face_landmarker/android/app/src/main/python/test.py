import os
from com.chaquo.python import Python

def storeFile(filename):
    context = Python.getPlatform().getApplication()
    directory = context.getFilesDir().toString()
    file_path = os.path.join(directory, filename)
    f = open(file_path, "w")
    f.write("Is this working?")
    return file_path
