# Regras de negócio

## Eleição

- a configuração corrente é a linha de maior ID;
- datas são obrigatórias para abrir e o fim deve ser posterior ao início;
- eleitores e candidatos não podem ser alterados enquanto estiver aberta;
- resultados por candidato só aparecem depois do encerramento;
- "Nova eleição" cria outra configuração e preserva votos anteriores;
- relatórios e totais consultam somente a eleição corrente.

## Fluxo presencial da cabine única

1. O mesário entra em `/mesario/login`, confere documento/crachá e assinatura.
2. No painel, digita a matrícula conferida e libera o eleitor.
3. O tablet, autenticado em `/cabine/login`, mostra que aguarda liberação.
4. O eleitor digita sua matrícula. Somente a matrícula liberada é aceita.
5. Três tentativas erradas bloqueiam a liberação; o mesário pode cancelar.
6. A sessão expira automaticamente se não for usada no prazo.
7. A cabine valida novamente eleição, eleitor, candidato e sessão ao votar.
8. A participação é marcada atomicamente e o voto anônimo é persistido.
9. A sessão é concluída e os dados do eleitor são removidos do tablet.

## Separação do voto

`participacoes_eleicao` informa apenas se um usuário votou naquela eleição.
`votos` informa apenas candidato, eleição, horário e token. Não existe chave
entre as duas tabelas. As operações fazem parte da mesma transação: falha ao
salvar o voto também desfaz a marcação de participação.

## Candidatos e eleitores

- matrícula: somente dígitos, de 1 a 20 caracteres, única;
- candidato: número entre 1 e 99, nome de 2 a 255 caracteres, número único;
- somente eleitor e candidato ativos participam do fluxo;
- planilha aceita `.xlsx`, possui limites contra ZIP bomb e rejeita macros e
  fórmulas;
- foto opcional deve ser JPEG/PNG válido e é regravada pelo servidor.
