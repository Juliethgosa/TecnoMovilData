#!/usr/bin/env bash
# Compila y ejecuta TecnoMovil Data (requiere JDK 17 o superior).
# Uso: ./ejecutar.sh [numero_de_usuarios]
set -e
rm -rf out && mkdir out
javac --release 17 -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -cp out co.edu.iudigital.tecnomovil.Main "$@"
