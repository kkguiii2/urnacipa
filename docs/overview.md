# Visão geral

## Problema e objetivo

O sistema informatiza uma eleição da CIPA. Ele reúne o cadastro dos participantes,
o controle do período eleitoral, a identificação do eleitor, a escolha de um
candidato e a apuração administrativa.

O objetivo observável é permitir uma votação web com uma área pública de urna e
uma área administrativa protegida.

## Público e atores

| Ator | Evidência de uso |
| --- | --- |
| Eleitor | informa a matrícula, confirma a identidade e registra um voto |
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
4. O eleitor informa uma matrícula ativa que ainda não votou.
5. O nome é exibido para confirmação e a matrícula é guardada na sessão.
6. A urna lista candidatos ativos e recebe a escolha.
7. O serviço valida novamente eleição, eleitor e candidato.
8. Um voto anônimo em relação à tabela de usuários é salvo e o eleitor é marcado
   como tendo votado.
9. O administrador acompanha totais, ranking e relatório.
10. O encerramento manual ou agendado fecha a eleição e tenta enviar o relatório.

## Escopo confirmado

- uma eleição representada pela configuração mais recente;
- um papel administrativo (`ROLE_ADMIN`);
- candidatos numerados;
- eleitor identificado somente por matrícula;
- voto armazenado com `candidato_id`, token UUID e data/hora;
- relatório Excel e resumo textual;
- interface em português, renderizada com Thymeleaf.

## Limitações observadas

- não há suporte implementado a múltiplas eleições históricas;
- o voto não contém FK JPA nem referência ao eleitor;
- não há paginação ou filtros nas listas;
- não há edição de eleitor ou candidato; somente inclusão, exclusão e, para
  candidato, ativação/desativação;
- não há reabertura distinta: abrir novamente apenas troca o status para
  `ABERTA`, sem limpar votos ou `usuario.votou`;
- não existe exportação CSV ou PDF efetiva; o e-mail recebe PDF como `null`;
- não há auditoria administrativa, recuperação de senha ou troca de senha pela
  interface;
- não há documentação de acessibilidade formal nem testes end-to-end;
- as fotos ficam no sistema de arquivos local.

## Fora do escopo do estado atual

“Chamados”, prestadores, equipamentos e os fluxos de abertura, edição, conclusão
e reabertura de chamado citados no briefing não existem neste repositório. Eles
não são regras deste sistema de votação.
