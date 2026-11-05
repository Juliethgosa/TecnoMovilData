package co.edu.iudigital.tecnomovil.modelo;

/** Recorrido entre dos eventos consecutivos de un mismo usuario. Inmutable. */
public record Tramo(String origen, String destino, long segundos) {

    public String etiqueta() {
        return origen + " -> " + destino;
    }
}
