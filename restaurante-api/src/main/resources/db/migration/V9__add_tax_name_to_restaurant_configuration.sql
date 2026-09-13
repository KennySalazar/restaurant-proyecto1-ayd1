SET search_path TO restaurante, public;

ALTER TABLE configuraciones_restaurante
    ADD COLUMN nombre_impuesto VARCHAR(80) NOT NULL DEFAULT 'IVA';

ALTER TABLE configuraciones_restaurante
    ADD CONSTRAINT chk_config_nombre_impuesto
        CHECK (NULLIF(BTRIM(nombre_impuesto), '') IS NOT NULL);