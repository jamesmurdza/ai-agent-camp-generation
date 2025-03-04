package org.example;

/**
 * for explanation on AI-models see:
 * <a href="https://platform.openai.com/docs/models">https://platform.openai.com/docs/models</a>
 */
public enum AiModels {
    GPT_4O("A large model like gpt-4o offers a very high level of intelligence and strong performance, with higher cost per token"),
    GPT_4O_MINI("A small model like gpt-4o-mini offers intelligence not quite on the level of the larger model, but it's faster and less expensive per token."),
    O1("A reasoning model like the o1 family of models is slower to return a result, and uses more tokens to \"think,\" but is capable of advanced reasoning, coding, and multi-step planning.");

    private final String description;
    private final String id;
    AiModels(String description){
        this.description = description;
        id = name().toLowerCase().replace("_","-");
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }
}
