# TecnoMovil Data

Módulo en Java puro (sin frameworks) que procesa los registros diarios del sistema de transporte urbano de la ciudad ficticia de TecnoValle usando programación funcional: funciones puras, colecciones inmutables, lambdas y pipelines de Streams.

Caso de estudio de la Unidad 2 (EA2), Tecnología en Desarrollo de Software – IU Digital de Antioquia.

## El problema

El sistema actual procesa entre 6 y 12 millones de registros diarios de forma imperativa y secuencial: tarda hasta 2 horas, comparte estructuras mutables y produce conteos erróneos por condiciones de carrera. Este módulo reemplaza el procesamiento y la agregación diaria con un enfoque declarativo que puede ejecutarse en paralelo sin cambiar el resultado.

Cada registro tiene `idUsuario`, `ruta`, `estacion`, `accion` (`ENTRADA` / `SALIDA`) y `timestamp`.

## Arquitectura

```
src/tecnomovil/
├── Main.java                      Ejecuta cada tarea en secuencial y en paralelo, mide tiempos y compara resultados
├── modelo/
│   ├── Registro.java              record inmutable de un paso por una estación
│   ├── Accion.java                enum ENTRADA / SALIDA
│   ├── Desplazamiento.java        record: movimiento entre dos estaciones consecutivas (tarea e)
│   └── EstadoRuta.java            record: ocupación de una ruta frente a su capacidad (tarea f)
├── datos/
│   └── GeneradorRegistros.java    2.000.000 de registros simulados, reproducibles por semilla
└── servicio/
    ├── AnalizadorTransporte.java  Tareas a, b y c
    └── AnalisisAvanzado.java      Tareas d, e y f
```

Ambos servicios reciben un `boolean paralelo` en el constructor: con `true` todos los pipelines usan `parallelStream()`.

## Tareas

| Tarea | Método | Cómo se resuelve |
|---|---|---|
| a) Afluencia por estación | `afluenciaPorEstacion` | `filter(esEntrada)` + `groupingBy(estacion, counting())` |
| b) Horas pico | `flujoPorHora`, `horasPico` | `groupingBy(hora)` en un `TreeMap` inmutable, ordenado por volumen |
| c) Rutas más utilizadas | `rutasMasUtilizadas` | `groupingBy(ruta, counting())` + top N |
| d) Patrones de viaje | `patronesDeViaje` | Agrupa por usuario, ordena cada viaje por `timestamp` y lo proyecta a estaciones |
| e) Tiempo entre estaciones | `tiempoPromedioEntreEstaciones`, `tramosMasLentos` | Pares consecutivos con `IntStream.range` + `flatMap`, promedio con `averagingLong` |
| f) Sobrecarga de rutas | `detectarSobrecarga` | Ocupación = ingresos / capacidad; `CRITICA` si supera el umbral (80 %) |

## Principios funcionales aplicados

- **Funciones puras:** ningún método modifica la lista recibida ni depende de estado externo. Ejemplos: `paresConsecutivos`, `ordenarCronologicamente`, `Desplazamiento.entre`.
- **Inmutabilidad:** los datos son `record`; los resultados se devuelven con `Stream.toList()`, `Map.entry` y `Collections.unmodifiableSortedMap`.
- **Funciones de orden superior:** `superaUmbral(umbral)` devuelve un `Predicate<Double>`; `evaluarRuta(capacidad, criterio)` devuelve una `Function`; `Main.ejecutarTarea` recibe `Supplier` y `Consumer`.
- **Lambdas y referencias a métodos:** `Registro::esEntrada`, `Desplazamiento::tramo`, `ocupacion -> ocupacion > umbral`.
- **Streams declarativos:** `filter`, `map`, `flatMap`, `sorted`, `limit` y los collectors `groupingBy`, `counting`, `averagingLong`, `collectingAndThen`, `toMap`.
- **Paralelización segura:** al no haber efectos secundarios, `parallelStream()` produce exactamente el mismo resultado que `stream()`. `Main` lo verifica en cada tarea.

## Resultados (2.000.000 de registros)

Todas las tareas imprimen `Secuencial y paralelo coinciden: true`. Tiempos de una ejecución de referencia:

| Tarea | Secuencial | Paralelo |
|---|---|---|
| a) Afluencia | 111 ms | 64 ms |
| b) Horas pico | 200 ms | 89 ms |
| c) Rutas | 116 ms | 42 ms |
| d) Patrones | 2003 ms | 1934 ms |
| e) Promedio | 4142 ms | 2336 ms |
| e) Tramos | 2767 ms | 2114 ms |
| f) Sobrecarga | 99 ms | 32 ms |

La tarea d) casi no mejora en paralelo porque su paso dominante, construir el mapa ordenado de 50.000 usuarios, es secuencial.

## Ejecución

Requiere JDK 17+.

```bash
javac -d out $(find src -name "*.java")
java -cp out tecnomovil.Main
```
