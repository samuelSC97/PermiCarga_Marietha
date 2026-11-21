-- =============================================================================
-- PermiCarga Marietha
-- Script 04 de 06: Procedimientos almacenados de mantenimiento
--
-- Todo el acceso del aplicativo a la base de datos pasa por estos procedimientos.
-- El cliente Java no escribe una sola sentencia SQL: unicamente invoca
-- procedimientos mediante CallableStatement. Esa decision persigue tres cosas:
--
--   1. La regla de negocio vive en la base de datos, no en el cliente. Si manana
--      se escribe otro cliente (web, movil), hereda las mismas validaciones.
--   2. El plan de ejecucion queda precompilado en el servidor.
--   3. Al no concatenarse texto SQL en el cliente, la inyeccion de SQL deja de
--      ser posible por construccion.
--
-- Convencion de nombres:
--   SP_<ENTIDAD>_<ACCION>   procedimientos de mantenimiento
--   SP_REP_<nn>_<ASUNTO>    procedimientos de reporte (script 05)
--
-- Los errores de negocio se devuelven con SIGNAL SQLSTATE '45000' y un mensaje
-- redactado en castellano, que el aplicativo muestra tal cual al usuario.
-- =============================================================================

USE marietha;

DELIMITER $$

-- =============================================================================
-- A. ACCESO AL SISTEMA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- SP_USUARIO_AUTENTICAR
-- Valida las credenciales de ingreso. La contrasena viaja en claro desde el
-- formulario, pero se compara contra el resumen SHA-256 almacenado: en la tabla
-- no existe la contrasena, solo su huella.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_USUARIO_AUTENTICAR$$
CREATE PROCEDURE SP_USUARIO_AUTENTICAR(
    IN p_usuario VARCHAR(20),
    IN p_clave   VARCHAR(100)
)
BEGIN
    DECLARE v_id     INT UNSIGNED DEFAULT NULL;
    DECLARE v_estado VARCHAR(10)  DEFAULT NULL;

    SELECT Id_usuario, Estado INTO v_id, v_estado
    FROM   USUARIO
    WHERE  Usuario = p_usuario
    LIMIT  1;

    IF v_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El usuario indicado no existe.';
    END IF;

    IF v_estado = 'INACTIVO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La cuenta esta desactivada. Consulte al administrador.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM USUARIO
                   WHERE Id_usuario = v_id AND Clave = SHA2(p_clave, 256)) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La contrasena es incorrecta.';
    END IF;

    UPDATE USUARIO SET Ultimo_acceso = NOW() WHERE Id_usuario = v_id;

    SELECT Id_usuario, Usuario, Nombre_completo, Perfil, Ultimo_acceso
    FROM   USUARIO
    WHERE  Id_usuario = v_id;
END$$


-- =============================================================================
-- B. EMPRESA TRANSPORTISTA
-- =============================================================================

DROP PROCEDURE IF EXISTS SP_EMPRESA_LISTAR$$
CREATE PROCEDURE SP_EMPRESA_LISTAR(
    IN p_incluir_inactivas BOOLEAN
)
BEGIN
    SELECT RUC_de_la_empresa         AS 'RUC',
           Nombre_empresa_transporte AS 'Razon social',
           Representante             AS 'Representante legal',
           Domicilio                 AS 'Domicilio',
           Registro_MTC              AS 'Registro MTC',
           IFNULL(Telefono, '-')     AS 'Telefono',
           Estado                    AS 'Estado'
    FROM   EMPRESA_TRANSPORTISTA
    WHERE  p_incluir_inactivas = TRUE OR Estado = 'ACTIVO'
    ORDER  BY Estado, Nombre_empresa_transporte;
END$$


DROP PROCEDURE IF EXISTS SP_EMPRESA_BUSCAR$$
CREATE PROCEDURE SP_EMPRESA_BUSCAR(
    IN p_texto VARCHAR(100)
)
BEGIN
    SELECT RUC_de_la_empresa         AS 'RUC',
           Nombre_empresa_transporte AS 'Razon social',
           Representante             AS 'Representante legal',
           Domicilio                 AS 'Domicilio',
           Registro_MTC              AS 'Registro MTC',
           IFNULL(Telefono, '-')     AS 'Telefono',
           Estado                    AS 'Estado'
    FROM   EMPRESA_TRANSPORTISTA
    WHERE  RUC_de_la_empresa         LIKE CONCAT('%', p_texto, '%')
       OR  Nombre_empresa_transporte LIKE CONCAT('%', p_texto, '%')
       OR  Representante             LIKE CONCAT('%', p_texto, '%')
       OR  Registro_MTC              LIKE CONCAT('%', p_texto, '%')
    ORDER  BY Estado, Nombre_empresa_transporte;
END$$


-- -----------------------------------------------------------------------------
-- SP_EMPRESA_OBTENER
-- Trae un registro con los nombres fisicos de las columnas, no con los rotulos
-- legibles que usan los procedimientos de listado. La diferencia es deliberada:
-- la rejilla muestra titulos pensados para leerse, mientras que el formulario
-- necesita identificar cada campo por su nombre real para volcarlo en su caja
-- de texto correspondiente.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_EMPRESA_OBTENER$$
CREATE PROCEDURE SP_EMPRESA_OBTENER(
    IN p_ruc CHAR(11)
)
BEGIN
    SELECT RUC_de_la_empresa, Nombre_empresa_transporte, Representante,
           Domicilio, Registro_MTC, IFNULL(Telefono, '') AS Telefono, Estado
    FROM   EMPRESA_TRANSPORTISTA
    WHERE  RUC_de_la_empresa = p_ruc;
END$$


DROP PROCEDURE IF EXISTS SP_EMPRESA_INSERTAR$$
CREATE PROCEDURE SP_EMPRESA_INSERTAR(
    IN p_ruc     CHAR(11),
    IN p_nombre  VARCHAR(100),
    IN p_repr    VARCHAR(80),
    IN p_domic   VARCHAR(150),
    IN p_mtc     VARCHAR(15),
    IN p_telef   CHAR(9)
)
BEGIN
    IF EXISTS (SELECT 1 FROM EMPRESA_TRANSPORTISTA WHERE RUC_de_la_empresa = p_ruc) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya existe una empresa registrada con ese RUC.';
    END IF;

    INSERT INTO EMPRESA_TRANSPORTISTA
        (RUC_de_la_empresa, Nombre_empresa_transporte, Representante,
         Domicilio, Registro_MTC, Telefono, Estado)
    VALUES
        (p_ruc, p_nombre, p_repr, p_domic, p_mtc,
         NULLIF(TRIM(IFNULL(p_telef, '')), ''), 'ACTIVO');

    SELECT p_ruc AS RUC_de_la_empresa,
           'Empresa registrada correctamente.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_EMPRESA_MODIFICAR$$
CREATE PROCEDURE SP_EMPRESA_MODIFICAR(
    IN p_ruc     CHAR(11),
    IN p_nombre  VARCHAR(100),
    IN p_repr    VARCHAR(80),
    IN p_domic   VARCHAR(150),
    IN p_mtc     VARCHAR(15),
    IN p_telef   CHAR(9)
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM EMPRESA_TRANSPORTISTA WHERE RUC_de_la_empresa = p_ruc) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una empresa con ese RUC.';
    END IF;

    UPDATE EMPRESA_TRANSPORTISTA
    SET    Nombre_empresa_transporte = p_nombre,
           Representante             = p_repr,
           Domicilio                 = p_domic,
           Registro_MTC              = p_mtc,
           Telefono                  = NULLIF(TRIM(IFNULL(p_telef, '')), '')
    WHERE  RUC_de_la_empresa = p_ruc;

    SELECT p_ruc AS RUC_de_la_empresa,
           'Empresa actualizada correctamente.' AS Mensaje;
END$$


-- -----------------------------------------------------------------------------
-- SP_EMPRESA_BAJA
-- Baja logica. No se ejecuta DELETE: la fila permanece y solo cambia su Estado,
-- porque las autorizaciones ya emitidas la siguen referenciando y su historia no
-- puede desaparecer. Se impide dar de baja una empresa que tenga autorizaciones
-- todavia vigentes, que seria dejar circulando un permiso sin titular activo.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_EMPRESA_BAJA$$
CREATE PROCEDURE SP_EMPRESA_BAJA(
    IN p_ruc CHAR(11)
)
BEGIN
    DECLARE v_vigentes INT DEFAULT 0;

    IF NOT EXISTS (SELECT 1 FROM EMPRESA_TRANSPORTISTA WHERE RUC_de_la_empresa = p_ruc) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una empresa con ese RUC.';
    END IF;

    SELECT COUNT(*) INTO v_vigentes
    FROM   FORMULARIO_AUTORIZACION
    WHERE  RUC_de_la_empresa = p_ruc
      AND  Estado = 'ACTIVO'
      AND  CURDATE() BETWEEN Vigencia_desde AND Vigencia_hasta;

    IF v_vigentes > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede dar de baja: la empresa tiene autorizaciones vigentes.';
    END IF;

    UPDATE EMPRESA_TRANSPORTISTA SET Estado = 'INACTIVO' WHERE RUC_de_la_empresa = p_ruc;

    SELECT p_ruc AS RUC_de_la_empresa,
           'Empresa dada de baja. Sus autorizaciones historicas se conservan.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_EMPRESA_REACTIVAR$$
CREATE PROCEDURE SP_EMPRESA_REACTIVAR(
    IN p_ruc CHAR(11)
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM EMPRESA_TRANSPORTISTA WHERE RUC_de_la_empresa = p_ruc) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una empresa con ese RUC.';
    END IF;

    UPDATE EMPRESA_TRANSPORTISTA SET Estado = 'ACTIVO' WHERE RUC_de_la_empresa = p_ruc;

    SELECT p_ruc AS RUC_de_la_empresa, 'Empresa reactivada.' AS Mensaje;
END$$


-- =============================================================================
-- C. VEHICULO
-- =============================================================================

DROP PROCEDURE IF EXISTS SP_VEHICULO_LISTAR$$
CREATE PROCEDURE SP_VEHICULO_LISTAR(
    IN p_incluir_inactivos BOOLEAN
)
BEGIN
    SELECT Placa_de_vehiculo        AS 'Placa',
           Descripcion_del_vehiculo AS 'Tipo de unidad',
           Marca_del_vehiculo       AS 'Marca',
           Modelo_de_vehiculo       AS 'Modelo',
           Largo_de_vehiculo        AS 'Largo (m)',
           Ancho_de_vehiculo        AS 'Ancho (m)',
           Alto_de_vehiculo         AS 'Alto (m)',
           Numero_de_ejes           AS 'Ejes',
           Numero_de_neumaticos     AS 'Neumaticos',
           Peso_seco                AS 'Tara (kg)',
           Estado                   AS 'Estado'
    FROM   VEHICULO
    WHERE  p_incluir_inactivos = TRUE OR Estado = 'ACTIVO'
    ORDER  BY Estado, Descripcion_del_vehiculo, Placa_de_vehiculo;
END$$


DROP PROCEDURE IF EXISTS SP_VEHICULO_BUSCAR$$
CREATE PROCEDURE SP_VEHICULO_BUSCAR(
    IN p_texto VARCHAR(60)
)
BEGIN
    SELECT Placa_de_vehiculo        AS 'Placa',
           Descripcion_del_vehiculo AS 'Tipo de unidad',
           Marca_del_vehiculo       AS 'Marca',
           Modelo_de_vehiculo       AS 'Modelo',
           Largo_de_vehiculo        AS 'Largo (m)',
           Ancho_de_vehiculo        AS 'Ancho (m)',
           Alto_de_vehiculo         AS 'Alto (m)',
           Numero_de_ejes           AS 'Ejes',
           Numero_de_neumaticos     AS 'Neumaticos',
           Peso_seco                AS 'Tara (kg)',
           Estado                   AS 'Estado'
    FROM   VEHICULO
    WHERE  Placa_de_vehiculo  LIKE CONCAT('%', p_texto, '%')
       OR  Marca_del_vehiculo LIKE CONCAT('%', p_texto, '%')
       OR  Modelo_de_vehiculo LIKE CONCAT('%', p_texto, '%')
    ORDER  BY Estado, Placa_de_vehiculo;
END$$


-- -----------------------------------------------------------------------------
-- SP_VEHICULO_OBTENER
-- Devuelve la ficha con los nombres fisicos de columna, para alimentar el
-- formulario de modificacion.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_VEHICULO_OBTENER$$
CREATE PROCEDURE SP_VEHICULO_OBTENER(
    IN p_placa CHAR(6)
)
BEGIN
    SELECT Placa_de_vehiculo, Descripcion_del_vehiculo, Marca_del_vehiculo,
           Modelo_de_vehiculo, Largo_de_vehiculo, Ancho_de_vehiculo,
           Alto_de_vehiculo, Numero_de_ejes, Numero_de_neumaticos,
           Peso_seco, Estado
    FROM   VEHICULO
    WHERE  Placa_de_vehiculo = p_placa;
END$$


DROP PROCEDURE IF EXISTS SP_VEHICULO_INSERTAR$$
CREATE PROCEDURE SP_VEHICULO_INSERTAR(
    IN p_placa   CHAR(6),
    IN p_tipo    VARCHAR(20),
    IN p_marca   VARCHAR(30),
    IN p_modelo  VARCHAR(30),
    IN p_largo   DECIMAL(5,2),
    IN p_ancho   DECIMAL(4,2),
    IN p_alto    DECIMAL(4,2),
    IN p_ejes    TINYINT UNSIGNED,
    IN p_neum    TINYINT UNSIGNED,
    IN p_peso    MEDIUMINT UNSIGNED
)
BEGIN
    IF EXISTS (SELECT 1 FROM VEHICULO WHERE Placa_de_vehiculo = p_placa) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya existe un vehiculo registrado con esa placa.';
    END IF;

    IF p_neum < p_ejes * 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El numero de neumaticos no puede ser menor a dos por eje.';
    END IF;

    INSERT INTO VEHICULO
        (Placa_de_vehiculo, Descripcion_del_vehiculo, Marca_del_vehiculo,
         Modelo_de_vehiculo, Largo_de_vehiculo, Ancho_de_vehiculo, Alto_de_vehiculo,
         Numero_de_ejes, Numero_de_neumaticos, Peso_seco, Estado)
    VALUES
        (p_placa, p_tipo, p_marca, p_modelo, p_largo, p_ancho, p_alto,
         p_ejes, p_neum, p_peso, 'ACTIVO');

    SELECT p_placa AS Placa_de_vehiculo,
           'Vehiculo registrado correctamente.' AS Mensaje;
END$$


-- -----------------------------------------------------------------------------
-- SP_VEHICULO_MODIFICAR
-- Cambiar el peso seco de una unidad ya declarada en autorizaciones alteraria el
-- peso total de traslados historicos. El procedimiento lo permite, porque puede
-- tratarse de la correccion de un dato mal tomado, pero lo deja anotado en la
-- auditoria con su valor anterior: esa es exactamente la trazabilidad que
-- faltaba cuando el dato se repetia a mano en cada formulario.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_VEHICULO_MODIFICAR$$
CREATE PROCEDURE SP_VEHICULO_MODIFICAR(
    IN p_placa   CHAR(6),
    IN p_tipo    VARCHAR(20),
    IN p_marca   VARCHAR(30),
    IN p_modelo  VARCHAR(30),
    IN p_largo   DECIMAL(5,2),
    IN p_ancho   DECIMAL(4,2),
    IN p_alto    DECIMAL(4,2),
    IN p_ejes    TINYINT UNSIGNED,
    IN p_neum    TINYINT UNSIGNED,
    IN p_peso    MEDIUMINT UNSIGNED
)
BEGIN
    DECLARE v_usos INT DEFAULT 0;

    IF NOT EXISTS (SELECT 1 FROM VEHICULO WHERE Placa_de_vehiculo = p_placa) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe un vehiculo con esa placa.';
    END IF;

    IF p_neum < p_ejes * 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El numero de neumaticos no puede ser menor a dos por eje.';
    END IF;

    SELECT COUNT(*) INTO v_usos
    FROM   FORMULARIO_VEHICULOS
    WHERE  Placa_de_vehiculo = p_placa;

    UPDATE VEHICULO
    SET    Descripcion_del_vehiculo = p_tipo,
           Marca_del_vehiculo       = p_marca,
           Modelo_de_vehiculo       = p_modelo,
           Largo_de_vehiculo        = p_largo,
           Ancho_de_vehiculo        = p_ancho,
           Alto_de_vehiculo         = p_alto,
           Numero_de_ejes           = p_ejes,
           Numero_de_neumaticos     = p_neum,
           Peso_seco                = p_peso
    WHERE  Placa_de_vehiculo = p_placa;

    SELECT p_placa AS Placa_de_vehiculo,
           CONCAT('Vehiculo actualizado. Participa en ', v_usos,
                  ' autorizacion(es); el cambio queda registrado en auditoria.') AS Mensaje;
END$$


-- -----------------------------------------------------------------------------
-- SP_VEHICULO_BAJA
-- Baja logica de una unidad. Se impide si participa en alguna autorizacion
-- vigente, porque el permiso en circulacion la declara como parte del conjunto.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_VEHICULO_BAJA$$
CREATE PROCEDURE SP_VEHICULO_BAJA(
    IN p_placa CHAR(6)
)
BEGIN
    DECLARE v_vigentes INT DEFAULT 0;

    IF NOT EXISTS (SELECT 1 FROM VEHICULO WHERE Placa_de_vehiculo = p_placa) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe un vehiculo con esa placa.';
    END IF;

    SELECT COUNT(*) INTO v_vigentes
    FROM        FORMULARIO_VEHICULOS    fv
    INNER JOIN  FORMULARIO_AUTORIZACION fa ON fa.Numero_autogenerado = fv.Numero_autogenerado
    WHERE       fv.Placa_de_vehiculo = p_placa
      AND       fa.Estado = 'ACTIVO'
      AND       CURDATE() BETWEEN fa.Vigencia_desde AND fa.Vigencia_hasta;

    IF v_vigentes > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede dar de baja: la unidad figura en autorizaciones vigentes.';
    END IF;

    UPDATE VEHICULO SET Estado = 'INACTIVO' WHERE Placa_de_vehiculo = p_placa;

    SELECT p_placa AS Placa_de_vehiculo,
           'Unidad retirada de la flota operativa. Su historial se conserva.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_VEHICULO_REACTIVAR$$
CREATE PROCEDURE SP_VEHICULO_REACTIVAR(
    IN p_placa CHAR(6)
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM VEHICULO WHERE Placa_de_vehiculo = p_placa) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe un vehiculo con esa placa.';
    END IF;

    UPDATE VEHICULO SET Estado = 'ACTIVO' WHERE Placa_de_vehiculo = p_placa;

    SELECT p_placa AS Placa_de_vehiculo, 'Unidad reincorporada a la flota.' AS Mensaje;
END$$


-- =============================================================================
-- D. BONIFICACION
-- =============================================================================

DROP PROCEDURE IF EXISTS SP_BONIFICACION_LISTAR$$
CREATE PROCEDURE SP_BONIFICACION_LISTAR(
    IN p_incluir_inactivas BOOLEAN
)
BEGIN
    SELECT Placa_bonificacion, Permiso_de_bonificacion,
           Peso_bruto_total_con_bonificacion, Estado
    FROM   BONIFICACION
    WHERE  p_incluir_inactivas = TRUE OR Estado = 'ACTIVO'
    ORDER  BY Estado, Placa_bonificacion;
END$$


DROP PROCEDURE IF EXISTS SP_BONIFICACION_INSERTAR$$
CREATE PROCEDURE SP_BONIFICACION_INSERTAR(
    IN p_placa   CHAR(6),
    IN p_permiso CHAR(10),
    IN p_peso    MEDIUMINT UNSIGNED
)
BEGIN
    IF EXISTS (SELECT 1 FROM BONIFICACION WHERE Placa_bonificacion = p_placa) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Esa placa ya tiene una bonificacion registrada.';
    END IF;

    IF p_peso <= 48000 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La bonificacion solo aplica por encima de 48000 kg.';
    END IF;

    INSERT INTO BONIFICACION
        (Placa_bonificacion, Permiso_de_bonificacion,
         Peso_bruto_total_con_bonificacion, Estado)
    VALUES (p_placa, p_permiso, p_peso, 'ACTIVO');

    SELECT p_placa AS Placa_bonificacion,
           'Bonificacion registrada correctamente.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_BONIFICACION_MODIFICAR$$
CREATE PROCEDURE SP_BONIFICACION_MODIFICAR(
    IN p_placa   CHAR(6),
    IN p_permiso CHAR(10),
    IN p_peso    MEDIUMINT UNSIGNED
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM BONIFICACION WHERE Placa_bonificacion = p_placa) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una bonificacion para esa placa.';
    END IF;

    IF p_peso <= 48000 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La bonificacion solo aplica por encima de 48000 kg.';
    END IF;

    UPDATE BONIFICACION
    SET    Permiso_de_bonificacion           = p_permiso,
           Peso_bruto_total_con_bonificacion = p_peso
    WHERE  Placa_bonificacion = p_placa;

    SELECT p_placa AS Placa_bonificacion, 'Bonificacion actualizada.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_BONIFICACION_BAJA$$
CREATE PROCEDURE SP_BONIFICACION_BAJA(
    IN p_placa CHAR(6)
)
BEGIN
    DECLARE v_vigentes INT DEFAULT 0;

    IF NOT EXISTS (SELECT 1 FROM BONIFICACION WHERE Placa_bonificacion = p_placa) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una bonificacion para esa placa.';
    END IF;

    SELECT COUNT(*) INTO v_vigentes
    FROM   FORMULARIO_AUTORIZACION
    WHERE  Placa_bonificacion = p_placa
      AND  Estado = 'ACTIVO'
      AND  CURDATE() BETWEEN Vigencia_desde AND Vigencia_hasta;

    IF v_vigentes > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede dar de baja: la bonificacion se usa en permisos vigentes.';
    END IF;

    UPDATE BONIFICACION SET Estado = 'INACTIVO' WHERE Placa_bonificacion = p_placa;

    SELECT p_placa AS Placa_bonificacion, 'Bonificacion dada de baja.' AS Mensaje;
END$$


-- =============================================================================
-- E. AUTORIZACION
-- =============================================================================

DROP PROCEDURE IF EXISTS SP_AUTORIZACION_LISTAR$$
CREATE PROCEDURE SP_AUTORIZACION_LISTAR(
    IN p_situacion VARCHAR(15)
)
BEGIN
    SELECT Numero_autogenerado                AS 'Numero',
           Nombre_empresa_transporte          AS 'Empresa',
           Origen_traslado                    AS 'Origen',
           Destino_traslado                   AS 'Destino',
           Fecha_salida                       AS 'Salida',
           Fecha_llegada                      AS 'Llegada',
           Tipo_de_transporte                 AS 'Tipo',
           IFNULL(Conjunto_vehicular, '-')    AS 'Conjunto vehicular',
           IFNULL(Peso_de_vehiculo_total, 0)  AS 'Peso total (kg)',
           Vigencia_desde                     AS 'Vigente desde',
           Vigencia_hasta                     AS 'Vigente hasta',
           Situacion                          AS 'Situacion',
           Estado                             AS 'Estado'
    FROM   V_AUTORIZACION_COMPLETA
    WHERE  p_situacion IS NULL
       OR  p_situacion = ''
       OR  p_situacion = 'TODAS'
       OR  Situacion   = p_situacion
    ORDER  BY Vigencia_hasta DESC;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_BUSCAR$$
CREATE PROCEDURE SP_AUTORIZACION_BUSCAR(
    IN p_texto VARCHAR(150)
)
BEGIN
    SELECT Numero_autogenerado                AS 'Numero',
           Nombre_empresa_transporte          AS 'Empresa',
           Origen_traslado                    AS 'Origen',
           Destino_traslado                   AS 'Destino',
           Fecha_salida                       AS 'Salida',
           Fecha_llegada                      AS 'Llegada',
           Tipo_de_transporte                 AS 'Tipo',
           IFNULL(Conjunto_vehicular, '-')    AS 'Conjunto vehicular',
           IFNULL(Peso_de_vehiculo_total, 0)  AS 'Peso total (kg)',
           Vigencia_desde                     AS 'Vigente desde',
           Vigencia_hasta                     AS 'Vigente hasta',
           Situacion                          AS 'Situacion',
           Estado                             AS 'Estado'
    FROM   V_AUTORIZACION_COMPLETA
    WHERE  Numero_autogenerado        LIKE CONCAT('%', p_texto, '%')
       OR  Nombre_empresa_transporte  LIKE CONCAT('%', p_texto, '%')
       OR  Origen_traslado            LIKE CONCAT('%', p_texto, '%')
       OR  Destino_traslado           LIKE CONCAT('%', p_texto, '%')
       OR  IFNULL(Conjunto_vehicular, '') LIKE CONCAT('%', p_texto, '%')
    ORDER  BY Vigencia_hasta DESC;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_OBTENER$$
CREATE PROCEDURE SP_AUTORIZACION_OBTENER(
    IN p_numero CHAR(10)
)
BEGIN
    SELECT Numero_autogenerado, RUC_de_la_empresa, Origen_traslado, Destino_traslado,
           Fecha_salida, Fecha_llegada, Descripcion_de_la_mercancia,
           Largo_de_mercancia, Ancho_de_mercancia, Alto_de_mercancia,
           Peso_de_mercancia, Tipo_de_transporte, Exceso_anterior, Exceso_posterior,
           Largo_total_vehiculo, Ancho_total_vehiculo, Alto_total_vehiculo,
           Numero_vehiculos_apoyo, Apoyo_policial, Placa_bonificacion,
           Fecha_aprobacion, Hora_aprobacion, Horario_de_autorizacion_valida,
           Vigencia_desde, Vigencia_hasta, Estado
    FROM   FORMULARIO_AUTORIZACION
    WHERE  Numero_autogenerado = p_numero;
END$$


-- -----------------------------------------------------------------------------
-- SP_AUTORIZACION_INSERTAR
-- Registra la cabecera del permiso. Las firmas se cargan con el procedimiento
-- SP_AUTORIZACION_CARGAR_FIRMA, de modo que el usuario puede adjuntar la imagen
-- escaneada despues de capturar los datos y no se bloquea el registro por no
-- tener el escaneo a la mano.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_AUTORIZACION_INSERTAR$$
CREATE PROCEDURE SP_AUTORIZACION_INSERTAR(
    IN p_numero      CHAR(10),
    IN p_ruc         CHAR(11),
    IN p_origen      VARCHAR(150),
    IN p_destino     VARCHAR(150),
    IN p_f_salida    DATE,
    IN p_f_llegada   DATE,
    IN p_mercancia   VARCHAR(200),
    IN p_largo_m     DECIMAL(5,2),
    IN p_ancho_m     DECIMAL(5,2),
    IN p_alto_m      DECIMAL(5,2),
    IN p_peso_m      MEDIUMINT UNSIGNED,
    IN p_tipo        VARCHAR(10),
    IN p_exc_ant     DECIMAL(4,2),
    IN p_exc_pos     DECIMAL(4,2),
    IN p_largo_t     DECIMAL(5,2),
    IN p_ancho_t     DECIMAL(4,2),
    IN p_alto_t      DECIMAL(4,2),
    IN p_veh_apoyo   TINYINT UNSIGNED,
    IN p_apoyo_pol   BOOLEAN,
    IN p_placa_bon   CHAR(6),
    IN p_horario     VARCHAR(30),
    IN p_vig_desde   DATE,
    IN p_vig_hasta   DATE
)
BEGIN
    DECLARE v_estado_emp VARCHAR(10) DEFAULT NULL;

    IF EXISTS (SELECT 1 FROM FORMULARIO_AUTORIZACION WHERE Numero_autogenerado = p_numero) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya existe una autorizacion con ese numero.';
    END IF;

    SELECT Estado INTO v_estado_emp
    FROM   EMPRESA_TRANSPORTISTA
    WHERE  RUC_de_la_empresa = p_ruc;

    IF v_estado_emp IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La empresa indicada no esta registrada.';
    END IF;

    IF v_estado_emp = 'INACTIVO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede emitir un permiso a una empresa dada de baja.';
    END IF;

    IF p_f_llegada < p_f_salida THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de llegada no puede ser anterior a la de salida.';
    END IF;

    IF p_vig_hasta < p_vig_desde THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La vigencia final no puede ser anterior a la inicial.';
    END IF;

    IF p_placa_bon IS NOT NULL AND p_placa_bon <> ''
       AND NOT EXISTS (SELECT 1 FROM BONIFICACION
                       WHERE Placa_bonificacion = p_placa_bon AND Estado = 'ACTIVO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La bonificacion indicada no existe o esta dada de baja.';
    END IF;

    INSERT INTO FORMULARIO_AUTORIZACION
        (Numero_autogenerado, RUC_de_la_empresa, Firma_del_transportista,
         Origen_traslado, Destino_traslado, Fecha_salida, Fecha_llegada,
         Descripcion_de_la_mercancia, Largo_de_mercancia, Ancho_de_mercancia,
         Alto_de_mercancia, Peso_de_mercancia, Tipo_de_transporte, Exceso_anterior,
         Exceso_posterior, Largo_total_vehiculo, Ancho_total_vehiculo,
         Alto_total_vehiculo, Numero_vehiculos_apoyo, Apoyo_policial,
         Placa_bonificacion, Horario_de_autorizacion_valida,
         Vigencia_desde, Vigencia_hasta, Firma_y_sello_unidad_pesajes, Estado)
    VALUES
        (p_numero, p_ruc, UNHEX('FFD8FFE0'),
         p_origen, p_destino, p_f_salida, p_f_llegada,
         p_mercancia, p_largo_m, p_ancho_m, p_alto_m, p_peso_m, p_tipo, p_exc_ant,
         p_exc_pos, p_largo_t, p_ancho_t, p_alto_t, p_veh_apoyo, p_apoyo_pol,
         NULLIF(TRIM(IFNULL(p_placa_bon, '')), ''), p_horario,
         p_vig_desde, p_vig_hasta, UNHEX('FFD8FFE0'), 'ACTIVO');

    SELECT p_numero AS Numero_autogenerado,
           'Autorizacion registrada. Declare ahora su conjunto vehicular y sus rutas.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_MODIFICAR$$
CREATE PROCEDURE SP_AUTORIZACION_MODIFICAR(
    IN p_numero      CHAR(10),
    IN p_origen      VARCHAR(150),
    IN p_destino     VARCHAR(150),
    IN p_f_salida    DATE,
    IN p_f_llegada   DATE,
    IN p_mercancia   VARCHAR(200),
    IN p_largo_m     DECIMAL(5,2),
    IN p_ancho_m     DECIMAL(5,2),
    IN p_alto_m      DECIMAL(5,2),
    IN p_peso_m      MEDIUMINT UNSIGNED,
    IN p_tipo        VARCHAR(10),
    IN p_exc_ant     DECIMAL(4,2),
    IN p_exc_pos     DECIMAL(4,2),
    IN p_largo_t     DECIMAL(5,2),
    IN p_ancho_t     DECIMAL(4,2),
    IN p_alto_t      DECIMAL(4,2),
    IN p_veh_apoyo   TINYINT UNSIGNED,
    IN p_apoyo_pol   BOOLEAN,
    IN p_placa_bon   CHAR(6),
    IN p_horario     VARCHAR(30),
    IN p_vig_desde   DATE,
    IN p_vig_hasta   DATE
)
BEGIN
    DECLARE v_estado VARCHAR(10) DEFAULT NULL;

    SELECT Estado INTO v_estado
    FROM   FORMULARIO_AUTORIZACION
    WHERE  Numero_autogenerado = p_numero;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una autorizacion con ese numero.';
    END IF;

    IF v_estado = 'INACTIVO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La autorizacion esta anulada. Reactivela antes de modificarla.';
    END IF;

    IF p_f_llegada < p_f_salida THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de llegada no puede ser anterior a la de salida.';
    END IF;

    IF p_vig_hasta < p_vig_desde THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La vigencia final no puede ser anterior a la inicial.';
    END IF;

    UPDATE FORMULARIO_AUTORIZACION
    SET    Origen_traslado                = p_origen,
           Destino_traslado               = p_destino,
           Fecha_salida                   = p_f_salida,
           Fecha_llegada                  = p_f_llegada,
           Descripcion_de_la_mercancia    = p_mercancia,
           Largo_de_mercancia             = p_largo_m,
           Ancho_de_mercancia             = p_ancho_m,
           Alto_de_mercancia              = p_alto_m,
           Peso_de_mercancia              = p_peso_m,
           Tipo_de_transporte             = p_tipo,
           Exceso_anterior                = p_exc_ant,
           Exceso_posterior               = p_exc_pos,
           Largo_total_vehiculo           = p_largo_t,
           Ancho_total_vehiculo           = p_ancho_t,
           Alto_total_vehiculo            = p_alto_t,
           Numero_vehiculos_apoyo         = p_veh_apoyo,
           Apoyo_policial                 = p_apoyo_pol,
           Placa_bonificacion             = NULLIF(TRIM(IFNULL(p_placa_bon, '')), ''),
           Horario_de_autorizacion_valida = p_horario,
           Vigencia_desde                 = p_vig_desde,
           Vigencia_hasta                 = p_vig_hasta
    WHERE  Numero_autogenerado = p_numero;

    SELECT p_numero AS Numero_autogenerado, 'Autorizacion actualizada.' AS Mensaje;
END$$


-- -----------------------------------------------------------------------------
-- SP_AUTORIZACION_BAJA
-- Anulacion logica del permiso. Las unidades, rutas y ejes declarados se
-- conservan: la autorizacion anulada sigue siendo parte del historial de la
-- empresa y puede necesitar consultarse ante una fiscalizacion posterior.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_AUTORIZACION_BAJA$$
CREATE PROCEDURE SP_AUTORIZACION_BAJA(
    IN p_numero CHAR(10),
    IN p_motivo VARCHAR(150)
)
BEGIN
    DECLARE v_estado VARCHAR(10) DEFAULT NULL;

    SELECT Estado INTO v_estado
    FROM   FORMULARIO_AUTORIZACION
    WHERE  Numero_autogenerado = p_numero;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una autorizacion con ese numero.';
    END IF;

    IF v_estado = 'INACTIVO' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La autorizacion ya se encuentra anulada.';
    END IF;

    -- El motivo se deja en una variable de sesion para que lo recoja el trigger
    -- TR_autorizacion_despues_modificar, que es quien escribe la bitacora. Asi
    -- la anulacion queda registrada una sola vez y con su razon.
    SET @motivo_baja = p_motivo;

    UPDATE FORMULARIO_AUTORIZACION
    SET    Estado = 'INACTIVO'
    WHERE  Numero_autogenerado = p_numero;

    SET @motivo_baja = NULL;

    SELECT p_numero AS Numero_autogenerado,
           'Autorizacion anulada. Su historial se conserva.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_REACTIVAR$$
CREATE PROCEDURE SP_AUTORIZACION_REACTIVAR(
    IN p_numero CHAR(10)
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM FORMULARIO_AUTORIZACION WHERE Numero_autogenerado = p_numero) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una autorizacion con ese numero.';
    END IF;

    UPDATE FORMULARIO_AUTORIZACION SET Estado = 'ACTIVO' WHERE Numero_autogenerado = p_numero;

    SELECT p_numero AS Numero_autogenerado, 'Autorizacion reactivada.' AS Mensaje;
END$$


-- -----------------------------------------------------------------------------
-- SP_AUTORIZACION_CARGAR_FIRMA
-- Adjunta una imagen escaneada a la autorizacion. p_cual admite los valores
-- TRANSPORTISTA o PESAJES, que corresponden a las dos columnas MEDIUMBLOB.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_AUTORIZACION_CARGAR_FIRMA$$
CREATE PROCEDURE SP_AUTORIZACION_CARGAR_FIRMA(
    IN p_numero  CHAR(10),
    IN p_cual    VARCHAR(15),
    IN p_imagen  MEDIUMBLOB
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM FORMULARIO_AUTORIZACION WHERE Numero_autogenerado = p_numero) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una autorizacion con ese numero.';
    END IF;

    IF p_imagen IS NULL OR LENGTH(p_imagen) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El archivo de imagen esta vacio.';
    END IF;

    IF p_cual = 'TRANSPORTISTA' THEN
        UPDATE FORMULARIO_AUTORIZACION
        SET    Firma_del_transportista = p_imagen
        WHERE  Numero_autogenerado = p_numero;
    ELSEIF p_cual = 'PESAJES' THEN
        UPDATE FORMULARIO_AUTORIZACION
        SET    Firma_y_sello_unidad_pesajes = p_imagen
        WHERE  Numero_autogenerado = p_numero;
    ELSE
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Indique TRANSPORTISTA o PESAJES.';
    END IF;

    SELECT p_numero AS Numero_autogenerado,
           CONCAT('Imagen de ', LOWER(p_cual), ' almacenada (',
                  LENGTH(p_imagen), ' bytes).') AS Mensaje;
END$$


-- -----------------------------------------------------------------------------
-- Composicion de la autorizacion: unidades, rutas y ejes
-- -----------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS SP_AUTORIZACION_VEHICULOS_LISTAR$$
CREATE PROCEDURE SP_AUTORIZACION_VEHICULOS_LISTAR(
    IN p_numero CHAR(10)
)
BEGIN
    SELECT  v.Placa_de_vehiculo        AS 'Placa',
            v.Descripcion_del_vehiculo AS 'Tipo de unidad',
            v.Marca_del_vehiculo       AS 'Marca',
            v.Modelo_de_vehiculo       AS 'Modelo',
            v.Numero_de_ejes           AS 'Ejes',
            v.Peso_seco                AS 'Tara (kg)'
    FROM        FORMULARIO_VEHICULOS fv
    INNER JOIN  VEHICULO             v ON v.Placa_de_vehiculo = fv.Placa_de_vehiculo
    WHERE       fv.Numero_autogenerado = p_numero
    ORDER  BY   v.Descripcion_del_vehiculo, v.Placa_de_vehiculo;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_VEHICULO_AGREGAR$$
CREATE PROCEDURE SP_AUTORIZACION_VEHICULO_AGREGAR(
    IN p_numero CHAR(10),
    IN p_placa  CHAR(6)
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM FORMULARIO_AUTORIZACION WHERE Numero_autogenerado = p_numero) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una autorizacion con ese numero.';
    END IF;

    IF EXISTS (SELECT 1 FROM FORMULARIO_VEHICULOS
               WHERE Numero_autogenerado = p_numero AND Placa_de_vehiculo = p_placa) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Esa unidad ya esta declarada en la autorizacion.';
    END IF;

    INSERT INTO FORMULARIO_VEHICULOS (Numero_autogenerado, Placa_de_vehiculo)
    VALUES (p_numero, p_placa);

    SELECT p_placa AS Placa_de_vehiculo, 'Unidad agregada al conjunto.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_VEHICULO_QUITAR$$
CREATE PROCEDURE SP_AUTORIZACION_VEHICULO_QUITAR(
    IN p_numero CHAR(10),
    IN p_placa  CHAR(6)
)
BEGIN
    DECLARE v_cuantos INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cuantos
    FROM   FORMULARIO_VEHICULOS
    WHERE  Numero_autogenerado = p_numero;

    IF v_cuantos <= 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La autorizacion debe declarar al menos una unidad.';
    END IF;

    -- La bitacora la escribe el trigger TR_fv_despues_borrar.
    DELETE FROM FORMULARIO_VEHICULOS
    WHERE  Numero_autogenerado = p_numero AND Placa_de_vehiculo = p_placa;

    SELECT p_placa AS Placa_de_vehiculo, 'Unidad retirada del conjunto.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_RUTAS_LISTAR$$
CREATE PROCEDURE SP_AUTORIZACION_RUTAS_LISTAR(
    IN p_numero CHAR(10)
)
BEGIN
    SELECT Ruta_a_transitar AS 'Tramo de ruta nacional'
    FROM   FORMULARIO_RUTAS
    WHERE  Numero_autogenerado = p_numero
    ORDER  BY Ruta_a_transitar;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_RUTA_AGREGAR$$
CREATE PROCEDURE SP_AUTORIZACION_RUTA_AGREGAR(
    IN p_numero CHAR(10),
    IN p_ruta   VARCHAR(40)
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM FORMULARIO_AUTORIZACION WHERE Numero_autogenerado = p_numero) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una autorizacion con ese numero.';
    END IF;

    IF EXISTS (SELECT 1 FROM FORMULARIO_RUTAS
               WHERE Numero_autogenerado = p_numero AND Ruta_a_transitar = p_ruta) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ese tramo ya esta declarado en la autorizacion.';
    END IF;

    INSERT INTO FORMULARIO_RUTAS (Numero_autogenerado, Ruta_a_transitar)
    VALUES (p_numero, p_ruta);

    SELECT p_ruta AS Ruta_a_transitar, 'Tramo agregado.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_RUTA_QUITAR$$
CREATE PROCEDURE SP_AUTORIZACION_RUTA_QUITAR(
    IN p_numero CHAR(10),
    IN p_ruta   VARCHAR(40)
)
BEGIN
    DECLARE v_cuantas INT DEFAULT 0;

    SELECT COUNT(*) INTO v_cuantas
    FROM   FORMULARIO_RUTAS
    WHERE  Numero_autogenerado = p_numero;

    IF v_cuantas <= 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La autorizacion debe declarar al menos un tramo de ruta.';
    END IF;

    -- La bitacora la escribe el trigger TR_fr_despues_borrar.
    DELETE FROM FORMULARIO_RUTAS
    WHERE  Numero_autogenerado = p_numero AND Ruta_a_transitar = p_ruta;

    SELECT p_ruta AS Ruta_a_transitar, 'Tramo retirado.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_EJES_LISTAR$$
CREATE PROCEDURE SP_AUTORIZACION_EJES_LISTAR(
    IN p_numero CHAR(10)
)
BEGIN
    SELECT Numero_eje AS 'Numero de eje',
           Peso_eje   AS 'Peso declarado (kg)'
    FROM   DISTRIBUCION_PESO_EJE
    WHERE  Numero_autogenerado = p_numero
    ORDER  BY Numero_eje;
END$$


-- -----------------------------------------------------------------------------
-- SP_AUTORIZACION_EJE_REGISTRAR
-- Provias Nacional exige la distribucion por eje solo cuando el conjunto supera
-- los 48000 kg. El procedimiento consulta la vista del peso total para hacer
-- cumplir esa condicion sin que el usuario tenga que calcularla.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_AUTORIZACION_EJE_REGISTRAR$$
CREATE PROCEDURE SP_AUTORIZACION_EJE_REGISTRAR(
    IN p_numero CHAR(10),
    IN p_eje    TINYINT UNSIGNED,
    IN p_peso   MEDIUMINT UNSIGNED
)
BEGIN
    DECLARE v_total INT DEFAULT 0;

    IF NOT EXISTS (SELECT 1 FROM FORMULARIO_AUTORIZACION WHERE Numero_autogenerado = p_numero) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe una autorizacion con ese numero.';
    END IF;

    SELECT IFNULL(Peso_de_vehiculo_total, 0) INTO v_total
    FROM   V_PESO_TOTAL_AUTORIZACION
    WHERE  Numero_autogenerado = p_numero;

    IF v_total <= 48000 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La distribucion por eje solo se declara si el conjunto supera 48000 kg.';
    END IF;

    INSERT INTO DISTRIBUCION_PESO_EJE (Numero_autogenerado, Numero_eje, Peso_eje)
    VALUES (p_numero, p_eje, p_peso)
    ON DUPLICATE KEY UPDATE Peso_eje = p_peso;

    SELECT p_eje AS Numero_eje, 'Peso del eje registrado.' AS Mensaje;
END$$


DROP PROCEDURE IF EXISTS SP_AUTORIZACION_EJE_QUITAR$$
CREATE PROCEDURE SP_AUTORIZACION_EJE_QUITAR(
    IN p_numero CHAR(10),
    IN p_eje    TINYINT UNSIGNED
)
BEGIN
    DELETE FROM DISTRIBUCION_PESO_EJE
    WHERE  Numero_autogenerado = p_numero AND Numero_eje = p_eje;

    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ese eje no estaba declarado en la autorizacion.';
    END IF;

    -- La bitacora la escribe el trigger TR_dpe_despues_borrar.
    SELECT p_eje AS Numero_eje, 'Declaracion del eje retirada.' AS Mensaje;
END$$


-- =============================================================================
-- F. LISTAS AUXILIARES PARA LOS DESPLEGABLES DEL APLICATIVO
-- =============================================================================

DROP PROCEDURE IF EXISTS SP_COMBO_EMPRESAS$$
CREATE PROCEDURE SP_COMBO_EMPRESAS()
BEGIN
    SELECT RUC_de_la_empresa, Nombre_empresa_transporte
    FROM   EMPRESA_TRANSPORTISTA
    WHERE  Estado = 'ACTIVO'
    ORDER  BY Nombre_empresa_transporte;
END$$


DROP PROCEDURE IF EXISTS SP_COMBO_VEHICULOS$$
CREATE PROCEDURE SP_COMBO_VEHICULOS()
BEGIN
    SELECT Placa_de_vehiculo, Descripcion_del_vehiculo, Peso_seco
    FROM   V_FLOTA_ACTIVA
    ORDER  BY Descripcion_del_vehiculo, Placa_de_vehiculo;
END$$


DROP PROCEDURE IF EXISTS SP_COMBO_BONIFICACIONES$$
CREATE PROCEDURE SP_COMBO_BONIFICACIONES()
BEGIN
    SELECT Placa_bonificacion, Peso_bruto_total_con_bonificacion
    FROM   BONIFICACION
    WHERE  Estado = 'ACTIVO'
    ORDER  BY Placa_bonificacion;
END$$


DROP PROCEDURE IF EXISTS SP_SIGUIENTE_NUMERO$$
CREATE PROCEDURE SP_SIGUIENTE_NUMERO()
BEGIN
    DECLARE v_max INT DEFAULT 1000000;

    SELECT IFNULL(MAX(CAST(SUBSTRING(Numero_autogenerado, 4) AS UNSIGNED)), 1000000)
    INTO   v_max
    FROM   FORMULARIO_AUTORIZACION;

    SELECT CONCAT('AS-', LPAD(v_max + 1, 7, '0')) AS Numero_sugerido;
END$$

DELIMITER ;
