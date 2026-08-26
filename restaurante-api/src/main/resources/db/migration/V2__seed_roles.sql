INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'Administrador del restaurante'),
    ('WAITER', 'Mesero'),
    ('KITCHEN', 'Personal de cocina'),
    ('CASHIER', 'Cajero')
ON CONFLICT (name) DO NOTHING;
