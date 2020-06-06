package com.ds3161.rabbit.test;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Receiver {

	AtomicInteger atomicCount = new AtomicInteger();

	@Bean
	public Consumer<Flux<Message<Map<String, Object>>>> receive() {
		return messages -> messages.doOnNext(message -> {
			int count = atomicCount.getAndIncrement();
			if (count == 5 || (count > 5 && count % 5 == 0)) {
				nackMessage(message,false);
			}
			else {
				nackMessage(message,true);
			}
		}).subscribe();
	}

	private static void ackMessage(Message<Map<String, Object>> message) {
		try {
			log.info("acking message: {}", message);
			final Channel channel = message.getHeaders().get(AmqpHeaders.CHANNEL, Channel.class);
			if (nonNull(channel)) {
				channel.basicAck(message.getHeaders().get(AmqpHeaders.DELIVERY_TAG, Long.class), false);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void nackMessage(Message<Map<String, Object>> message, boolean requeue) {
		try {
			log.info("Nack message (requeue:{}) : {}", requeue,message);

			final Channel channel = message.getHeaders().get(AmqpHeaders.CHANNEL, Channel.class);
			if (nonNull(channel)) {
				channel.basicNack(message.getHeaders().get(AmqpHeaders.DELIVERY_TAG, Long.class), false, requeue);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}