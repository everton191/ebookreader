# NarrationPlan

O plano e' metadado local por `fictionId/chapterId/segmentId`. O player nao
espera por ele: quando faltar, usa narrador neutro e a analise pode persistir
depois. Cada registro contem tipo, falante, emocao, intensidade, confianca,
versao da analise, versao do modelo e hash do texto.

Respostas do modelo so' sao aceitas se cumprirem o schema restrito. Saida
invalida, modelo ausente ou baixa evidencia seguem o fallback conservador.
