# Prefetch adaptativo por tempo

O baseline atingiu 111,58 s prontos porque a fila era limitada somente por
chunks; uma frase longa pode representar dezenas de segundos.

`AdaptivePrefetchController` define:

- crítico: 5 s;
- alvo: 25 s;
- máximo diagnóstico: 45 s.

O produtor sintetiza enquanto o headroom está abaixo do alvo e aguarda uma
redução antes do próximo segmento. O limite máximo pode ser excedido por um
único segmento indivisível já sintetizado, mas não por acúmulo ilimitado.
Velocidade, sample rate e duração real do PCM já entram no headroom.

O controle preserva a fila limitada existente e funciona tanto no produtor
serial quanto nos workers paralelos.
