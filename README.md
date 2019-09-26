# very-realistic-police-car-simulator


This project isn't focused on the game part, but on the gamepad (microcontroller) part and Bluetooth communication between them. So while watching the demo look at playability(e.g. advanced collision detection 😚) with a grain of salt.

The most important files are:
1. [arduino_main.c](microcontroller/arduino_main.c) - 
2. [BtService.java](app/src/main/java/com/example/otto/agameagain/BtService.java)
3. [Gamepad.java](app/src/main/java/com/example/otto/agameagain/Gamepad.java)
4. [GamePadListener.java](app/src/main/java/com/example/otto/agameagain/GamePadListener.java)

Less important but still something is happening in them:
5. [SelectDeviceActivity.java](app/src/main/java/com/example/otto/agameagain/SelectDeviceActivity.java)
6. [GameActivity.java](app/src/main/java/com/example/otto/agameagain/GameActivity.java)

## Demo
[![VERY REALISTIC POLICE CAR SIMULATOR DEMO](https://img.youtube.com/vi/69nwqNPbf4E/0.jpg)](https://www.youtube.com/watch?v=69nwqNPbf4E)


Microcontroller used it's
  NUCLEO-F411RE - STM32 Nucleo-64 development board with STM32F411RE MCU
with 
 	KA-Nucleo-UniExp - expander (shield) compatible with Arduino / NUCLEO with Bluetooth 2.0+EDR, MEMS LIS35D and temperature sensor
