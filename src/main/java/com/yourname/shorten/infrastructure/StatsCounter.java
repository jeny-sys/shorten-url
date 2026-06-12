package com.yourname.shorten.infrastructure;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class StatsCounter {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redis;

    public StatsCounter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void increment(String shortCode) {
        redis.opsForValue().increment(totalKey(shortCode));
        String dayKey = dayKey(shortCode, LocalDate.now());
        redis.opsForValue().increment(dayKey);
        redis.expire(dayKey, Duration.ofDays(30));
    }

    public long getTotal(String shortCode) {
        String value = redis.opsForValue().get(totalKey(shortCode));
        return value == null ? 0L : Long.parseLong(value);
    }

    public List<DayCount> last7Days(String shortCode) {
        LocalDate today = LocalDate.now();
        return IntStream.range(0, 7)
                .mapToObj(i -> today.minusDays(i))
                .map(d -> {
                    String value = redis.opsForValue().get(dayKey(shortCode, d));
                    return new DayCount(d.format(DAY_FMT), value == null ? 0L : Long.parseLong(value));
                })
                .collect(Collectors.toList());
    }

    private String totalKey(String shortCode) { return "stats:" + shortCode; }
    private String dayKey(String shortCode, LocalDate day) {
        return "stats:" + shortCode + ":day:" + day.format(DAY_FMT);
    }

    public record DayCount(String date, long clicks) {}
}
