package com.synapse.account_service;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.account_service.config.RedisTemplateStubConfig;

@Transactional
@ActiveProfiles("test")
@SpringBootTest(classes = {AccountServiceConfig.class, RedisTemplateStubConfig.class})
@AutoConfigureMockMvc
public class TestConfig {
    
}
