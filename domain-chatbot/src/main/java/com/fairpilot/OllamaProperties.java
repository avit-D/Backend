package com.fairpilot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.properties 의 spring.ai.ollama.* 를 바인딩
 * 모델명 등 런타임 참조가 필요한 곳에 주입
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.ai.ollama.chat")
public class OllamaProperties {
    private String model = "llama3.2";
}
