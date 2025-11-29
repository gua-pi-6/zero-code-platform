package com.chen.core;

import com.chen.ai.AiCodeGeneratorService;
import com.chen.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
class AiGenerateFacadeTest {

    private static final Logger log = LoggerFactory.getLogger(AiGenerateFacadeTest.class);
    @Resource
    private AiGenerateFacade aiGenerateFacade;

    @Test
    void generateWithSaveStream() {
        String userMessage = "生成一个登录页面,总共不超过20行代码";

        Flux<String> result = aiGenerateFacade.generateWithSaveStream(userMessage, CodeGenTypeEnum.MULTI_FILE);
        List<String> stringList = result.collectList().block();
        Assertions.assertNotNull(stringList);

        String joinedString = String.join("", stringList);
        Assertions.assertNotNull(joinedString);
    }
}