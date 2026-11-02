-- =====================================================================
-- Dados de referência e demonstração.
-- Contas demonstrativas usam hashes BCrypt; credenciais não são documentadas aqui.
-- =====================================================================

INSERT INTO species (name, description) VALUES ('Cachorro', 'Canino doméstico');
INSERT INTO species (name, description) VALUES ('Gato', 'Felino doméstico');
INSERT INTO species (name, description) VALUES ('Ave', 'Aves ornamentais e de companhia');
INSERT INTO species (name, description) VALUES ('Roedor', 'Hamsters, coelhos e afins');

INSERT INTO event_type (name, points_reward, category) VALUES ('Vacinação', 10, 'PREVENTIVO');
INSERT INTO event_type (name, points_reward, category) VALUES ('Consulta de rotina', 5, 'PREVENTIVO');
INSERT INTO event_type (name, points_reward, category) VALUES ('Vermifugação', 5, 'PREVENTIVO');
INSERT INTO event_type (name, points_reward, category) VALUES ('Cirurgia', 0, 'TRATAMENTO');

INSERT INTO reward_action (name, points_value, description) VALUES ('CADASTRO_PET', 5, 'Pontos por cadastrar um novo pet');
INSERT INTO reward_action (name, points_value, description) VALUES ('EVENTO_SAUDE_REALIZADO', 10, 'Pontos por evento de saúde concluído');
INSERT INTO reward_action (name, points_value, description) VALUES ('ASSINATURA_ATIVA', 15, 'Pontos por assinatura de plano ativa');
INSERT INTO reward_action (name, points_value, description) VALUES ('RESGATE_CUPOM', 0, 'Consumo de pontos para resgate de cupom');

INSERT INTO risk_level (name, description, min_score, max_score) VALUES ('BAIXO', 'Baixo risco de saúde', 0, 30);
INSERT INTO risk_level (name, description, min_score, max_score) VALUES ('MEDIO', 'Risco moderado, atenção recomendada', 31, 60);
INSERT INTO risk_level (name, description, min_score, max_score) VALUES ('ALTO', 'Alto risco, acompanhamento necessário', 61, 100);

-- Usuário administrador do sistema
INSERT INTO tutor (name, role, email, phone, password_hash)
VALUES ('Administrador PetFlow', 'ADMIN', 'admin@petflow.com', '(11) 90000-0000',
        '$2b$10$h5iXTBt/JpyOL/.UE2bmM.QvC3W/wCSb7M75bMI1Y.6Fn7D1cVWjS');

-- Tutor de demonstração
INSERT INTO tutor (name, role, email, phone, password_hash)
VALUES ('Maria Souza', 'TUTOR', 'maria@petflow.com', '11-98001-0001',
        '$2b$10$/vPZqCCwPJSroSMducM2T.ZEgB9RFu2vz4K0jAoy3o10erMPVqJmu');

-- Clínica e planos de demonstração
INSERT INTO clinic (name, address, phone, cnpj)
VALUES ('Clínica Amigo Fiel', 'Av. dos Pets, 500 - São Paulo/SP', '(11) 3333-4444', '12.345.678/0001-90');

INSERT INTO plan (clinic_id, name, description, price, duration_days, points_per_event)
VALUES (1, 'Plano Essencial', 'Consultas e vacinas básicas', 79.90, 365, 1);

INSERT INTO plan (clinic_id, name, description, price, duration_days, points_per_event)
VALUES (1, 'Plano Premium', 'Cobertura completa com pontuação em dobro', 149.90, 365, 2);

-- Parceiro de desconto
INSERT INTO partner_discount (clinic_id, partner_name, category, discount_percent)
VALUES (1, 'PetShop Amigo Fiel', 'PETSHOP', 15.00);

-- Templates de cupom (sem description)
INSERT INTO coupon_template (partner_discount_id, title, discount_value, discount_type, points_required)
VALUES (1, '15% OFF em ração', 15.00, 'PERCENTUAL', 20);

INSERT INTO coupon_template (partner_discount_id, title, discount_value, discount_type, points_required)
VALUES (1, 'R$20 OFF em consultas', 20.00, 'VALOR_FIXO', 50);

INSERT INTO coupon_template (partner_discount_id, title, discount_value, discount_type, points_required)
VALUES (1, '10% OFF em medicamentos', 10.00, 'PERCENTUAL', 30);

INSERT INTO coupon_template (partner_discount_id, title, discount_value, discount_type, points_required)
VALUES (1, 'Banho e Tosa Grátis', 80.00, 'VALOR_FIXO', 100);

-- Cupons disponíveis
INSERT INTO coupon (template_id, code, status, expiration_date)
VALUES (1, 'RACAO15-A1', 'DISPONIVEL', DATE '2027-12-31');

INSERT INTO coupon (template_id, code, status, expiration_date)
VALUES (1, 'RACAO15-A2', 'DISPONIVEL', DATE '2027-12-31');

INSERT INTO coupon (template_id, code, status, expiration_date)
VALUES (2, 'CONSULTA20-A1', 'DISPONIVEL', DATE '2027-12-31');

INSERT INTO coupon (template_id, code, status, expiration_date)
VALUES (3, 'MED10-A1', 'DISPONIVEL', DATE '2027-12-31');

INSERT INTO coupon (template_id, code, status, expiration_date)
VALUES (4, 'BANHO80-A1', 'DISPONIVEL', DATE '2027-12-31');

-- Pontos iniciais para o tutor Ana Lima (para testar resgate)
INSERT INTO reward_point (tutor_id, reward_action_id, points, reference_type, reference_id)
VALUES (2, 1, 100, 'SISTEMA', NULL);