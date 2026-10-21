package com.sgopireddy.ies.ai;

/**
 * The LLM is an unreliable remote dependency, so it sits behind an interface:
 * the notice path can be tested, and run locally, without an API key or a
 * network call. {@code TemplateLlmClient} is the default; a hosted model is
 * enabled by configuration.
 */
public interface LlmClient {

    /**
     * @param systemPrompt the rules the model must follow
     * @param userPrompt   the grounded facts and the request
     * @return generated text
     * @throws LlmUnavailableException when the model cannot be reached
     */
    String complete(String systemPrompt, String userPrompt);

    /** Identifies which implementation produced a given output, for the audit record. */
    String modelId();
}
