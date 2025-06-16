// backend/src/main/java/com/example/AppGYM/dto/DailyDto.java
package com.example.AppGYM.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyDto {
    public LocalDate date;
    public List<MachineEntryDto> exercises;
}
