package co.edu.iudigital.tecnomovil;

import co.edu.iudigital.tecnomovil.analisis.AnalizadorTransporte;
import co.edu.iudigital.tecnomovil.datos.GeneradorRegistros;
import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Conteo;
import co.edu.iudigital.tecnomovil.modelo.EstadoRuta;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.modelo.TiempoPromedio;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * TecnoMovil Data: módulo funcional de procesamiento y agregación de datos diarios de transporte.
 *
 * Uso:  java -cp out co.edu.iudigital.tecnomovil.Main [usuarios]
 * Por defecto se simulan 250.000 usuarios (≈ 1 millón de registros).
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        // Salida en UTF-8 para que las tildes se vean bien en la consola
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        int usuarios = args.length > 0 ? Integer.parseInt(args[0]) : 250_000;

        titulo("1. VERIFICACIÓN CON DATOS DE PRUEBA (resultado calculado a mano)");
        List<VerificacionFuncional.Resultado> pruebas = VerificacionFuncional.ejecutar(false);
        pruebas.forEach(p -> System.out.printf("  [%s] %s%n", p.correcto() ? "OK" : "FALLO", p.nombre()));
        long correctas = pruebas.stream().filter(VerificacionFuncional.Resultado::correcto).count();
        System.out.printf("  Resultado: %d de %d pruebas correctas%n", correctas, pruebas.size());

        titulo("2. SIMULACIÓN DE DATOS");
        long t0 = System.nanoTime();
        List<Registro> registros = GeneradorRegistros.generar(usuarios, 2026L);
        System.out.printf(Locale.US, "  Usuarios simulados : %,d%n", usuarios);
        System.out.printf(Locale.US, "  Registros generados: %,d (%d ms)%n", registros.size(), ms(t0));

        titulo("3. INMUTABILIDAD");
        try {
            registros.add(new Registro("X", "R1", "Central", Accion.ENTRADA, LocalDateTime.now()));
            System.out.println("  ERROR: la lista se pudo modificar");
        } catch (UnsupportedOperationException e) {
            System.out.println("  La lista original es inmutable: add() lanzó UnsupportedOperationException");
        }

        AnalizadorTransporte secuencial = new AnalizadorTransporte(false);
        long entradas = registros.stream().filter(Registro::esEntrada).count();
        long umbral = Math.round(entradas / (double) GeneradorRegistros.RUTAS.size() * 1.25);

        titulo("4. RESULTADOS (procesamiento secuencial)");

        subtitulo("a) Afluencia por estación (ingresos del día)");
        secuencial.afluenciaPorEstacion(registros).forEach(c -> System.out.printf(Locale.US, "  %-12s %,10d%n", c.clave(), c.total()));

        subtitulo("b) Horas pico (3 horas con más ingresos)");
        secuencial.horasPico(registros, 3).forEach(c -> System.out.printf(Locale.US, "  %02d:00 - %02d:59 %,10d%n", c.clave(), c.clave(), c.total()));

        subtitulo("c) Rutas más utilizadas (top 5)");
        secuencial.rutasMasUtilizadas(registros, 5).forEach(c -> System.out.printf(Locale.US, "  %-4s %,10d%n", c.clave(), c.total()));

        subtitulo("d) Patrones de viaje (primeros 3 usuarios)");
        secuencial.patronesDeViaje(registros).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(3)
                .forEach(e -> System.out.printf("  %s: %s%n", e.getKey(), String.join(" > ", e.getValue())));

        subtitulo("e) Tiempo promedio entre estaciones (5 tramos con más muestras)");
        secuencial.tiemposPromedioEntreEstaciones(registros).stream()
                .sorted((a, b) -> Long.compare(b.muestras(), a.muestras()))
                .limit(5)
                .forEach(t -> System.out.printf(Locale.US, "  %-24s %6.1f min  (%,d muestras)%n", t.tramo(), t.minutos(), t.muestras()));

        subtitulo("f) Estado de las rutas (umbral crítico: más de " + String.format(Locale.US, "%,d", umbral) + " ingresos)");
        secuencial.estadoDeRutas(registros, ocupacion -> ocupacion > umbral).forEach(r ->
                System.out.printf(Locale.US, "  %-4s %,10d  %s%n", r.ruta(), r.ocupacion(), r.critica() ? "CRÍTICA" : "normal"));

        titulo("5. SECUENCIAL vs PARALELO (parallelStream)");
        AnalizadorTransporte paralelo = new AnalizadorTransporte(true);
        compararModos("a) Afluencia por estación",
                () -> secuencial.afluenciaPorEstacion(registros), () -> paralelo.afluenciaPorEstacion(registros));
        compararModos("b) Horas pico",
                () -> secuencial.horasPico(registros, 3), () -> paralelo.horasPico(registros, 3));
        compararModos("c) Rutas más utilizadas",
                () -> secuencial.rutasMasUtilizadas(registros, 5), () -> paralelo.rutasMasUtilizadas(registros, 5));
        compararModos("d) Patrones de viaje",
                () -> secuencial.patronesDeViaje(registros), () -> paralelo.patronesDeViaje(registros));
        compararModos("e) Tiempos promedio",
                () -> secuencial.tiemposPromedioEntreEstaciones(registros), () -> paralelo.tiemposPromedioEntreEstaciones(registros));
        compararModos("f) Estado de rutas",
                () -> secuencial.estadoDeRutas(registros, o -> o > umbral), () -> paralelo.estadoDeRutas(registros, o -> o > umbral));

        System.out.println();
        System.out.printf("  La lista original sigue con %,d registros (no se modificó).%n", registros.size());
    }

    /** Ejecuta la misma operación en modo secuencial y paralelo y verifica que den el mismo resultado. */
    private static <T> void compararModos(String nombre, Supplier<T> secuencial, Supplier<T> paralelo) {
        long t1 = System.nanoTime();
        T r1 = secuencial.get();
        long msSecuencial = ms(t1);
        long t2 = System.nanoTime();
        T r2 = paralelo.get();
        long msParalelo = ms(t2);
        System.out.printf("  %-28s secuencial %5d ms | paralelo %5d ms | mismo resultado: %s%n",
                nombre, msSecuencial, msParalelo, r1.equals(r2) ? "SÍ" : "NO");
    }

    private static long ms(long desdeNanos) {
        return (System.nanoTime() - desdeNanos) / 1_000_000;
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=".repeat(78));
        System.out.println(texto);
        System.out.println("=".repeat(78));
    }

    private static void subtitulo(String texto) {
        System.out.println();
        System.out.println(" " + texto);
    }
}
