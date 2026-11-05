package co.edu.iudigital.tecnomovil.modelo;

/** Ocupación (simulada) de una ruta y si se marcó como "crítica". */
public record EstadoRuta(String ruta, long ocupacion, boolean critica) { }
