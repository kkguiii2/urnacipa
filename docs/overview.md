# Visão geral

## Problema e objetivo

O sistema informatiza uma eleição da CIPA. Ele reúne o cadastro dos participantes,
o controle do período eleitoral, a identificação do eleitor, a escolha de um
candidato e a apuração administrativa.

O objetivo observável é permitir uma votação presencial em cabine única, com
autorização independente do mesário e administração protegida.

## Público e atores

| Ator | Evidência de uso |
| --- | --- |
| Eleitor | informa a matrícula, confirma a identidade e registra um voto |
| Mesário | confere a pessoa e libera uma matrícula para a cabine |
| Dispositivo da cabine | exige credencial própria e executa a urna |
| Administrador ativo | gerencia eleitores, candidatos, eleição e relatórios |
| Agendador interno | verifica o fim da eleição a cada 60 segundos |
| PostgreSQL | persiste a configuração, usuários, candidatos, votos e admins |
| Servidor SMTP | recebe o relatório quando há destinatário configurado |

Quantidade prevista de usuários, volume esperado, requisitos legais, papéis
organizacionais e ambiente oficial de hospedagem: **Esta informação não pôde ser
confirmada no estado atual do projeto.**

## Funcionalidades e fluxo geral

1. Um administrador autenticado cadastra ou importa eleitores.
2. Cadastra candidatos, opcionalmente com foto.
3. Define início e fim e abre a eleição.
4. O mesário confere o eleitor e libera sua matrícula.
5. A cabine aceita somente a matrícula liberada dentro do prazo.
6. A urna lista candidatos ativos e recebe a escolha.
7. O serviço valida eleição, eleitor, candidato e sessão da cabine.
8. A participação é marcada atomicamente e o voto é salvo sem referência ao eleitor.
9. O administrador acompanha a participação; o ranking só abre após encerrar.
10. O encerramento manual ou agendado fecha a eleição e tenta enviar o relatório.

## Escopo confirmado

- uma eleição representada pela configuração mais recente;
- papéis separados `ROLE_ADMIN`, `ROLE_MESARIO` e `ROLE_CABINE`;
- candidatos numerados;
- eleitor autorizado presencialmente e identificado pela matrícula liberada;
- voto armazenado com eleição, candidato, token UUID e data/hora;
- relatório Excel e resumo textual;
- interface em português, renderizada com Thymeleaf.

## Limitações observadas

- o voto não contém FK JPA nem referência ao eleitor;
- não há paginação ou filtros nas listas;
- não há edição de eleitor ou candidato; somente inclusão, exclusão e, para
  candidato, ativação/desativação;
- uma nova eleição precisa ser criada explicitamente após encerrar a anterior;
- não existe exportação CSV ou PDF efetiva; o e-mail recebe PDF como `null`;
- não há auditoria administrativa, recuperação de senha ou troca de senha pela
  interface;
- não há documentação de acessibilidade formal nem testes end-to-end;
- as fotos ficam no sistema de arquivos local.

## Fora do escopo do estado atual

“Chamados”, prestadores, equipamentos e os fluxos de abertura, edição, conclusão
e reabertura de chamado citados no briefing não existem neste repositório. Eles
não são regras deste sistema de votação.
