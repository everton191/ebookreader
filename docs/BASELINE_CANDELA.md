# Baseline do Candela original

Data do registro: 21 de agosto de 2026 (America/Fortaleza)

## Origem

- Repositório upstream: `https://github.com/techempower-org/candela`
- Branch upstream: `main`
- Commit original: `1b7719d822902ad1bf5c2a6f3585da26b5f4edbf`
- Commit original (assunto): `feat: live-follow the playing chapter in the chapter list (#1676) (#1699)`
- Código funcional alterado nesta fase: não
- Licença preservada: GPL-3.0 (`LICENSE` original)

## Ambiente de build

- Sistema: Windows 11 x64
- Gradle Wrapper: 9.6.1
- Android SDK: 37
- Build Tools: 37.0.0
- JDK usado pelo Gradle: Microsoft OpenJDK 17.0.20
- SDK local: `C:\Users\PAESS\AppData\Local\Android\Sdk`
- Comando: `.\gradlew.bat :app:assembleRelease --console=plain`

O primeiro diagnóstico executado com JDK 21 identificou que o módulo
`core-plugin-ksp` exige explicitamente um toolchain Java 17. Após instalar e
selecionar o Microsoft OpenJDK 17, o mesmo build original foi concluído sem
alterar o projeto.

## Resultado do build

- Estado: aprovado
- Variante: `release`
- Aplicativo: `org.techempower.candela`
- Version name: `1.13.1`
- Version code: `272`
- Min SDK: 26
- Target SDK: 36
- APK: `app/build/outputs/apk/release/candela-v1.13.1.apk`
- Tamanho: 192.131.699 bytes (aproximadamente 183,2 MiB)
- SHA-256: `2359BCC8370C28B819DCE4C2822A7EEA027EA9EDDFDD086C6F2350990AA50D0D`
- Duração observada da compilação inicial: aproximadamente 14 minutos

O APK é um artefato de build e permanece ignorado pelo Git. Ele pode ser
reproduzido localmente com o comando registrado acima.

## Validação no aparelho

Validação executada em um ASUS Zenfone 8 (`ASUS_I005DA`), serial ADB
`RBAISCBR000F2X2`.

| Verificação | Estado | Observação |
| --- | --- | --- |
| Instalar APK | Aprovado | `adb install -r` retornou `Success` |
| Abrir aplicativo | Aprovado | `MainActivity` abriu e onboarding chegou à Biblioteca |
| Importar e abrir EPUB | Aprovado | EPUB público do Project Gutenberg importado; título e 9 capítulos detectados |
| Reproduzir com TTS | Aprovado com ressalva | System TTS offline reproduziu o capítulo 3 via Media3/AudioTrack |
| Startup frio | 1.100 ms | Medido por `am start -W -S` (`TotalTime`) |
| RAM após onboarding | 124.868 KiB PSS | Aproximadamente 121,9 MiB |
| RAM durante reprodução | 143.625 KiB PSS | Aproximadamente 140,3 MiB |
| Tempo AudioTrack para áudio | 1.279 ms | Criação às 12:26:37.842; estado `started` às 12:26:39.121 |
| MediaSession | Aprovado | Sessão ativa, estado `PLAYING`, metadados do livro e capítulo 3 |
| Tela apagada | Aprovado | Posição avançou de 36.781 ms para 51.826 ms durante 12 s em `Dozing` |

Livro usado: *The Adventures of Sherlock Holmes*, de Arthur Conan Doyle,
eBook 1661 do Project Gutenberg. O EPUB de teste ficou apenas no diretório de
build e no armazenamento de Downloads do aparelho; não foi adicionado ao Git.

## Erros e riscos conhecidos

- O build local precisa de JDK 17 disponível para o toolchain do
  `core-plugin-ksp`.
- A voz neural Lessac selecionada no onboarding não ficou pronta para uso e o
  primeiro capítulo terminou em `Couldn't load this chapter`. A reprodução só
  foi aprovada após ativar manualmente uma voz System TTS offline e abrir o
  capítulo 3. Esse comportamento precisa permanecer registrado para comparação.
- O log do primeiro carregamento registrou `NumberFormatException` para
  `HIGH` e `MalformedURLException: no protocol`; não houve crash fatal, mas
  esses eventos são riscos do baseline original.
- Nenhum módulo, provider, permissão ou integração foi desabilitado ou removido.

## Critério para iniciar a Fase 2

A Fase 1 agora possui comparação material de instalação, abertura, EPUB, TTS,
startup, RAM, MediaSession e reprodução com tela apagada. Antes da Fase 2, usar
este registro como limite de regressão e investigar separadamente o onboarding
da voz Lessac, sem misturar a correção com a remoção gradual de integrações.
