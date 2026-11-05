package co.edu.iudigital.tecnomovil.modelo;

/** Par (clave, total) usado para devolver conteos y rankings. Inmutable. */
public record Conteo<K>(K clave, long total) { }
