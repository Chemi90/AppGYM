package com.example.AppGYM.dto;

import java.time.LocalDate;

/**
 * DTO simple para el formulario de medidas / fotos.
 * Usamos un <b>record</b> de Java 17, cuyos “accessors” se llaman exactamente:
 * {@code date()}, {@code weightKg()}, {@code neckCm()}, … — los mismos que
 * invoca StatsController.
 *
 * <p> Spring 6 soporta la vinculación automática (@ModelAttribute) a records,
 * incluido multipart/form-data.  Por tanto no necesitas setters.
 */
public record BodyStatsDto(
        /* fecha de la medición (opcional) */
        LocalDate date,

        /* medidas en cm (peso en kg) — todos opcionales */
        Double weightKg,
        Double neckCm,
        Double chestCm,
        Double waistCm,
        Double lowerAbsCm,
        Double hipCm,
        Double bicepsCm,
        Double bicepsFlexCm,
        Double forearmCm,
        Double thighCm,
        Double calfCm
) { }
