# API REST de Gestión de Reservas de Laboratorios (`reservas-labs-api`)

Proyecto desarrollado con **Spring Boot 3.2.x** y **Java 17** para la gestión y reserva de espacios físicos de laboratorios universitarios. La aplicación implementa arquitectura en capas, persistencia con Spring Data JPA, validaciones de dominio y manejo centralizado de excepciones HTTP.

---

## 🛠️ Tecnologías y Dependencias

- **Java**: 17
- **Spring Boot**: 3.2.x
- **Build Tool**: Maven
- **Spring Data JPA & Hibernate**: Persistencia de datos.
- **H2 Database**: Base de datos en memoria para entorno de desarrollo y pruebas.
- **Lombok**: Reducción de código repetitivo (Getters, Setters, Constructor con/sin argumentos).
- **Validation (Hibernate Validator)**: Validación de entrada con anotaciones `@Valid`.

---

## 🏗️ Estructura del Proyecto

```text
src/main/java/com/universidad/reservaslabs/
├── ReservasLabsApiApplication.java
├── controller/
│   ├── LaboratorioController.java
│   └── ReservaController.java
├── exception/
│   ├── GlobalRestExceptionHandler.java
│   ├── RecursoNoEncontradoException.java
│   └── ReservaConflictException.java
├── model/
│   ├── EstadoReserva.java
│   ├── Laboratorio.java
│   └── Reserva.java
├── repository/
│   ├── LaboratorioRepository.java
│   └── ReservaRepository.java
└── service/
    └── LaboratorioService.java
    └── ReservaService.java
```

---

## 📐 Decisiones de Diseño y Arquitectura

### 1. Manejo de Solapamientos (Repository vs Service)
- **Consulta en SQL:** El método `ReservaRepository.buscarSolapamientos(...)` utiliza una consulta JPQL con la condición `r.inicio < :fin AND r.fin > :inicio` para filtrar desde el motor de base de datos las reservas que se cruzan con el horario solicitado. Esto evita cargar todo el histórico de reservas a memoria de Java, garantizando escalabilidad a medida que el sistema crece.
- **Responsabilidad del Service:** Si el controlador llamara directamente a `buscarSolapamientos()` omitiendo la capa de servicio, el controlador asumiría reglas de negocio que no le corresponden. El `Repository` responde la pregunta de datos (*"¿Existen reservas activas en ese rango?"*), mientras que el `ReservaService` evalúa la decisión de dominio (*"Si existen coincidencias, bloquear la creación y lanzar una `ReservaConflictException`"*).

### 2. Clasificación de Reglas de Negocio
- **Reglas sin estado (Pure Java en Service):** La validación de horario de atención (07:00 - 21:00) y de duración permitida (entre 30 minutos y 3 horas) no requieren consultar la base de datos. Se ejecutan directamente en `ReservaService.validarHorarioYDuracion()` utilizando la API de `java.time`, ahorrando consultas innecesarias.
- **Reglas con estado (Apoyo en Repository):** Validaciones que dependen de otras entidades persistidas (como verificar si un laboratorio existe, si tiene solapamientos o si una reserva ya inició) se ejecutan combinando consultas de `ReservaRepository` / `LaboratorioRepository` con la lógica de negocio del servicio.

### 3. Excepción intencional: `LaboratorioController`
- `LaboratorioController` accede directamente a `LaboratorioRepository` porque la gestión de laboratorios representa un CRUD básico sin reglas de negocio complejas en esta fase. Introducir un `LaboratorioService` sin validaciones adicionales habría resultado en el antipatrón de **Service anémico** (un servicio que solo sirve de *passthrough*). La capa `Service` se justifica únicamente cuando existen reglas de dominio que aplicar.

### 4. Aislamiento de Excepciones HTTP (`@RestControllerAdvice`)
- La anotación `@RestControllerAdvice(annotations = RestController.class)` delimita el manejo de excepciones únicamente a las clases marcadas con `@RestController`. Esto garantiza que los errores retornen respuestas en JSON estructurado (`400`, `404`, `409`) sin interferir con futuros controladores MVC de vistas HTML.

---

## 📌 Endpoints de la API REST

### Laboratorios
- `GET /api/laboratorios` — Listar todos los laboratorios.
- `GET /api/laboratorios/{id}` — Obtener detalle de un laboratorio.
- `POST /api/laboratorios` — Crear un nuevo laboratorio.

### Reservas
- `GET /api/reservas` — Listar todas las reservas.
- `GET /api/reservas/{id}` — Obtener detalle de una reserva.
- `GET /api/reservas/laboratorio/{laboratorioId}` — Obtener reservas por laboratorio.
- `POST /api/reservas` — Crear una reserva (Aplica reglas de negocio).
- `DELETE /api/reservas/{id}` — Cancelar una reserva.