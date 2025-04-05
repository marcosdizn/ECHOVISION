from PIL import Image
import requests 

# Imagen de Internet
image_url = "https://helpx.adobe.com/content/dam/help/en/photoshop/using/convert-color-image-black-white/jcr_content/main-pars/before_and_after/image-before/Landscape-Color.jpg"
image = Image.open(requests.get(image_url, stream=True).raw)

# Imagen en el PC
# image = Image.open('ruta_imagen','r') 

import torch
from transformers import BitsAndBytesConfig

quantization_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_compute_dtype=torch.float16
)

from transformers import pipeline
model_id = "llava-hf/llava-1.5-7b-hf"
pipe = pipeline("image-to-text", model=model_id, model_kwargs={"quantization_config": quantization_config})


max_new_tokens = 200
prompt = "USER: <image>\nWhat is this place?\nASSISTANT:"

outputs = pipe(image, prompt=prompt, generate_kwargs={"max_new_tokens": 200})


print(outputs[0]["generated_text"])