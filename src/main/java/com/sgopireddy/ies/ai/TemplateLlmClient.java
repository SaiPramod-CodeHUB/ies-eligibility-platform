package com.sgopireddy.ies.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a hosted model. It composes the notice from the
 * grounded facts it is handed, which is exactly what the system prompt
 * instructs a real model to do.
 *
 * This exists so the notice pipeline — prompt assembly, grounding, validation,
 * persistence — is fully tested without an API key, and so a demo runs offline.
 * Point {@code ies.ai.llm} at a hosted client to swap it.
 */
@Component
@ConditionalOnMissingBean(name = "hostedLlmClient")
public class TemplateLlmClient implements LlmClient {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        // The grounded facts arrive as a FACTS block; echo them as prose.
        StringBuilder out = new StringBuilder();
        boolean inFacts = false;
        for (String line : userPrompt.lines().toList()) {
            if (line.startsWith("FACTS:")) { inFacts = true; continue; }
            if (line.startsWith("REQUEST:")) { inFacts = false; continue; }
            if (inFacts && !line.isBlank()) {
                out.append(line.replaceFirst("^\\s*-\\s*", "")).append('\n');
            }
        }
        return out.toString().strip();
    }

    @Override public String modelId() { return "template-deterministic-v1"; }
}
