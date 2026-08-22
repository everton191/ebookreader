# Diagnóstico Gemma

`GemmaDiagnostics` começa sem medições. Após uma carga ou inferência real ele
registra estado, tempo de carga, tempo de análise, PSS do processo, contagem
de análises/falhas e uma estimativa de tokens por segundo baseada no texto
processado. Não há dado pré-preenchido: a validação no Zenfone deve coletar os
valores reais durante playback + prefetch + análise.
