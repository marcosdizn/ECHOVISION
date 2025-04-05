#include "BluetoothSerial.h"
#include "esp_camera.h"

// Pin definition for camera
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
int cameraImageBrightness = 0;         // Image brightness (-2 to +2)

const int brightLED = 4;       // onboard Illumination/flash LED pin (4)
const int ledFreq = 5000;      // PWM settings
const int ledChannel = 15;     // camera uses timer1
const int ledRresolution = 8;  // resolution (8 = from 0 to 255)
const int pushButton = 33;     //FMD 16;

void setup() {
  Serial.begin(460800); // REVISAR
  
  
  SerialBT.begin(device_name);  // Bluetooth device name
  Serial.printf("The device with name \"%s\" is started.\nNow you can pair it with Bluetooth!\n", device_name.c_str());
  #ifdef USE_PIN
    //SerialBT.setPin(pin);
    Serial.println("Using PIN");
  #endif

  pinMode(pushButton, INPUT_PULLUP);

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
  config.xclk_freq_hz = 10000000;     // XCLK 20MHz or 10MHz for OV2640 double FPS (Experimental)
  config.pixel_format = PIXFORMAT_YUV422; // Options =  YUV422, GRAYSCALE, RGB565, JPEG, RGB888
  config.frame_size = FRAMESIZE_SXGA;  // Image sizes: 160x120 (QQVGA), 128x160 (QQVGA2), 176x144 (QCIF), 240x176 (HQVGA), 320x240 (QVGA),
                                      //              400x296 (CIF), 640x480 (VGA, default), 800x600 (SVGA), 1024x768 (XGA), 1280x1024 (SXGA),
                                      //              1600x1200 (UXGA)
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 0;  // 0-63 lower number means higher quality (15 by default)
  config.fb_count = 1;      // if more than one, i2s runs in continuous mode. Use only with JPEG
  config.grab_mode = CAMERA_GRAB_LATEST;

  // check the esp32cam board has a psram chip installed (extra memory used for storing captured images)
  // if PSRAM IC present, init with UXGA resolution and higher JPEG quality
  //                      for larger pre-allocated frame buffer.

  if (psramFound()) {
    Serial.println("PSRAM found");
    config.fb_location = CAMERA_FB_IN_PSRAM;  //FMD en PSRAM me da problemas (sólo QQVGA para GRAYSCALE y VGA para JPEG);
  } else {
    // Limit the frame size when PSRAM is not available
    Serial.println("PSRAM not found");
    config.fb_location = CAMERA_FB_IN_DRAM;
    config.frame_size = FRAMESIZE_CIF;
    config.jpeg_quality = 12;
    config.fb_count = 1;
  }
  
  //FMD config.fb_location = CAMERA_FB_IN_PSRAM;
  //config.grab_mode = CAMERA_GRAB_LATEST;   //Only for JPEG 
  

  // Camera init
  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.print(F("Camera init failed with error "));
    Serial.println(err);
    delay(1000);
    ESP.restart();
    //return;
  }

  // Sensor configuration
  bool sensorOk = cameraImageSettings();
  if (sensorOk) {
    Serial.println(F("Sensor configurado"));
  } else {
    Serial.println(F("ERROR: no se pudo configurar el sensor"));
  }
  //}
}

int currentState = 0;
int currRead = 0;
int lastRead = 0;

unsigned long lastDebounceTime = 0;
unsigned long debounceDelay = 50;

bool takePhoto = false;

void loop() {
  // Check for commands from Bluetooth
  //int read = analogRead(33);
  currRead = digitalRead(pushButton);
  takePhoto = false;
  if (currRead != lastRead) {
    lastDebounceTime = millis();
  }
  if ((millis() - lastDebounceTime) > debounceDelay) {
    if (currRead != currentState) {
      currentState = currRead;

      if (currentState == 0) {
        takePhoto = true;
      }
    }
  }

  //SerialBT.available != 0
  if (takePhoto) {
    /*
    if(SerialBT.available){
    
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
    }else{
      capturePhoto();
    */
    capturePhoto();
  }

  lastRead = currRead;
}

bool cameraImageSettings() {

  if (serialDebug) Serial.println(F("Applying camera settings"));

  sensor_t *s = esp_camera_sensor_get();
  if (s == NULL) {
    if (serialDebug) Serial.println(F("Error: problem reading camera sensor settings"));
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

  fb = esp_camera_fb_get();  // capture image frame from camera
  if(!fb) {
    Serial.println(F("Camera capture failed"));
    delay(1000);
    //ESP.restart();
    return;
  }
  esp_camera_fb_return(fb);
  fb = NULL;
  delay(100); // REVISAR*/
  
  fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println(F("Camera capture failed"));
    delay(1000);
    //ESP.restart();
    return;
  }  
  
  Serial.print(F("Longitud del buffer: "));
  Serial.println(fb->len);

  uint8_t *fbBuf = fb->buf;
  size_t fbLen = fb->len;
  //Serial.print("Orig.size: ");
  //Serial.println(fbLen);

  // Send original binary photo over Serial
  //Serial.write(fbBuf, fbLen);
  //Serial.flush();
  // Send original binary photo over Bluetooth
  //SerialBT.write(fbBuf, fbLen);
  //SerialBT.flush();
  //Serial.println("Enviado BT.")
  
  // Para comprimir NUNCA USAR si PIXFORMAT_JPEG (la imagen ya está comprimida)

  uint8_t *fb_jpeg = NULL;
  size_t jpeg_size = 0;
  bool converted = frame2jpg(fb, 63, &fb_jpeg, &jpeg_size);
  if(!converted){
      Serial.print(F("JPEG compression failed"));
      esp_camera_fb_return(fb);
      return;
  }
  Serial.print(F("JPEG.size: "));
  Serial.println(jpeg_size); 

  //Código para ver el buffer por el puerto serie
  /*
  for(size_t i = 0; i < jpeg_size;i++){
    Serial.print(fb_jpeg[i], HEX);
    Serial.print(" ");
  }
  Serial.println();
  */

  // Send JPEG binary photo over Serial
  //Serial.write(fb_jpeg, jpeg_size);
  //Serial.flush();

  // Send JPEG binary photo over Bluetooth
  //SerialBT.write((uint8_t *)fb->buf, fb->len);
  //SerialBT.flush();
  unsigned long time1 = millis();
  SerialBT.write(fb_jpeg, jpeg_size);
  SerialBT.flush();
  unsigned long time2 = millis() - time1;
  Serial.print(F("Enviado BT. Tiempo: "));
  Serial.print(time2);
  Serial.println(F(" ms"));

  esp_camera_fb_return(fb);
  fb = NULL;

  free(fb_jpeg);

  delay(1000);
}
