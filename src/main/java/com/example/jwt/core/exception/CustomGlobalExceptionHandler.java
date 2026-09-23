package com.example.jwt.core.exception;

import com.example.jwt.domain.module.exception.ModuleNotFoundException;
import com.example.jwt.domain.module.exception.ModuleServiceUnavailableException;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class CustomGlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ResponseError handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    return new ResponseError()
        .setTimeStamp(LocalDate.now())
        .setErrors(ex.getBindingResult().getFieldErrors().stream().collect(
            Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage())))
        .build();
  }

  /** Ungueltige Parameter, z.B. keine gueltige UUID in der URL -> 400. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ResponseError handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return error("INVALID_PARAMETER",
        String.format("Parameter '%s' has an invalid value", ex.getName()));
  }

  /** User (oder anderer Datensatz) nicht gefunden -> 404. */
  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  public ResponseError handleNotFound(NoSuchElementException ex) {
    return error("NOT_FOUND", ex.getMessage());
  }

  /** Modul existiert im module_service nicht -> 404. */
  @ExceptionHandler(ModuleNotFoundException.class)
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  public ResponseError handleModuleNotFound(ModuleNotFoundException ex) {
    return error("MODULE_NOT_FOUND", ex.getMessage());
  }

  /** module_service nicht erreichbar / Circuit Breaker offen -> 503. */
  @ExceptionHandler(ModuleServiceUnavailableException.class)
  @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
  public ResponseError handleModuleServiceUnavailable(ModuleServiceUnavailableException ex) {
    return error("MODULE_SERVICE_UNAVAILABLE", ex.getMessage());
  }

  private ResponseError error(String code, String message) {
    return new ResponseError()
        .setTimeStamp(LocalDate.now())
        .setErrors(Map.of("code", code, "message", message == null ? "" : message))
        .build();
  }
}
