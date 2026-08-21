# Matriz de redução de módulos do Candela

Baseline auditado: commit `2422ccf1`, 48 módulos Gradle, APK release de
192.131.699 bytes. O impacto por módulo não é diretamente aditivo porque R8
remove código inalcançável e dependências são compartilhadas. `Baixo`, `Médio`
e `Alto` abaixo são estimativas relativas baseadas em classes, recursos,
dependências e bibliotecas nativas observadas no grafo real.

| Módulo | Responsabilidade | Dependências relevantes | APK impact | Decisão |
|---|---|---|---:|---|
| app | Application, DI, navegação, persistência de settings | core e sources | Alto | KEEP |
| feature | Compose UI, biblioteca, reader, settings, vozes | core, rss, ocr, epub-writer | Alto | KEEP |
| core-data | Room, modelos, repositórios, contratos de source | AndroidX/Room | Alto | KEEP |
| core-playback | Media3, MediaSession, AudioTrack, TTS, cache e prefetch | core-data, Azure, audiobook-writer | Alto | KEEP |
| core-ui | tema e componentes compartilhados | core-playback | Médio | KEEP |
| core-llm | contratos e providers de LLM | core-data | Médio | KEEP_TEMPORARILY |
| core-sync | sincronização InstantDB | core-data | Médio | INVESTIGATE |
| core-plugin-ksp | geração dos bindings de sources/vozes | Kotlin/KSP | Build only | KEEP |
| core-source-testkit | contratos de teste de sources | core-data | Test only | KEEP |
| core-voice-testkit | contratos de teste de engines | core-playback | Test only | KEEP |
| baselineprofile | geração de Baseline Profiles | app | Test only | KEEP |
| wear | companion Wear OS | core-playback/core-ui | Médio | KEEP_TEMPORARILY |
| source-epub | importação e leitura EPUB local | core-data | Médio | KEEP |
| source-pdf | importação e leitura PDF local | core-data/PdfBox no app | Alto | KEEP |
| source-google-drive | seleção/importação pelo Drive | core-data/Google auth | Médio | KEEP |
| source-ocr | OCR local de documento impresso | core-data/ML Kit | Alto | KEEP_TEMPORARILY |
| source-handbook | manual offline narrável | core-data/assets | Baixo | KEEP_TEMPORARILY |
| source-epub-writer | exportador EPUB | core-data | Baixo | KEEP_TEMPORARILY |
| source-audiobook-writer | contratos de exportação de audiobook | core-playback | Baixo | KEEP_TEMPORARILY |
| source-azure | TTS Azure cloud | core-data/core-playback/app DI | Médio | DISABLE |
| source-royalroad | ficção web Royal Road e autenticação | core-data/app auth | Médio | DISABLE |
| source-github | repositórios GitHub como conteúdo | core-data/app auth | Médio | DISABLE |
| source-mempalace | servidor Memory Palace | core-data/app settings | Baixo | DISABLE |
| source-rss | feeds RSS/Atom | core-data/feature | Médio | DISABLE |
| source-outline | wiki Outline | core-data/app settings | Baixo | DISABLE |
| source-gutenberg | catálogo Project Gutenberg | core-data/epub | Médio | KEEP_TEMPORARILY |
| source-ao3 | Archive of Our Own | core-data/epub/app auth | Médio | DISABLE |
| source-standard-ebooks | catálogo Standard Ebooks | core-data/epub | Médio | KEEP_TEMPORARILY |
| source-wikipedia | Wikipedia | core-data/app browse | Médio | DISABLE |
| source-wikisource | Wikisource | core-data | Baixo | REMOVE |
| source-radio | streaming de rádio | core-data/app config | Médio | DISABLE |
| source-librivox | audiobooks públicos por streaming | core-data | Médio | REMOVE |
| source-notion | páginas/bancos Notion | core-data/app settings | Médio | DISABLE |
| source-hackernews | Hacker News | core-data | Baixo | REMOVE |
| source-google-news | notícias Google | core-data/readability | Médio | REMOVE |
| source-arxiv | artigos arXiv | core-data | Baixo | REMOVE |
| source-plos | artigos PLOS | core-data | Baixo | REMOVE |
| source-primegaming | promoções Prime Gaming | core-data/app config | Baixo | DISABLE |
| source-discord | canais Discord | core-data/app settings | Médio | DISABLE |
| source-epic-free-games | promoções Epic Games | core-data | Baixo | REMOVE |
| source-telegram | canais Telegram | core-data/app settings | Médio | DISABLE |
| source-slack | canais Slack | core-data/app settings | Médio | DISABLE |
| source-matrix | salas Matrix | core-data/app settings | Médio | DISABLE |
| source-readability | extrator de artigos por URL | core-data | Médio | REMOVE |
| source-reddit | posts Reddit | core-data/app settings | Médio | DISABLE |
| source-palace | biblioteca OPDS Palace | core-data/epub/app settings | Médio | DISABLE |
| source-bookshare | biblioteca DAISY/Bookshare | core-data/app settings | Médio | DISABLE |
| source-calendar | agenda do aparelho como conteúdo | core-data/Calendar provider | Baixo | REMOVE |

## Ordem segura

1. Remover módulos-folha sem imports diretos fora do próprio módulo.
2. Compilar e validar o player.
3. Desabilitar integrações acopladas no registro/UI.
4. Extrair ou apagar bindings/configurações somente após não haver consumidor.
5. Remover Azure por último entre os providers cloud, porque hoje
   `core-playback` e os diagnósticos dependem diretamente do módulo.

Google Drive, EPUB, PDF, biblioteca, reader, Media3, MediaSession, AudioTrack,
System TTS, engines neurais locais, VoiceManager, cache e prefetch são limites
de preservação da fase.
