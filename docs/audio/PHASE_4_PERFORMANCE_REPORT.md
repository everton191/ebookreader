# Fase 4 — relatório de performance

## Baseline

- engine/modelo: Piper Lessac low;
- carga: 2.405 ms;
- Play→áudio: 5.992 ms;
- RTF inicial: 0,11–0,31;
- buffer máximo observado: 111,58 s;
- RAM debug: 739→766 MB PSS em 60 s;
- underruns: 1 de partida;
- tela apagada: posição avançou 45,44 s em uma janela de 60 s;
- MediaSession: ativa, estado Playing;
- falhas/fallbacks: nenhum observado na janela.

## Alteração orientada pela medição

Foi adicionado back-pressure por tempo com 5/25/45 s e sinal de pressão para
tarefas secundárias. O pré-renderizador de capítulos agora pausa quando a fila
de áudio está crítica e trabalha em cadência reduzida durante a recuperação.
Não houve troca de engine nem aumento de workers.

Também foram criados, no núcleo de reprodução:

- perfis conservadores `AUTOMATIC`, `ECONOMY`, `BALANCED` e `HIGH`;
- cálculo de benchmark por voz (mediana, p95 e classificação conservadora);
- política limitada de retry/fallback, sem loop infinito.

Esses três componentes têm testes unitários, mas ainda não estão conectados às
preferências/UI nem ao fluxo de troca do motor. Portanto não são apresentados
como funcionalidade final nesta medição.

## Gate

**AINDA NÃO PASS.** O usuário removeu o aparelho antes do reteste e do teste
de uma hora/vários capítulos. Localmente passaram a compilação do módulo e os
testes de perfis/benchmark/fallback e de suspensão/retomada do governador.
Faltam: build instrumentado pós-mudança no aparelho, cache hit/seek, capítulo
seguinte, RAM longa, underrun longo, fallback real, posição persistida e
comparação antes/depois. A Fase 5 permanece bloqueada.

## Reteste parcial — voz do sistema em PT-BR

No Zenfone 8, com `português (Brasil) (offline) #1` ativo, um reteste curto
em 2026-08-21 confirmou que a síntese não é o gargalo: `MODEL_LOAD_MS=1718`,
`PLAY_TO_FIRST_AUDIO_MS=5681`, RTF entre `0,040` e `0,067` e headroom de
`17,5–35,1 s`. O foco de áudio estava concedido ao processo do Candela durante
o teste.

O mesmo reteste observou underrun do `AudioTrack` na retomada rápida e uma
tentativa de tela apagada que terminou em estado pausado; por isso estes dados
não podem aprovar o gate. A causa da pausa ainda precisa ser reproduzida de
modo determinístico, corrigida se for do player e validada em playback longo.
A Fase 5 continua bloqueada.
