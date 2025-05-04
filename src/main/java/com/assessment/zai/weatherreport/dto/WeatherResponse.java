package com.assessment.zai.weatherreport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class WeatherResponse {
    private double temperatureDegrees;
    private double windSpeed;
}
