package com.example.jwt.domain.module;

import com.example.jwt.domain.module.dto.ModuleAssignmentDTO;
import com.example.jwt.domain.module.dto.ModuleDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Neue Schnittstelle (Aufgabe 6): einem User ein Modul zuweisen.
 *
 * <p>PUT    /users/{userId}/modules/{moduleId}  -> Modul zuweisen
 * <p>GET    /users/{userId}/modules             -> zugewiesene Module lesen
 * <p>DELETE /users/{userId}/modules/{moduleId}  -> Zuweisung entfernen
 */
@Validated
@RestController
@RequestMapping("/users")
public class UserModuleController {

  private final UserModuleService userModuleService;

  public UserModuleController(UserModuleService userModuleService) {
    this.userModuleService = userModuleService;
  }

  @PutMapping("/{userId}/modules/{moduleId}")
  public ResponseEntity<ModuleAssignmentDTO> assignModule(@PathVariable UUID userId,
      @PathVariable UUID moduleId) {
    return ResponseEntity.ok(userModuleService.assignModule(userId, moduleId));
  }

  @GetMapping("/{userId}/modules")
  public ResponseEntity<List<ModuleDTO>> retrieveAssignedModules(@PathVariable UUID userId) {
    return ResponseEntity.ok(userModuleService.findAssignedModules(userId));
  }

  @DeleteMapping("/{userId}/modules/{moduleId}")
  public ResponseEntity<Void> removeModule(@PathVariable UUID userId,
      @PathVariable UUID moduleId) {
    userModuleService.removeModule(userId, moduleId);
    return ResponseEntity.noContent().build();
  }
}
