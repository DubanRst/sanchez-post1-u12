# Reembolsos Integrado — Proyecto integrador de patrones y arquitectura

## Descripción

Repositorio del proyecto integrador (Unidad 12) de Patrones de Diseño de Software: un sistema Spring Boot para las solicitudes de reembolso de gastos corporativos del área de Finanzas. La Parte 1 identifica la variabilidad de los requisitos y la resuelve con patrones; la Parte 2 evalúa la arquitectura, la protege con ArchUnit y documenta lo que se descartó por sobreingeniería.

## Diseño Propuesto

Esta sección se escribió antes de implementar cualquier patrón, a partir de la especificación del sistema y aplicando el test de explicabilidad de la Guía (si no se puede justificar un patrón en una oración, probablemente no se necesita).

| Requisito | Qué varía | Patrón propuesto | Justificación |
|-----------|-----------|------------------|---------------|
| RF1: validación en varias etapas | El conjunto y el orden de las verificaciones previas a la aprobación, que además crecerá (por ejemplo, un límite anual acumulado). | Chain of Responsibility | Se usa una cadena y no un único método con tres `if` porque cada verificación es independiente, se ejecuta en orden y corta en el primer rechazo, y agregar una cuarta debe consistir en enchufar un eslabón nuevo sin tocar los existentes. |
| RF2: cálculo distinto por categoría | La fórmula del monto aprobado, propia de cada categoría y con categorías nuevas previstas (por ejemplo, bienestar). | Strategy | Se usa Strategy y no un `switch` sobre la categoría porque cada categoría es un algoritmo intercambiable elegido por un único dato, y las categorías futuras deben añadirse como clases nuevas sin editar las fórmulas vigentes. |
| RF3: notificación simultánea | Qué partes reaccionan a un cambio de estado y qué hace cada una; el número de interesados crecerá (panel de métricas). | Observer | Se usa Observer y no llamadas directas del servicio a cada área porque un cambio de estado debe llegar a un conjunto creciente de interesados con reacciones distintas sin que el servicio conozca a ninguno: el panel de métricas será un suscriptor más. |


## Decisiones de diseño

### Decisión 1: orden de la cadena de validadores

Los validadores se encadenan en este orden: documentos soporte, monto máximo y duplicados. El criterio es el costo de ejecución. `ValidadorDocumentosSoporte` y `ValidadorMontoMaximo` son comprobaciones de un solo campo, de costo constante, mientras que `ValidadorDuplicados` recorre la lista de solicitudes pendientes del empleado. Como la cadena se corta en el primer rechazo, dejar el eslabón caro al final garantiza que solo se ejecuta cuando la solicitud ya superó las verificaciones baratas: las solicitudes obviamente inválidas (sin documentos o con un monto fuera de tope) nunca pagan la comparación. Lo que se pierde es que, si una solicitud tiene varios problemas, el empleado solo ve el primero y puede descubrir el duplicado después de corregir los otros dos; ordenar por costo prioriza la eficiencia sobre la experiencia de corregir todo en una sola pasada. Con el duplicado primero ocurriría lo contrario: se pagaría siempre la comparación, incluso para solicitudes que serían rechazadas por documentos. Una limitación reconocida del diseño actual es que el servicio carga la lista de pendientes antes de iniciar la cadena, de modo que el orden evita la comparación pero no esa consulta; si el volumen lo justificara, la lista se pasaría como un `Supplier` para cargarla solo cuando el eslabón la necesite.

### Decisión 2: mecanismo de selección de la calculadora

`SelectorCalculadora` recibe del contenedor de Spring una `List<CalculadoraReembolso>` con todos los `@Component` registrados y construye un mapa por categoría, en lugar de un método con un `switch` sobre `CategoriaGasto`. El RF2 exige agregar categorías nuevas (por ejemplo, bienestar) sin modificar las fórmulas existentes: con el mapa basta crear una clase que implemente `CalculadoraReembolso` y anotarla con `@Component`, y ninguna línea ya escrita se toca; con el `switch` habría que editar el método cada vez, con el riesgo de romper las ramas vigentes o de olvidar la nueva. El mapa añade dos garantías: si dos calculadoras declaran la misma categoría, el arranque falla al construirlo (clave duplicada), y una categoría sin calculadora produce un error explícito en lugar de un caso silencioso. Límite conocido: agregar una categoría también exige registrar su tope en `ValidadorMontoMaximo`, que mantiene su propio mapa; se acepta hoy porque son solo tres categorías (YAGNI) y se reconsideraría si la lista creciera o los topes cambiaran con frecuencia.


### Decisión 3: capas simples para la persistencia y sin cambios en la notificación

`SolicitudReembolsoService` sigue dependiendo directamente de `RepositorioSolicitudesJpa`, sin un puerto `RepositorioSolicitudes` con adaptador JPA. Los datos del propio proyecto no justifican la arquitectura Hexagonal para la persistencia: existe una sola dependencia (un repositorio con un único método derivado), Spring Data JPA ya abstrae el motor de base de datos y no hay ningún plan de cambiarlo; el criterio de la Guía (sistemas que sobrevivirán 3 años o más con cambios de infraestructura) no se cumple para esta dependencia. Para la notificación la respuesta es distinta: Contabilidad anunció una integración con una API externa para el próximo trimestre, pero `ObservadorCambioEstado` ya es el punto de extensión necesario, y `NotificadorContabilidad` puede reemplazar su implementación interna sin tocar el servicio ni los demás observadores. Por eso tampoco se agrega nada en `procesamiento.notificacion`. La decisión y sus consecuencias están en `docs/adr/ADR-001.md`.

### Decisión 4: qué prohíbe exactamente la Regla 1 de ArchUnit

La regla `procesamientoSinPersistenciaNiWeb` prohíbe que `procesamiento` dependa de `infraestructura`, `adaptadores`, `org.springframework.data` y `jakarta.persistence`, pero no prohíbe todo `org.springframework`. Las anotaciones de estereotipo (`@Component`) solo declaran a Spring qué clases construir e inyectar: no acoplan la lógica de negocio a ningún mecanismo de acceso a datos ni a la capa web, y son las que permiten que Spring inyecte las listas de validadores, calculadoras y observadores que hacen posible los tres patrones. Lo que sí propagaría cambios hacia las reglas de negocio es depender del acceso a datos (cambiar de base de datos) o de la capa web (cambiar de protocolo). Si la regla prohibiera cualquier import de `org.springframework`, habría que sacar el cableado a clases de configuración en otro paquete, lo que añadiría ceremonia sin proteger nada que hoy esté en riesgo. La regla distingue entonces lo que es inyección de dependencias (aceptable) de lo que es dependencia de infraestructura (no aceptable), que es el límite que realmente importa.

## Resumen de ADR-001

Se mantiene arquitectura en capas simples para la persistencia (sin puerto ni mapper) y no se modifica la notificación, porque `ObservadorCambioEstado` ya cubre el único cambio de infraestructura con fecha conocida: la integración de Contabilidad con una API externa. Consecuencias aceptadas: una eventual migración de motor de base de datos exigirá introducir el puerto retroactivamente; la entidad de dominio lleva anotaciones `jakarta.persistence` al no existir un mapper; y la asimetría entre persistencia y notificación puede parecer una inconsistencia a quien no lea el ADR (`docs/adr/ADR-001.md`).

## Qué se descartó y por qué

Cada elemento responde a la pregunta de la Guía (sección 4.2): ¿la variabilidad que justificaría la alternativa es real y documentada hoy, o es especulación sobre un futuro que aún no llegó? Ningún descarte es definitivo: cada uno indica la señal que haría reconsiderarlo.

### 1. `ApplicationEventPublisher` en lugar de una lista de observadores inyectada

- **Qué se consideró:** publicar un evento de dominio con `ApplicationEventPublisher` y escuchar con `@EventListener`, en vez de inyectar `List<ObservadorCambioEstado>` en el servicio.
- **Por qué se descartó:** hay exactamente tres observadores fijos y conocidos en tiempo de compilación, así que un bus de eventos genérico no aporta desacoplamiento adicional (ya lo da la interfaz) y sí dificulta rastrear quién reacciona a qué cambio. No supera el test de explicabilidad: no hay una oración que justifique la indirección extra.
- **Señal que haría reconsiderar:** que los suscriptores pasaran a ser decenas, que se agregaran desde módulos desplegados por separado, o que se necesitara procesamiento asíncrono o ligado a la transacción (`@TransactionalEventListener`).

### 2. Visitor o Abstract Factory para exportar reportes en varios formatos

- **Qué se consideró:** anticipar que Contabilidad pediría exportar el historial en PDF y Excel además de CSV, y construir desde ahora una jerarquía de exportadores.
- **Por qué se descartó:** hoy solo existe el requisito de exportar en CSV; no hay una segunda solicitud real de otro formato, así que no se cumple la Rule of Three y la abstracción resolvería un problema que todavía no existe (YAGNI).
- **Señal que haría reconsiderar:** una segunda solicitud real de un formato distinto; con la tercera, la abstracción estaría plenamente justificada.

### 3. Una jerarquía de excepciones por validador

- **Qué se consideró:** reemplazar `SolicitudInvalidaException` (con el motivo como texto) por `DocumentosFaltantesException`, `MontoExcedidoException` y `SolicitudDuplicadaException`.
- **Por qué se descartó:** hay un único consumidor, `ManejadorErrores`, y trata todos los rechazos igual (HTTP 422 con el mensaje). Ninguna decisión del sistema depende del tipo de rechazo, así que no se puede explicar en una oración qué problema resuelven tres clases en lugar de una: el motivo en texto ya distingue los casos para quien lo lee.
- **Señal que haría reconsiderar:** que algún cliente deba reaccionar distinto según el motivo (por ejemplo, códigos de error de API diferentes por tipo, reintentos solo para un caso, o reportes por motivo de rechazo).

### 4. Topes por categoría en configuración externa

- **Qué se consideró:** sacar los topes de `ValidadorMontoMaximo` y de las calculadoras a un `@ConfigurationProperties` o a un registro de políticas por categoría.
- **Por qué se descartó:** los topes actuales son tres valores fijos de la política vigente que no han cambiado, y no existe un requisito de modificarlos sin volver a desplegar. Externalizarlos añadiría un mecanismo de configuración, validación y pruebas para una variabilidad hipotética.
- **Señal que haría reconsiderar:** que Finanzas pida cambiar los topes sin redespliegue, o la llegada de la cuarta categoría (bienestar), que obligará a editar `ValidadorMontoMaximo` y a duplicar el cambio en dos clases.