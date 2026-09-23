package com.example.jwt.domain.module.exception;

import java.util.UUID;

/** Das Modul existiert im module_service nicht (bzw. ist nicht verfuegbar) -> HTTP 404. */
public class ModuleNotFoundException extends RuntimeException {

  public ModuleNotFoundException(UUID moduleId) {
    super(String.format("Module with ID '%s' is not available", moduleId));
  }
}
