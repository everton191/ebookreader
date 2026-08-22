# Prefetch adaptativo por tempo

O baseline atingiu 111,58 s prontos porque a fila era limitada somente por
chunks; uma frase longa pode representar dezenas de segundos.

`AdaptivePrefetchController` define:

- crítico: 5 s;
- alvo adaptativo: 20–35 s (25 s no caso neutro);
- máximo rígido: 45 s.

O alvo leva em conta RTF observado, duração média das frases, PSS do processo,
profundidade da fila, engine e preset. O produtor reserva o tempo estimado antes
de iniciar cada geração. Assim, dois ou três workers não conseguem atravessar o
limite simultaneamente só porque ainda não publicaram seus PCM. A reserva é
trocada pela duração real ao entrar na fila ou liberada em falha/cancelamento.

O produtor aguarda quando a soma de áudio pronto e reservado atingiu o alvo. O
máximo de 45 s permanece fixo; apenas um segmento indivisível que sozinho seja
maior pode excedê-lo. Velocidade, sample rate e duração real do PCM entram no
headroom.

O controle preserva a fila limitada existente e funciona tanto no produtor
serial quanto nos workers paralelos.
