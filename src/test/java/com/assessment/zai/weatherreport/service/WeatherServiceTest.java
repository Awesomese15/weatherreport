package com.assessment.zai.weatherreport.service;

import com.assessment.zai.weatherreport.dto.WeatherResponse;
import com.assessment.zai.weatherreport.provider.OpenWeatherMapProvider;
import com.assessment.zai.weatherreport.provider.WeatherStackProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherServiceTest {

    private WeatherStackProvider primaryProvider;
    private OpenWeatherMapProvider secondaryProvider;
    private CacheManager cacheManager;
    private Cache cache;
    private WeatherService weatherService;

    private final String CITY = "Sydney";

    @BeforeEach
    void setUp() {
        primaryProvider = mock(WeatherStackProvider.class);
        secondaryProvider = mock(OpenWeatherMapProvider.class);
        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);

        when(cacheManager.getCache("weather")).thenReturn(cache);

        weatherService = new WeatherService(primaryProvider, secondaryProvider, cacheManager);
    }

    @Test
    void getWeather_primarySuccess_returnsPrimaryResponse() {
        WeatherResponse primaryResponse = new WeatherResponse();
        primaryResponse.setTemperatureDegrees(25.0);
        primaryResponse.setWindSpeed(10.0);

        when(primaryProvider.fetch(CITY)).thenReturn(primaryResponse);

        WeatherResponse response = weatherService.getWeather(CITY);

        assertNotNull(response);
        assertEquals(25.0, response.getTemperatureDegrees());
        assertEquals(10.0, response.getWindSpeed());

        verify(primaryProvider, times(1)).fetch(CITY);
        verifyNoInteractions(secondaryProvider);
    }

    @Test
    void getWeather_primaryFails_secondarySucceeds_returnsSecondaryResponse() {
        WeatherResponse secondaryResponse = new WeatherResponse();
        secondaryResponse.setTemperatureDegrees(20.0);
        secondaryResponse.setWindSpeed(5.0);

        when(primaryProvider.fetch(CITY)).thenThrow(new RuntimeException("Primary failed"));
        when(secondaryProvider.fetch(CITY)).thenReturn(secondaryResponse);

        // Call fallbackToSecondary directly to simulate circuit breaker fallback
        WeatherResponse response = weatherService.fallbackToSecondary(new RuntimeException("Primary failed"), CITY);

        assertNotNull(response);
        assertEquals(20.0, response.getTemperatureDegrees());
        assertEquals(5.0, response.getWindSpeed());

        verify(primaryProvider, never()).fetch(CITY); // fallbackToSecondary does not call primary
        verify(secondaryProvider, times(1)).fetch(CITY);
    }

    @Test
    void getSecondary_secondaryFails_cacheHasData_returnsCachedResponse() {
        WeatherResponse cachedResponse = new WeatherResponse();
        cachedResponse.setTemperatureDegrees(15.0);
        cachedResponse.setWindSpeed(3.0);

        when(secondaryProvider.fetch(CITY)).thenThrow(new RuntimeException("Secondary failed"));
        when(cache.get(CITY, WeatherResponse.class)).thenReturn(cachedResponse);

        // Call getSecondary to trigger fallbackToCache
        RuntimeException ex = assertThrows(RuntimeException.class, () -> weatherService.getSecondary(CITY));
        assertEquals("Secondary failed", ex.getMessage());  // <-- changed here

        // Call fallbackToCache manually simulating fallback
        WeatherResponse response = weatherService.fallbackToCache(ex, CITY);

        assertNotNull(response);
        assertEquals(15.0, response.getTemperatureDegrees());
        assertEquals(3.0, response.getWindSpeed());

        verify(secondaryProvider, times(1)).fetch(CITY);
        verify(cache, times(1)).get(CITY, WeatherResponse.class);
    }


    @Test
    void fallbackToCache_noCache_throwsIllegalStateException() {
        when(cacheManager.getCache("weather")).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> weatherService.fallbackToCache(new RuntimeException("fail"), CITY));

        assertEquals("All weather providers are down", ex.getMessage());
    }

    @Test
    void fallbackToCache_cacheEmpty_throwsIllegalStateException() {
        when(cache.get(CITY, WeatherResponse.class)).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> weatherService.fallbackToCache(new RuntimeException("fail"), CITY));

        assertEquals("All weather providers are down", ex.getMessage());
    }
}