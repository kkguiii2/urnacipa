# Regras de negócio

## Eleição

- a configuração corrente é a linha de maior `id`;
- na ausência de configuração, o sistema cria uma com status `FECHADA`;
- abrir define apenas `status=ABERTA`;
- encerrar define apenas `status=FECHADA`;
- o período é válido quando o status está aberto, o momento atual não é anterior
  ao início e não é posterior ao fim;
- início ou fim nulos tornam aquele limite aberto no método de domínio, embora o
  formulário administrativo exija os dois;
- o fuso padrão da JVM é alterado para `America/Manaus`;
- a cada 60 segundos, o scheduler fecha uma eleição aberta cujo fim já passou e
  tenta gerar/enviar relatório.

Não há validação explícita garantindo `dataFim > dataInicio`.

## Identificação e voto

1. A matrícula deve conter apenas dígitos.
2. O usuário deve existir, estar ativo e ainda não ter votado.
3. Matrícula e nome são guardados na sessão.
4. Após confirmação, a urna apresenta candidatos ativos ordenados por número.
5. No envio, o sistema valida novamente eleição, usuário e candidato.
6. O voto recebe `candidatoId`, token UUID e horário.
7. O usuário é marcado com `votou=true`.
8. A sessão é invalidada após o sucesso.

O voto não guarda a matrícula nem o ID do usuário. Esse desenho separa o registro
do voto do cadastro do eleitor, mas a intenção histórica de anonimato:
**Esta informação não pôde ser confirmada no estado atual do projeto.**

O bloqueio de voto repetido depende de `Usuario.votou`. Não há constraint única
relacionando voto a eleitor nem lock de linha; concorrência simultânea para a
mesma matrícula não é coberta por teste.

## Reabertura

Não existe operação de reabertura com semântica própria. O endpoint de abertura
pode definir novamente `ABERTA`, mas votos anteriores e flags `votou` permanecem.
Portanto, isso não inicia uma nova eleição.

## Eleitores

- cadastro manual exige matrícula numérica e não cadastrada;
- novo eleitor recebe `ativo=true` e `votou=false`;
- a lista exibe todos os eleitores;
- exclusão é física por ID;
- não há endpoint de edição, ativação/desativação ou reset de voto.

### Importação Excel

- aceita somente nome terminado em `.xlsx` e pacote ZIP/OOXML válido;
- rejeita arquivo vazio, corrompido, macros, fórmulas e limites excedidos;
- lê somente a primeira aba;
- cabeçalhos obrigatórios: `matricula` e `nome`, sem diferenciação de caixa e
  com espaços externos removidos;
- a ordem das colunas é livre e cabeçalhos duplicados são rejeitados;
- linhas totalmente vazias são ignoradas;
- matrícula e nome são lidos com `DataFormatter` e aparados;
- matrícula: obrigatória, somente dígitos, máximo 20 caracteres;
- matrícula numérica: inteira e até 15 dígitos, para evitar arredondamento do
  Excel; valores maiores devem ser texto;
- nome: obrigatório, máximo 255 caracteres;
- a segunda ocorrência da matrícula no mesmo arquivo é classificada como
  `DUPLICADO_NA_PLANILHA`;
- matrícula já existente não é atualizada e recebe `JA_CADASTRADO`;
- cada inserção usa transação `REQUIRES_NEW`, permitindo sucesso parcial;
- violação concorrente PostgreSQL SQLSTATE `23505` vira `JA_CADASTRADO`;
- erros estruturais interrompem antes das inserções; erros de uma linha não
  impedem outras linhas válidas;
- o resultado fica na sessão e pode produzir um XLSX de linhas não importadas.

## Candidatos

- número é único;
- o formulário limita visualmente número entre 1 e 99;
- novo candidato fica ativo;
- foto é opcional, salva com nome UUID e extensão extraída do nome original;
- candidatos podem ser ativados/desativados;
- somente ativos aparecem na urna e na busca JSON;
- exclusão é física e não remove a foto do disco pelo fluxo atual.

Não há edição de nome, número ou foto por endpoint.

## Dashboard e apuração

- total de eleitores = usuários ativos;
- total de votos = quantidade de linhas em `votos`;
- participação = votos / eleitores ativos × 100;
- ranking agrupa votos por `candidatoId` e ordena pela contagem decrescente;
- empates não têm critério secundário declarado.

Excluir/inativar eleitores depois da votação pode alterar o denominador de
participação. Excluir candidato não exclui votos e pode fazer relatórios tratarem
o candidato como ausente.

## Relatórios e exportações

- a página administrativa mostra ranking e resumo textual;
- o download gera `relatorio_cipa.xlsx`;
- o Excel contém período, totais, participação, ranking e total;
- o encerramento e a ação “Enviar” tentam anexar o Excel a um e-mail;
- destinatário vazio faz o envio ser ignorado com log de aviso;
- PDF não é gerado; `EmailService.enviarRelatorio` aceita um segundo anexo, mas
  `RelatorioService` sempre passa `null`;
- não há exportação CSV.

## Itens do briefing que não pertencem ao projeto

Abertura/edição/conclusão/reabertura de chamado, prestadores, equipamentos e
filtros não foram encontrados. **Esta informação não pôde ser confirmada no
estado atual do projeto.**
