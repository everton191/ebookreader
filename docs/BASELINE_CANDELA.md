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

Nenhum dispositivo ou emulador apareceu em `adb devices` durante esta fase.
Por isso, os itens abaixo estão pendentes e não foram inferidos a partir do
sucesso do build:

| Verificação | Estado | Observação |
| --- | --- | --- |
| Instalar APK | Pendente | Sem dispositivo ADB conectado |
| Abrir aplicativo | Pendente | Depende da instalação |
| Importar e abrir EPUB | Pendente | Exige teste manual no aparelho |
| Reproduzir com TTS | Pendente | Exige voz/modelo e teste manual |
| Startup | Não medido | Requer instrumentação no aparelho |
| RAM | Não medida | Requer processo em execução |
| Tempo Play para áudio | Não medido | Requer reprodução real |

## Erros e riscos conhecidos

- O build local precisa de JDK 17 disponível para o toolchain do
  `core-plugin-ksp`.
- A Fase 1 ainda precisa da validação funcional em hardware Android antes de
  servir como comparação completa para as fases seguintes.
- Nenhum módulo, provider, permissão ou integração foi desabilitado ou removido.

## Critério para iniciar a Fase 2

Antes de remover integrações, conectar um aparelho Android e completar a tabela
de validação com pelo menos instalação, abertura, EPUB e TTS. Depois registrar
startup, RAM e tempo Play para áudio para que a versão enxuta tenha comparação
material com o Candela original.
