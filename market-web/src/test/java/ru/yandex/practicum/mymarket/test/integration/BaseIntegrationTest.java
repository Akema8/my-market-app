package ru.yandex.practicum.mymarket.test.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.mymarket.test.config.TestConfig;

@SpringBootTest
@Import(TestConfig.class)
public abstract class BaseIntegrationTest {
}
