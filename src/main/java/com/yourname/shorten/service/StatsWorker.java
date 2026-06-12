package com.yourname.shorten.service;

import com.yourname.shorten.infrastructure.ClickEventProducer;
import com.yourname.shorten.infrastructure.StatsCounter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class StatsWorker {

    private static final Logger log = LoggerFactory.getLogger(StatsWorker.class);
    private static final String GROUP = "stats-worker";
    private static final String CONSUMER = "consumer-1";

    private final StringRedisTemplate redis;
    private final StatsCounter counter;

    public StatsWorker(StringRedisTemplate redis, StatsCounter counter) {
        this.redis = redis;
        this.counter = counter;
    }

    @PostConstruct
    void ensureGroup() {
        try {
            redis.opsForStream().createGroup(ClickEventProducer.STREAM_KEY, ReadOffset.from("0"), GROUP);
        } catch (Exception ignored) {
            // group already exists
        }
    }

    @Scheduled(fixedDelay = 1000)
    void consume() {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(GROUP, CONSUMER),
                StreamReadOptions.empty().count(100).block(Duration.ofMillis(100)),
                StreamOffset.create(ClickEventProducer.STREAM_KEY, ReadOffset.lastConsumed())
        );
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            Object code = record.getValue().get("shortCode");
            if (code != null) {
                counter.increment(code.toString());
            }
            redis.opsForStream().acknowledge(ClickEventProducer.STREAM_KEY, GROUP, record.getId());
        }
    }
}
