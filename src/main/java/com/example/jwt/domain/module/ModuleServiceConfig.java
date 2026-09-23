package com.example.jwt.domain.module;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.http.HttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Baut den REST Client zum module_service inklusive Timeout, Retry und Circuit Breaker.
 *
 * <p>Reihenfolge der Absicherung: Retry umschliesst den Circuit Breaker. Jeder einzelne
 * Versuch laeuft also durch den Circuit Breaker; ist dieser offen, wird sofort
 * abgebrochen statt weiter zu versuchen.
 */
@Configuration
public class ModuleServiceConfig {

  public static final String MODULE_SERVICE = "moduleService";

  /** Timeout: Connect-Timeout am HttpClient, Read-Timeout an der RequestFactory. */
  @Bean
  public RestClient moduleServiceRestClient(ModuleServiceProperties properties,
      ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
    HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(properties.getConnectTimeout())
        .build();

    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.getReadTimeout());

    return restClientBuilderProvider.getIfAvailable(RestClient::builder)
        .baseUrl(properties.getBaseUrl())
        .requestFactory(requestFactory)
        .build();
  }

  /** Circuit Breaker: 4xx sind fachliche Antworten und zaehlen nicht als Fehler. */
  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry(ModuleServiceProperties properties) {
    ModuleServiceProperties.CircuitBreakerSettings settings = properties.getCircuitBreaker();
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(settings.getSlidingWindowSize())
        .minimumNumberOfCalls(settings.getMinimumNumberOfCalls())
        .failureRateThreshold(settings.getFailureRateThreshold())
        .waitDurationInOpenState(settings.getWaitDurationInOpenState())
        .permittedNumberOfCallsInHalfOpenState(settings.getPermittedNumberOfCallsInHalfOpenState())
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
        .build();

    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
    registry.circuitBreaker(MODULE_SERVICE);
    return registry;
  }

  /** Retry: nur bei Netzwerkfehlern, Timeouts und 5xx erneut versuchen. */
  @Bean
  public RetryRegistry retryRegistry(ModuleServiceProperties properties) {
    ModuleServiceProperties.RetrySettings settings = properties.getRetry();
    RetryConfig config = RetryConfig.custom()
        .maxAttempts(settings.getMaxAttempts())
        .waitDuration(settings.getWaitDuration())
        .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
        .build();

    RetryRegistry registry = RetryRegistry.of(config);
    registry.retry(MODULE_SERVICE);
    return registry;
  }

  /**
   * Meldet die Resilience4j Metriken an Micrometer, damit Prometheus sie abholen kann
   * (resilience4j_circuitbreaker_state, resilience4j_retry_calls_total, ...).
   * Spring Boot bindet alle MeterBinder Beans automatisch an die MeterRegistry.
   */
  @Bean
  public TaggedCircuitBreakerMetrics circuitBreakerMetrics(CircuitBreakerRegistry registry) {
    return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
  }

  @Bean
  public TaggedRetryMetrics retryMetrics(RetryRegistry registry) {
    return TaggedRetryMetrics.ofRetryRegistry(registry);
  }
}
