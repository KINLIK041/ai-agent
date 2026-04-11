package com.kinlik.aicodehelper.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();

    public WeatherSummary getTodayWeather(String city) {
        String targetCity = (city == null || city.isBlank()) ? "Beijing" : city;
        String url = "https://wttr.in/" + targetCity + "?format=j1";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();
            if (body == null) {
                return new WeatherSummary(targetCity, "天气数据暂不可用", "--", "--", "可以安排轻量活动。");
            }
            var current = ((java.util.List<Map>) body.get("current_condition")).get(0);
            var weather = ((java.util.List<Map>) body.get("weather")).get(0);
            String desc = ((java.util.List<Map>) current.get("weatherDesc")).get(0).get("value").toString();
            String temp = current.get("temp_C") + "°C";
            String humidity = current.get("humidity") + "%";
            String max = weather.get("maxtempC") + "°C";
            String min = weather.get("mintempC") + "°C";
            String suggestion = "今天" + desc + "，气温 " + min + " ~ " + max + "，适合安排 " + (desc.contains("rain") || desc.contains("雨") ? "室内放松和复盘" : "轻量外出与散步");
            return new WeatherSummary(targetCity, desc, temp, humidity, suggestion);
        } catch (Exception e) {
            return new WeatherSummary(targetCity, "天气数据暂不可用", "--", "--", "先按室内节奏安排今天。 ");
        }
    }

    public record WeatherSummary(String city, String description, String temperature, String humidity, String suggestion) {
    }
}
