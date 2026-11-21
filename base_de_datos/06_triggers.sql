-- =============================================================================
-- PermiCarga Marietha
-- Script 06 de 06: Triggers
--
-- Los triggers cumplen dos funciones distintas y conviene no confundirlas:
--
--   a) Los triggers BEFORE hacen cumplir reglas de negocio que ninguna
--      restriccion declarativa puede expresar, porque dependen del estado de
--      otra tabla. Una CHECK no puede consultar si el vehiculo que se esta
--      declarando sigue activo; un trigger si.
--
--   b) Los triggers AFTER escriben la bitacora de auditoria. Se colocan en la
--      base de datos y no en el aplicativo a proposito: asi quedan registrados
--      tambien los cambios hechos desde MySQL Workbench o desde cualquier otro
--      cliente futuro. Si la auditoria dependiera del cliente Java, bastaria
--      con no usarlo para que los cambios pasaran inadvertidos.
--
-- Este script se ejecuta despues de la carga de datos de prueba, para que la
-- bitacora refleje unicamente la operacion real del aplicativo y no las mas de
-- trescientas filas de la carga inicial.
-- =============================================================================

USE marietha;

DELIMITER $$

-- =============================================================================
-- A. EMPRESA_TRANSPORTISTA
-- =============================================================================

DROP TRIGGER IF EXISTS TR_empresa_antes_insertar$$
CREATE TRIGGER TR_empresa_antes_insertar
BEFORE INSERT ON EMPRESA_TRANSPORTISTA
FOR EACH ROW
BEGIN
    -- Normaliza el registro MTC a mayusculas, para que la busqueda no dependa
    -- de como lo haya escrito el operador.
    SET NEW.Registro_MTC = UPPER(TRIM(NEW.Registro_MTC));
    SET NEW.Nombre_empresa_transporte = TRIM(NEW.Nombre_empresa_transporte);

    IF EXISTS (SELECT 1 FROM EMPRESA_TRANSPORTISTA
               WHERE Registro_MTC = UPPER(TRIM(NEW.Registro_MTC))) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ese registro MTC ya pertenece a otra empresa.';
    END IF;
END$$


DROP TRIGGER IF EXISTS TR_empresa_despues_insertar$$
CREATE TRIGGER TR_empresa_despues_insertar
AFTER INSERT ON EMPRESA_TRANSPORTISTA
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('EMPRESA_TRANSPORTISTA', 'INSERCION', NEW.RUC_de_la_empresa,
            CONCAT('Alta de ', NEW.Nombre_empresa_transporte,
                   ', representada por ', NEW.Representante));
END$$


-- -----------------------------------------------------------------------------
-- Un cambio de Estado no es una modificacion cualquiera: es una baja o una
-- reactivacion. El trigger lo distingue y lo registra como tal, de modo que en
-- la bitacora se lee lo que de verdad ocurrio.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS TR_empresa_despues_modificar$$
CREATE TRIGGER TR_empresa_despues_modificar
AFTER UPDATE ON EMPRESA_TRANSPORTISTA
FOR EACH ROW
BEGIN
    IF OLD.Estado = 'ACTIVO' AND NEW.Estado = 'INACTIVO' THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('EMPRESA_TRANSPORTISTA', 'BAJA', NEW.RUC_de_la_empresa,
                CONCAT('Baja logica de ', NEW.Nombre_empresa_transporte));

    ELSEIF OLD.Estado = 'INACTIVO' AND NEW.Estado = 'ACTIVO' THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('EMPRESA_TRANSPORTISTA', 'REACTIVACION', NEW.RUC_de_la_empresa,
                CONCAT('Reactivacion de ', NEW.Nombre_empresa_transporte));

    ELSE
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('EMPRESA_TRANSPORTISTA', 'MODIFICACION', NEW.RUC_de_la_empresa,
                CONCAT_WS(' | ',
                    IF(OLD.Nombre_empresa_transporte <> NEW.Nombre_empresa_transporte,
                       CONCAT('Razon social: ', OLD.Nombre_empresa_transporte,
                              ' -> ', NEW.Nombre_empresa_transporte), NULL),
                    IF(OLD.Representante <> NEW.Representante,
                       CONCAT('Representante: ', OLD.Representante,
                              ' -> ', NEW.Representante), NULL),
                    IF(IFNULL(OLD.Telefono,'') <> IFNULL(NEW.Telefono,''),
                       CONCAT('Telefono: ', IFNULL(OLD.Telefono,'-'),
                              ' -> ', IFNULL(NEW.Telefono,'-')), NULL),
                    IF(OLD.Domicilio <> NEW.Domicilio, 'Cambio de domicilio', NULL)));
    END IF;
END$$


-- =============================================================================
-- B. VEHICULO
-- =============================================================================

DROP TRIGGER IF EXISTS TR_vehiculo_antes_insertar$$
CREATE TRIGGER TR_vehiculo_antes_insertar
BEFORE INSERT ON VEHICULO
FOR EACH ROW
BEGIN
    SET NEW.Placa_de_vehiculo  = UPPER(TRIM(NEW.Placa_de_vehiculo));
    SET NEW.Marca_del_vehiculo = TRIM(NEW.Marca_del_vehiculo);

    -- Regla fisica: cada eje lleva como minimo dos neumaticos.
    IF NEW.Numero_de_neumaticos < NEW.Numero_de_ejes * 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un eje no puede tener menos de dos neumaticos.';
    END IF;

    -- Un tracto arrastra la carga, no la soporta: su tara propia no deberia
    -- acercarse al limite legal del conjunto.
    IF NEW.Descripcion_del_vehiculo = 'Tracto' AND NEW.Peso_seco > 20000 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La tara declarada es excesiva para un tracto.';
    END IF;
END$$


DROP TRIGGER IF EXISTS TR_vehiculo_despues_insertar$$
CREATE TRIGGER TR_vehiculo_despues_insertar
AFTER INSERT ON VEHICULO
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('VEHICULO', 'INSERCION', NEW.Placa_de_vehiculo,
            CONCAT('Alta de ', NEW.Descripcion_del_vehiculo, ' ',
                   NEW.Marca_del_vehiculo, ' ', NEW.Modelo_de_vehiculo,
                   ', tara ', NEW.Peso_seco, ' kg'));
END$$


-- -----------------------------------------------------------------------------
-- El peso seco merece un registro propio. Fue precisamente la divergencia de
-- este dato entre dos formularios en papel lo que origino el proyecto: el mismo
-- tracto figuraba con 9000 kg en una autorizacion y con 10000 kg en otra. Ahora
-- el dato vive en un solo lugar, y si alguien lo corrige queda escrito quien lo
-- hizo, cuando y de que valor a que valor.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS TR_vehiculo_despues_modificar$$
CREATE TRIGGER TR_vehiculo_despues_modificar
AFTER UPDATE ON VEHICULO
FOR EACH ROW
BEGIN
    IF OLD.Estado = 'ACTIVO' AND NEW.Estado = 'INACTIVO' THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('VEHICULO', 'BAJA', NEW.Placa_de_vehiculo,
                'Unidad retirada de la flota operativa');

    ELSEIF OLD.Estado = 'INACTIVO' AND NEW.Estado = 'ACTIVO' THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('VEHICULO', 'REACTIVACION', NEW.Placa_de_vehiculo,
                'Unidad reincorporada a la flota');

    ELSEIF OLD.Peso_seco <> NEW.Peso_seco THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('VEHICULO', 'MODIFICACION', NEW.Placa_de_vehiculo,
                CONCAT('ATENCION, cambio de tara: ', OLD.Peso_seco, ' kg -> ',
                       NEW.Peso_seco, ' kg. Afecta el peso total de ',
                       (SELECT COUNT(*) FROM FORMULARIO_VEHICULOS
                        WHERE Placa_de_vehiculo = NEW.Placa_de_vehiculo),
                       ' autorizacion(es)'));

    ELSE
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('VEHICULO', 'MODIFICACION', NEW.Placa_de_vehiculo,
                CONCAT_WS(' | ',
                    IF(OLD.Marca_del_vehiculo <> NEW.Marca_del_vehiculo,
                       CONCAT('Marca: ', OLD.Marca_del_vehiculo,
                              ' -> ', NEW.Marca_del_vehiculo), NULL),
                    IF(OLD.Modelo_de_vehiculo <> NEW.Modelo_de_vehiculo,
                       CONCAT('Modelo: ', OLD.Modelo_de_vehiculo,
                              ' -> ', NEW.Modelo_de_vehiculo), NULL),
                    IF(OLD.Numero_de_ejes <> NEW.Numero_de_ejes,
                       CONCAT('Ejes: ', OLD.Numero_de_ejes,
                              ' -> ', NEW.Numero_de_ejes), NULL),
                    'Actualizacion de ficha tecnica'));
    END IF;
END$$


-- =============================================================================
-- C. BONIFICACION
-- =============================================================================

DROP TRIGGER IF EXISTS TR_bonificacion_antes_insertar$$
CREATE TRIGGER TR_bonificacion_antes_insertar
BEFORE INSERT ON BONIFICACION
FOR EACH ROW
BEGIN
    SET NEW.Placa_bonificacion = UPPER(TRIM(NEW.Placa_bonificacion));

    -- La bonificacion se otorga a una placa concreta, de modo que esa placa
    -- tiene que existir en la flota.
    IF NOT EXISTS (SELECT 1 FROM VEHICULO
                   WHERE Placa_de_vehiculo = UPPER(TRIM(NEW.Placa_bonificacion))) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe en la flota un vehiculo con esa placa.';
    END IF;
END$$


DROP TRIGGER IF EXISTS TR_bonificacion_despues_insertar$$
CREATE TRIGGER TR_bonificacion_despues_insertar
AFTER INSERT ON BONIFICACION
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('BONIFICACION', 'INSERCION', NEW.Placa_bonificacion,
            CONCAT('Permiso ', NEW.Permiso_de_bonificacion, ', tope ',
                   NEW.Peso_bruto_total_con_bonificacion, ' kg'));
END$$


DROP TRIGGER IF EXISTS TR_bonificacion_despues_modificar$$
CREATE TRIGGER TR_bonificacion_despues_modificar
AFTER UPDATE ON BONIFICACION
FOR EACH ROW
BEGIN
    IF OLD.Estado <> NEW.Estado THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('BONIFICACION',
                IF(NEW.Estado = 'INACTIVO', 'BAJA', 'REACTIVACION'),
                NEW.Placa_bonificacion,
                CONCAT('Cambio de estado a ', NEW.Estado));
    ELSE
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('BONIFICACION', 'MODIFICACION', NEW.Placa_bonificacion,
                CONCAT('Tope: ', OLD.Peso_bruto_total_con_bonificacion,
                       ' kg -> ', NEW.Peso_bruto_total_con_bonificacion, ' kg'));
    END IF;
END$$


-- =============================================================================
-- D. FORMULARIO_AUTORIZACION
-- =============================================================================

DROP TRIGGER IF EXISTS TR_autorizacion_antes_insertar$$
CREATE TRIGGER TR_autorizacion_antes_insertar
BEFORE INSERT ON FORMULARIO_AUTORIZACION
FOR EACH ROW
BEGIN
    SET NEW.Numero_autogenerado = UPPER(TRIM(NEW.Numero_autogenerado));

    -- La vigencia no puede empezar antes de que el permiso se apruebe.
    IF NEW.Vigencia_desde < NEW.Fecha_aprobacion THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La vigencia no puede iniciar antes de la fecha de aprobacion.';
    END IF;

    -- El traslado tiene que ocurrir dentro de la ventana de vigencia.
    IF NEW.Fecha_salida < NEW.Vigencia_desde OR NEW.Fecha_llegada > NEW.Vigencia_hasta THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las fechas del traslado quedan fuera del periodo de vigencia.';
    END IF;

    -- Un transporte declarado como Especial exige al menos un vehiculo guia.
    IF NEW.Tipo_de_transporte = 'Especial' AND NEW.Numero_vehiculos_apoyo = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un transporte especial requiere al menos un vehiculo de apoyo.';
    END IF;
END$$


DROP TRIGGER IF EXISTS TR_autorizacion_despues_insertar$$
CREATE TRIGGER TR_autorizacion_despues_insertar
AFTER INSERT ON FORMULARIO_AUTORIZACION
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('FORMULARIO_AUTORIZACION', 'INSERCION', NEW.Numero_autogenerado,
            CONCAT('Emision de permiso ', NEW.Tipo_de_transporte, ': ',
                   NEW.Origen_traslado, ' -> ', NEW.Destino_traslado,
                   ', vigencia ', NEW.Vigencia_desde, ' a ', NEW.Vigencia_hasta));
END$$


-- -----------------------------------------------------------------------------
-- El motivo de la anulacion lo fija el procedimiento SP_AUTORIZACION_BAJA en la
-- variable de sesion @motivo_baja justo antes de ejecutar la actualizacion. El
-- trigger lo recoge y lo deja escrito en la bitacora. De este modo la anulacion
-- queda registrada una sola vez, con su razon, venga del aplicativo o de una
-- actualizacion manual.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS TR_autorizacion_despues_modificar$$
CREATE TRIGGER TR_autorizacion_despues_modificar
AFTER UPDATE ON FORMULARIO_AUTORIZACION
FOR EACH ROW
BEGIN
    IF OLD.Estado = 'ACTIVO' AND NEW.Estado = 'INACTIVO' THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('FORMULARIO_AUTORIZACION', 'BAJA', NEW.Numero_autogenerado,
                CONCAT('Anulacion. Motivo: ',
                       IFNULL(NULLIF(TRIM(IFNULL(@motivo_baja, '')), ''), 'no indicado')));

    ELSEIF OLD.Estado = 'INACTIVO' AND NEW.Estado = 'ACTIVO' THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('FORMULARIO_AUTORIZACION', 'REACTIVACION', NEW.Numero_autogenerado,
                'Autorizacion reactivada');

    ELSEIF OLD.Firma_del_transportista <> NEW.Firma_del_transportista
        OR OLD.Firma_y_sello_unidad_pesajes <> NEW.Firma_y_sello_unidad_pesajes THEN
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('FORMULARIO_AUTORIZACION', 'MODIFICACION', NEW.Numero_autogenerado,
                'Se adjunto o reemplazo una imagen de firma');

    ELSE
        INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
        VALUES ('FORMULARIO_AUTORIZACION', 'MODIFICACION', NEW.Numero_autogenerado,
                CONCAT_WS(' | ',
                    IF(OLD.Destino_traslado <> NEW.Destino_traslado,
                       CONCAT('Destino: ', OLD.Destino_traslado,
                              ' -> ', NEW.Destino_traslado), NULL),
                    IF(OLD.Peso_de_mercancia <> NEW.Peso_de_mercancia,
                       CONCAT('Mercancia: ', OLD.Peso_de_mercancia,
                              ' kg -> ', NEW.Peso_de_mercancia, ' kg'), NULL),
                    IF(OLD.Vigencia_hasta <> NEW.Vigencia_hasta,
                       CONCAT('Vigencia hasta: ', OLD.Vigencia_hasta,
                              ' -> ', NEW.Vigencia_hasta), NULL),
                    'Actualizacion de datos del permiso'));
    END IF;
END$$


-- =============================================================================
-- E. FORMULARIO_VEHICULOS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Esta es la regla que ninguna restriccion declarativa puede expresar: la clave
-- foranea garantiza que la placa exista, pero no que la unidad siga activa ni
-- que la autorizacion no este anulada. Hace falta consultar otra tabla, y eso
-- solo puede hacerlo un trigger.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS TR_fv_antes_insertar$$
CREATE TRIGGER TR_fv_antes_insertar
BEFORE INSERT ON FORMULARIO_VEHICULOS
FOR EACH ROW
BEGIN
    DECLARE v_estado_veh VARCHAR(10) DEFAULT NULL;
    DECLARE v_estado_aut VARCHAR(10) DEFAULT NULL;

    SELECT Estado INTO v_estado_veh
    FROM   VEHICULO WHERE Placa_de_vehiculo = NEW.Placa_de_vehiculo;

    SELECT Estado INTO v_estado_aut
    FROM   FORMULARIO_AUTORIZACION WHERE Numero_autogenerado = NEW.Numero_autogenerado;

    IF v_estado_veh = 'INACTIVO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede declarar una unidad dada de baja.';
    END IF;

    IF v_estado_aut = 'INACTIVO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede modificar el conjunto de una autorizacion anulada.';
    END IF;
END$$


DROP TRIGGER IF EXISTS TR_fv_despues_insertar$$
CREATE TRIGGER TR_fv_despues_insertar
AFTER INSERT ON FORMULARIO_VEHICULOS
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('FORMULARIO_VEHICULOS', 'INSERCION', NEW.Numero_autogenerado,
            CONCAT('Se declaro la unidad ', NEW.Placa_de_vehiculo, ' en el conjunto'));
END$$


-- -----------------------------------------------------------------------------
-- Las tablas de relacion si admiten borrado fisico, porque quitar una unidad de
-- un conjunto no es dar de baja la unidad: es corregir la composicion declarada.
-- Aun asi queda registrado, de modo que la correccion no pasa inadvertida.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS TR_fv_despues_borrar$$
CREATE TRIGGER TR_fv_despues_borrar
AFTER DELETE ON FORMULARIO_VEHICULOS
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('FORMULARIO_VEHICULOS', 'BAJA', OLD.Numero_autogenerado,
            CONCAT('Se retiro la unidad ', OLD.Placa_de_vehiculo,
                   ' del conjunto declarado'));
END$$


-- =============================================================================
-- E-bis. FORMULARIO_RUTAS
-- =============================================================================

DROP TRIGGER IF EXISTS TR_fr_despues_insertar$$
CREATE TRIGGER TR_fr_despues_insertar
AFTER INSERT ON FORMULARIO_RUTAS
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('FORMULARIO_RUTAS', 'INSERCION', NEW.Numero_autogenerado,
            CONCAT('Se declaro el tramo ', NEW.Ruta_a_transitar));
END$$


DROP TRIGGER IF EXISTS TR_fr_despues_borrar$$
CREATE TRIGGER TR_fr_despues_borrar
AFTER DELETE ON FORMULARIO_RUTAS
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('FORMULARIO_RUTAS', 'BAJA', OLD.Numero_autogenerado,
            CONCAT('Se retiro el tramo ', OLD.Ruta_a_transitar));
END$$


-- =============================================================================
-- F. DISTRIBUCION_PESO_EJE
-- =============================================================================

-- -----------------------------------------------------------------------------
-- El numero de eje declarado no puede exceder los ejes que realmente tiene el
-- conjunto vehicular. Comprobarlo exige sumar los ejes de las unidades
-- declaradas, que es una consulta a dos tablas.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS TR_dpe_antes_insertar$$
CREATE TRIGGER TR_dpe_antes_insertar
BEFORE INSERT ON DISTRIBUCION_PESO_EJE
FOR EACH ROW
BEGIN
    DECLARE v_ejes INT DEFAULT 0;

    SELECT IFNULL(SUM(v.Numero_de_ejes), 0) INTO v_ejes
    FROM        FORMULARIO_VEHICULOS fv
    INNER JOIN  VEHICULO             v ON v.Placa_de_vehiculo = fv.Placa_de_vehiculo
    WHERE       fv.Numero_autogenerado = NEW.Numero_autogenerado;

    IF v_ejes > 0 AND NEW.Numero_eje > v_ejes THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El numero de eje supera los ejes del conjunto declarado.';
    END IF;
END$$


DROP TRIGGER IF EXISTS TR_dpe_despues_insertar$$
CREATE TRIGGER TR_dpe_despues_insertar
AFTER INSERT ON DISTRIBUCION_PESO_EJE
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('DISTRIBUCION_PESO_EJE', 'INSERCION', NEW.Numero_autogenerado,
            CONCAT('Eje ', NEW.Numero_eje, ' declarado con ', NEW.Peso_eje, ' kg'));
END$$


DROP TRIGGER IF EXISTS TR_dpe_despues_modificar$$
CREATE TRIGGER TR_dpe_despues_modificar
AFTER UPDATE ON DISTRIBUCION_PESO_EJE
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('DISTRIBUCION_PESO_EJE', 'MODIFICACION', NEW.Numero_autogenerado,
            CONCAT('Eje ', NEW.Numero_eje, ': ', OLD.Peso_eje,
                   ' kg -> ', NEW.Peso_eje, ' kg'));
END$$


DROP TRIGGER IF EXISTS TR_dpe_despues_borrar$$
CREATE TRIGGER TR_dpe_despues_borrar
AFTER DELETE ON DISTRIBUCION_PESO_EJE
FOR EACH ROW
BEGIN
    INSERT INTO AUDITORIA (Tabla_afectada, Operacion, Clave_afectada, Detalle)
    VALUES ('DISTRIBUCION_PESO_EJE', 'BAJA', OLD.Numero_autogenerado,
            CONCAT('Se retiro la declaracion del eje ', OLD.Numero_eje));
END$$

DELIMITER ;


-- =============================================================================
-- Verificacion
-- =============================================================================
SELECT TRIGGER_NAME       AS 'Trigger',
       EVENT_MANIPULATION AS 'Evento',
       ACTION_TIMING      AS 'Momento',
       EVENT_OBJECT_TABLE AS 'Tabla'
FROM   information_schema.TRIGGERS
WHERE  TRIGGER_SCHEMA = 'marietha'
ORDER  BY EVENT_OBJECT_TABLE, ACTION_TIMING, TRIGGER_NAME;
