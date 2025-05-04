package com.assessment.zai.weatherreport.service;

import com.assessment.zai.weatherreport.dto.WeatherResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.assessment.zai.weatherreport.provider.OpenWeatherMapProvider;
import com.assessment.zai.weatherreport.provider.WeatherProvider;
import com.assessment.zai.weatherreport.provider.WeatherStackProvider;

/**
 * Service responsible for retrieving weather information.
 * Implements a fallback strategy:
 * 1. First tries the primary weather provider (WeatherStack)
 * 2. If primary fails, falls back to secondary provider (OpenWeatherMap)
 * 3. If both fail, returns cached data if available
 * 4. If no cached data, throws an exception
 */
@Service
public class WeatherService {
    private static final String CACHE = "weather";
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private final WeatherProvider primary;
    private final WeatherProvider secondary;
    private final CacheManager cacheManager;

    /**
     * Constructs the WeatherService with primary and secondary providers
     *
     * @param primary      The primary weather provider (WeatherStack)
     * @param secondary    The secondary weather provider (OpenWeatherMap)
     * @param cacheManager The cache manager for storing weather data
     */
    public WeatherService(WeatherStackProvider primary,
                          OpenWeatherMapProvider secondary,
                          CacheManager cacheManager) {
        this.primary = primary;
        this.secondary = secondary;
        this.cacheManager = cacheManager;
    }

    /**
     * Gets weather information for a specified city
     * Results are cached by city name
     *
     * @param city The name of the city to get weather for
     * @return Weather information response
     */
    @Cacheable(cacheNames = CACHE, key = "#city")
    @CircuitBreaker(name = "primary", fallbackMethod = "fallbackToSecondary")
    public WeatherResponse getWeather(String city) {
        logger.info("Fetching weather from primary provider for {}", city);
        return primary.fetch(city);
    }

    /**
     * Fallback method when primary provider fails
     *
     * @param ex   The exception that caused the fallback
     * @param city The city to fetch weather for
     * @return Weather information from secondary provider
     */
    public WeatherResponse fallbackToSecondary(Exception ex, String city) {
        logger.info("Primary provider failed, falling back to secondary for {}", city);
        return getSecondary(city);
    }

    /**
     * Gets weather from secondary provider
     *
     * @param city The city to fetch weather for
     * @return Weather information from secondary provider
     */
    @CircuitBreaker(name = "secondary", fallbackMethod = "fallbackToCache")
    WeatherResponse getSecondary(String city) {
        logger.info("Fetching weather from secondary provider for {}", city);
        return secondary.fetch(city);
    }

    /**
     * Fallback to cached data when both providers fail
     *
     * @param t    The throwable that caused the fallback
     * @param city The city to fetch weather for
     * @return Cached weather information if available
     * @throws IllegalStateException if no cached data is available
     */
    public WeatherResponse fallbackToCache(Throwable t, String city) {
        logger.info("Both providers failed, attempting to use cached data for {}", city);
        Cache cache = cacheManager.getCache(CACHE);
        if (cache != null) {
            WeatherResponse stale = cache.get(city, WeatherResponse.class);
            if (stale != null) return stale;
        }
        logger.error("All weather providers are down and no cache available for {}", city);
        throw new IllegalStateException("All weather providers are down");
    }
}
