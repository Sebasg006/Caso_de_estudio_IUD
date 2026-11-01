package tecnomovil.servicio;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tecnomovil.modelo.Registro;

/**
 * Autor: Sara Gabriela Castro Mora
 * Fecha de creacion: 2026-11-01
 * Proposito: operaciones funcionales de analisis sobre los registros diarios del transporte.
 * Todas las operaciones son funciones puras: no modifican la lista recibida, no usan estado
 * compartido y devuelven colecciones inmutables, por eso funcionan igual en secuencial y en paralelo.
 */
public final class AnalizadorTransporte {

    private final boolean paralelo;

    /**
     * Crea el analizador indicando si los pipelines se ejecutan con parallelStream().
     *
     * @param paralelo true para procesar en paralelo, false para procesar en secuencial
     */
    public AnalizadorTransporte(boolean paralelo) {
        this.paralelo = paralelo;
    }

    /**
     * a) Cuenta cuantos ingresos tiene cada estacion durante el dia.
     *
     * @param registros registros del dia
     * @return estaciones con su total de ingresos, de mayor a menor afluencia
     */
    public List<Map.Entry<String, Long>> afluenciaPorEstacion(List<Registro> registros) {
        Map<String, Long> ingresosPorEstacion = flujo(registros)
                .filter(Registro::esEntrada)
                .collect(Collectors.groupingBy(Registro::estacion, Collectors.counting()));

        return ordenarDeMayorAMenor(ingresosPorEstacion).toList();
    }

    /**
     * b) Agrupa todos los registros (entradas y salidas) por hora del dia.
     *
     * @param registros registros del dia
     * @return mapa inmutable ordenado por hora con el total de registros de cada hora
     */
    public SortedMap<Integer, Long> flujoPorHora(List<Registro> registros) {
        return flujo(registros)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::hora, TreeMap::new, Collectors.counting()),
                        Collections::unmodifiableSortedMap));
    }

    /**
     * b) Identifica las horas con mayor flujo de pasajeros.
     *
     * @param registros registros del dia
     * @param cantidad  numero de horas pico a devolver
     * @return horas con su total de registros, de mayor a menor flujo
     */
    public List<Map.Entry<Integer, Long>> horasPico(List<Registro> registros, int cantidad) {
        return ordenarDeMayorAMenor(flujoPorHora(registros))
                .limit(cantidad)
                .toList();
    }

    /**
     * c) Determina las rutas con mayor volumen de uso, medido por ingresos de usuarios.
     *
     * @param registros registros del dia
     * @param cantidad  numero de rutas a devolver
     * @return rutas con su total de ingresos, de mayor a menor uso
     */
    public List<Map.Entry<String, Long>> rutasMasUtilizadas(List<Registro> registros, int cantidad) {
        Map<String, Long> ingresosPorRuta = flujo(registros)
                .filter(Registro::esEntrada)
                .collect(Collectors.groupingBy(Registro::ruta, Collectors.counting()));

        return ordenarDeMayorAMenor(ingresosPorRuta)
                .limit(cantidad)
                .toList();
    }

    /**
     * Abre el stream de registros en secuencial o en paralelo segun la configuracion.
     */
    private Stream<Registro> flujo(List<Registro> registros) {
        return paralelo ? registros.parallelStream() : registros.stream();
    }

    /**
     * Ordena un conteo de mayor a menor total; en empate ordena por clave para que el
     * resultado sea siempre el mismo. Copia cada par en una entrada inmutable.
     */
    private static <K extends Comparable<K>> Stream<Map.Entry<K, Long>> ordenarDeMayorAMenor(Map<K, Long> conteos) {
        return conteos.entrySet().stream()
                .sorted(Map.Entry.<K, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(conteo -> Map.entry(conteo.getKey(), conteo.getValue()));
    }
}
