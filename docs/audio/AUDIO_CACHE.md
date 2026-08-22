# Cache de áudio

O cache Candela foi preservado. Ele possui PCM/index/manifesto em disco, quota
configurável, eviction, lease de escritor único, invalidação de parcial e
replay por `CacheFileSource`.

A chave atual inclui `bookId`, capítulo, índice de segmentos, SHA-256 do texto,
engine, modelo, versão real do artefato, voz, velocidade, pitch, estilo, preset,
versão do chunker/normalização e hash do dicionário de pronúncia. O estilo também
identifica cadência, pausa de acessibilidade e roteamento automático de idioma.
Qualquer alteração que muda o áudio produz outro namespace e não reutiliza PCM
incompatível. Metadados antigos continuam decodificáveis, mas somente uma chave
exata é aberta como hit de reprodução.

Há dois níveis:

- RAM: `TtsRamCache`, LRU de 16 MiB para segmentos próximos. A chave combina o
  hash completo do render com o ID/range da frase. Sobrevive a seek e reconstrução
  do pipeline enquanto o processo estiver vivo; expõe entries, bytes, hits,
  misses e evictions em log `Phase4Tts`;
- disco: `PcmCache`, com quota configurável, LRU por último acesso, limpeza
  automática/manual, lease de escritor único e estatísticas por voz.

No seek para uma frase ainda no LRU, o novo pipeline coloca o PCM diretamente na
fila sem chamar a engine. Um teste de integração confirma duas construções do
pipeline com uma única chamada total ao sintetizador. Se o capítulo em disco já
está completo, `CacheFileSource` continua sendo a fonte preferida.

O primeiro play medido foi miss. O hit de RAM está coberto localmente; latência e
hit de disco em seek/reabertura ainda precisam de confirmação no Zenfone 8.
