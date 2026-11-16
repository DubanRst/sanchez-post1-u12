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