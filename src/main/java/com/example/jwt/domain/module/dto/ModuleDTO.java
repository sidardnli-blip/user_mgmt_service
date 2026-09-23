package com.example.jwt.domain.module.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Antwort des module_service auf GET /api/v1/modules/{id}.
 * Unbekannte Felder (created_at, updated_at) werden ignoriert.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModuleDTO(UUID id, String code, String name, String description) {

}
