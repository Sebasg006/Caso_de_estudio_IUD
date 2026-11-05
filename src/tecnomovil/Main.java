package tecnomovil;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import tecnomovil.datos.GeneradorRegistros;
import tecnomovil.modelo.EstadoRuta;
import tecnomovil.modelo.Registro;
import tecnomovil.servicio.AnalisisAvanzado;
import tecnomovil.servicio.AnalizadorTransporte;

/**
 * Autor: Sara Gabriela Castro Mora
 * Fecha de creacion: 2026-11-01
 * Actualizacion: 2026-11-04 - se integran las tareas d, e y f (AnalisisAvanzado).
 * Proposito: punto de entrada del modulo TecnoMovil Data. Genera los datos simulados, ejecuta
 * las tareas a a f en secuencial y en paralelo, y verifica que ambos modos den el mismo resultado.
 */
public final class Main {

    private static final int TOTAL_REGISTROS = 2_000_000;
    private static final long SEMILLA = 2026L;
    private static final int TOP = 5;
    private static final int USUARIOS_DE_EJEMPLO = 3;
    private static final int ESTACIONES_DE_EJEMPLO = 8;
    private static final long CAPACIDAD_DIARIA_POR_RUTA = 250_000L;
    private static final double UMBRAL_CRITICO = 0.80;

    private Main() {
    }

    /**
     * Ejecuta el analisis completo y muestra los resultados en consola.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        List<Registro> registros = GeneradorRegistros.generar(TOTAL_REGISTROS, SEMILLA, LocalDate.of(2026, 11, 1));
        System.out.printf("Registros simulados: %,d%n%n", registros.size());

        AnalizadorTransporte secuencial = new AnalizadorTransporte(false);
        AnalizadorTransporte paralelo = new AnalizadorTransporte(true);
        AnalisisAvanzado avanzadoSecuencial = new AnalisisAvanzado(false);
        AnalisisAvanzado avanzadoParalelo = new AnalisisAvanzado(true);

        ejecutarTarea("a) Afluencia por estacion (ingresos)",
                () -> secuencial.afluenciaPorEstacion(registros),
                () -> paralelo.afluenciaPorEstacion(registros),
                Main::mostrarConteos);

        ejecutarTarea("b) Horas pico (top " + TOP + ", entradas y salidas)",
                () -> secuencial.horasPico(registros, TOP),
                () -> paralelo.horasPico(registros, TOP),
                Main::mostrarConteos);

        ejecutarTarea("c) Rutas mas utilizadas (top " + TOP + ", ingresos)",
                () -> secuencial.rutasMasUtilizadas(registros, TOP),
                () -> paralelo.rutasMasUtilizadas(registros, TOP),
                Main::mostrarConteos);

        ejecutarTarea("d) Patrones de viaje por usuario (primeros " + USUARIOS_DE_EJEMPLO + " usuarios)",
                () -> avanzadoSecuencial.patronesDeViaje(registros),
                () -> avanzadoParalelo.patronesDeViaje(registros),
                Main::mostrarPatrones);

        ejecutarTarea("e) Tiempo promedio entre estaciones consecutivas",
                () -> avanzadoSecuencial.tiempoPromedioEntreEstaciones(registros),
                () -> avanzadoParalelo.tiempoPromedioEntreEstaciones(registros),
                minutos -> System.out.printf("  Promedio general: %.2f minutos%n", minutos));

        ejecutarTarea("e) Tramos con mayor tiempo promedio (top " + TOP + ")",
                () -> avanzadoSecuencial.tramosMasLentos(registros, TOP),
                () -> avanzadoParalelo.tramosMasLentos(registros, TOP),
                Main::mostrarMinutos);

        ejecutarTarea(String.format("f) Sobrecarga de rutas (capacidad %,d ingresos/dia, umbral %.0f %%)",
                        CAPACIDAD_DIARIA_POR_RUTA, UMBRAL_CRITICO * 100),
                () -> avanzadoSecuencial.detectarSobrecarga(registros, CAPACIDAD_DIARIA_POR_RUTA, UMBRAL_CRITICO),
                () -> avanzadoParalelo.detectarSobrecarga(registros, CAPACIDAD_DIARIA_POR_RUTA, UMBRAL_CRITICO),
                Main::mostrarEstadosRuta);
    }

    /**
     * Ejecuta una tarea en ambos modos, muestra su resultado e indica si secuencial y paralelo coinciden.
     * Es una funcion de orden superior: recibe las tareas y la forma de mostrarlas como funciones.
     */
    private static <T> void ejecutarTarea(String titulo, Supplier<T> tareaSecuencial, Supplier<T> tareaParalela,
                                          Consumer<T> mostrar) {
        System.out.println(titulo);
        T secuencial = medir("secuencial", tareaSecuencial);
        T paralelo = medir("paralelo", tareaParalela);
        mostrar.accept(secuencial);
        System.out.println("  Secuencial y paralelo coinciden: " + secuencial.equals(paralelo));
        System.out.println();
    }

    /**
     * Ejecuta una tarea, imprime cuanto tardo y devuelve su resultado.
     */
    private static <T> T medir(String modo, Supplier<T> tarea) {
        long inicio = System.nanoTime();
        T resultado = tarea.get();
        System.out.printf("  [%s] %d ms%n", modo, (System.nanoTime() - inicio) / 1_000_000);
        return resultado;
    }

    private static <K> void mostrarConteos(List<Map.Entry<K, Long>> conteos) {
        conteos.forEach(conteo -> System.out.printf("  %-28s %,10d%n", conteo.getKey(), conteo.getValue()));
    }

    private static void mostrarMinutos(List<Map.Entry<String, Double>> tramos) {
        tramos.forEach(tramo -> System.out.printf("  %-46s %8.2f min%n", tramo.getKey(), tramo.getValue()));
    }

    private static void mostrarPatrones(SortedMap<String, List<String>> patrones) {
        System.out.printf("  Usuarios con viajes: %,d%n", patrones.size());
        patrones.entrySet().stream()
                .limit(USUARIOS_DE_EJEMPLO)
                .forEach(patron -> System.out.printf("  %-7s (%d estaciones) %s%s%n",
                        patron.getKey(),
                        patron.getValue().size(),
                        String.join(" > ", patron.getValue().subList(0,
                                Math.min(ESTACIONES_DE_EJEMPLO, patron.getValue().size()))),
                        patron.getValue().size() > ESTACIONES_DE_EJEMPLO ? " > ..." : ""));
    }

    private static void mostrarEstadosRuta(List<EstadoRuta> estados) {
        estados.forEach(estado -> System.out.printf("  %-28s %,10d  %6.1f %%  %s%n",
                estado.ruta(), estado.ingresos(), estado.ocupacion() * 100, estado.estado()));
        System.out.printf("  Rutas criticas: %d de %d%n",
                estados.stream().filter(EstadoRuta::critica).count(), estados.size());
    }
}
