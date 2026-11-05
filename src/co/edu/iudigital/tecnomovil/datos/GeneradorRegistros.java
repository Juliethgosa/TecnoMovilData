package co.edu.iudigital.tecnomovil.datos;

import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

/**
 * Genera datos SIMULADOS de validadores y sensores.
 *
 * Es determinista: con los mismos parámetros (usuarios, semilla) produce siempre exactamente
 * los mismos registros, así que los resultados del análisis se pueden repetir y comparar.
 * La lista devuelta es inmutable.
 */
public final class GeneradorRegistros {

    public static final List<String> ESTACIONES = List.of(
            "Central", "Norte", "Sur", "Oriente", "Occidente", "Universidad",
            "Aeropuerto", "Terminal", "Hospital", "Parque", "Plaza Mayor", "Industrial");

    public static final List<String> RUTAS = List.of("R1", "R2", "R3", "R4", "R5", "R6", "A1", "A2");

    public static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    private GeneradorRegistros() { }

    /** Genera los registros de {@code usuarios} personas durante un día, ordenados por timestamp. */
    public static List<Registro> generar(int usuarios, long semilla) {
        return IntStream.range(0, usuarios)
                .mapToObj(u -> registrosDeUsuario(u, semilla))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(Registro::timestamp).thenComparing(Registro::idUsuario))
                .toList(); // toList() devuelve una lista inmutable
    }

    private static List<Registro> registrosDeUsuario(int numeroUsuario, long semilla) {
        SplittableRandom azar = new SplittableRandom(semilla + numeroUsuario * 7_919L);
        String idUsuario = String.format("U%06d", numeroUsuario);
        int viajes = 1 + azar.nextInt(3);

        List<Registro> resultado = new ArrayList<>(); // lista local, se devuelve ya inmutable
        LocalDateTime inicio = LocalDateTime.of(DIA, LocalTime.of(horaInicial(azar), azar.nextInt(60), azar.nextInt(60)));
        String origen = estacionSesgada(azar);

        for (int i = 0; i < viajes && inicio.toLocalDate().equals(DIA); i++) {
            String destino = destinoDistinto(azar, origen);
            String ruta = RUTAS.get(Math.min(azar.nextInt(RUTAS.size()), azar.nextInt(RUTAS.size())));
            LocalDateTime fin = inicio.plusMinutes(8 + azar.nextInt(38));

            resultado.add(new Registro(idUsuario, ruta, origen, Accion.ENTRADA, inicio));
            resultado.add(new Registro(idUsuario, ruta, destino, Accion.SALIDA, fin));

            origen = destino;
            inicio = fin.plusMinutes(30 + azar.nextInt(150));
        }
        return List.copyOf(resultado);
    }

    /** Más tráfico en la mañana (6-8) y en la tarde (16-19) que en el resto del día. */
    private static int horaInicial(SplittableRandom azar) {
        int sorteo = azar.nextInt(100);
        if (sorteo < 35) return 6 + azar.nextInt(3);
        if (sorteo < 65) return 16 + azar.nextInt(4);
        return 5 + azar.nextInt(17);
    }

    /** Algunas estaciones (las primeras de la lista) reciben más usuarios que otras. */
    private static String estacionSesgada(SplittableRandom azar) {
        int n = ESTACIONES.size();
        return ESTACIONES.get(Math.min(azar.nextInt(n), azar.nextInt(n)));
    }

    private static String destinoDistinto(SplittableRandom azar, String origen) {
        String destino = estacionSesgada(azar);
        while (destino.equals(origen)) {
            destino = ESTACIONES.get(azar.nextInt(ESTACIONES.size()));
        }
        return destino;
    }
}
