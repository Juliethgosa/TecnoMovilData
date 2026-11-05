package co.edu.iudigital.tecnomovil.modelo;

/** Tipo de evento que registra un validador o sensor: "entrada" o "salida". */
public enum Accion {
    ENTRADA("entrada"),
    SALIDA("salida");

    private final String texto;

    Accion(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }
}
