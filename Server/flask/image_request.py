import base64
import json
import requests
from json_data_model import ImageDataRequest

# Define constants
IMAGE_PATH = 'images/fernando-alonso-aston-martin-r-2.jpg'
API_URL = "http://localhost:4000/processImage"

def encode_image(image_path):
    """Read image, encode it in Base64 and return the encoded string."""
    with open(image_path, 'rb') as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')

def send_post_request(url, data):
    """Send a POST request to the specified URL with the provided data."""
    response = requests.post(url, data=json.dumps(data), headers={'Content-Type': 'application/json'})
    return response.text

# Encode image
encoded_image = encode_image(IMAGE_PATH)

# Create image data instance
image_data = ImageDataRequest(encoded_image)

# Send POST request and print the response
response = send_post_request(API_URL, image_data.__dict__)
print(response)