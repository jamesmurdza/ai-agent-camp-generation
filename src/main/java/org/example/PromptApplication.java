package org.example;

import jakarta.annotation.Nullable;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ask OpenAI a complex question
 */
public class PromptApplication implements AiConstants{
    private static final Logger log = LoggerFactory.getLogger(PromptApplication.class);
    private final @Nullable String openApiKey;
    private final String modelName;
    private final boolean isValidKey;
    public PromptApplication(String[] args) {
        openApiKey = ChatHelper.determineInputVariable(args, OPENAI_API_KEY);
        modelName = ChatHelper.determineInputVariable(args, MODEL_NAME, AiModels.GPT_4O.getId());
        isValidKey = openApiKey!=null && openApiKey.length()>5 && !openApiKey.equals(OPENAI_API_KEY);
        log.info("{}={}", OPENAI_API_KEY, openApiKey );
    }



    public static void main(String[] args) throws InterruptedException {
        try {
            var main = new PromptApplication(args);

            if (main.openApiKey==null) {
                log.error("cannot start: no open api key provided.");
                return;
            }
            if (!main.isValidKey) {
                log.error("cannot start: invalid key provided {}", main.openApiKey);
                return;
            }

            var chatHistory = ChatHelper.createChatHistory();
            if (chatHistory == null) {
                return;
            }


            // Loop until the LLM has decided it's finished
            boolean finished = false;
            int counter = 0;
            while (!finished) {
                log.info("step {}", counter);
                finished = ChatHelper.executeChatStep(chatHistory, main.openApiKey, main.modelName);
                counter++;
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Fatal error: {}" , e.getMessage());
        }
    }

    private static class ChatHelper {
        // User prompt for the calculator
        private static final String USER_PROMPT = "What is the square root of 98237948273498274?";
        private static String determineInputVariable(String[] args, String varName) {
            return determineInputVariable(args, varName, null);
        }
        private static String determineInputVariable(String[] args, String varName, @Nullable String defaultVal) {
            if (System.getenv(varName)!=null) {
                return System.getenv(varName);
            } else
            if (System.getProperty(varName)!=null) {
                return System.getProperty(varName);
            }
            for (String arg : args) {
                if (arg.startsWith(varName)) {
                    String[] parts = arg.split("=");
                    return parts[1];
                }
            }
            return defaultVal;
        }
        private static HttpRequest createPostRequest(JSONArray chatHistory, String openApiKey, String modelName) throws JSONException {
            var requestBody = new JSONObject()
                    .put("model", modelName)
                    .put("messages", chatHistory);

            return HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
        }

        public static String callGPT(JSONArray chatHistory, String openApiKey, String modelName) throws InterruptedException {
            try {
                HttpRequest request = createPostRequest(chatHistory, openApiKey, modelName);

                try (var client = HttpClient.newHttpClient()) {
                    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    log.info("response status: {}", response.statusCode());
                    String responseBody = response.body();
                    if (response.statusCode()>250) {
                        log.info("response body: {}", responseBody);
                    }
                    return new JSONObject(responseBody)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString(CONTENT);
                }
            } catch (IllegalStateException| JSONException e) {
                throw new IllegalStateException("Failed to process JSON response", e);
            } catch (InterruptedException e) {
                throw new InterruptedException("Failed to call GPT API (interrupted)");
            } catch (Exception e) {
                throw new IllegalStateException("Failed to call GPT API", e);
            }
        }
        static boolean executeChatStep(JSONArray chatHistory, String openApiKey, String modelName) throws InterruptedException {
            boolean finished = false;
            try {
                // Call GPT and add the response to memory
                String responseText = callGPT(chatHistory, openApiKey, modelName);
                log.info("Agent response: {}" , responseText);
                chatHistory.put(new JSONObject()
                        .put("role", "assistant")
                        .put(CONTENT, responseText));

                // Parse the response for the resulting action
                String action = ResponseReader.getResponseAction(responseText);
                String actionInput = ResponseReader.getResponseActionInput(responseText);

                // The LLM decided to use the calculator
                if ("Calculator".equalsIgnoreCase(action.trim())) {
                    String calcResult = ResponseReader.calculator(actionInput);
                    chatHistory.put(new JSONObject()
                            .put("role", "user")
                            .put(CONTENT, "Observation: " + calcResult));
                    log.info("Calculator result: {}" , calcResult);
                }

                // The LLM decided to respond to the user
                else if ("Response To Human".equalsIgnoreCase(action.trim())) {
                    log.info("Final response: {}" , actionInput);
                    finished = true;
                }
            } catch (JSONException e) {
                log.error("Error during JSON processing: {}" , e.getMessage());
                finished = true;
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error during processing: {}" , e.getMessage());
                finished = true;
            }
            return finished;
        }

        @Nullable
        static JSONArray createChatHistory() {
            // Initial chat history in JSON format
            JSONArray chatHistory = new JSONArray();
            try {
                chatHistory.put(new JSONObject()
                        .put("role", "system")
                        .put(CONTENT, "You have access to the following tools:\n" +
                                "Calculator: Useful for when you need to answer questions about math. Use MathJS code, eg: 2 + 2\n" +
                                "Response To Human: When you need to respond to the human you are talking to.\n\n" +
                                "You will receive a message from the human, then you should use a tool to answer the question. For this, you should use the following format:\n\n"
                                +
                                "Action: the action to take, should be one of [Calculator, Response To Human]\n" +
                                "Action Input: the input to the action, without quotes"));

                chatHistory.put(new JSONObject()
                        .put("role", "user")
                        .put("content", ChatHelper.USER_PROMPT));
                return chatHistory;
            } catch (JSONException e) {
                log.error("Error initializing chat history: {}" , e.getMessage());

                return null;
            }
        }
    }



    private static class ResponseReader {

        // Function to parse the action from the response
        public static String getResponseAction(String responseText) {
            String[] lines = responseText.trim().split("\n");
            return lines[0].split("Action:")[1].trim();
        }

        // Function to parse the action input from the response
        public static String getResponseActionInput(String responseText) {
            String[] lines = responseText.trim().split("\n");
            return lines[1].split("Action Input:")[1].trim();
        }

        // Function to evaluate mathematical expressions
        public static String calculator(String expression) {
            License.iConfirmNonCommercialUse("GPT Calculator");
            try {
                Expression exp = new Expression(expression);
                double result = exp.calculate();
                return String.valueOf(result);
            } catch (Exception e) {
                return "Error in calculation: " + e.getMessage();
            }
        }
    }

}
