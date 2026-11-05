package co.edu.iudigital.tecnomovil.analisis;

import co.edu.iudigital.tecnomovil.modelo.Conteo;
import co.edu.iudigital.tecnomovil.modelo.EstadoRuta;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.modelo.TiempoPromedio;
import co.edu.iudigital.tecnomovil.modelo.Tramo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Operaciones funcionales del módulo de procesamiento y agregación de datos diarios
 * (requerimiento 3.2, tareas a-f).
 *
 * Reglas de diseño:
 *  - Cada método es una FUNCIÓN PURA: el resultado depende solo de los argumentos, no lee ni
 *    escribe estado externo y no modifica la lista recibida.
 *  - Todos los resultados son colecciones INMUTABLES (List.copyOf, Map.copyOf, toList()).
 *  - Todo se resuelve con pipelines de Streams y lambdas, sin ciclos ni variables acumuladoras.
 *  - La única "configuración" es el campo final {@code paralelo}: con true se usa parallelStream().
 *    Como no hay estado compartido mutable, el resultado es el mismo en ambos modos.
 */
public final class AnalizadorTransporte {

    private final boolean paralelo;

    public AnalizadorTransporte(boolean paralelo) {
        this.paralelo = paralelo;
    }

    /** Crea el flujo (secuencial o paralelo) sin modificar la colección original. */
    private <T> Stream<T> flujo(Collection<T> datos) {
        return paralelo ? datos.parallelStream() : datos.stream();
    }

    // ------------------------------------------------------------------
    // Funciones de orden superior reutilizables
    // ------------------------------------------------------------------

    /**
     * Cuenta registros agrupándolos por una clave. Recibe DOS funciones como parámetro:
     * un {@link Predicate} (qué registros cuentan) y una {@link Function} (por qué se agrupan).
     */
    public <K> Map<K, Long> contarPor(List<Registro> registros,
                                      Predicate<Registro> filtro,
                                      Function<Registro, K> clasificador) {
        return flujo(registros)
                .filter(filtro)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(clasificador, Collectors.counting()),
                        Map::copyOf));
    }

    /** Ordena un mapa de conteos de mayor a menor (desempate por clave) y toma los primeros n. */
    public static <K extends Comparable<K>> List<Conteo<K>> ranking(Map<K, Long> conteos, int n) {
        return conteos.entrySet().stream()
                .map(e -> new Conteo<>(e.getKey(), e.getValue()))
                .sorted(Comparator.<Conteo<K>>comparingLong(Conteo::total).reversed()
                        .thenComparing(Conteo::clave))
                .limit(n)
                .toList();
    }

    // ------------------------------------------------------------------
    // a) Afluencia por estación
    // ------------------------------------------------------------------

    /** Cuántos usuarios ingresan a cada estación durante el día (de mayor a menor). */
    public List<Conteo<String>> afluenciaPorEstacion(List<Registro> registros) {
        Map<String, Long> porEstacion = contarPor(registros, Registro::esEntrada, Registro::estacion);
        return ranking(porEstacion, Integer.MAX_VALUE);
    }

    // ------------------------------------------------------------------
    // b) Horas pico
    // ------------------------------------------------------------------

    /** Las {@code n} horas del día con más ingresos de pasajeros. */
    public List<Conteo<Integer>> horasPico(List<Registro> registros, int n) {
        Map<Integer, Long> porHora = contarPor(registros, Registro::esEntrada, Registro::hora);
        return ranking(porHora, n);
    }

    // ------------------------------------------------------------------
    // c) Rutas más utilizadas
    // ------------------------------------------------------------------

    /** Las {@code n} rutas con mayor volumen de uso (ingresos). */
    public List<Conteo<String>> rutasMasUtilizadas(List<Registro> registros, int n) {
        Map<String, Long> porRuta = contarPor(registros, Registro::esEntrada, Registro::ruta);
        return ranking(porRuta, n);
    }

    // ------------------------------------------------------------------
    // d) Patrones de viaje por usuario
    // ------------------------------------------------------------------

    /**
     * Para cada usuario, la lista de estaciones visitadas en el orden en que aparecen
     * en los registros. groupingBy conserva el orden de encuentro de una lista ordenada,
     * también en modo paralelo.
     */
    public Map<String, List<String>> patronesDeViaje(List<Registro> registros) {
        return flujo(registros)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                Registro::idUsuario,
                                Collectors.collectingAndThen(
                                        Collectors.mapping(Registro::estacion, Collectors.toList()),
                                        List::copyOf)),
                        Map::copyOf));
    }

    // ------------------------------------------------------------------
    // e) Tiempo promedio entre estaciones
    // ------------------------------------------------------------------

    private record Resumen(double segundosPromedio, long muestras) { }

    /**
     * Tiempo promedio que tardan los usuarios entre estaciones consecutivas.
     * Función auxiliar pura: convierte los registros de UN usuario en tramos entre
     * eventos consecutivos (ordenados por tiempo) que ocurren en estaciones distintas.
     */
    private static Stream<Tramo> tramosConsecutivos(List<Registro> delUsuario) {
        List<Registro> ordenados = delUsuario.stream()
                .sorted(Comparator.comparing(Registro::timestamp))
                .toList();
        return IntStream.range(0, ordenados.size() - 1)
                .mapToObj(i -> {
                    Registro desde = ordenados.get(i);
                    Registro hasta = ordenados.get(i + 1);
                    long segundos = java.time.Duration.between(desde.timestamp(), hasta.timestamp()).getSeconds();
                    return new Tramo(desde.estacion(), hasta.estacion(), segundos);
                })
                .filter(t -> !t.origen().equals(t.destino()) && t.segundos() >= 0);
    }

    public List<TiempoPromedio> tiemposPromedioEntreEstaciones(List<Registro> registros) {
        Map<String, Resumen> porTramo = flujo(registros)
                .collect(Collectors.groupingBy(Registro::idUsuario))          // Map<usuario, registros>
                .values().stream()
                .flatMap(AnalizadorTransporte::tramosConsecutivos)             // Stream<Tramo>
                .collect(Collectors.groupingBy(
                        Tramo::etiqueta,
                        Collectors.teeing(
                                Collectors.averagingLong(Tramo::segundos),
                                Collectors.counting(),
                                Resumen::new)));

        return porTramo.entrySet().stream()
                .map(e -> new TiempoPromedio(e.getKey(), e.getValue().segundosPromedio() / 60.0, e.getValue().muestras()))
                .sorted(Comparator.comparing(TiempoPromedio::tramo))
                .toList();
    }

    // ------------------------------------------------------------------
    // f) Detección de sobrecarga en rutas
    // ------------------------------------------------------------------

    /**
     * Marca como crítica una ruta cuando su ocupación (simulada como el número de ingresos
     * del día) cumple el criterio recibido. El criterio es una lambda: {@code ocupacion -> ocupacion > umbral}.
     */
    public List<EstadoRuta> estadoDeRutas(List<Registro> registros, LongPredicate esCritica) {
        return contarPor(registros, Registro::esEntrada, Registro::ruta).entrySet().stream()
                .map(e -> new EstadoRuta(e.getKey(), e.getValue(), esCritica.test(e.getValue())))
                .sorted(Comparator.comparingLong(EstadoRuta::ocupacion).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }
}
