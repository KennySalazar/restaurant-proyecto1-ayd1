
SET search_path TO restaurante, public;
SET TIME ZONE 'UTC';



CREATE OR REPLACE FUNCTION fn_actualizar_timestamp()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    NEW.actualizado_en := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_restaurantes_actualizado_en
BEFORE UPDATE ON restaurantes
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_usuarios_actualizado_en
BEFORE UPDATE ON usuarios
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_categorias_insumo_actualizado_en
BEFORE UPDATE ON categorias_insumo
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_insumos_actualizado_en
BEFORE UPDATE ON insumos
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_platillos_actualizado_en
BEFORE UPDATE ON platillos
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_modificadores_actualizado_en
BEFORE UPDATE ON modificadores
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_combos_actualizado_en
BEFORE UPDATE ON combos
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_mesas_actualizado_en
BEFORE UPDATE ON mesas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_clientes_actualizado_en
BEFORE UPDATE ON clientes
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_reservas_actualizado_en
BEFORE UPDATE ON reservas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_lista_espera_actualizado_en
BEFORE UPDATE ON lista_espera
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_cuentas_actualizado_en
BEFORE UPDATE ON cuentas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_subcuentas_actualizado_en
BEFORE UPDATE ON subcuentas
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_comanda_detalles_actualizado_en
BEFORE UPDATE ON comanda_detalles
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_secuencias_facturacion_actualizado_en
BEFORE UPDATE ON secuencias_facturacion
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE OR REPLACE FUNCTION fn_proteger_configuracion_restaurante()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'Las configuraciones se historizan y no pueden eliminarse';
    END IF;

    IF OLD.estado = 'HISTORICA' THEN
        RAISE EXCEPTION 'Una configuracion HISTORICA es inmutable';
    END IF;

    IF NEW.estado = OLD.estado THEN
        RAISE EXCEPTION
            'Para cambiar la configuracion cree una nueva version vigente';
    END IF;

    IF OLD.estado <> 'VIGENTE' OR NEW.estado <> 'HISTORICA' THEN
        RAISE EXCEPTION 'Transicion de configuracion no valida';
    END IF;

    IF (to_jsonb(NEW) - ARRAY[
            'estado', 'vigente_hasta'
        ]) IS DISTINCT FROM
       (to_jsonb(OLD) - ARRAY[
            'estado', 'vigente_hasta'
        ]) THEN
        RAISE EXCEPTION
            'No se pueden alterar los valores de una configuracion publicada';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_proteger_configuracion_restaurante
BEFORE UPDATE OR DELETE ON configuraciones_restaurante
FOR EACH ROW EXECUTE FUNCTION fn_proteger_configuracion_restaurante();

CREATE OR REPLACE FUNCTION fn_validar_unidad_insumo(
    p_insumo_id BIGINT,
    p_unidad_medida_id SMALLINT
)
RETURNS VOID
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_dimension_stock  VARCHAR(15);
    v_dimension_receta VARCHAR(15);
BEGIN
    SELECT us.dimension, ur.dimension
      INTO v_dimension_stock, v_dimension_receta
      FROM insumos i
      JOIN unidades_medida us ON us.id = i.unidad_stock_id
      JOIN unidades_medida ur ON ur.id = p_unidad_medida_id
     WHERE i.id = p_insumo_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Insumo o unidad de medida inexistente';
    END IF;

    IF v_dimension_stock <> v_dimension_receta THEN
        RAISE EXCEPTION
            'La unidad de receta no es compatible con la unidad de stock del insumo';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION fn_validar_receta_detalle()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado                 VARCHAR(15);
    v_restaurante_platillo   BIGINT;
    v_restaurante_insumo     BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT estado INTO v_estado
          FROM receta_versiones
         WHERE id = OLD.receta_version_id;

        IF FOUND AND v_estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'Solo se pueden modificar detalles de una receta en BORRADOR';
        END IF;

        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        SELECT estado INTO v_estado
          FROM receta_versiones
         WHERE id = OLD.receta_version_id;

        IF NOT FOUND OR v_estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'Solo se pueden modificar detalles de una receta en BORRADOR';
        END IF;
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT estado INTO v_estado
          FROM receta_versiones
         WHERE id = NEW.receta_version_id;

        IF NOT FOUND OR v_estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'Solo se pueden agregar detalles a una receta en BORRADOR';
        END IF;

        PERFORM fn_validar_unidad_insumo(NEW.insumo_id, NEW.unidad_medida_id);

        SELECT p.restaurante_id, i.restaurante_id
          INTO v_restaurante_platillo, v_restaurante_insumo
          FROM receta_versiones rv
          JOIN platillos p ON p.id = rv.platillo_id
          JOIN insumos i ON i.id = NEW.insumo_id
         WHERE rv.id = NEW.receta_version_id;

        IF v_restaurante_platillo <> v_restaurante_insumo THEN
            RAISE EXCEPTION
                'El insumo no pertenece al restaurante del platillo';
        END IF;

        RETURN NEW;
    END IF;

    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_validar_receta_detalle
BEFORE INSERT OR UPDATE OR DELETE ON receta_detalles
FOR EACH ROW EXECUTE FUNCTION fn_validar_receta_detalle();

CREATE OR REPLACE FUNCTION fn_validar_receta_modificador_detalle()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado                   VARCHAR(15);
    v_restaurante_modificador  BIGINT;
    v_restaurante_insumo       BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT estado INTO v_estado
          FROM receta_modificador_versiones
         WHERE id = OLD.receta_modificador_version_id;

        IF FOUND AND v_estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'Solo se pueden modificar detalles de una receta de modificador en BORRADOR';
        END IF;

        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        SELECT estado INTO v_estado
          FROM receta_modificador_versiones
         WHERE id = OLD.receta_modificador_version_id;

        IF NOT FOUND OR v_estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'Solo se pueden modificar detalles de una receta de modificador en BORRADOR';
        END IF;
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT estado INTO v_estado
          FROM receta_modificador_versiones
         WHERE id = NEW.receta_modificador_version_id;

        IF NOT FOUND OR v_estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'Solo se pueden agregar detalles a una receta de modificador en BORRADOR';
        END IF;

        PERFORM fn_validar_unidad_insumo(NEW.insumo_id, NEW.unidad_medida_id);

        SELECT m.restaurante_id, i.restaurante_id
          INTO v_restaurante_modificador, v_restaurante_insumo
          FROM receta_modificador_versiones rmv
          JOIN modificadores m ON m.id = rmv.modificador_id
          JOIN insumos i ON i.id = NEW.insumo_id
         WHERE rmv.id = NEW.receta_modificador_version_id;

        IF v_restaurante_modificador <> v_restaurante_insumo THEN
            RAISE EXCEPTION
                'El insumo no pertenece al restaurante del modificador';
        END IF;

        RETURN NEW;
    END IF;

    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_validar_receta_modificador_detalle
BEFORE INSERT OR UPDATE OR DELETE ON receta_modificador_detalles
FOR EACH ROW EXECUTE FUNCTION fn_validar_receta_modificador_detalle();

CREATE OR REPLACE FUNCTION fn_validar_estado_receta_version()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'Una receta nueva debe crearse en estado BORRADOR';
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        IF OLD.estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'No se puede eliminar una receta VIGENTE o HISTORICA';
        END IF;
        RETURN OLD;
    END IF;

    IF OLD.estado = 'HISTORICA' THEN
        RAISE EXCEPTION 'Una receta HISTORICA es inmutable';
    END IF;

    IF OLD.estado = 'VIGENTE' THEN
        IF NEW.estado <> 'HISTORICA' THEN
            RAISE EXCEPTION
                'Una receta VIGENTE solo puede pasar a HISTORICA';
        END IF;

        IF NEW.platillo_id <> OLD.platillo_id
           OR NEW.numero_version <> OLD.numero_version
           OR NEW.vigente_desde IS DISTINCT FROM OLD.vigente_desde
           OR NEW.creado_por_id IS DISTINCT FROM OLD.creado_por_id
           OR NEW.creado_en IS DISTINCT FROM OLD.creado_en THEN
            RAISE EXCEPTION
                'No se pueden alterar los datos historicos de una receta VIGENTE';
        END IF;

        RETURN NEW;
    END IF;

    IF OLD.estado = 'BORRADOR' AND NEW.estado = 'VIGENTE' THEN
        IF NOT EXISTS (
            SELECT 1
              FROM receta_detalles rd
             WHERE rd.receta_version_id = OLD.id
        ) THEN
            RAISE EXCEPTION
                'No se puede publicar una receta sin insumos';
        END IF;
    ELSIF NEW.estado <> 'BORRADOR' THEN
        RAISE EXCEPTION
            'La transicion de estado solicitada para la receta no es valida';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_estado_receta_version
BEFORE INSERT OR UPDATE OR DELETE ON receta_versiones
FOR EACH ROW EXECUTE FUNCTION fn_validar_estado_receta_version();

CREATE OR REPLACE FUNCTION fn_validar_estado_receta_modificador_version()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'Una receta de modificador nueva debe crearse en BORRADOR';
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        IF OLD.estado <> 'BORRADOR' THEN
            RAISE EXCEPTION
                'No se puede eliminar una receta de modificador publicada';
        END IF;
        RETURN OLD;
    END IF;

    IF OLD.estado = 'HISTORICA' THEN
        RAISE EXCEPTION
            'Una receta de modificador HISTORICA es inmutable';
    END IF;

    IF OLD.estado = 'VIGENTE' THEN
        IF NEW.estado <> 'HISTORICA' THEN
            RAISE EXCEPTION
                'Una receta de modificador VIGENTE solo puede pasar a HISTORICA';
        END IF;

        IF NEW.modificador_id <> OLD.modificador_id
           OR NEW.numero_version <> OLD.numero_version
           OR NEW.vigente_desde IS DISTINCT FROM OLD.vigente_desde
           OR NEW.creado_por_id IS DISTINCT FROM OLD.creado_por_id
           OR NEW.creado_en IS DISTINCT FROM OLD.creado_en THEN
            RAISE EXCEPTION
                'No se pueden alterar los datos historicos del modificador';
        END IF;

        RETURN NEW;
    END IF;

    IF OLD.estado = 'BORRADOR' AND NEW.estado = 'VIGENTE' THEN
        IF NOT EXISTS (
            SELECT 1
              FROM receta_modificador_detalles rmd
             WHERE rmd.receta_modificador_version_id = OLD.id
        ) THEN
            RAISE EXCEPTION
                'No se puede publicar una receta de modificador sin insumos';
        END IF;
    ELSIF NEW.estado <> 'BORRADOR' THEN
        RAISE EXCEPTION
            'La transicion de la receta de modificador no es valida';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_estado_receta_modificador_version
BEFORE INSERT OR UPDATE OR DELETE ON receta_modificador_versiones
FOR EACH ROW EXECUTE FUNCTION fn_validar_estado_receta_modificador_version();

CREATE OR REPLACE FUNCTION fn_validar_usuario_operacion(
    p_usuario_id BIGINT,
    p_restaurante_id BIGINT,
    p_rol_requerido VARCHAR(30) DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante BIGINT;
    v_rol         VARCHAR(30);
    v_activo      BOOLEAN;
BEGIN
    IF p_usuario_id IS NULL THEN
        RETURN;
    END IF;

    SELECT u.restaurante_id, r.name, au.enabled
      INTO v_restaurante, v_rol, v_activo
      FROM usuarios u
      JOIN public.app_users au ON au.id = u.id
      JOIN public.roles r ON r.id = au.role_id
     WHERE u.id = p_usuario_id;

    IF NOT FOUND OR NOT v_activo THEN
        RAISE EXCEPTION 'El usuario de la operacion no existe o esta inactivo';
    END IF;

    IF v_restaurante <> p_restaurante_id THEN
        RAISE EXCEPTION
            'El usuario no pertenece al restaurante de la operacion';
    END IF;

    IF p_rol_requerido IS NOT NULL AND v_rol <> p_rol_requerido THEN
        RAISE EXCEPTION
            'La operacion requiere el rol %, pero el usuario tiene rol %',
            p_rol_requerido, v_rol;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION fn_validar_catalogo_restaurante()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante_padre BIGINT;
    v_restaurante_hijo  BIGINT;
BEGIN
    CASE TG_TABLE_NAME
        WHEN 'insumos' THEN
            SELECT restaurante_id INTO v_restaurante_padre
              FROM categorias_insumo
             WHERE id = NEW.categoria_insumo_id;
            v_restaurante_hijo := NEW.restaurante_id;

        WHEN 'platillos' THEN
            SELECT restaurante_id INTO v_restaurante_padre
              FROM categorias_platillo
             WHERE id = NEW.categoria_platillo_id;
            v_restaurante_hijo := NEW.restaurante_id;

        WHEN 'platillo_modificadores' THEN
            SELECT restaurante_id INTO v_restaurante_padre
              FROM platillos
             WHERE id = NEW.platillo_id;
            SELECT restaurante_id INTO v_restaurante_hijo
              FROM modificadores
             WHERE id = NEW.modificador_id;

        WHEN 'combo_detalles' THEN
            SELECT restaurante_id INTO v_restaurante_padre
              FROM combos
             WHERE id = NEW.combo_id;
            SELECT restaurante_id INTO v_restaurante_hijo
              FROM platillos
             WHERE id = NEW.platillo_id;

        WHEN 'mesas' THEN
            SELECT restaurante_id INTO v_restaurante_padre
              FROM zonas_mesa
             WHERE id = NEW.zona_mesa_id;
            v_restaurante_hijo := NEW.restaurante_id;

        ELSE
            RAISE EXCEPTION
                'La tabla % no esta soportada por la validacion de catalogos',
                TG_TABLE_NAME;
    END CASE;

    IF v_restaurante_padre IS DISTINCT FROM v_restaurante_hijo THEN
        RAISE EXCEPTION
            'Las entidades relacionadas deben pertenecer al mismo restaurante';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_insumo_restaurante
BEFORE INSERT OR UPDATE ON insumos
FOR EACH ROW EXECUTE FUNCTION fn_validar_catalogo_restaurante();

CREATE TRIGGER trg_platillo_restaurante
BEFORE INSERT OR UPDATE ON platillos
FOR EACH ROW EXECUTE FUNCTION fn_validar_catalogo_restaurante();

CREATE TRIGGER trg_platillo_modificador_restaurante
BEFORE INSERT OR UPDATE ON platillo_modificadores
FOR EACH ROW EXECUTE FUNCTION fn_validar_catalogo_restaurante();

CREATE TRIGGER trg_combo_detalle_restaurante
BEFORE INSERT OR UPDATE ON combo_detalles
FOR EACH ROW EXECUTE FUNCTION fn_validar_catalogo_restaurante();

CREATE TRIGGER trg_mesa_restaurante
BEFORE INSERT OR UPDATE ON mesas
FOR EACH ROW EXECUTE FUNCTION fn_validar_catalogo_restaurante();

CREATE OR REPLACE FUNCTION fn_validar_reserva()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_capacidad        SMALLINT;
    v_restaurante_mesa BIGINT;
    v_mesa_activa      BOOLEAN;
    v_restaurante_ref  BIGINT;
BEGIN
    IF TG_OP = 'INSERT' AND NEW.estado NOT IN ('PENDIENTE', 'CONFIRMADA') THEN
        RAISE EXCEPTION
            'Una reserva nueva debe iniciar PENDIENTE o CONFIRMADA';
    END IF;

    IF TG_OP = 'UPDATE' AND NEW.estado IS DISTINCT FROM OLD.estado THEN
        IF NOT (
            (OLD.estado = 'PENDIENTE'
                AND NEW.estado IN ('CONFIRMADA', 'CANCELADA'))
            OR (OLD.estado = 'CONFIRMADA'
                AND NEW.estado IN (
                    'CLIENTE_PRESENTE', 'CANCELADA', 'NO_ASISTIO'
                ))
            OR (OLD.estado = 'CLIENTE_PRESENTE'
                AND NEW.estado IN ('ATENDIDA', 'CANCELADA'))
        ) THEN
            RAISE EXCEPTION
                'Transicion de reserva no valida: % -> %',
                OLD.estado, NEW.estado;
        END IF;
    END IF;

    IF NEW.estado = 'CONFIRMADA' AND NEW.confirmada_en IS NULL THEN
        NEW.confirmada_en := CURRENT_TIMESTAMP;
    ELSIF NEW.estado = 'CLIENTE_PRESENTE' AND NEW.llegada_en IS NULL THEN
        NEW.llegada_en := CURRENT_TIMESTAMP;
    ELSIF NEW.estado = 'CANCELADA' AND NEW.cancelada_en IS NULL THEN
        NEW.cancelada_en := CURRENT_TIMESTAMP;
    END IF;

    SELECT capacidad, restaurante_id, activo
      INTO v_capacidad, v_restaurante_mesa, v_mesa_activa
      FROM mesas
     WHERE id = NEW.mesa_id
     FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La mesa indicada no existe';
    END IF;

    IF NOT v_mesa_activa THEN
        RAISE EXCEPTION 'No se puede reservar una mesa inactiva';
    END IF;

    IF v_restaurante_mesa <> NEW.restaurante_id THEN
        RAISE EXCEPTION
            'La mesa no pertenece al restaurante de la reserva';
    END IF;

    IF NEW.cantidad_personas > v_capacidad THEN
        RAISE EXCEPTION
            'La capacidad de la mesa es insuficiente para la reserva';
    END IF;

    IF NEW.estado IN ('PENDIENTE', 'CONFIRMADA', 'CLIENTE_PRESENTE')
       AND CURRENT_TIMESTAMP >= NEW.fecha_hora_inicio
       AND CURRENT_TIMESTAMP < NEW.fecha_hora_fin
       AND (
            EXISTS (
                SELECT 1
                  FROM cuenta_mesas cm
                  JOIN cuentas cu ON cu.id = cm.cuenta_id
                 WHERE cm.mesa_id = NEW.mesa_id
                   AND cm.activa = TRUE
                   AND cu.reserva_id IS DISTINCT FROM NEW.id
            )
            OR EXISTS (
                SELECT 1
                  FROM lista_espera le
                 WHERE le.mesa_sugerida_id = NEW.mesa_id
                   AND le.estado IN ('SUGERIDA', 'NOTIFICADA')
            )
       ) THEN
        RAISE EXCEPTION
            'La mesa ya esta ocupada o sugerida durante el horario actual';
    END IF;

    IF NEW.cliente_id IS NOT NULL THEN
        SELECT restaurante_id INTO v_restaurante_ref
          FROM clientes
         WHERE id = NEW.cliente_id;

        IF v_restaurante_ref IS DISTINCT FROM NEW.restaurante_id THEN
            RAISE EXCEPTION
                'El cliente no pertenece al restaurante de la reserva';
        END IF;
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.creada_por_id,
        NEW.restaurante_id,
        NULL
    );

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_reserva
BEFORE INSERT OR UPDATE ON reservas
FOR EACH ROW EXECUTE FUNCTION fn_validar_reserva();

CREATE OR REPLACE FUNCTION fn_validar_lista_espera()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante_ref BIGINT;
    v_capacidad       SMALLINT;
    v_mesa_activa     BOOLEAN;
    v_estado_mesa     VARCHAR(20);
BEGIN
    IF TG_OP = 'INSERT' AND NEW.estado <> 'ESPERANDO' THEN
        RAISE EXCEPTION
            'Una entrada nueva de lista de espera debe iniciar ESPERANDO';
    END IF;

    IF TG_OP = 'UPDATE' AND NEW.estado IS DISTINCT FROM OLD.estado THEN
        IF NOT (
            (OLD.estado = 'ESPERANDO'
                AND NEW.estado IN ('SUGERIDA', 'SENTADA', 'RETIRADA'))
            OR (OLD.estado = 'SUGERIDA'
                AND NEW.estado IN (
                    'ESPERANDO', 'NOTIFICADA', 'SENTADA', 'RETIRADA'
                ))
            OR (OLD.estado = 'NOTIFICADA'
                AND NEW.estado IN ('ESPERANDO', 'SENTADA', 'RETIRADA'))
        ) THEN
            RAISE EXCEPTION
                'Transicion de lista de espera no valida: % -> %',
                OLD.estado, NEW.estado;
        END IF;
    END IF;

    IF NEW.estado = 'ESPERANDO' THEN
        NEW.mesa_sugerida_id := NULL;
        NEW.sugerida_en := NULL;
        NEW.notificada_en := NULL;
        NEW.sentada_en := NULL;
        NEW.retirada_en := NULL;
    ELSIF NEW.estado = 'SUGERIDA' AND NEW.sugerida_en IS NULL THEN
        NEW.sugerida_en := CURRENT_TIMESTAMP;
    ELSIF NEW.estado = 'NOTIFICADA' AND NEW.notificada_en IS NULL THEN
        NEW.notificada_en := CURRENT_TIMESTAMP;
    ELSIF NEW.estado = 'SENTADA' THEN
        NEW.sugerida_en := COALESCE(NEW.sugerida_en, CURRENT_TIMESTAMP);
        NEW.sentada_en := COALESCE(NEW.sentada_en, CURRENT_TIMESTAMP);
    ELSIF NEW.estado = 'RETIRADA' AND NEW.retirada_en IS NULL THEN
        NEW.retirada_en := CURRENT_TIMESTAMP;
    END IF;

    IF NEW.cliente_id IS NOT NULL THEN
        SELECT restaurante_id INTO v_restaurante_ref
          FROM clientes
         WHERE id = NEW.cliente_id;

        IF v_restaurante_ref IS DISTINCT FROM NEW.restaurante_id THEN
            RAISE EXCEPTION
                'El cliente no pertenece al restaurante de la lista de espera';
        END IF;
    END IF;

    IF NEW.mesa_sugerida_id IS NOT NULL THEN
        SELECT restaurante_id, capacidad, activo, estado_actual
          INTO v_restaurante_ref, v_capacidad, v_mesa_activa, v_estado_mesa
          FROM mesas
         WHERE id = NEW.mesa_sugerida_id
         FOR UPDATE;

        IF v_restaurante_ref IS DISTINCT FROM NEW.restaurante_id
           OR NOT v_mesa_activa THEN
            RAISE EXCEPTION
                'La mesa sugerida no pertenece al restaurante o esta inactiva';
        END IF;

        IF v_capacidad < NEW.cantidad_personas THEN
            RAISE EXCEPTION
                'La mesa sugerida no tiene capacidad suficiente';
        END IF;

        IF NEW.estado IN ('SUGERIDA', 'NOTIFICADA')
           AND (
                v_estado_mesa NOT IN ('LIBRE', 'RESERVADA')
                OR EXISTS (
                    SELECT 1
                      FROM cuenta_mesas cm
                     WHERE cm.mesa_id = NEW.mesa_sugerida_id
                       AND cm.activa = TRUE
                )
                OR EXISTS (
                    SELECT 1
                      FROM reservas r
                     WHERE r.mesa_id = NEW.mesa_sugerida_id
                       AND r.estado IN (
                           'PENDIENTE', 'CONFIRMADA', 'CLIENTE_PRESENTE'
                       )
                       AND CURRENT_TIMESTAMP >= r.fecha_hora_inicio
                       AND CURRENT_TIMESTAMP < r.fecha_hora_fin
                )
           ) THEN
            RAISE EXCEPTION
                'La mesa sugerida ya esta ocupada o bloqueada por una reserva';
        END IF;
    END IF;

    IF NEW.estado IN ('SUGERIDA', 'NOTIFICADA', 'SENTADA')
       AND (NEW.mesa_sugerida_id IS NULL OR NEW.sugerida_en IS NULL) THEN
        RAISE EXCEPTION
            'El estado de espera requiere una mesa y fecha de sugerencia';
    END IF;

    IF NEW.estado = 'SENTADA' AND NEW.sentada_en IS NULL THEN
        RAISE EXCEPTION
            'Una entrada SENTADA debe registrar la fecha de asiento';
    END IF;

    IF NEW.estado = 'NOTIFICADA' AND NEW.notificada_en IS NULL THEN
        RAISE EXCEPTION
            'Una entrada NOTIFICADA debe registrar la fecha de notificacion';
    END IF;

    IF NEW.estado = 'RETIRADA' AND NEW.retirada_en IS NULL THEN
        RAISE EXCEPTION
            'Una entrada RETIRADA debe registrar la fecha de retiro';
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.registrada_por_id,
        NEW.restaurante_id,
        NULL
    );

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_lista_espera
BEFORE INSERT OR UPDATE ON lista_espera
FOR EACH ROW EXECUTE FUNCTION fn_validar_lista_espera();

CREATE OR REPLACE FUNCTION fn_sincronizar_mesa_lista_espera()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_actual VARCHAR(20);
    v_mesa_anterior BIGINT;
BEGIN
    IF NEW.estado IN ('SUGERIDA', 'NOTIFICADA')
       AND NEW.mesa_sugerida_id IS NOT NULL THEN
        SELECT estado_actual
          INTO v_estado_actual
          FROM mesas
         WHERE id = NEW.mesa_sugerida_id
         FOR UPDATE;

        IF v_estado_actual = 'LIBRE' THEN
            UPDATE mesas
               SET estado_actual = 'RESERVADA'
             WHERE id = NEW.mesa_sugerida_id;

            INSERT INTO historial_estados_mesa (
                mesa_id, estado_anterior, estado_nuevo, usuario_id, motivo
            ) VALUES (
                NEW.mesa_sugerida_id, 'LIBRE', 'RESERVADA',
                NEW.registrada_por_id,
                'Mesa sugerida al siguiente cliente de la lista de espera'
            );
        END IF;
    END IF;

    IF TG_OP = 'UPDATE'
       AND OLD.mesa_sugerida_id IS NOT NULL
       AND OLD.estado IN ('SUGERIDA', 'NOTIFICADA')
       AND (
            NEW.estado NOT IN ('SUGERIDA', 'NOTIFICADA')
            OR NEW.mesa_sugerida_id IS DISTINCT FROM OLD.mesa_sugerida_id
       ) THEN
        v_mesa_anterior := OLD.mesa_sugerida_id;

        SELECT estado_actual
          INTO v_estado_actual
          FROM mesas
         WHERE id = v_mesa_anterior
         FOR UPDATE;

        IF v_estado_actual = 'RESERVADA'
           AND NOT EXISTS (
                SELECT 1
                  FROM cuenta_mesas cm
                 WHERE cm.mesa_id = v_mesa_anterior
                   AND cm.activa = TRUE
           )
           AND NOT EXISTS (
                SELECT 1
                  FROM lista_espera le
                 WHERE le.mesa_sugerida_id = v_mesa_anterior
                   AND le.estado IN ('SUGERIDA', 'NOTIFICADA')
                   AND le.id <> NEW.id
           )
           AND NOT EXISTS (
                SELECT 1
                  FROM reservas r
                 WHERE r.mesa_id = v_mesa_anterior
                   AND r.estado IN (
                       'PENDIENTE', 'CONFIRMADA', 'CLIENTE_PRESENTE'
                   )
                   AND CURRENT_TIMESTAMP >= r.fecha_hora_inicio
                   AND CURRENT_TIMESTAMP < r.fecha_hora_fin
           ) THEN
            UPDATE mesas
               SET estado_actual = 'LIBRE'
             WHERE id = v_mesa_anterior;

            INSERT INTO historial_estados_mesa (
                mesa_id, estado_anterior, estado_nuevo, usuario_id, motivo
            ) VALUES (
                v_mesa_anterior, 'RESERVADA', 'LIBRE',
                NEW.registrada_por_id,
                'Sugerencia de lista de espera finalizada sin ocupacion'
            );
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_sincronizar_mesa_lista_espera
AFTER INSERT OR UPDATE OF estado, mesa_sugerida_id ON lista_espera
FOR EACH ROW EXECUTE FUNCTION fn_sincronizar_mesa_lista_espera();

CREATE OR REPLACE FUNCTION fn_sugerir_siguiente_lista_espera(
    p_restaurante_id BIGINT,
    p_mesa_id BIGINT
)
RETURNS BIGINT
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_capacidad SMALLINT;
    v_espera_id BIGINT;
BEGIN
    SELECT capacidad INTO v_capacidad
      FROM mesas
     WHERE id = p_mesa_id
       AND restaurante_id = p_restaurante_id
       AND activo = TRUE
       AND estado_actual = 'LIBRE'
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'La mesa no existe, no pertenece al restaurante o no esta libre';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM cuenta_mesas cm
         WHERE cm.mesa_id = p_mesa_id
           AND cm.activa = TRUE
    ) OR EXISTS (
        SELECT 1
          FROM lista_espera le
         WHERE le.mesa_sugerida_id = p_mesa_id
           AND le.estado IN ('SUGERIDA', 'NOTIFICADA')
    ) OR EXISTS (
        SELECT 1
          FROM reservas r
         WHERE r.mesa_id = p_mesa_id
           AND r.estado IN ('PENDIENTE', 'CONFIRMADA', 'CLIENTE_PRESENTE')
           AND CURRENT_TIMESTAMP >= r.fecha_hora_inicio
           AND CURRENT_TIMESTAMP < r.fecha_hora_fin
    ) THEN
        RAISE EXCEPTION
            'La mesa ya esta ocupada, sugerida o bloqueada por una reserva';
    END IF;

    SELECT id INTO v_espera_id
      FROM lista_espera
     WHERE restaurante_id = p_restaurante_id
       AND estado = 'ESPERANDO'
       AND cantidad_personas <= v_capacidad
     ORDER BY hora_llegada, id
     FOR UPDATE SKIP LOCKED
     LIMIT 1;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    UPDATE lista_espera
       SET estado = 'SUGERIDA',
           mesa_sugerida_id = p_mesa_id,
           sugerida_en = CURRENT_TIMESTAMP
     WHERE id = v_espera_id;

    RETURN v_espera_id;
END;
$$;

CREATE OR REPLACE FUNCTION fn_validar_cuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante_ref BIGINT;
    v_capacidad       SMALLINT;
    v_activo          BOOLEAN;
    v_mesa_ref        BIGINT;
    v_estado_ref      VARCHAR(20);
    v_capacidad_total INTEGER;
BEGIN
    SELECT restaurante_id, capacidad, activo
      INTO v_restaurante_ref, v_capacidad, v_activo
      FROM mesas
     WHERE id = NEW.mesa_id
     FOR SHARE;

    IF NOT FOUND
       OR v_restaurante_ref <> NEW.restaurante_id
       OR NOT v_activo THEN
        RAISE EXCEPTION
            'La mesa de la cuenta no pertenece al restaurante o esta inactiva';
    END IF;

    IF TG_OP = 'UPDATE' THEN
        SELECT COALESCE(SUM(m.capacidad), v_capacidad)
          INTO v_capacidad_total
          FROM mesas m
         WHERE m.id IN (
            SELECT cm.mesa_id
              FROM cuenta_mesas cm
             WHERE cm.cuenta_id = NEW.id
               AND cm.activa = TRUE
               AND (
                    NEW.mesa_id = OLD.mesa_id
                    OR cm.mesa_id <> OLD.mesa_id
               )
            UNION
            SELECT NEW.mesa_id
         );
    ELSE
        v_capacidad_total := v_capacidad;
    END IF;

    IF NEW.cantidad_personas > v_capacidad_total THEN
        RAISE EXCEPTION 'La mesa no tiene capacidad para la cuenta';
    END IF;

    IF (TG_OP = 'INSERT' OR NEW.mesa_id IS DISTINCT FROM OLD.mesa_id)
       AND EXISTS (
            SELECT 1
              FROM cuenta_mesas cm
             WHERE cm.mesa_id = NEW.mesa_id
               AND cm.activa = TRUE
               AND (TG_OP = 'INSERT' OR cm.cuenta_id <> NEW.id)
       ) THEN
        RAISE EXCEPTION 'La mesa ya esta vinculada a otra cuenta activa';
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.mesero_id,
        NEW.restaurante_id,
        'WAITER'
    );

    IF NEW.cliente_id IS NOT NULL THEN
        SELECT restaurante_id INTO v_restaurante_ref
          FROM clientes
         WHERE id = NEW.cliente_id;
        IF v_restaurante_ref IS DISTINCT FROM NEW.restaurante_id THEN
            RAISE EXCEPTION
                'El cliente no pertenece al restaurante de la cuenta';
        END IF;
    END IF;

    IF NEW.reserva_id IS NOT NULL THEN
        SELECT restaurante_id, mesa_id, estado
          INTO v_restaurante_ref, v_mesa_ref, v_estado_ref
          FROM reservas
         WHERE id = NEW.reserva_id;
        IF v_restaurante_ref IS DISTINCT FROM NEW.restaurante_id
           OR (TG_OP = 'INSERT' AND v_mesa_ref IS DISTINCT FROM NEW.mesa_id)
           OR v_estado_ref NOT IN ('CONFIRMADA', 'CLIENTE_PRESENTE', 'ATENDIDA') THEN
            RAISE EXCEPTION
                'La reserva no corresponde a la cuenta o no puede atenderse';
        END IF;
    END IF;

    IF (TG_OP = 'INSERT' OR NEW.mesa_id IS DISTINCT FROM OLD.mesa_id)
       AND EXISTS (
        SELECT 1
          FROM reservas r
         WHERE r.mesa_id = NEW.mesa_id
           AND r.estado IN ('PENDIENTE', 'CONFIRMADA', 'CLIENTE_PRESENTE')
           AND CURRENT_TIMESTAMP >= r.fecha_hora_inicio
           AND CURRENT_TIMESTAMP < r.fecha_hora_fin
           AND r.id IS DISTINCT FROM NEW.reserva_id
    ) THEN
        RAISE EXCEPTION
            'La mesa esta bloqueada por una reserva vigente';
    END IF;

    IF NEW.lista_espera_id IS NOT NULL THEN
        SELECT restaurante_id, mesa_sugerida_id, estado
          INTO v_restaurante_ref, v_mesa_ref, v_estado_ref
          FROM lista_espera
         WHERE id = NEW.lista_espera_id;
        IF v_restaurante_ref IS DISTINCT FROM NEW.restaurante_id
           OR (TG_OP = 'INSERT' AND v_mesa_ref IS DISTINCT FROM NEW.mesa_id)
           OR v_estado_ref NOT IN ('SUGERIDA', 'NOTIFICADA', 'SENTADA') THEN
            RAISE EXCEPTION
                'La entrada de espera no corresponde a la cuenta';
        END IF;
    END IF;

    IF (TG_OP = 'INSERT' OR NEW.mesa_id IS DISTINCT FROM OLD.mesa_id)
       AND EXISTS (
        SELECT 1
          FROM lista_espera le
         WHERE le.mesa_sugerida_id = NEW.mesa_id
           AND le.estado IN ('SUGERIDA', 'NOTIFICADA')
           AND le.id IS DISTINCT FROM NEW.lista_espera_id
    ) THEN
        RAISE EXCEPTION
            'La mesa esta reservada para un cliente de la lista de espera';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_cuenta
BEFORE INSERT OR UPDATE ON cuentas
FOR EACH ROW EXECUTE FUNCTION fn_validar_cuenta();

CREATE OR REPLACE FUNCTION fn_validar_fusion_cuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante_origen  BIGINT;
    v_restaurante_destino BIGINT;
    v_estado_origen       VARCHAR(25);
    v_estado_destino      VARCHAR(25);
    v_rol_usuario         VARCHAR(30);
    v_usuario_activo      BOOLEAN;
    v_restaurante_usuario BIGINT;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION
            'El registro historico de una fusion es inmutable';
    END IF;

    IF NEW.cuenta_origen_id = NEW.cuenta_destino_id THEN
        RAISE EXCEPTION 'Una cuenta no puede fusionarse consigo misma';
    END IF;

    PERFORM 1
      FROM cuentas c
     WHERE c.id IN (NEW.cuenta_origen_id, NEW.cuenta_destino_id)
     ORDER BY c.id
     FOR UPDATE;

    SELECT restaurante_id, estado
      INTO v_restaurante_origen, v_estado_origen
      FROM cuentas
     WHERE id = NEW.cuenta_origen_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La cuenta origen de la fusion no existe';
    END IF;

    SELECT restaurante_id, estado
      INTO v_restaurante_destino, v_estado_destino
      FROM cuentas
     WHERE id = NEW.cuenta_destino_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La cuenta destino de la fusion no existe';
    END IF;

    IF v_restaurante_origen <> v_restaurante_destino THEN
        RAISE EXCEPTION
            'Las cuentas fusionadas deben pertenecer al mismo restaurante';
    END IF;

    IF v_estado_origen NOT IN ('ABIERTA', 'LISTA_COBRO')
       OR v_estado_destino <> 'ABIERTA' THEN
        RAISE EXCEPTION
            'La fusion requiere una cuenta origen abierta/lista para cobro y una cuenta destino abierta';
    END IF;

    IF EXISTS (
        SELECT 1 FROM subcuentas s
         WHERE s.cuenta_id IN (NEW.cuenta_origen_id, NEW.cuenta_destino_id)
    ) OR EXISTS (
        SELECT 1 FROM facturas f
         WHERE f.cuenta_id IN (NEW.cuenta_origen_id, NEW.cuenta_destino_id)
    ) THEN
        RAISE EXCEPTION
            'No se pueden fusionar cuentas que ya tienen divisiones o facturas';
    END IF;

    SELECT u.restaurante_id, r.name, au.enabled
      INTO v_restaurante_usuario, v_rol_usuario, v_usuario_activo
      FROM usuarios u
      JOIN public.app_users au ON au.id = u.id
      JOIN public.roles r ON r.id = au.role_id
     WHERE u.id = NEW.realizada_por_id;

    IF NOT FOUND OR NOT v_usuario_activo
       OR v_restaurante_usuario <> v_restaurante_origen
       OR v_rol_usuario NOT IN ('WAITER', 'ADMIN') THEN
        RAISE EXCEPTION
            'La fusion requiere un mesero o administrador activo del restaurante';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_fusion_cuenta
BEFORE INSERT OR UPDATE OR DELETE ON fusiones_cuenta
FOR EACH ROW EXECUTE FUNCTION fn_validar_fusion_cuenta();

CREATE OR REPLACE FUNCTION fn_ejecutar_fusion_cuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_ultima_ronda      INTEGER;
    v_rondas_origen     INTEGER;
    v_mesero_destino    BIGINT;
    v_personas_origen   SMALLINT;
BEGIN
    SELECT mesero_id
      INTO v_mesero_destino
      FROM cuentas
     WHERE id = NEW.cuenta_destino_id;

    SELECT cantidad_personas
      INTO v_personas_origen
      FROM cuentas
     WHERE id = NEW.cuenta_origen_id;

    SELECT COALESCE(MAX(numero_ronda), 0)
      INTO v_ultima_ronda
      FROM comandas
     WHERE cuenta_id = NEW.cuenta_destino_id;

    SELECT COUNT(*)
      INTO v_rondas_origen
      FROM comandas
     WHERE cuenta_id = NEW.cuenta_origen_id;

    IF v_ultima_ronda + v_rondas_origen > 32767 THEN
        RAISE EXCEPTION
            'La fusion supera el maximo de rondas permitido por cuenta';
    END IF;

    WITH rondas_origen AS (
        SELECT
            co.id,
            ROW_NUMBER() OVER (ORDER BY co.numero_ronda, co.id) AS nueva_posicion
          FROM comandas co
         WHERE co.cuenta_id = NEW.cuenta_origen_id
    )
    UPDATE comandas co
       SET cuenta_id = NEW.cuenta_destino_id,
           numero_ronda = (
               v_ultima_ronda + ro.nueva_posicion
           )::SMALLINT,
           mesero_id = v_mesero_destino
      FROM rondas_origen ro
     WHERE co.id = ro.id;

    UPDATE cuentas
       SET estado = 'FUSIONADA',
           observaciones = CONCAT_WS(
               ' | ', NULLIF(observaciones, ''),
               'Fusionada en cuenta ' || NEW.cuenta_destino_id
           )
     WHERE id = NEW.cuenta_origen_id;

    UPDATE cuentas
       SET cantidad_personas = cantidad_personas + v_personas_origen,
           observaciones = CONCAT_WS(
               ' | ', NULLIF(observaciones, ''),
               'Recibe fusion de cuenta ' || NEW.cuenta_origen_id
           )
     WHERE id = NEW.cuenta_destino_id;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_ejecutar_fusion_cuenta
AFTER INSERT ON fusiones_cuenta
FOR EACH ROW EXECUTE FUNCTION fn_ejecutar_fusion_cuenta();

CREATE OR REPLACE FUNCTION fn_proteger_cuenta_terminal()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF OLD.estado IN ('CERRADA', 'CANCELADA', 'FUSIONADA') THEN
        RAISE EXCEPTION
            'Una cuenta cerrada, cancelada o fusionada es inmutable';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_proteger_cuenta_terminal
BEFORE UPDATE ON cuentas
FOR EACH ROW EXECUTE FUNCTION fn_proteger_cuenta_terminal();

CREATE OR REPLACE FUNCTION fn_validar_transicion_mesa()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF NEW.estado_actual = OLD.estado_actual THEN
        RETURN NEW;
    END IF;

    IF NOT (
        (OLD.estado_actual = 'LIBRE'
            AND NEW.estado_actual IN ('RESERVADA', 'OCUPADA'))
        OR (OLD.estado_actual = 'RESERVADA'
            AND NEW.estado_actual IN ('LIBRE', 'OCUPADA'))
        OR (OLD.estado_actual = 'OCUPADA'
            AND NEW.estado_actual IN ('LIBRE', 'CUENTA_SOLICITADA'))
        OR (OLD.estado_actual = 'CUENTA_SOLICITADA'
            AND NEW.estado_actual IN ('OCUPADA', 'LIBRE'))
    ) THEN
        RAISE EXCEPTION
            'Transicion de mesa no valida: % -> %',
            OLD.estado_actual, NEW.estado_actual;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_transicion_mesa
BEFORE UPDATE OF estado_actual ON mesas
FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_mesa();

CREATE OR REPLACE FUNCTION fn_validar_transicion_cuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_total_facturado DECIMAL(14,2);
    v_total_pagado    DECIMAL(14,2);
BEGIN
    IF NEW.estado = OLD.estado THEN
        RETURN NEW;
    END IF;

    IF NOT (
        (OLD.estado = 'ABIERTA'
            AND NEW.estado IN ('LISTA_COBRO', 'CANCELADA', 'FUSIONADA'))
        OR (OLD.estado = 'LISTA_COBRO'
            AND NEW.estado IN (
                'ABIERTA', 'PARCIALMENTE_PAGADA', 'CERRADA',
                'CANCELADA', 'FUSIONADA'
            ))
        OR (OLD.estado = 'PARCIALMENTE_PAGADA'
            AND NEW.estado IN ('CERRADA', 'CANCELADA'))
    ) THEN
        RAISE EXCEPTION
            'Transicion de cuenta no valida: % -> %',
            OLD.estado, NEW.estado;
    END IF;

    IF NEW.estado = 'LISTA_COBRO' AND NEW.solicitada_cobro_en IS NULL THEN
        NEW.solicitada_cobro_en := CURRENT_TIMESTAMP;
    END IF;

    IF NEW.estado = 'CERRADA' THEN
        IF EXISTS (
            SELECT 1
              FROM comandas co
             WHERE co.cuenta_id = NEW.id
               AND co.estado NOT IN ('ENTREGADA', 'CANCELADA')
        ) THEN
            RAISE EXCEPTION
                'La cuenta no puede cerrarse mientras tenga comandas activas';
        END IF;

        IF EXISTS (
            SELECT 1
              FROM subcuentas s
             WHERE s.cuenta_id = NEW.id
               AND s.estado = 'PENDIENTE'
        ) THEN
            RAISE EXCEPTION
                'La cuenta no puede cerrarse mientras tenga subcuentas pendientes';
        END IF;

        IF EXISTS (
            SELECT 1
              FROM comanda_detalles cd
              JOIN comandas co ON co.id = cd.comanda_id
             WHERE co.cuenta_id = NEW.id
               AND cd.estado = 'ENTREGADO'
             GROUP BY cd.id, cd.cantidad
            HAVING COALESCE((
                SELECT SUM(fd.cantidad)
                  FROM factura_detalles fd
                  JOIN facturas f ON f.id = fd.factura_id
                 WHERE f.cuenta_id = NEW.id
                   AND f.estado = 'EMITIDA'
                   AND fd.comanda_detalle_id = cd.id
            ), 0) <> cd.cantidad
        ) THEN
            RAISE EXCEPTION
                'La cuenta no puede cerrarse: existen platillos entregados sin facturar completamente';
        END IF;

        SELECT
            COALESCE(SUM(f.total), 0),
            COALESCE(SUM(p.total_pagado), 0)
          INTO v_total_facturado, v_total_pagado
          FROM facturas f
          LEFT JOIN (
              SELECT factura_id, SUM(monto) AS total_pagado
                FROM pagos
               GROUP BY factura_id
          ) p ON p.factura_id = f.id
         WHERE f.cuenta_id = NEW.id
           AND f.estado = 'EMITIDA';

        IF NOT EXISTS (
            SELECT 1
              FROM facturas f
             WHERE f.cuenta_id = NEW.id
               AND f.estado = 'EMITIDA'
        ) OR ROUND(v_total_pagado, 2) <> ROUND(v_total_facturado, 2) THEN
            RAISE EXCEPTION
                'La cuenta solo puede cerrarse cuando sus facturas esten pagadas';
        END IF;
    END IF;

    IF NEW.estado = 'CANCELADA' THEN
        IF EXISTS (
            SELECT 1
              FROM comandas co
             WHERE co.cuenta_id = NEW.id
               AND co.estado <> 'CANCELADA'
        ) THEN
            RAISE EXCEPTION
                'La cuenta no puede cancelarse mientras tenga comandas sin cancelar';
        END IF;

        IF EXISTS (
            SELECT 1
              FROM facturas f
             WHERE f.cuenta_id = NEW.id
               AND f.estado = 'EMITIDA'
        ) THEN
            RAISE EXCEPTION
                'Anule primero las facturas emitidas antes de cancelar la cuenta';
        END IF;
    END IF;

    IF NEW.estado IN ('CERRADA', 'CANCELADA', 'FUSIONADA')
       AND NEW.cerrada_en IS NULL THEN
        NEW.cerrada_en := CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_transicion_cuenta
BEFORE UPDATE OF estado ON cuentas
FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_cuenta();

CREATE OR REPLACE FUNCTION fn_validar_subcuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_cuenta      VARCHAR(25);
    v_porcentaje_actual  DECIMAL(9,4);
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.subtotal_snapshot := 0;
    END IF;

    SELECT estado INTO v_estado_cuenta
      FROM cuentas
     WHERE id = NEW.cuenta_id
     FOR SHARE;

    IF NOT FOUND OR v_estado_cuenta IN ('CERRADA', 'CANCELADA', 'FUSIONADA') THEN
        RAISE EXCEPTION
            'No se pueden crear o modificar subcuentas de una cuenta terminal';
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.estado IN ('FACTURADA', 'CANCELADA') THEN
        RAISE EXCEPTION 'Una subcuenta terminal es inmutable';
    END IF;

    IF TG_OP = 'UPDATE'
       AND (
            NEW.cuenta_id IS DISTINCT FROM OLD.cuenta_id
            OR NEW.tipo_division IS DISTINCT FROM OLD.tipo_division
            OR NEW.porcentaje_asignado IS DISTINCT FROM OLD.porcentaje_asignado
       )
       AND EXISTS (
            SELECT 1
              FROM subcuenta_detalles sd
             WHERE sd.subcuenta_id = OLD.id
       ) THEN
        RAISE EXCEPTION
            'Elimine primero las asignaciones antes de cambiar la division';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM subcuentas s
         WHERE s.cuenta_id = NEW.cuenta_id
           AND s.id <> COALESCE(NEW.id, -1)
           AND s.estado <> 'CANCELADA'
           AND s.tipo_division <> NEW.tipo_division
    ) THEN
        RAISE EXCEPTION
            'Todas las subcuentas activas deben usar el mismo tipo de division';
    END IF;

    IF NEW.tipo_division = 'PERSONAS' THEN
        SELECT COALESCE(SUM(s.porcentaje_asignado), 0)
          INTO v_porcentaje_actual
          FROM subcuentas s
         WHERE s.cuenta_id = NEW.cuenta_id
           AND s.id <> COALESCE(NEW.id, -1)
           AND s.estado <> 'CANCELADA';

        IF ROUND(v_porcentaje_actual + NEW.porcentaje_asignado, 4) > 100 THEN
            RAISE EXCEPTION
                'Los porcentajes de las subcuentas superan el 100 por ciento';
        END IF;
    END IF;

    IF TG_OP = 'UPDATE'
       AND NEW.subtotal_snapshot IS DISTINCT FROM OLD.subtotal_snapshot
       AND current_setting(
            'restaurante.permitir_actualizacion_subcuenta', TRUE
       ) IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION
            'El subtotal de una subcuenta se calcula automaticamente';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_subcuenta
BEFORE INSERT OR UPDATE ON subcuentas
FOR EACH ROW EXECUTE FUNCTION fn_validar_subcuenta();

CREATE OR REPLACE FUNCTION fn_validar_asignacion_subcuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_cuenta_subcuenta BIGINT;
    v_estado_subcuenta VARCHAR(15);
    v_tipo_subcuenta   VARCHAR(15);
    v_porcentaje       DECIMAL(7,4);
    v_cuenta_detalle   BIGINT;
    v_cantidad_vendida DECIMAL(12,6);
    v_cantidad_asignada DECIMAL(14,6);
BEGIN
    IF TG_OP = 'UPDATE'
       AND NEW.subcuenta_id IS DISTINCT FROM OLD.subcuenta_id
       AND EXISTS (
            SELECT 1
              FROM subcuentas s
             WHERE s.id = OLD.subcuenta_id
               AND s.estado <> 'PENDIENTE'
       ) THEN
        RAISE EXCEPTION
            'No se puede mover una asignacion desde una subcuenta terminal';
    END IF;

    SELECT cuenta_id, estado, tipo_division, porcentaje_asignado
      INTO v_cuenta_subcuenta, v_estado_subcuenta,
           v_tipo_subcuenta, v_porcentaje
      FROM subcuentas
     WHERE id = COALESCE(NEW.subcuenta_id, OLD.subcuenta_id);

    IF NOT FOUND THEN

        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RAISE EXCEPTION 'La subcuenta indicada no existe';
    END IF;

    IF v_estado_subcuenta <> 'PENDIENTE' THEN
        RAISE EXCEPTION
            'Solo se pueden editar asignaciones de una subcuenta PENDIENTE';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;

    SELECT co.cuenta_id, cd.cantidad
      INTO v_cuenta_detalle, v_cantidad_vendida
      FROM comanda_detalles cd
      JOIN comandas co ON co.id = cd.comanda_id
     WHERE cd.id = NEW.comanda_detalle_id
     FOR UPDATE OF cd;

    IF v_cuenta_detalle IS DISTINCT FROM v_cuenta_subcuenta THEN
        RAISE EXCEPTION
            'El platillo asignado no pertenece a la cuenta dividida';
    END IF;

    IF v_tipo_subcuenta = 'ITEMS'
       AND NEW.cantidad_asignada <> TRUNC(NEW.cantidad_asignada) THEN
        RAISE EXCEPTION
            'La division por ITEMS requiere cantidades enteras';
    END IF;

    IF v_tipo_subcuenta = 'PERSONAS'
       AND NEW.cantidad_asignada <> ROUND(
            v_cantidad_vendida * v_porcentaje / 100,
            6
       ) THEN
        RAISE EXCEPTION
            'La cantidad asignada no coincide con el porcentaje de la persona';
    END IF;

    SELECT COALESCE(SUM(sd.cantidad_asignada), 0)
      INTO v_cantidad_asignada
      FROM subcuenta_detalles sd
      JOIN subcuentas s ON s.id = sd.subcuenta_id
     WHERE sd.comanda_detalle_id = NEW.comanda_detalle_id
       AND s.estado <> 'CANCELADA'
       AND sd.id <> COALESCE(NEW.id, -1);

    IF ROUND(v_cantidad_asignada + NEW.cantidad_asignada, 6)
       > v_cantidad_vendida THEN
        RAISE EXCEPTION
            'La cantidad asignada supera la cantidad vendida del platillo';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_asignacion_subcuenta
BEFORE INSERT OR UPDATE OR DELETE ON subcuenta_detalles
FOR EACH ROW EXECUTE FUNCTION fn_validar_asignacion_subcuenta();

CREATE OR REPLACE FUNCTION fn_recalcular_subtotal_subcuenta(
    p_subcuenta_id BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    PERFORM set_config(
        'restaurante.permitir_actualizacion_subcuenta', 'true', TRUE
    );

    UPDATE subcuentas s
       SET subtotal_snapshot = COALESCE((
            SELECT ROUND(SUM(
                sd.cantidad_asignada * (
                    cd.precio_unitario_snapshot + COALESCE((
                        SELECT SUM(
                            cdm.cantidad * cdm.precio_adicional_snapshot
                        )
                          FROM comanda_detalle_modificadores cdm
                         WHERE cdm.comanda_detalle_id = cd.id
                    ), 0)
                )
            ), 2)
              FROM subcuenta_detalles sd
              JOIN comanda_detalles cd ON cd.id = sd.comanda_detalle_id
             WHERE sd.subcuenta_id = s.id
       ), 0)
     WHERE s.id = p_subcuenta_id
       AND s.estado = 'PENDIENTE';

    PERFORM set_config(
        'restaurante.permitir_actualizacion_subcuenta', 'false', TRUE
    );
END;
$$;

CREATE OR REPLACE FUNCTION fn_actualizar_subtotal_subcuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM fn_recalcular_subtotal_subcuenta(OLD.subcuenta_id);
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE')
       AND (TG_OP <> 'UPDATE' OR NEW.subcuenta_id IS DISTINCT FROM OLD.subcuenta_id) THEN
        PERFORM fn_recalcular_subtotal_subcuenta(NEW.subcuenta_id);
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_actualizar_subtotal_subcuenta
AFTER INSERT OR UPDATE OR DELETE ON subcuenta_detalles
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_subtotal_subcuenta();

CREATE OR REPLACE FUNCTION fn_validar_transicion_subcuenta()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF NEW.estado = OLD.estado THEN
        RETURN NEW;
    END IF;

    IF OLD.estado <> 'PENDIENTE'
       OR NEW.estado NOT IN ('FACTURADA', 'CANCELADA') THEN
        RAISE EXCEPTION
            'Transicion de subcuenta no valida: % -> %', OLD.estado, NEW.estado;
    END IF;

    IF NEW.estado = 'FACTURADA' AND NOT EXISTS (
        SELECT 1
          FROM facturas f
         WHERE f.subcuenta_id = NEW.id
           AND f.estado = 'EMITIDA'
           AND (
                SELECT COALESCE(SUM(p.monto), 0)
                  FROM pagos p
                 WHERE p.factura_id = f.id
           ) = f.total
           AND f.subtotal = NEW.subtotal_snapshot
    ) THEN
        RAISE EXCEPTION
            'La subcuenta solo se marca FACTURADA cuando su factura esta pagada';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_transicion_subcuenta
BEFORE UPDATE OF estado ON subcuentas
FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_subcuenta();

CREATE OR REPLACE FUNCTION fn_validar_comanda()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante BIGINT;
    v_mesero       BIGINT;
    v_estado       VARCHAR(25);
BEGIN
    IF TG_OP = 'INSERT' AND NEW.estado <> 'BORRADOR' THEN
        RAISE EXCEPTION 'Una comanda nueva debe crearse en BORRADOR';
    END IF;

    SELECT restaurante_id, mesero_id, estado
      INTO v_restaurante, v_mesero, v_estado
      FROM cuentas
     WHERE id = NEW.cuenta_id
     FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La cuenta de la comanda no existe';
    END IF;

    IF v_estado <> 'ABIERTA' THEN
        RAISE EXCEPTION
            'No se pueden agregar comandas a una cuenta en estado %', v_estado;
    END IF;

    IF NEW.mesero_id <> v_mesero THEN
        RAISE EXCEPTION
            'La comanda debe pertenecer al mesero responsable de la cuenta';
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.mesero_id,
        v_restaurante,
        'WAITER'
    );

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_comanda
BEFORE INSERT OR UPDATE OF cuenta_id, mesero_id ON comandas
FOR EACH ROW EXECUTE FUNCTION fn_validar_comanda();

CREATE OR REPLACE FUNCTION fn_validar_comanda_detalle_producto()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_comanda       VARCHAR(20);
    v_restaurante_cuenta   BIGINT;
    v_restaurante_producto BIGINT;
    v_platillo_receta      BIGINT;
    v_estado_receta        VARCHAR(15);
    v_nombre_producto      VARCHAR(150);
    v_precio_producto      DECIMAL(12,2);
    v_tiempo_producto      SMALLINT;
    v_costo_producto       DECIMAL(14,4);
BEGIN
    SELECT co.estado, cu.restaurante_id
      INTO v_estado_comanda, v_restaurante_cuenta
      FROM comandas co
      JOIN cuentas cu ON cu.id = co.cuenta_id
     WHERE co.id = NEW.comanda_id
     FOR SHARE OF co;

    IF NOT FOUND OR v_estado_comanda <> 'BORRADOR' THEN
        RAISE EXCEPTION
            'Los productos de una comanda solo se editan en BORRADOR';
    END IF;

    IF TG_OP = 'UPDATE' AND EXISTS (
        SELECT 1
          FROM subcuenta_detalles sd
         WHERE sd.comanda_detalle_id = OLD.id
    ) THEN
        RAISE EXCEPTION
            'Elimine las asignaciones de subcuenta antes de modificar el producto';
    END IF;

    IF NEW.platillo_id IS NOT NULL THEN
        SELECT
            restaurante_id, nombre, precio_venta,
            tiempo_preparacion_minutos
          INTO
            v_restaurante_producto, v_nombre_producto, v_precio_producto,
            v_tiempo_producto
          FROM platillos
         WHERE id = NEW.platillo_id
           AND activo = TRUE;

        SELECT platillo_id, estado
          INTO v_platillo_receta, v_estado_receta
          FROM receta_versiones
         WHERE id = NEW.receta_version_id;

        IF v_restaurante_producto IS DISTINCT FROM v_restaurante_cuenta
           OR v_platillo_receta IS DISTINCT FROM NEW.platillo_id
           OR v_estado_receta IS DISTINCT FROM 'VIGENTE' THEN
            RAISE EXCEPTION
                'El platillo o su receta vigente no corresponden a la comanda';
        END IF;

        SELECT ROUND(COALESCE(SUM(
            rd.cantidad * ur.factor_a_base / us.factor_a_base
            * i.costo_unitario_actual
        ), 0), 4)
          INTO v_costo_producto
          FROM receta_detalles rd
          JOIN insumos i ON i.id = rd.insumo_id
          JOIN unidades_medida ur ON ur.id = rd.unidad_medida_id
          JOIN unidades_medida us ON us.id = i.unidad_stock_id
         WHERE rd.receta_version_id = NEW.receta_version_id;
    ELSE
        SELECT
            restaurante_id, nombre, precio_venta,
            tiempo_preparacion_minutos
          INTO
            v_restaurante_producto, v_nombre_producto, v_precio_producto,
            v_tiempo_producto
          FROM combos
         WHERE id = NEW.combo_id
           AND activo = TRUE
           AND disponible_manual = TRUE
           AND (fecha_inicio IS NULL OR CURRENT_TIMESTAMP >= fecha_inicio)
           AND (fecha_fin IS NULL OR CURRENT_TIMESTAMP <= fecha_fin);

        IF v_restaurante_producto IS DISTINCT FROM v_restaurante_cuenta
           OR NOT EXISTS (
                SELECT 1 FROM combo_detalles cd
                 WHERE cd.combo_id = NEW.combo_id
           ) OR EXISTS (
                SELECT 1
                  FROM combo_detalles cod
                  LEFT JOIN platillos p ON p.id = cod.platillo_id
                 WHERE cod.combo_id = NEW.combo_id
                   AND (
                        p.id IS NULL
                        OR p.activo = FALSE
                        OR p.disponible_manual = FALSE
                        OR NOT EXISTS (
                            SELECT 1
                              FROM receta_versiones rv
                             WHERE rv.platillo_id = cod.platillo_id
                               AND rv.estado = 'VIGENTE'
                               AND EXISTS (
                                    SELECT 1
                                      FROM receta_detalles rd
                                     WHERE rd.receta_version_id = rv.id
                               )
                        )
                   )
           ) THEN
            RAISE EXCEPTION
                'El combo no esta disponible o tiene componentes sin receta vigente';
        END IF;

        SELECT ROUND(COALESCE(SUM(
            cod.cantidad
            * rd.cantidad * ur.factor_a_base / us.factor_a_base
            * i.costo_unitario_actual
        ), 0), 4)
          INTO v_costo_producto
          FROM combo_detalles cod
          JOIN receta_versiones rv
            ON rv.platillo_id = cod.platillo_id
           AND rv.estado = 'VIGENTE'
          JOIN receta_detalles rd ON rd.receta_version_id = rv.id
          JOIN insumos i ON i.id = rd.insumo_id
          JOIN unidades_medida ur ON ur.id = rd.unidad_medida_id
          JOIN unidades_medida us ON us.id = i.unidad_stock_id
         WHERE cod.combo_id = NEW.combo_id;
    END IF;

    NEW.nombre_snapshot := v_nombre_producto;
    NEW.precio_unitario_snapshot := v_precio_producto;
    NEW.tiempo_estimado_minutos := v_tiempo_producto;
    NEW.costo_unitario_snapshot := v_costo_producto;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_comanda_detalle_producto
BEFORE INSERT OR UPDATE OF
    comanda_id, platillo_id, combo_id, receta_version_id, cantidad,
    nombre_snapshot, precio_unitario_snapshot, costo_unitario_snapshot,
    tiempo_estimado_minutos, notas_especiales
ON comanda_detalles
FOR EACH ROW EXECUTE FUNCTION fn_validar_comanda_detalle_producto();

CREATE OR REPLACE FUNCTION fn_congelar_recetas_combo_comanda()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    DELETE FROM comanda_detalle_combo_recetas
     WHERE comanda_detalle_id = NEW.id;

    IF NEW.combo_id IS NOT NULL THEN
        INSERT INTO comanda_detalle_combo_recetas (
            comanda_detalle_id, platillo_id, receta_version_id,
            cantidad_platillo, costo_unitario_snapshot
        )
        SELECT
            NEW.id,
            cod.platillo_id,
            rv.id,
            cod.cantidad,
            ROUND(COALESCE(SUM(
                rd.cantidad * ur.factor_a_base / us.factor_a_base
                * i.costo_unitario_actual
            ), 0), 4)
          FROM combo_detalles cod
          JOIN receta_versiones rv
            ON rv.platillo_id = cod.platillo_id
           AND rv.estado = 'VIGENTE'
          JOIN receta_detalles rd ON rd.receta_version_id = rv.id
          JOIN insumos i ON i.id = rd.insumo_id
          JOIN unidades_medida ur ON ur.id = rd.unidad_medida_id
          JOIN unidades_medida us ON us.id = i.unidad_stock_id
         WHERE cod.combo_id = NEW.combo_id
         GROUP BY cod.platillo_id, rv.id, cod.cantidad;

        IF NOT FOUND THEN
            RAISE EXCEPTION
                'No fue posible congelar las recetas de los componentes del combo';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_congelar_recetas_combo_comanda
AFTER INSERT OR UPDATE OF platillo_id, combo_id ON comanda_detalles
FOR EACH ROW EXECUTE FUNCTION fn_congelar_recetas_combo_comanda();

CREATE OR REPLACE FUNCTION fn_validar_modificador_comanda()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_platillo_id        BIGINT;
    v_estado_comanda     VARCHAR(20);
    v_modificador_receta BIGINT;
    v_estado_receta      VARCHAR(15);
    v_nombre_modificador VARCHAR(120);
    v_precio_modificador DECIMAL(12,2);
    v_costo_modificador  DECIMAL(14,4);
    v_maximo_selecciones SMALLINT;
BEGIN
    SELECT cd.platillo_id, co.estado
      INTO v_platillo_id, v_estado_comanda
      FROM comanda_detalles cd
      JOIN comandas co ON co.id = cd.comanda_id
     WHERE cd.id = NEW.comanda_detalle_id
     FOR SHARE OF co;

    IF NOT FOUND OR v_estado_comanda <> 'BORRADOR' THEN
        RAISE EXCEPTION
            'Los modificadores solo se editan con la comanda en BORRADOR';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM subcuenta_detalles sd
         WHERE sd.comanda_detalle_id = NEW.comanda_detalle_id
    ) THEN
        RAISE EXCEPTION
            'Elimine las asignaciones de subcuenta antes de modificar agregados';
    END IF;

    SELECT pm.maximo_selecciones
      INTO v_maximo_selecciones
      FROM platillo_modificadores pm
     WHERE pm.platillo_id = v_platillo_id
       AND pm.modificador_id = NEW.modificador_id;

    IF v_platillo_id IS NULL OR NOT FOUND THEN
        RAISE EXCEPTION
            'El modificador no esta habilitado para el platillo';
    END IF;

    IF NEW.cantidad > v_maximo_selecciones THEN
        RAISE EXCEPTION
            'La cantidad del modificador supera el maximo permitido (%)',
            v_maximo_selecciones;
    END IF;

    SELECT nombre, precio_adicional
      INTO v_nombre_modificador, v_precio_modificador
      FROM modificadores
     WHERE id = NEW.modificador_id
       AND activo = TRUE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El modificador no existe o esta inactivo';
    END IF;

    IF NEW.receta_modificador_version_id IS NOT NULL THEN
        SELECT modificador_id, estado
          INTO v_modificador_receta, v_estado_receta
          FROM receta_modificador_versiones
         WHERE id = NEW.receta_modificador_version_id;

        IF v_modificador_receta IS DISTINCT FROM NEW.modificador_id
           OR v_estado_receta IS DISTINCT FROM 'VIGENTE' THEN
            RAISE EXCEPTION
                'La receta del modificador no es la version vigente correcta';
        END IF;

        SELECT ROUND(COALESCE(SUM(
            rmd.cantidad * ur.factor_a_base / us.factor_a_base
            * i.costo_unitario_actual
            * CASE rmd.tipo_ajuste
                WHEN 'AGREGAR' THEN 1
                ELSE -1
              END
        ), 0), 4)
          INTO v_costo_modificador
          FROM receta_modificador_detalles rmd
          JOIN insumos i ON i.id = rmd.insumo_id
          JOIN unidades_medida ur ON ur.id = rmd.unidad_medida_id
          JOIN unidades_medida us ON us.id = i.unidad_stock_id
         WHERE rmd.receta_modificador_version_id =
               NEW.receta_modificador_version_id;
    ELSIF EXISTS (
        SELECT 1
          FROM receta_modificador_versiones rmv
         WHERE rmv.modificador_id = NEW.modificador_id
           AND rmv.estado = 'VIGENTE'
    ) THEN
        RAISE EXCEPTION
            'Debe conservarse la version vigente de la receta del modificador';
    ELSE
        v_costo_modificador := 0;
    END IF;

    NEW.nombre_snapshot := v_nombre_modificador;
    NEW.precio_adicional_snapshot := v_precio_modificador;
    NEW.costo_adicional_snapshot := v_costo_modificador;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_modificador_comanda
BEFORE INSERT OR UPDATE ON comanda_detalle_modificadores
FOR EACH ROW EXECUTE FUNCTION fn_validar_modificador_comanda();

CREATE OR REPLACE FUNCTION fn_bloquear_eliminacion_modificador_enviado()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_comanda VARCHAR(20);
BEGIN
    SELECT co.estado
      INTO v_estado_comanda
      FROM comanda_detalles cd
      JOIN comandas co ON co.id = cd.comanda_id
     WHERE cd.id = OLD.comanda_detalle_id;

    IF FOUND AND EXISTS (
        SELECT 1
          FROM subcuenta_detalles sd
         WHERE sd.comanda_detalle_id = OLD.comanda_detalle_id
    ) THEN
        RAISE EXCEPTION
            'Elimine las asignaciones de subcuenta antes de quitar agregados';
    END IF;

    IF FOUND AND v_estado_comanda <> 'BORRADOR' THEN
        RAISE EXCEPTION
            'Un modificador enviado no puede eliminarse de la comanda';
    END IF;

    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_bloquear_eliminacion_modificador_enviado
BEFORE DELETE ON comanda_detalle_modificadores
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion_modificador_enviado();

CREATE OR REPLACE FUNCTION fn_validar_cancelacion_comanda_detalle()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante      BIGINT;
    v_estado_detalle   VARCHAR(20);
    v_rol_solicitante  VARCHAR(30);
    v_rol_autorizador  VARCHAR(30);
BEGIN
    SELECT cu.restaurante_id, cd.estado
      INTO v_restaurante, v_estado_detalle
      FROM comanda_detalles cd
      JOIN comandas co ON co.id = cd.comanda_id
      JOIN cuentas cu ON cu.id = co.cuenta_id
     WHERE cd.id = NEW.comanda_detalle_id
     FOR SHARE OF cd;

    IF NOT FOUND OR v_estado_detalle = 'BORRADOR' THEN
        RAISE EXCEPTION
            'La cancelacion requiere un platillo que ya fue enviado a cocina';
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.solicitada_por_id, v_restaurante, NULL
    );
    PERFORM fn_validar_usuario_operacion(
        NEW.autorizada_por_id, v_restaurante, NULL
    );

    SELECT r.name
      INTO v_rol_solicitante
      FROM usuarios u
      JOIN public.app_users au ON au.id = u.id
      JOIN public.roles r ON r.id = au.role_id
     WHERE u.id = NEW.solicitada_por_id;

    IF NEW.autorizada_por_id IS NOT NULL THEN
        SELECT r.name
          INTO v_rol_autorizador
          FROM usuarios u
          JOIN public.app_users au ON au.id = u.id
          JOIN public.roles r ON r.id = au.role_id
         WHERE u.id = NEW.autorizada_por_id;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF OLD.estado_solicitud IN ('APROBADA', 'RECHAZADA') THEN
            RAISE EXCEPTION
                'Una solicitud de cancelacion resuelta es inmutable';
        END IF;

        IF NEW.estado_solicitud NOT IN ('PENDIENTE', 'APROBADA', 'RECHAZADA') THEN
            RAISE EXCEPTION 'Estado de cancelacion no valido';
        END IF;

        IF NEW.comanda_detalle_id <> OLD.comanda_detalle_id
           OR NEW.tipo <> OLD.tipo
           OR NEW.solicitada_por_id <> OLD.solicitada_por_id
           OR NEW.solicitada_en <> OLD.solicitada_en THEN
            RAISE EXCEPTION
                'No se pueden cambiar los datos originales de la solicitud';
        END IF;
    END IF;

    IF NEW.estado_solicitud IN ('APROBADA', 'RECHAZADA')
       AND NEW.resuelta_en IS NULL THEN
        NEW.resuelta_en := CURRENT_TIMESTAMP;
    END IF;

    IF NEW.tipo = 'NO_DISPONIBLE_COCINA' THEN
        IF v_rol_solicitante <> 'KITCHEN' THEN
            RAISE EXCEPTION
                'Solo cocina puede registrar un platillo como no disponible';
        END IF;

        IF NEW.estado_solicitud = 'APROBADA'
           AND v_rol_autorizador <> 'KITCHEN' THEN
            RAISE EXCEPTION
                'La no disponibilidad debe quedar autorizada por cocina';
        END IF;

        IF NEW.estado_solicitud = 'APROBADA'
           AND (
                (v_estado_detalle = 'RECIBIDO'
                    AND NEW.accion_inventario <> 'REINTEGRAR')
                OR (v_estado_detalle = 'EN_PREPARACION'
                    AND NEW.accion_inventario <> 'REGISTRAR_MERMA')
           ) THEN
            RAISE EXCEPTION
                'La accion de inventario no corresponde al avance del platillo';
        END IF;
    ELSE
        IF v_rol_solicitante NOT IN ('WAITER', 'ADMIN') THEN
            RAISE EXCEPTION
                'La cancelacion debe ser solicitada por un mesero o administrador';
        END IF;

        IF NEW.estado_solicitud = 'APROBADA'
           AND v_estado_detalle = 'EN_PREPARACION'
           AND v_rol_autorizador <> 'ADMIN' THEN
            RAISE EXCEPTION
                'Cancelar un platillo en preparacion requiere autorizacion de administrador';
        END IF;

        IF NEW.estado_solicitud = 'APROBADA'
           AND v_estado_detalle = 'RECIBIDO'
           AND v_rol_autorizador NOT IN ('WAITER', 'ADMIN') THEN
            RAISE EXCEPTION
                'La cancelacion antes de preparar requiere autorizacion de mesero o administrador';
        END IF;

        IF NEW.estado_solicitud = 'APROBADA'
           AND (
                (v_estado_detalle = 'RECIBIDO'
                    AND NEW.accion_inventario <> 'REINTEGRAR')
                OR (v_estado_detalle = 'EN_PREPARACION'
                    AND NEW.accion_inventario <> 'REGISTRAR_MERMA')
           ) THEN
            RAISE EXCEPTION
                'La cancelacion debe reintegrar antes de preparar o registrar merma durante la preparacion';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_cancelacion_comanda_detalle
BEFORE INSERT OR UPDATE ON cancelaciones_comanda_detalle
FOR EACH ROW EXECUTE FUNCTION fn_validar_cancelacion_comanda_detalle();

CREATE OR REPLACE FUNCTION fn_validar_transicion_comanda_detalle()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_comanda VARCHAR(20);
    v_restaurante    BIGINT;
    v_rol_requerido  VARCHAR(30);
    v_rol_usuario    VARCHAR(30);
BEGIN
    IF NEW.estado = OLD.estado THEN
        RETURN NEW;
    END IF;

    IF NOT (
        (OLD.estado = 'BORRADOR' AND NEW.estado = 'RECIBIDO')
        OR (OLD.estado = 'RECIBIDO'
            AND NEW.estado IN ('EN_PREPARACION', 'CANCELADO', 'NO_DISPONIBLE'))
        OR (OLD.estado = 'EN_PREPARACION'
            AND NEW.estado IN ('LISTO', 'CANCELADO', 'NO_DISPONIBLE'))
        OR (OLD.estado = 'LISTO' AND NEW.estado = 'ENTREGADO')
    ) THEN
        RAISE EXCEPTION
            'Transicion de platillo no valida: % -> %',
            OLD.estado, NEW.estado;
    END IF;

    SELECT co.estado, cu.restaurante_id
      INTO v_estado_comanda, v_restaurante
      FROM comandas co
      JOIN cuentas cu ON cu.id = co.cuenta_id
     WHERE co.id = NEW.comanda_id;

    IF NEW.estado = 'RECIBIDO' AND v_estado_comanda = 'BORRADOR' THEN
        RAISE EXCEPTION
            'No se puede recibir un platillo antes de enviar la comanda';
    END IF;

    IF NEW.actualizado_por_id IS NULL THEN
        RAISE EXCEPTION
            'El cambio de estado del platillo requiere un usuario responsable';
    END IF;

    IF NEW.estado IN ('EN_PREPARACION', 'LISTO', 'NO_DISPONIBLE') THEN
        v_rol_requerido := 'KITCHEN';
    ELSIF NEW.estado = 'ENTREGADO' THEN
        v_rol_requerido := 'WAITER';
    ELSE
        v_rol_requerido := NULL;
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.actualizado_por_id,
        v_restaurante,
        v_rol_requerido
    );

    SELECT r.name
      INTO v_rol_usuario
      FROM usuarios u
      JOIN public.app_users au ON au.id = u.id
      JOIN public.roles r ON r.id = au.role_id
     WHERE u.id = NEW.actualizado_por_id;

    IF NEW.estado = 'NO_DISPONIBLE' THEN
        -- Cocina puede ejecutar esta salida directamente. Se genera en la misma
        -- transaccion la evidencia aprobada y la accion de inventario adecuada.
        IF NOT EXISTS (
            SELECT 1
              FROM cancelaciones_comanda_detalle ccd
             WHERE ccd.comanda_detalle_id = OLD.id
               AND ccd.estado_solicitud = 'APROBADA'
               AND ccd.tipo = 'NO_DISPONIBLE_COCINA'
        ) THEN
            INSERT INTO cancelaciones_comanda_detalle (
                comanda_detalle_id, tipo, motivo, estado_solicitud,
                accion_inventario, solicitada_por_id, autorizada_por_id,
                resuelta_en
            ) VALUES (
                OLD.id,
                'NO_DISPONIBLE_COCINA',
                'Platillo marcado no disponible por cocina',
                'APROBADA',
                CASE OLD.estado
                    WHEN 'RECIBIDO' THEN 'REINTEGRAR'
                    ELSE 'REGISTRAR_MERMA'
                END,
                NEW.actualizado_por_id,
                NEW.actualizado_por_id,
                CURRENT_TIMESTAMP
            );
        END IF;
    ELSIF NEW.estado = 'CANCELADO' THEN
        IF v_rol_usuario NOT IN ('WAITER', 'ADMIN') THEN
            RAISE EXCEPTION
                'Solo un mesero o administrador puede ejecutar la cancelacion';
        END IF;

        IF NOT EXISTS (
            SELECT 1
              FROM cancelaciones_comanda_detalle ccd
              JOIN usuarios ua ON ua.id = ccd.autorizada_por_id
              JOIN public.app_users aua ON aua.id = ua.id
              JOIN public.roles ra ON ra.id = aua.role_id
             WHERE ccd.comanda_detalle_id = OLD.id
               AND ccd.estado_solicitud = 'APROBADA'
               AND ccd.tipo <> 'NO_DISPONIBLE_COCINA'
               AND (
                    (OLD.estado = 'RECIBIDO'
                        AND ra.name IN ('WAITER', 'ADMIN'))
                    OR (OLD.estado = 'EN_PREPARACION'
                        AND ra.name = 'ADMIN')
               )
        ) THEN
            RAISE EXCEPTION
                'La cancelacion requiere la autorizacion aprobada correspondiente';
        END IF;
    END IF;

    IF NEW.estado = 'RECIBIDO' AND NEW.recibido_en IS NULL THEN
        NEW.recibido_en := CURRENT_TIMESTAMP;
    ELSIF NEW.estado = 'EN_PREPARACION'
          AND NEW.preparacion_iniciada_en IS NULL THEN
        NEW.preparacion_iniciada_en := CURRENT_TIMESTAMP;
    ELSIF NEW.estado = 'LISTO' AND NEW.listo_en IS NULL THEN
        NEW.listo_en := CURRENT_TIMESTAMP;
    ELSIF NEW.estado = 'ENTREGADO' AND NEW.entregado_en IS NULL THEN
        NEW.entregado_en := CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_transicion_comanda_detalle
BEFORE UPDATE OF estado ON comanda_detalles
FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_comanda_detalle();

-- Si la resolucion aprobada ordena REINTEGRAR, repone exactamente las
-- cantidades descontadas por ese detalle. La operacion es atomica con el
-- cambio de estado y queda registrada en el kardex.
CREATE OR REPLACE FUNCTION fn_reintegrar_cancelacion_comanda_detalle()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_cancelacion_id BIGINT;
    v_restaurante_id BIGINT;
BEGIN
    IF NEW.estado NOT IN ('CANCELADO', 'NO_DISPONIBLE')
       OR NEW.estado = OLD.estado THEN
        RETURN NEW;
    END IF;

    SELECT ccd.id, cu.restaurante_id
      INTO v_cancelacion_id, v_restaurante_id
      FROM cancelaciones_comanda_detalle ccd
      JOIN comanda_detalles cd ON cd.id = ccd.comanda_detalle_id
      JOIN comandas co ON co.id = cd.comanda_id
      JOIN cuentas cu ON cu.id = co.cuenta_id
     WHERE ccd.comanda_detalle_id = NEW.id
       AND ccd.estado_solicitud = 'APROBADA'
       AND ccd.accion_inventario = 'REINTEGRAR';

    IF NOT FOUND THEN
        RETURN NEW;
    END IF;

    INSERT INTO movimientos_inventario (
        restaurante_id, insumo_id, tipo, cantidad,
        costo_unitario_snapshot, cancelacion_comanda_detalle_id,
        motivo, usuario_responsable_id
    )
    SELECT
        v_restaurante_id,
        mi.insumo_id,
        'REINTEGRO_CANCELACION',
        mi.cantidad,
        mi.costo_unitario_snapshot,
        v_cancelacion_id,
        'Reintegro automatico por cancelacion del detalle ' || NEW.id,
        NEW.actualizado_por_id
      FROM movimientos_inventario mi
     WHERE mi.comanda_detalle_id = NEW.id
       AND mi.tipo = 'SALIDA_VENTA'
     ORDER BY mi.insumo_id;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_reintegrar_cancelacion_comanda_detalle
AFTER UPDATE OF estado ON comanda_detalles
FOR EACH ROW EXECUTE FUNCTION fn_reintegrar_cancelacion_comanda_detalle();

CREATE OR REPLACE FUNCTION fn_bloquear_eliminacion_detalle_enviado()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_comanda VARCHAR(20);
BEGIN
    SELECT estado INTO v_estado_comanda
      FROM comandas
     WHERE id = OLD.comanda_id;

    IF v_estado_comanda <> 'BORRADOR' THEN
        RAISE EXCEPTION
            'Un platillo enviado no se elimina; debe registrarse su cancelacion';
    END IF;

    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_bloquear_eliminacion_detalle_enviado
BEFORE DELETE ON comanda_detalles
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion_detalle_enviado();

CREATE OR REPLACE FUNCTION fn_validar_transicion_comanda()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_cuenta VARCHAR(25);
BEGIN
    IF NEW.estado = OLD.estado THEN
        RETURN NEW;
    END IF;

    IF NOT (
        (OLD.estado = 'BORRADOR' AND NEW.estado = 'RECIBIDA')
        OR (OLD.estado = 'RECIBIDA'
            AND NEW.estado IN ('EN_PREPARACION', 'CANCELADA'))
        OR (OLD.estado = 'EN_PREPARACION'
            AND NEW.estado IN ('LISTA', 'CANCELADA'))
        OR (OLD.estado = 'LISTA' AND NEW.estado = 'ENTREGADA')
    ) THEN
        RAISE EXCEPTION
            'Transicion de comanda no valida: % -> %', OLD.estado, NEW.estado;
    END IF;

    IF OLD.estado = 'BORRADOR' AND NEW.estado = 'RECIBIDA' THEN
        SELECT cu.estado
          INTO v_estado_cuenta
          FROM cuentas cu
         WHERE cu.id = NEW.cuenta_id
         FOR SHARE;

        IF v_estado_cuenta IS DISTINCT FROM 'ABIERTA' THEN
            RAISE EXCEPTION
                'Solo se pueden enviar comandas de una cuenta ABIERTA';
        END IF;
    END IF;

    IF NEW.estado = 'LISTA' AND EXISTS (
        SELECT 1
          FROM comanda_detalles cd
         WHERE cd.comanda_id = NEW.id
           AND cd.estado NOT IN ('LISTO', 'CANCELADO', 'NO_DISPONIBLE')
    ) THEN
        RAISE EXCEPTION
            'La comanda no puede marcarse LISTA con platillos pendientes';
    END IF;

    IF NEW.estado = 'ENTREGADA' AND EXISTS (
        SELECT 1
          FROM comanda_detalles cd
         WHERE cd.comanda_id = NEW.id
           AND cd.estado NOT IN ('ENTREGADO', 'CANCELADO', 'NO_DISPONIBLE')
    ) THEN
        RAISE EXCEPTION
            'La comanda no puede entregarse con platillos pendientes';
    END IF;

    IF NEW.estado = 'CANCELADA' AND EXISTS (
        SELECT 1
          FROM comanda_detalles cd
         WHERE cd.comanda_id = NEW.id
           AND cd.estado NOT IN ('CANCELADO', 'NO_DISPONIBLE')
    ) THEN
        RAISE EXCEPTION
            'La comanda no puede cancelarse mientras conserve platillos activos';
    END IF;

    IF NEW.estado = 'RECIBIDA' AND NEW.enviada_en IS NULL THEN
        NEW.enviada_en := CURRENT_TIMESTAMP;
    END IF;

    IF NEW.estado IN ('ENTREGADA', 'CANCELADA')
       AND NEW.finalizada_en IS NULL THEN
        NEW.finalizada_en := CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_transicion_comanda
BEFORE UPDATE OF estado ON comandas
FOR EACH ROW EXECUTE FUNCTION fn_validar_transicion_comanda();

-- Al cambiar una comanda de BORRADOR a RECIBIDA, descuenta en una sola
-- transaccion las recetas base, los componentes de combos y los modificadores.
-- El trigger de kardex bloquea cada insumo y revierte todo si alguno no alcanza.
CREATE OR REPLACE FUNCTION fn_descontar_inventario_comanda()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante_id BIGINT;
BEGIN
    IF OLD.estado <> 'BORRADOR' OR NEW.estado <> 'RECIBIDA' THEN
        RETURN NEW;
    END IF;

    SELECT restaurante_id INTO v_restaurante_id
      FROM cuentas
     WHERE id = NEW.cuenta_id
     FOR SHARE;

    IF NOT EXISTS (
        SELECT 1 FROM comanda_detalles cd
         WHERE cd.comanda_id = NEW.id
           AND cd.estado = 'BORRADOR'
    ) THEN
        RAISE EXCEPTION 'No se puede enviar una comanda sin platillos';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM comanda_detalles cd
          LEFT JOIN platillos p ON p.id = cd.platillo_id
          LEFT JOIN receta_versiones rv ON rv.id = cd.receta_version_id
         WHERE cd.comanda_id = NEW.id
           AND cd.platillo_id IS NOT NULL
           AND (
                p.activo = FALSE
                OR p.disponible_manual = FALSE
                OR rv.platillo_id IS DISTINCT FROM cd.platillo_id
                OR rv.estado IS DISTINCT FROM 'VIGENTE'
           )
    ) THEN
        RAISE EXCEPTION
            'La comanda contiene un platillo no disponible o una receta no vigente';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM comanda_detalles cd
          JOIN combos c ON c.id = cd.combo_id
         WHERE cd.comanda_id = NEW.id
           AND cd.combo_id IS NOT NULL
           AND (
                c.activo = FALSE
                OR c.disponible_manual = FALSE
                OR (c.fecha_inicio IS NOT NULL
                    AND CURRENT_TIMESTAMP < c.fecha_inicio)
                OR (c.fecha_fin IS NOT NULL
                    AND CURRENT_TIMESTAMP > c.fecha_fin)
                OR NOT EXISTS (
                    SELECT 1
                      FROM comanda_detalle_combo_recetas cdcr
                     WHERE cdcr.comanda_detalle_id = cd.id
                )
                OR EXISTS (
                    SELECT 1
                      FROM comanda_detalle_combo_recetas cdcr
                      LEFT JOIN receta_versiones rv
                        ON rv.id = cdcr.receta_version_id
                     WHERE cdcr.comanda_detalle_id = cd.id
                       AND (
                            rv.platillo_id IS DISTINCT FROM cdcr.platillo_id
                            OR NOT EXISTS (
                                SELECT 1
                                  FROM receta_detalles rd
                                 WHERE rd.receta_version_id = cdcr.receta_version_id
                            )
                       )
                )
           )
    ) THEN
        RAISE EXCEPTION
            'La comanda contiene un combo no disponible o incompleto';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM (
                SELECT i.activo
                  FROM comanda_detalles cd
                  JOIN receta_detalles rd
                    ON rd.receta_version_id = cd.receta_version_id
                  JOIN insumos i ON i.id = rd.insumo_id
                 WHERE cd.comanda_id = NEW.id
                   AND cd.platillo_id IS NOT NULL

                UNION ALL

                SELECT i.activo
                  FROM comanda_detalles cd
                  JOIN comanda_detalle_combo_recetas cdcr
                    ON cdcr.comanda_detalle_id = cd.id
                  JOIN receta_detalles rd
                    ON rd.receta_version_id = cdcr.receta_version_id
                  JOIN insumos i ON i.id = rd.insumo_id
                 WHERE cd.comanda_id = NEW.id
                   AND cd.combo_id IS NOT NULL

                UNION ALL

                SELECT i.activo
                  FROM comanda_detalles cd
                  JOIN comanda_detalle_modificadores cdm
                    ON cdm.comanda_detalle_id = cd.id
                  JOIN receta_modificador_detalles rmd
                    ON rmd.receta_modificador_version_id =
                       cdm.receta_modificador_version_id
                  JOIN insumos i ON i.id = rmd.insumo_id
                 WHERE cd.comanda_id = NEW.id
          ) ingredientes
         WHERE ingredientes.activo = FALSE
    ) THEN
        RAISE EXCEPTION
            'La comanda requiere un insumo inactivo';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM comanda_detalles cd
          JOIN comanda_detalle_combo_recetas cdcr
            ON cdcr.comanda_detalle_id = cd.id
          JOIN platillos p ON p.id = cdcr.platillo_id
         WHERE cd.comanda_id = NEW.id
           AND cd.combo_id IS NOT NULL
           AND (p.activo = FALSE OR p.disponible_manual = FALSE)
    ) THEN
        RAISE EXCEPTION
            'La comanda contiene un combo con componentes no disponibles';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM comanda_detalles cd
          JOIN platillo_modificadores pm
            ON pm.platillo_id = cd.platillo_id
           AND pm.obligatorio = TRUE
         WHERE cd.comanda_id = NEW.id
           AND NOT EXISTS (
                SELECT 1
                  FROM comanda_detalle_modificadores cdm
                 WHERE cdm.comanda_detalle_id = cd.id
                   AND cdm.modificador_id = pm.modificador_id
           )
    ) THEN
        RAISE EXCEPTION
            'La comanda omite uno o mas modificadores obligatorios';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM comanda_detalles cd
          JOIN comanda_detalle_modificadores cdm
            ON cdm.comanda_detalle_id = cd.id
          JOIN modificadores m ON m.id = cdm.modificador_id
          LEFT JOIN receta_modificador_versiones rmv
            ON rmv.id = cdm.receta_modificador_version_id
         WHERE cd.comanda_id = NEW.id
           AND (
                m.activo = FALSE
                OR (
                    cdm.receta_modificador_version_id IS NOT NULL
                    AND rmv.estado IS DISTINCT FROM 'VIGENTE'
                )
           )
    ) THEN
        RAISE EXCEPTION
            'La comanda contiene un modificador inactivo o una receta no vigente';
    END IF;

    WITH requerimientos AS (
        SELECT
            cd.id AS comanda_detalle_id,
            rd.insumo_id,
            cd.cantidad
                * rd.cantidad
                * ur.factor_a_base
                / us.factor_a_base AS cantidad_stock
          FROM comanda_detalles cd
          JOIN receta_detalles rd
            ON rd.receta_version_id = cd.receta_version_id
          JOIN insumos i ON i.id = rd.insumo_id
          JOIN unidades_medida ur ON ur.id = rd.unidad_medida_id
          JOIN unidades_medida us ON us.id = i.unidad_stock_id
         WHERE cd.comanda_id = NEW.id
           AND cd.platillo_id IS NOT NULL

        UNION ALL

        SELECT
            cd.id AS comanda_detalle_id,
            rd.insumo_id,
            cd.cantidad
                * cdcr.cantidad_platillo
                * rd.cantidad
                * ur.factor_a_base
                / us.factor_a_base AS cantidad_stock
          FROM comanda_detalles cd
          JOIN comanda_detalle_combo_recetas cdcr
            ON cdcr.comanda_detalle_id = cd.id
          JOIN receta_detalles rd
            ON rd.receta_version_id = cdcr.receta_version_id
          JOIN insumos i ON i.id = rd.insumo_id
          JOIN unidades_medida ur ON ur.id = rd.unidad_medida_id
          JOIN unidades_medida us ON us.id = i.unidad_stock_id
         WHERE cd.comanda_id = NEW.id
           AND cd.combo_id IS NOT NULL

        UNION ALL

        SELECT
            cd.id AS comanda_detalle_id,
            rmd.insumo_id,
            cd.cantidad
                * cdm.cantidad
                * rmd.cantidad
                * ur.factor_a_base
                / us.factor_a_base
                * CASE rmd.tipo_ajuste
                    WHEN 'AGREGAR' THEN 1
                    ELSE -1
                  END AS cantidad_stock
          FROM comanda_detalles cd
          JOIN comanda_detalle_modificadores cdm
            ON cdm.comanda_detalle_id = cd.id
          JOIN receta_modificador_detalles rmd
            ON rmd.receta_modificador_version_id =
               cdm.receta_modificador_version_id
          JOIN insumos i ON i.id = rmd.insumo_id
          JOIN unidades_medida ur ON ur.id = rmd.unidad_medida_id
          JOIN unidades_medida us ON us.id = i.unidad_stock_id
         WHERE cd.comanda_id = NEW.id
    ),
    totales AS (
        SELECT
            comanda_detalle_id,
            insumo_id,
            ROUND(GREATEST(SUM(cantidad_stock), 0), 4) AS cantidad_stock
          FROM requerimientos
         GROUP BY comanda_detalle_id, insumo_id
    )
    INSERT INTO movimientos_inventario (
        restaurante_id, insumo_id, tipo, cantidad,
        comanda_detalle_id, motivo, usuario_responsable_id
    )
    SELECT
        v_restaurante_id,
        t.insumo_id,
        'SALIDA_VENTA',
        t.cantidad_stock,
        t.comanda_detalle_id,
        'Consumo automatico al enviar comanda ' || NEW.id,
        NEW.mesero_id
      FROM totales t
     WHERE t.cantidad_stock > 0
     ORDER BY t.insumo_id, t.comanda_detalle_id;

    UPDATE comanda_detalles
       SET estado = 'RECIBIDO',
           recibido_en = COALESCE(recibido_en, NEW.enviada_en),
           actualizado_por_id = NEW.mesero_id
     WHERE comanda_id = NEW.id
       AND estado = 'BORRADOR';

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_descontar_inventario_comanda
AFTER UPDATE OF estado ON comandas
FOR EACH ROW EXECUTE FUNCTION fn_descontar_inventario_comanda();

-- El stock y su costo promedio no se corrigen con UPDATE directo. Toda
-- variacion debe dejar una fila en el kardex y pasar por la funcion interna.
CREATE OR REPLACE FUNCTION fn_bloquear_actualizacion_directa_stock()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF current_setting(
        'restaurante.permitir_actualizacion_stock', TRUE
    ) IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION
            'No actualice stock o costo directamente; registre un movimiento de inventario';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_bloquear_actualizacion_directa_stock
BEFORE UPDATE OF stock_actual, costo_unitario_actual ON insumos
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_actualizacion_directa_stock();

-- El kardex bloquea el insumo y actualiza el stock dentro de la misma
-- transaccion, impidiendo consumos concurrentes de la misma ultima existencia.
CREATE OR REPLACE FUNCTION fn_aplicar_movimiento_inventario()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_stock               DECIMAL(18,4);
    v_costo               DECIMAL(14,4);
    v_costo_promedio      DECIMAL(14,4);
    v_restaurante_insumo  BIGINT;
    v_restaurante_origen  BIGINT;
    v_insumo_origen       BIGINT;
    v_cantidad_origen     DECIMAL(18,4);
    v_costo_origen        DECIMAL(14,4);
    v_usuario_origen      BIGINT;
    v_accion_inventario   VARCHAR(25);
    v_estado_cancelacion  VARCHAR(15);
BEGIN
    SELECT stock_actual, costo_unitario_actual, restaurante_id
      INTO v_stock, v_costo, v_restaurante_insumo
      FROM insumos
     WHERE id = NEW.insumo_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El insumo del movimiento no existe';
    END IF;

    IF v_restaurante_insumo <> NEW.restaurante_id THEN
        RAISE EXCEPTION
            'El insumo no pertenece al restaurante del movimiento';
    END IF;

    IF NEW.tipo = 'ENTRADA_COMPRA' THEN
        SELECT
            eid.insumo_id, ei.restaurante_id, eid.cantidad,
            eid.costo_unitario, ei.recibida_por_id
          INTO
            v_insumo_origen, v_restaurante_origen, v_cantidad_origen,
            v_costo_origen, v_usuario_origen
          FROM entrada_inventario_detalles eid
          JOIN entradas_inventario ei ON ei.id = eid.entrada_id
         WHERE eid.id = NEW.entrada_detalle_id;

        IF NOT FOUND
           OR v_insumo_origen <> NEW.insumo_id
           OR v_restaurante_origen <> NEW.restaurante_id
           OR v_cantidad_origen <> NEW.cantidad
           OR v_usuario_origen <> NEW.usuario_responsable_id THEN
            RAISE EXCEPTION
                'El movimiento no coincide con el detalle de entrada';
        END IF;

        IF NEW.costo_unitario_snapshot IS NOT NULL
           AND NEW.costo_unitario_snapshot <> v_costo_origen THEN
            RAISE EXCEPTION
                'El costo del movimiento no coincide con la compra';
        END IF;
        NEW.costo_unitario_snapshot := v_costo_origen;

    ELSIF NEW.tipo = 'SALIDA_MERMA' THEN
        SELECT
            md.insumo_id, me.restaurante_id, md.cantidad,
            md.costo_unitario_snapshot, me.registrada_por_id
          INTO
            v_insumo_origen, v_restaurante_origen, v_cantidad_origen,
            v_costo_origen, v_usuario_origen
          FROM merma_detalles md
          JOIN mermas me ON me.id = md.merma_id
         WHERE md.id = NEW.merma_detalle_id;

        IF NOT FOUND
           OR v_insumo_origen <> NEW.insumo_id
           OR v_restaurante_origen <> NEW.restaurante_id
           OR v_cantidad_origen <> NEW.cantidad
           OR v_usuario_origen <> NEW.usuario_responsable_id THEN
            RAISE EXCEPTION
                'El movimiento no coincide con el detalle de merma';
        END IF;
        NEW.costo_unitario_snapshot := v_costo_origen;

    ELSIF NEW.tipo = 'SALIDA_VENTA' THEN
        SELECT cu.restaurante_id
          INTO v_restaurante_origen
          FROM comanda_detalles cd
          JOIN comandas co ON co.id = cd.comanda_id
          JOIN cuentas cu ON cu.id = co.cuenta_id
         WHERE cd.id = NEW.comanda_detalle_id;

        IF v_restaurante_origen IS DISTINCT FROM NEW.restaurante_id THEN
            RAISE EXCEPTION
                'El platillo vendido no pertenece al restaurante del movimiento';
        END IF;

    ELSIF NEW.tipo = 'REINTEGRO_CANCELACION' THEN
        SELECT
            cu.restaurante_id,
            ccd.accion_inventario,
            ccd.estado_solicitud
          INTO
            v_restaurante_origen,
            v_accion_inventario,
            v_estado_cancelacion
          FROM cancelaciones_comanda_detalle ccd
          JOIN comanda_detalles cd ON cd.id = ccd.comanda_detalle_id
          JOIN comandas co ON co.id = cd.comanda_id
          JOIN cuentas cu ON cu.id = co.cuenta_id
         WHERE ccd.id = NEW.cancelacion_comanda_detalle_id;

        IF v_restaurante_origen IS DISTINCT FROM NEW.restaurante_id
           OR v_accion_inventario IS DISTINCT FROM 'REINTEGRAR'
           OR v_estado_cancelacion IS DISTINCT FROM 'APROBADA' THEN
            RAISE EXCEPTION
                'La cancelacion no autoriza el reintegro de inventario';
        END IF;

        SELECT mi.cantidad, mi.costo_unitario_snapshot
          INTO v_cantidad_origen, v_costo_origen
          FROM cancelaciones_comanda_detalle ccd
          JOIN movimientos_inventario mi
            ON mi.comanda_detalle_id = ccd.comanda_detalle_id
           AND mi.tipo = 'SALIDA_VENTA'
           AND mi.insumo_id = NEW.insumo_id
         WHERE ccd.id = NEW.cancelacion_comanda_detalle_id;

        IF NOT FOUND OR v_cantidad_origen <> NEW.cantidad THEN
            RAISE EXCEPTION
                'El reintegro debe coincidir con la salida original del insumo';
        END IF;

        IF NEW.costo_unitario_snapshot IS NOT NULL
           AND NEW.costo_unitario_snapshot <> v_costo_origen THEN
            RAISE EXCEPTION
                'El costo del reintegro no coincide con la salida original';
        END IF;
        NEW.costo_unitario_snapshot := v_costo_origen;
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.usuario_responsable_id,
        NEW.restaurante_id,
        CASE
            WHEN NEW.tipo IN (
                'ENTRADA_COMPRA', 'SALIDA_MERMA',
                'AJUSTE_ENTRADA', 'AJUSTE_SALIDA'
            ) THEN 'ADMIN'
            ELSE NULL
        END
    );

    NEW.stock_anterior := v_stock;
    NEW.costo_unitario_snapshot :=
        COALESCE(NEW.costo_unitario_snapshot, v_costo);

    IF NEW.costo_unitario_snapshot < 0 THEN
        RAISE EXCEPTION 'El costo del movimiento no puede ser negativo';
    END IF;

    PERFORM set_config(
        'restaurante.permitir_actualizacion_stock', 'true', TRUE
    );

    IF NEW.tipo IN (
        'ENTRADA_COMPRA', 'AJUSTE_ENTRADA', 'REINTEGRO_CANCELACION'
    ) THEN
        NEW.stock_resultante := v_stock + NEW.cantidad;

        IF NEW.tipo = 'ENTRADA_COMPRA' THEN
            v_costo_promedio := ROUND(
                ((v_stock * v_costo)
                    + (NEW.cantidad * NEW.costo_unitario_snapshot))
                / (v_stock + NEW.cantidad),
                4
            );

            IF v_costo_promedio <> v_costo THEN
                INSERT INTO historial_costos_insumo (
                    insumo_id, costo_anterior, costo_nuevo, motivo,
                    usuario_id, vigente_desde
                ) VALUES (
                    NEW.insumo_id, v_costo, v_costo_promedio,
                    'Actualizacion por costo promedio de entrada de inventario',
                    NEW.usuario_responsable_id, NEW.creado_en
                );
            END IF;

            UPDATE insumos
               SET stock_actual = NEW.stock_resultante,
                   costo_unitario_actual = v_costo_promedio
             WHERE id = NEW.insumo_id;
        ELSE
            UPDATE insumos
               SET stock_actual = NEW.stock_resultante
             WHERE id = NEW.insumo_id;
        END IF;
    ELSE
        IF v_stock < NEW.cantidad THEN
            RAISE EXCEPTION
                'Stock insuficiente: el movimiento produciria inventario negativo';
        END IF;

        NEW.stock_resultante := v_stock - NEW.cantidad;

        UPDATE insumos
           SET stock_actual = NEW.stock_resultante
         WHERE id = NEW.insumo_id;
    END IF;

    PERFORM set_config(
        'restaurante.permitir_actualizacion_stock', 'false', TRUE
    );

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_aplicar_movimiento_inventario
BEFORE INSERT ON movimientos_inventario
FOR EACH ROW EXECUTE FUNCTION fn_aplicar_movimiento_inventario();

CREATE OR REPLACE FUNCTION fn_notificar_stock_bajo()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_nombre       VARCHAR(120);
    v_stock_minimo DECIMAL(18,4);
    v_rol_admin_id BIGINT;
BEGIN
    SELECT nombre, stock_minimo
      INTO v_nombre, v_stock_minimo
      FROM insumos
     WHERE id = NEW.insumo_id;

    IF NEW.stock_resultante <= v_stock_minimo THEN
        SELECT id INTO v_rol_admin_id
          FROM public.roles
         WHERE name = 'ADMIN';

        IF NOT EXISTS (
            SELECT 1
              FROM notificaciones n
             WHERE n.restaurante_id = NEW.restaurante_id
               AND n.tipo = 'STOCK_BAJO'
               AND n.entidad = 'INSUMO'
               AND n.entidad_id = NEW.insumo_id::TEXT
               AND n.leida = FALSE
        ) THEN
            INSERT INTO notificaciones (
                restaurante_id, rol_destinatario_id, tipo, titulo,
                mensaje, entidad, entidad_id, prioridad
            ) VALUES (
                NEW.restaurante_id,
                v_rol_admin_id,
                'STOCK_BAJO',
                'Insumo con stock bajo',
                'El insumo ' || v_nombre || ' alcanzo el nivel minimo de stock',
                'INSUMO',
                NEW.insumo_id::TEXT,
                CASE
                    WHEN NEW.stock_resultante = 0 THEN 'CRITICA'
                    ELSE 'ALTA'
                END
            );
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_notificar_stock_bajo
AFTER INSERT ON movimientos_inventario
FOR EACH ROW EXECUTE FUNCTION fn_notificar_stock_bajo();

-- Los detalles de compras y mermas generan su kardex automaticamente. Asi no
-- puede existir un documento de inventario sin el movimiento correspondiente.
CREATE OR REPLACE FUNCTION fn_generar_movimiento_documento_inventario()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante_id BIGINT;
    v_usuario_id     BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'entrada_inventario_detalles' THEN
        SELECT restaurante_id, recibida_por_id
          INTO v_restaurante_id, v_usuario_id
          FROM entradas_inventario
         WHERE id = NEW.entrada_id;

        INSERT INTO movimientos_inventario (
            restaurante_id, insumo_id, tipo, cantidad,
            costo_unitario_snapshot, entrada_detalle_id,
            motivo, usuario_responsable_id
        ) VALUES (
            v_restaurante_id,
            NEW.insumo_id,
            'ENTRADA_COMPRA',
            NEW.cantidad,
            NEW.costo_unitario,
            NEW.id,
            'Entrada de inventario registrada automaticamente',
            v_usuario_id
        );
    ELSE
        SELECT restaurante_id, registrada_por_id
          INTO v_restaurante_id, v_usuario_id
          FROM mermas
         WHERE id = NEW.merma_id;

        INSERT INTO movimientos_inventario (
            restaurante_id, insumo_id, tipo, cantidad,
            costo_unitario_snapshot, merma_detalle_id,
            motivo, usuario_responsable_id
        ) VALUES (
            v_restaurante_id,
            NEW.insumo_id,
            'SALIDA_MERMA',
            NEW.cantidad,
            NEW.costo_unitario_snapshot,
            NEW.id,
            'Merma registrada automaticamente',
            v_usuario_id
        );
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_movimiento_entrada_inventario
AFTER INSERT ON entrada_inventario_detalles
FOR EACH ROW EXECUTE FUNCTION fn_generar_movimiento_documento_inventario();

CREATE TRIGGER trg_movimiento_merma_inventario
AFTER INSERT ON merma_detalles
FOR EACH ROW EXECUTE FUNCTION fn_generar_movimiento_documento_inventario();

CREATE OR REPLACE FUNCTION fn_bloquear_libro_mayor()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    RAISE EXCEPTION
        'La tabla % es inmutable; registre un movimiento compensatorio',
        TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_movimiento_inventario_inmutable
BEFORE UPDATE OR DELETE ON movimientos_inventario
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE TRIGGER trg_entrada_detalle_inmutable
BEFORE UPDATE OR DELETE ON entrada_inventario_detalles
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE TRIGGER trg_merma_detalle_inmutable
BEFORE UPDATE OR DELETE ON merma_detalles
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE TRIGGER trg_historial_costos_inmutable
BEFORE UPDATE OR DELETE ON historial_costos_insumo
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE TRIGGER trg_cancelacion_detalle_inmutable
BEFORE DELETE ON cancelaciones_comanda_detalle
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE TRIGGER trg_transaccion_caja_inmutable
BEFORE UPDATE OR DELETE ON transacciones_caja
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE OR REPLACE FUNCTION fn_validar_documento_inventario()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF TG_TABLE_NAME = 'entradas_inventario' THEN
        IF TG_OP = 'UPDATE' AND EXISTS (
            SELECT 1
              FROM entrada_inventario_detalles eid
             WHERE eid.entrada_id = OLD.id
        ) THEN
            RAISE EXCEPTION
                'Una entrada con movimientos de kardex es inmutable';
        END IF;

        PERFORM fn_validar_usuario_operacion(
            NEW.recibida_por_id, NEW.restaurante_id, 'ADMIN'
        );
    ELSE
        IF TG_OP = 'UPDATE' AND EXISTS (
            SELECT 1
              FROM merma_detalles md
             WHERE md.merma_id = OLD.id
        ) THEN
            RAISE EXCEPTION
                'Una merma con movimientos de kardex es inmutable';
        END IF;

        PERFORM fn_validar_usuario_operacion(
            NEW.registrada_por_id, NEW.restaurante_id, 'ADMIN'
        );
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_entrada_inventario
BEFORE INSERT OR UPDATE ON entradas_inventario
FOR EACH ROW EXECUTE FUNCTION fn_validar_documento_inventario();

CREATE TRIGGER trg_validar_merma
BEFORE INSERT OR UPDATE ON mermas
FOR EACH ROW EXECUTE FUNCTION fn_validar_documento_inventario();

CREATE OR REPLACE FUNCTION fn_validar_turno_caja()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_restaurante BIGINT;
    v_caja_activa BOOLEAN;
    v_esperado    DECIMAL(12,2);
BEGIN
    SELECT restaurante_id, activo
      INTO v_restaurante, v_caja_activa
      FROM cajas
     WHERE id = NEW.caja_id
     FOR SHARE;

    IF NOT FOUND OR NOT v_caja_activa THEN
        RAISE EXCEPTION 'La caja no existe o esta inactiva';
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.cajero_id, v_restaurante, 'CASHIER'
    );

    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'ABIERTA' THEN
            RAISE EXCEPTION 'Un turno nuevo debe iniciar ABIERTO';
        END IF;
        RETURN NEW;
    END IF;

    IF OLD.estado = 'CERRADA' THEN
        RAISE EXCEPTION 'Un turno de caja cerrado es inmutable';
    END IF;

    IF NEW.caja_id <> OLD.caja_id
       OR NEW.cajero_id <> OLD.cajero_id
       OR NEW.monto_inicial_efectivo <> OLD.monto_inicial_efectivo
       OR NEW.abierta_en <> OLD.abierta_en
       OR NEW.observaciones_apertura IS DISTINCT FROM OLD.observaciones_apertura THEN
        RAISE EXCEPTION
            'La caja, cajero y datos de apertura de un turno son inmutables';
    END IF;

    IF NEW.estado = 'CERRADA' THEN
        SELECT ROUND(
            OLD.monto_inicial_efectivo + COALESCE(SUM(CASE
                WHEN mp.afecta_efectivo AND f.estado = 'EMITIDA' THEN p.monto
                ELSE 0
            END), 0),
            2
        )
          INTO v_esperado
          FROM facturas f
          LEFT JOIN pagos p ON p.factura_id = f.id
          LEFT JOIN metodos_pago mp ON mp.id = p.metodo_pago_id
         WHERE f.turno_caja_id = OLD.id;

        NEW.efectivo_esperado_cierre := v_esperado;
        NEW.diferencia_cierre := ROUND(
            NEW.efectivo_real_cierre - v_esperado,
            2
        );
        NEW.cerrada_en := COALESCE(NEW.cerrada_en, CURRENT_TIMESTAMP);
    ELSIF NEW.estado <> 'ABIERTA' THEN
        RAISE EXCEPTION 'Transicion de turno de caja no valida';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_turno_caja
BEFORE INSERT OR UPDATE ON turnos_caja
FOR EACH ROW EXECUTE FUNCTION fn_validar_turno_caja();

-- Impide emitir documentos sin un turno abierto y valida que cuenta,
-- configuracion, cajero, mesero y restaurante pertenezcan a la misma operacion.
CREATE OR REPLACE FUNCTION fn_validar_factura()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_turno          VARCHAR(10);
    v_cajero_turno          BIGINT;
    v_restaurante_turno     BIGINT;
    v_restaurante_cuenta    BIGINT;
    v_mesero_cuenta         BIGINT;
    v_estado_cuenta         VARCHAR(25);
    v_restaurante_config    BIGINT;
    v_estado_config         VARCHAR(15);
    v_impuesto_config       DECIMAL(7,4);
    v_propina_config        DECIMAL(7,4);
    v_propina_editable      BOOLEAN;
    v_puntos_por_moneda     DECIMAL(12,4);
    v_valor_punto_config    DECIMAL(12,4);
    v_cuenta_subcuenta      BIGINT;
    v_estado_subcuenta      VARCHAR(15);
    v_tipo_subcuenta        VARCHAR(15);
    v_subtotal_subcuenta    DECIMAL(12,2);
    v_cliente_cuenta        BIGINT;
    v_restaurante_cliente   BIGINT;
BEGIN
    IF NEW.estado <> 'EMITIDA' THEN
        RAISE EXCEPTION 'Una factura nueva debe emitirse en estado EMITIDA';
    END IF;

    SELECT tc.estado, tc.cajero_id, ca.restaurante_id
      INTO v_estado_turno, v_cajero_turno, v_restaurante_turno
      FROM turnos_caja tc
      JOIN cajas ca ON ca.id = tc.caja_id
     WHERE tc.id = NEW.turno_caja_id
     FOR UPDATE OF tc;

    IF NOT FOUND OR v_estado_turno <> 'ABIERTA' THEN
        RAISE EXCEPTION
            'No se puede facturar: el turno de caja no esta abierto';
    END IF;

    IF v_cajero_turno <> NEW.cajero_id THEN
        RAISE EXCEPTION
            'El cajero de la factura no corresponde al turno de caja';
    END IF;

    IF v_restaurante_turno <> NEW.restaurante_id THEN
        RAISE EXCEPTION
            'El turno de caja no pertenece al restaurante de la factura';
    END IF;

    SELECT restaurante_id, mesero_id, estado, cliente_id
     INTO v_restaurante_cuenta, v_mesero_cuenta, v_estado_cuenta,
          v_cliente_cuenta
      FROM cuentas
     WHERE id = NEW.cuenta_id
     FOR UPDATE;

    IF NOT FOUND
       OR v_restaurante_cuenta <> NEW.restaurante_id
       OR v_mesero_cuenta <> NEW.mesero_id THEN
        RAISE EXCEPTION
            'La cuenta, el restaurante y el mesero de la factura no coinciden';
    END IF;

    IF v_estado_cuenta NOT IN ('LISTA_COBRO', 'PARCIALMENTE_PAGADA') THEN
        RAISE EXCEPTION
            'La cuenta debe estar lista para cobro antes de facturar';
    END IF;

    IF NEW.cliente_id IS DISTINCT FROM v_cliente_cuenta THEN
        RAISE EXCEPTION
            'El cliente de la factura debe coincidir con el de la cuenta';
    END IF;

    IF NEW.cliente_id IS NOT NULL THEN
        SELECT restaurante_id INTO v_restaurante_cliente
          FROM clientes
         WHERE id = NEW.cliente_id;
        IF v_restaurante_cliente IS DISTINCT FROM NEW.restaurante_id THEN
            RAISE EXCEPTION
                'El cliente no pertenece al restaurante de la factura';
        END IF;
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.mesero_id, NEW.restaurante_id, 'WAITER'
    );
    PERFORM fn_validar_usuario_operacion(
        NEW.cajero_id, NEW.restaurante_id, 'CASHIER'
    );

    IF NEW.subcuenta_id IS NOT NULL THEN
        SELECT
            cuenta_id, estado, tipo_division,
            subtotal_snapshot
          INTO
            v_cuenta_subcuenta, v_estado_subcuenta, v_tipo_subcuenta,
            v_subtotal_subcuenta
          FROM subcuentas
         WHERE id = NEW.subcuenta_id
         FOR SHARE;

        IF NOT FOUND
           OR v_cuenta_subcuenta <> NEW.cuenta_id
           OR v_estado_subcuenta <> 'PENDIENTE' THEN
            RAISE EXCEPTION
                'La subcuenta no pertenece a la cuenta o no esta pendiente';
        END IF;

        IF EXISTS (
            SELECT 1
              FROM facturas
             WHERE cuenta_id = NEW.cuenta_id
               AND subcuenta_id IS NULL
               AND estado = 'EMITIDA'
        ) THEN
            RAISE EXCEPTION
                'La cuenta ya posee una factura completa emitida';
        END IF;

        IF ROUND(NEW.subtotal, 2) <> ROUND(v_subtotal_subcuenta, 2) THEN
            RAISE EXCEPTION
                'El subtotal de la factura no coincide con la subcuenta';
        END IF;

        IF v_tipo_subcuenta = 'PERSONAS'
           AND ROUND((
                SELECT COALESCE(SUM(s.porcentaje_asignado), 0)
                  FROM subcuentas s
                 WHERE s.cuenta_id = NEW.cuenta_id
                   AND s.tipo_division = 'PERSONAS'
                   AND s.estado <> 'CANCELADA'
           ), 4) <> 100.0000 THEN
            RAISE EXCEPTION
                'La division por personas debe distribuir exactamente el 100 por ciento';
        END IF;
    ELSIF EXISTS (
        SELECT 1
          FROM facturas
         WHERE cuenta_id = NEW.cuenta_id
           AND estado = 'EMITIDA'
    ) THEN
        RAISE EXCEPTION
            'La cuenta ya posee facturas emitidas y no puede facturarse completa';
    ELSIF EXISTS (
        SELECT 1
          FROM subcuentas s
         WHERE s.cuenta_id = NEW.cuenta_id
           AND s.estado <> 'CANCELADA'
    ) THEN
        RAISE EXCEPTION
            'La cuenta tiene una division activa y debe facturarse por subcuentas';
    END IF;

    SELECT
        restaurante_id, estado, porcentaje_impuesto,
        porcentaje_propina, propina_editable,
        puntos_por_moneda, valor_monetario_punto
      INTO
        v_restaurante_config, v_estado_config, v_impuesto_config,
        v_propina_config, v_propina_editable,
        v_puntos_por_moneda, v_valor_punto_config
      FROM configuraciones_restaurante
     WHERE id = NEW.configuracion_id
     FOR SHARE;

    IF NOT FOUND
       OR v_restaurante_config <> NEW.restaurante_id
       OR v_estado_config <> 'VIGENTE' THEN
        RAISE EXCEPTION
            'La configuracion de la factura no es la vigente del restaurante';
    END IF;

    IF NEW.porcentaje_impuesto <> v_impuesto_config THEN
        RAISE EXCEPTION
            'El impuesto de la factura no coincide con la configuracion vigente';
    END IF;

    IF NOT v_propina_editable
       AND NEW.porcentaje_propina <> v_propina_config THEN
        RAISE EXCEPTION
            'La propina no es editable y debe usar el porcentaje configurado';
    END IF;

    IF NEW.monto_impuesto <> ROUND(
        (NEW.subtotal - NEW.descuento_total)
        * NEW.porcentaje_impuesto / 100,
        2
    ) THEN
        RAISE EXCEPTION
            'El monto de impuesto no coincide con su base y porcentaje';
    END IF;

    IF NEW.monto_propina <> ROUND(
        (NEW.subtotal - NEW.descuento_total)
        * NEW.porcentaje_propina / 100,
        2
    ) THEN
        RAISE EXCEPTION
            'El monto de propina no coincide con su base y porcentaje';
    END IF;

    IF NEW.descuento_puntos <> ROUND(
        NEW.puntos_redimidos * v_valor_punto_config,
        2
    ) THEN
        RAISE EXCEPTION
            'El descuento por puntos no coincide con su valor configurado';
    END IF;

    IF NEW.cliente_id IS NOT NULL
       AND NEW.puntos_otorgados <> FLOOR(
        NEW.total * v_puntos_por_moneda
    )::BIGINT THEN
        RAISE EXCEPTION
            'Los puntos otorgados no coinciden con el total pagado y la configuracion';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM secuencias_facturacion sf
         WHERE sf.restaurante_id = NEW.restaurante_id
           AND sf.tipo_documento = NEW.tipo_documento
           AND sf.serie = NEW.serie
           AND sf.activo = TRUE
           AND sf.ultimo_numero >= NEW.numero_correlativo
    ) THEN
        RAISE EXCEPTION
            'El correlativo no fue reservado con la secuencia de facturacion';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_factura
BEFORE INSERT ON facturas
FOR EACH ROW EXECUTE FUNCTION fn_validar_factura();

-- Permite pagos combinados, serializa los pagos de una factura y evita cobrar
-- de mas. Tambien valida la referencia de los metodos que la requieren.
CREATE OR REPLACE FUNCTION fn_validar_pago()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_total               DECIMAL(12,2);
    v_pagado              DECIMAL(12,2);
    v_estado_factura      VARCHAR(15);
    v_turno_id            BIGINT;
    v_estado_turno        VARCHAR(10);
    v_cajero_id           BIGINT;
    v_afecta_efectivo     BOOLEAN;
    v_requiere_referencia BOOLEAN;
BEGIN
    SELECT f.total, f.estado, f.turno_caja_id, f.cajero_id
      INTO v_total, v_estado_factura, v_turno_id, v_cajero_id
      FROM facturas f
     WHERE f.id = NEW.factura_id
     FOR UPDATE;

    IF NOT FOUND OR v_estado_factura <> 'EMITIDA' THEN
        RAISE EXCEPTION
            'La factura no existe o no se encuentra emitida';
    END IF;

    SELECT estado
      INTO v_estado_turno
      FROM turnos_caja
     WHERE id = v_turno_id
     FOR SHARE;

    IF NOT FOUND OR v_estado_turno <> 'ABIERTA' THEN
        RAISE EXCEPTION
            'No se puede registrar el pago: la caja esta cerrada';
    END IF;

    IF v_cajero_id <> NEW.registrado_por_id THEN
        RAISE EXCEPTION
            'El pago debe ser registrado por el cajero de la factura';
    END IF;

    SELECT afecta_efectivo, requiere_referencia
      INTO v_afecta_efectivo, v_requiere_referencia
      FROM metodos_pago
     WHERE id = NEW.metodo_pago_id
       AND activo = TRUE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El metodo de pago no existe o esta inactivo';
    END IF;

    IF v_requiere_referencia
       AND NULLIF(BTRIM(NEW.referencia), '') IS NULL THEN
        RAISE EXCEPTION
            'El metodo de pago seleccionado requiere una referencia';
    END IF;

    IF v_afecta_efectivo THEN
        NEW.monto_recibido := COALESCE(NEW.monto_recibido, NEW.monto);
        NEW.cambio_entregado := ROUND(NEW.monto_recibido - NEW.monto, 2);
    ELSIF NEW.monto_recibido IS NOT NULL OR NEW.cambio_entregado IS NOT NULL THEN
        RAISE EXCEPTION
            'Monto recibido y cambio solo aplican a pagos en efectivo';
    END IF;

    SELECT COALESCE(SUM(monto), 0)
      INTO v_pagado
      FROM pagos
     WHERE factura_id = NEW.factura_id;

    IF ROUND(v_pagado + NEW.monto, 2) > ROUND(v_total, 2) THEN
        RAISE EXCEPTION 'La suma de pagos supera el total de la factura';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_pago
BEFORE INSERT ON pagos
FOR EACH ROW EXECUTE FUNCTION fn_validar_pago();

CREATE OR REPLACE FUNCTION fn_validar_transaccion_caja()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_turno        VARCHAR(10);
    v_cajero_turno        BIGINT;
    v_restaurante         BIGINT;
    v_monto_inicial       DECIMAL(12,2);
    v_turno_factura       BIGINT;
    v_estado_factura      VARCHAR(15);
    v_propina_factura     DECIMAL(12,2);
    v_descuento_puntos    DECIMAL(12,2);
    v_puntos_redimidos    BIGINT;
    v_factura_pago        BIGINT;
    v_monto_pago          DECIMAL(12,2);
BEGIN
    SELECT tc.estado, tc.cajero_id, ca.restaurante_id,
           tc.monto_inicial_efectivo
      INTO v_estado_turno, v_cajero_turno, v_restaurante, v_monto_inicial
      FROM turnos_caja tc
      JOIN cajas ca ON ca.id = tc.caja_id
     WHERE tc.id = NEW.turno_caja_id
     FOR SHARE OF tc;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El turno de la transaccion de caja no existe';
    END IF;

    IF NEW.tipo = 'CIERRE' AND v_estado_turno <> 'CERRADA' THEN
        RAISE EXCEPTION
            'La transaccion de cierre requiere un turno CERRADO';
    ELSIF NEW.tipo <> 'CIERRE' AND v_estado_turno <> 'ABIERTA' THEN
        RAISE EXCEPTION
            'La transaccion requiere un turno de caja ABIERTO';
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.registrada_por_id, v_restaurante, NULL
    );

    IF NEW.tipo <> 'AJUSTE'
       AND NEW.registrada_por_id <> v_cajero_turno THEN
        RAISE EXCEPTION
            'La transaccion debe registrarla el cajero responsable del turno';
    END IF;

    IF NEW.factura_id IS NOT NULL THEN
        SELECT
            turno_caja_id, estado, monto_propina,
            descuento_puntos, puntos_redimidos
          INTO
            v_turno_factura, v_estado_factura, v_propina_factura,
            v_descuento_puntos, v_puntos_redimidos
          FROM facturas
         WHERE id = NEW.factura_id;

        IF NOT FOUND
           OR v_turno_factura <> NEW.turno_caja_id
           OR (NEW.tipo <> 'AJUSTE' AND v_estado_factura <> 'EMITIDA') THEN
            RAISE EXCEPTION
                'La factura no corresponde al turno de la transaccion';
        END IF;
    END IF;

    IF NEW.pago_id IS NOT NULL THEN
        SELECT factura_id, monto
          INTO v_factura_pago, v_monto_pago
          FROM pagos
         WHERE id = NEW.pago_id;

        IF NOT FOUND OR v_factura_pago <> NEW.factura_id THEN
            RAISE EXCEPTION
                'El pago no corresponde a la factura de la transaccion';
        END IF;
    END IF;

    CASE NEW.tipo
        WHEN 'APERTURA' THEN
            IF NEW.monto <> v_monto_inicial OR NEW.puntos <> 0 THEN
                RAISE EXCEPTION
                    'La apertura debe registrar el monto inicial y cero puntos';
            END IF;
        WHEN 'VENTA' THEN
            IF NEW.monto <> v_monto_pago OR NEW.puntos <> 0 THEN
                RAISE EXCEPTION
                    'La venta debe coincidir con el pago registrado';
            END IF;
        WHEN 'PROPINA' THEN
            IF NEW.monto <> v_propina_factura OR NEW.puntos <> 0 THEN
                RAISE EXCEPTION
                    'La propina no coincide con la factura';
            END IF;
        WHEN 'REDENCION_PUNTOS' THEN
            IF NEW.monto <> v_descuento_puntos
               OR NEW.puntos <> v_puntos_redimidos THEN
                RAISE EXCEPTION
                    'La redencion no coincide con la factura';
            END IF;
        WHEN 'CIERRE' THEN
            IF NEW.monto <> 0 OR NEW.puntos <> 0 THEN
                RAISE EXCEPTION
                    'El cierre se registra con monto y puntos en cero';
            END IF;
        ELSE
            NULL;
    END CASE;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_transaccion_caja
BEFORE INSERT ON transacciones_caja
FOR EACH ROW EXECUTE FUNCTION fn_validar_transaccion_caja();

CREATE OR REPLACE FUNCTION fn_validar_transacciones_turno_diferido()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado VARCHAR(10);
BEGIN
    SELECT estado INTO v_estado
      FROM turnos_caja
     WHERE id = NEW.id;

    IF NOT FOUND THEN
        RETURN NEW;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM transacciones_caja tc
         WHERE tc.turno_caja_id = NEW.id
           AND tc.tipo = 'APERTURA'
           AND tc.monto = NEW.monto_inicial_efectivo
           AND tc.registrada_por_id = NEW.cajero_id
    ) THEN
        RAISE EXCEPTION
            'El turno % debe registrar una apertura que coincida con monto y cajero',
            NEW.id;
    END IF;

    IF v_estado = 'CERRADA' AND NOT EXISTS (
        SELECT 1
          FROM transacciones_caja tc
         WHERE tc.turno_caja_id = NEW.id
           AND tc.tipo = 'CIERRE'
    ) THEN
        RAISE EXCEPTION
            'El turno % debe registrar su transaccion de cierre', NEW.id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_transacciones_turno_diferido
AFTER INSERT OR UPDATE ON turnos_caja
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_validar_transacciones_turno_diferido();

CREATE OR REPLACE FUNCTION fn_validar_totales_factura_diferido()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_factura_id         BIGINT;
    v_estado             VARCHAR(15);
    v_cuenta_id          BIGINT;
    v_subcuenta_id       BIGINT;
    v_subtotal           DECIMAL(12,2);
    v_total              DECIMAL(12,2);
    v_puntos_otorgados   BIGINT;
    v_puntos_redimidos   BIGINT;
    v_monto_propina      DECIMAL(12,2);
    v_descuento_puntos   DECIMAL(12,2);
    v_suma_detalles      DECIMAL(14,2);
    v_suma_pagos         DECIMAL(14,2);
    v_puntos_otorgados_registrados BIGINT;
    v_puntos_redimidos_registrados BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'facturas' THEN
        v_factura_id := COALESCE(NEW.id, OLD.id);
    ELSIF TG_TABLE_NAME IN ('factura_detalles', 'pagos') THEN
        v_factura_id := COALESCE(NEW.factura_id, OLD.factura_id);
    ELSE
        SELECT factura_id INTO v_factura_id
          FROM factura_detalles
         WHERE id = COALESCE(NEW.factura_detalle_id, OLD.factura_detalle_id);
    END IF;

    SELECT
        estado, cuenta_id, subcuenta_id, subtotal, total,
        puntos_otorgados, puntos_redimidos,
        monto_propina, descuento_puntos
      INTO
        v_estado, v_cuenta_id, v_subcuenta_id, v_subtotal, v_total,
        v_puntos_otorgados, v_puntos_redimidos,
        v_monto_propina, v_descuento_puntos
      FROM facturas
     WHERE id = v_factura_id;

    IF NOT FOUND OR v_estado <> 'EMITIDA' THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    SELECT COALESCE(SUM(subtotal_linea), 0)
      INTO v_suma_detalles
      FROM factura_detalles
     WHERE factura_id = v_factura_id;

    IF ROUND(v_suma_detalles, 2) <> ROUND(v_subtotal, 2) THEN
        RAISE EXCEPTION
            'El subtotal de la factura % no coincide con sus detalles',
            v_factura_id;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM factura_detalles fd
          JOIN comanda_detalles cd ON cd.id = fd.comanda_detalle_id
          JOIN comandas co ON co.id = cd.comanda_id
         WHERE fd.factura_id = v_factura_id
           AND (
                co.cuenta_id <> v_cuenta_id
                OR fd.platillo_id IS DISTINCT FROM cd.platillo_id
                OR fd.combo_id IS DISTINCT FROM cd.combo_id
                OR fd.nombre_snapshot IS DISTINCT FROM cd.nombre_snapshot
                OR fd.precio_unitario_snapshot
                    IS DISTINCT FROM cd.precio_unitario_snapshot
                OR fd.total_modificadores_snapshot IS DISTINCT FROM ROUND(
                    COALESCE((
                        SELECT SUM(
                            cdm.cantidad * cdm.precio_adicional_snapshot
                        )
                          FROM comanda_detalle_modificadores cdm
                         WHERE cdm.comanda_detalle_id = cd.id
                    ), 0),
                    2
                )
                OR fd.costo_unitario_snapshot IS DISTINCT FROM ROUND(
                    GREATEST(cd.costo_unitario_snapshot + COALESCE((
                        SELECT SUM(
                            cdm.cantidad * cdm.costo_adicional_snapshot
                        )
                          FROM comanda_detalle_modificadores cdm
                         WHERE cdm.comanda_detalle_id = cd.id
                    ), 0), 0),
                    4
                )
                OR cd.estado <> 'ENTREGADO'
           )
    ) THEN
        RAISE EXCEPTION
            'Un detalle facturado no pertenece a la cuenta o producto original';
    END IF;

    IF v_subcuenta_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
              FROM factura_detalles fd
              LEFT JOIN subcuenta_detalles sd
                ON sd.subcuenta_id = v_subcuenta_id
               AND sd.comanda_detalle_id = fd.comanda_detalle_id
             WHERE fd.factura_id = v_factura_id
               AND sd.cantidad_asignada IS DISTINCT FROM fd.cantidad
        ) OR EXISTS (
            SELECT 1
              FROM subcuenta_detalles sd
              LEFT JOIN factura_detalles fd
                ON fd.factura_id = v_factura_id
               AND fd.comanda_detalle_id = sd.comanda_detalle_id
             WHERE sd.subcuenta_id = v_subcuenta_id
               AND fd.id IS NULL
        ) THEN
            RAISE EXCEPTION
                'La factura no coincide con los items asignados a la subcuenta';
        END IF;
    ELSE
        IF EXISTS (
            SELECT 1
              FROM comanda_detalles cd
              JOIN comandas co ON co.id = cd.comanda_id
              LEFT JOIN factura_detalles fd
                ON fd.factura_id = v_factura_id
               AND fd.comanda_detalle_id = cd.id
             WHERE co.cuenta_id = v_cuenta_id
               AND cd.estado = 'ENTREGADO'
               AND (
                    fd.id IS NULL
                    OR fd.cantidad <> cd.cantidad
               )
        ) THEN
            RAISE EXCEPTION
                'La factura completa no incluye todos los platillos entregados';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM factura_detalles fd
         WHERE fd.factura_id = v_factura_id
           AND fd.total_modificadores_snapshot <> ROUND(
                COALESCE((
                    SELECT SUM(
                        fdm.cantidad * fdm.precio_adicional_snapshot
                    )
                      FROM factura_detalle_modificadores fdm
                     WHERE fdm.factura_detalle_id = fd.id
                ), 0),
                2
           )
    ) THEN
        RAISE EXCEPTION
            'El total de modificadores no coincide con su detalle de factura';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM factura_detalles fd
          JOIN factura_detalle_modificadores fdm
            ON fdm.factura_detalle_id = fd.id
          LEFT JOIN comanda_detalle_modificadores cdm
            ON cdm.comanda_detalle_id = fd.comanda_detalle_id
           AND cdm.modificador_id = fdm.modificador_id
         WHERE fd.factura_id = v_factura_id
           AND (
                cdm.id IS NULL
                OR fdm.nombre_snapshot IS DISTINCT FROM cdm.nombre_snapshot
                OR fdm.cantidad IS DISTINCT FROM cdm.cantidad
                OR fdm.precio_adicional_snapshot
                    IS DISTINCT FROM cdm.precio_adicional_snapshot
           )
    ) OR EXISTS (
        SELECT 1
          FROM factura_detalles fd
          JOIN comanda_detalle_modificadores cdm
            ON cdm.comanda_detalle_id = fd.comanda_detalle_id
          LEFT JOIN factura_detalle_modificadores fdm
            ON fdm.factura_detalle_id = fd.id
           AND fdm.modificador_id = cdm.modificador_id
         WHERE fd.factura_id = v_factura_id
           AND fdm.id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Los modificadores facturados no coinciden con la comanda';
    END IF;

    SELECT COALESCE(SUM(monto), 0)
      INTO v_suma_pagos
      FROM pagos
     WHERE factura_id = v_factura_id;

    IF ROUND(v_suma_pagos, 2) <> ROUND(v_total, 2) THEN
        RAISE EXCEPTION
            'La factura % debe quedar totalmente pagada en la transaccion',
            v_factura_id;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM pagos p
          LEFT JOIN transacciones_caja tc
            ON tc.pago_id = p.id
           AND tc.factura_id = v_factura_id
           AND tc.tipo = 'VENTA'
         WHERE p.factura_id = v_factura_id
           AND tc.id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Cada pago de la factura % debe tener su transaccion de caja',
            v_factura_id;
    END IF;

    IF v_monto_propina > 0 AND NOT EXISTS (
        SELECT 1
          FROM transacciones_caja tc
         WHERE tc.factura_id = v_factura_id
           AND tc.tipo = 'PROPINA'
    ) THEN
        RAISE EXCEPTION
            'La propina de la factura % no fue registrada en caja',
            v_factura_id;
    END IF;

    IF v_puntos_redimidos > 0 AND NOT EXISTS (
        SELECT 1
          FROM transacciones_caja tc
         WHERE tc.factura_id = v_factura_id
           AND tc.tipo = 'REDENCION_PUNTOS'
    ) THEN
        RAISE EXCEPTION
            'La redencion de la factura % no fue registrada en caja',
            v_factura_id;
    END IF;

    SELECT
        COALESCE(SUM(CASE
            WHEN tipo = 'OTORGAMIENTO' THEN cantidad_puntos
            ELSE 0
        END), 0),
        ABS(COALESCE(SUM(CASE
            WHEN tipo = 'REDENCION' THEN cantidad_puntos
            ELSE 0
        END), 0))
      INTO
        v_puntos_otorgados_registrados,
        v_puntos_redimidos_registrados
      FROM movimientos_puntos
     WHERE factura_id = v_factura_id;

    IF v_puntos_otorgados_registrados <> v_puntos_otorgados
       OR v_puntos_redimidos_registrados <> v_puntos_redimidos THEN
        RAISE EXCEPTION
            'Los movimientos de puntos no coinciden con la factura %',
            v_factura_id;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_totales_factura_cabecera
AFTER INSERT OR UPDATE ON facturas
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_validar_totales_factura_diferido();

CREATE CONSTRAINT TRIGGER trg_totales_factura_detalles
AFTER INSERT OR UPDATE OR DELETE ON factura_detalles
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_validar_totales_factura_diferido();

CREATE CONSTRAINT TRIGGER trg_totales_factura_modificadores
AFTER INSERT OR UPDATE OR DELETE ON factura_detalle_modificadores
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_validar_totales_factura_diferido();

CREATE CONSTRAINT TRIGGER trg_totales_factura_pagos
AFTER INSERT OR UPDATE OR DELETE ON pagos
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION fn_validar_totales_factura_diferido();

CREATE TRIGGER trg_pago_inmutable
BEFORE UPDATE OR DELETE ON pagos
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE TRIGGER trg_factura_detalle_inmutable
BEFORE UPDATE OR DELETE ON factura_detalles
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE TRIGGER trg_factura_modificador_inmutable
BEFORE UPDATE OR DELETE ON factura_detalle_modificadores
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE OR REPLACE FUNCTION fn_aplicar_movimiento_puntos()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_saldo             BIGINT;
    v_restaurante       BIGINT;
    v_cliente_factura   BIGINT;
    v_estado_factura    VARCHAR(15);
    v_puntos_otorgados  BIGINT;
    v_puntos_redimidos  BIGINT;
    v_descuento_puntos  DECIMAL(12,2);
    v_total_factura     DECIMAL(12,2);
    v_total_pagado      DECIMAL(14,2);
BEGIN
    SELECT saldo_puntos, restaurante_id
      INTO v_saldo, v_restaurante
      FROM clientes
     WHERE id = NEW.cliente_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'El cliente del movimiento de puntos no existe';
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.registrado_por_id,
        v_restaurante,
        NULL
    );

    IF NEW.factura_id IS NOT NULL THEN
        SELECT
            cliente_id, estado, puntos_otorgados, puntos_redimidos,
            descuento_puntos, total
          INTO v_cliente_factura, v_estado_factura,
               v_puntos_otorgados, v_puntos_redimidos,
               v_descuento_puntos, v_total_factura
          FROM facturas
         WHERE id = NEW.factura_id
         FOR SHARE;

        IF NOT FOUND
           OR v_estado_factura <> 'EMITIDA'
           OR v_cliente_factura IS DISTINCT FROM NEW.cliente_id THEN
            RAISE EXCEPTION
                'La factura no corresponde al cliente del movimiento de puntos';
        END IF;

        IF NEW.tipo = 'OTORGAMIENTO'
           AND NEW.cantidad_puntos <> v_puntos_otorgados THEN
            RAISE EXCEPTION
                'Los puntos otorgados no coinciden con la factura';
        END IF;

        IF NEW.tipo = 'REDENCION'
           AND ABS(NEW.cantidad_puntos) <> v_puntos_redimidos THEN
            RAISE EXCEPTION
                'Los puntos redimidos no coinciden con la factura';
        END IF;

        IF NEW.tipo = 'REDENCION'
           AND NEW.valor_monetario <> v_descuento_puntos THEN
            RAISE EXCEPTION
                'El valor monetario redimido no coincide con la factura';
        END IF;

        SELECT COALESCE(SUM(monto), 0)
          INTO v_total_pagado
          FROM pagos
         WHERE factura_id = NEW.factura_id;

        IF ROUND(v_total_pagado, 2) <> ROUND(v_total_factura, 2) THEN
            RAISE EXCEPTION
                'Los puntos solo se procesan cuando la factura esta pagada';
        END IF;
    END IF;

    NEW.saldo_anterior := v_saldo;
    NEW.saldo_resultante := v_saldo + NEW.cantidad_puntos;

    IF NEW.saldo_resultante < 0 THEN
        RAISE EXCEPTION
            'El cliente no posee puntos suficientes para la redencion';
    END IF;

    UPDATE clientes
       SET saldo_puntos = NEW.saldo_resultante
     WHERE id = NEW.cliente_id;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_aplicar_movimiento_puntos
BEFORE INSERT ON movimientos_puntos
FOR EACH ROW EXECUTE FUNCTION fn_aplicar_movimiento_puntos();

CREATE TRIGGER trg_movimiento_puntos_inmutable
BEFORE UPDATE OR DELETE ON movimientos_puntos
FOR EACH ROW EXECUTE FUNCTION fn_bloquear_libro_mayor();

CREATE OR REPLACE FUNCTION fn_proteger_factura()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_ajuste_puntos BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'Las facturas no se eliminan; deben anularse con trazabilidad';
    END IF;

    IF OLD.estado <> 'EMITIDA' OR NEW.estado <> 'ANULADA' THEN
        RAISE EXCEPTION
            'Una factura solo puede cambiar de EMITIDA a ANULADA';
    END IF;

    IF (to_jsonb(NEW) - ARRAY[
            'estado', 'anulada_en', 'anulada_por_id', 'motivo_anulacion'
        ]) IS DISTINCT FROM
       (to_jsonb(OLD) - ARRAY[
            'estado', 'anulada_en', 'anulada_por_id', 'motivo_anulacion'
        ]) THEN
        RAISE EXCEPTION
            'No se pueden modificar los importes ni referencias de una factura';
    END IF;

    IF NEW.anulada_en IS NULL THEN
        NEW.anulada_en := CURRENT_TIMESTAMP;
    END IF;

    IF NEW.anulada_por_id IS NULL
       OR NULLIF(BTRIM(NEW.motivo_anulacion), '') IS NULL THEN
        RAISE EXCEPTION
            'La anulacion requiere usuario responsable y motivo';
    END IF;

    PERFORM fn_validar_usuario_operacion(
        NEW.anulada_por_id,
        OLD.restaurante_id,
        'ADMIN'
    );

    v_ajuste_puntos := OLD.puntos_redimidos - OLD.puntos_otorgados;
    IF OLD.cliente_id IS NOT NULL AND v_ajuste_puntos <> 0 THEN
        INSERT INTO movimientos_puntos (
            cliente_id, factura_id, tipo, cantidad_puntos,
            valor_monetario, motivo, registrado_por_id
        ) VALUES (
            OLD.cliente_id,
            OLD.id,
            'AJUSTE',
            v_ajuste_puntos,
            0,
            'Reversion de puntos por anulacion de factura',
            NEW.anulada_por_id
        );
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_proteger_factura
BEFORE UPDATE OR DELETE ON facturas
FOR EACH ROW EXECUTE FUNCTION fn_proteger_factura();

CREATE OR REPLACE FUNCTION fn_registrar_visita_cliente()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
BEGIN
    IF OLD.estado <> 'CERRADA'
       AND NEW.estado = 'CERRADA'
       AND NEW.cliente_id IS NOT NULL THEN
        UPDATE clientes
           SET total_visitas = total_visitas + 1,
               ultima_visita_en = NEW.cerrada_en
         WHERE id = NEW.cliente_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_registrar_visita_cliente
AFTER UPDATE OF estado ON cuentas
FOR EACH ROW EXECUTE FUNCTION fn_registrar_visita_cliente();

CREATE OR REPLACE FUNCTION fn_estado_mesa_para_cuenta(p_estado_cuenta VARCHAR)
RETURNS VARCHAR
LANGUAGE sql
IMMUTABLE
SET search_path = restaurante, public
AS $$
    SELECT CASE p_estado_cuenta
        WHEN 'ABIERTA'              THEN 'OCUPADA'
        WHEN 'LISTA_COBRO'          THEN 'CUENTA_SOLICITADA'
        WHEN 'PARCIALMENTE_PAGADA'  THEN 'CUENTA_SOLICITADA'
        WHEN 'CERRADA'              THEN 'LIBRE'
        WHEN 'CANCELADA'            THEN 'LIBRE'
        WHEN 'FUSIONADA'            THEN NULL
        ELSE NULL
    END;
$$;

CREATE OR REPLACE FUNCTION fn_sincronizar_estado_mesa()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_estado_objetivo    VARCHAR(20);
    v_estado_mesa_actual VARCHAR(20);
    v_cuenta_destino     BIGINT;
    v_estado_destino     VARCHAR(25);
    v_usuario_fusion     BIGINT;
    v_mesa               RECORD;
BEGIN

    IF TG_OP = 'INSERT'
       OR (TG_OP = 'UPDATE' AND NEW.mesa_id IS DISTINCT FROM OLD.mesa_id) THEN
        IF TG_OP = 'UPDATE' THEN
            UPDATE cuenta_mesas
               SET activa = FALSE,
                   es_principal = FALSE,
                   desvinculada_en = CURRENT_TIMESTAMP
             WHERE cuenta_id = NEW.id
               AND mesa_id = OLD.mesa_id
               AND activa = TRUE
               AND es_principal = TRUE;

            SELECT estado_actual
              INTO v_estado_mesa_actual
              FROM mesas
             WHERE id = OLD.mesa_id
             FOR UPDATE;

            IF v_estado_mesa_actual IS DISTINCT FROM 'LIBRE' THEN
                UPDATE mesas
                   SET estado_actual = 'LIBRE'
                 WHERE id = OLD.mesa_id;

                INSERT INTO historial_estados_mesa (
                    mesa_id, cuenta_id, estado_anterior, estado_nuevo,
                    usuario_id, motivo
                ) VALUES (
                    OLD.mesa_id, NEW.id, v_estado_mesa_actual, 'LIBRE',
                    NEW.mesero_id, 'Liberada por transferencia de cuenta'
                );
            END IF;
        END IF;

        UPDATE cuenta_mesas
           SET es_principal = FALSE
         WHERE cuenta_id = NEW.id
           AND activa = TRUE;

        INSERT INTO cuenta_mesas (
            cuenta_id, mesa_id, es_principal, activa,
            vinculada_en, desvinculada_en
        ) VALUES (
            NEW.id, NEW.mesa_id, TRUE, TRUE,
            CURRENT_TIMESTAMP, NULL
        )
        ON CONFLICT (cuenta_id, mesa_id) DO UPDATE
           SET es_principal = TRUE,
               activa = TRUE,
               vinculada_en = CURRENT_TIMESTAMP,
               desvinculada_en = NULL;

        IF TG_OP = 'INSERT' AND NEW.reserva_id IS NOT NULL THEN
            UPDATE reservas
               SET estado = 'CLIENTE_PRESENTE'
             WHERE id = NEW.reserva_id
               AND estado = 'CONFIRMADA';

            UPDATE reservas
               SET estado = 'ATENDIDA'
             WHERE id = NEW.reserva_id
               AND estado = 'CLIENTE_PRESENTE';
        END IF;

        IF TG_OP = 'INSERT' AND NEW.lista_espera_id IS NOT NULL THEN
            UPDATE lista_espera
               SET estado = 'SENTADA'
             WHERE id = NEW.lista_espera_id
               AND estado IN ('SUGERIDA', 'NOTIFICADA');
        END IF;
    END IF;


    IF TG_OP = 'UPDATE'
       AND OLD.estado <> 'FUSIONADA'
       AND NEW.estado = 'FUSIONADA' THEN
        SELECT fc.cuenta_destino_id, fc.realizada_por_id, c.estado
          INTO v_cuenta_destino, v_usuario_fusion, v_estado_destino
          FROM fusiones_cuenta fc
          JOIN cuentas c ON c.id = fc.cuenta_destino_id
         WHERE fc.cuenta_origen_id = NEW.id
         FOR SHARE OF c;

        IF NOT FOUND THEN
            RAISE EXCEPTION
                'La cuenta no puede marcarse FUSIONADA sin una fusion registrada';
        END IF;

        v_estado_objetivo := fn_estado_mesa_para_cuenta(v_estado_destino);

        FOR v_mesa IN
            SELECT cm.id AS vinculo_id, cm.mesa_id
              FROM cuenta_mesas cm
             WHERE cm.cuenta_id = NEW.id
               AND cm.activa = TRUE
             ORDER BY cm.mesa_id
             FOR UPDATE
        LOOP
            UPDATE cuenta_mesas
               SET activa = FALSE,
                   es_principal = FALSE,
                   desvinculada_en = CURRENT_TIMESTAMP
             WHERE id = v_mesa.vinculo_id;

            INSERT INTO cuenta_mesas (
                cuenta_id, mesa_id, es_principal, origen_cuenta_id,
                activa, vinculada_en, desvinculada_en
            ) VALUES (
                v_cuenta_destino, v_mesa.mesa_id, FALSE, NEW.id,
                TRUE, CURRENT_TIMESTAMP, NULL
            )
            ON CONFLICT (cuenta_id, mesa_id) DO UPDATE
               SET activa = TRUE,
                   es_principal = FALSE,
                   origen_cuenta_id = NEW.id,
                   vinculada_en = CURRENT_TIMESTAMP,
                   desvinculada_en = NULL;

            SELECT estado_actual
              INTO v_estado_mesa_actual
              FROM mesas
             WHERE id = v_mesa.mesa_id
             FOR UPDATE;

            IF v_estado_mesa_actual IS DISTINCT FROM v_estado_objetivo THEN
                UPDATE mesas
                   SET estado_actual = v_estado_objetivo
                 WHERE id = v_mesa.mesa_id;

                INSERT INTO historial_estados_mesa (
                    mesa_id, cuenta_id, estado_anterior, estado_nuevo,
                    usuario_id, motivo
                ) VALUES (
                    v_mesa.mesa_id, v_cuenta_destino,
                    v_estado_mesa_actual, v_estado_objetivo,
                    v_usuario_fusion,
                    'Mesa conservada por fusion de cuentas'
                );
            END IF;
        END LOOP;

        RETURN NEW;
    END IF;

    v_estado_objetivo := fn_estado_mesa_para_cuenta(NEW.estado);

    IF v_estado_objetivo IS NULL THEN
        RETURN NEW;
    END IF;

    FOR v_mesa IN
        SELECT cm.id AS vinculo_id, cm.mesa_id
          FROM cuenta_mesas cm
         WHERE cm.cuenta_id = NEW.id
           AND cm.activa = TRUE
         ORDER BY cm.mesa_id
         FOR UPDATE
    LOOP
        SELECT estado_actual
          INTO v_estado_mesa_actual
          FROM mesas
         WHERE id = v_mesa.mesa_id
         FOR UPDATE;

        IF v_estado_mesa_actual IS DISTINCT FROM v_estado_objetivo THEN
            UPDATE mesas
               SET estado_actual = v_estado_objetivo
             WHERE id = v_mesa.mesa_id;

            INSERT INTO historial_estados_mesa (
                mesa_id, cuenta_id, estado_anterior, estado_nuevo,
                usuario_id, motivo
            ) VALUES (
                v_mesa.mesa_id, NEW.id,
                v_estado_mesa_actual, v_estado_objetivo,
                NEW.mesero_id,
                'Sincronizado automaticamente desde cuentas.estado'
            );
        END IF;

        IF NEW.estado IN ('CERRADA', 'CANCELADA') THEN
            UPDATE cuenta_mesas
               SET activa = FALSE,
                   es_principal = FALSE,
                   desvinculada_en = CURRENT_TIMESTAMP
             WHERE id = v_mesa.vinculo_id;
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_sincronizar_estado_mesa
AFTER INSERT OR UPDATE OF estado, mesa_id ON cuentas
FOR EACH ROW EXECUTE FUNCTION fn_sincronizar_estado_mesa();

CREATE OR REPLACE FUNCTION fn_validar_calificacion_servicio()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_cuenta_id  BIGINT;
    v_cliente_id BIGINT;
    v_mesero_id  BIGINT;
    v_estado     VARCHAR(15);
BEGIN
    SELECT cuenta_id, cliente_id, mesero_id, estado
      INTO v_cuenta_id, v_cliente_id, v_mesero_id, v_estado
      FROM facturas
     WHERE id = NEW.factura_id;

    IF NOT FOUND
       OR v_estado <> 'EMITIDA'
       OR v_cuenta_id <> NEW.cuenta_id
       OR v_cliente_id IS DISTINCT FROM NEW.cliente_id
       OR v_mesero_id <> NEW.mesero_id THEN
        RAISE EXCEPTION
            'La calificacion no coincide con la factura y el servicio recibido';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_calificacion_servicio
BEFORE INSERT OR UPDATE ON calificaciones_servicio
FOR EACH ROW EXECUTE FUNCTION fn_validar_calificacion_servicio();

CREATE OR REPLACE FUNCTION fn_siguiente_numero_factura(
    p_restaurante_id BIGINT,
    p_tipo_documento VARCHAR(20),
    p_serie VARCHAR(20)
)
RETURNS BIGINT
LANGUAGE plpgsql
SET search_path = restaurante, public
AS $$
DECLARE
    v_siguiente_numero BIGINT;
BEGIN
    UPDATE secuencias_facturacion
       SET ultimo_numero = ultimo_numero + 1
     WHERE restaurante_id = p_restaurante_id
       AND tipo_documento = p_tipo_documento
       AND serie = p_serie
       AND activo = TRUE
    RETURNING ultimo_numero INTO v_siguiente_numero;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No existe una secuencia de facturacion activa';
    END IF;

    RETURN v_siguiente_numero;
END;
$$;



INSERT INTO restaurantes (
    id, nombre, nombre_comercial, moneda, zona_horaria
) VALUES (
    1, 'Restaurante Principal', 'Restaurante Principal', 'GTQ', 'America/Guatemala'
);

INSERT INTO usuarios (
    id, restaurante_id, codigo_empleado,
    nombres, apellidos, fecha_contratacion
)
SELECT
    au.id,
    1,
    CASE
        WHEN r.name = 'ADMIN' THEN 'ADMIN-' || LPAD(au.id::TEXT, 4, '0')
        ELSE 'EMP-' || LPAD(au.id::TEXT, 4, '0')
    END,
    CASE WHEN r.name = 'ADMIN' THEN 'Administrador' ELSE 'Empleado' END,
    'Inicial',
    CURRENT_DATE
FROM public.app_users au
JOIN public.roles r ON r.id = au.role_id
ON CONFLICT (id) DO NOTHING;


INSERT INTO unidades_medida (
    id, codigo, nombre, abreviatura, dimension, factor_a_base
) VALUES
    (1, 'GRAMO',     'Gramo',      'g',  'MASA',     1.000000),
    (2, 'KILOGRAMO', 'Kilogramo',  'kg', 'MASA',  1000.000000),
    (3, 'MILILITRO', 'Mililitro',  'ml', 'VOLUMEN',  1.000000),
    (4, 'LITRO',      'Litro',      'l',  'VOLUMEN',1000.000000),
    (5, 'UNIDAD',     'Unidad',     'u',  'CONTEO',   1.000000);

INSERT INTO categorias_insumo (
    restaurante_id, nombre, descripcion
) VALUES
    (1, 'Proteina', 'Carnes, aves, pescados y otras proteinas'),
    (1, 'Verdura',  'Vegetales, frutas y hierbas'),
    (1, 'Lacteo',   'Leche, quesos y derivados'),
    (1, 'Abarrote', 'Productos secos y de despensa'),
    (1, 'Bebida',   'Insumos para bebidas'),
    (1, 'Otro',     'Insumos no clasificados');

INSERT INTO categorias_platillo (
    restaurante_id, codigo, nombre, orden_visual
) VALUES
    (1, 'ENTRADA',      'Entrada',      1),
    (1, 'PLATO_FUERTE', 'Plato fuerte', 2),
    (1, 'BEBIDA',       'Bebida',       3),
    (1, 'POSTRE',       'Postre',       4);

INSERT INTO configuraciones_restaurante (
    restaurante_id, porcentaje_impuesto, porcentaje_propina,
    propina_editable, puntos_por_moneda, valor_monetario_punto,
    duracion_reserva_minutos, tolerancia_reserva_minutos,
    estado
) VALUES (
    1, 12.0000, 10.0000, TRUE, 1.0000, 0.0100, 120, 15, 'VIGENTE'
);

INSERT INTO zonas_mesa (restaurante_id, nombre, descripcion) VALUES
    (1, 'Salon',   'Area principal del restaurante'),
    (1, 'Terraza', 'Area exterior o terraza'),
    (1, 'Barra',   'Asientos ubicados en barra');

INSERT INTO cajas (restaurante_id, codigo, nombre, ubicacion) VALUES
    (1, 'CAJA-01', 'Caja principal', 'Area de cobro');

INSERT INTO metodos_pago (
    id, codigo, nombre, afecta_efectivo, requiere_referencia
) VALUES
    (1, 'EFECTIVO', 'Efectivo', TRUE,  FALSE),
    (2, 'TARJETA',  'Tarjeta',  FALSE, TRUE);

INSERT INTO secuencias_facturacion (
    restaurante_id, tipo_documento, serie, ultimo_numero
) VALUES
    (1, 'COMPROBANTE', 'A', 0);

ALTER TABLE restaurantes ALTER COLUMN id RESTART WITH 2;
ALTER TABLE unidades_medida ALTER COLUMN id RESTART WITH 6;
ALTER TABLE metodos_pago ALTER COLUMN id RESTART WITH 3;

CREATE OR REPLACE VIEW vw_stock_bajo AS
SELECT
    i.id AS insumo_id,
    i.restaurante_id,
    i.codigo,
    i.nombre AS insumo,
    ci.nombre AS categoria,
    i.stock_actual,
    i.stock_minimo,
    i.stock_maximo,
    um.codigo AS unidad_codigo,
    um.abreviatura AS unidad,
    i.costo_unitario_actual,
    ROUND(i.stock_actual * i.costo_unitario_actual, 4) AS valor_existencia,
    CASE
        WHEN i.stock_actual = 0 THEN 'AGOTADO'
        WHEN i.stock_actual <= i.stock_minimo THEN 'BAJO'
        ELSE 'NORMAL'
    END AS nivel_stock
FROM insumos i
JOIN categorias_insumo ci ON ci.id = i.categoria_insumo_id
JOIN unidades_medida um ON um.id = i.unidad_stock_id
WHERE i.activo = TRUE
  AND i.stock_actual <= i.stock_minimo;

CREATE OR REPLACE VIEW vw_costos_platillo AS
SELECT
    p.id AS platillo_id,
    p.restaurante_id,
    p.codigo,
    p.nombre AS platillo,
    cp.nombre AS categoria,
    p.precio_venta,
    rv.id AS receta_version_id,
    rv.numero_version,
    CAST(COALESCE(SUM(
        (rd.cantidad * ur.factor_a_base / us.factor_a_base)
        * i.costo_unitario_actual
    ), 0) AS DECIMAL(14,4)) AS costo_receta_actual,
    CAST(p.precio_venta - COALESCE(SUM(
        (rd.cantidad * ur.factor_a_base / us.factor_a_base)
        * i.costo_unitario_actual
    ), 0) AS DECIMAL(14,4)) AS margen_bruto,
    CAST(CASE
        WHEN p.precio_venta = 0 THEN NULL
        ELSE ((p.precio_venta - COALESCE(SUM(
            (rd.cantidad * ur.factor_a_base / us.factor_a_base)
            * i.costo_unitario_actual
        ), 0)) / p.precio_venta) * 100
    END AS DECIMAL(9,4)) AS porcentaje_margen
FROM platillos p
JOIN categorias_platillo cp ON cp.id = p.categoria_platillo_id
LEFT JOIN receta_versiones rv
       ON rv.platillo_id = p.id
      AND rv.estado = 'VIGENTE'
LEFT JOIN receta_detalles rd ON rd.receta_version_id = rv.id
LEFT JOIN insumos i ON i.id = rd.insumo_id
LEFT JOIN unidades_medida ur ON ur.id = rd.unidad_medida_id
LEFT JOIN unidades_medida us ON us.id = i.unidad_stock_id
GROUP BY
    p.id, p.restaurante_id, p.codigo, p.nombre,
    cp.nombre, p.precio_venta, rv.id, rv.numero_version;

CREATE OR REPLACE VIEW vw_disponibilidad_platillos AS
SELECT
    p.id AS platillo_id,
    p.restaurante_id,
    p.codigo,
    p.nombre AS platillo,
    p.activo,
    p.disponible_manual,
    rv.id AS receta_version_id,
    CASE
        WHEN p.activo = FALSE OR p.disponible_manual = FALSE THEN 0
        WHEN rv.id IS NULL OR COUNT(rd.id) = 0 THEN 0
        WHEN MIN(CASE
            WHEN i.activo = FALSE THEN 0
            ELSE FLOOR(
                i.stock_actual /
                NULLIF(
                    rd.cantidad * ur.factor_a_base / us.factor_a_base,
                    0
                )
            )
        END) < 1 THEN 0
        ELSE 1
    END AS disponible,
    CASE
        WHEN rv.id IS NULL OR COUNT(rd.id) = 0 THEN 0
        ELSE GREATEST(0, COALESCE(MIN(CASE
            WHEN i.activo = FALSE THEN 0
            ELSE FLOOR(
                i.stock_actual /
                NULLIF(
                    rd.cantidad * ur.factor_a_base / us.factor_a_base,
                    0
                )
            )
        END), 0))
    END AS porciones_disponibles
FROM platillos p
LEFT JOIN receta_versiones rv
       ON rv.platillo_id = p.id
      AND rv.estado = 'VIGENTE'
LEFT JOIN receta_detalles rd ON rd.receta_version_id = rv.id
LEFT JOIN insumos i ON i.id = rd.insumo_id
LEFT JOIN unidades_medida ur ON ur.id = rd.unidad_medida_id
LEFT JOIN unidades_medida us ON us.id = i.unidad_stock_id
GROUP BY
    p.id, p.restaurante_id, p.codigo, p.nombre,
    p.activo, p.disponible_manual, rv.id;

CREATE OR REPLACE VIEW vw_disponibilidad_combos AS
WITH componentes AS (
    SELECT
        cd.combo_id,
        COUNT(*) AS cantidad_componentes,
        BOOL_AND(
            p.activo = TRUE
            AND p.disponible_manual = TRUE
            AND rv.id IS NOT NULL
            AND EXISTS (
                SELECT 1
                  FROM receta_detalles rd_existente
                 WHERE rd_existente.receta_version_id = rv.id
            )
        ) AS recetas_completas
    FROM combo_detalles cd
    JOIN platillos p ON p.id = cd.platillo_id
    LEFT JOIN receta_versiones rv
           ON rv.platillo_id = cd.platillo_id
          AND rv.estado = 'VIGENTE'
    GROUP BY cd.combo_id
),
requerimientos AS (
    SELECT
        cd.combo_id,
        rd.insumo_id,
        SUM(
            cd.cantidad
            * rd.cantidad
            * ur.factor_a_base
            / us.factor_a_base
        ) AS cantidad_requerida_stock
    FROM combo_detalles cd
    JOIN receta_versiones rv
      ON rv.platillo_id = cd.platillo_id
     AND rv.estado = 'VIGENTE'
    JOIN receta_detalles rd ON rd.receta_version_id = rv.id
    JOIN insumos i ON i.id = rd.insumo_id
    JOIN unidades_medida ur ON ur.id = rd.unidad_medida_id
    JOIN unidades_medida us ON us.id = i.unidad_stock_id
    GROUP BY cd.combo_id, rd.insumo_id
),
capacidad AS (
    SELECT
        r.combo_id,
        MIN(CASE
            WHEN i.activo = FALSE THEN 0
            ELSE FLOOR(
                i.stock_actual / NULLIF(r.cantidad_requerida_stock, 0)
            )
        END) AS combos_posibles
    FROM requerimientos r
    JOIN insumos i ON i.id = r.insumo_id
    GROUP BY r.combo_id
)
SELECT
    c.id AS combo_id,
    c.restaurante_id,
    c.codigo,
    c.nombre AS combo,
    c.activo,
    c.disponible_manual,
    CASE
        WHEN c.activo = FALSE OR c.disponible_manual = FALSE THEN 0
        WHEN c.fecha_inicio IS NOT NULL AND CURRENT_TIMESTAMP < c.fecha_inicio THEN 0
        WHEN c.fecha_fin IS NOT NULL AND CURRENT_TIMESTAMP > c.fecha_fin THEN 0
        WHEN COALESCE(comp.cantidad_componentes, 0) = 0 THEN 0
        WHEN COALESCE(comp.recetas_completas, FALSE) = FALSE THEN 0
        WHEN COALESCE(cap.combos_posibles, 0) < 1 THEN 0
        ELSE 1
    END AS disponible,
    GREATEST(0, COALESCE(cap.combos_posibles, 0)) AS combos_disponibles
FROM combos c
LEFT JOIN componentes comp ON comp.combo_id = c.id
LEFT JOIN capacidad cap ON cap.combo_id = c.id;

CREATE OR REPLACE VIEW vw_ocupacion_mesas_actual AS
SELECT
    m.id AS mesa_id,
    m.restaurante_id,
    m.numero,
    m.capacidad,
    z.nombre AS zona,
    CASE
        WHEN cu.id IS NOT NULL THEN m.estado_actual
        WHEN le.id IS NOT NULL OR r.id IS NOT NULL THEN 'RESERVADA'
        ELSE m.estado_actual
    END AS estado_visual,
    cu.id AS cuenta_actual_id,
    cu.numero_cuenta,
    cm.es_principal,
    r.id AS reserva_actual_id,
    r.nombre_cliente AS cliente_reserva,
    r.fecha_hora_fin AS reserva_hasta,
    le.id AS lista_espera_actual_id,
    le.nombre_cliente AS cliente_lista_espera
FROM mesas m
JOIN zonas_mesa z ON z.id = m.zona_mesa_id
LEFT JOIN cuenta_mesas cm
       ON cm.mesa_id = m.id
      AND cm.activa = TRUE
LEFT JOIN cuentas cu
       ON cu.id = cm.cuenta_id
      AND cu.estado IN ('ABIERTA', 'LISTA_COBRO', 'PARCIALMENTE_PAGADA')
LEFT JOIN LATERAL (
    SELECT r0.id, r0.nombre_cliente, r0.fecha_hora_fin
      FROM reservas r0
     WHERE r0.mesa_id = m.id
       AND r0.estado IN ('PENDIENTE', 'CONFIRMADA', 'CLIENTE_PRESENTE')
       AND CURRENT_TIMESTAMP >= r0.fecha_hora_inicio
       AND CURRENT_TIMESTAMP < r0.fecha_hora_fin
     ORDER BY r0.fecha_hora_inicio, r0.id
     LIMIT 1
) r ON TRUE
LEFT JOIN LATERAL (
    SELECT le0.id, le0.nombre_cliente
      FROM lista_espera le0
     WHERE le0.mesa_sugerida_id = m.id
       AND le0.estado IN ('SUGERIDA', 'NOTIFICADA')
     ORDER BY le0.sugerida_en, le0.id
     LIMIT 1
) le ON TRUE
WHERE m.activo = TRUE
ORDER BY z.nombre, m.numero;

CREATE OR REPLACE VIEW vw_comandas_cocina AS
SELECT
    cd.id AS comanda_detalle_id,
    co.id AS comanda_id,
    co.numero_ronda,
    cu.id AS cuenta_id,
    cu.numero_cuenta,
    m.numero AS mesa,
    cd.nombre_snapshot AS platillo_o_combo,
    cd.cantidad,
    cd.notas_especiales,
    cd.estado,
    cd.tiempo_estimado_minutos,
    cd.recibido_en,
    cd.recibido_en
        + make_interval(mins => cd.tiempo_estimado_minutos)
        AS fecha_hora_limite,
    CASE
        WHEN cd.estado IN ('RECIBIDO', 'EN_PREPARACION')
         AND cd.recibido_en IS NOT NULL
         AND CURRENT_TIMESTAMP > (
             cd.recibido_en
             + make_interval(mins => cd.tiempo_estimado_minutos)
         )
        THEN 1 ELSE 0
    END AS tiempo_excedido,
    CASE
        WHEN cd.recibido_en IS NULL THEN 0
        ELSE GREATEST(
            0,
            FLOOR(EXTRACT(EPOCH FROM (
                CURRENT_TIMESTAMP
                - (
                    cd.recibido_en
                    + make_interval(mins => cd.tiempo_estimado_minutos)
                )
            )) / 60)
        )
    END AS minutos_atraso,
    cu.mesero_id
FROM comanda_detalles cd
JOIN comandas co ON co.id = cd.comanda_id
JOIN cuentas cu ON cu.id = co.cuenta_id
JOIN mesas m ON m.id = cu.mesa_id
WHERE cd.estado IN ('RECIBIDO', 'EN_PREPARACION', 'LISTO')
ORDER BY cd.recibido_en, cd.id;

CREATE OR REPLACE VIEW vw_ventas_detalladas AS
SELECT
    f.id AS factura_id,
    f.restaurante_id,
    f.numero_documento,
    f.emitida_en,
    f.cuenta_id,
    cu.mesa_id,
    m.numero AS mesa,
    f.mesero_id,
    CONCAT(um.nombres, ' ', um.apellidos) AS mesero,
    f.cajero_id,
    CONCAT(uc.nombres, ' ', uc.apellidos) AS cajero,
    fd.platillo_id,
    fd.combo_id,
    fd.nombre_snapshot AS producto,
    fd.categoria_snapshot AS categoria,
    fd.cantidad,
    fd.precio_unitario_snapshot,
    fd.subtotal_linea,
    ROUND(fd.costo_unitario_snapshot * fd.cantidad, 4) AS costo_total,
    ROUND(
        fd.subtotal_linea - (fd.costo_unitario_snapshot * fd.cantidad),
        4
    ) AS margen_estimado
FROM facturas f
JOIN factura_detalles fd ON fd.factura_id = f.id
JOIN cuentas cu ON cu.id = f.cuenta_id
JOIN mesas m ON m.id = cu.mesa_id
JOIN usuarios um ON um.id = f.mesero_id
JOIN usuarios uc ON uc.id = f.cajero_id
WHERE f.estado = 'EMITIDA';

CREATE OR REPLACE VIEW vw_reporte_mermas AS
SELECT
    'DOCUMENTO_MERMA'::VARCHAR(30) AS fuente,
    me.id AS merma_id,
    NULL::BIGINT AS cancelacion_comanda_detalle_id,
    me.restaurante_id,
    me.numero_documento,
    me.tipo_motivo,
    me.motivo_general,
    me.registrada_en,
    md.insumo_id,
    i.nombre AS insumo,
    ci.nombre AS categoria,
    md.cantidad,
    um.abreviatura AS unidad,
    md.costo_unitario_snapshot,
    md.costo_total,
    me.registrada_por_id
FROM mermas me
JOIN merma_detalles md ON md.merma_id = me.id
JOIN insumos i ON i.id = md.insumo_id
JOIN categorias_insumo ci ON ci.id = i.categoria_insumo_id
JOIN unidades_medida um ON um.id = i.unidad_stock_id

UNION ALL

SELECT
    'CANCELACION_COCINA'::VARCHAR(30) AS fuente,
    NULL::BIGINT AS merma_id,
    ccd.id AS cancelacion_comanda_detalle_id,
    cu.restaurante_id,
    ('CANCELACION-' || ccd.id)::VARCHAR(40) AS numero_documento,
    'CANCELACION_COCINA'::VARCHAR(20) AS tipo_motivo,
    ccd.motivo AS motivo_general,
    ccd.resuelta_en AS registrada_en,
    mi.insumo_id,
    i.nombre AS insumo,
    ci.nombre AS categoria,
    mi.cantidad,
    um.abreviatura AS unidad,
    mi.costo_unitario_snapshot,
    ROUND(mi.cantidad * mi.costo_unitario_snapshot, 4) AS costo_total,
    ccd.autorizada_por_id AS registrada_por_id
FROM cancelaciones_comanda_detalle ccd
JOIN comanda_detalles cd ON cd.id = ccd.comanda_detalle_id
JOIN comandas co ON co.id = cd.comanda_id
JOIN cuentas cu ON cu.id = co.cuenta_id
JOIN movimientos_inventario mi
  ON mi.comanda_detalle_id = cd.id
 AND mi.tipo = 'SALIDA_VENTA'
JOIN insumos i ON i.id = mi.insumo_id
JOIN categorias_insumo ci ON ci.id = i.categoria_insumo_id
JOIN unidades_medida um ON um.id = i.unidad_stock_id
WHERE ccd.estado_solicitud = 'APROBADA'
  AND ccd.accion_inventario = 'REGISTRAR_MERMA';

CREATE OR REPLACE VIEW vw_desempeno_meseros AS
SELECT
    u.id AS mesero_id,
    u.restaurante_id,
    CONCAT(u.nombres, ' ', u.apellidos) AS mesero,
    COALESCE(v.cantidad_facturas, 0) AS cantidad_facturas,
    COALESCE(v.total_vendido, 0) AS total_vendido,
    COALESCE(c.cantidad_calificaciones, 0) AS cantidad_calificaciones,
    c.calificacion_promedio
FROM usuarios u
JOIN public.app_users au ON au.id = u.id
JOIN public.roles r ON r.id = au.role_id AND r.name = 'WAITER'
LEFT JOIN (
    SELECT
        mesero_id,
        COUNT(*) AS cantidad_facturas,
        SUM(total) AS total_vendido
    FROM facturas
    WHERE estado = 'EMITIDA'
    GROUP BY mesero_id
) v ON v.mesero_id = u.id
LEFT JOIN (
    SELECT
        mesero_id,
        COUNT(*) AS cantidad_calificaciones,
        ROUND(AVG(calificacion), 2) AS calificacion_promedio
    FROM calificaciones_servicio
    GROUP BY mesero_id
) c ON c.mesero_id = u.id
WHERE au.enabled = TRUE;

CREATE OR REPLACE VIEW vw_reporte_fidelizacion AS
SELECT
    c.id AS cliente_id,
    c.restaurante_id,
    CONCAT(c.nombres, ' ', COALESCE(c.apellidos, '')) AS cliente,
    c.telefono,
    c.saldo_puntos,
    c.total_visitas,
    c.ultima_visita_en,
    COALESCE(SUM(CASE
        WHEN mp.tipo = 'OTORGAMIENTO'
         AND (mp.factura_id IS NULL OR f.estado = 'EMITIDA')
        THEN mp.cantidad_puntos
        ELSE 0
    END), 0) AS puntos_otorgados,
    ABS(COALESCE(SUM(CASE
        WHEN mp.tipo = 'REDENCION'
         AND (mp.factura_id IS NULL OR f.estado = 'EMITIDA')
        THEN mp.cantidad_puntos
        ELSE 0
    END), 0)) AS puntos_redimidos,
    COALESCE(SUM(CASE
        WHEN mp.tipo = 'AJUSTE' THEN mp.cantidad_puntos
        ELSE 0
    END), 0) AS puntos_ajustados,
    COALESCE(SUM(mp.cantidad_puntos), 0) AS movimiento_neto_historico
FROM clientes c
LEFT JOIN movimientos_puntos mp ON mp.cliente_id = c.id
LEFT JOIN facturas f ON f.id = mp.factura_id
GROUP BY
    c.id, c.restaurante_id, c.nombres, c.apellidos,
    c.telefono, c.saldo_puntos, c.total_visitas, c.ultima_visita_en;

CREATE OR REPLACE VIEW vw_cuadre_turnos_caja AS
SELECT
    tc.id AS turno_caja_id,
    ca.restaurante_id,
    ca.codigo AS caja_codigo,
    tc.cajero_id,
    CONCAT(u.nombres, ' ', u.apellidos) AS cajero,
    tc.estado,
    tc.abierta_en,
    tc.cerrada_en,
    tc.monto_inicial_efectivo,
    COALESCE(SUM(CASE
        WHEN mp.afecta_efectivo = TRUE AND f.estado = 'EMITIDA' THEN p.monto
        ELSE 0
    END), 0) AS ventas_efectivo,
    COALESCE(SUM(CASE
        WHEN mp.afecta_efectivo = FALSE AND f.estado = 'EMITIDA' THEN p.monto
        ELSE 0
    END), 0) AS ventas_no_efectivo,
    ROUND(
        tc.monto_inicial_efectivo + COALESCE(SUM(CASE
            WHEN mp.afecta_efectivo = TRUE AND f.estado = 'EMITIDA' THEN p.monto
            ELSE 0
        END), 0),
        2
    ) AS efectivo_esperado_calculado,
    tc.efectivo_esperado_cierre,
    tc.efectivo_real_cierre,
    tc.diferencia_cierre
FROM turnos_caja tc
JOIN cajas ca ON ca.id = tc.caja_id
JOIN usuarios u ON u.id = tc.cajero_id
LEFT JOIN facturas f ON f.turno_caja_id = tc.id
LEFT JOIN pagos p ON p.factura_id = f.id
LEFT JOIN metodos_pago mp ON mp.id = p.metodo_pago_id
GROUP BY
    tc.id, ca.restaurante_id, ca.codigo, tc.cajero_id,
    u.nombres, u.apellidos, tc.estado, tc.abierta_en, tc.cerrada_en,
    tc.monto_inicial_efectivo, tc.efectivo_esperado_cierre,
    tc.efectivo_real_cierre, tc.diferencia_cierre;
