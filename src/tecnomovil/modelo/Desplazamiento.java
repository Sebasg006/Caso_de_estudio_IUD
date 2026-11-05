package tecnomovil.modelo;

import java.time.Duration;
import java.util.Objects;

/**
 * Fecha de creacion: 2026-11-04
 * Proposito: desplazamiento inmutable de un usuario entre dos estaciones consecutivas de su viaje.
 *
 * @param origen   estacion del registro anterior
 * @param destino  estacion del registro siguiente
 * @param duracion tiempo transcurrido entre ambos registros
 */
public record Desplazamiento(String origen, String destino, Duration duracion) {

    /**
     * Valida que ningun campo llegue nulo y que la duracion no sea negativa.
     */
    public Desplazamiento {
        Objects.requireNonNull(origen, "origen es obligatorio");
        Objects.requireNonNull(destino, "destino es obligatorio");
        Objects.requireNonNull(duracion, "duracion es obligatoria");
        if (duracion.isNegative()) {
            throw new IllegalArgumentException("la duracion no puede ser negativa");
        }
    }

    /**
     * Funcion pura que construye el desplazamiento entre dos registros consecutivos de un usuario.
     *
     * @param anterior  registro previo en orden cronologico
     * @param siguiente registro posterior en orden cronologico
     * @return desplazamiento de la estacion anterior a la siguiente
     */
    public static Desplazamiento entre(Registro anterior, Registro siguiente) {
        return new Desplazamiento(anterior.estacion(), siguiente.estacion(),
                Duration.between(anterior.timestamp(), siguiente.timestamp()));
    }

    /**
     * Indica si el usuario realmente cambio de estacion.
     *
     * @return true cuando origen y destino son distintos
     */
    public boolean cambiaDeEstacion() {
        return !origen.equals(destino);
    }

    /**
     * Nombre del tramo recorrido, por ejemplo "Portal Norte -> Universidad".
     *
     * @return tramo en formato origen -> destino
     */
    public String tramo() {
        return origen + " -> " + destino;
    }
}
