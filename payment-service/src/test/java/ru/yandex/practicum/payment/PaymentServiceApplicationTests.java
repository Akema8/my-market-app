package ru.yandex.practicum.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@SpringBootTest
class PaymentServiceApplicationTests {

	@MockBean
	private ReactiveRedisTemplate<String, Long> redisTemplate;

	@Test
	void contextLoads() {
	}
}
