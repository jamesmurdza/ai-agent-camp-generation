# Building an AI Agent from Scratch

We're going to build [this program](/index.js) step-by-step.

## 1. **Calling GPT-4**

In this section, we make a simple API call to OpenAI using the provided client and log the result:

<img src="chart-gpt.svg " />

```javascript
import OpenAI from 'openai';

// Initialize the OpenAI client
const client = new OpenAI();

// Create a chat completion
const completion = await client.chat.completions.create({
  model: "gpt-4o-mini",
  messages: [
	  { role: "user", content: "Which salsa do you prefer, verde or roja?" }
	]
});

// Get the response text from the completion
const responseText = completion.choices[0].message.content;

// Log the response from OpenAI
console.log("Response from OpenAI:", responseText);
```

This sends a query about salsa, and then prints the model's response. We can refactor this to be a little cleaner:

```javascript
...

async function callGPT(text) {
    const completion = await client.chat.completions.create({
        model: "gpt-4o-mini",
        messages: chatHistory,
    });
    return completion.choices[0].message.content;
}

const chatHistory = [
  { role: "user", content: "Which salsa do you prefer, verde or roja?" }
];

const responseText = await callGPT(chatHistory)

...
```

---

## 2. **Using a system prompt**

The next step involves adding a **system prompt** that defines the behavior of the AI. In this case, we ask the assistant to always respond in Spanish:

```javascript
...

const chatHistory = [
  {
    role: "system",
    content: "Eres un asistente de inteligencia artificial que siempre responde en español."
  },
  {
    role: "user",
    content: "Which salsa do you prefer, verde or roja?"
  }
];

...
```

In this example, the assistant will respond in Spanish no matter what language the user uses to ask a question.

---

## 3. **Having GPT-4 open a coconut**

Here, we provide a task where the assistant must choose one object out of three options to solve a problem:

<img src="chart-coconut.svg " />

```javascript
...

const chatHistory = [
  {
    role: "system",
    content: `
    You have access to several tools. You will receive a message from the human, then you should use a tool to answer the question. Use the following format:

    Action: the action to take, should be one of [Machete, Screwdriver, Hammer]
    `
  },
  {
    role: "user", 
    content: "Please open a coconut."
  }
];

...
```

The assistant needs to decide on the correct object to answer the user's question.

---

## 4. **Give GPT a second chance**

In this example, we check the action selected by the LLM and let it learn from the action's result:

<img src="chart-coconut-try-again.svg " />

```javascript
...

// Call GPT with the current chat history
const responseText = await callGPT(chatHistory);
// Add the response to the agent's memory
chatHistory.push({ role: "assistant", content: responseText });
console.log("Response from OpenAI:", responseText);

// The LLM chose the correct tool!
if (responseText.includes("Screwdriver")) {
    console.log("Correct object chosen!"); // We are finished.
}
// The LLM chose a different tool...
else {
    // Remember that we failed
    chatHistory.push({ role: "user", content: "Nope, try again!" });
    console.log("Incorrect object chosen. Asking again...");

    // Call GPT again
    const responseText2 = await callGPT(chatHistory);
    console.log("Response from OpenAI:", responseText2);
}
```

This gives allows the assistant to try another time if it made the wrong choice.

## 5. Unlimited chances!

Now, we make GPT guess again and again until it finally gets the correct response.

<img src="chart-coconut-retries.svg " />

```javascript
...

let finished = false;

while (!finished) {
    // Call GPT with the current chat history
    const responseText = await callGPT(chatHistory);
    // Remember that we failed
    chatHistory.push({ role: "assistant", content: responseText });
    console.log("Response from OpenAI:", responseText);

    // The LLM chose the correct tool!
    if (responseText.includes("Screwdriver")) {
        // We are finished.
        console.log("Correct object chosen!");
        finished = true;
    }
    // The LLM chose a different tool...
    else {
        // Add the failure to the agent's memory
        chatHistory.push({ role: "user", content: "Nope, try again!" });
        console.log("Incorrect object chosen. Asking again...");
    }
}
```

---

## 6. **Defining a calculator**

There is no LLM involved in this code. This code takes a mathematical equation, evaluates it, and gives the result.

<img src="chart-calculator.svg " />

```javascript
import { evaluate } from 'mathjs';

function calculator(expression) {
  try {
    return evaluate(expression); // Uses mathjs to evaluate the expression
  } catch (e) {
    return `Error in calculation: ${e}`;
  }
}
```

This function safely evaluates a mathematical expression and returns the result or an error message if the calculation fails.

---

## 7. **Asking GPT to solve a math problem**

Now, we ask GPT if it wants to use the calculator.

```javascript
...

const userPrompt = "What is the square root of 98237948273498274?"

const chatHistory = [
  {
    role: "system",
    content: `
    You have access to the following tools:
    Calculator: Useful for when you need to answer questions about math. Use MathJS code, eg: 2 + 2
    Response To Human: When you need to respond to the human you are talking to.

    You will receive a message from the human, then you should use a tool to answer the question. For this, you should use the following format:

    Action: the action to take, should be one of [Calculator, Response To Human]
    Action Input: the input to the action, without quotes
    `
  },
  {
    role: "user", 
    content: userPrompt
  }
];
```

## 8. Picking a tool with input

We can use this code to check what GPT decided to do.

<img src="chart-parser.svg " />

```javascript
...

// Function to parse the action and action input
function parseAgentResponse(text) {
    const lines = text.trim().split('\n');
    return {
      action: lines[0].split('Action:')[1].trim(),
      actionInput: lines[1].split('Action Input:')[1].trim()
    };
}
  
const textResponse = callGPT(chatHistory); // Call GPT with the current chat history
const parsedResponse = parseAgentResponse(textResponse); // Parse the agent's response
console.log(parsedResponse)
```

## 9. An AI assistant with tool use

Finally, we've empowered GPT with a tool.

<img src="chart-tool-use.svg " />

```javascript
...

let finished = false;

// Loop until the task is finished
while (!finished) {
  // Call GPT with the current chat history
  const responseText = await callGPT(chatHistory);
  console.log("Agent response:", responseText);
  chatHistory.push({ role: "assistant", content: responseText });
  
  // Parse the agent's response to extract action and action input
  const { action, actionInput } = parseAgentResponse(responseText);

  // Check if the action is to use the calculator
  if (action == "Calculator") {
      // Calculate the result based on the action input
      const calcResult = calculator(actionInput);
      // Add the observation to the chat history as a user's message
      chatHistory.push({ role: "user", content: `Observation: ${calcResult}` });
      console.log("Calculator result:", calcResult);
  }
  
  // Check if the action is to respond to a human
  else if (action == "Response To Human") {
      console.log("Final response:", actionInput);
      finished = true; // Stop because we are finished.
  }
}
```

After each tool use, GPT can decide whether to continue or not. We can give it as many tools as we want!
