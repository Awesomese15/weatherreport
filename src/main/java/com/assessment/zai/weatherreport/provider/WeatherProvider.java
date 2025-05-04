package com.assessment.zai.weatherreport.provider;

import com.assessment.zai.weatherreport.dto.WeatherResponse;


public interface WeatherProvider {
    WeatherResponse fetch(String city);
}
