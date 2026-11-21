-- =============================================================================
-- PermiCarga Marietha
-- Script 03 de 05: Vistas
--
-- Las vistas concentran los enlaces entre tablas que el aplicativo necesita una
-- y otra vez. Al dejarlos resueltos aqui, los procedimientos de reporte quedan
-- mas cortos y, sobre todo, la regla de negocio se escribe una sola vez: si
-- manana cambia la forma de calcular el peso total, se corrige en la vista y no
-- en cada consulta del aplicativo.
-- =============================================================================

USE marietha;


-- -----------------------------------------------------------------------------
-- V_PESO_TOTAL_AUTORIZACION
-- Resuelve el atributo derivado Peso de vehiculo (total). No pudo implementarse
-- como columna generada porque su calculo cruza tres tablas: la autorizacion
-- aporta el peso de la mercancia y las unidades declaradas aportan su tara.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW V_PESO_TOTAL_AUTORIZACION AS
SELECT  fa.Numero_autogenerado,
        SUM(v.Peso_seco)                                AS Peso_seco_conjunto,
        fa.Peso_de_mercancia,
        SUM(v.Peso_seco) + fa.Peso_de_mercancia         AS Peso_de_vehiculo_total,
        COUNT(v.Placa_de_vehiculo)                      AS Unidades_declaradas,
        GROUP_CONCAT(v.Placa_de_vehiculo
                     ORDER BY v.Descripcion_del_vehiculo
                     SEPARATOR ' + ')                   AS Conjunto_vehicular
FROM        FORMULARIO_AUTORIZACION fa
INNER JOIN  FORMULARIO_VEHICULOS    fv ON fv.Numero_autogenerado = fa.Numero_autogenerado
INNER JOIN  VEHICULO                v  ON v.Placa_de_vehiculo    = fv.Placa_de_vehiculo
GROUP BY    fa.Numero_autogenerado, fa.Peso_de_mercancia;


-- -----------------------------------------------------------------------------
-- V_AUTORIZACION_VIGENCIA
-- Clasifica cada autorizacion segun donde cae la fecha de hoy dentro de su
-- periodo de vigencia. El aplicativo pinta esta columna en la rejilla principal,
-- de modo que el administrador ve de un vistazo con que permisos puede circular.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW V_AUTORIZACION_VIGENCIA AS
SELECT  fa.Numero_autogenerado,
        fa.RUC_de_la_empresa,
        e.Nombre_empresa_transporte,
        fa.Origen_traslado,
        fa.Destino_traslado,
        fa.Fecha_salida,
        fa.Fecha_llegada,
        fa.Tipo_de_transporte,
        fa.Peso_de_mercancia,
        fa.Vigencia_desde,
        fa.Vigencia_hasta,
        DATEDIFF(fa.Vigencia_hasta, CURDATE())          AS Dias_restantes,
        CASE
            WHEN fa.Estado = 'INACTIVO'                 THEN 'ANULADA'
            WHEN CURDATE() <  fa.Vigencia_desde         THEN 'PROGRAMADA'
            WHEN CURDATE() >  fa.Vigencia_hasta         THEN 'VENCIDA'
            WHEN DATEDIFF(fa.Vigencia_hasta, CURDATE()) <= 7 THEN 'POR VENCER'
            ELSE 'VIGENTE'
        END                                             AS Situacion,
        fa.Estado
FROM        FORMULARIO_AUTORIZACION fa
INNER JOIN  EMPRESA_TRANSPORTISTA   e ON e.RUC_de_la_empresa = fa.RUC_de_la_empresa;


-- -----------------------------------------------------------------------------
-- V_AUTORIZACION_COMPLETA
-- Rejilla principal del aplicativo: une la autorizacion con su empresa, su
-- conjunto vehicular y su peso total ya calculado, de modo que la pantalla de
-- consulta se alimenta de una sola lectura.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW V_AUTORIZACION_COMPLETA AS
SELECT  av.Numero_autogenerado,
        av.Nombre_empresa_transporte,
        av.Origen_traslado,
        av.Destino_traslado,
        av.Fecha_salida,
        av.Fecha_llegada,
        av.Tipo_de_transporte,
        pt.Conjunto_vehicular,
        pt.Peso_seco_conjunto,
        av.Peso_de_mercancia,
        pt.Peso_de_vehiculo_total,
        av.Vigencia_desde,
        av.Vigencia_hasta,
        av.Situacion,
        av.Estado
FROM        V_AUTORIZACION_VIGENCIA        av
LEFT JOIN   V_PESO_TOTAL_AUTORIZACION      pt
            ON pt.Numero_autogenerado = av.Numero_autogenerado;


-- -----------------------------------------------------------------------------
-- V_FLOTA_ACTIVA
-- Unidades disponibles para declararse en una autorizacion nueva, con el numero
-- de traslados en que ya participaron. El aplicativo la usa para llenar los
-- desplegables, de modo que una unidad dada de baja no puede volver a elegirse.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW V_FLOTA_ACTIVA AS
SELECT  v.Placa_de_vehiculo,
        v.Descripcion_del_vehiculo,
        v.Marca_del_vehiculo,
        v.Modelo_de_vehiculo,
        v.Numero_de_ejes,
        v.Peso_seco,
        COUNT(fv.Numero_autogenerado)                   AS Traslados_realizados,
        v.Estado
FROM        VEHICULO             v
LEFT JOIN   FORMULARIO_VEHICULOS fv ON fv.Placa_de_vehiculo = v.Placa_de_vehiculo
WHERE       v.Estado = 'ACTIVO'
GROUP BY    v.Placa_de_vehiculo, v.Descripcion_del_vehiculo, v.Marca_del_vehiculo,
            v.Modelo_de_vehiculo, v.Numero_de_ejes, v.Peso_seco, v.Estado;
