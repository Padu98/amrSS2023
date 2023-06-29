#!/usr/bin/env python3

from paho.mqtt import client as mqtt_client
import json

broker = '127.0.0.1'
port = 1883
topic = 'amr/data'
client_id = 'test'

def connect_mqtt() -> mqtt_client:
   def on_connect(client, userdata, flag, rc):
      if rc == 0:
        print('super geschafft!')
      else:
        print('verdammt!')
   client = mqtt_client.Client(client_id)
   client.on_connect = on_connect
   client.connect(broker, port)
   return client

def subscribe(client: mqtt_client):
   def on_message(client, userdate, msg):
     print(f'Nachricht: `{msg.payload.decode()}`')
     message = json.loads(msg.payload.decode())
     print(message)  
   client.subscribe(topic)
   client.on_message = on_message

def run():
  client = connect_mqtt()
  subscribe(client)
  client.loop_forever()

if __name__ == '__main__':
   run()
