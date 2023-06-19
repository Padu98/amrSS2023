import rclpy
from rclpy.node import Node
from std_msgs.msg import Float32
import paho.mqtt.client as mqtt

class RobotArmController(Node):
    def __init__(self):
        super().__init__('robot_arm_controller')
        self.publisher_ = self.create_publisher(Float32, 'robot_arm_position', 10)
        
        # MQTT Broker Configuration
        self.broker_address = 'mqtt_broker_ip_address'
        self.broker_port = 1883
        self.client = mqtt.Client()
        self.client.username_pw_set('amr', 'amr')  # Set MQTT credentials
        self.client.on_connect = self.on_mqtt_connect
        self.client.on_message = self.on_mqtt_message
        self.client.connect(self.broker_address, self.broker_port, 60)
        self.client.loop_start()
        
    def on_mqtt_connect(self, client, userdata, flags, rc):
        self.client.subscribe('robot_arm_movement')

    def on_mqtt_message(self, client, userdata, msg):
        position = float(msg.payload)
        self.move_robot_arm(position)
        
    def move_robot_arm(self, position):
        msg = Float32()
        msg.data = position
        self.publisher_.publish(msg)
        self.get_logger().info('Robot arm position: %.2f' % position)
        
def main(args=None):
    rclpy.init(args=args)
    robot_arm_controller = RobotArmController()
    
    try:
        while rclpy.ok():
            rclpy.spin_once(robot_arm_controller, timeout_sec=0.1)
    except KeyboardInterrupt:
        pass


    robot_arm_controller.destroy_node()
    rclpy.shutdown()

if __name__ == '__main__':
    main()
