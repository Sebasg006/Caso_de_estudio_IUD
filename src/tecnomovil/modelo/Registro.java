package tecnomovil.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Autor: Sara Gabriela Castro Mora
 * Fecha de creacion: 2026-11-01
 * Proposito: registro inmutable de un paso de usuario por una estacion del sistema de transporte.
 *
 * @param idUsuario identificador del usuario que valida su tarjeta
 * @param ruta      ruta en la que se produce el registro
 * @param estacion  estacion donde se produce el registro
 * @param accion    entrada o salida de la estacion
 * @param timestamp fecha y hora exacta del registro
 */
public record Registro(String idUsuario, String ruta, String estacion, Accion accion, LocalDateTime timestamp) {

    /**
     * Valida que ningun campo del registro llegue nulo.
     */
    public Registro {
        Objects.requireNonNull(idUsuario, "idUsuario es obligatorio");
        Objects.requireNonNull(ruta, "ruta es obligatoria");
        Objects.requireNonNull(estacion, "estacion es obligatoria");
        Objects.requireNonNull(accion, "accion es obligatoria");
        Objects.requireNonNull(timestamp, "timestamp es obligatorio");
    }

    /**
     * Indica si el registro corresponde a un ingreso a la estacion.
     *
     * @return true cuando la accion es ENTRADA
     */
    public boolean esEntrada() {
        return accion == Accion.ENTRADA;
    }

    /**
     * Obtiene la hora del dia (0 a 23) en que ocurrio el registro.
     *
     * @return hora del timestamp
     */
    public int hora() {
        return timestamp.getHour();
    }
}
