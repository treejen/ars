package com.hktv.ars.rabbitmq.listener;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AddressInfoListener {

    private static final ObjectMapper mapper = new ObjectMapper();


//    @RabbitListener(queues = "${rabbitmq.ars_queue.address-info.queue}")
    public void addressInfo(Message messageData) {
        try {

            log.info("RabbitMQ Queue product-info-mix_queue, request body : [{}]", messageData);

        } catch (Exception e) {

        }

    }
}
