package com.yourname.shorten.controller;

import com.yourname.shorten.infrastructure.StatsCounter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsCounter counter;

    public StatsController(StatsCounter counter) {
        this.counter = counter;
    }

    @GetMapping("/{shortCode}")
    public Map<String, Object> stats(@PathVariable String shortCode) {
        long total = counter.getTotal(shortCode);
        List<StatsCounter.DayCount> last7 = counter.last7Days(shortCode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shortCode", shortCode);
        result.put("totalClicks", total);
        result.put("last7Days", last7);
        return result;
    }
}
