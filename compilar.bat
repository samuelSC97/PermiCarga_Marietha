@echo off
REM ===========================================================================
REM  PermiCarga Marietha - Compilacion del aplicativo (Windows)
REM
REM  Genera PermiCargaMarietha.jar, un archivo unico y autonomo que incluye el
REM  controlador JDBC, de modo que se ejecute en cualquier computadora con Java
REM  sin instalar nada mas.
REM
REM  Requiere el JDK (no solo el JRE), porque usa javac y jar.
REM ===========================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo  1. Limpiando compilaciones anteriores
if exist clases rmdir /s /q clases
if exist fat    rmdir /s /q fat
if exist PermiCargaMarietha.jar del /q PermiCargaMarietha.jar
mkdir clases
mkdir fat

echo  2. Compilando el codigo fuente
if exist fuentes.txt del /q fuentes.txt
for /r src %%f in (*.java) do echo %%f>> fuentes.txt
javac -encoding UTF-8 -Xlint:-serial -d clases -cp "lib/*" @fuentes.txt
if errorlevel 1 (
    echo.
    echo  [ERROR] La compilacion fallo. Revise los mensajes anteriores.
    pause
    exit /b 1
)

echo  3. Incorporando el controlador JDBC
cd fat
for %%j in (..\lib\mariadb-java-client-*.jar) do jar xf "%%j"
if exist META-INF\*.SF  del /q META-INF\*.SF
if exist META-INF\*.DSA del /q META-INF\*.DSA
if exist META-INF\*.RSA del /q META-INF\*.RSA
if exist module-info.class del /q module-info.class
cd ..

echo  4. Empaquetando
xcopy /e /i /y /q clases\* fat\ >nul
copy /y config.properties fat\ >nul
(
echo Main-Class: pe.edu.ucsm.gdi.permicarga.Main
echo Implementation-Title: PermiCarga Marietha
echo Implementation-Version: 1.0
echo Class-Path: lib/mysql-connector-j.jar lib/
) > manifiesto.txt
jar cfm PermiCargaMarietha.jar manifiesto.txt -C fat .

rmdir /s /q fat
del /q fuentes.txt manifiesto.txt

echo.
echo  Listo: PermiCargaMarietha.jar
echo  Ejecutar con:  java -jar PermiCargaMarietha.jar
echo.
pause
endlocal
