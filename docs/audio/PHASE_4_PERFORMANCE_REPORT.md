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
tarefas secundárias. Não houve troca de engine nem aumento de workers.

## Gate

**AINDA NÃO PASS.** O usuário removeu o aparelho antes do reteste e do teste
de uma hora/vários capítulos. Faltam: build instrumentado pós-mudança, cache
hit/seek, capítulo seguinte, RAM longa, underrun longo, fallback real, posição
persistida e comparação antes/depois. A Fase 5 permanece bloqueada.
