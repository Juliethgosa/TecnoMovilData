package co.edu.iudigital.tecnomovil;

import co.edu.iudigital.tecnomovil.analisis.AnalizadorTransporte;
import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Conteo;
import co.edu.iudigital.tecnomovil.modelo.EstadoRuta;
import co.edu.iudigital.tecnomovil.modelo.Registro;
import co.edu.iudigital.tecnomovil.modelo.TiempoPromedio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Comprobaciones con un conjunto PEQUEÑO de datos cuyo resultado se calculó a mano.
 * No usa frameworks externos: cada prueba compara el valor esperado con el obtenido.
 */
public final class VerificacionFuncional {

    public record Resultado(String nombre, boolean correcto, String esperado, String obtenido) { }

    private VerificacionFuncional() { }

    private static Registro reg(String usuario, String ruta, String estacion, Accion accion, int hora, int minuto) {
        return new Registro(usuario, ruta, estacion, accion, LocalDateTime.of(2026, 10, 20, hora, minuto));
    }

    /** Conjunto de prueba (12 registros de 5 usuarios) con respuesta conocida. */
    public static List<Registro> datosDePrueba() {
        return List.of(
                reg("U1", "R1", "Central", Accion.ENTRADA, 7, 0),
                reg("U1", "R1", "Norte", Accion.SALIDA, 7, 20),
                reg("U2", "R1", "Central", Accion.ENTRADA, 7, 10),
                reg("U3", "R2", "Norte", Accion.ENTRADA, 7, 30),
                reg("U2", "R1", "Norte", Accion.SALIDA, 7, 40),
                reg("U5", "R1", "Central", Accion.ENTRADA, 7, 45),
                reg("U3", "R2", "Sur", Accion.SALIDA, 7, 55),
                reg("U5", "R1", "Norte", Accion.SALIDA, 8, 5),
                reg("U1", "R2", "Norte", Accion.ENTRADA, 17, 0),
                reg("U4", "R3", "Sur", Accion.ENTRADA, 17, 20),
                reg("U1", "R2", "Sur", Accion.SALIDA, 17, 30),
                reg("U4", "R3", "Central", Accion.SALIDA, 17, 50));
    }

    private static Resultado comprobar(String nombre, Object esperado, Object obtenido) {
        return new Resultado(nombre, esperado.equals(obtenido), String.valueOf(esperado), String.valueOf(obtenido));
    }

    private static List<String> textos(List<Conteo<String>> conteos) {
        return conteos.stream().map(c -> c.clave() + "=" + c.total()).toList();
    }

    public static List<Resultado> ejecutar(boolean paralelo) {
        AnalizadorTransporte analizador = new AnalizadorTransporte(paralelo);
        List<Registro> datos = datosDePrueba();
        List<Registro> copiaAntes = new ArrayList<>(datos); // foto de la lista antes de analizar

        Map<String, List<String>> patrones = analizador.patronesDeViaje(datos);
        List<TiempoPromedio> tiempos = analizador.tiemposPromedioEntreEstaciones(datos);
        List<EstadoRuta> estados = analizador.estadoDeRutas(datos, ocupacion -> ocupacion > 2);

        return Stream.of(
                comprobar("a) Afluencia por estación",
                        List.of("Central=3", "Norte=2", "Sur=1"),
                        textos(analizador.afluenciaPorEstacion(datos))),
                comprobar("b) Hora pico (hora=ingresos)",
                        List.of(new Conteo<>(7, 4L), new Conteo<>(17, 2L)),
                        analizador.horasPico(datos, 2)),
                comprobar("c) Rutas más utilizadas",
                        List.of("R1=3", "R2=2", "R3=1"),
                        textos(analizador.rutasMasUtilizadas(datos, 3))),
                comprobar("d) Patrón de viaje de U1",
                        List.of("Central", "Norte", "Norte", "Sur"),
                        patrones.get("U1")),
                comprobar("d) Patrón de viaje de U4",
                        List.of("Sur", "Central"),
                        patrones.get("U4")),
                comprobar("e) Central -> Norte (minutos, muestras)",
                        "23.3 min (3 muestras)",
                        tiempos.stream().filter(t -> t.tramo().equals("Central -> Norte"))
                                .map(t -> String.format(java.util.Locale.US, "%.1f min (%d muestras)", t.minutos(), t.muestras()))
                                .findFirst().orElse("sin datos")),
                comprobar("e) Norte -> Sur (minutos, muestras)",
                        "27.5 min (2 muestras)",
                        tiempos.stream().filter(t -> t.tramo().equals("Norte -> Sur"))
                                .map(t -> String.format(java.util.Locale.US, "%.1f min (%d muestras)", t.minutos(), t.muestras()))
                                .findFirst().orElse("sin datos")),
                comprobar("f) Rutas críticas (umbral > 2)",
                        List.of("R1"),
                        estados.stream().filter(EstadoRuta::critica).map(EstadoRuta::ruta).toList()),
                comprobar("Inmutabilidad: la lista original no cambió tras el análisis",
                        true,
                        datos.equals(copiaAntes))
        ).toList();
    }
}
