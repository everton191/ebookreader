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
- política limitada de retry/fallback, sem loop infinito;
- validação de tamanho e SHA-256 antes da instalação atômica de Piper;
- fallback apenas para outra voz local no mesmo idioma ou TTS do sistema
  comprovadamente offline.

Os perfis e o benchmark ainda não estão conectados às preferências/UI. A
política de retry/fallback, por outro lado, já está conectada à carga do motor:
cada voz tem até duas tentativas limitadas por tempo, seguida no máximo por uma
alternativa local no mesmo idioma. A escolha persistida do usuário não é
alterada pelo fallback.

## Gate

**AINDA NÃO PASS.** O APK pós-mudança foi compilado e instalado no Zenfone 8,
mas ainda faltam: audição do Faber íntegro, teste de uma hora/vários capítulos,
cache hit/seek, tela apagada, capítulo seguinte, RAM longa, underrun longo,
fallback real e posição persistida. A Fase 5 permanece bloqueada.

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

## Reteste parcial — modelos neurais PT-BR

O Dii high apresentou sotaque brasileiro aceitável para o usuário. No aparelho,
o primeiro áudio apareceu em 6.797 ms, com RTF entre 0,129 e 0,281, buffer entre
7 e 38 segundos e RAM debug entre 534 e 724 MB PSS. A voz ainda foi percebida
como abafada ou com ruído, portanto não é a escolha final aprovada.

O Kokoro/Alex foi descartado como opção padrão por sotaque inadequado, além de
ter apresentado pressão alta de memória e RTF longo de 1,245 em medições
anteriores.

Foi detectado no aparelho um arquivo Faber incompleto de 31.951.705 bytes. O
catálogo agora declara 63.201.428 bytes e SHA-256 do artefato oficial; downloads
truncados ou adulterados são recusados antes da instalação. O Faber completo já
está no aparelho e pronto para audição, mas sua qualidade ainda não foi aprovada
pelo usuário.

## Voz neural online opcional

O atalho de configurações para Azure Neural foi exposto na interface enxuta e
traduzido para PT-BR. A integração continua opcional e exige região e chave da
conta do próprio usuário; nenhuma credencial foi incorporada ao APK. O seletor
de contingência agora aceita somente vozes neurais realmente instaladas no
aparelho, evitando recursão para Azure ou IDs incompatíveis do TTS do sistema.

A tela informa a cota F0 de 500 mil caracteres neurais por mês. Compilação e
testes locais do provedor passaram, mas uma síntese online real não foi executada
por não haver credencial do usuário no projeto. Isso não conta como aprovação do
gate offline.

## Evidências da rodada de 2026-08-21

- `:app:assembleDebug` e `:source-azure:testDebugUnitTest`: PASS;
- contratos do hub compacto e da navegação: PASS;
- testes de normalização numérica, troca de voz, retry/fallback e integridade:
  PASS, incluindo a suíte completa `VoiceManagerTest`;
- APK debug: 224.860.623 bytes, SHA-256
  `B37C4715F716A76C2328D470BB351C32E1041C46CF6364B9BA682D3B98FAE7A5`;
- instalação ADB no Zenfone 8: `Success`;
- Faber e Dii no armazenamento privado do app: tamanho e SHA-256 conferidos
  contra o catálogo.

Essas evidências aprovam código, artefato, instalação e integridade. Elas não
substituem a audição humana nem os cenários longos ainda listados no gate.
