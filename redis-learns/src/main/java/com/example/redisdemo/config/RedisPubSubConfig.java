package com.example.redisdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.concurrent.CountDownLatch;

/**
 * Redis Pub/Sub wiring.
 *
 * <p>Messages published to {@code demo:channel} are received by {@link DemoSubscriber}
 * and logged. Pub/Sub is fire-and-forget: if no subscriber is listening, the
 * message is lost (unlike Streams).
 */
@Configuration
public class RedisPubSubConfig {

    public static final String CHANNEL = "demo:channel";

    @Bean
    public MessageListenerAdapter listenerAdapter(DemoSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory,
                                                  MessageListenerAdapter adapter) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(factory);
        c.addMessageListener(adapter, new ChannelTopic(CHANNEL));
        return c;
    }

    public static class DemoSubscriber {
        private final CountDownLatch latch = new CountDownLatch(1);
        private String lastMessage;

        public void onMessage(String message, String channel) {
            this.lastMessage = message;
            latch.countDown();
            System.out.println("[PUBSUB] channel=" + channel + " msg=" + message);
        }

        public CountDownLatch getLatch() { return latch; }
        public String getLastMessage() { return lastMessage; }
    }

    @Bean
    public DemoSubscriber demoSubscriber() {
        return new DemoSubscriber();
    }
}
