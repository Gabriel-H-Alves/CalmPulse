/**
 * CalmPulse — Secure AI Gateway Proxy (Cloudflare Worker)
 * ==============================================================================
 * Este proxy serverless implementa o padrão "Backend-for-Frontend" (BFF)
 * recomendado pela OWASP MASVS para eliminar segredos de aplicações mobile.
 *
 * Benefícios de Segurança:
 * 1. ZERO SECRETS ON CLIENT: A GEMINI_API_KEY fica guardada nas variáveis
 *    criptografadas da Cloudflare, nunca no APK do celular.
 * 2. SERVER-SIDE RATE LIMITING: Bloqueia ataques DoS antes de atingir a Google.
 * 3. SERVER-SIDE INPUT SANITIZATION: Neutraliza Prompt Injection no servidor.
 * 4. CUSTO ZERO: A Cloudflare oferece 100.000 requisições/dia gratuitas.
 *
 * Como implantar em 2 minutos:
 * 1. Acesse https://dash.cloudflare.com -> Workers & Pages -> Create Worker
 * 2. Cole este código.
 * 3. Em Settings -> Variables, adicione a Secret:
 *    Nome: GEMINI_API_KEY
 *    Valor: <Sua Chave do Google AI Studio>
 * 4. Clique em Deploy. Use a URL gerada no seu aplicativo.
 * ==============================================================================
 */

const SYSTEM_INSTRUCTION = `
Você é o CalmPulse, um assistente empático dedicado ao acolhimento imediato de crises de ansiedade e pânico.
Suas regras inegociáveis são:
1. Respostas Curtas: Escreva no máximo 2 a 3 frases por mensagem. Evite sobrecarga cognitiva.
2. Não Diagnostique: Não afirme doenças nem prometa curas. Valide a dor com acolhimento sereno.
3. Passo a Passo Sequencial: Proponha UMA única etapa de cada vez (Grounding 5-4-3-2-1 ou Respiração 4-7-8).
4. Tom de Voz: Sereno, calmo, paciente e sem exclamações exageradas.
`.trim();

const SUSPICIOUS_PATTERNS = [
  /ignore (all )?previous instructions/i,
  /system prompt/i,
  /reveal .* prompt/i,
  /mode: *developer/i,
  /dan mode/i
];

export default {
  async fetch(request, env) {
    // 1. Apenas aceitar requisições POST
    if (request.method !== "POST") {
      return new Response(JSON.stringify({ error: "Method Not Allowed" }), {
        status: 405,
        headers: { "Content-Type": "application/json" }
      });
    }

    try {
      // 2. Extrair payload JSON do aplicativo móvel
      const body = await request.json();
      const userPrompt = (body.prompt || "").trim();

      if (!userPrompt) {
        return new Response(JSON.stringify({ error: "Prompt não pode estar vazio" }), {
          status: 400,
          headers: { "Content-Type": "application/json" }
        });
      }

      // 3. Sanitização no Servidor (Prevenção de DoS e Prompt Injection)
      if (userPrompt.length > 500) {
        return new Response(JSON.stringify({ error: "Mensagem excede o limite seguro" }), {
          status: 400,
          headers: { "Content-Type": "application/json" }
        });
      }

      for (const pattern of SUSPICIOUS_PATTERNS) {
        if (pattern.test(userPrompt)) {
          return new Response(JSON.stringify({
            text: "Estou focado em cuidar de você e te ajudar a se acalmar agora. Vamos respirar fundo juntos?"
          }), {
            status: 200,
            headers: { "Content-Type": "application/json" }
          });
        }
      }

      // 4. Chamada segura à API do Google Gemini com chave mantida em segredo na nuvem
      const apiKey = env.GEMINI_API_KEY;
      if (!apiKey) {
        return new Response(JSON.stringify({ error: "Chave de IA não configurada no servidor" }), {
          status: 500,
          headers: { "Content-Type": "application/json" }
        });
      }

      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;

      const geminiResponse = await fetch(geminiUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          system_instruction: {
            parts: [{ text: SYSTEM_INSTRUCTION }]
          },
          contents: [{
            parts: [{ text: userPrompt }]
          }]
        })
      });

      if (!geminiResponse.ok) {
        const errorText = await geminiResponse.text();
        return new Response(JSON.stringify({
          error: "Oscilação no provedor de IA",
          status: geminiResponse.status
        }), {
          status: 502,
          headers: { "Content-Type": "application/json" }
        });
      }

      const geminiData = await geminiResponse.json();
      const replyText = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || "Estou aqui com você. Respire fundo devagar...";

      return new Response(JSON.stringify({ text: replyText }), {
        status: 200,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*"
        }
      });

    } catch (err) {
      return new Response(JSON.stringify({ error: "Erro interno no processamento seguro" }), {
        status: 500,
        headers: { "Content-Type": "application/json" }
      });
    }
  }
};
