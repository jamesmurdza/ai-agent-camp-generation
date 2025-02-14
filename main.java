import org.mariuszgromada.math.mxparser.Expression;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.json.JSONArray;
import org.json.JSONObject;

public class GPTCalculator {
    private static final String OPENAI_API_KEY = "YOUR_OPENAI_API_KEY"; // Replace with your OpenAI API key

    public static void main(String[] args) {
        // User prompt for the calculator
        String userPrompt = "What is the square root of 98237948273498274?";

        // Initial chat history in JSON format
        JSONArray chatHistory = new JSONArray();
        chatHistory.put(new JSONObject().put("role", "system").put("content", "You have access to the following tools:\n" +
                "Calculator: Useful for when you need to answer questions about math. Use MathJS code, eg: 2 + 2\n" +
                "Response To Human: When you need to respond to the human you are talking to.\n\n" +
                "You will receive a message from the human, then you should use a tool to answer the question. For this, you should use the following format:\n\n" +
                "Action: the action to take, should be one of [Calculator, Response To Human]\n" +
                "Action Input: the input to the action, without quotes"));
        chatHistory.put(new JSONObject().put("role", "user").put("content", userPrompt));

        // Loop until the LLM has decided it's finished
        boolean finished = false;
        while (!finished) {
            try {
                // Call GPT and add the response to memory
                String responseText = callGPT(chatHistory);
                System.out.println("Agent response: " + responseText);
                chatHistory.put(new JSONObject().put("role", "assistant").put("content", responseText));

                // Parse the response for the resulting action
                String action = getResponseAction(responseText);
                String actionInput = getResponseActionInput(responseText);

                // The LLM decided to use the calculator
                if ("Calculator".equalsIgnoreCase(action.trim())) {
                    String calcResult = calculator(actionInput);
                    chatHistory.put(new JSONObject().put("role", "user").put("content", "Observation: " + calcResult));
                    System.out.println("Calculator result: " + calcResult);
                }

                // The LLM decided to respond to the user
                else if ("Response To Human".equalsIgnoreCase(action.trim())) {
                    System.out.println("Final response: " + actionInput);
                    finished = true;
                }
            } catch (Exception e) {
                System.err.println("Error during processing: " + e.getMessage());
                finished = true;
            }
        }
    }

    public static String callGPT(JSONArray chatHistory) {
        var requestBody = new JSONObject()
            .put("model", "gpt-4")
            .put("messages", chatHistory);

        var request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + OPENAI_API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        try {
            var response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
                
            return new JSONObject(response.body())
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
        } catch (Exception e) {
            throw new RuntimeException("Failed to call GPT API", e);
        }
    }

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
        try {
            Expression exp = new Expression(expression);
            double result = exp.calculate();
            return String.valueOf(result);
        } catch (Exception e) {
            return "Error in calculation: " + e.getMessage();
        }
    }
}
