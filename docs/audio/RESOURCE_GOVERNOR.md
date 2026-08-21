# Resource Governor

`PlaybackResourceGovernor` publica um sinal único para tarefas secundárias:

- menos de 5 s: `SUSPENDED`;
- entre 5 e 25 s: `THROTTLED`;
- 25 s ou mais: `ALLOWED`.

Playback e a síntese do segmento atual nunca são suspensos. O pré-renderizador
de capítulos já observa o sinal: aguarda enquanto `SUSPENDED` e aplica uma
pausa curta por sentença enquanto `THROTTLED`. Downloads e a futura análise
Gemma ainda deverão observar o mesmo fluxo. O estado volta a `ALLOWED` quando
o pipeline fecha. A integração Gemma não será feita antes do gate PASS.

O fluxo reativo e as transições de 5/25 s têm testes unitários. A eficácia
térmica e a ausência de competição perceptível continuam pendentes de reteste
no aparelho.
