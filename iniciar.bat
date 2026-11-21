@echo off
REM ===========================================================================
REM  PermiCarga Marietha - Inicio del aplicativo
REM
REM  Este archivo funciona desde cualquier carpeta y desde cualquier
REM  computadora, porque no contiene ninguna ruta fija: la variable %~dp0 es
REM  la carpeta donde esta este mismo archivo. Por eso puede copiarse la
REM  carpeta completa a otro equipo y seguira funcionando.
REM ===========================================================================
setlocal
cd /d "%~dp0"
title PermiCarga Marietha

echo.
echo   PermiCarga Marietha
echo   Gestion de permisos de carga sobredimensionada
echo   ---------------------------------------------
echo.

REM --- Comprobar que Java este instalado ---
java -version >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] No se encontro Java en esta computadora.
    echo.
    echo   El aplicativo necesita Java 17 o superior para funcionar.
    echo   Puede descargarlo sin costo desde:  https://adoptium.net
    echo.
    echo   Una vez instalado, vuelva a ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

REM --- Comprobar que el programa este presente ---
if not exist "PermiCargaMarietha.jar" (
    echo   [ERROR] No se encontro el archivo PermiCargaMarietha.jar
    echo.
    echo   Debe estar en esta misma carpeta:
    echo   %~dp0
    echo.
    pause
    exit /b 1
)

echo   Iniciando...
echo.
java -jar "PermiCargaMarietha.jar"

if errorlevel 1 (
    echo.
    echo   El programa termino con un error. Revise el mensaje anterior.
    echo.
    pause
)
endlocal
