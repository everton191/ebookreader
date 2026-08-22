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
- benchmark silencioso de cinco frases PT-BR por voz/modelo/preset, persistido
  com mediana, p95, RAM e classificação conservadora;
- indicação na biblioteca quando uma voz apresenta risco de tempo real ou é
  mais adequada para audiobook preparado;
- preparação antecipada real da voz ao abrir o leitor, sem iniciar áudio;
- fila observável `WAITING/GENERATING/READY/PLAYING/PLAYED/FAILED`;
- alvo de prefetch 20–35 s, reservas concorrentes e máximo rígido de 45 s;
- cache RAM LRU de 16 MiB para seek/rebuild, além do cache LRU em disco;
- chave de cache com texto, engine, modelo/versão, estilo e preset;
- política limitada de retry/fallback, sem loop infinito;
- validação de tamanho e SHA-256 antes da instalação atômica de Piper;
- fallback apenas para outra voz local no mesmo idioma ou TTS do sistema
  comprovadamente offline.

O preset efetivo permanece `Automática` por padrão; os detalhes internos não são
expostos na interface enxuta. O benchmark já alimenta a indicação de capacidade
na biblioteca de vozes. A política de retry/fallback está conectada à carga do motor:
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

## Fechamento local adicional

- compilação `core-playback`, `feature` e `app`: PASS;
- testes de fila, prefetch, governor, chave/cache, benchmark e `VoiceManager`:
  PASS;
- teste de seek/rebuild em RAM: segundo pipeline reutilizou o PCM e o motor foi
  chamado uma única vez;
- downloads de modelos de voz agora também observam o `PlaybackResourceGovernor`:
  pausam antes da próxima leitura de 64 KiB se o buffer estiver crítico e cedem
  cadência durante a recuperação, preservando o arquivo `.part`;
- APK debug novo: 226.553.012 bytes, SHA-256
  `82AFFC1358D65993D24C0C0C5F8DDFD7B0D59249B4A0FCB0EBC9BC19106C4A97`.

O gate permanece **AINDA NÃO PASS**: este fechamento local não substitui o teste
de pelo menos uma hora/vários capítulos, tela apagada, capítulo seguinte,
fallback real, RAM longa, underrun longo e posição persistida no Zenfone 8. A
Fase 5 continua bloqueada.

## Teste no Zenfone 8 — Faber PT-BR e limite de segmento

No teste real de 2026-08-21 com `piper_faber_pt_BR_medium`, o modelo carregou
em 3.416 ms, o primeiro áudio foi registrado em 10.767 ms e o RTF observado
ficou entre 0,166 e 0,414. Depois do preenchimento inicial, o buffer ficou
entre aproximadamente 16,9 e 29,2 s.

O teste também encontrou uma pressão de memória que impede o PASS: segmentos
excepcionalmente longos (376 e 534 caracteres) coincidiram com o PSS crescendo
de cerca de 700 MB para 1.064 MB; `dumpsys meminfo` confirmou 818 MB no heap
nativo. O cache RAM continha apenas cerca de 7 MB, portanto ele não explica o
salto. O app foi pausado assim que a evidência apareceu.

Como correção, o `SentenceChunker` passou a aplicar de fato o limite de 240
caracteres por utterance, preferindo vírgula, pontuação de cláusula ou espaço.
A versão do chunker foi elevada a 5, invalidando PCM produzido pela divisão
anterior. Testes de chunker/chave de cache e a compilação do APK passaram; o
APK corrigido foi instalado no Zenfone 8 (SHA-256
`A3DF2F2AFA6C37F564CC5949D792DD45733091BB474EA11DB534DCC85F3442F7`).

O reteste de memória dessa correção ainda é pendente: após a instalação outra
atividade foi trazida para o primeiro plano, e nenhum toque adicional foi feito
fora do leitor. Assim, a Fase 4 permanece **AINDA NÃO PASS**.
