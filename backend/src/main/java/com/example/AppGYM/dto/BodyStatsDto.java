// backend/src/main/java/com/example/AppGYM/dto/BodyStatsDto.java
package com.example.AppGYM.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BodyStatsDto {
    public LocalDate date;
    public Double weightKg;
    public Double neckCm,chestCm,waistCm,lowerAbsCm,hipCm,
            bicepsCm,bicepsFlexCm,forearmCm,thighCm,calfCm;
    public String frontImgUrl,sideImgUrl,backImgUrl;
}
