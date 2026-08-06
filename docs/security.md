# Segurança

## Identidades separadas

O sistema possui três cadeias do Spring Security:

- `/admin/**`: conta persistida em `admins`, BCrypt e `ROLE_ADMIN`;
- `/mesario/**`: conta persistida em `mesarios`, BCrypt e `ROLE_MESARIO`;
- `/cabine/**`, `/auth/**` e `/votacao/**`: credencial do dispositivo e `ROLE_CABINE`.

Cada cadeia tem login próprio, proteção CSRF, migração do identificador da
sessão no login, uma sessão concorrente e logout somente por `POST`. As demais
rotas são negadas por padrão. Recursos estáticos e `/uploads/**` são liberados
explicitamente.

## Autorização presencial

A matrícula, sozinha, não inicia uma votação. O mesário confere a pessoa e a
assinatura e libera sua matrícula no painel. O PostgreSQL mantém uma cabine
única com lock pessimista, portanto não existem duas liberações ativas ao mesmo
tempo. A cabine aceita somente a matrícula liberada; três erros bloqueiam a
sessão. A liberação expira em 180 segundos e, após identificação, em 600
segundos por padrão.

O mesário vê quem foi liberado e o estado da cabine, mas não recebe o candidato
escolhido. O registro `votos` contém eleição, candidato, token aleatório e
horário; não contém usuário nem matrícula. A participação fica em tabela
separada e sua marcação condicional atômica impede duas requisições de votar.

## Privacidade e integridade

- logs de voto não incluem matrícula, usuário, candidato ou token;
- a tela de sucesso não revela em quem foi o voto;
- resultados por candidato, download e envio ficam bloqueados enquanto a
  eleição está aberta;
- cadastros de eleitores/candidatos ficam congelados durante a eleição;
- cada voto pertence a uma eleição e votos antigos permanecem no histórico;
- exclusão de candidato com voto é recusada;
- valores de candidato no navegador usam `textContent`, não HTML dinâmico.

## Uploads

Fotos têm limite de 5 MB, precisam decodificar como JPEG ou PNG, respeitam
limites de dimensão/pixels e são regravadas pelo servidor com nome UUID. Nome,
extensão e MIME informados pelo cliente não são confiados. O recurso é público
porque a urna precisa exibir as fotos antes de qualquer identidade de eleitor.

## Produção

Forneça `ADMIN_PASSWORD`, `MESARIO_PASSWORD` e `CABINE_PASSWORD` como segredos
fortes. Nenhuma conta inicial é criada quando sua senha está vazia. Use HTTPS,
defina `SESSION_COOKIE_SECURE=true`, proteja PostgreSQL e mantenha banco e uploads
em volumes persistentes. Política organizacional, retenção, auditoria formal e
backup continuam sendo responsabilidades operacionais.
