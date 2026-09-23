package com.example.jwt.domain.module.dto;

import java.util.UUID;

/** Antwort des user_mgmt_service nach einer erfolgreichen Modulzuweisung. */
public record ModuleAssignmentDTO(UUID userId, ModuleDTO module, String status) {

}
