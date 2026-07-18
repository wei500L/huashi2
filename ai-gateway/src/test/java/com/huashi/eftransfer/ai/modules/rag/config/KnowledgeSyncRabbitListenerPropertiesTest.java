package com.huashi.eftransfer.ai.modules.rag.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.integration.LexicalKnowledgeChangedEventListener;
import com.huashi.eftransfer.ai.modules.rag.repository.IntegrationConsumeRecordRepository;
import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeIngestionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeSyncRabbitListenerPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "spring.rabbitmq.listener.simple.auto-startup=false",
                    "spring.rabbitmq.listener.simple.prefetch=1",
                    "spring.rabbitmq.listener.simple.concurrency=1",
                    "spring.rabbitmq.listener.simple.max-concurrency=1"
            );

    @Test
    void shouldApplySingleFlightListenerSettingsToKnowledgeSyncConsumer() {
        contextRunner.run(context -> {
            RabbitListenerEndpointRegistry registry = context.getBean(RabbitListenerEndpointRegistry.class);

            assertThat(registry.getListenerContainers()).hasSize(1);
            assertThat(registry.getListenerContainers().iterator().next())
                    .isInstanceOf(SimpleMessageListenerContainer.class);

            SimpleMessageListenerContainer container =
                    (SimpleMessageListenerContainer) registry.getListenerContainers().iterator().next();

            assertThat(ReflectionTestUtils.getField(container, "prefetchCount")).isEqualTo(1);
            assertThat(ReflectionTestUtils.getField(container, "concurrentConsumers")).isEqualTo(1);
            assertThat(ReflectionTestUtils.getField(container, "maxConcurrentConsumers")).isEqualTo(1);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(KnowledgeSyncRabbitConfig.class)
    static class TestConfiguration {

        @Bean
        LexicalKnowledgeChangedEventListener lexicalKnowledgeChangedEventListener(
                ObjectMapper objectMapper,
                KnowledgeIngestionService knowledgeIngestionService,
                IntegrationConsumeRecordRepository integrationConsumeRecordRepository,
                RetryTemplate knowledgeSyncRetryTemplate
        ) {
            return new LexicalKnowledgeChangedEventListener(
                    objectMapper,
                    knowledgeIngestionService,
                    integrationConsumeRecordRepository,
                    knowledgeSyncRetryTemplate
            );
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        KnowledgeIngestionService knowledgeIngestionService() {
            return Mockito.mock(KnowledgeIngestionService.class);
        }

        @Bean
        IntegrationConsumeRecordRepository integrationConsumeRecordRepository() {
            return Mockito.mock(IntegrationConsumeRecordRepository.class);
        }
    }
}
