package co.edu.iudigital.tecnomovil.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro de un evento de transporte (requerimiento 3.1).
 *
 * Es un {@code record}: todos sus campos son finales, no tiene setters y, por tanto,
 * una vez creado NUNCA cambia (objeto inmutable).
 */
public record Registro(String idUsuario, String ruta, String estacion, Accion accion, LocalDateTime timestamp) {

    public Registro {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(estacion, "estacion");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    /** Hora del día (0-23) en la que ocurrió el evento. */
    public int hora() {
        return timestamp.getHour();
    }

    public boolean esEntrada() {
        return accion == Accion.ENTRADA;
    }
}
