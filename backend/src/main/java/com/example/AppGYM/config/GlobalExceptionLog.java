package com.example.AppGYM.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/**
 * Intercepta cualquier excepción no controlada, la registra con stack-trace
 * completo y la vuelve a lanzar para que Spring devuelva el código adecuado.
 */
@ControllerAdvice @Slf4j
public class GlobalExceptionLog {

    @ExceptionHandler(Throwable.class)
    public void logAll(Throwable ex) throws Throwable {
        if (ex instanceof ResponseStatusException rse) {
            log.error("UNCAUGHT → status={}  {}", rse.getStatusCode(), rse.getReason(), ex);
            throw ex;                         // deja que Spring responda
        }
        log.error("UNCAUGHT 500 → {}", ex.getMessage(), ex);
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
    }
}
