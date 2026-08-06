# Liberação de cabine — pesquisa

## Objetivo

Impedir que um eleitor use somente a matrícula de outra pessoa na urna presencial de uma única cabine.

## Ameaças consideradas

- eleitor informa matrícula de terceiro;
- duas liberações simultâneas ou abandonadas;
- reutilização da mesma liberação;
- duas requisições concorrentes registram mais de um voto;
- mesário ou logs relacionam eleitor e candidato;
- dispositivo não autorizado tenta usar uma liberação da cabine;
- conteúdo de candidato ou upload executa código no navegador.

## Abordagem escolhida

- autenticação MVC por sessão e papel `ROLE_MESARIO`;
- mesário confere identidade/assinatura e libera uma matrícula;
- uma única sessão ativa para a cabine, com validade de três minutos e três tentativas;
- tablet usa credencial de cabine mantida em sessão e somente ele identifica o eleitor;
- voto e participação são persistidos em uma transação atômica;
- voto não contém eleitor e logs nunca registram matrícula junto com candidato;
- uploads de imagem são decodificados e regravados em formato permitido.

## Decisões de privacidade

A sessão da cabine registra a participação e o mesário responsável, mas não o candidato. A tabela de votos registra somente eleição, candidato, recibo aleatório e momento. A tela de sucesso não mostra a escolha.

