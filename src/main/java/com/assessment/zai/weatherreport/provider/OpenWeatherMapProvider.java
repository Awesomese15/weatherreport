package com.assessment.zai.weatherreport.provider;

import com.assessment.zai.weatherreport.dto.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * OpenWeatherMap API provider implementation
 * Acts as a secondary/fallback weather data source
 */
@Component
public class OpenWeatherMapProvider implements WeatherProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenWeatherMapProvider.class);
    private static final String OPENWEATHERMAP_API_URL =
            "http://api.openweathermap.org/data/2.5/weather?q=%s,AU&appid=%s&units=metric";
    private static final String COD_KEY = "cod";
    private static final String SUCCESS_CODE = "200";
    private static final String MESSAGE_KEY = "message";
    private static final String MAIN_KEY = "main";
    private static final String WIND_KEY = "wind";
    private static final String TEMP_KEY = "temp";
    private static final String SPEED_KEY = "speed";
    private static final String NULL_RESPONSE_ERROR = "Received null response from OpenWeatherMap API";
    private static final String FETCH_FAILURE_ERROR = "Failed to fetch data from OpenWeatherMap API";
    private static final String API_ERROR_PREFIX = "OpenWeatherMap API error: ";
    private static final String MISSING_DATA_ERROR = "Invalid response format from OpenWeatherMap API";
    private static final String INVALID_DATA_ERROR = "Invalid data in OpenWeatherMap API response";
    private static final String CACHE_VALUE = "weatherData";
    private static final String CACHE_KEY_SUFFIX = "_openweathermap";
    private static final String GENERAL_ERROR_PREFIX = "Failed to get weather data: ";

    private final RestTemplate template;

    @Value("${openweathermap.app-id}")
    private String appId;

    public OpenWeatherMapProvider(RestTemplate template) {
        this.template = template;
        logger.debug("OpenWeatherMapProvider initialized");
    }

    /**
     * Fetches weather data for the specified city from OpenWeatherMap API.
     *
     * @param city The city name to get weather data for
     * @return WeatherResponse containing temperature and wind speed
     * @throws RuntimeException If there is an error fetching or parsing the data
     */
    @Override
    public WeatherResponse fetch(String city) {
        logger.debug("Fetching weather data from OpenWeatherMap API for {}", city);
        try {
            String url = String.format(OPENWEATHERMAP_API_URL, city, appId);
            Map<?, ?> resp = template.getForObject(url, Map.class);

            if (resp == null) {
                logger.error(NULL_RESPONSE_ERROR);
                throw new RuntimeException(FETCH_FAILURE_ERROR);
            }

            if (resp.containsKey(COD_KEY) && !resp.get(COD_KEY).toString().equals(SUCCESS_CODE)) {
                logger.error("Error from OpenWeatherMap API: {}", resp.get(MESSAGE_KEY));
                throw new RuntimeException(API_ERROR_PREFIX + resp.get(MESSAGE_KEY));
            }

            Map<?, ?> main = (Map<?, ?>) resp.get(MAIN_KEY);
            Map<?, ?> wind = (Map<?, ?>) resp.get(WIND_KEY);

            if (main == null || wind == null) {
                logger.error("Missing data in the response: main={}, wind={}", main, wind);
                throw new RuntimeException(MISSING_DATA_ERROR);
            }

            Number temperature = (Number) main.get(TEMP_KEY);
            Number windSpeed = (Number) wind.get(SPEED_KEY);

            if (temperature == null || windSpeed == null) {
                logger.error("Missing temperature or wind speed in the response");
                throw new RuntimeException(INVALID_DATA_ERROR);
            }

            WeatherResponse out = new WeatherResponse();
            out.setTemperatureDegrees(temperature.doubleValue());
            out.setWindSpeed(windSpeed.doubleValue());

            logger.debug("Successfully fetched weather data for {}: temp={}, wind={}",
                    city, temperature, windSpeed);
            return out;
        } catch (Exception e) {
            logger.error("Error fetching weather data from OpenWeatherMap API", e);
            throw new RuntimeException(GENERAL_ERROR_PREFIX + e.getMessage(), e);
        }
    }
}
