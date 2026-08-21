# Resource Governor

`PlaybackResourceGovernor` publica um sinal único para tarefas secundárias:

- menos de 5 s: `SUSPENDED`;
- entre 5 e 25 s: `THROTTLED`;
- 25 s ou mais: `ALLOWED`.

Playback e a síntese do segmento atual nunca são suspensos. Downloads e a
futura análise Gemma deverão observar esse sinal. O estado volta a `ALLOWED`
quando o pipeline fecha. A integração Gemma não será feita antes do gate PASS.
