package tecnomovil;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import tecnomovil.datos.GeneradorRegistros;
import tecnomovil.modelo.Registro;
import tecnomovil.servicio.AnalizadorTransporte;

/**no
 * Autor: Sara Gabriela Castro Mora
 * Fecha de creacion: 2026-11-01
 * Proposito: punto de entrada del modulo TecnoMovil Data. Genera los datos simulados, ejecuta
 * las tareas a, b y c en secuencial y en paralelo, y verifica que ambos modos den el mismo resultado.
 */
public final class Main {

    private static final int TOTAL_REGISTROS = 2_000_000;
    private static final long SEMILLA = 2026L;
    private static final int TOP = 5;

    private Main() {
    }

    /**
     * Ejecuta el analisis completo y muestra los resultados en consola.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        List<Registro> registros = GeneradorRegistros.generar(TOTAL_REGISTROS, SEMILLA, LocalDate.of(2026, 11, 1));
        System.out.printf("Registros simulados: %,d%n", registros.size());

        AnalizadorTransporte secuencial = new AnalizadorTransporte(false);
        AnalizadorTransporte paralelo = new AnalizadorTransporte(true);

        ejecutarTarea("a) Afluencia por estacion (ingresos)",
                () -> secuencial.afluenciaPorEstacion(registros),
                () -> paralelo.afluenciaPorEstacion(registros));

        ejecutarTarea("b) Horas pico (top " + TOP + ", entradas y salidas)",
                () -> secuencial.horasPico(registros, TOP),
                () -> paralelo.horasPico(registros, TOP));

        ejecutarTarea("c) Rutas mas utilizadas (top " + TOP + ", ingresos)",
                () -> secuencial.rutasMasUtilizadas(registros, TOP),
                () -> paralelo.rutasMasUtilizadas(registros, TOP));
    }

    /**
     * Ejecuta una tarea en ambos modos, muestra su resultado e indica si secuencial y paralelo coinciden.
     */
    private static <K> void ejecutarTarea(String titulo, Supplier<List<Map.Entry<K, Long>>> tareaSecuencial,
                                          Supplier<List<Map.Entry<K, Long>>> tareaParalela) {
        System.out.println(titulo);
        List<Map.Entry<K, Long>> secuencial = medir("secuencial", tareaSecuencial);
        List<Map.Entry<K, Long>> paralelo = medir("paralelo", tareaParalela);
        secuencial.forEach(conteo -> System.out.printf("  %-28s %,10d%n", conteo.getKey(), conteo.getValue()));
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
}
