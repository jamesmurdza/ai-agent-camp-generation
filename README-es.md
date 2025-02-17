# Construyendo un Agente de IA desde Cero

Vamos a construir [este programa](/index.js) paso a paso.

## 1. **Llamando a GPT-4**

En esta sección, hacemos una llamada simple a la API de OpenAI usando el cliente proporcionado y registramos el resultado:

<img src="chart-gpt.svg " />

```javascript
import OpenAI from 'openai';

// Inicializar el cliente de OpenAI
const client = new OpenAI();

// Crear una completación de chat
const completion = await client.chat.completions.create({
  model: "gpt-4o-mini",
  messages: [
	  { role: "user", content: "Which salsa do you prefer, verde or roja?" }
	]
});

// Obtener el texto de respuesta de la completación
const responseText = completion.choices[0].message.content;

// Registrar la respuesta de OpenAI
console.log("Response from OpenAI:", responseText);
```

Esto envía una consulta sobre salsa y luego imprime la respuesta del modelo. Podemos refactorizar esto para que sea un poco más limpio:

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

## 2. **Usando un prompt de sistema**

El siguiente paso implica agregar un **prompt de sistema** que define el comportamiento de la IA. En este caso, le pedimos al asistente que siempre responda en español:

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

En este ejemplo, el asistente responderá en español sin importar el idioma que use el usuario para hacer una pregunta.

---

## 3. **Haciendo que GPT-4 abra un coco**

Aquí, proporcionamos una tarea donde el asistente debe elegir un objeto entre tres opciones para resolver un problema:

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

El asistente necesita decidir sobre el objeto correcto para responder la pregunta del usuario.

---

## 4. **Darle a GPT una segunda oportunidad**

En este ejemplo, verificamos la acción seleccionada por el LLM y le permitimos aprender del resultado de la acción:

<img src="chart-coconut-try-again.svg " />

```javascript
...

// Llamar a GPT con el historial de chat actual
const responseText = await callGPT(chatHistory);
// Agregar la respuesta a la memoria del agente
chatHistory.push({ role: "assistant", content: responseText });
console.log("Response from OpenAI:", responseText);

// ¡El LLM eligió la herramienta correcta!
if (responseText.includes("Screwdriver")) {
    console.log("Correct object chosen!"); // Hemos terminado.
}
// El LLM eligió una herramienta diferente...
else {
    // Recordar que fallamos
    chatHistory.push({ role: "user", content: "Nope, try again!" });
    console.log("Incorrect object chosen. Asking again...");

    // Llamar a GPT de nuevo
    const responseText2 = await callGPT(chatHistory);
    console.log("Response from OpenAI:", responseText2);
}
```

Esto permite que el asistente intente otra vez si tomó la decisión incorrecta.

## 5. ¡Oportunidades ilimitadas!

Ahora, hacemos que GPT adivine una y otra vez hasta que finalmente obtenga la respuesta correcta.

<img src="chart-coconut-retries.svg " />

```javascript
...

let finished = false;

while (!finished) {
    // Llamar a GPT con el historial de chat actual
    const responseText = await callGPT(chatHistory);
    // Recordar que fallamos
    chatHistory.push({ role: "assistant", content: responseText });
    console.log("Response from OpenAI:", responseText);

    // ¡El LLM eligió la herramienta correcta!
    if (responseText.includes("Screwdriver")) {
        // Hemos terminado.
        console.log("Correct object chosen!");
        finished = true;
    }
    // El LLM eligió una herramienta diferente...
    else {
        // Agregar el fallo a la memoria del agente
        chatHistory.push({ role: "user", content: "Nope, try again!" });
        console.log("Incorrect object chosen. Asking again...");
    }
}
```

---

## 6. **Definiendo una calculadora**

No hay LLM involucrado en este código. Este código toma una ecuación matemática, la evalúa y proporciona el resultado.

<img src="chart-calculator.svg " />

```javascript
import { evaluate } from 'mathjs';

function calculator(expression) {
  try {
    return evaluate(expression); // Usa mathjs para evaluar la expresión
  } catch (e) {
    return `Error in calculation: ${e}`;
  }
}
```

Esta función evalúa de manera segura una expresión matemática y devuelve el resultado o un mensaje de error si el cálculo falla.

---

## 7. **Pidiendo a GPT que resuelva un problema matemático**

Ahora, le preguntamos a GPT si quiere usar la calculadora.

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

## 8. Eligiendo una herramienta con entrada

Podemos usar este código para verificar qué decidió hacer GPT.

<img src="chart-parser.svg " />

```javascript
...

// Función para analizar la acción y la entrada de la acción
function parseAgentResponse(text) {
    const lines = text.trim().split('\n');
    return {
      action: lines[0].split('Action:')[1].trim(),
      actionInput: lines[1].split('Action Input:')[1].trim()
    };
}
  
const textResponse = callGPT(chatHistory); // Llamar a GPT con el historial de chat actual
const parsedResponse = parseAgentResponse(textResponse); // Analizar la respuesta del agente
console.log(parsedResponse)
```

## 9. Un asistente de IA con uso de herramientas

Finalmente, hemos dotado a GPT con una herramienta.

<img src="chart-tool-use.svg " />

```javascript
...

let finished = false;

// Bucle hasta que la tarea esté terminada
while (!finished) {
  // Llamar a GPT con el historial de chat actual
  const responseText = await callGPT(chatHistory);
  console.log("Agent response:", responseText);
  chatHistory.push({ role: "assistant", content: responseText });
  
  // Analizar la respuesta del agente para extraer la acción y la entrada de la acción
  const { action, actionInput } = parseAgentResponse(responseText);

  // Verificar si la acción es usar la calculadora
  if (action == "Calculator") {
      // Calcular el resultado basado en la entrada de la acción
      const calcResult = calculator(actionInput);
      // Agregar la observación al historial del chat como mensaje del usuario
      chatHistory.push({ role: "user", content: `Observation: ${calcResult}` });
      console.log("Calculator result:", calcResult);
  }
  
  // Verificar si la acción es responder al humano
  else if (action == "Response To Human") {
      console.log("Final response:", actionInput);
      finished = true; // Parar porque hemos terminado.
  }
}
```

¡Después de cada uso de herramienta, GPT puede decidir si continuar o no. Podemos darle tantas herramientas como queramos!
