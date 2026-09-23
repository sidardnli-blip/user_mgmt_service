package com.example.jwt.domain.module;

import com.example.jwt.domain.module.dto.ModuleAssignmentDTO;
import com.example.jwt.domain.module.dto.ModuleDTO;
import com.example.jwt.domain.module.exception.ModuleNotFoundException;
import com.example.jwt.domain.module.exception.ModuleServiceUnavailableException;
import com.example.jwt.domain.user.UserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Fachlogik der Modulzuweisung.
 *
 * <p>Ablauf: User lokal pruefen -> Modul beim module_service pruefen -> Zuweisung
 * im module_service schreiben. Der user_mgmt_service hat keinen direkten Zugriff
 * auf die MySQL Datenbank des module_service.
 */
@Service
public class UserModuleService {

  private static final String METRIC_NAME = "module_assignment_requests";

  private final UserService userService;
  private final ModuleClient moduleClient;
  private final MeterRegistry meterRegistry;

  public UserModuleService(UserService userService, ModuleClient moduleClient,
      MeterRegistry meterRegistry) {
    this.userService = userService;
    this.moduleClient = moduleClient;
    this.meterRegistry = meterRegistry;
  }

  public ModuleAssignmentDTO assignModule(UUID userId, UUID moduleId) {
    requireExistingUser(userId);
    try {
      ModuleDTO module = moduleClient.findModule(moduleId)
          .orElseThrow(() -> new ModuleNotFoundException(moduleId));
      moduleClient.assignModuleToUser(userId, moduleId);
      count("assigned");
      return new ModuleAssignmentDTO(userId, module, "ASSIGNED");
    } catch (ModuleNotFoundException notFound) {
      count("module_not_found");
      throw notFound;
    } catch (ModuleServiceUnavailableException unavailable) {
      count("service_unavailable");
      throw unavailable;
    }
  }

  public List<ModuleDTO> findAssignedModules(UUID userId) {
    requireExistingUser(userId);
    return moduleClient.findModulesOfUser(userId);
  }

  public void removeModule(UUID userId, UUID moduleId) {
    requireExistingUser(userId);
    moduleClient.removeModuleFromUser(userId, moduleId);
  }

  private void requireExistingUser(UUID userId) {
    if (!userService.existsById(userId)) {
      throw new NoSuchElementException(String.format("User with ID '%s' could not be found", userId));
    }
  }

  /** Eigene Fachmetrik fuer Prometheus: module_assignment_requests_total{result="..."} */
  private void count(String result) {
    Counter.builder(METRIC_NAME)
        .description("Ergebnisse der Modulzuweisungen")
        .tag("result", result)
        .register(meterRegistry)
        .increment();
  }
}
