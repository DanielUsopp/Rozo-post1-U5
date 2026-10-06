# Post-contenido — Unidad 5: Integración en Aplicaciones Web

## Descripción
Repositorio del post-contenido de la Unidad 5 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`reservas-labs-api`) para la reserva de laboratorios de cómputo, con dos partes: una API REST en capas (`Entity`, `Repository`, `Service`, `Controller`) sobre H2, y una vista Thymeleaf (MVC clásico) que reutiliza el mismo Service.

## Parte 1 — Repository, Service y Controller REST
`LaboratorioRepository` y `ReservaRepository` extienden `JpaRepository`; `ReservaRepository` agrega una consulta JPQL propia para detectar solapamientos de horario. `ReservaService` concentra las reglas de negocio (solapamiento, horario de atención, duración, cancelación tardía). `ReservaController` y `LaboratorioController` exponen `/api/reservas` y `/api/laboratorios`. Ver paquetes `model/`, `repository/`, `service/`, `exception/` y `controller/`.

## Parte 2 — Vista MVC con Thymeleaf
`ReservaWebController` expone `/reservas` con Thymeleaf, inyectando la **MISMA** instancia de `ReservaService` que usa la API REST — sin Service duplicado. `ReservaWebExceptionHandler` maneja las mismas excepciones de dominio que `GlobalRestExceptionHandler`, con presentación distinta (redirección con mensaje en lugar de JSON). Ver paquete `web/` y `templates/reservas/`.

## Cómo ejecutar
```bash
$ mvn clean package
$ mvn spring-boot:run
API REST: http://localhost:8080/api/reservas
Vista MVC: http://localhost:8080/reservas
```


## Decisiones de diseño

## Punto de decisión 1 — Ubicación de la validación de solapamiento

La consulta `ReservaRepository.buscarSolapamientos` realiza el filtrado eficiente directamente en la base de datos mediante SQL/JPQL para evitar cargar en memoria todas las reservas del laboratorio, garantizando la escalabilidad a medida que crece el volumen de datos. Sin embargo, la decisión de negocio (interpretar el resultado y lanzar `ReservaConflictException` con un mensaje descriptivo) la toma exclusivamente ReservaService.

Si el Controller llamara directamente a `buscarSolapamientos()`, se rompería el principio de separación de capas: el controlador asumiría responsabilidades de dominio y orquestación de excepciones, mezclando la capa de presentación HTTP con las reglas de negocio y acoplando la lógica al punto de entrada.

## Punto de decisión 2 — Reglas con y sin apoyo del Repository
No todas las reglas de negocio requieren consultar la base de datos de persistencia:

Reglas sin acceso a datos: La validación del horario de atención (07:00 a 21:00) y de la duración permitida (entre 30 minutos y 3 horas) depende únicamente de los atributos del objeto `Reserva` enviado. Por ello, validarHorarioYDuracion se ejecuta en `ReservaService` mediante Java puro, sin consultar la base de datos.

Reglas con apoyo del Repository: La detección de solapamientos requiere comparar la reserva entrante contra el estado de otras reservas activas guardadas en el sistema, por lo que depende obligatoriamente de una consulta en el Repository.

Criterio general: Si la regla de negocio evalúa únicamente la coherencia interna de la entidad, no requiere Repository; si depende del estado global o de otras entidades del sistema, requiere apoyo del Repository.

## Punto de decisión 3 — Cómo comparten Service el Controller MVC y el REST
Tanto `ReservaWebController (MVC)` como `ReservaController (REST)` inyectan por constructor la misma clase `ReservaService`, gestionada por Spring como un bean único singleton. Ninguno de los controladores reimplementa la validación de solapamiento de horarios ni de límites de atención.

Duplicar la lógica creando un `ReservaWebService` o copiando las validaciones en el controlador MVC habría generado duplicación de código (Code Smell), obligando a actualizar las reglas de negocio en múltiples lugares ante un cambio futuro, lo cual destruye la mantenibilidad que la capa Service busca proteger.

Referencia en código:

`Inyección REST`: com.universidad.reservaslabs.controller.ReservaController (private final ReservaService service;)

`Inyección MVC`: com.universidad.reservaslabs.web.ReservaWebController (private final ReservaService service;)

## Punto de decisión 4 — Manejo de errores consistente entre MVC y REST
Aunque ambas superficies de presentación consumen las mismas excepciones de dominio (`ReservaConflictException`, `RecursoNoEncontradoException`) lanzadas por `ReservaService`, el formato de respuesta que requiere cada interfaz es diferente. Un `@RestControllerAdvice` serializa siempre la respuesta a JSON con códigos HTTP (409, 404), mientras que la vista MVC Thymeleaf necesita redireccionar al formulario (`redirect:/reservas/nueva`) con atributos flash para desplegar un mensaje legible en la plantilla HTML.

Para mantener una separación clara sin meter condicionales (if/else) sobre los encabezados HTTP Accept, se usaron dos manejadores acotados:

`GlobalRestExceptionHandler`: Restringido con annotations = RestController.class.

`ReservaWebExceptionHandler`: Restringido con assignableTypes = ReservaWebController.class.

Ambos comparten el mismo vocabulario de excepciones de dominio pero adaptan la respuesta a su respectivo cliente.


## Herramientas utilizadas
Java 17+, Spring Boot 3.2.x, Spring Data JPA, H2 Database, Thymeleaf

Apache Maven, Postman/curl, Git, GitHub

## Conclusiones
La implementación de esta arquitectura demostró que la separación rigurosa de responsabilidades permite construir aplicaciones desacopladas, mantenibles y extensibles. El desafío más exigente radicó en trazar la frontera exacta de las reglas de negocio para no caer en el antipatrón de Anemic Domain Model o en el extremo opuesto de sobrecargar los repositorios con lógica de dominio. Comprender que el Repository solo consulta datos y que el Service decide el flujo permitió que dos interfaces completamente distintas (`REST` y `MVC Thymeleaf`) compartieran la misma lógica de validación sin duplicar una sola línea de código. La gestión diferenciada de excepciones mediante `@ControllerAdvice` confirmó que el vocabulario del dominio debe ser único, dejando la adaptación del formato (JSON o HTML) como una preocupación exclusiva de la capa de presentación.
```