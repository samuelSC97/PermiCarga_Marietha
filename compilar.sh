#!/bin/bash
# =============================================================================
# PermiCarga Marietha - Compilacion del aplicativo
#
# Genera PermiCargaMarietha.jar, un archivo unico y autonomo que incluye el
# controlador JDBC, de modo que se ejecute en cualquier computadora que tenga
# Java instalado sin necesidad de instalar nada mas.
#
# Uso:   ./compilar.sh
# =============================================================================
set -e
cd "$(dirname "$0")"

echo "1. Limpiando compilaciones anteriores"
rm -rf clases fat PermiCargaMarietha.jar
mkdir -p clases fat

echo "2. Compilando el codigo fuente"
find src -name "*.java" > fuentes.txt
javac -encoding UTF-8 -Xlint:-serial -d clases -cp "lib/*" @fuentes.txt
echo "   $(find clases -name '*.class' | wc -l) clases generadas"

echo "3. Incorporando el controlador JDBC"
cd fat
unzip -oq ../lib/mariadb-java-client-*.jar
rm -rf META-INF/*.SF META-INF/*.DSA META-INF/*.RSA module-info.class 2>/dev/null || true
cd ..

echo "4. Empaquetando"
cp -r clases/* fat/
cp config.properties fat/
cat > manifiesto.txt <<'EOF'
Main-Class: pe.edu.ucsm.gdi.permicarga.Main
Implementation-Title: PermiCarga Marietha
Implementation-Version: 1.0
Class-Path: lib/mysql-connector-j.jar lib/
EOF
jar cfm PermiCargaMarietha.jar manifiesto.txt -C fat .
rm -rf fat fuentes.txt manifiesto.txt

echo
echo "Listo: $(ls -lh PermiCargaMarietha.jar | awk '{print $5}')  PermiCargaMarietha.jar"
echo "Ejecutar con:  java -jar PermiCargaMarietha.jar"
