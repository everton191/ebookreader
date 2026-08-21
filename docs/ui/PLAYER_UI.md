# Player e mini-player

O player continua usando o `PlaybackController` global e a MediaSession
existente. O mini-player `NowPlayingDock` observa esse mesmo estado; não cria
engine, fila ou instância de reprodução paralela.

O mini-player exibe capa, livro, capítulo, progresso e reproduzir/pausar; em
tablet também oferece próximo capítulo. Tocar no corpo retorna ao leitor.
Rótulos e descrições de acessibilidade principais receberam recursos PT-BR.

Esta fase preservou a estrutura funcional do player para proteger retomada,
seek, velocidade, troca de capítulo, tela bloqueada e MediaSession. Uma
reorganização visual profunda não foi feita sem validação integral no aparelho.
