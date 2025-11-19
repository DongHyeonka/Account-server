package com.synapse.account_service.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.synapse.account_service.domain.RefreshToken;

@TestConfiguration
public class RedisTemplateStubConfig {

    @Bean
    @Primary
    public RedisTemplate<String, RefreshToken> refreshTokenRedisTemplateStub() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, RefreshToken> redisTemplate = Mockito.mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, RefreshToken> valueOperations = Mockito.mock(ValueOperations.class);
        Map<String, RefreshToken> store = new ConcurrentHashMap<>();

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Mockito.when(valueOperations.get(Mockito.anyString()))
                .thenAnswer(invocation -> store.get(invocation.getArgument(0)));

        Mockito.doAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    RefreshToken value = invocation.getArgument(1);
                    store.put(key, value);
                    return null;
                })
                .when(valueOperations)
                .set(Mockito.anyString(), Mockito.any(RefreshToken.class));

        Mockito.doAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    RefreshToken value = invocation.getArgument(1);
                    store.put(key, value);
                    return null;
                })
                .when(valueOperations)
                .set(Mockito.anyString(), Mockito.any(RefreshToken.class), Mockito.anyLong(), Mockito.any(TimeUnit.class));

        Mockito.doAnswer(invocation -> store.remove(invocation.getArgument(0)) != null)
                .when(redisTemplate)
                .delete(Mockito.anyString());

        return redisTemplate;
    }
}
