package com.huashi.eftransfer.app.modules.ai.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class AiPromptTemplateService {

    private final ResourceLoader resourceLoader;

    public AiPromptTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String loadSystemPrompt(String scene, String version) {
        return load(scene, version, "system");
    }

    public String renderUserPrompt(String scene, String version, Map<String, String> variables) {
        String template = load(scene, version, "user");
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String load(String scene, String version, String name) {
        Resource resource = resourceLoader.getResource("classpath:prompts/ai/" + scene + "/" + version + "/" + name + ".md");
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load AI prompt resource " + resource.getDescription(), exception);
        }
    }
}
