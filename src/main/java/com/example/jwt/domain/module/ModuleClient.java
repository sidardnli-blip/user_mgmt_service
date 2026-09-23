package com.example.jwt.domain.module;

import com.example.jwt.domain.module.dto.ModuleDTO;
import com.example.jwt.domain.module.exception.ModuleNotFoundException;
import com.example.jwt.domain.module.exception.ModuleServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Synchroner REST Client zum module_service ueber den Kubernetes Service.
 * Jeder Aufruf ist mit Timeout, Retry und Circuit Breaker abgesichert.
 */
@Component
public class ModuleClient {

  private final RestClient restClient;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;
  private final Logger logger;

  public ModuleClient(RestClient moduleServiceRestClient,
      CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry, Logger logger) {
    this.restClient = moduleServiceRestClient;
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(ModuleServiceConfig.MODULE_SERVICE);
    this.retry = retryRegistry.retry(ModuleServiceConfig.MODULE_SERVICE);
    this.logger = logger;
  }

  /**
   * Prueft, ob ein Modul verfuegbar ist. Ein 404 ist eine fachliche Antwort
   * (Modul gibt es nicht) und wird bewusst NICHT als technischer Fehler gewertet.
   */
  public Optional<ModuleDTO> findModule(UUID moduleId) {
    return execute(() -> {
      try {
        return Optional.ofNullable(restClient.get()
            .uri("/api/v1/modules/{moduleId}", moduleId)
            .retrieve()
            .body(ModuleDTO.class));
      } catch (HttpClientErrorException.NotFound notFound) {
        return Optional.empty();
      }
    });
  }

  /** Schreibt die Zuweisung im module_service (idempotent). */
  public void assignModuleToUser(UUID userId, UUID moduleId) {
    execute(() -> {
      try {
        restClient.put()
            .uri("/api/v1/users/{userId}/modules/{moduleId}", userId, moduleId)
            .retrieve()
            .toBodilessEntity();
        return null;
      } catch (HttpClientErrorException.NotFound notFound) {
        throw new ModuleNotFoundException(moduleId);
      }
    });
  }

  /** Liest alle einem User zugewiesenen Module. */
  public List<ModuleDTO> findModulesOfUser(UUID userId) {
    List<ModuleDTO> modules = execute(() -> restClient.get()
        .uri("/api/v1/users/{userId}/modules", userId)
        .retrieve()
        .body(new ParameterizedTypeReference<List<ModuleDTO>>() {
        }));
    return modules == null ? List.of() : modules;
  }

  /** Entfernt eine Zuweisung wieder. */
  public void removeModuleFromUser(UUID userId, UUID moduleId) {
    execute(() -> {
      try {
        restClient.delete()
            .uri("/api/v1/users/{userId}/modules/{moduleId}", userId, moduleId)
            .retrieve()
            .toBodilessEntity();
        return null;
      } catch (HttpClientErrorException.NotFound notFound) {
        throw new ModuleNotFoundException(moduleId);
      }
    });
  }

  private <T> T execute(Supplier<T> call) {
    Supplier<T> guarded = Retry.decorateSupplier(retry,
        CircuitBreaker.decorateSupplier(circuitBreaker, call));
    try {
      return guarded.get();
    } catch (CallNotPermittedException openCircuit) {
      logger.warn("Circuit Breaker '{}' ist offen - Aufruf an module_service abgelehnt",
          ModuleServiceConfig.MODULE_SERVICE);
      throw new ModuleServiceUnavailableException(
          "module_service ist aktuell nicht verfuegbar (Circuit Breaker offen)", openCircuit);
    } catch (ResourceAccessException | HttpServerErrorException failure) {
      logger.warn("Aufruf an module_service fehlgeschlagen: {}", failure.getMessage());
      throw new ModuleServiceUnavailableException(
          "module_service ist aktuell nicht verfuegbar", failure);
    }
  }
}
