// backend/src/main/java/com/example/AppGYM/dto/ProfileDto.java
package com.example.AppGYM.dto;

import lombok.Data;

@Data
public class ProfileDto {
    public String firstName,lastName;
    public Integer age;
    public Double heightCm,weightKg;
    public Double neckCm,chestCm,waistCm,lowerAbsCm,hipCm,
            bicepsCm,bicepsFlexCm,forearmCm,thighCm,calfCm;
    public String frontImgUrl,sideImgUrl,backImgUrl;
}
