package de.pbma.moa.amr;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class MQTTLogic {
    private static final String TAG = MQTTLogic.class.getCanonicalName();
    private static final String BROKER = "";
    private static final String USERNAME = "amr";
    private static final String PASSWORD = "amr";
    private final ConcurrentSkipListSet<String> subscriptions = new ConcurrentSkipListSet<>();
    private final ConcurrentHashMap<Long, MQTTLogic.Pair<String, String>> pendingMessages = new ConcurrentHashMap<>();
    final private CopyOnWriteArrayList<MQTTListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong pendingMessageId = new AtomicLong(0);
    private ExecutorService mqttExecutor;
    private ExecutorService callbackExecutor;
    private volatile MqttClient mqttClient;
    private final AtomicBoolean blockConnectionRequests = new AtomicBoolean(false);
    private final AtomicBoolean mqttClientConnected = new AtomicBoolean(false);

    private final MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {}

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {}

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {}
    };

    public boolean registerMQTTListener(MQTTListener mqttListener) {
        return listeners.addIfAbsent(mqttListener);
    }

    public boolean deRegisterMQTTListener(MQTTListener mqttListener) {
        return listeners.remove(mqttListener);
    }

    public void connect() {
        if (blockConnectionRequests.get() || mqttClientConnected.get()) {
            Log.v(TAG, "Pending Request");
            return;
        }
        if (mqttExecutor != null) {
            mqttExecutor.shutdown();
        }
        mqttExecutor = Executors.newSingleThreadExecutor();
        if (callbackExecutor != null) {
            callbackExecutor.shutdown();
        }
        callbackExecutor = Executors.newSingleThreadExecutor();
        blockConnectionRequests.set(true);
        mqttExecutor.execute(() -> {
            try {
                mqttClient = new MqttClient(BROKER, MqttClient.generateClientId(), null);
                mqttClient.setCallback(mqttCallback);
                MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                mqttConnectOptions.setCleanSession(true);
                mqttConnectOptions.setPassword(PASSWORD.toCharArray());
                mqttConnectOptions.setUserName(USERNAME);

                mqttClient.connect(mqttConnectOptions);
                blockConnectionRequests.set(false);
                mqttClientConnected.set(true);
                for (MQTTListener listener : listeners) {
                    listener.onConnected();
                }
                Log.v(TAG, "mqttclient is connected!");
            } catch (MqttException e) {
                blockConnectionRequests.set(false);
                mqttClientConnected.set(false);
                Log.e(TAG, "connection failure", e);
            }
        });
    }

    public void subscribe(String topicFilter) {
        if (!mqttClientConnected.get() && !blockConnectionRequests.get()) {
            throw new RuntimeException("connect not yet called");
        }
        subscriptions.add(topicFilter);
        mqttExecutor.execute(() -> {
            try {
                Log.v(TAG, "subscribe: " + topicFilter + mqttClientConnected.get());
                MqttClient client = mqttClient;
                if (client != null && mqttClientConnected.get()) {
                    client.subscribe(topicFilter);
                }
            } catch (MqttException e) {
                Log.e(TAG, String.format("  subscribe failed: topic=%s, cause=%s", topicFilter, e.getMessage()));
                subscriptions.remove(topicFilter);
                Log.v(TAG, e.getMessage());
            }
        });
    }

    public void unsubscribe(String topicFilter) {
        if (!mqttClientConnected.get() && !mqttClientConnected.get()) {
            throw new RuntimeException("connect not yet called");
        }
        mqttExecutor.execute(() -> {
            try {
                boolean contained = subscriptions.remove(topicFilter);
                if (contained) {
                    MqttClient client = mqttClient;
                    if (client != null && mqttClientConnected.get()) {
                        mqttClient.unsubscribe(topicFilter);
                    }
                }
            } catch (MqttException e) {
                Log.v(TAG, e.getMessage());
            }
        });
    }

    public void disconnectMQTTClient() {
        Log.v(TAG, "disconnect");
        final MqttClient client = mqttClient;
        mqttClient = null;

        if (client != null && mqttClientConnected.get()) {
            for (String subscription : subscriptions) {  //todo pruefen on das so geht
                unsubscribe(subscription);
            }
        }

        if (callbackExecutor != null) {
            List<Runnable> pending = callbackExecutor.shutdownNow();
            if (!pending.isEmpty()) {
                Log.w(TAG, String.format("disconnect: %d incoming lost", pending.size()));
            }
            callbackExecutor = null;
        }
        if (client != null && mqttClientConnected.get()) {
            mqttClientConnected.set(false);
            List<Runnable> pending = mqttExecutor.shutdownNow();
            mqttExecutor = null;
            if (!pending.isEmpty()) {
                Log.w(TAG, String.format("disconnect: %d outgoing lost", pending.size()));
            }
            new Thread() {
                @Override
                public void run() {
                    try {
                        client.disconnect();
                        Log.v(TAG, "disconnedted");
                    } catch (MqttException ignored) {
                    }
                }
            }.start();
        }
        List<MQTTLogic.Pair<String, String>> pending = getClearPendingMessages();
        if (!pending.isEmpty()) {
            Log.v(TAG, "messages pending: " + pending);
        }
    }

    public List<MQTTLogic.Pair<String, String>> getClearPendingMessages() {
        List<MQTTLogic.Pair<String, String>> pm = new ArrayList<>(pendingMessages.size());
        pm.addAll(pendingMessages.values());
        pendingMessages.clear();
        return pm;
    }

    public void send(final String topic, final String msg) {
        if (!mqttClientConnected.get() && !mqttClientConnected.get()) {
            throw new RuntimeException("connect not yet called");
        }
        final long id = pendingMessageId.incrementAndGet();
        pendingMessages.put(id, MQTTLogic.Pair.createPair(topic, msg));
        mqttExecutor.execute(() -> {
            try {
                pendingMessages.remove(id);
                MqttMessage message = new MqttMessage();
                message.setPayload(msg.getBytes());
                message.setQos(1);
                MqttClient client = mqttClient;
                if (client != null) {
                    pendingMessages.remove(id);
                    client.publish(topic, message);
                }
            } catch (MqttException e) {
                Log.v(TAG, e.getMessage());
            }
        });
    }

    public static class Pair<S, T> {
        private final S s;
        private final T t;

        public static <S, T> Pair<S, T> createPair(S s, T t) {
            return new Pair<>(s, t);
        }

        public Pair(S s, T t) {
            this.s = s;
            this.t = t;
        }
    }
}
