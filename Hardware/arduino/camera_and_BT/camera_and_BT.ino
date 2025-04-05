#include "BluetoothSerial.h"
#include "esp_camera.h"

// Pin definition for camera
//#define CAMERA_MODEL_AI_THINKER // Define the camera model used
#define CAMERA_MODEL_WROVER_KIT
#include "camera_pins.h"

String device_name = "EchoVision";
const char *pin = "1234";  // Change this to a secure PIN for pairing

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to enable it
#endif

#if !defined(CONFIG_BT_SPP_ENABLED)
#error Serial Bluetooth not available or not enabled. It is only available for the ESP32 chip.
#endif

BluetoothSerial SerialBT;

bool serialDebug = 1;

#define PIXFORMAT PIXFORMAT_GRAYSCALE  // image format, Options =  YUV422, GRAYSCALE, RGB565, JPEG, RGB888
int cameraImageBrightness = 0;         // Image brightness (-2 to +2)

const int brightLED = 4;       // onboard Illumination/flash LED pin (4)
const int ledFreq = 5000;      // PWM settings
const int ledChannel = 15;     // camera uses timer1
const int ledRresolution = 8;  // resolution (8 = from 0 to 255)

void setup() {
  Serial.begin(921600);
  SerialBT.begin(device_name);  // Bluetooth device name
  Serial.printf("The device with name \"%s\" is started.\nNow you can pair it with Bluetooth!\n", device_name.c_str());

#ifdef USE_PIN
  SerialBT.setPin(pin);
  Serial.println("Using PIN");
#endif

  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;       // XCLK 20MHz or 10MHz for OV2640 double FPS (Experimental)
  config.pixel_format = PIXFORMAT;      // Options =  YUV422, GRAYSCALE, RGB565, JPEG, RGB888
  config.frame_size = FRAMESIZE_CIF;  // Image sizes: 160x120 (QQVGA), 128x160 (QQVGA2), 176x144 (QCIF), 240x176 (HQVGA), 320x240 (QVGA),
                                        //              400x296 (CIF), 640x480 (VGA, default), 800x600 (SVGA), 1024x768 (XGA), 1280x1024 (SXGA),
                                        //              1600x1200 (UXGA)
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 0;  // 0-63 lower number means higher quality (15 by default)
  config.fb_count = 1;      // if more than one, i2s runs in continuous mode. Use only with JPEG
  config.grab_mode = CAMERA_GRAB_LATEST;

  // check the esp32cam board has a psram chip installed (extra memory used for storing captured images)
  // if PSRAM IC present, init with UXGA resolution and higher JPEG quality
  //                      for larger pre-allocated frame buffer.
  /*
  if (true){
    //config.pixel_format == PIXFORMAT_JPEG
    if (psramFound()) {
      Serial.println("PSRAM found");
      config.jpeg_quality = 10;
      config.fb_count = 1;
      config.grab_mode = CAMERA_GRAB_LATEST;
    } else {
      // Limit the frame size when PSRAM is not available
      Serial.println("PSRAM not found");
      config.frame_size = FRAMESIZE_SVGA;
      config.fb_location = CAMERA_FB_IN_DRAM;
    }
  } else {
    // Best option for face detection/recognition
    config.frame_size = FRAMESIZE_240X240;
  */
  // Camera init
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Camera init failed with error 0x%x\n", err);
    return;
  }

  // Sensor configuration
  bool sensorOk = cameraImageSettings();
  if (sensorOk) {
    Serial.println("Sensor configurado");
  } else {
    Serial.println("ERROR: no se pudo configurar el sensor");
  }
  //}
}

void loop() {
  // Check for commands from Bluetooth
  if (SerialBT.available()) {
    char command = SerialBT.read();
    switch (command) {
      case '1':
        capturePhoto();  // Capture photo command
        break;
      case '2':
        // Other commands to control camera functions
        break;
      default:
        // Handle unrecognized commands
        break;
    }
  }

  delay(20);  // Adjust delay as needed
}

bool cameraImageSettings() {

  if (serialDebug) Serial.println("Applying camera settings");

  sensor_t *s = esp_camera_sensor_get();
  if (s == NULL) {
    if (serialDebug) Serial.println("Error: problem reading camera sensor settings");
    return 0;
  }

  // enable auto adjust
  s->set_gain_ctrl(s, 1);                       // auto gain on
  s->set_exposure_ctrl(s, 1);                   // auto exposure on
  s->set_awb_gain(s, 1);                        // Auto White Balance enable (0 or 1)
  s->set_brightness(s, cameraImageBrightness);  // (-2 to 2) - set brightness

  return 1;
}  // cameraImageSettings

// Function to capture a photo
void capturePhoto() {
  camera_fb_t *fb = NULL;
  fb = esp_camera_fb_get();   // capture image frame from camera
  esp_camera_fb_return(fb);
  fb = NULL;
  fb = esp_camera_fb_get();
  
  if (!fb) {
    Serial.println("Camera capture failed");
    return;
  }

  // Send photo over Bluetooth
  Serial.print("Longitud del buffer: ");
  Serial.println(fb->len);

  //Código para ver el buffer por el puerto serie
  /*
  for(size_t i = 0; i < fb->len;i++){
    //char hex[2];
    //sprintf(hex, "%02X", fb->buf[i]);
    Serial.print(fb->buf[i]);
    Serial.print(" ");
  }
  Serial.println();
  */

  SerialBT.write((uint8_t *)fb->buf, fb->len);
  SerialBT.flush();
  esp_camera_fb_return(fb);
}
