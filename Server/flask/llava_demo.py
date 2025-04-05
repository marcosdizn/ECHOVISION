import base64
import io
from PIL import Image
import torch
from transformers import BitsAndBytesConfig, pipeline

# Constants
_MODEL_ID = "llava-hf/llava-1.5-7b-hf"
_PROMPT = "USER: <image>\nWhat is this place?\nASSISTANT:"
_MAX_NEW_TOKENS = 200

def _create_pipeline(model_id):
    """Create a pipeline for image-to-text conversion."""
    quantization_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_compute_dtype=torch.float16
    )
    return pipeline("image-to-text", model=model_id, model_kwargs={"quantization_config": quantization_config})

def _generate_text(pipe, image, prompt, max_new_tokens):
    """Generate text using the provided pipeline and image."""
    outputs = pipe(image, prompt=prompt, generate_kwargs={"max_new_tokens": max_new_tokens})
    return outputs[0]["generated_text"]

def process_image(encoded_string):
    """Process a base64 encoded image and generate text."""
    # Decode the base64 string into bytes
    img_data = base64.b64decode(encoded_string)

    # Convert the bytes to a PIL Image object
    image = Image.open(io.BytesIO(img_data))

    # Create pipeline
    pipe = _create_pipeline(_MODEL_ID)

    # Generate and return text
    return _generate_text(pipe, image, _PROMPT, _MAX_NEW_TOKENS)