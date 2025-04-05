# EchoVision 👁️‍🗨️

## Introduction  
A low-cost, AI-powered wearable assistant designed to help visually impaired individuals understand their surroundings through real-time image-to-audio descriptions.

---

## 🎯 Overview

**EchoVision** is a smart wearable system designed to empower individuals with severe visual impairments by helping them interpret their environment through auditory feedback. The system captures images using a wearable camera embedded in 3D-printed smart glasses and converts them into spoken descriptions using a combination of OCR and multimodal AI technologies.

By combining embedded hardware, mobile development, and advanced AI, EchoVision aims to offer an accessible alternative to expensive assistive technologies already in the market.

![Architecture](https://github.com/marcosdizn/ECHOVISION/blob/main/Poster.png?raw=true)

## 🛠️ Tech Stack

- **Embedded**: ESP32-WROVER CAM (programmed with Arduino IDE)
- **Mobile**: Android (Java)
- **Backend**: Python, Flask server on Ubuntu
- **AI/ML**:
  - Google Cloud Vision OCR (text recognition)
  - LLaVA (Large Language and Vision Assistant) for scene description
  - Amazon Polly for text-to-speech conversion

## 📱 App Features

- 🔗 **Bluetooth Integration**: Connects with ESP32 for image capture via custom smart glasses.
- 📷 **Dual Image Input**: Accepts images from mobile gallery/camera or ESP32 glasses.
- 🧠 **Intelligent Scene Description**:
  - **Text Mode**: Extracts and reads aloud any text in the image using OCR.
  - **Scene Mode**: Describes the visual environment using the LLaVA model.
- 🎤 **Voice Control**: Navigate the app via speech commands like “texto”, “entorno”, etc.
- 🔊 **Audio Output**: Natural speech synthesis using Amazon Polly.

## 🧱 Architecture

```
[ESP32 Glasses] ⇄ (Bluetooth) ⇄ [Android App] ⇄ (HTTP) ⇄ [Flask Server]
                                                                 ⇓
                                         [OCR or Scene Analysis] → [Amazon Polly] → Audio
```

![Architecture](https://github.com/marcosdizn/ECHOVISION/blob/main/Architecture.png?raw=true)


- Lightweight architecture ensures the mobile app remains efficient, with heavy processing done server-side.

## 👓 Hardware

- **ESP32-WROVER CAM**:
  - Low-cost microcontroller with 2MP camera (OV2640 sensor).
  - Captures black-and-white images to optimize bandwidth and speed.
- **Smart Glasses**:
  - Custom-designed 3D-printed frame.
  - Embedded ESP32, push button, and cable management with thermal shrink tubing.

## 🔍 Use Cases

- Reading signs, menus, or documents independently.
- Understanding surroundings like street scenes, objects, or people.
- Performing daily tasks without relying on others for visual assistance.

## 📊 Results

- High accuracy for OCR in readable images.
- LLaVA provides detailed scene interpretation even from low-resolution black-and-white images.
- Effective voice interaction for hands-free use.

## 🚀 Future Work

- Improve camera quality and mount comfort.
- Add internal battery and audio jack integration.
- Expand support to iOS and wearable platforms (e.g., WearOS).
- Real-time obstacle detection.
- Multi-language text recognition and synthesis.
- Reduce latency and enhance UI/UX.

## 🤝 Contributing  
We welcome contributions to enhance functionality and optimize performance. To contribute:  
1. Fork this repository.  
2. Create a new branch for your feature.  
3. Submit a pull request with a detailed description of changes.  
