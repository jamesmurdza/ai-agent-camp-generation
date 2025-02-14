import OpenAI from 'openai';

// Tool implementations
import { evaluate } from 'mathjs';

function calculator(expression) {
  try {
    return evaluate(expression);
  } catch (e) {
    return `Error in calculation: ${e}`;
  }
}

function parseAgentResponse(text) {
  const lines = text.trim().split('\n');
  return {
    action: lines[0].split('Action:')[1].trim(),
    actionInput: lines[1].split('Action Input:')[1].trim()
  };
}

async function runAgent(userPrompt) {
  const client = new OpenAI();

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

  while (true) {
    const completion = await client.chat.completions.create({
      model: "gpt-4o-mini",
      messages: chatHistory
    });

    const responseText = completion.choices[0].message.content;
    console.log("Agent response:", responseText);

    const { action, actionInput } = parseAgentResponse(responseText);

    if (!action) {
      console.error("No valid action found in response");
      break;
    }

    switch (action.trim()) {
      case "Calculator":
        const calcResult = calculator(actionInput);
        console.log("Calculator result:", calcResult);
        chatHistory.push(
          { role: "assistant", content: responseText },
          { role: "user", content: `Observation: ${calcResult}` }
        );
        break;

      case "Response To Human":
        console.log("Final response:", actionInput);
        return actionInput;

      default:
        console.error("Unknown action:", action);
        return;
    }
  }
}

// Example usage
async function main() {
  try {
    const response = await runAgent("What is the square root of 144?");
    console.log("Agent completed with response:", response);
  } catch (error) {
    console.error("Error running agent:", error);
  }
}

main();
