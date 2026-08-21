# Cache de áudio

O cache Candela foi preservado. Ele possui PCM/index/manifesto em disco, quota
configurável, eviction, lease de escritor único, invalidação de parcial e
replay por `CacheFileSource`.

A chave atual inclui capítulo, voz, velocidade, pitch, versão do chunker e hash
do dicionário de pronúncia. Engine/modelo são representados pelo `voiceId` na
base atual; estilo e preset ainda não alteram áudio na Lessac selecionada.
Quando esses parâmetros passarem a alterar síntese, deverão ser adicionados à
chave antes de habilitar o recurso correspondente.

O primeiro play medido foi miss. A validação de hit em seek/reabertura e as
estatísticas de uma sessão longa permanecem no gate do dispositivo.
