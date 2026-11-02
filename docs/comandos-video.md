# Comandos para copiar e colar, por parte do vídeo

Complemento do [roteiro-video.md](roteiro-video.md). A numeração das partes é a mesma. Rode tudo no **terminal do WSL (Ubuntu)**, na raiz do projeto, salvo indicação contrária.

> Regra do professor (feedback do CP6): **nenhum build ou deploy pelo terminal e nenhuma branch criada pelo terminal.** O único script de criação rodado no vídeo é o `01-create-infra.sh` (infraestrutura). Tudo o que compila e implanta a aplicação é da pipeline.
>
> Troque os textos entre `< >` pelos seus valores. **Nunca cole senhas na tela**: os scripts leem o `.env.azure`, que não deve ser aberto na gravação.

---

## A. Preparação (fora do vídeo, antes de gravar)

### A1. Login e ferramentas
```bash
az login
az account show --query "{assinatura:name, usuario:user.name}" -o table
psql --version                    # obrigatório (usado pelo script 01 e pelo sql.sh): sudo apt update && sudo apt install -y postgresql-client
git config --global user.name "<seu nome>"
git config --global user.email "<seu e-mail>"
```
Tenha pronta, fora da câmera, uma credencial do Azure Repos (PAT do Azure DevOps com Code: Read & Write) para o primeiro `git push` (Parte 4).

### A2. Confirmar o estado inicial limpo
```bash
# o Resource Group NÃO pode existir (esperado: false)
az group exists -n 561413-petflow-s4-devops-rg

# o código local NÃO pode ter .git (esperado: "No such file or directory")
ls -d .git 2>&1

# a alteração da demo NÃO pode estar aplicada (deve mostrar "max = 100" e NÃO "min = 2")
grep -n "@Size" src/main/java/br/com/petflow/petflow_api/dto/PetRequestDTO.java | head -3

# o teste novo NÃO pode existir (esperado: "No such file or directory")
ls src/test/java/br/com/petflow/petflow_api/dto 2>&1
```

### A3. Se você ensaiou (voltar ao estado inicial)
```bash
bash scripts/99-delete-infra.sh   # apaga o Resource Group; pede para digitar o nome. Espere terminar (alguns minutos)
rm -rf .git                       # remove o repositório local do ensaio
```
No Azure DevOps, apague o projeto de ensaio (*Project settings → Overview → Delete*). Se a alteração da demo ficou aplicada nos arquivos, desfaça manualmente: volte o `@Size` para `max = 100` e apague a pasta `src/test/java/br/com/petflow/petflow_api/dto`.

---

## Parte 1: Abertura e diagrama
Nenhum comando.

---

## Parte 2: Ambiente e criação da infraestrutura

**2.1 Confirmar o login do Azure CLI**
```bash
az account show --query "{assinatura:name, usuario:user.name}" -o table
```

**2.3 Criar toda a infraestrutura (leva alguns minutos; comente cada passo `==> n/6`)**
```bash
bash scripts/01-create-infra.sh
```
No final o script imprime a URL do Web App, o endereço do PostgreSQL e o `DB_URL`. **Copie o `DB_URL`** (Parte 5).

**2.4 Conferir que o banco responde (ainda sem tabelas: o schema nasce no primeiro deploy)**
```bash
bash scripts/sql.sh "SELECT current_database(), version()"
```

---

## Parte 3: Projeto no Azure DevOps e acesso do professor
Nenhum comando (tudo na interface do Azure DevOps). Textos para colar:

- **Project name:** `PetFlow`
- **Description:**
```text
Projeto para entrega da Sprint 4 do professor João Carlos Menk. Integrantes: 561413 - Lucas Grillo Alcântara - 2TDSPX; 561469 - Pietro Ferreira Gomes Abrahamian - 2TDSPX; 561792 - Pedro Peres Benitez - 2TDSPX; 562027 - Lucca Ramos Mussumecci - 2TDSPX
```
- **Professor:** `pf0841@fiap.com.br`, access level **Basic**, grupo **Contributors**.

---

## Parte 4: Código no Azure Repos e política da master
Na tela *Repos → Files* do projeto novo, clique em **Clone** e copie a URL HTTPS. Depois, no terminal, um comando por vez:
```bash
git init -b master
git add .
git status
git commit -m "PetFlow Sprint 4: aplicação, pipelines e scripts de infraestrutura"
git remote add origin "<URL-de-clone-do-Azure-Repos>"
git push -u origin master
```
No `git status`, **confirme que o `.env.azure` não aparece** (está no `.gitignore`). Depois, a política da `master` na interface: *Repos → Branches → master → ⋯ → Branch policies* (1 revisor com "Allow requestors to approve their own changes" e "Check for linked work items: Required").

---

## Parte 5: Segredos (Variable Group) e Service Connection
Nenhum comando (interface do Azure DevOps). Variable Group `petflow-s4-secrets`:

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

Service connection: nome `sc-petflow-s4`, escopo no Resource Group `561413-petflow-s4-devops-rg`, marcando *Grant access permission to all pipelines*.

---

## Parte 6: Pipelines por dentro e primeiro CI/CD
Criar e executar as pipelines é pela interface. Caminhos e nomes:

| Caminho do YAML | Nome da pipeline (exato) |
|---|---|
| `/pipelines/ci.yml` | `petflow-ci` |
| `/pipelines/cd.yml` | `petflow-cd` |
| `/pipelines/mirror-github.yml` | `petflow-mirror-github` |

Só o `petflow-ci` recebe **Run pipeline** (primeira vez); o `petflow-cd` dispara sozinho.

**Quando o CD terminar (verde), no terminal:**
```bash
curl -s https://561413-petflow-s4-api.azurewebsites.net/actuator/health
bash scripts/sql.sh "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name"
```
O health deve responder `{"status":"UP"}` e a segunda consulta lista as tabelas criadas pelo Flyway.

---

## Parte 7: Task e branch pelo Azure Boards
A branch é criada **no Azure Boards** (Task → Development → Create a branch). No terminal, **só** isto:
```bash
git fetch
git checkout <nome-da-branch-criada-pela-task>
git branch --show-current       # confirma que você está na branch da Task
```
Não use `git checkout -b` nem `git branch <nome>`.

---

## Parte 8: Alteração no código
Os testes rodam pelo **painel de testes do VS Code** (não pelo terminal). Crie a pasta do teste:
```bash
mkdir -p src/test/java/br/com/petflow/petflow_api/dto
```

**Arquivo novo:** `src/test/java/br/com/petflow/petflow_api/dto/PetRequestValidationTest.java`
```java
package br.com.petflow.petflow_api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PetRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> invalidFields(String name) {
        PetRequestDTO dto = PetRequestDTO.builder().name(name).tutorId(1L).speciesId(1L).build();
        return validator.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void rejeitaNomeComMenosDeDoisCaracteres() {
        assertThat(invalidFields("A")).containsExactly("name");
    }

    @Test
    void aceitaNomeComDoisCaracteres() {
        assertThat(invalidFields("Bo")).isEmpty();
    }
}
```
Rode `rejeitaNomeComMenosDeDoisCaracteres` pelo painel: **deve falhar**.

**Alteração no código:** em `src/main/java/br/com/petflow/petflow_api/dto/PetRequestDTO.java`, troque
```java
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
```
por
```java
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
```
Rode o teste de novo pelo painel: **deve passar**.

---

## Parte 9: Commit, push, Pull Request e merge
```bash
git status
git add src/
git commit -m "Validar tamanho minimo do nome do pet"
git push
```
Use `git add src/` (e não `git add .`). Se o `git push` reclamar que a branch não tem upstream: `git push -u origin <nome-da-branch-da-task>`.

Depois, **na interface do Azure Repos**: Pull requests → New → vincule a Task → aprove → **Complete** (marque "Complete associated work items"). Não rode mais nada no terminal: o CI dispara sozinho.

---

## Partes 10 e 11: CI e CD automáticos
Nenhum comando. Só a interface do Azure DevOps (Pipelines). **Não clique em Run.**

Para diagnosticar o deploy, só se algo falhar:
```bash
az webapp log tail -g 561413-petflow-s4-devops-rg -n 561413-petflow-s4-api
```

---

## Parte 12: Recursos atualizados no Portal Azure
Nenhum comando. Só o Portal.

---

## Parte 13: App em nuvem com a alteração
Depois do CD verde. URL do Swagger: `https://561413-petflow-s4-api.azurewebsites.net/swagger`

Saúde (opcional, no terminal):
```bash
curl -s https://561413-petflow-s4-api.azurewebsites.net/actuator/health
```

**Swagger → `POST /auth/login`** (a senha do seed vem do README do repo Java; não mostre em tela):
```json
{ "email": "maria@petflow.com", "password": "<senha-do-seed>" }
```
Copie o `token`, clique em **Authorize** e cole. Anote o `id` do tutor (no seed é `2`).

**Swagger → `POST /pets` com nome inválido** (deve dar **400** e a mensagem `O nome deve ter entre 2 e 100 caracteres`):
```json
{ "name": "A", "tutorId": 2, "speciesId": 1 }
```

---

## Parte 14: CRUD completo com prova no banco
Alterne entre o Swagger (nuvem) e o terminal. Em cada passo, primeiro a chamada, depois o `SELECT`.

**14.1 Estado inicial (terminal), esperado 0 linhas**
```bash
bash scripts/sql.sh "SELECT id, name, weight FROM pet WHERE name LIKE 'Demo%'"
```

**14.2 INSERIR (Swagger → `POST /pets`, esperado 201; anote o `id`)**
```json
{ "name": "Demo Rex", "breed": "Labrador", "birthDate": "2021-05-10", "weight": 12.5, "tutorId": 2, "speciesId": 1 }
```
Prova (terminal):
```bash
bash scripts/sql.sh "SELECT id, name, breed, weight, tutor_id, species_id FROM pet WHERE name LIKE 'Demo%'"
```
Bônus, pontos gerados pelo cadastro:
```bash
bash scripts/sql.sh "SELECT id, tutor_id, points, reference_type, reference_id FROM reward_point WHERE tutor_id = 2 ORDER BY id DESC LIMIT 3"
```

**14.3 CONSULTAR (Swagger → `GET /pets/{id}`, esperado 200)**
```bash
bash scripts/sql.sh "SELECT id, name, breed, weight FROM pet WHERE id = <ID>"
```

**14.4 ATUALIZAR (Swagger → `PUT /pets/{id}`, esperado 200)**
```json
{ "name": "Demo Rex Atualizado", "breed": "Labrador", "birthDate": "2021-05-10", "weight": 14.0, "tutorId": 2, "speciesId": 1 }
```
Prova (esperado: nome `Demo Rex Atualizado` e peso `14.00`):
```bash
bash scripts/sql.sh "SELECT id, name, weight FROM pet WHERE id = <ID>"
```

**14.5 DELETAR (Swagger → `DELETE /pets/{id}`, esperado 204; opcional: `GET` → 404)**
Prova (esperado `0`):
```bash
bash scripts/sql.sh "SELECT COUNT(*) AS pets_demo_restantes FROM pet WHERE name LIKE 'Demo%'"
```

As mesmas consultas estão em [`database/crud-verification.sql`](../database/crud-verification.sql):
```bash
bash scripts/sql.sh -f database/crud-verification.sql
```

**Se o `sql.sh` falhar ou demorar**, abra o `psql` direto (digite a senha fora do enquadramento):
```bash
psql "host=561413-petflow-s4-pg.postgres.database.azure.com port=5432 dbname=petflow user=<DB_USERNAME do .env.azure> sslmode=require"
```
e rode os `SELECT` normalmente (`\q` para sair).

---

## Parte 15: Fechamento
```bash
git log --oneline -3
```
Compare o hash com o último commit do repositório espelho no GitHub.

---

## Resolução de problemas durante a gravação

| Sintoma | Comando |
|---|---|
| `sql.sh` recusado por firewall (seu IP mudou) | `curl -s https://api.ipify.org` para ver o IP e depois `bash scripts/01-create-infra.sh` (atualiza a regra `AllowClientIP`) |
| `sql.sh` pede login | `az login` |
| App em nuvem não sobe (health 503/000) | `az webapp log tail -g 561413-petflow-s4-devops-rg -n 561413-petflow-s4-api` |
| Ver os recursos criados | `az resource list -g 561413-petflow-s4-devops-rg -o table` |
| Estou na branch errada | `git branch --show-current` e `git checkout <branch>` |

---

## B. Depois da entrega

Só **depois que o professor corrigir** (o app e o banco precisam estar no ar para ele):
```bash
# remove o acesso do seu IP ao PostgreSQL (opcional, antes de apagar tudo)
az postgres flexible-server firewall-rule delete -g 561413-petflow-s4-devops-rg -n 561413-petflow-s4-pg --rule-name AllowClientIP --yes

# apaga toda a infraestrutura do Sprint 4 (pede para digitar o nome do Resource Group)
bash scripts/99-delete-infra.sh
```
**Não** rode isto antes da correção: o professor perderia o acesso ao app e ao banco.
