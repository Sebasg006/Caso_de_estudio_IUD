package tecnomovil.datos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.SplittableRandom;

import tecnomovil.modelo.Accion;
import tecnomovil.modelo.Registro;

/**
 * Autor: Sara Gabriela Castro Mora
 * Fecha de creacion: 2026-11-01
 * Proposito: generar un conjunto grande e inmutable de registros simulados del transporte urbano.
 * La misma semilla siempre produce los mismos datos, lo que permite comparar resultados.
 */
public final class GeneradorRegistros {

    private static final List<String> RUTAS = List.of(
            "T01 Troncal Norte", "T02 Troncal Sur", "T03 Avenida Central", "A10 Alimentadora Oriente",
            "A11 Alimentadora Occidente", "A12 Circular Universitaria", "E20 Expreso Aeropuerto", "N30 Nocturna");

    private static final List<String> ESTACIONES = List.of(
            "Portal Norte", "Estacion Central", "Plaza Mayor", "Terminal Sur", "Parque Industrial",
            "Universidad", "Hospital General", "Centro Administrativo", "Barrio Nuevo", "Aeropuerto");

    private static final List<Integer> HORAS_PICO = List.of(6, 7, 8, 17, 18, 19);
    private static final int HORA_APERTURA = 5;
    private static final int HORA_CIERRE = 22;
    private static final int TOTAL_USUARIOS = 50_000;

    private GeneradorRegistros() {
    }

    /**
     * Genera una lista inmutable de registros simulados para una fecha dada.
     * Cada registro se construye a partir de su propia semilla, sin estado compartido.
     *
     * @param cantidad numero de registros a generar
     * @param semilla  semilla base para reproducir los mismos datos
     * @param fecha    dia al que pertenecen los registros
     * @return lista inmutable de registros
     */
    public static List<Registro> generar(int cantidad, long semilla, LocalDate fecha) {
        return new SplittableRandom(semilla)
                .longs(cantidad)
                .mapToObj(semillaRegistro -> crearRegistro(semillaRegistro, fecha))
                .toList();
    }

    /**
     * Construye un registro simulado a partir de una semilla. Concentra la mitad de los
     * registros en horas pico y favorece unas rutas y estaciones sobre otras.
     */
    private static Registro crearRegistro(long semilla, LocalDate fecha) {
        SplittableRandom aleatorio = new SplittableRandom(semilla);
        int hora = aleatorio.nextBoolean()
                ? HORAS_PICO.get(aleatorio.nextInt(HORAS_PICO.size()))
                : aleatorio.nextInt(HORA_APERTURA, HORA_CIERRE + 1);
        LocalDateTime timestamp = fecha.atTime(hora, aleatorio.nextInt(60), aleatorio.nextInt(60));

        return new Registro(
                "U" + aleatorio.nextInt(1, TOTAL_USUARIOS + 1),
                elegirConSesgo(RUTAS, aleatorio),
                elegirConSesgo(ESTACIONES, aleatorio),
                aleatorio.nextBoolean() ? Accion.ENTRADA : Accion.SALIDA,
                timestamp);
    }

    /**
     * Elige un elemento de la lista dando mas probabilidad a los primeros,
     * para que los datos simulados tengan estaciones y rutas mas concurridas.
     */
    private static String elegirConSesgo(List<String> opciones, SplittableRandom aleatorio) {
        return opciones.get(Math.min(aleatorio.nextInt(opciones.size()), aleatorio.nextInt(opciones.size())));
    }
}
