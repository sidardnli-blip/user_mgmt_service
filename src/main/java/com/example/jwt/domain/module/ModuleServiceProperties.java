package com.example.jwt.domain.module;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfiguration fuer die Kommunikation mit dem module_service.
 * Alle Werte kommen zur Laufzeit aus der Kubernetes ConfigMap (keine Hardcodings).
 */
@Component
@ConfigurationProperties("module-service")
public class ModuleServiceProperties {

  /** Basis-URL des Kubernetes Service, z.B. http://module-service:8000 */
  private String baseUrl = "http://module-service:8000";

  /** Timeout fuer den Verbindungsaufbau. */
  private Duration connectTimeout = Duration.ofSeconds(2);

  /** Timeout fuer das Lesen der Antwort. */
  private Duration readTimeout = Duration.ofSeconds(3);

  private final RetrySettings retry = new RetrySettings();

  private final CircuitBreakerSettings circuitBreaker = new CircuitBreakerSettings();

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
  }

  public RetrySettings getRetry() {
    return retry;
  }

  public CircuitBreakerSettings getCircuitBreaker() {
    return circuitBreaker;
  }

  public static class RetrySettings {

    /** Gesamtzahl der Versuche (1 = kein Retry). */
    private int maxAttempts = 3;

    /** Wartezeit zwischen zwei Versuchen. */
    private Duration waitDuration = Duration.ofMillis(200);

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public Duration getWaitDuration() {
      return waitDuration;
    }

    public void setWaitDuration(Duration waitDuration) {
      this.waitDuration = waitDuration;
    }
  }

  public static class CircuitBreakerSettings {

    /** Ab welcher Fehlerquote in Prozent der Circuit Breaker oeffnet. */
    private float failureRateThreshold = 50f;

    /** Groesse des Auswertungsfensters (Anzahl Calls). */
    private int slidingWindowSize = 10;

    /** Mindestanzahl Calls, bevor die Fehlerquote ueberhaupt bewertet wird. */
    private int minimumNumberOfCalls = 5;

    /** Wie lange der Circuit Breaker offen bleibt, bevor er halb oeffnet. */
    private Duration waitDurationInOpenState = Duration.ofSeconds(20);

    /** Wie viele Testaufrufe im Zustand HALF_OPEN erlaubt sind. */
    private int permittedNumberOfCallsInHalfOpenState = 3;

    public float getFailureRateThreshold() {
      return failureRateThreshold;
    }

    public void setFailureRateThreshold(float failureRateThreshold) {
      this.failureRateThreshold = failureRateThreshold;
    }

    public int getSlidingWindowSize() {
      return slidingWindowSize;
    }

    public void setSlidingWindowSize(int slidingWindowSize) {
      this.slidingWindowSize = slidingWindowSize;
    }

    public int getMinimumNumberOfCalls() {
      return minimumNumberOfCalls;
    }

    public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
      this.minimumNumberOfCalls = minimumNumberOfCalls;
    }

    public Duration getWaitDurationInOpenState() {
      return waitDurationInOpenState;
    }

    public void setWaitDurationInOpenState(Duration waitDurationInOpenState) {
      this.waitDurationInOpenState = waitDurationInOpenState;
    }

    public int getPermittedNumberOfCallsInHalfOpenState() {
      return permittedNumberOfCallsInHalfOpenState;
    }

    public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
      this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
    }
  }
}
