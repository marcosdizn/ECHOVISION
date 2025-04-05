from flask import Flask, jsonify, request
from json_data_model import ImageDataRequest
from llava_demo import process_image

app = Flask(__name__)

@app.route('/processImage', methods=['POST'])
def process_image_route():
    # Parse the request data
    data = request.get_json()

    # Validate and deserialize the data
    image_data_request = ImageDataRequest(**data)

    # Pass the encoded image string to process_image
    result = process_image(image_data_request.image)

    # Return the result
    return jsonify({"message": "Image processed successfully", "result": result})

if __name__ == "__main__":
    app.run(debug=True, port=4000)