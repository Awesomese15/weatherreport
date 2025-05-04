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
 * Weather provider implementation that fetches data from WeatherStack API.
 * This provider connects to the WeatherStack API and parses the response
 * to extract temperature and wind speed data.
 */
@Component
public class WeatherStackProvider implements WeatherProvider {
    private static final Logger logger = LoggerFactory.getLogger(WeatherStackProvider.class);
    private final RestTemplate template;

    @Value("${weatherstack.access-key}")
    private String key;

    private static final String WEATHERSTACK_API_URL = "http://api.weatherstack.com/current?access_key=%s&query=%s";
    private static final String CURRENT_KEY = "current";
    private static final String ERROR_KEY = "error";
    private static final String TEMPERATURE_KEY = "temperature";
    private static final String WIND_SPEED_KEY = "wind_speed";

    /**
     * Constructs a new WeatherStackProvider with the given RestTemplate.
     *
     * @param template The RestTemplate used to make HTTP requests
     */
    public WeatherStackProvider(RestTemplate template) {
        this.template = template;
        logger.info("WeatherStackProvider initialized");
    }

    /**
     * Fetches weather data for the specified city from WeatherStack API.
     *
     * @param city The city name to get weather data for
     * @return WeatherResponse containing temperature and wind speed
     * @throws RuntimeException If there is an error fetching or parsing the data
     */
    @Override
    public WeatherResponse fetch(String city) {
        logger.info("Fetching weather data from WeatherStack API for city: {}", city);
        try {
            String url = String.format(WEATHERSTACK_API_URL, key, city);
            Map<?, ?> resp = template.getForObject(url, Map.class);

            if (resp == null) {
                logger.error("Received null response from WeatherStack API");
                throw new RuntimeException("Failed to get weather data: null response");
            }

            if (resp.containsKey(ERROR_KEY)) {
                Map<?, ?> error = (Map<?, ?>) resp.get(ERROR_KEY);
                logger.error("Error from WeatherStack API: {}", error);
                throw new RuntimeException("WeatherStack API error: " + error.get("info"));
            }

            Map<?, ?> curr = (Map<?, ?>) resp.get(CURRENT_KEY);
            if (curr == null) {
                logger.error("Missing 'current' data in the response: {}", resp);
                throw new RuntimeException("Invalid response format: missing current data");
            }

            WeatherResponse out = new WeatherResponse();
            try {
                Number temperature = (Number) curr.get(TEMPERATURE_KEY);
                Number windSpeed = (Number) curr.get(WIND_SPEED_KEY);

                if (temperature == null || windSpeed == null) {
                    logger.error("Missing temperature or wind speed data: {}", curr);
                    throw new RuntimeException("Missing required weather data");
                }

                out.setTemperatureDegrees(temperature.doubleValue());
                out.setWindSpeed(windSpeed.doubleValue());
                logger.info("Successfully fetched weather data for {}: temp={}, wind={}",
                        city, temperature, windSpeed);
                return out;
            } catch (ClassCastException e) {
                logger.error("Failed to parse weather data: {}", curr, e);
                throw new RuntimeException("Invalid data format in weather response", e);
            }
        } catch (Exception e) {
            logger.error("Error fetching weather data for {}: {}", city, e.getMessage(), e);
            throw new RuntimeException("Failed to get weather data: " + e.getMessage(), e);
        }
    }
}
