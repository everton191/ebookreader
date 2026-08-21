# Relatório da Fase 3 — UI enxuta e PT-BR

## Entregue

- navegação reduzida a Biblioteca, Downloads e Configurações;
- Biblioteca local com importação pelo seletor Android;
- Settings Hub focado em Aparência, Leitura, Voz e reprodução, Vozes,
  Downloads, status de IA local, Avançado e Sobre;
- 152 recursos PT-BR nos caminhos principais;
- download Lessac com `.part`, fsync, tamanho e instalação atômica;
- logs de download, arquivo, hash e carga da Lessac;
- player/mini-player preservando um único controlador.

Nenhum componente Gemma foi adicionado ou ativado.

## Validação executada

- `:core-playback:compileReleaseKotlin`: aprovado.
- `:feature:compileReleaseKotlin`: aprovado.
- `:app:assembleRelease`: aprovado.
- APK `candela-v1.13.1.apk`: 191.991.371 bytes.
- `adb install -r`: aprovado no Zenfone 8 `RBAISCBR000F2X2`.
- Teste unitário direcionado: Robolectric rejeitou o SDK 37 em
  `DefaultSdkProvider`; não houve falha de asserção do código alterado.

## Pendente no aparelho

O aparelho bloqueou por autenticação após a instalação. Restam onboarding
limpo da Lessac, EPUB novo, três partidas frias, RAM, troca de voz, seek,
velocidade, capítulo, retomada, tela bloqueada, MediaSession e inspeção PT-BR.

## Riscos e dívida técnica

- O catálogo não fornece hash esperado da Lessac.
- Strings literais continuam em telas avançadas/legadas ocultas.
- Rotas antigas foram preservadas internamente.
- A UI de vozes manteve a organização Candela existente; a mudança foi de
  confiabilidade e tradução, não uma reescrita do catálogo.
