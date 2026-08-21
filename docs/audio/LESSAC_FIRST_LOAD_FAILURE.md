# Falha no primeiro carregamento da Lessac

## Evidência do baseline

No Zenfone 8, a Lessac foi escolhida no onboarding. Ao abrir o primeiro EPUB,
o app permaneceu em `Loading voice + chapter text` e terminou em
`Couldn't load this chapter`. Uma voz System TTS offline reproduziu por
AudioTrack e MediaSession.

## Diagnóstico

O fluxo gravava diretamente em `model.onnx` e considerava a voz instalada pela
existência dos arquivos. O catálogo declara URL e tamanho, mas não SHA-256
esperado. A execução observada não prova se a causa foi arquivo parcial,
corrupção ou falha do runtime ONNX.

## Correção da Fase 3

O modelo passa a ser baixado em `model.onnx.part`, recebe `flush` e `fsync`,
tem o tamanho declarado validado e só então é movido atomicamente para o nome
final. Em erro, o `.part` é descartado e o arquivo final anterior não é
substituído.

Foram adicionados logs de download (`LESSAC_DOWNLOAD_*`, bytes esperados e
gravados), arquivo final (existência, tamanho e SHA-256) e carga do modelo
(`LESSAC_MODEL_*`, `LESSAC_LOAD_*`, exceção e fallback).

Sem hash esperado publicado, o hash registrado é diagnóstico e não prova de
autenticidade. A correção compilou; a revalidação limpa no aparelho ficou
pendente porque o dispositivo bloqueou por autenticação.
