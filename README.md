# Building an AI Agent from Scratch

This script demonstrates various concepts for interacting with the OpenAI API, managing conversation history, and processing input. Below, we break down each key part of the script:

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

```javascript
...

const responseText = await callGPT(chatHistory);
chatHistory.push({ role: "assistant", content: responseText });
console.log("Response from OpenAI:", responseText);

if (responseText.includes("Screwdriver")) {
    console.log("Correct object chosen!");
} else {
    chatHistory.push({ role: "user", content: "Nope, try again!" });
    console.log("Incorrect object chosen. Asking again...");
  
    const responseText2 = await callGPT(chatHistory);
    console.log("Response from OpenAI:", responseText2);
}
```

This gives allows the assistant to try another time if it made the wrong choice.

## 5. Unlimited chances!

<img src="chart-coconut-retries.svg " />

```javascript
...

let finished = false;

while (!finished) {
    const responseText = await callGPT(chatHistory);
    chatHistory.push({ role: "assistant", content: responseText });
    console.log("Response from OpenAI:", responseText);

    if (responseText.includes("Screwdriver")) {
        console.log("Correct object chosen!");
        finished = true;
    } else {
        chatHistory.push({ role: "user", content: "Nope, try again!" });
        console.log("Incorrect object chosen. Asking again...");
    }
}
```

---

## 6. **Defining a calculator**

There is no AI, here, and that's the point. Instead of something silly like an imaginary machete, let's give the AI assistant something useful.

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

## 7. **An AI assistant plus a calculator**

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

<img src="chart-parser.svg " />

```javascript
...

function parseAgentResponse(text) {
  const lines = text.trim().split('\n');
  return {
    action: lines[0].split('Action:')[1].trim(),
    actionInput: lines[1].split('Action Input:')[1].trim()
  };
}

const textResponse = callGPT(chatHistory);
const parsedResponse = parseAgentResponse(textResponse);
console.log(parsedResponse)
```

## 9. An AI assistant with tool use

<img src="chart-tool-use.svg " />

```javascript
...

let finished = false;

while (!finished) {
  const responseText = await callGPT(chatHistory);
  console.log("Agent response:", responseText);
  chatHistory.push({ role: "assistant", content: responseText })
  
  const { action, actionInput } = parseAgentResponse(responseText);

  if (action == "Calculator") {
      const calcResult = calculator(actionInput);
      chatHistory.push({ role: "user", content: `Observation: ${calcResult}` })
      console.log("Calculator result:", calcResult);
  }
  
  else if (action == "Response To Human") {
      console.log("Final response:", actionInput);
      finished = true;
  }
}
```

This loop will either invoke the calculator for mathematical queries or respond to the user with the assistant's chosen action.
