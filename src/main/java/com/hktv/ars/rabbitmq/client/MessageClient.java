package com.hktv.ars.rabbitmq.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageClient {

    private final RabbitTemplate rabbitTemplate;

    public void sendData(String exchange, String routingKey, String data) {
        log.info(String.format("ARS mq client,exchanger: %s, routingKey: %s, data: %s", exchange, routingKey, data));

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("text/plain");
        Message message = new Message(data.getBytes(), messageProperties);
        rabbitTemplate.send(exchange, routingKey, message);
    }

}
