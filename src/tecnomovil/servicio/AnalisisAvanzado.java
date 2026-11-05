package tecnomovil.servicio;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import tecnomovil.modelo.Desplazamiento;
import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.Registro;

/**
 * Fecha de creacion: 2026-11-04
 * Proposito: tareas avanzadas d, e y f del modulo TecnoMovil Data (patrones de viaje, tiempos entre
 * estaciones y sobrecarga de rutas). Igual que AnalizadorTransporte, todas las operaciones son funciones
 * puras: no modifican la lista recibida, no usan estado compartido y devuelven colecciones inmutables,
 * por eso dan el mismo resultado en secuencial y en paralelo.
 */
public final class AnalisisAvanzado {

    private static final double SEGUNDOS_POR_MINUTO = 60.0;

    /** Ordena los usuarios por su numero: U1, U2, ..., U10 en lugar de U1, U10, U2. */
    private static final Comparator<String> ORDEN_USUARIOS =
            Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder());

    private static final Comparator<Registro> ORDEN_CRONOLOGICO = Comparator.comparing(Registro::timestamp);

    private final boolean paralelo;

    /**
     * Crea el analizador indicando si los pipelines se ejecutan con parallelStream().
     *
     * @param paralelo true para procesar en paralelo, false para procesar en secuencial
     */
    public AnalisisAvanzado(boolean paralelo) {
        this.paralelo = paralelo;
    }

    /**
     * d) Genera, para cada usuario, el listado de estaciones que visito en orden cronologico.
     *
     * @param registros registros del dia
     * @return mapa inmutable usuario -> estaciones visitadas, ordenado por numero de usuario
     */
    public SortedMap<String, List<String>> patronesDeViaje(List<Registro> registros) {
        return viajesPorUsuario(registros).entrySet().stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                viaje -> viaje.getKey(),
                                viaje -> estacionesVisitadas(viaje.getValue()),
                                (primero, segundo) -> primero,
                                () -> new TreeMap<String, List<String>>(ORDEN_USUARIOS)),
                        Collections::unmodifiableSortedMap));
    }

    /**
     * e) Estima el tiempo promedio, en minutos, que tardan los usuarios en moverse entre
     * estaciones consecutivas de su viaje.
     *
     * @param registros registros del dia
     * @return promedio en minutos, o 0 si no hay desplazamientos
     */
    public double tiempoPromedioEntreEstaciones(List<Registro> registros) {
        return desplazamientos(registros)
                .mapToLong(desplazamiento -> desplazamiento.duracion().toSeconds())
                .average()
                .orElse(0)
                / SEGUNDOS_POR_MINUTO;
    }

    /**
     * e) Calcula el tiempo promedio por tramo (origen -> destino) y devuelve los tramos mas lentos.
     *
     * @param registros registros del dia
     * @param cantidad  numero de tramos a devolver
     * @return tramos con su tiempo promedio en minutos, de mayor a menor
     */
    public List<Map.Entry<String, Double>> tramosMasLentos(List<Registro> registros, int cantidad) {
        Map<String, Double> minutosPorTramo = desplazamientos(registros)
                .collect(Collectors.groupingBy(Desplazamiento::tramo,
                        Collectors.collectingAndThen(
                                Collectors.averagingLong(desplazamiento -> desplazamiento.duracion().toSeconds()),
                                segundos -> segundos / SEGUNDOS_POR_MINUTO)));

        return minutosPorTramo.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(cantidad)
                .map(tramo -> Map.entry(tramo.getKey(), tramo.getValue()))
                .toList();
    }

    /**
     * f) Evalua la ocupacion diaria de cada ruta y marca como critica la que supera el umbral.
     *
     * @param registros       registros del dia
     * @param capacidadDiaria capacidad diaria simulada de cada ruta (ingresos)
     * @param umbral          fraccion de ocupacion a partir de la cual la ruta es critica (0.8 = 80 %)
     * @return estado de cada ruta, de mayor a menor ocupacion
     */
    public List<EstadoRuta> detectarSobrecarga(List<Registro> registros, long capacidadDiaria, double umbral) {
        if (capacidadDiaria <= 0 || umbral <= 0) {
            throw new IllegalArgumentException("capacidadDiaria y umbral deben ser mayores que cero");
        }
        return flujo(registros)
                .filter(Registro::esEntrada)
                .collect(Collectors.groupingBy(Registro::ruta, Collectors.counting()))
                .entrySet().stream()
                .map(evaluarRuta(capacidadDiaria, superaUmbral(umbral)))
                .sorted(Comparator.comparingDouble(EstadoRuta::ocupacion).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }

    /**
     * f) Funcion de orden superior: construye el criterio que decide si una ocupacion es critica.
     *
     * @param umbral fraccion de ocupacion limite
     * @return predicado que es verdadero cuando la ocupacion supera el umbral
     */
    public static Predicate<Double> superaUmbral(double umbral) {
        return ocupacion -> ocupacion > umbral;
    }

    /**
     * Funcion de orden superior: recibe la capacidad y el criterio de criticidad y devuelve
     * la funcion que transforma un conteo (ruta, ingresos) en el estado de la ruta.
     */
    private static Function<Map.Entry<String, Long>, EstadoRuta> evaluarRuta(long capacidad,
                                                                             Predicate<Double> esCritica) {
        return conteo -> {
            double ocupacion = (double) conteo.getValue() / capacidad;
            return new EstadoRuta(conteo.getKey(), conteo.getValue(), capacidad, ocupacion,
                    esCritica.test(ocupacion));
        };
    }

    /**
     * Agrupa los registros por usuario, cada grupo ordenado cronologicamente. Lo usan las tareas d y e.
     * groupingBy conserva el orden de aparicion incluso en paralelo, y el ordenamiento es estable,
     * por lo que registros con el mismo timestamp mantienen el orden en que aparecen.
     */
    private Map<String, List<Registro>> viajesPorUsuario(List<Registro> registros) {
        return flujo(registros)
                .collect(Collectors.groupingBy(Registro::idUsuario,
                        Collectors.collectingAndThen(Collectors.toList(), AnalisisAvanzado::ordenarCronologicamente)));
    }

    /**
     * Convierte los viajes de cada usuario en el flujo de desplazamientos entre estaciones distintas.
     */
    private Stream<Desplazamiento> desplazamientos(List<Registro> registros) {
        Map<String, List<Registro>> viajes = viajesPorUsuario(registros);
        Stream<List<Registro>> flujoViajes = paralelo ? viajes.values().parallelStream() : viajes.values().stream();
        return flujoViajes.flatMap(AnalisisAvanzado::paresConsecutivos);
    }

    /**
     * Funcion pura: recorre un viaje ordenado y produce un desplazamiento por cada par de
     * registros consecutivos en estaciones diferentes.
     */
    private static Stream<Desplazamiento> paresConsecutivos(List<Registro> viaje) {
        return IntStream.range(1, viaje.size())
                .mapToObj(indice -> Desplazamiento.entre(viaje.get(indice - 1), viaje.get(indice)))
                .filter(Desplazamiento::cambiaDeEstacion);
    }

    /**
     * Funcion pura: devuelve una copia inmutable de los registros ordenada por timestamp.
     */
    private static List<Registro> ordenarCronologicamente(List<Registro> registros) {
        return registros.stream().sorted(ORDEN_CRONOLOGICO).toList();
    }

    /**
     * Funcion pura: extrae la secuencia de estaciones de un viaje ya ordenado.
     */
    private static List<String> estacionesVisitadas(List<Registro> viaje) {
        return viaje.stream().map(Registro::estacion).toList();
    }

    /**
     * Abre el stream de registros en secuencial o en paralelo segun la configuracion.
     */
    private Stream<Registro> flujo(List<Registro> registros) {
        return paralelo ? registros.parallelStream() : registros.stream();
    }
}
