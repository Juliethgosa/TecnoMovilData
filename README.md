# TecnoMovil Data – Módulo funcional de procesamiento de datos de transporte

Solución al caso de estudio de la Unidad 2 (IU Digital de Antioquia): procesamiento y agregación
de datos diarios de transporte urbano usando **programación funcional en Java** (Streams, lambdas,
funciones puras y colecciones inmutables). No usa frameworks ni librerías externas.

## Requisitos
- JDK 17 o superior (`java -version` y `javac -version` deben mostrar 17+).

## Ejecutar
- Linux / macOS: `./ejecutar.sh` (opcional: número de usuarios, por ejemplo `./ejecutar.sh 100000`)
- Windows: `ejecutar.bat`
- Manual:
  ```
  javac --release 17 -encoding UTF-8 -d out $(find src -name "*.java")
  java -cp out co.edu.iudigital.tecnomovil.Main
  ```

## Estructura
```
src/co/edu/iudigital/tecnomovil/
├── Main.java                      Ejecuta verificación, simulación y resultados
├── VerificacionFuncional.java     Pruebas con datos pequeños (sin frameworks)
├── modelo/                        Registro, Accion, Conteo, Tramo, TiempoPromedio, EstadoRuta
├── datos/GeneradorRegistros.java  Datos simulados deterministas (semilla fija)
└── analisis/AnalizadorTransporte.java   Tareas a) a f) del caso de estudio
```

## Tareas implementadas
| Tarea | Método |
|---|---|
| a) Afluencia por estación | `afluenciaPorEstacion` |
| b) Horas pico | `horasPico` |
| c) Rutas más utilizadas | `rutasMasUtilizadas` |
| d) Patrones de viaje por usuario | `patronesDeViaje` |
| e) Tiempo promedio entre estaciones | `tiemposPromedioEntreEstaciones` |
| f) Detección de sobrecarga en rutas | `estadoDeRutas` |

## Decisiones técnicas
- **Funciones puras:** ningún método del analizador lee ni modifica estado externo.
- **Inmutabilidad:** `Registro` es un `record`; todas las listas y mapas devueltos son inmutables.
- **Streams y lambdas:** todo se resuelve con pipelines declarativos, sin ciclos ni acumuladores.
- **Paralelización:** `new AnalizadorTransporte(true)` usa `parallelStream()` y produce el mismo resultado.

## Informe
El informe en PDF (APA 7) se encuentra en la carpeta `docs/`.
