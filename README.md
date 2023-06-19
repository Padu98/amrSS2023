# amrSS2023


Erstelle einen neuen Ordner für deinen ROS 2-Workspace + Initialisierung:
```
mkdir -p ~/ros2_workspace/src
cd ~/ros2_workspace
colcon init
. /opt/ros/foxy/setup.bash   //hier eventuell anpassen
cd ~/ros2_workspace/src
```

Ros2 Paket erstellen (hier my_package):
```
ros2 pkg create --build-type ament_python my_package
cd ~/ros2_workspace/src/my_package
```

setup.py mit eigenem Skript ersetzen oder einfach den code ersetzen dann diesesn/nächsten Schritt jumpen
danach in CMakeLists.txt

```
install(PROGRAMS
  scripts/your_script.py
  DESTINATION lib/${PROJECT_NAME}
)
```
danach:
```
cd ~/ros2_workspace
colcon build
. install/setup.bash
```
zum Starten des Skriptes:

```
ros2 run my_package your_script.py
```
