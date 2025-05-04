package com.assessment.zai.weatherreport.controller;

import com.assessment.zai.weatherreport.dto.WeatherResponse;
import com.assessment.zai.weatherreport.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/v1")
public class WeatherController {
    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);
    private final WeatherService service;
    public WeatherController(WeatherService service) {
        this.service = service;
    }
    /**
     * Retrieves the current weather for a specified city
     *
     * @param city the city name to get weather for (default: melbourne)
     * @return weather information response
     */
    @GetMapping("/weather")
    public WeatherResponse weather(@RequestParam(defaultValue = "melbourne") String city) {
        log.info("Calling weather method");
        return service.getWeather(city);
    }
}
