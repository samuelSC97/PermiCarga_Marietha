-- =============================================================================
-- PermiCarga Marietha
-- Script 05 de 06: Procedimientos de reporte
--
-- La consigna pide al menos diez reportes y que al menos seis de ellos resuelvan
-- con JOIN. Aqui se implementan quince, de los cuales trece enlazan dos o mas
-- tablas. Cada uno responde a una pregunta que el administrador de Marietha se
-- hace en su trabajo diario, no a un ejercicio de sintaxis.
--
--   Reporte                                          Tablas enlazadas   JOIN
--   01 Autorizaciones vigentes                       2                  SI
--   02 Autorizaciones proximas a vencer              2                  SI
--   03 Conjunto vehicular declarado                  3                  SI
--   04 Conjuntos que superan las 48 toneladas        3                  SI
--   05 Rutas mas transitadas                         2                  SI
--   06 Flota sin utilizar                            2                  SI
--   07 Resumen de permisos por empresa               2                  SI
--   08 Distribucion de peso por eje                  2                  SI
--   09 Traslados con apoyo policial                  3                  SI
--   10 Historial de traslados de una unidad          4                  SI
--   11 Aprovechamiento de las bonificaciones         2                  SI
--   12 Carga de trabajo mensual                      2                  SI
--   13 Destinos mas frecuentes                       1                  NO
--   14 Bitacora de auditoria                         1                  NO
--   15 Unidades dadas de baja y su historial         3                  SI
-- =============================================================================

USE marietha;

DELIMITER $$

-- -----------------------------------------------------------------------------
-- REPORTE 01: Autorizaciones vigentes a la fecha
-- Para que sirve: saber con que permisos puede circular hoy la empresa, sin
-- revisar formulario por formulario.
-- Enlaza: FORMULARIO_AUTORIZACION con EMPRESA_TRANSPORTISTA.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_01_VIGENTES$$
CREATE PROCEDURE SP_REP_01_VIGENTES()
BEGIN
    SELECT      fa.Numero_autogenerado                   AS 'Numero',
                e.Nombre_empresa_transporte              AS 'Empresa',
                fa.Origen_traslado                       AS 'Origen',
                fa.Destino_traslado                      AS 'Destino',
                fa.Vigencia_desde                        AS 'Vigente desde',
                fa.Vigencia_hasta                        AS 'Vigente hasta',
                DATEDIFF(fa.Vigencia_hasta, CURDATE())   AS 'Dias restantes'
    FROM        FORMULARIO_AUTORIZACION fa
    INNER JOIN  EMPRESA_TRANSPORTISTA   e
                ON e.RUC_de_la_empresa = fa.RUC_de_la_empresa
    WHERE       fa.Estado = 'ACTIVO'
      AND       CURDATE() BETWEEN fa.Vigencia_desde AND fa.Vigencia_hasta
    ORDER BY    fa.Vigencia_hasta;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 02: Autorizaciones proximas a vencer
-- Para que sirve: anticipar la renovacion y no quedarse sin permiso a mitad de
-- un contrato de transporte. El plazo es parametrico.
-- Enlaza: FORMULARIO_AUTORIZACION con EMPRESA_TRANSPORTISTA.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_02_POR_VENCER$$
CREATE PROCEDURE SP_REP_02_POR_VENCER(
    IN p_dias INT
)
BEGIN
    SELECT      fa.Numero_autogenerado                   AS 'Numero',
                e.Nombre_empresa_transporte              AS 'Empresa',
                fa.Destino_traslado                      AS 'Destino',
                fa.Vigencia_hasta                        AS 'Vence el',
                DATEDIFF(fa.Vigencia_hasta, CURDATE())   AS 'Dias restantes'
    FROM        FORMULARIO_AUTORIZACION fa
    INNER JOIN  EMPRESA_TRANSPORTISTA   e
                ON e.RUC_de_la_empresa = fa.RUC_de_la_empresa
    WHERE       fa.Estado = 'ACTIVO'
      AND       fa.Vigencia_hasta BETWEEN CURDATE()
                                      AND DATE_ADD(CURDATE(), INTERVAL IFNULL(p_dias, 7) DAY)
    ORDER BY    fa.Vigencia_hasta;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 03: Conjunto vehicular declarado en cada autorizacion
-- Para que sirve: verificar que unidades amparan cada permiso y cuanto pesa el
-- conjunto. Es la consulta que da sentido al modelo normalizado, porque el peso
-- de cada unidad se lee de VEHICULO y no de lo que se escribio en el formulario.
-- Enlaza: FORMULARIO_AUTORIZACION, FORMULARIO_VEHICULOS y VEHICULO.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_03_CONJUNTO_VEHICULAR$$
CREATE PROCEDURE SP_REP_03_CONJUNTO_VEHICULAR()
BEGIN
    SELECT      fa.Numero_autogenerado                            AS 'Numero',
                GROUP_CONCAT(v.Placa_de_vehiculo
                             ORDER BY v.Descripcion_del_vehiculo
                             SEPARATOR ' + ')                     AS 'Conjunto',
                COUNT(v.Placa_de_vehiculo)                        AS 'Unidades',
                SUM(v.Peso_seco)                                  AS 'Tara (kg)',
                fa.Peso_de_mercancia                              AS 'Mercancia (kg)',
                SUM(v.Peso_seco) + fa.Peso_de_mercancia           AS 'Peso total (kg)'
    FROM        FORMULARIO_AUTORIZACION fa
    INNER JOIN  FORMULARIO_VEHICULOS    fv
                ON fv.Numero_autogenerado = fa.Numero_autogenerado
    INNER JOIN  VEHICULO                v
                ON v.Placa_de_vehiculo = fv.Placa_de_vehiculo
    WHERE       fa.Estado = 'ACTIVO'
    GROUP BY    fa.Numero_autogenerado, fa.Peso_de_mercancia
    ORDER BY    SUM(v.Peso_seco) + fa.Peso_de_mercancia DESC;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 04: Conjuntos que superan las 48 toneladas
-- Para que sirve: identificar los traslados que obligan a declarar distribucion
-- por eje y a tramitar bonificacion por suspension neumatica.
-- Enlaza: FORMULARIO_AUTORIZACION, la vista del peso total y BONIFICACION.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_04_SOBRE_48_TONELADAS$$
CREATE PROCEDURE SP_REP_04_SOBRE_48_TONELADAS()
BEGIN
    SELECT      fa.Numero_autogenerado                      AS 'Numero',
                pt.Conjunto_vehicular                       AS 'Conjunto',
                pt.Peso_de_vehiculo_total                   AS 'Peso total (kg)',
                pt.Peso_de_vehiculo_total - 48000           AS 'Exceso sobre 48 t (kg)',
                IFNULL(b.Placa_bonificacion, 'Sin bonificacion')  AS 'Bonificacion',
                IFNULL(b.Peso_bruto_total_con_bonificacion, 48000) AS 'Tope permitido (kg)',
                CASE
                    WHEN b.Placa_bonificacion IS NULL THEN 'EXCEDE EL TOPE'
                    WHEN pt.Peso_de_vehiculo_total <= b.Peso_bruto_total_con_bonificacion
                        THEN 'Dentro del tope bonificado'
                    ELSE 'EXCEDE INCLUSO CON BONIFICACION'
                END                                         AS 'Situacion'
    FROM        FORMULARIO_AUTORIZACION    fa
    INNER JOIN  V_PESO_TOTAL_AUTORIZACION  pt
                ON pt.Numero_autogenerado = fa.Numero_autogenerado
    LEFT JOIN   BONIFICACION               b
                ON b.Placa_bonificacion = fa.Placa_bonificacion
    WHERE       fa.Estado = 'ACTIVO'
      AND       pt.Peso_de_vehiculo_total > 48000
    ORDER BY    pt.Peso_de_vehiculo_total DESC;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 05: Rutas mas transitadas
-- Para que sirve: saber en que corredores se concentra la operacion, que es el
-- insumo para negociar tarifas y para prever el desgaste de las unidades.
-- Enlaza: FORMULARIO_RUTAS con FORMULARIO_AUTORIZACION.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_05_RUTAS_TRANSITADAS$$
CREATE PROCEDURE SP_REP_05_RUTAS_TRANSITADAS()
BEGIN
    SELECT      fr.Ruta_a_transitar                       AS 'Tramo',
                COUNT(*)                                  AS 'Veces declarado',
                SUM(fa.Peso_de_mercancia)                 AS 'Carga acumulada (kg)',
                MIN(fa.Fecha_salida)                      AS 'Primer traslado',
                MAX(fa.Fecha_salida)                      AS 'Ultimo traslado'
    FROM        FORMULARIO_RUTAS        fr
    INNER JOIN  FORMULARIO_AUTORIZACION fa
                ON fa.Numero_autogenerado = fr.Numero_autogenerado
    WHERE       fa.Estado = 'ACTIVO'
    GROUP BY    fr.Ruta_a_transitar
    ORDER BY    COUNT(*) DESC, fr.Ruta_a_transitar;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 06: Unidades de la flota sin utilizar
-- Para que sirve: detectar capital inmovilizado. Una unidad registrada que nunca
-- se declaro en un permiso esta generando costo de mantenimiento sin producir.
-- Enlaza: VEHICULO con FORMULARIO_VEHICULOS mediante LEFT JOIN, que es lo que
-- permite ver justamente las filas que no tienen correspondencia.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_06_FLOTA_SIN_USO$$
CREATE PROCEDURE SP_REP_06_FLOTA_SIN_USO()
BEGIN
    SELECT      v.Placa_de_vehiculo                       AS 'Placa',
                v.Descripcion_del_vehiculo                AS 'Tipo',
                v.Marca_del_vehiculo                      AS 'Marca',
                v.Modelo_de_vehiculo                      AS 'Modelo',
                v.Peso_seco                               AS 'Tara (kg)',
                v.Estado                                  AS 'Estado'
    FROM        VEHICULO             v
    LEFT JOIN   FORMULARIO_VEHICULOS fv
                ON fv.Placa_de_vehiculo = v.Placa_de_vehiculo
    WHERE       fv.Placa_de_vehiculo IS NULL
    ORDER BY    v.Estado, v.Descripcion_del_vehiculo, v.Placa_de_vehiculo;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 07: Resumen de permisos por empresa
-- Para que sirve: comparar la actividad de cada transportista, util cuando la
-- base de datos da servicio a mas de una empresa.
-- Enlaza: EMPRESA_TRANSPORTISTA con FORMULARIO_AUTORIZACION.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_07_RESUMEN_POR_EMPRESA$$
CREATE PROCEDURE SP_REP_07_RESUMEN_POR_EMPRESA()
BEGIN
    SELECT      e.Nombre_empresa_transporte               AS 'Empresa',
                e.RUC_de_la_empresa                       AS 'RUC',
                COUNT(fa.Numero_autogenerado)             AS 'Permisos emitidos',
                SUM(CASE WHEN fa.Estado = 'ACTIVO'
                          AND CURDATE() BETWEEN fa.Vigencia_desde AND fa.Vigencia_hasta
                         THEN 1 ELSE 0 END)               AS 'Vigentes hoy',
                SUM(CASE WHEN fa.Estado = 'INACTIVO' THEN 1 ELSE 0 END) AS 'Anulados',
                IFNULL(SUM(fa.Peso_de_mercancia), 0)      AS 'Carga total (kg)',
                e.Estado                                  AS 'Estado'
    FROM        EMPRESA_TRANSPORTISTA   e
    LEFT JOIN   FORMULARIO_AUTORIZACION fa
                ON fa.RUC_de_la_empresa = e.RUC_de_la_empresa
    GROUP BY    e.RUC_de_la_empresa, e.Nombre_empresa_transporte, e.Estado
    ORDER BY    COUNT(fa.Numero_autogenerado) DESC;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 08: Distribucion de peso por eje
-- Para que sirve: comprobar que ningun eje supera el maximo legal antes de
-- presentarse al control de pesaje, que es donde se detectan las multas.
-- Enlaza: DISTRIBUCION_PESO_EJE con FORMULARIO_AUTORIZACION.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_08_PESO_POR_EJE$$
CREATE PROCEDURE SP_REP_08_PESO_POR_EJE(
    IN p_numero CHAR(10)
)
BEGIN
    SELECT      dpe.Numero_autogenerado                   AS 'Numero',
                dpe.Numero_eje                            AS 'Eje',
                dpe.Peso_eje                              AS 'Peso del eje (kg)',
                ROUND(100 * dpe.Peso_eje /
                      SUM(dpe.Peso_eje) OVER
                      (PARTITION BY dpe.Numero_autogenerado), 1) AS 'Porcentaje',
                fa.Destino_traslado                       AS 'Destino'
    FROM        DISTRIBUCION_PESO_EJE   dpe
    INNER JOIN  FORMULARIO_AUTORIZACION fa
                ON fa.Numero_autogenerado = dpe.Numero_autogenerado
    WHERE       p_numero IS NULL
       OR       p_numero = ''
       OR       dpe.Numero_autogenerado = p_numero
    ORDER BY    dpe.Numero_autogenerado, dpe.Numero_eje;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 09: Traslados que requieren apoyo policial
-- Para que sirve: coordinar la escolta con la autoridad con la debida
-- anticipacion, porque sin ella el traslado no puede iniciarse.
-- Enlaza: FORMULARIO_AUTORIZACION, EMPRESA_TRANSPORTISTA y la vista del peso.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_09_APOYO_POLICIAL$$
CREATE PROCEDURE SP_REP_09_APOYO_POLICIAL()
BEGIN
    SELECT      fa.Numero_autogenerado                    AS 'Numero',
                e.Nombre_empresa_transporte               AS 'Empresa',
                fa.Fecha_salida                           AS 'Salida',
                fa.Origen_traslado                        AS 'Origen',
                fa.Destino_traslado                       AS 'Destino',
                fa.Numero_vehiculos_apoyo                 AS 'Vehiculos guia',
                IFNULL(pt.Peso_de_vehiculo_total, 0)      AS 'Peso total (kg)'
    FROM        FORMULARIO_AUTORIZACION   fa
    INNER JOIN  EMPRESA_TRANSPORTISTA     e
                ON e.RUC_de_la_empresa = fa.RUC_de_la_empresa
    LEFT JOIN   V_PESO_TOTAL_AUTORIZACION pt
                ON pt.Numero_autogenerado = fa.Numero_autogenerado
    WHERE       fa.Apoyo_policial = TRUE
      AND       fa.Estado = 'ACTIVO'
    ORDER BY    fa.Fecha_salida DESC;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 10: Historial de traslados de una unidad
-- Para que sirve: reconstruir donde estuvo una placa y bajo que permiso, que es
-- lo primero que pide una fiscalizacion o un peritaje por siniestro.
-- Enlaza: VEHICULO, FORMULARIO_VEHICULOS, FORMULARIO_AUTORIZACION y EMPRESA.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_10_HISTORIAL_UNIDAD$$
CREATE PROCEDURE SP_REP_10_HISTORIAL_UNIDAD(
    IN p_placa CHAR(6)
)
BEGIN
    SELECT      v.Placa_de_vehiculo                       AS 'Placa',
                v.Descripcion_del_vehiculo                AS 'Tipo',
                fa.Numero_autogenerado                    AS 'Autorizacion',
                e.Nombre_empresa_transporte               AS 'Empresa',
                fa.Fecha_salida                           AS 'Salida',
                fa.Destino_traslado                       AS 'Destino',
                fa.Peso_de_mercancia                      AS 'Mercancia (kg)',
                fa.Estado                                 AS 'Estado del permiso'
    FROM        VEHICULO                v
    INNER JOIN  FORMULARIO_VEHICULOS    fv
                ON fv.Placa_de_vehiculo = v.Placa_de_vehiculo
    INNER JOIN  FORMULARIO_AUTORIZACION fa
                ON fa.Numero_autogenerado = fv.Numero_autogenerado
    INNER JOIN  EMPRESA_TRANSPORTISTA   e
                ON e.RUC_de_la_empresa = fa.RUC_de_la_empresa
    WHERE       p_placa IS NULL
       OR       p_placa = ''
       OR       v.Placa_de_vehiculo = p_placa
    ORDER BY    v.Placa_de_vehiculo, fa.Fecha_salida DESC;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 11: Aprovechamiento de las bonificaciones
-- Para que sirve: saber si las bonificaciones tramitadas se estan usando. Cada
-- una tiene un costo de tramite, y una bonificacion sin uso es dinero perdido.
-- Enlaza: BONIFICACION con FORMULARIO_AUTORIZACION mediante LEFT JOIN.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_11_BONIFICACIONES$$
CREATE PROCEDURE SP_REP_11_BONIFICACIONES()
BEGIN
    SELECT      b.Placa_bonificacion                      AS 'Placa',
                b.Permiso_de_bonificacion                 AS 'Permiso',
                b.Peso_bruto_total_con_bonificacion       AS 'Tope bonificado (kg)',
                COUNT(fa.Numero_autogenerado)             AS 'Veces utilizada',
                IFNULL(MAX(fa.Fecha_salida), '-')         AS 'Ultimo uso',
                b.Estado                                  AS 'Estado'
    FROM        BONIFICACION            b
    LEFT JOIN   FORMULARIO_AUTORIZACION fa
                ON fa.Placa_bonificacion = b.Placa_bonificacion
               AND fa.Estado = 'ACTIVO'
    GROUP BY    b.Placa_bonificacion, b.Permiso_de_bonificacion,
                b.Peso_bruto_total_con_bonificacion, b.Estado
    ORDER BY    COUNT(fa.Numero_autogenerado) DESC;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 12: Carga de trabajo mensual
-- Para que sirve: ver la estacionalidad del negocio y dimensionar la flota y el
-- personal administrativo para los meses de mayor demanda.
-- Enlaza: FORMULARIO_AUTORIZACION con EMPRESA_TRANSPORTISTA.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_12_CARGA_MENSUAL$$
CREATE PROCEDURE SP_REP_12_CARGA_MENSUAL()
BEGIN
    SELECT      DATE_FORMAT(fa.Fecha_salida, '%Y-%m')     AS 'Mes',
                COUNT(*)                                  AS 'Traslados',
                COUNT(DISTINCT fa.RUC_de_la_empresa)      AS 'Empresas',
                SUM(fa.Peso_de_mercancia)                 AS 'Carga total (kg)',
                ROUND(AVG(fa.Peso_de_mercancia))          AS 'Carga promedio (kg)',
                SUM(CASE WHEN fa.Tipo_de_transporte = 'Especial'
                         THEN 1 ELSE 0 END)               AS 'De tipo especial'
    FROM        FORMULARIO_AUTORIZACION fa
    INNER JOIN  EMPRESA_TRANSPORTISTA   e
                ON e.RUC_de_la_empresa = fa.RUC_de_la_empresa
    WHERE       fa.Estado = 'ACTIVO'
    GROUP BY    DATE_FORMAT(fa.Fecha_salida, '%Y-%m')
    ORDER BY    DATE_FORMAT(fa.Fecha_salida, '%Y-%m') DESC;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 13: Destinos mas frecuentes
-- Para que sirve: conocer los corredores habituales para planificar retornos con
-- carga y evitar viajes vacios.
-- Consulta sobre una sola tabla, con agrupacion.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_13_DESTINOS_FRECUENTES$$
CREATE PROCEDURE SP_REP_13_DESTINOS_FRECUENTES()
BEGIN
    SELECT      Destino_traslado                          AS 'Destino',
                COUNT(*)                                  AS 'Traslados',
                SUM(Peso_de_mercancia)                    AS 'Carga acumulada (kg)',
                ROUND(AVG(DATEDIFF(Fecha_llegada, Fecha_salida)), 1) AS 'Dias promedio',
                MAX(Fecha_salida)                         AS 'Ultimo traslado'
    FROM        FORMULARIO_AUTORIZACION
    WHERE       Estado = 'ACTIVO'
    GROUP BY    Destino_traslado
    ORDER BY    COUNT(*) DESC;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 14: Bitacora de auditoria
-- Para que sirve: saber quien registro, modifico o dio de baja cada dato y
-- cuando. Es el historial de nivel de registro descrito en el informe.
-- Consulta sobre la tabla que escriben los triggers.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_14_AUDITORIA$$
CREATE PROCEDURE SP_REP_14_AUDITORIA(
    IN p_tabla VARCHAR(40),
    IN p_desde DATE
)
BEGIN
    SELECT      Fecha_hora                                AS 'Fecha y hora',
                Tabla_afectada                            AS 'Tabla',
                Operacion                                 AS 'Operacion',
                Clave_afectada                            AS 'Registro',
                Detalle                                   AS 'Detalle',
                Usuario_bd                                AS 'Cuenta'
    FROM        AUDITORIA
    WHERE       (p_tabla IS NULL OR p_tabla = '' OR Tabla_afectada = p_tabla)
      AND       (p_desde IS NULL OR DATE(Fecha_hora) >= p_desde)
    ORDER BY    Fecha_hora DESC
    LIMIT       500;
END$$


-- -----------------------------------------------------------------------------
-- REPORTE 15: Unidades dadas de baja y su historial
-- Para que sirve: comprobar que al retirar una unidad de la flota no se perdio
-- su historia. Es la demostracion de que el borrado logico cumple su proposito.
-- Enlaza: VEHICULO, FORMULARIO_VEHICULOS y FORMULARIO_AUTORIZACION.
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REP_15_BAJAS_CON_HISTORIAL$$
CREATE PROCEDURE SP_REP_15_BAJAS_CON_HISTORIAL()
BEGIN
    SELECT      v.Placa_de_vehiculo                       AS 'Placa',
                v.Descripcion_del_vehiculo                AS 'Tipo',
                v.Marca_del_vehiculo                      AS 'Marca',
                COUNT(fa.Numero_autogenerado)             AS 'Traslados en su historial',
                IFNULL(MIN(fa.Fecha_salida), '-')         AS 'Primer traslado',
                IFNULL(MAX(fa.Fecha_salida), '-')         AS 'Ultimo traslado'
    FROM        VEHICULO                v
    LEFT JOIN   FORMULARIO_VEHICULOS    fv
                ON fv.Placa_de_vehiculo = v.Placa_de_vehiculo
    LEFT JOIN   FORMULARIO_AUTORIZACION fa
                ON fa.Numero_autogenerado = fv.Numero_autogenerado
    WHERE       v.Estado = 'INACTIVO'
    GROUP BY    v.Placa_de_vehiculo, v.Descripcion_del_vehiculo, v.Marca_del_vehiculo
    ORDER BY    COUNT(fa.Numero_autogenerado) DESC;
END$$

DELIMITER ;
