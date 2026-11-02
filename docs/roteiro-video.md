# Roteiro do vídeo: DevOps Tools & Cloud Computing (Sprint 4)

> **Roteiro de leitura individual.** O texto em citação (`>`) é para **ler em voz alta**. Tudo em **[COLCHETES]** é indicação para você e não é lido: **[TELA]** o que mostrar, **[AÇÃO]** o que clicar ou configurar, **[COMANDO]** o que digitar no terminal. A lista completa de comandos, para copiar e colar, está em [comandos-video.md](comandos-video.md).
>
> **Requisitos do enunciado:** 720p ou mais, áudio claro, sem legendas, explicação por voz, ferramentas já abertas, **nada em localhost**, todos os itens entregues explicados pelo aluno.
>
> **Duração:** como o vídeo começa da criação da infraestrutura e inclui um primeiro ciclo de CI/CD, ele fica em torno de **60 a 70 minutos com as esperas**. As esperas das Partes 2 (criação do PostgreSQL) e 6 (primeiro CI/CD) podem ser cortadas na edição. **As Partes 10 e 11 (CI e CD da alteração de código) devem ficar integrais**, com a explicação de cada etapa durante a execução: é critério de nota.

## 1. Ordem do vídeo

| Parte | O que acontece | Tempo (sem cortes) |
|---|---|---|
| 1 | Abertura, solução e diagrama | 3 min |
| 2 | Ambiente e criação da infraestrutura (`01-create-infra.sh`) | 10 a 15 min |
| 3 | Projeto no Azure DevOps e acesso do professor | 3 min |
| 4 | Código no Azure Repos e política da `master` | 3 min |
| 5 | Segredos (Variable Group) e Service Connection | 3 min |
| 6 | Pipelines por dentro e primeiro CI/CD | 12 a 15 min |
| 7 | Task e branch pelo Azure Boards | 2 min |
| 8 | Alteração no código (teste vermelho, regra, teste verde) | 3 min |
| 9 | Commit, push, Pull Request e merge | 2 min |
| 10 | CI automático, explicado durante a execução | 4 a 5 min |
| 11 | CD automático, explicado durante a execução | 5 a 7 min |
| 12 | Recursos atualizados no Portal Azure | 2 min |
| 13 | App em nuvem com a alteração | 2 min |
| 14 | CRUD completo com prova no banco | 4 min |
| 15 | Fechamento | 1 min |

## 2. Como a nota é composta e onde cada parte responde

| Critério (enunciado) | Pontos | Partes |
|---|---:|---|
| Diagrama de arquitetura + fluxo CI/CD | 20 | 1 |
| Vídeo com CI/CD | 80 | 2 a 15 |
| Descrição da solução (README) | penalidade -10 | 1 |
| Projeto Azure DevOps configurado e professor convidado (Basic) | zero de nota | 3 |
| Penalidades de pipeline (artefato, testes, disparo automático, deploy, segredos) | -15 a -30 cada | 5, 6, 10, 11 |
| Penalidade de CRUD sem prova no banco | -30 | 14 |

## 3. O que o professor disse no CP6 e como este roteiro responde

| Feedback (CP6, nota 9,0) | O que fazer agora | Partes |
|---|---|---|
| "Testes de execução do app e persistência devem ser feitos **após o deploy automático** da pipeline" | O app só é exercitado depois de um CD verde. O "antes" da alteração é o teste unitário falhando no VS Code. O CRUD completo acontece depois do CD da alteração | 8, 12, 13, 14 |
| "Faltou mostrar o **artefato publicado (JAR)**" | Abrir o run do CI, aba **Artifacts**, entrar em `drop` e mostrar `petflow-api.jar`. Mostrar o CD baixando esse mesmo artefato | 6, 10, 11 |
| "O Build é criado de forma **manual pelo terminal**? Não entendi" | Dizer em voz alta que o único build é o do CI. Os testes locais rodam pelo **painel de testes do VS Code**, nunca por `mvnw package/verify` | 8, 10 |
| "A branch é criada **sem uma tarefa, via terminal**" | Criar a **Task** no Boards e a branch pelo botão **Create a branch** da própria Task. No terminal só `git fetch` e `git checkout` | 7 |
| "Depois da alteração **não mostrou o CD**, somente o CI" | Acompanhar o `petflow-cd` disparando sozinho ao fim do CI, com os dois estágios (Deploy e SmokeTest) | 11 |
| "Verifique 06 (execução de ponta a ponta) e 07 (execução das pipelines)" | Fluxo contínuo, sem pular: alteração, commit, CI, artefato, CD, Portal, app, CRUD | 8 a 14 |

## 4. Preparação (fora do vídeo, antes de gravar)

**Estado inicial limpo (o vídeo cria tudo do zero)**
1. O Resource Group `561413-petflow-s4-devops-rg` **não existe** (se existir de um ensaio, apague com `bash scripts/99-delete-infra.sh` e espere terminar).
2. O projeto `PetFlow` **não existe** na organização (se existir de um ensaio, apague-o antes).
3. O código local **não tem pasta `.git`** e a alteração da demo **não está aplicada** (`PetRequestDTO` ainda com `@Size(max = 100, ...)`; a pasta `src/test/java/br/com/petflow/petflow_api/dto` não existe).

**Contas e credenciais (fora da câmera)**
4. `az login` ativo na conta que enxerga a assinatura Azure for Students (a do Pietro) e `.env.azure` preenchido.
5. No navegador, a mesma conta Microsoft logada no Azure DevOps, para a Service Connection enxergar a assinatura.
6. `git config --global user.name` e `user.email` configurados no WSL, e uma credencial do Azure Repos pronta (PAT do Azure DevOps com permissão de Code: Read & Write), para o primeiro `git push` não travar.
7. GitHub: repositório vazio `lgaxd/petflow-devops-sprint4` criado e um PAT com escopo `repo`, para o espelho. Os valores ficam só no Variable Group.
8. Organização do Azure DevOps com **Parallel jobs** Microsoft-hosted liberado (Organization settings → Pipelines → Parallel jobs). Sem isso as pipelines ficam em *Queued*.

**Ferramentas abertas, em abas, na ordem do roteiro**
9. VS Code (WSL) na pasta `DevOps`; terminal WSL na raiz do projeto, com `psql` instalado.
10. Portal Azure em *Resource groups*; Azure DevOps (`dev.azure.com`) na página da organização.
11. Diagrama `docs/img/diagrama-arquitetura.png` e o README renderizado.
12. Um bloco de notas com os dados do CRUD (abaixo) e com os valores do `.env.azure` **fora do enquadramento**.
13. Gravação em 720p ou mais, tela cheia, zoom do navegador em 110%, microfone testado, notificações desligadas. **Nunca abrir o `.env.azure` na tela.**

**Dados fixos do CRUD (bloco de notas)**
- Login: `maria@petflow.com` e a senha do seed (cole sem mostrar).
- Corpo do POST `/pets`: `{"name":"Demo Rex","breed":"Labrador","birthDate":"2021-05-10","weight":12.5,"tutorId":2,"speciesId":1}` (`tutorId` é o `id` devolvido pelo login da Maria; no seed é 2).
- Corpo do PUT `/pets/{id}`: `{"name":"Demo Rex Atualizado","breed":"Labrador","birthDate":"2021-05-10","weight":14.0,"tutorId":2,"speciesId":1}`.
- Corpo do POST com nome inválido: `{"name":"A","tutorId":2,"speciesId":1}`.

---

## Parte 1: Abertura, solução e diagrama (≈ 3 min)

> **Como ler esta parte:** o texto em citação (`>`) é para **ler em voz alta**. Tudo em **[COLCHETES]** é indicação para você, não é lido. Cada trecho diz o que está na tela, em que parte do vídeo aquela etapa será executada e qual comando roda (a lista completa de comandos está em [comandos-video.md](comandos-video.md)).

### 1.1 Abertura

**[TELA]** README do projeto renderizado, com o título e a descrição visíveis.
**[COMANDO]** Nenhum.

> Olá! Eu sou o Lucas Grillo Alcântara, RM 561413, da turma 2TDSPX, e este é o projeto de DevOps do nosso grupo para a Sprint 4, feito junto com o Pietro, o Pedro e o Lucca.
>
> O PetFlow é uma API de saúde preventiva para pets, com gamificação. O tutor cadastra os seus pets, registra eventos de saúde, como vacinas e consultas, e ganha pontos a cada evento realizado. Esses pontos viram cupons de desconto em clínicas parceiras.
>
> Nesta entrega, o foco não é a aplicação em si, e sim o caminho que o código percorre até a nuvem. Cada alteração na branch master é compilada, testada, empacotada e implantada no Azure de forma automática, sem nenhum passo manual. Vou mostrar isso do começo ao fim: a criação do ambiente, as pipelines, uma alteração de código e a aplicação funcionando na nuvem, com o banco de dados.

### 1.2 Tecnologias

**[TELA]** Continue no README, na tabela de tecnologias.
**[COMANDO]** Nenhum.

> A aplicação foi feita em Java 21 com Spring Boot 4. Ela roda em um Azure Web App Linux, e o banco de dados é um PostgreSQL gerenciado do Azure, o Azure Database for PostgreSQL, que guarda os dados de forma persistente e só aceita conexão segura, com SSL.
>
> A infraestrutura é criada por scripts do Azure CLI, e as pipelines são escritas em YAML no Azure DevOps. As senhas e as chaves ficam em um grupo de variáveis protegido e nunca aparecem no código.

### 1.3 Diagrama: visão geral

**[TELA]** Abra `docs/img/diagrama-arquitetura.png` em tela cheia.
**[APONTE]** Passe o mouse pelas duas pessoas do diagrama: o **Desenvolvedor** (canto superior esquerdo) e o **Usuário final** (embaixo).
**[COMANDO]** Nenhum.

> Este é o diagrama da arquitetura e do fluxo de CI/CD. Ele tem duas pessoas: o desenvolvedor, que escreve e envia o código, e o usuário final, que usa a aplicação. As etapas estão numeradas e eu vou percorrer todas, na ordem.

### 1.4 Antes da etapa 1: a infraestrutura

**[APONTE]** A caixa **Scripts Azure CLI**, dentro da área do Microsoft Azure, e a seta "cria" até o Web App.
**[ONDE ACONTECE NO VÍDEO]** Parte 2, quando eu crio o ambiente.
**[COMANDO]** `bash scripts/01-create-infra.sh`

> Antes da primeira etapa, a infraestrutura precisa existir. Ela é criada por este script do Azure CLI, que monta o grupo de recursos, o PostgreSQL, o plano e o Web App. Isso acontece uma única vez, e eu vou executar o script mais adiante, no vídeo.

### 1.5 As 12 etapas

**Etapa 1**
**[APONTE]** Do **Desenvolvedor** para o **Azure Boards**.
**[ONDE ACONTECE NO VÍDEO]** Parte 7.
**[COMANDO]** Nenhum para criar a Task (é na interface do Azure Boards). Depois da branch criada, no terminal: `git fetch` e `git checkout <nome-da-branch-da-task>`.

> Etapa 1. O desenvolvedor começa criando uma Task no Azure Boards. É a partir dessa Task que nasce a branch de trabalho, já vinculada ao item.

**Etapa 2**
**[APONTE]** Do **Azure Boards** para o **Azure Repos**.
**[ONDE ACONTECE NO VÍDEO]** Partes 8 e 9.
**[COMANDO]** Parte 8: nenhum no terminal (os testes rodam pelo painel do VS Code). Parte 9: `git add src/`, depois `git commit -m "Validar tamanho minimo do nome do pet"`, depois `git push`.

> Etapa 2. O desenvolvedor altera o código, envia a branch para o Azure Repos e abre uma Pull Request para a master. Quando a Pull Request é concluída, o merge acontece.

**Etapa 3**
**[APONTE]** Do **Azure Repos** para a **Pipeline CI**.
**[ONDE ACONTECE NO VÍDEO]** Parte 10.
**[COMANDO]** Nenhum. Não clicar em Run.

> Etapa 3. O merge na master dispara a pipeline de CI sozinha. Eu não vou clicar em Run em nenhum momento.

**Etapa 4**
**[APONTE]** O texto "mvn verify · testes JUnit" dentro da **Pipeline CI**.
**[ONDE ACONTECE NO VÍDEO]** Parte 10.
**[COMANDO]** Nenhum. Roda dentro da pipeline.

> Etapa 4. O CI executa o mvn clean verify: compila o projeto e roda todos os testes automatizados. Se algum teste falhar, a pipeline para e nada é publicado.

**Etapa 5**
**[APONTE]** A seta entre a **Pipeline CI** e a **Pipeline CD**.
**[ONDE ACONTECE NO VÍDEO]** Parte 10, na aba Artifacts do CI.
**[COMANDO]** Nenhum.

> Etapa 5. Se tudo passou, o CI publica os resultados dos testes e o artefato chamado drop, que contém o arquivo petflow-api.jar.

**Etapa 6**
**[APONTE]** A **Pipeline CD**, no topo.
**[ONDE ACONTECE NO VÍDEO]** Parte 11.
**[COMANDO]** Nenhum. Não clicar em Run.

> Etapa 6. A publicação de um artefato novo dispara a pipeline de CD, também sozinha.

**Etapa 7**
**[APONTE]** O **Variable Group**, com a seta vermelha subindo para a Pipeline CD.
**[ONDE ACONTECE NO VÍDEO]** Partes 5 (criar o grupo com os cadeados) e 11 (log com os valores mascarados).
**[COMANDO]** Nenhum.

> Etapa 7. O CD lê os segredos do Variable Group e grava como configurações do Web App. As senhas nunca aparecem no código nem nos arquivos YAML.

**Etapa 8**
**[APONTE]** A linha azul da **Pipeline CD** até o **Azure Web App**.
**[ONDE ACONTECE NO VÍDEO]** Parte 11.
**[COMANDO]** Nenhum. Roda dentro da pipeline. (Só se o deploy falhar: `az webapp log tail -g 561413-petflow-s4-devops-rg -n 561413-petflow-s4-api`.)

> Etapa 8. O CD faz o deploy do JAR no Web App com o az webapp deploy e espera o health check confirmar que a aplicação e o PostgreSQL estão no ar.

**Etapa 9**
**[APONTE]** O texto "9 Smoke test do CD", na parte de baixo da área do Azure.
**[ONDE ACONTECE NO VÍDEO]** Parte 11, no segundo estágio do CD.
**[COMANDO]** Nenhum. Roda dentro da pipeline.

> Etapa 9. Em seguida roda um smoke test na URL pública: ele faz login, cria, consulta, atualiza e remove um pet, e também confere o agendamento de eventos.

**Etapa 10**
**[APONTE]** Do **Usuário final** para o **Azure Web App**.
**[ONDE ACONTECE NO VÍDEO]** Parte 13.
**[COMANDO]** Opcional no terminal: `curl -s https://561413-petflow-s4-api.azurewebsites.net/actuator/health`. No navegador: abrir o Swagger.

> Etapa 10. O usuário final acessa a API pela URL HTTPS do Web App, usando o Swagger.

**Etapa 11**
**[APONTE]** As setas entre o **Azure Web App** e o **Azure Database for PostgreSQL**.
**[ONDE ACONTECE NO VÍDEO]** Parte 14.
**[COMANDO]** Depois de cada operação do CRUD, por exemplo: `bash scripts/sql.sh "SELECT id, name, breed, weight, tutor_id, species_id FROM pet WHERE name LIKE 'Demo%'"`.

> Etapa 11. O Web App lê e grava no PostgreSQL por JDBC com SSL. Na primeira subida, o Flyway cria as tabelas e os dados iniciais.

**Etapa 12**
**[APONTE]** A caixa **GitHub (espelho)**, à esquerda, e a seta que sai do Azure Repos.
**[ONDE ACONTECE NO VÍDEO]** Parte 15.
**[COMANDO]** `git log --oneline -3`, para comparar o hash com o último commit do GitHub.

> Etapa 12. Em paralelo, cada merge é espelhado automaticamente no GitHub, que é um dos links entregues no PDF. Vamos entregar também o link do projeto do Azure DevOps para conferência do Azure Repos.

### 1.6 Transição

**[TELA]** Volte para o VS Code com o projeto aberto.
**[COMANDO]** Nenhum.

> Este é o fluxo completo. Agora vou mostrar o ambiente e as ferramentas, e começar criando a infraestrutura.

---

## Parte 2: Ambiente e criação da infraestrutura (≈ 10 a 15 min)

### 2.1 Ponto de partida

**[TELA]** Portal Azure → *Resource groups*. Mostre que **não existe** o grupo `561413-petflow-s4-devops-rg`. (Existe o `561413-petflow-rg`, da disciplina de Java: aponte e diga que não será tocado.)
**[COMANDO]** No terminal WSL: `az account show --query "{assinatura:name, usuario:user.name}" -o table`

> Vou começar do zero. Neste momento não existe nada deste projeto no Azure. Aqui no Portal estão os grupos de recursos da assinatura: o 561413-petflow-rg pertence à disciplina de Java e não será alterado. O grupo de recursos desta entrega ainda não existe.
>
> No terminal, confirmo que o Azure CLI está logado. Esta é a assinatura Azure for Students que estamos usando, de um dos integrantes do grupo.

### 2.2 Ferramentas e configuração

**[TELA]** VS Code com a pasta do projeto. Abra `scripts/01-create-infra.sh` e o `.env.azure.example` (**nunca o `.env.azure`**).
**[COMANDO]** Nenhum.

> Aqui no VS Code está o projeto. Na pasta scripts estão os scripts do Azure CLI que criam a infraestrutura, e na pasta pipelines estão as pipelines em YAML.
>
> Todas as configurações do ambiente, como nomes de recursos e senhas, ficam em um arquivo chamado ponto env ponto azure, que não vai para o Git. O que vai para o repositório é este modelo, o env azure example, sem nenhum segredo.

### 2.3 Criar a infraestrutura

**[TELA]** Terminal WSL na raiz do projeto.
**[COMANDO]** `bash scripts/01-create-infra.sh`

> Vou executar o script que cria toda a infraestrutura. Ele tem seis passos e eu vou comentar cada um conforme aparece.

**Quando aparecer `==> 1/6 Resource Group`**
> Passo um: o grupo de recursos, que reúne tudo o que esta entrega vai usar na Azure, na região South Africa North.

**Quando aparecer `==> 2/6 PostgreSQL Flexible Server`** *(é o passo mais demorado: use a espera para explicar)*
> Passo dois: o banco de dados. É um Azure Database for PostgreSQL Flexible Server, versão 16, um serviço gerenciado: o Azure cuida do disco, do backup e das atualizações. Usamos o tamanho Burstable B1ms, com 32 gigabytes de armazenamento persistente, então os dados sobrevivem a reinícios e a novos deploys. A conexão exige SSL. Esse passo demora alguns minutos.

**Quando aparecer `==> 3/6 Firewall`**
> Passo três: o firewall. O servidor aceita conexões dos serviços do Azure, como o nosso Web App, e libero também o meu IP, para poder consultar o banco daqui do terminal durante a demonstração.

**Quando aparecer `==> 4/6 Usuário da aplicação`**
> Passo quatro: o usuário do banco que a aplicação vai usar. Ele não é o administrador do servidor, tem permissão só no nosso banco.

**Quando aparecer `==> 5/6 App Service Plan`**
> Passo cinco: o plano do App Service, Linux, tamanho B1, que fornece a máquina onde a aplicação vai rodar.

**Quando aparecer `==> 6/6 Web App`**
> Passo seis: o Web App com Java 21, e as variáveis de ambiente da aplicação. Os valores de senha são gravados direto no Azure e não são exibidos aqui.

**Quando terminar (`Infraestrutura pronta.`)**
**[TELA]** Aponte no terminal a URL do Web App, o endereço do PostgreSQL e o `DB_URL` impresso no final. **Copie o `DB_URL`** para o bloco de notas.

> A infraestrutura está pronta. O script mostrou o endereço do Web App, o do PostgreSQL e o DB_URL, que vou usar daqui a pouco para configurar a pipeline.

### 2.4 Conferir no Portal e no banco

**[TELA]** Portal Azure → atualize *Resource groups* → abra `561413-petflow-s4-devops-rg`: o **App Service plan**, o **Web App** e o **PostgreSQL Flexible Server**. Abra o PostgreSQL → **Networking** (regras de firewall: serviços Azure e seu IP). Abra o Web App → **Environment variables** (valores ocultos).
**[COMANDO]** `bash scripts/sql.sh "SELECT current_database(), version()"`

> Aqui está o grupo de recursos que o script criou: o plano, o Web App e o servidor PostgreSQL. No servidor, em Networking, estão as regras de firewall. E nas variáveis de ambiente do Web App, os valores sensíveis aparecem ocultos.
>
> Agora uma consulta ao banco na nuvem para confirmar que a conexão funciona. Ainda não existem tabelas: o schema é criado pela própria aplicação, na primeira vez que ela sobe, com o Flyway.

---

## Parte 3: Projeto no Azure DevOps e acesso do professor (≈ 3 min)

**[TELA]** `dev.azure.com`, página da organização.
**[AÇÃO]** *New project* com:
- **Project name:** `PetFlow`
- **Description:** `Projeto para entrega da Sprint 4 do professor João Carlos Menk. Integrantes: 561413 - Lucas Grillo Alcântara - 2TDSPX; 561469 - Pietro Ferreira Gomes Abrahamian - 2TDSPX; 561792 - Pedro Peres Benitez - 2TDSPX; 562027 - Lucca Ramos Mussumecci - 2TDSPX`
- **Visibility:** Private
- *Advanced*: **Version control:** Git; **Work item process:** Scrum
- *Create*.
**[COMANDO]** Nenhum.

> Agora o projeto no Azure DevOps. O nome é PetFlow. Na descrição coloco o professor da disciplina, João Carlos Menk, e os integrantes do grupo, com RM e turma. A visibilidade é privada, o controle de versão é Git e o processo de itens de trabalho é Scrum.

**[AÇÃO]** *Organization settings* → *Users* → *Add users*: e-mail `pf0841@fiap.com.br`, *Access level* **Basic**, projeto `PetFlow`, grupo **Contributors**. Confirme que ele aparece na lista com acesso **Basic**.

> Agora convido o professor. Adiciono o e-mail dele, com nível de acesso Basic, no nosso projeto, como colaborador. Aqui na lista, ele aparece com acesso Basic.

---

## Parte 4: Código no Azure Repos e política da master (≈ 3 min)

**[TELA]** Azure DevOps → *Repos* → *Files* (repositório vazio). Copie a URL HTTPS de clone (botão *Clone*).
**[COMANDO]** No terminal WSL, na raiz do projeto, um por vez:
```bash
git init -b master
git add .
git status
git commit -m "PetFlow Sprint 4: aplicação, pipelines e scripts de infraestrutura"
git remote add origin "<URL-de-clone-do-Azure-Repos>"
git push -u origin master
```

> O repositório do projeto está vazio, então vou enviar o código. Primeiro crio o repositório Git local, com a branch master.
>
> Agora adiciono os arquivos e, antes de fazer o commit, confiro o git status. Reparem que o arquivo ponto env ponto azure não aparece na lista: ele está no gitignore, então as senhas nunca vão para o repositório.
>
> Faço o commit, aponto o repositório remoto para o Azure Repos e envio a branch master.

**[TELA]** Atualize *Repos* → *Files*: aparecem `pipelines/`, `scripts/`, `src/`, `docs/`, `README.md`. Confirme que a branch padrão é **master** (*Repos → Branches*).

> O código chegou ao Azure Repos, com as pipelines, os scripts, o código-fonte e a documentação. A branch padrão é a master.

**[AÇÃO]** *Repos → Branches* → `master` → `⋯` → **Branch policies**: **Require a minimum number of reviewers** = 1, marcando *Allow requestors to approve their own changes*; **Check for linked work items** = *Required*.

> Por fim, protejo a master com uma política de branch: toda mudança precisa de uma Pull Request com um revisor e com uma Task vinculada. Assim nada entra na master sem passar por esse fluxo.

---

## Parte 5: Segredos (Variable Group) e Service Connection (≈ 3 min)

### 5.1 Variable Group

**[TELA]** Azure DevOps → *Pipelines* → *Library* → **+ Variable group**.
**[AÇÃO]** Nome `petflow-s4-secrets`. Crie as variáveis abaixo (cole os valores a partir do bloco de notas, **sem mostrar o `.env.azure`**). Para as marcadas, clique no **cadeado**:

| Variável | Valor | Cadeado |
|---|---|:---:|
| `DB_URL` | o impresso pelo script 01 | sim |
| `DB_USERNAME` | do `.env.azure` | sim |
| `DB_PASSWORD` | do `.env.azure` | sim |
| `JWT_SECRET` | do `.env.azure` | sim |
| `SMOKE_TUTOR_EMAIL` | `maria@petflow.com` | não |
| `SMOKE_TUTOR_PASSWORD` | `Tutor@123` | sim |
| `GITHUB_PAT` | PAT do GitHub (escopo `repo`) | sim |
| `GITHUB_REPO` | `github.com/lgaxd/petflow-devops-sprint4` | não |

Salve com **Save**.
**[COMANDO]** Nenhum.

> Agora os segredos. As pipelines precisam de usuário e senha do banco, da chave de assinatura dos tokens e de um token do GitHub. Nada disso vai para o código nem para os arquivos YAML. Eu guardo tudo em um grupo de variáveis do Azure DevOps, e marco com o cadeado as que são sensíveis: depois de salvas, elas não podem mais ser lidas e aparecem mascaradas nos logs.

### 5.2 Service Connection

**[TELA]** *Project settings* → *Service connections* → **New service connection** → *Azure Resource Manager*.
**[AÇÃO]** *Identity type*: **App registration (automatic)**; *Credential*: **Workload identity federation**; *Scope level*: **Subscription** → a assinatura Azure for Students → *Resource group*: `561413-petflow-s4-devops-rg`; *Service connection name*: `sc-petflow-s4`; marque **Grant access permission to all pipelines**; *Save*.
**[COMANDO]** Nenhum.

> Para a pipeline poder implantar no Azure, preciso de uma conexão de serviço. Escolho o Azure Resource Manager com federação de identidade, o que dispensa guardar senha. O escopo é só o grupo de recursos deste projeto, que o script acabou de criar, então a pipeline só consegue mexer nos recursos desta entrega.

---

## Parte 6: Pipelines por dentro e primeiro CI/CD (≈ 12 a 15 min)

### 6.1 Explicar o CI e o CD

**[TELA]** Azure Repos → `pipelines/ci.yml`.
**[COMANDO]** Nenhum.

> Vamos ver a pipeline de CI. O gatilho é a branch master: a cada alteração nela a pipeline dispara. Ela roda em um agente Ubuntu hospedado pela Microsoft.
>
> Os passos, na ordem: instala o Java 21; restaura o cache das dependências do Maven, para ser mais rápido; executa o mvn clean verify, que compila e roda todos os testes automatizados, e publica o resultado dos testes e a cobertura; copia o arquivo petflow-api.jar e os scripts do CD; e publica tudo como um artefato chamado drop. Se um teste falhar, a pipeline para e nada é publicado.

**[TELA]** `pipelines/cd.yml`.

> Agora o CD. Ele não tem um gatilho de branch: ele está ligado ao CI. O bloco resources.pipelines diz que, sempre que o petflow-ci publicar um artefato novo na master, esta pipeline dispara sozinha.
>
> Tem dois estágios. O primeiro, Deploy, baixa o artefato do CI, e uma tarefa do Azure CLI grava as configurações do Web App com os segredos do grupo de variáveis, faz o deploy do JAR e espera o health check confirmar que a aplicação e o banco estão no ar. O segundo, SmokeTest, só roda se o deploy deu certo: ele faz login e executa um CRUD de pets e um agendamento de evento na URL pública. O CD não compila de novo: ele usa exatamente o JAR gerado no CI.

**[TELA]** `pipelines/mirror-github.yml` (rápido).

> E esta terceira pipeline espelha cada merge da master no GitHub, automaticamente.

### 6.2 Criar as pipelines

**[TELA]** Azure DevOps → *Pipelines* → **New pipeline** → *Azure Repos Git* → o repositório → **Existing Azure Pipelines YAML file**.
**[AÇÃO]** Crie as três, **nesta ordem**, escolhendo *Branch* `master`, e use **Save** (seta ao lado de *Run*), sem executar. Depois renomeie cada uma (*Pipelines* → `⋯` → *Rename/move*):

| Caminho do YAML | Nome (exato) |
|---|---|
| `/pipelines/ci.yml` | `petflow-ci` |
| `/pipelines/cd.yml` | `petflow-cd` |
| `/pipelines/mirror-github.yml` | `petflow-mirror-github` |

**[COMANDO]** Nenhum.

> Agora crio as pipelines a partir desses arquivos YAML que estão no repositório. Primeiro o CI, que chamo de petflow-ci. Depois o CD, petflow-cd, e por último a do espelho no GitHub. O nome petflow-ci precisa ser exatamente esse, porque o CD faz referência a ele.

### 6.3 Primeiro ciclo (cria o schema do banco)

**[AÇÃO]** *Pipelines* → `petflow-ci` → **Run pipeline** → *Run*. (Esta primeira execução é manual porque ainda não houve commit depois de criar a pipeline. Se pedir **Permit** para o Variable Group ou a Service Connection, clique em *Permit*.)
**[COMANDO]** Nenhum.

> Como ainda não houve nenhuma alteração depois de criar a pipeline, esta primeira execução eu disparo manualmente, só desta vez. A partir daqui, tudo será automático. Esta primeira rodada faz o primeiro deploy e cria o banco de dados.

**Enquanto o CI roda:** comente os passos conforme aparecem (instalar Java, cache, `mvn clean verify`, publicar testes, publicar artefato). Resumo curto, porque a explicação completa será repetida na Parte 10.
**Ao terminar o CI:** mostre a aba **Tests** (todos passando) e o artefato **drop** com o `petflow-api.jar`.

> O CI terminou. Estes são os testes que passaram, e este é o artefato publicado, o petflow-api.jar.

**[TELA]** Volte para *Pipelines*: o **`petflow-cd` iniciou sozinho** ao fim do CI. Aguarde os dois estágios (*Deploy* e *SmokeTest*) ficarem verdes.

> Reparem: eu não cliquei em nada, e o CD já começou sozinho porque o CI publicou um artefato novo. Ele está fazendo o deploy no Web App e depois vai rodar o teste automático.

**Quando o CD terminar (verde):**
**[COMANDO]**
```bash
curl -s https://561413-petflow-s4-api.azurewebsites.net/actuator/health
bash scripts/sql.sh "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name"
```

> O primeiro ciclo terminou. O health check mostra que a aplicação está no ar e conectada ao banco. E agora o banco já tem as tabelas, criadas pela aplicação com o Flyway no primeiro start. Com o ambiente pronto, vou fazer uma alteração de código e acompanhar todo o fluxo automático.

---

## Parte 7: Task e branch pelo Azure Boards (≈ 2 min)

**[TELA]** Azure DevOps → *Boards* → *Work items*.
**[AÇÃO]** **New Work Item → Task**, título `Validar tamanho mínimo do nome do pet`, atribuída a você, estado **Active**. Dentro da Task, painel **Development** → **Create a branch**, base `master`, e confirme.
**[COMANDO]** No terminal WSL, na raiz do projeto:
```bash
git fetch
git checkout <nome-da-branch-criada-pela-task>
git branch --show-current
```

> Vou fazer uma alteração no código. Primeiro, crio uma Task no Azure Boards, chamada Validar tamanho mínimo do nome do pet, e deixo ela ativa.
>
> A branch de trabalho eu crio a partir da própria Task, por este botão, e não pelo terminal: assim a branch já nasce vinculada ao item. No terminal, eu só baixo as informações e mudo para essa branch.

---

## Parte 8: Alteração no código (≈ 3 min)

**[TELA]** VS Code, branch da Task. Abra `src/main/java/br/com/petflow/petflow_api/dto/PetRequestDTO.java`.
**[COMANDO]** `mkdir -p src/test/java/br/com/petflow/petflow_api/dto`

> Hoje o nome do pet só tem um limite máximo de 100 caracteres. Isso significa que um pet com nome de uma única letra é aceito. A regra nova é exigir no mínimo dois caracteres.
>
> Vou trabalhar com o teste primeiro. Crio um teste novo, que verifica que um nome com uma letra é rejeitado e que um nome com duas letras é aceito.

**[AÇÃO]** Crie `src/test/java/br/com/petflow/petflow_api/dto/PetRequestValidationTest.java` com o conteúdo de [comandos-video.md](comandos-video.md) (Parte 8).
**[AÇÃO]** Rode **só esse teste pelo painel de testes do VS Code** (não pelo terminal): `rejeitaNomeComMenosDeDoisCaracteres` deve **falhar**.

> Executo o teste pelo painel de testes do VS Code. Ele falha, como esperado, porque a regra ainda não existe. Quero deixar claro que isto é só a execução de um teste unitário: o build que gera o artefato é o da pipeline.

**[AÇÃO]** No `PetRequestDTO.java`, troque o `@Size` do `name` por:
`@Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")`
**[AÇÃO]** Rode o teste de novo pelo painel: deve **passar**.

> Agora implemento a regra: o tamanho mínimo passa a ser dois, com uma mensagem clara. Rodo o teste de novo, e agora ele passa.

---

## Parte 9: Commit, push, Pull Request e merge (≈ 2 min)

**[COMANDO]**
```bash
git status
git add src/
git commit -m "Validar tamanho minimo do nome do pet"
git push
```

> Vou enviar a alteração. Confiro o status, adiciono só a pasta src, faço o commit e envio para a branch da Task.

**[TELA]** Azure DevOps → *Repos* → *Pull requests* → **New pull request** (branch da Task → `master`).
**[AÇÃO]** Vincule a Task (*Work items*). Aprove a PR. Clique em **Complete**, deixando marcado *Complete associated work items*.
**[COMANDO]** Nenhum a partir daqui.

> No Azure Repos abro uma Pull Request da branch da Task para a master e vinculo a Task. Conforme a política, a PR precisa ser aprovada. Aprovo e concluo o merge.
>
> A partir de agora, eu não vou rodar mais nenhum comando e não vou clicar em Run: o merge na master dispara a pipeline de CI sozinha.

---

## Parte 10: CI automático, explicado durante a execução (≈ 4 a 5 min)

**[TELA]** Azure DevOps → *Pipelines* → `petflow-ci`: um run novo **iniciou sozinho** (motivo **Individual CI**, com o commit do merge). Abra o run e acompanhe cada step.
**[COMANDO]** Nenhum.

> Aqui está o CI. Ele começou sozinho, assim que o merge foi concluído: o motivo é Individual CI, e este é o commit do merge. Vamos acompanhar cada passo.

**Narre cada step conforme ele roda:**

> Primeiro, a preparação do agente e o download do código da master.
>
> Agora o Java 21 é instalado e o cache das dependências do Maven é restaurado.
>
> Este é o passo principal: o mvn clean verify. Ele compila o projeto e executa todos os testes automatizados. Se algum falhar, a pipeline para aqui e nada é publicado, e é isso que protege o deploy.
>
> Os resultados dos testes e a cobertura são publicados.
>
> Por fim, o arquivo petflow-api.jar e os scripts do CD são copiados e publicados como o artefato drop.

**Ao terminar, mostre (nesta ordem):**
**[TELA]** (1) Aba **Tests**: o total passando (190), com o `PetRequestValidationTest` incluído. (2) **Code coverage**. (3) No resumo do run, **1 published** (Artifacts) → pasta **`drop`** → **`petflow-api.jar`**, com zoom no nome.

> O CI terminou com sucesso. Aqui na aba de testes, todos passaram, inclusive o teste novo que criei. Esta é a cobertura de código. E este é o artefato publicado: o petflow-api.jar. Esse mesmo arquivo é o que o CD vai implantar. Não existe um segundo build.

---

## Parte 11: CD automático, explicado durante a execução (≈ 5 a 7 min)

**[TELA]** *Pipelines* → o **`petflow-cd`** iniciou **sozinho** (acionado pelo run do `petflow-ci`). Abra o run e mostre os dois estágios no gráfico: **Deploy** e **SmokeTest**.
**[COMANDO]** Nenhum.

> Assim que o CI terminou, o CD começou sozinho, acionado pelo artefato novo. Eu não executei nada. Aqui podemos ver o vínculo com o run do CI e os dois estágios: Deploy e SmokeTest.

**Narre cada step conforme ele roda (use a espera para explicar; não corte):**

> No estágio Deploy, o primeiro passo baixa o artefato do CI, o drop, com o petflow-api.jar.
>
> Agora o passo do Azure CLI. Primeiro ele grava as configurações do Web App, usando os segredos do grupo de variáveis. Reparem nos logs: os valores aparecem mascarados, com asteriscos.
>
> Depois ele faz o deploy do JAR no Web App e fica consultando o health check, até a aplicação e o PostgreSQL responderem que estão no ar. Isso pode levar alguns minutos, porque a aplicação Spring precisa iniciar.
>
> Terminado o Deploy, começa o SmokeTest: um teste automático na URL pública. Ele faz login, cria um pet, consulta, atualiza e remove, e também cria um evento agendado com data futura e confere que um evento realizado com data futura é rejeitado.

**Ao terminar:** mostre os **dois estágios verdes** e abra o log do SmokeTest com as linhas `PASS ... -> 201 / 200 / 204`.

> O CD terminou, os dois estágios estão verdes. Estas linhas PASS mostram cada chamada do teste automático e o status retornado. O código novo já está implantado na nuvem.

---

## Parte 12: Recursos atualizados no Portal Azure (≈ 2 min)

**[TELA]** Portal Azure → `561413-petflow-s4-devops-rg` → **Web App** → **Deployment Center** → **Logs** (o deploy mais recente, com data e hora de agora). Depois **Environment variables** (valores ocultos). Depois o servidor **PostgreSQL** → **Overview**.
**[COMANDO]** Nenhum.

> Agora, no Portal Azure, vamos ver os recursos atualizados pelo deploy. No Web App, em Deployment Center, está o deploy mais recente, feito agora há pouco pela pipeline. Em Environment variables, estão as configurações da aplicação, escritas pelo CD, com os valores sensíveis ocultos. E aqui está o servidor PostgreSQL, disponível, com o banco que a aplicação usa.

---

## Parte 13: App em nuvem com a alteração (≈ 2 min)

**[TELA]** Navegador: `https://561413-petflow-s4-api.azurewebsites.net/swagger` (barra de endereço visível, **nunca localhost**).
**[COMANDO]** Opcional no terminal: `curl -s https://561413-petflow-s4-api.azurewebsites.net/actuator/health`

> Este é o aplicativo que a pipeline acabou de implantar, rodando na nuvem, no endereço do Azure Web App.

**[AÇÃO]** Swagger → `POST /auth/login` com `{ "email": "maria@petflow.com", "password": "<senha-do-seed>" }`. Copie o `token`, clique em **Authorize** e cole.
**[AÇÃO]** `POST /pets` com `{ "name": "A", "tutorId": 2, "speciesId": 1 }` → **400**, mensagem `O nome deve ter entre 2 e 100 caracteres`.

> Faço login com um tutor de demonstração e autorizo o Swagger com o token. Agora tento cadastrar um pet com o nome A, de uma letra só. A resposta é 400, com a mensagem nova: o nome deve ter entre 2 e 100 caracteres. Isso prova que a alteração de código feita na branch da Task chegou à nuvem pela pipeline.

---

## Parte 14: CRUD completo com prova no banco (≈ 4 min)

**Regra:** cada operação é feita pela API na nuvem (Swagger) e provada **logo depois** com um `SELECT` no PostgreSQL na nuvem. Deixe Swagger e terminal visíveis.

### 14.1 Estado inicial
**[COMANDO]** `bash scripts/sql.sh "SELECT id, name, weight FROM pet WHERE name LIKE 'Demo%'"`

> Vou fazer o CRUD completo e, depois de cada operação, comprovar no banco com um SELECT. Primeiro, o estado inicial: nenhum pet de demonstração.

### 14.2 Inserir
**[AÇÃO]** Swagger → `POST /pets` com `{"name":"Demo Rex","breed":"Labrador","birthDate":"2021-05-10","weight":12.5,"tutorId":2,"speciesId":1}` → **201**. Anote o `id`.
**[COMANDO]** `bash scripts/sql.sh "SELECT id, name, breed, weight, tutor_id, species_id FROM pet WHERE name LIKE 'Demo%'"`

> Inserir: cadastro o pet Demo Rex. A API devolveu 201 e o identificador. No banco, o SELECT mostra a linha na tabela PET, com os mesmos dados.

**[COMANDO]** (bônus) `bash scripts/sql.sh "SELECT id, tutor_id, points, reference_type, reference_id FROM reward_point WHERE tutor_id = 2 ORDER BY id DESC LIMIT 3"`

> Como bônus, o cadastro do pet também gerou pontos para o tutor, que a aplicação grava na tabela de pontos.

### 14.3 Consultar
**[AÇÃO]** Swagger → `GET /pets/{id}` → **200**.
**[COMANDO]** `bash scripts/sql.sh "SELECT id, name, breed, weight FROM pet WHERE id = <ID>"`

> Consultar: a API retorna o pet pelo identificador, e o SELECT no banco confirma os mesmos valores.

### 14.4 Atualizar
**[AÇÃO]** Swagger → `PUT /pets/{id}` com `{"name":"Demo Rex Atualizado","breed":"Labrador","birthDate":"2021-05-10","weight":14.0,"tutorId":2,"speciesId":1}` → **200**.
**[COMANDO]** `bash scripts/sql.sh "SELECT id, name, weight FROM pet WHERE id = <ID>"`

> Atualizar: mudo o nome e o peso. A API devolve 200, e o SELECT mostra o nome novo e o peso 14.

### 14.5 Deletar
**[AÇÃO]** Swagger → `DELETE /pets/{id}` → **204**. (Opcional: `GET /pets/{id}` → **404**.)
**[COMANDO]** `bash scripts/sql.sh "SELECT COUNT(*) AS pets_demo_restantes FROM pet WHERE name LIKE 'Demo%'"`

> Deletar: removo o pet. A API devolve 204, e o SELECT mostra zero registros. Com isso, o ciclo completo, inserir, consultar, atualizar e deletar, está comprovado no banco de dados na nuvem.

---

## Parte 15: Fechamento (≈ 1 min)

**[TELA]** Azure DevOps → *Pipelines*: a pipeline `petflow-mirror-github` rodou no merge. Depois o repositório do **GitHub** com o mesmo commit.
**[COMANDO]** `git log --oneline -3` (compare o hash com o último commit do GitHub)

> Para fechar: o merge também foi espelhado automaticamente no GitHub, com o mesmo commit.
>
> Recapitulando: uma Task gerou a branch, a alteração de código virou Pull Request e merge, o CI compilou, testou e publicou o JAR, o CD implantou no Web App, e a aplicação na nuvem, conectada ao PostgreSQL gerenciado, passou no CRUD, com cada operação comprovada no banco. Tudo automatizado e com os segredos protegidos. Obrigado.

---

## Checklist final de penalidades (marque antes de enviar)

**Entrega**
- [ ] Nada em localhost em nenhuma cena (barra de endereço sempre visível)
- [ ] PDF só com nome completo, RM, turma, link do GitHub, link do YouTube e link do projeto Azure DevOps
- [ ] Professor convidado com acesso **Basic** e o projeto acessível a ele; vídeo acessível a ele
- [ ] Vídeo em 720p ou mais, áudio claro, sem legenda, com voz explicando

**Azure DevOps e pipelines**
- [ ] Projeto `PetFlow`, Private, Git, Scrum, descrição com o professor e os integrantes
- [ ] CI disparou **sozinho** após o merge na `master` (motivo: Individual CI)
- [ ] CD disparou **sozinho** após o artefato (e foi **mostrado depois da alteração**)
- [ ] Cada step do CI e do CD **explicado durante a execução**
- [ ] Testes automatizados mostrados na aba Tests
- [ ] **Artefato JAR** mostrado em Artifacts do run do CI
- [ ] Segredos mostrados protegidos (Variable Group com cadeado, App Settings ocultos)

**Código e banco**
- [ ] Alteração feita no **código** (não no README), com teste novo
- [ ] Branch criada **pela Task no Boards**, não pelo terminal
- [ ] Nenhum build ou deploy manual no terminal (o script 01 só cria a infraestrutura)
- [ ] Recursos mostrados no Portal Azure, incluindo o deploy mais recente
- [ ] App em nuvem **depois do CD** mostrando a mudança (400 para nome `A`)
- [ ] CRUD completo (inserir, consultar, atualizar, deletar), **cada operação provada por `SELECT`**

## Se algo der errado durante a gravação

| Problema | O que fazer |
|---|---|
| `01-create-infra.sh` falha no passo 4 (usuário do banco) | Leia o erro. O servidor já foi criado: corrija e rode o script de novo (ele reaproveita o que existe) |
| Service Connection automática falha (permissão no Microsoft Entra) | Use *App registration (manual)* ou *Service principal (manual)* com um service principal criado antes, fora da câmera |
| Pipeline fica em *Queued* | Organização sem Parallel jobs liberado (Organization settings → Pipelines → Parallel jobs) |
| CI não inicia sozinho após o merge | Conferir o `trigger: master` e se o merge foi na `master`. Pare, corrija e regrave a partir da Parte 9; não clique em Run no vídeo |
| CI vermelho | Ler o log do step que falhou, corrigir e novo commit na mesma branch; mostrar a correção (é uma cena válida) |
| CD não dispara | Conferir se o nome da pipeline de CI é exatamente `petflow-ci` e se o `resources.pipelines` do `cd.yml` aponta para ele |
| CD pede autorização (*Permit*) | Clique em *Permit* para o Variable Group e a Service Connection na primeira execução |
| Deploy demora ou health 503/000 | Aguardar até 10 min. 503 indica Web App sem acesso ao PostgreSQL (firewall, `DB_URL` ou senha); `az webapp log tail -g 561413-petflow-s4-devops-rg -n 561413-petflow-s4-api` mostra o motivo |
| `sql.sh` recusado por firewall | Seu IP mudou: rode `scripts/01-create-infra.sh` de novo (atualiza a regra `AllowClientIP`) |
| Swagger responde 401 | Token expirado ou sem **Authorize**: refaça o login |
