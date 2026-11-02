# Alteração de código da demonstração

O enunciado exige uma alteração **no código** (nunca no README) feita na branch criada a partir da Task. Esta é a mudança, já pensada para ser visível no app em nuvem. **Ela não está aplicada no repositório**: aplique só durante a gravação.

## O problema

Hoje `PetRequestDTO.name` aceita qualquer nome não vazio, até `"A"`. A regra nova exige **no mínimo 2 caracteres** e responde **HTTP 400** com a mensagem nova.

## Como mostrar o "antes" sem testar o app antes do deploy

O professor pediu (feedback do CP6) que os testes do app e da persistência aconteçam **depois do deploy automático**. Por isso a prova do "antes" é o **teste unitário falhando no VS Code** (painel de testes, sem terminal), e a prova do "depois" é o app em nuvem recusando o nome `"A"` com **400**, depois do CD. Não chame o app em nuvem antes da pipeline rodar.

## Passo 1: escrever o teste (ele vai falhar)

Crie `src/test/java/br/com/petflow/petflow_api/dto/PetRequestValidationTest.java`:

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

Rode só esse teste pelo painel de testes do VS Code (ícone de frasco, ou o botão *Run Test* acima do método): `rejeitaNomeComMenosDeDoisCaracteres` **falha** (a regra ainda não existe). Isso é execução de teste, não build.

## Passo 2: implementar a regra

Em `src/main/java/br/com/petflow/petflow_api/dto/PetRequestDTO.java`, troque:

```java
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    String name,
```

por:

```java
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    String name,
```

Rode o teste de novo pelo painel: **passa**. A suíte inteira (188 testes antes, 190 depois) é executada pelo CI, não por você.

## Passo 3: entregar pela pipeline (sem build manual)

Siga a seção 8 de [azure-devops.md](azure-devops.md): commit e push na branch da Task, PR, aprovação, merge. **Não rode build, deploy nem `az` pelo terminal**: o CI e o CD fazem tudo.

## Passo 4: prova na nuvem (depois do CD terminar)

`POST /pets` com `"name": "A"` agora retorna **400** com `"O nome deve ter entre 2 e 100 caracteres"`. Em seguida faça o CRUD completo (inserir, consultar, atualizar, deletar) com um `SELECT` no PostgreSQL a cada passo (`database/crud-verification.sql`).
