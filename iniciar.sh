#!/bin/bash
# ===========================================================================
#  PermiCarga Marietha - Inicio del aplicativo (Linux y macOS)
#
#  Funciona desde cualquier carpeta: se situa en el directorio donde esta
#  este mismo archivo, de modo que no depende de rutas fijas.
# ===========================================================================
cd "$(dirname "$0")" || exit 1

echo
echo "  PermiCarga Marietha"
echo "  Gestion de permisos de carga sobredimensionada"
echo "  ---------------------------------------------"
echo

if ! command -v java >/dev/null 2>&1; then
    echo "  [ERROR] No se encontro Java en esta computadora."
    echo
    echo "  El aplicativo necesita Java 17 o superior."
    echo "  Puede descargarlo sin costo desde: https://adoptium.net"
    echo
    exit 1
fi

if [ ! -f "PermiCargaMarietha.jar" ]; then
    echo "  [ERROR] No se encontro PermiCargaMarietha.jar en:"
    echo "  $(pwd)"
    echo
    exit 1
fi

echo "  Iniciando..."
echo
exec java -jar PermiCargaMarietha.jar
