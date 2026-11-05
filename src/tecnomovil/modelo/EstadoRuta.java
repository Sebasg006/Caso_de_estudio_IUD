package tecnomovil.modelo;

import java.util.Objects;

/**
 * Fecha de creacion: 2026-11-04
 * Proposito: resultado inmutable de evaluar la ocupacion diaria de una ruta frente a su capacidad simulada.
 *
 * @param ruta      ruta evaluada
 * @param ingresos  total de ingresos de usuarios a la ruta en el dia
 * @param capacidad capacidad diaria simulada de la ruta
 * @param ocupacion ingresos divididos entre capacidad (1.0 equivale al 100 %)
 * @param critica   true cuando la ocupacion supera el umbral definido
 */
public record EstadoRuta(String ruta, long ingresos, long capacidad, double ocupacion, boolean critica) {

    /**
     * Valida que la ruta exista y que los valores numericos sean coherentes.
     */
    public EstadoRuta {
        Objects.requireNonNull(ruta, "ruta es obligatoria");
        if (ingresos < 0 || capacidad <= 0) {
            throw new IllegalArgumentException("ingresos debe ser >= 0 y capacidad > 0");
        }
    }

    /**
     * Texto del estado para mostrar en reportes.
     *
     * @return "CRITICA" o "NORMAL"
     */
    public String estado() {
        return critica ? "CRITICA" : "NORMAL";
    }
}
