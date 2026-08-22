# Resource Governor

`PlaybackResourceGovernor` publica um sinal único para tarefas secundárias:

- menos de 5 s: `SUSPENDED`;
- entre 5 e 25 s: `THROTTLED`;
- 25 s ou mais: `ALLOWED`.

Playback e a síntese do segmento atual nunca são suspensos. O pré-renderizador
de capítulos e os downloads de modelos de voz observam o sinal: aguardam
enquanto `SUSPENDED` e aplicam uma pausa curta por unidade de trabalho enquanto
`THROTTLED`. O download preserva o arquivo `.part` durante a espera. A futura
análise Gemma ainda deverá observar o mesmo fluxo. O estado volta a `ALLOWED`
quando o pipeline fecha. A integração Gemma não será feita antes do gate PASS.

O fluxo reativo e as transições de 5/25 s têm testes unitários. A eficácia
térmica e a ausência de competição perceptível continuam pendentes de reteste
no aparelho.
