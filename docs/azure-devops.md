# Configuração do Azure DevOps (passo a passo)

Tudo no portal <https://dev.azure.com>. Itens com 🎥 valem como cena do vídeo ou print.

## 0. Antes de tudo
1. **Paralelismo dos agentes** (*Organization settings → Pipelines → Parallel jobs*): Microsoft-hosted deve ter 1 ou mais. Se for 0, peça o grant em <https://aka.ms/azpipelines-parallelism-request>.
2. **Segredos locais**: `cp .env.azure.example .env.azure` e preencha `PG_ADMIN_PASSWORD`, `DB_PASSWORD`, `SMOKE_TUTOR_PASSWORD` e o `JWT_SECRET` (`openssl rand -base64 32`).

## 1. Projeto 🎥
*New project*:
- **Name:** `PetFlow`
- **Description:** `Projeto para entrega da Sprint 4 do professor João Carlos Menk. Integrantes: 561413 - Lucas Grillo Alcântara - 2TDSPX; 561469 - Pietro Ferreira Gomes Abrahamian - 2TDSPX; 561792 - Pedro Peres Benitez - 2TDSPX; 562027 - Lucca Ramos Mussumecci - 2TDSPX`
- **Visibility:** Private. Em *Advanced*: Version control **Git**, Work item process **Scrum**.

## 2. Infraestrutura na Azure
No WSL, na raiz do projeto: `az login` e depois `bash scripts/01-create-infra.sh`. O script cria o Resource Group, o PostgreSQL Flexible Server (de 5 a 10 minutos; também cria o usuário da aplicação e libera o seu IP), o plano B1 e o Web App. No fim imprime o `DB_URL` para o passo 4.

## 3. Repositório (Azure Repos)
O repo vazio já existe. Faça o primeiro push da pasta `DevOps` (única vez; depois só a branch da Task):
```bash
git init -b master
git add . && git status          # .env.azure e target/ NÃO podem aparecer
git commit -m "PetFlow Sprint 4: aplicação, pipelines e scripts de infraestrutura"
git remote add origin https://dev.azure.com/<org>/PetFlow/_git/PetFlow
git push -u origin master
```
Em *Repos → Branches*, deixe **master** como branch padrão.

**Políticas da master** 🎥 (*Branches → master → ⋯ → Branch policies*): 1 revisor com "Allow requestors to approve their own changes" e "Check for linked work items: Required".

## 4. Variable Group (segredos)
*Pipelines → Library → + Variable group*, nome `petflow-s4-secrets`. Marque o cadeado (secret) nos sensíveis:

| Variável | Valor | Secreta |
|---|---|:---:|
| `DB_URL` | impresso pelo script 01 (`jdbc:postgresql://<servidor>.postgres.database.azure.com:5432/petflow?sslmode=require`) | sim |
| `DB_USERNAME` | o mesmo do `.env.azure` (padrão `petflow_user`) | sim |
| `DB_PASSWORD` | do `.env.azure` | sim |
| `JWT_SECRET` | do `.env.azure` | sim |
| `SMOKE_TUTOR_EMAIL` | `maria@petflow.com` | não |
| `SMOKE_TUTOR_PASSWORD` | senha do tutor do seed (README do repo Java) | sim |
| `GITHUB_PAT` | PAT do GitHub com escopo `repo` | sim |
| `GITHUB_REPO` | `github.com/lgaxd/petflow-devops-sprint4` | não |

## 5. Service connection
*Project settings → Service connections → New → Azure Resource Manager*: **Workload identity federation (automatic)**, escopo *Subscription* e Resource Group `561413-petflow-s4-devops-rg`. Nome: **`sc-petflow-s4`**. Marque *Grant access permission to all pipelines*.

## 6. Pipelines 🎥
*Pipelines → New pipeline → Azure Repos Git → repo → Existing Azure Pipelines YAML file*:

| Nome (exato) | Arquivo |
|---|---|
| **`petflow-ci`** | `/pipelines/ci.yml` |
| **`petflow-cd`** | `/pipelines/cd.yml` |
| `petflow-mirror-github` | `/pipelines/mirror-github.yml` |

Use *Save* (seta ao lado de *Run*) e renomeie em *⋯ → Rename/move*. O nome `petflow-ci` precisa ser exatamente esse: o CD o usa em `resources.pipelines.source`. Rode `petflow-ci` uma vez; o `petflow-cd` dispara sozinho ao fim. Se pedir autorização para o Variable Group, clique em **Permit**.

Evidências: log de cada step, aba **Tests** (188 testes; 190 depois da demo), **Code coverage** e o artefato **drop** com `petflow-api.jar`.

## 7. Espelho GitHub (uma vez)
1. Crie o repo vazio `lgaxd/petflow-devops-sprint4` no GitHub.
2. Gere um PAT (Settings → Developer settings) com escopo `repo` e salve em `GITHUB_PAT` (item 4).
3. A cada push na `master`, o `petflow-mirror-github` envia o mesmo commit. Nada manual depois disso.

## 8. Fluxo da demo 🎥
1. *Boards → Work items → New → Task*: `Validar tamanho mínimo do nome do pet`, estado **Active**.
2. Na Task, painel **Development → Create a branch** (base `master`). **Nunca crie a branch pelo terminal.**
3. No VS Code (WSL): `git fetch` e `git checkout <branch-da-task>`. Siga [mudanca-pr.md](mudanca-pr.md): teste que falha (pelo painel de testes do VS Code), regra, teste que passa, commit e push. Não rode `mvnw package`/`verify` nem faça deploy pelo terminal: o professor estranhou "build manual" no CP6.
4. *Repos → Pull requests → New* (branch → `master`), vincule a Task, aprove e clique em **Complete** (deixe marcado "Complete associated work items").
5. Mostre o `petflow-ci` iniciando sozinho, depois o `petflow-cd`, os logs, a aba Tests e o artefato JAR.
6. **Só depois do CD terminar**: Portal Azure com os recursos, Swagger no Web App, CRUD com SELECT no PostgreSQL.

## 9. Acesso do professor 🎥
*Organization settings → Users → Add users*: `pf0841@fiap.com.br`, access level **Basic**, projeto `PetFlow`, grupo **Contributors**. Confira em *Users* que aparece como Basic.

## 10. Antes de entregar
Nada em localhost, professor convidado, PDF com os links (veja `entrega/`) e a `master` do GitHub sincronizada.
