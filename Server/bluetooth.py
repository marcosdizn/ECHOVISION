import time
import os
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from PIL import Image
import requests 
import torch
from transformers import BitsAndBytesConfig, pipeline

# Ejecutar en la misma carpeta donde se reciben los archivos por Bluetooth

class ImageHandler(FileSystemEventHandler):
    def on_created(self, event):
        if event.is_directory:
            return
        filename, file_extension = os.path.splitext(event.src_path)
        if file_extension.lower() in ['.jpg', '.jpeg', '.png', '.gif']:
            print("Imagen detectada:", event.src_path)
            try:
                process_image(event.src_path)
            except Exception as e:
                print("Error al procesar la imagen:", e)

def process_image(image_path):
    img = Image.open(image_path)

    quantization_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_compute_dtype=torch.float16
    )

    model_id = "llava-hf/llava-1.5-7b-hf"
    pipe = pipeline("image-to-text", model=model_id, model_kwargs={"quantization_config": quantization_config})

    max_new_tokens = 200
    prompt = "USER: <image>\nWhat is this place?\nASSISTANT:"

    outputs = pipe(img, prompt=prompt, generate_kwargs={"max_new_tokens": 200})

    print(outputs[0]["generated_text"])

if __name__ == "__main__":
    current_dir = os.getcwd()
    event_handler = ImageHandler()
    observer = Observer()
    observer.schedule(event_handler, current_dir, recursive=False)
    observer.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()
