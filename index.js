const OpenAI = require("openai"); // Import the OpenAI library for AI functionality
const mathjs = require("mathjs"); // Import mathjs for mathematical evaluations
const readline = require("readline"); // For user input in the terminal

// Initialize a new OpenAI client
const client = new OpenAI();

// Initialize readline interface for user input
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
});

// Call the GPT model for text completion
async function callGPT(text) {
    const completion = await client.chat.completions.create({
        model: "gpt-4o-mini",
        messages: text,
    });
    return completion.choices[0].message.content;
}

// Initial chat history, before we start the agent
const chatHistory = [
    {
        role: "system",
        content: `
    You have access to the following tools:
    Calculator: Useful for when you need to answer questions about math. Use MathJS code, eg: 2 + 2
    Response To Human: When you need to respond to the human you are talking to.

    You will receive a message from the human, then you should use a tool to answer the question. For this, you should use the following format:

    Action: the action to take, should be one of [Calculator, Response To Human]
    Action Input: the input to the action (do not use quotes)
    `,
    },
];

// Extract the action and action input from the LLM's response
function parseAgentResponse(text) {
    const lines = text.trim().split("\n");
    return {
        action: lines[0].split("Action:")[1].trim(),
        actionInput: lines[1].split("Action Input:")[1].trim(),
    };
}

// Function to evaluate mathematical expressions
function calculator(expression) {
    try {
        return mathjs.evaluate(expression); // Uses mathjs to evaluate the expression
    } catch (e) {
        return `Error in calculation: ${e}`; // Return error message if calculation fails
    }
}

// Function to prompt the user for input and start the process
async function start() {
    while (true) {
        // Ask the user for a prompt dynamically
        const userPrompt = await new Promise((resolve) => {
            rl.question("User: ", resolve);
        });

        let finished = false;

        // Loop until the LLM has decided it's finished
        while (!finished) {
            // Add the user's prompt to the chat history
            chatHistory.push({
                role: "user",
                content: userPrompt,
            });

            // Call GPT and add the response to memory
            const responseText = await callGPT(chatHistory);
            console.log(responseText);
            chatHistory.push({ role: "assistant", content: responseText });

            // Parse the response for the resulting action
            const { action, actionInput } = parseAgentResponse(responseText);

            // The LLM decided to use the calculator.
            if (action.trim() === "Calculator") {
                const calcResult = calculator(actionInput);
                chatHistory.push({
                    role: "user",
                    content: `Observation: ${calcResult}`,
                });
                console.log("Calculator result:", calcResult);
            }

            // The LLM decided to respond to the user.
            else if (action.trim() === "Response To Human") {
                console.log("Final response:", actionInput); // Log the final response
                finished = true;
            }
        }
    }
}

// Start the process
start();
