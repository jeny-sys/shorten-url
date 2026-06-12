package com.yourname.shorten.infrastructure;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ClickEventProducer {

    public static final String STREAM_KEY = "clicks-stream";

    private final StringRedisTemplate redis;

    public ClickEventProducer(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void publish(String shortCode) {
        MapRecord<String, String, String> record = StreamRecords.mapBacked(
                Map.of("shortCode", shortCode, "ts", String.valueOf(System.currentTimeMillis()))
        ).withStreamKey(STREAM_KEY);
        redis.opsForStream().add(record);
    }
}
