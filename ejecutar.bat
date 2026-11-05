@echo off
chcp 65001 >nul
rem Compila y ejecuta TecnoMovil Data (requiere JDK 17 o superior).
rem Uso: ejecutar.bat [numero_de_usuarios]
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fuentes.txt
javac --release 17 -encoding UTF-8 -d out @fuentes.txt
if errorlevel 1 exit /b 1
del fuentes.txt
java -Dfile.encoding=UTF-8 -cp out co.edu.iudigital.tecnomovil.Main %*
