-- SELECTs para provar cada operação do CRUD de Pets no PostgreSQL (Azure).
-- Uso: bash scripts/sql.sh -f database/crud-verification.sql   (ou rode cada SELECT separadamente)

-- 0) Antes de tudo: tutor da conta de demonstração e espécies
SELECT ID, NAME, EMAIL FROM TUTOR WHERE EMAIL = 'maria@petflow.com';
SELECT ID, NAME FROM SPECIES ORDER BY ID;

-- 1) INSERT (POST /pets): o pet novo aparece
SELECT ID, NAME, BREED, WEIGHT, TUTOR_ID, SPECIES_ID FROM PET ORDER BY ID DESC LIMIT 5;

-- 2) SELECT (GET /pets/{id}): conferir a linha pelo id retornado pela API
SELECT ID, NAME, BREED, WEIGHT FROM PET WHERE NAME LIKE 'Demo%';

-- 3) UPDATE (PUT /pets/{id}): nome e peso novos
SELECT ID, NAME, BREED, WEIGHT FROM PET WHERE NAME LIKE 'Demo%';

-- 4) DELETE (DELETE /pets/{id}): nenhuma linha restante
SELECT COUNT(*) AS PETS_DEMO_RESTANTES FROM PET WHERE NAME LIKE 'Demo%';
