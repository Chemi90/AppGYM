// backend/src/main/java/com/example/AppGYM/dto/MachineEntryDto.java
package com.example.AppGYM.dto;

import lombok.Data;

@Data
public class MachineEntryDto {
    public String name;
    public Double weightKg;
    public Integer reps;
    public Integer sets;
}
