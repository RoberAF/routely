-- Galicia demo data: 3 sales reps, 4 app users, 40 customers (Lugo city + wider
-- Lugo/Ourense province towns at least 15km from the Lugo depot).

INSERT INTO sales_rep (id, name, email, home_base) VALUES
    (1, 'Laura Castro', 'laura.castro@routely.dev', ST_SetSRID(ST_MakePoint(-7.5559, 43.0121), 4326)),
    (2, 'Brais Seoane', 'brais.seoane@routely.dev', ST_SetSRID(ST_MakePoint(-7.5145, 42.5219), 4326)),
    (3, 'Antía Vázquez', 'antia.vazquez@routely.dev', ST_SetSRID(ST_MakePoint(-7.8639, 42.3358), 4326));

-- Demo passwords (documented, pinned by SeedPasswordsTest): admin123 / manager123 / laura123 / brais123
INSERT INTO app_user (id, email, password_hash, role, sales_rep_id) VALUES
    (1, 'admin@routely.dev', '$2a$10$lkx0vKAiPyb/bzKfuqFAkek0Yo8xtILpbQtSC.I2rK7x1KWZpOz4.', 'ADMIN', NULL),
    (2, 'manager@routely.dev', '$2a$10$ISEHnLtAeO0L7HtHh0ZSR.1BulWOKzCqbTcrgqMh1ShOPzg109kCW', 'MANAGER', NULL),
    (3, 'laura@routely.dev', '$2a$10$s9RI0osAvApT5zRJRg2BzeHmx0s7031Y5Fybm1WOY3OjFJyOpnQlC', 'REP', 1),
    (4, 'brais@routely.dev', '$2a$10$SBsq.Z4QpfhLFihvnerpjOtdeznlHtly6Onsd61mQbMmzHFGA1RcW', 'REP', 2);

INSERT INTO customer (id, name, address, location, priority, time_window_open, time_window_close, active) VALUES
    -- Lugo city (pinned rows, other packages assert on these)
    (1, 'Cafetería Rúa Nova', 'Rúa Nova 12, Lugo', ST_SetSRID(ST_MakePoint(-7.5560, 43.0096), 4326), 'NORMAL', NULL, NULL, TRUE),
    (2, 'Estanco Porta Miñá', 'Porta Miñá 3, Lugo', ST_SetSRID(ST_MakePoint(-7.5620, 43.0080), 4326), 'NORMAL', '09:30', '13:30', TRUE),
    (3, 'Ferretería As Termas', 'Rúa das Termas 5, Lugo', ST_SetSRID(ST_MakePoint(-7.5490, 43.0205), 4326), 'KEY', NULL, NULL, TRUE),
    (4, 'Panadería O Ceao', 'Rúa Illa de Sálvora 2, O Ceao, Lugo', ST_SetSRID(ST_MakePoint(-7.5710, 43.0410), 4326), 'LOW', NULL, NULL, TRUE),
    (5, 'Quiosco Parque Rosalía', 'Parque Rosalía de Castro, Lugo', ST_SetSRID(ST_MakePoint(-7.5580, 43.0110), 4326), 'NORMAL', NULL, NULL, FALSE),

    -- Wider Lugo/Ourense province, all >= 15km from the Lugo depot
    (6, 'Bazar Monforte Centro', 'Rúa Cardenal Rodrigo de Castro 8, Monforte de Lemos', ST_SetSRID(ST_MakePoint(-7.5150, 42.5222), 4326), 'NORMAL', NULL, NULL, TRUE),
    (7, 'Panadería Camiño Francés', 'Rúa Maior 22, Sarria', ST_SetSRID(ST_MakePoint(-7.4116, 42.7791), 4326), 'KEY', NULL, NULL, TRUE),
    (8, 'Ferretería Chantada', 'Praza do Concello 4, Chantada', ST_SetSRID(ST_MakePoint(-7.7735, 42.6067), 4326), 'NORMAL', '09:00', '13:30', TRUE),
    (9, 'Frutería Valdeorras', 'Avenida de Ourense 15, O Barco de Valdeorras', ST_SetSRID(ST_MakePoint(-6.9801, 42.4178), 4326), 'LOW', NULL, NULL, TRUE),
    (10, 'Librería Pontevella', 'Rúa do Paseo 30, Ourense', ST_SetSRID(ST_MakePoint(-7.8618, 42.3382), 4326), 'KEY', NULL, NULL, TRUE),
    (11, 'Carnicería Terra Cha', 'Rúa Pardo de Cela 11, Vilalba', ST_SetSRID(ST_MakePoint(-7.6787, 43.3010), 4326), 'KEY', NULL, NULL, TRUE),
    (12, 'Óptica Ribadeo', 'Rúa Reinante 9, Ribadeo', ST_SetSRID(ST_MakePoint(-7.0382, 43.5393), 4326), 'NORMAL', NULL, NULL, TRUE),
    (13, 'Farmacia Viveiro', 'Rúa Nicolás Cora Montenegro 6, Viveiro', ST_SetSRID(ST_MakePoint(-7.5918, 43.6636), 4326), 'NORMAL', '16:00', '19:30', TRUE),
    (14, 'Droguería Mondoñedo', 'Rúa do Seminario 3, Mondoñedo', ST_SetSRID(ST_MakePoint(-7.3612, 43.4303), 4326), 'LOW', NULL, NULL, TRUE),
    (15, 'Bar O Cruceiro', 'Rúa do Camiño 14, Palas de Rei', ST_SetSRID(ST_MakePoint(-7.8668, 42.8752), 4326), 'NORMAL', NULL, NULL, TRUE),
    (16, 'Taller Becerreá', 'Rúa da Fonte 7, Becerreá', ST_SetSRID(ST_MakePoint(-7.1572, 42.8579), 4326), 'NORMAL', NULL, NULL, TRUE),
    (17, 'Supermercado A Fonsagrada', 'Rúa do Hospital 5, A Fonsagrada', ST_SetSRID(ST_MakePoint(-7.0657, 43.1269), 4326), 'NORMAL', '09:00', '13:30', TRUE),
    (18, 'Peluquería Quiroga', 'Rúa Antonio López Pérez 2, Quiroga', ST_SetSRID(ST_MakePoint(-7.2686, 42.4775), 4326), 'LOW', NULL, NULL, TRUE),
    (19, 'Papelería A Rúa', 'Avenida de Galicia 18, A Rúa', ST_SetSRID(ST_MakePoint(-7.1099, 42.3953), 4326), 'NORMAL', NULL, NULL, TRUE),
    (20, 'Zapatería Verín', 'Rúa Sousas 10, Verín', ST_SetSRID(ST_MakePoint(-7.4358, 41.9432), 4326), 'NORMAL', '16:00', '19:30', TRUE),
    (21, 'Autoescuela Limia', 'Rúa Antonio Prada 12, Xinzo de Limia', ST_SetSRID(ST_MakePoint(-7.7223, 42.0653), 4326), 'NORMAL', NULL, NULL, TRUE),
    (22, 'Gestoría Allariz', 'Rúa do Portelo 6, Allariz', ST_SetSRID(ST_MakePoint(-7.7993, 42.1924), 4326), 'NORMAL', '09:00', '13:30', TRUE),
    (23, 'Clínica Veterinaria O Carballiño', 'Avenida de Ourense 21, O Carballiño', ST_SetSRID(ST_MakePoint(-8.0768, 42.4328), 4326), 'KEY', NULL, NULL, TRUE),
    (24, 'Cafetería Ribadavia', 'Praza Maior 3, Ribadavia', ST_SetSRID(ST_MakePoint(-8.1413, 42.2901), 4326), 'LOW', NULL, NULL, TRUE),
    (25, 'Restaurante Celanova', 'Rúa Vilar 8, Celanova', ST_SetSRID(ST_MakePoint(-7.9548, 42.1542), 4326), 'NORMAL', NULL, NULL, TRUE),
    (26, 'Estanco Castro Caldelas', 'Rúa do Castelo 1, Castro Caldelas', ST_SetSRID(ST_MakePoint(-7.4130, 42.3779), 4326), 'LOW', NULL, NULL, TRUE),
    (27, 'Panadería Portomarín', 'Avenida de Sarria 4, Portomarín', ST_SetSRID(ST_MakePoint(-7.6139, 42.8097), 4326), 'NORMAL', '16:00', '19:30', TRUE),
    (28, 'Ferretería Monterroso', 'Rúa Xoán XXIII 9, Monterroso', ST_SetSRID(ST_MakePoint(-7.8318, 42.7942), 4326), 'NORMAL', NULL, NULL, TRUE),
    (29, 'Peixería Foz', 'Rúa Isidoro Velo 5, Foz', ST_SetSRID(ST_MakePoint(-7.2561, 43.5720), 4326), 'KEY', NULL, NULL, TRUE),
    (30, 'Frutería Burela', 'Rúa Marqués de Amboage 7, Burela', ST_SetSRID(ST_MakePoint(-7.3543, 43.6592), 4326), 'NORMAL', '09:00', '13:30', TRUE),
    (31, 'Adega Sober', 'Rúa da Ribeira Sacra 2, Sober', ST_SetSRID(ST_MakePoint(-7.5858, 42.4632), 4326), 'LOW', NULL, NULL, TRUE),
    (32, 'Bazar Taboada', 'Rúa Principal 11, Taboada', ST_SetSRID(ST_MakePoint(-7.7598, 42.7172), 4326), 'NORMAL', NULL, NULL, TRUE),
    (33, 'Farmacia Monforte', 'Rúa Roberto Baamonde 3, Monforte de Lemos', ST_SetSRID(ST_MakePoint(-7.5122, 42.5195), 4326), 'NORMAL', '16:00', '19:30', TRUE),
    (34, 'Papelería Ourense', 'Rúa Paz Novoa 16, Ourense', ST_SetSRID(ST_MakePoint(-7.8663, 42.3334), 4326), 'NORMAL', NULL, NULL, TRUE),
    (35, 'Carnicería Sarria', 'Rúa Formigueiro 6, Sarria', ST_SetSRID(ST_MakePoint(-7.4164, 42.7746), 4326), 'NORMAL', '09:00', '13:30', TRUE),
    (36, 'Cafetería Terra Cha', 'Rúa Ganado 4, Vilalba', ST_SetSRID(ST_MakePoint(-7.6833, 43.2964), 4326), 'NORMAL', NULL, NULL, TRUE),
    (37, 'Óptica Chantada', 'Rúa Otero Pedrayo 9, Chantada', ST_SetSRID(ST_MakePoint(-7.7686, 42.6113), 4326), 'LOW', NULL, NULL, TRUE),
    (38, 'Droguería Verín', 'Rúa Luís Espada 8, Verín', ST_SetSRID(ST_MakePoint(-7.4404, 41.9386), 4326), 'NORMAL', '16:00', '19:30', TRUE),
    (39, 'Taller O Carballiño', 'Avenida Buenos Aires 14, O Carballiño', ST_SetSRID(ST_MakePoint(-8.0814, 42.4282), 4326), 'NORMAL', NULL, NULL, TRUE),
    (40, 'Estanco Ribadeo', 'Rúa San Roque 5, Ribadeo', ST_SetSRID(ST_MakePoint(-7.0429, 43.5346), 4326), 'NORMAL', '09:00', '13:30', TRUE);

SELECT setval(pg_get_serial_sequence('sales_rep', 'id'), (SELECT MAX(id) FROM sales_rep));
SELECT setval(pg_get_serial_sequence('app_user', 'id'), (SELECT MAX(id) FROM app_user));
SELECT setval(pg_get_serial_sequence('customer', 'id'), (SELECT MAX(id) FROM customer));
