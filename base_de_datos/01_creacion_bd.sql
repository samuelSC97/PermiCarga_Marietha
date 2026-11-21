-- =============================================================================
-- PermiCarga Marietha
-- Script 01 de 05: Creacion de la base de datos
--
-- Proyecto : Desarrollo de la base de datos para la funcionalidad de Solicitud
--            de Permiso de Carga Sobredimensionada de la empresa Marietha
-- Curso    : Gestion de Datos e Informacion - Universidad Catolica de Santa Maria
-- Gestor   : MySQL 8.0.16 o superior / MariaDB 10.5 o superior
--
-- Este script crea el esquema fisico definido en la Entrega 4 y lo amplia con
-- los elementos que la Entrega 5 exige para el aplicativo cliente:
--   a) La columna Estado en las tablas maestras, que habilita la baja logica.
--   b) La tabla USUARIO, que da acceso al aplicativo mediante perfiles.
--   c) La tabla AUDITORIA, donde los triggers registran cada operacion.
--
-- Orden de ejecucion: 01 -> 02 -> 03 -> 04 -> 05
-- =============================================================================

DROP DATABASE IF EXISTS marietha;

CREATE DATABASE marietha
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE marietha;


-- -----------------------------------------------------------------------------
-- 1. EMPRESA_TRANSPORTISTA
-- Empresa que solicita el permiso ante Provias Nacional.
-- -----------------------------------------------------------------------------
CREATE TABLE EMPRESA_TRANSPORTISTA (
    RUC_de_la_empresa          CHAR(11)      NOT NULL
        COMMENT 'RUC que identifica de forma unica a la empresa transportista',
    Nombre_empresa_transporte  VARCHAR(100)  NOT NULL
        COMMENT 'Razon social de la empresa transportista',
    Representante              VARCHAR(80)   NOT NULL
        COMMENT 'Nombre completo del representante legal',
    Domicilio                  VARCHAR(150)  NOT NULL
        COMMENT 'Direccion legal registrada de la empresa',
    Registro_MTC               VARCHAR(15)   NOT NULL
        COMMENT 'Codigo de registro del transportista ante el MTC',
    Telefono                   CHAR(9)       NULL
        COMMENT 'Numero de contacto de la empresa',
    Estado                     ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO'
        COMMENT 'Baja logica: INACTIVO oculta el registro sin borrarlo',

    CONSTRAINT PK_empresa_transportista PRIMARY KEY (RUC_de_la_empresa),
    CONSTRAINT CHK_emp_ruc      CHECK (RUC_de_la_empresa REGEXP '^[0-9]{11}$'),
    CONSTRAINT CHK_emp_mtc      CHECK (Registro_MTC      REGEXP '^[0-9]{8}[A-Za-z]{3}$'),
    CONSTRAINT CHK_emp_telefono CHECK (Telefono IS NULL OR Telefono REGEXP '^[0-9]{9}$'),

    INDEX IDX_registro_mtc (Registro_MTC),
    INDEX IDX_emp_estado   (Estado)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Datos registrales de la empresa que solicita el permiso';


-- -----------------------------------------------------------------------------
-- 2. BONIFICACION
-- Bonificacion de peso por suspension neumatica, otorgada por placa.
-- -----------------------------------------------------------------------------
CREATE TABLE BONIFICACION (
    Placa_bonificacion                 CHAR(6)             NOT NULL
        COMMENT 'Placa del vehiculo con bonificacion por suspension neumatica',
    Permiso_de_bonificacion            CHAR(10)            NOT NULL
        COMMENT 'Numero de permiso que acredita la bonificacion de peso',
    Peso_bruto_total_con_bonificacion  MEDIUMINT UNSIGNED  NOT NULL
        COMMENT 'Peso bruto total permitido con bonificacion, en kilogramos',
    Estado                             ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO'
        COMMENT 'Baja logica: INACTIVO oculta el registro sin borrarlo',

    CONSTRAINT PK_bonificacion PRIMARY KEY (Placa_bonificacion),
    CONSTRAINT CHK_bon_placa   CHECK (Placa_bonificacion      REGEXP '^[A-Za-z0-9]{6}$'),
    CONSTRAINT CHK_bon_permiso CHECK (Permiso_de_bonificacion REGEXP '^AS-[0-9]{7}$'),
    CONSTRAINT CHK_bon_peso    CHECK (Peso_bruto_total_con_bonificacion > 48000),

    INDEX IDX_bon_estado (Estado)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Bonificacion de peso por suspension neumatica, por placa';


-- -----------------------------------------------------------------------------
-- 3. VEHICULO
-- Ficha tecnica de cada unidad de la flota. El peso seco se guarda una sola vez
-- en esta tabla, que es lo que impide la inconsistencia que origino el proyecto.
-- -----------------------------------------------------------------------------
CREATE TABLE VEHICULO (
    Placa_de_vehiculo         CHAR(6)             NOT NULL
        COMMENT 'Placa que identifica de forma unica a la unidad vehicular',
    Descripcion_del_vehiculo  ENUM('Tracto','Cama baja','Semirremolque') NOT NULL
        COMMENT 'Tipo de unidad vehicular declarada',
    Marca_del_vehiculo        VARCHAR(30)         NOT NULL
        COMMENT 'Marca del fabricante de la unidad',
    Modelo_de_vehiculo        VARCHAR(30)         NOT NULL
        COMMENT 'Modelo de la unidad vehicular',
    Largo_de_vehiculo         DECIMAL(5,2)        NOT NULL
        COMMENT 'Longitud de la unidad sin carga, en metros',
    Ancho_de_vehiculo         DECIMAL(4,2)        NOT NULL
        COMMENT 'Ancho de la unidad sin carga, en metros',
    Alto_de_vehiculo          DECIMAL(4,2)        NOT NULL
        COMMENT 'Altura de la unidad sin carga, en metros',
    Numero_de_ejes            TINYINT UNSIGNED    NOT NULL
        COMMENT 'Cantidad de ejes de la unidad',
    Numero_de_neumaticos      TINYINT UNSIGNED    NOT NULL
        COMMENT 'Cantidad de neumaticos de la unidad',
    Peso_seco                 MEDIUMINT UNSIGNED  NOT NULL
        COMMENT 'Peso de la unidad sin carga (tara), en kilogramos',
    Estado                    ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO'
        COMMENT 'Baja logica: INACTIVO retira la unidad de la flota operativa',

    CONSTRAINT PK_vehiculo    PRIMARY KEY (Placa_de_vehiculo),
    CONSTRAINT CHK_veh_placa  CHECK (Placa_de_vehiculo REGEXP '^[A-Za-z0-9]{6}$'),
    CONSTRAINT CHK_veh_largo  CHECK (Largo_de_vehiculo    > 0),
    CONSTRAINT CHK_veh_ancho  CHECK (Ancho_de_vehiculo    > 0),
    CONSTRAINT CHK_veh_alto   CHECK (Alto_de_vehiculo     > 0),
    CONSTRAINT CHK_veh_ejes   CHECK (Numero_de_ejes       BETWEEN 1 AND 7),
    CONSTRAINT CHK_veh_neum   CHECK (Numero_de_neumaticos > 0),
    CONSTRAINT CHK_veh_peso   CHECK (Peso_seco            > 0),

    INDEX IDX_descripcion_vehiculo (Descripcion_del_vehiculo),
    INDEX IDX_veh_estado           (Estado)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Ficha tecnica de cada unidad vehicular de la flota';


-- -----------------------------------------------------------------------------
-- 4. FORMULARIO_AUTORIZACION
-- Autorizacion de circulacion emitida por Provias Nacional. Es la tabla central.
-- -----------------------------------------------------------------------------
CREATE TABLE FORMULARIO_AUTORIZACION (
    Numero_autogenerado             CHAR(10)            NOT NULL
        COMMENT 'Codigo unico de la autorizacion emitida por Provias Nacional',
    RUC_de_la_empresa               CHAR(11)            NOT NULL
        COMMENT 'Empresa que solicita el permiso. FK a EMPRESA_TRANSPORTISTA',
    Firma_del_transportista         MEDIUMBLOB          NOT NULL
        COMMENT 'Firma del representante que valida la solicitud',
    Origen_traslado                 VARCHAR(150)        NOT NULL
        COMMENT 'Punto de partida de la carga, con la referencia de la via',
    Destino_traslado                VARCHAR(150)        NOT NULL
        COMMENT 'Punto de llegada de la carga, con la referencia de la via',
    Fecha_salida                    DATE                NOT NULL
        COMMENT 'Fecha en que inicia el traslado',
    Fecha_llegada                   DATE                NOT NULL
        COMMENT 'Fecha en que finaliza el traslado',
    Descripcion_de_la_mercancia     VARCHAR(200)        NOT NULL
        COMMENT 'Descripcion de la maquinaria trasladada, o la palabra Vacio',
    Largo_de_mercancia              DECIMAL(5,2)        NOT NULL DEFAULT 0.00
        COMMENT 'Longitud de la maquinaria transportada, en metros',
    Ancho_de_mercancia              DECIMAL(5,2)        NOT NULL DEFAULT 0.00
        COMMENT 'Ancho de la maquinaria transportada, en metros',
    Alto_de_mercancia               DECIMAL(5,2)        NOT NULL DEFAULT 0.00
        COMMENT 'Altura de la maquinaria transportada, en metros',
    Peso_de_mercancia               MEDIUMINT UNSIGNED  NOT NULL DEFAULT 0
        COMMENT 'Peso de la maquinaria transportada, en kilogramos',
    Tipo_de_transporte              ENUM('Normal','Especial') NOT NULL DEFAULT 'Normal'
        COMMENT 'Clasificacion del transporte declarado en la solicitud',
    Exceso_anterior                 DECIMAL(4,2)        NOT NULL DEFAULT 0.00
        COMMENT 'Exceso de carga en la parte delantera, en metros',
    Exceso_posterior                DECIMAL(4,2)        NOT NULL DEFAULT 0.00
        COMMENT 'Exceso de carga en la parte posterior, en metros',
    Largo_total_vehiculo            DECIMAL(5,2)        NOT NULL
        COMMENT 'Longitud total del conjunto vehicular ya cargado, en metros',
    Ancho_total_vehiculo            DECIMAL(4,2)        NOT NULL
        COMMENT 'Ancho total del conjunto vehicular ya cargado, en metros',
    Alto_total_vehiculo             DECIMAL(4,2)        NOT NULL
        COMMENT 'Altura total del conjunto vehicular ya cargado, en metros',
    Numero_vehiculos_apoyo          TINYINT UNSIGNED    NOT NULL DEFAULT 0
        COMMENT 'Cantidad de vehiculos guia asignados al traslado',
    Apoyo_policial                  BOOLEAN             NOT NULL DEFAULT FALSE
        COMMENT 'Indica si el traslado requiere escolta policial',
    Placa_bonificacion              CHAR(6)             NULL
        COMMENT 'Placa con bonificacion por suspension neumatica. FK a BONIFICACION',
    Fecha_aprobacion                DATE                NOT NULL DEFAULT (CURRENT_DATE)
        COMMENT 'Fecha en que el sistema emite la autorizacion',
    Hora_aprobacion                 TIME                NOT NULL DEFAULT (CURRENT_TIME)
        COMMENT 'Hora en que el sistema emite la autorizacion',
    Horario_de_autorizacion_valida  VARCHAR(30)         NOT NULL
                                    DEFAULT 'De 06:00 am a 06:00 pm'
        COMMENT 'Franja horaria diaria en que el permiso habilita la circulacion',
    Vigencia_desde                  DATE                NOT NULL
        COMMENT 'Fecha de inicio de vigencia de la autorizacion',
    Vigencia_hasta                  DATE                NOT NULL
        COMMENT 'Fecha de termino de vigencia de la autorizacion',
    Firma_y_sello_unidad_pesajes    MEDIUMBLOB          NOT NULL
        COMMENT 'Visto bueno y sello del funcionario de la Unidad de Pesajes',
    Estado                          ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO'
        COMMENT 'Baja logica: INACTIVO anula la autorizacion sin borrar su historia',

    CONSTRAINT PK_formulario_autorizacion PRIMARY KEY (Numero_autogenerado),

    CONSTRAINT CHK_fa_numero    CHECK (Numero_autogenerado REGEXP '^AS-[0-9]{7}$'),
    CONSTRAINT CHK_fa_fechas    CHECK (Fecha_llegada  >= Fecha_salida),
    CONSTRAINT CHK_fa_vigencia  CHECK (Vigencia_hasta >= Vigencia_desde),
    CONSTRAINT CHK_fa_merc_lar  CHECK (Largo_de_mercancia   >= 0),
    CONSTRAINT CHK_fa_merc_anc  CHECK (Ancho_de_mercancia   >= 0),
    CONSTRAINT CHK_fa_merc_alt  CHECK (Alto_de_mercancia    >= 0),
    CONSTRAINT CHK_fa_exc_ant   CHECK (Exceso_anterior      >= 0),
    CONSTRAINT CHK_fa_exc_pos   CHECK (Exceso_posterior     >= 0),
    CONSTRAINT CHK_fa_tot_lar   CHECK (Largo_total_vehiculo  > 0),
    CONSTRAINT CHK_fa_tot_anc   CHECK (Ancho_total_vehiculo  > 0),
    CONSTRAINT CHK_fa_tot_alt   CHECK (Alto_total_vehiculo   > 0),

    CONSTRAINT FK_fa_empresa FOREIGN KEY (RUC_de_la_empresa)
        REFERENCES EMPRESA_TRANSPORTISTA (RUC_de_la_empresa)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT FK_fa_bonificacion FOREIGN KEY (Placa_bonificacion)
        REFERENCES BONIFICACION (Placa_bonificacion)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    INDEX IDX_ruc_empresa        (RUC_de_la_empresa),
    INDEX IDX_placa_bonificacion (Placa_bonificacion),
    INDEX IDX_vigencia           (Vigencia_desde, Vigencia_hasta),
    INDEX IDX_fecha_salida       (Fecha_salida),
    INDEX IDX_destino            (Destino_traslado(40)),
    INDEX IDX_fa_estado          (Estado)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Autorizacion de circulacion emitida por Provias Nacional';


-- -----------------------------------------------------------------------------
-- 5. FORMULARIO_VEHICULOS
-- Unidades declaradas en cada autorizacion. Tabla de relacion: sus filas siguen
-- el estado de la autorizacion a la que pertenecen, por eso no lleva Estado.
-- -----------------------------------------------------------------------------
CREATE TABLE FORMULARIO_VEHICULOS (
    Numero_autogenerado  CHAR(10)  NOT NULL
        COMMENT 'Autorizacion a la que pertenece el vehiculo. FK',
    Placa_de_vehiculo    CHAR(6)   NOT NULL
        COMMENT 'Vehiculo declarado en esa autorizacion. FK',

    CONSTRAINT PK_formulario_vehiculos
        PRIMARY KEY (Numero_autogenerado, Placa_de_vehiculo),

    CONSTRAINT FK_fv_autorizacion FOREIGN KEY (Numero_autogenerado)
        REFERENCES FORMULARIO_AUTORIZACION (Numero_autogenerado)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT FK_fv_vehiculo FOREIGN KEY (Placa_de_vehiculo)
        REFERENCES VEHICULO (Placa_de_vehiculo)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    INDEX IDX_fv_placa (Placa_de_vehiculo)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Unidades vehiculares declaradas en cada autorizacion';


-- -----------------------------------------------------------------------------
-- 6. FORMULARIO_RUTAS
-- -----------------------------------------------------------------------------
CREATE TABLE FORMULARIO_RUTAS (
    Numero_autogenerado  CHAR(10)     NOT NULL
        COMMENT 'Autorizacion a la que pertenece el tramo. FK',
    Ruta_a_transitar     VARCHAR(40)  NOT NULL
        COMMENT 'Tramo de ruta nacional declarado, formato Departamento-CodigoRuta',

    CONSTRAINT PK_formulario_rutas
        PRIMARY KEY (Numero_autogenerado, Ruta_a_transitar),

    CONSTRAINT FK_fr_autorizacion FOREIGN KEY (Numero_autogenerado)
        REFERENCES FORMULARIO_AUTORIZACION (Numero_autogenerado)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    INDEX IDX_ruta (Ruta_a_transitar)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Tramos de ruta nacional declarados en cada autorizacion';


-- -----------------------------------------------------------------------------
-- 7. DISTRIBUCION_PESO_EJE
-- -----------------------------------------------------------------------------
CREATE TABLE DISTRIBUCION_PESO_EJE (
    Numero_autogenerado  CHAR(10)            NOT NULL
        COMMENT 'Autorizacion a la que pertenece el eje. FK',
    Numero_eje           TINYINT UNSIGNED    NOT NULL
        COMMENT 'Numero de orden del eje dentro del conjunto vehicular',
    Peso_eje             MEDIUMINT UNSIGNED  NOT NULL
        COMMENT 'Peso declarado para ese eje, en kilogramos',

    CONSTRAINT PK_distribucion_peso_eje
        PRIMARY KEY (Numero_autogenerado, Numero_eje),
    CONSTRAINT CHK_dpe_numero CHECK (Numero_eje BETWEEN 1 AND 7),
    CONSTRAINT CHK_dpe_peso   CHECK (Peso_eje   > 0),

    CONSTRAINT FK_dpe_autorizacion FOREIGN KEY (Numero_autogenerado)
        REFERENCES FORMULARIO_AUTORIZACION (Numero_autogenerado)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Peso declarado por cada eje, solo cuando se supera 48000 kg';


-- =============================================================================
-- TABLAS QUE INCORPORA LA ENTREGA 5 PARA SOSTENER AL APLICATIVO
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 8. USUARIO
-- Da acceso al aplicativo. La clave no se guarda en texto plano: se almacena el
-- resumen SHA-256 que devuelve la funcion SHA2, de modo que ni el administrador
-- de la base de datos puede leerla.
-- -----------------------------------------------------------------------------
CREATE TABLE USUARIO (
    Id_usuario       INT UNSIGNED  NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno del usuario',
    Usuario          VARCHAR(20)   NOT NULL
        COMMENT 'Nombre con el que la persona ingresa al aplicativo',
    Clave            CHAR(64)      NOT NULL
        COMMENT 'Resumen SHA-256 de la contrasena, nunca la contrasena en claro',
    Nombre_completo  VARCHAR(80)   NOT NULL
        COMMENT 'Nombre de la persona que usa esa cuenta',
    Perfil           ENUM('ADMINISTRADOR','OPERADOR','CONSULTA') NOT NULL
        COMMENT 'Permisos de la cuenta dentro del aplicativo',
    Ultimo_acceso    DATETIME      NULL
        COMMENT 'Fecha y hora del ultimo ingreso correcto',
    Estado           ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO'
        COMMENT 'Baja logica: INACTIVO impide el ingreso sin borrar la cuenta',

    CONSTRAINT PK_usuario PRIMARY KEY (Id_usuario),
    CONSTRAINT UQ_usuario_nombre UNIQUE (Usuario),
    CONSTRAINT CHK_usr_nombre CHECK (CHAR_LENGTH(Usuario) >= 4)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Cuentas de acceso al aplicativo, con su perfil de permisos';


-- -----------------------------------------------------------------------------
-- 9. AUDITORIA
-- Bitacora que escriben los triggers. Registra quien hizo que y cuando sobre
-- cada tabla maestra, que es el historial de nivel de registro descrito en la
-- seccion de gestion del almacenamiento del informe.
-- -----------------------------------------------------------------------------
CREATE TABLE AUDITORIA (
    Id_auditoria     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador correlativo del movimiento registrado',
    Tabla_afectada   VARCHAR(40)     NOT NULL
        COMMENT 'Nombre de la tabla sobre la que se ejecuto la operacion',
    Operacion        ENUM('INSERCION','MODIFICACION','BAJA','REACTIVACION') NOT NULL
        COMMENT 'Tipo de operacion ejecutada',
    Clave_afectada   VARCHAR(40)     NOT NULL
        COMMENT 'Valor de la clave primaria del registro afectado',
    Detalle          VARCHAR(255)    NULL
        COMMENT 'Descripcion legible de lo que cambio',
    Usuario_bd       VARCHAR(80)     NOT NULL DEFAULT (CURRENT_USER())
        COMMENT 'Cuenta de base de datos que ejecuto la operacion',
    Fecha_hora       DATETIME        NOT NULL DEFAULT (CURRENT_TIMESTAMP)
        COMMENT 'Momento exacto en que se registro el movimiento',

    CONSTRAINT PK_auditoria PRIMARY KEY (Id_auditoria),

    INDEX IDX_aud_tabla  (Tabla_afectada),
    INDEX IDX_aud_fecha  (Fecha_hora),
    INDEX IDX_aud_clave  (Clave_afectada)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Bitacora de operaciones escrita automaticamente por los triggers';
