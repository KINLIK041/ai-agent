package com.kinlik.aicodehelper.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class WeatherTool {

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool(name = "weatherSearch", value = "Query today's local weather summary for a city.")
    public String getWeather(@P("city to query") String city) {
        String targetCity = (city == null || city.isBlank()) ? "Beijing" : city;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity("https://wttr.in/" + targetCity + "?format=j1", Map.class);
            Map body = response.getBody();
            if (body == null) return "天气数据暂不可用";
            var current = ((List<Map>) body.get("current_condition")).get(0);
            String desc = ((List<Map>) current.get("weatherDesc")).get(0).get("value").toString();
            return targetCity + " 今天天气：" + desc + "，当前温度 " + current.get("temp_C") + "°C，湿度 " + current.get("humidity") + "%";
        } catch (Exception e) {
            return targetCity + " 天气查询失败";
        }
    }
}
