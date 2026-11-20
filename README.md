# Proyecto: agrega Swagger, DTOs, códigos HTTP y seguridad JWT

Este README es la guía de trabajo para el proyecto base. Sigue los pasos en
orden y modifica únicamente los archivos de `demo/` que se indican. Al
terminar, la API tendrá documentación OpenAPI, DTOs validados, respuestas HTTP
adecuadas y autorización por roles con JWT.

> **Política de acceso:** todos los endpoints `GET` son públicos. Las
> operaciones de escritura requieren rol, excepto el registro y login.
>
> - Productos y categorías: escrituras solo para `ADMIN`.
> - Comentarios e imágenes: escrituras para `USER` o `ADMIN`.
> - `GET /productos` y `GET /comentarios`, al igual que todos los demás GET,
>   no requieren token.

## Contexto del proyecto

- Spring Boot 3.3.4, Java 21, Spring MVC, Spring Data JPA y PostgreSQL.
- Código Java: `demo/src/main/java/com/example/demo/`.
- Configuración: `demo/src/main/resources/application.properties`.
- Pruebas: `demo/src/test/java/com/example/demo/`.
- Las entidades y controladores actuales reciben y devuelven directamente
  modelos JPA.

## Antes de comenzar

1. Revisa el README del proyecto y las entidades en `demo/src/main/java/com/example/demo/model/`.
2. Confirma que PostgreSQL esté disponible y que las propiedades existentes
   de conexión funcionen.
3. Desde el directorio `demo/`, ejecuta:

   ```bash
   ./mvnw test
   ```

4. Anota cualquier falla previa. No cambies las relaciones de base de datos
   como parte de este ejercicio.

La carpeta `../Spring/` contiene una versión resuelta para comparar después
de intentar los pasos. Usa primero esta guía y deja la comparación para el
final.

## Paso 1: Agrega Swagger y OpenAPI

### 1.1 Agrega Springdoc

En `demo/pom.xml`, agrega las siguientes dependencias. Spring Boot administra
las versiones de sus starters:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

No dupliques dependencias que ya existan. Springdoc 2.x es la línea para
Spring Boot 3.

### 1.2 Crea la configuración OpenAPI

Crea `demo/src/main/java/com/example/demo/OpenApiConfiguration.java`, dentro
del paquete base de la aplicación. Define un título y una versión para la API,
y un esquema HTTP Bearer con formato JWT para que Swagger UI pueda enviar
tokens:

```java
package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI apiDocumentation() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Spring Authenticacion y DTO")
                        .description("API de productos, categorías, comentarios e imágenes")
                        .version("2.0.0"))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

### 1.3 Documenta las operaciones

En cada controlador, agrega `@Tag` para agrupar operaciones y `@Operation`
para describir cada ruta. En las rutas protegidas, documenta también la
seguridad `bearerAuth`. Cada controlador y método deben conservar sus rutas
actuales salvo la corrección de imágenes del Paso 8.

Al finalizar, verifica:

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Springdoc inspecciona las rutas de Spring MVC, anotaciones y tipos de
solicitud/respuesta. Publica el documento OpenAPI en `/v3/api-docs`; Swagger
UI lee ese documento y presenta una interfaz para explorar y probar la API.

## Paso 2: Devuelve los códigos HTTP correctos

Actualiza los métodos CRUD de los cuatro controladores para que devuelvan
`ResponseEntity<T>`. No regreses `Optional` ni un `String` que contenga JSON.

| Operación | Resultado esperado |
|---|---|
| GET de colección existente | `200 OK`, con lista o arreglo vacío `[]`. |
| GET de recurso existente | `200 OK`, con el recurso. |
| GET de recurso inexistente | `404 Not Found`. |
| POST exitoso | `201 Created`, cuerpo y encabezado `Location`. |
| PUT exitoso | `200 OK`, cuerpo actualizado. No debe crear si no existe. |
| DELETE exitoso | `204 No Content`, sin cuerpo. |
| PUT o DELETE de ID inexistente | `404 Not Found`. |
| Cuerpo inválido después de agregar validación | `400 Bad Request`. |
| Solicitud protegida sin token válido | `401 Unauthorized`. |
| Token válido con rol insuficiente | `403 Forbidden`. |

Para una consulta, convierte el `Optional` del repositorio en respuesta:

```java
return repo.findById(id)
        .map(value -> ResponseEntity.ok(value))
        .orElseGet(() -> ResponseEntity.notFound().build());
```

Para creación, guarda primero para obtener el ID generado:

```java
Producto saved = repo.save(producto);
URI location = URI.create("/producto/" + saved.getId());
return ResponseEntity.created(location).body(saved);
```

Antes de PUT y DELETE, comprueba que el ID exista. No uses `202 Accepted`
para una eliminación inmediata; usa `204 No Content`.

## Paso 3: Define DTOs para separar API y entidades

Un DTO (*Data Transfer Object*) define qué campos puede enviar y recibir el
cliente. No permitas que el cliente controle el ID generado ni las relaciones
JPA (`producto`, categorías, comentarios o imágenes relacionados).

### 3.1 Crea DTOs de producto

Crea `demo/src/main/java/com/example/demo/dto/ProductoRequest.java`:

```java
package com.example.demo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductoRequest(
        @NotBlank String nombre,
        @NotBlank String descripcion,
        @NotBlank String lote,
        @NotNull @PositiveOrZero Float precioUnitario,
        @NotNull LocalDate fechaCaducidad) {
}
```

Usa `Float`, no `float`, en las entradas: el valor puede ser `null` y así la
validación puede detectar un dato omitido.

Crea `ProductoResponse.java` con el ID generado y los campos públicos del
producto. Agrega un método estático `from(Producto producto)` que convierta la
entidad en DTO. No incluyas sus colecciones JPA.

### 3.2 Crea DTOs para los demás recursos

En el mismo paquete `dto/`, agrega pares `Request` y `Response`:

| Recurso | Campos de entrada sugeridos | Validación mínima |
|---|---|---|
| Categoría | `nombre`, `descripcion`, `tipo`, `icono`, `imagenNavbar` | `nombre` no vacío. |
| Comentario | `username`, `comentario_corto`, `estrellas`, `comentario_largo`, `imagen1`, `imagen2` | Texto requerido; calificación de 1 a 5. |
| Imagen | `url`, `prioridad`, `tamanio`, `responsiva` | URL no vacía; prioridad no negativa. |

Cada `Response` debe exponer el ID y los campos adecuados, no la entidad
relacionada completa. Convierte a entidad mediante métodos como
`toEntity()` y actualiza una entidad ya cargada con métodos como
`updateEntity(...)`.

### 3.3 Migra los controladores a DTOs

En `ProductoController`, usa `ProductoRequest` para POST/PUT, y
`ProductoResponse` para las respuestas individuales y las listas. Aplica la
misma regla a `CategoriaController`, `ComentarioController` e
`ImagenController`.

Agrega `@Valid` a los cuerpos `@RequestBody`. Para las operaciones que reciben
un ID de producto, busca el producto en el servidor y asigna la relación; no
aceptes una entidad asociada desde el JSON.

Para las listas, mapea cada entidad:

```java
List<ProductoResponse> response = repo.findAll().stream()
        .map(ProductoResponse::from)
        .toList();
return ResponseEntity.ok(response);
```

Asegúrate de que las entidades tengan métodos de actualización para los
campos escalares necesarios. Cuando uses `@Builder` para una entidad que tiene
colecciones, inicializa esas colecciones con `@Builder.Default` y colecciones
vacías para evitar valores `null`.

## Paso 4: Diferencia PUT de PATCH

**PUT reemplaza todos los campos editables.** Requiere un cuerpo completo y
responde `404` cuando el recurso no existe.

**PATCH actualiza solo los campos enviados.** Crea
`ProductoPatchRequest.java` con campos opcionales (`String`, `Float`,
`LocalDate`) y validaciones que acepten `null` para representar “no enviado”.
Agrega un método `isEmpty()` para detectar un PATCH sin campos.

En `ProductoController`, agrega:

```java
@PatchMapping("producto/{id}")
public ResponseEntity<ProductoResponse> patchProducto(
        @PathVariable String id,
        @Valid @RequestBody ProductoPatchRequest patch) {
    // Rechaza patch vacío con 400.
    // Busca el producto; si falta, devuelve 404.
    // Cambia solo los campos no nulos, guarda y devuelve 200.
}
```

`@PathVariable` obtiene `id` de la URL; no significa PATCH. El nombre del
método HTTP es **PATCH**.

Prueba un cambio parcial:

```http
PATCH /producto/ID_PRODUCTO
Content-Type: application/json

{"nombre":"Nuevo nombre"}
```

Verifica que los demás valores no cambien. Esta estrategia interpreta `null`
como “no enviado”; por lo tanto, no permite borrar campos enviando `null`.

## Paso 5: Agrega cuentas persistidas

Crea la entidad `AppUser` en el paquete `model/` con:

- `id` generado;
- `username` único y obligatorio;
- `passwordHash` obligatorio;
- `role` obligatorio.

Crea `appUserRepo` en el paquete `repo/`, extendiendo
`JpaRepository<AppUser, String>`, con una búsqueda por username:

```java
Optional<AppUser> findByUsername(String username);
```

Hibernate creará/actualizará la tabla `app_users` de acuerdo con la
configuración JPA existente.

## Paso 6: Implementa registro, login y emisión del JWT

### 6.1 DTOs de autenticación

En `dto/`, crea:

- `RegisterRequest`: `username` obligatorio (3–50 caracteres), `password`
  obligatorio (8–100 caracteres).
- `LoginRequest`: username y password obligatorios.
- `UserResponse`: ID, username y rol; nunca la contraseña/hash.
- `AuthResponse`: `accessToken` y `tokenType`.

### 6.2 Registro

Crea `AuthController` con `POST /auth/register`. Comprueba si el username ya
existe (`409 Conflict`), codifica la contraseña con `BCryptPasswordEncoder`
y guarda siempre el rol `USER`. Ignora cualquier rol enviado por el cliente.
Devuelve `201 Created` y un `UserResponse`.

No agregues un endpoint público para crear administradores.

### 6.3 Login

Agrega `POST /auth/login`. Autentica las credenciales con
`AuthenticationManager`. Si son correctas, llama a `JwtTokenService` y
devuelve `200 OK` con:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer"
}
```

Si las credenciales no son válidas, devuelve `401 Unauthorized`.

### 6.4 Firma y contenido del JWT

Crea `service/JwtTokenService.java`. Usa un `JwtEncoder` y firma con HS256.
Incluye estos claims:

- `sub`: username;
- `roles`: roles sin el prefijo `ROLE_`;
- `iat`: hora de emisión;
- `exp`: expiración, por ejemplo una hora;
- `iss`: identificador de esta API.

El claim `roles` es el que permitirá autorizar las operaciones después.

## Paso 7: Configura Resource Server y permisos

Crea `SecurityConfig.java` en `com.example.demo`. Activa
`@EnableMethodSecurity`, deshabilita sesiones (stateless), configura el
`JwtEncoder`, `JwtDecoder`, `PasswordEncoder`, `AuthenticationManager` y
`JwtAuthenticationConverter`.

El conversor debe leer el claim `roles` y agregar el prefijo `ROLE_` para que
Spring reconozca `ADMIN` como `ROLE_ADMIN` y `USER` como `ROLE_USER`.

Configura las reglas del filtro en este orden:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
    .requestMatchers(HttpMethod.GET, "/**").permitAll()
    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .anyRequest().authenticated())
.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))
```

El matcher GET hace públicas **todas** las lecturas, incluyendo búsquedas por
ID y listas relacionadas. Las escrituras requieren un JWT.

Agrega `@PreAuthorize` a nivel de clase:

| Controlador | Regla |
|---|---|
| `ProductoController` | `hasRole('ADMIN')` |
| `CategoriaController` | `hasRole('ADMIN')` |
| `ComentarioController` | `hasAnyRole('USER', 'ADMIN')` |
| `ImagenController` | `hasAnyRole('USER', 'ADMIN')` |

Como el método GET debe poder sobreescribir la regla de clase, agrega
`@PreAuthorize("permitAll()")` en **cada método** anotado con `@GetMapping`.
Así las rutas GET son públicas aun si el controlador tiene autorización de
clase.

CSRF se desactiva porque la API es stateless y autentica operaciones de
escritura con Bearer tokens explícitos, no cookies de sesión. Usa HTTPS en
despliegues reales.

### Clave JWT

Configura opcionalmente `DEMO_JWT_SECRET` como clave aleatoria codificada en
Base64 de al menos 32 bytes. No confundas esta clave de firma con contraseñas
de usuario ni la subas al repositorio.

Si no defines la variable, el programa puede generar una clave aleatoria al
iniciar; esto sirve para desarrollo, pero los tokens dejarán de funcionar
después de reiniciar la aplicación. Para conservarlos durante pruebas locales:

```bash
export DEMO_JWT_SECRET="$(openssl rand -base64 32)"
```

No se necesitan variables de entorno `DEMO_ADMIN_PASSWORD` ni
`DEMO_USER_PASSWORD`: las cuentas se registran en la aplicación.

## Paso 8: Corrige la ruta GET duplicada de imágenes

El controlador base declara dos mappings GET con patrón `/imagen/{...}`:
buscar una imagen por ID y listar las imágenes de un producto. Spring MVC no
puede diferenciarlos por el nombre del argumento.

Mantén `GET /imagen/{id}` para una imagen y cambia la lista del producto a:

```text
GET /imagenes/producto/{productId}
```

Si la ruta de creación asociada a producto se conserva en
`POST /imagen/{productId}`, no entra en conflicto: el método HTTP es distinto.

## Paso 9: Documenta la seguridad en Swagger

Define `bearerAuth` como esquema HTTP Bearer con formato JWT en
`OpenApiConfiguration`. Agrega `@SecurityRequirement(name = "bearerAuth")` a
los controladores y `@SecurityRequirements` vacío en cada GET público para
que Swagger UI no solicite token en esas operaciones.

Las rutas de documentación deben estar permitidas en `SecurityConfig`. En
Swagger UI, usa **Authorize** y pega el token emitido por `/auth/login`.

## Paso 10: Prueba manualmente

Inicia PostgreSQL y la aplicación:

```bash
cd demo
./mvnw spring-boot:run
```

No se requieren credenciales de usuario configuradas en variables de entorno.
Registra una cuenta `USER` e inicia sesión:

```bash
curl -i -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"estudiante","password":"clave-local-segura-123"}'

curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"estudiante","password":"clave-local-segura-123"}'
```

Usa `accessToken` de la segunda respuesta:

```bash
curl -i http://localhost:8080/imagenes \
  -H 'Authorization: Bearer PEGA_AQUI_EL_TOKEN'
```

Comprueba que se puede hacer **cada GET sin token**:

```bash
curl -i http://localhost:8080/productos
curl -i http://localhost:8080/producto/ID_PRODUCTO
curl -i http://localhost:8080/comentarios
curl -i http://localhost:8080/comentarios/ID_PRODUCTO
curl -i http://localhost:8080/comentario/ID_COMENTARIO
curl -i http://localhost:8080/categorias
curl -i http://localhost:8080/categoria/ID_CATEGORIA
curl -i http://localhost:8080/categoria/producto/ID_PRODUCTO
curl -i http://localhost:8080/imagenes
curl -i http://localhost:8080/imagenes/producto/ID_PRODUCTO
curl -i http://localhost:8080/imagen/ID_IMAGEN
```

Los IDs inexistentes deben llegar al controlador y devolver `404`, no `401`.
Las colecciones vacías devuelven `200` con `[]`.

Para probar escrituras:

1. Sin token, intenta `POST /producto`: debe devolver `401`.
2. Usa el token de `USER` para intentar crear un producto: debe devolver
   `403`.
3. Usa el token `USER` en una escritura de comentarios o imágenes: debe
   permitirse si el cuerpo es válido.
4. Para probar permisos `ADMIN`, cambia el rol de una cuenta por un proceso
   administrativo confiable en la base de datos y vuelve a iniciar sesión:

   ```sql
   UPDATE app_users
   SET role = 'ADMIN'
   WHERE username = 'nombre-de-la-cuenta';
   ```

5. Usa el JWT nuevo para crear/actualizar/eliminar productos y categorías.
   No expongas una ruta pública de promoción de usuarios.

Prueba también entradas inválidas (`400`), ID inexistente (`404`), creación
exitosa (`201` y `Location`), actualización (`200`) y eliminación (`204`).

## Paso 11: Ejecuta las pruebas

Desde `demo/`:

```bash
./mvnw test
```

Agrega pruebas de integración que cubran:

- todos los endpoints GET responden sin token;
- una escritura protegida sin token responde `401`;
- un token de `USER` recibe `403` al escribir productos/categorías;
- `USER` o `ADMIN` puede escribir comentarios/imágenes;
- registro siempre crea rol `USER`;
- login emite JWT válido y uno inválido responde `401`;
- DTO inválido responde `400`;
- PATCH modifica solo los campos enviados;
- OpenAPI publica el esquema Bearer JWT.

## Checklist final

- [x] Swagger UI y `/v3/api-docs` cargan.
- [x] Los controladores usan DTOs en lugar de entidades JPA en sus contratos.
- [x] Las validaciones inválidas responden `400`.
- [x] PUT reemplaza campos editables y no crea recursos nuevos.
- [x] PATCH conserva los campos no enviados.
- [x] Todos los GET son públicos.
- [x] POST/PUT/PATCH/DELETE conservan autorización por rol.
- [x] Registro crea solo `USER`; login emite JWT Bearer.
- [x] La clave JWT se mantiene fuera del código.
- [x] No hay mappings GET duplicados para imágenes.
- [x] `./mvnw test` pasa.

## Referencia

La documentación OpenAPI toma como referencia
[Using Swagger 3 in Spring Boot 3](https://medium.com/@f.s.a.kuzman/using-swagger-3-in-spring-boot-3-c11a483ea6dc).
El flujo JWT se basa en conceptos de
[Implementing User Authentication in a Spring Boot Application](https://techoguide.medium.com/implementing-user-authentication-in-a-spring-boot-application-a-detailed-step-by-step-guide-f669f8943e1a).

---

## Apéndice: código completo de la solución

En cada paso crea o reemplaza el archivo en la ruta indicada, relativa a la
carpeta `demo/`. Los siguientes bloques corresponden a la solución funcional
del proyecto `../Spring/demo/`; no son pseudocódigo ni fragmentos aislados.
Las clases de entidades existentes (`Producto`, `Department`, `Comment` e
`Image`) conservan sus relaciones JPA. Solo se incluye el archivo de usuario
nuevo y las clases de DTO, configuración, autenticación y controladores que
se agregan o se reemplazan. También se incluyen las entidades que requieren
setters para actualización y valores iniciales para sus colecciones.

### A. Dependencias Maven

En `demo/pom.xml`, agrega las dependencias que falten dentro de
`<dependencies>`. No repitas las que ya estén en el archivo:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### B. OpenAPI

**Crear:** `demo/src/main/java/com/example/demo/OpenApiConfiguration.java`

```java
package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI apiDocumentation() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Spring Many-to-Many")
                        .description("API de productos, categorías, comentarios e imágenes")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

### C. DTOs de producto

**Crear:** `demo/src/main/java/com/example/demo/dto/ProductoRequest.java`

```java
package com.example.demo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductoRequest(
        @NotBlank String nombre,
        @NotBlank String descripcion,
        @NotBlank String lote,
        @NotNull @PositiveOrZero Float precioUnitario,
        @NotNull LocalDate fechaCaducidad) {
}
```

**Crear:** `demo/src/main/java/com/example/demo/dto/ProductoPatchRequest.java`

```java
package com.example.demo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductoPatchRequest(
        @Size(min = 1) String nombre,
        @Size(min = 1) String descripcion,
        @Size(min = 1) String lote,
        @PositiveOrZero Float precioUnitario,
        LocalDate fechaCaducidad) {

    public boolean isEmpty() {
        return nombre == null
                && descripcion == null
                && lote == null
                && precioUnitario == null
                && fechaCaducidad == null;
    }
}
```

**Crear:** `demo/src/main/java/com/example/demo/dto/ProductoResponse.java`

```java
package com.example.demo.dto;

import java.time.LocalDate;

import com.example.demo.model.Producto;

public record ProductoResponse(
        String id,
        String nombre,
        String descripcion,
        String lote,
        float precioUnitario,
        LocalDate fechaCaducidad) {

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getLote(),
                producto.getPrecioUnitario(),
                producto.getFechaCaducidad());
    }
}
```

### D. DTOs de categorías

**Crear:** `demo/src/main/java/com/example/demo/dto/CategoriaRequest.java`

```java
package com.example.demo.dto;

import com.example.demo.model.Department;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(
        @NotBlank String nombre,
        String descripcion,
        String tipo,
        String icono,
        String imagenNavbar) {

    public Department toEntity() {
        return Department.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .tipo(tipo)
                .icono(icono)
                .imagenNavbar(imagenNavbar)
                .build();
    }

    public void updateEntity(Department categoria) {
        categoria.setNombre(nombre);
        categoria.setDescripcion(descripcion);
        categoria.setTipo(tipo);
        categoria.setIcono(icono);
        categoria.setImagenNavbar(imagenNavbar);
    }
}
```

**Crear:** `demo/src/main/java/com/example/demo/dto/CategoriaResponse.java`

```java
package com.example.demo.dto;

import com.example.demo.model.Department;

public record CategoriaResponse(
        String id,
        String nombre,
        String descripcion,
        String tipo,
        String icono,
        String imagenNavbar) {

    public static CategoriaResponse from(Department categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getTipo(),
                categoria.getIcono(),
                categoria.getImagenNavbar());
    }
}
```

### E. DTOs de comentarios

**Crear:** `demo/src/main/java/com/example/demo/dto/ComentarioRequest.java`

```java
package com.example.demo.dto;

import com.example.demo.model.Comment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComentarioRequest(
        @NotBlank String username,
        @NotBlank String comentario_corto,
        @NotNull @Min(1) @Max(5) Short estrellas,
        String comentario_largo,
        String imagen1,
        String imagen2) {

    public Comment toEntity() {
        return Comment.builder()
                .username(username)
                .comentario_corto(comentario_corto)
                .estrellas(estrellas)
                .comentario_largo(comentario_largo)
                .imagen1(imagen1)
                .imagen2(imagen2)
                .build();
    }

    public void updateEntity(Comment comentario) {
        comentario.setUsername(username);
        comentario.setComentario_corto(comentario_corto);
        comentario.setEstrellas(estrellas);
        comentario.setComentario_largo(comentario_largo);
        comentario.setImagen1(imagen1);
        comentario.setImagen2(imagen2);
    }
}
```

**Crear:** `demo/src/main/java/com/example/demo/dto/ComentarioResponse.java`

```java
package com.example.demo.dto;

import com.example.demo.model.Comment;

public record ComentarioResponse(
        String id,
        String username,
        String comentario_corto,
        Short estrellas,
        String comentario_largo,
        String imagen1,
        String imagen2) {

    public static ComentarioResponse from(Comment comentario) {
        return new ComentarioResponse(
                comentario.getId(),
                comentario.getUsername(),
                comentario.getComentario_corto(),
                comentario.getEstrellas(),
                comentario.getComentario_largo(),
                comentario.getImagen1(),
                comentario.getImagen2());
    }
}
```

### F. DTOs de imágenes

**Crear:** `demo/src/main/java/com/example/demo/dto/ImagenRequest.java`

```java
package com.example.demo.dto;

import com.example.demo.model.Image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ImagenRequest(
        @NotBlank String url,
        @NotNull @PositiveOrZero Short prioridad,
        String tamanio,
        boolean responsiva) {

    public Image toEntity() {
        return Image.builder()
                .url(url)
                .prioridad(prioridad)
                .tamanio(tamanio)
                .responsiva(responsiva)
                .build();
    }

    public void updateEntity(Image imagen) {
        imagen.setUrl(url);
        imagen.setPrioridad(prioridad);
        imagen.setTamanio(tamanio);
        imagen.setResponsiva(responsiva);
    }
}
```

**Crear:** `demo/src/main/java/com/example/demo/dto/ImagenResponse.java`

```java
package com.example.demo.dto;

import com.example.demo.model.Image;

public record ImagenResponse(
        String id,
        String url,
        Short prioridad,
        String tamanio,
        boolean responsiva) {

    public static ImagenResponse from(Image imagen) {
        return new ImagenResponse(
                imagen.getId(),
                imagen.getUrl(),
                imagen.getPrioridad(),
                imagen.getTamanio(),
                imagen.isResponsiva());
    }
}
```

### G. DTOs de autenticación

**Crear:** `demo/src/main/java/com/example/demo/dto/RegisterRequest.java`

```java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password) {
}
```

**Crear:** `demo/src/main/java/com/example/demo/dto/LoginRequest.java`

```java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
```

**Crear:** `demo/src/main/java/com/example/demo/dto/UserResponse.java`

```java
package com.example.demo.dto;

public record UserResponse(String id, String username, String role) {
}
```

**Crear:** `demo/src/main/java/com/example/demo/dto/AuthResponse.java`

```java
package com.example.demo.dto;

public record AuthResponse(String accessToken, String tokenType) {
}
```

### H. Entidad y repositorio de usuario

**Crear:** `demo/src/main/java/com/example/demo/model/AppUser.java`

```java
package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    public AppUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
```

**Crear:** `demo/src/main/java/com/example/demo/repo/appUserRepo.java`

```java
package com.example.demo.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.AppUser;

public interface appUserRepo extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByUsername(String username);
}
```

### H.1 Entidades existentes que debes actualizar

Estos cuatro archivos ya existen en el proyecto base. Reemplaza su contenido
para que los DTOs puedan actualizar campos escalares y los builders
inicialicen correctamente las relaciones de colección.

**Reemplazar completo:** `demo/src/main/java/com/example/demo/model/Producto.java`

```java
package com.example.demo.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="producto")
@Entity
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private String id;

    @Setter
    private String nombre;

    @Setter
    private String descripcion;

    @Setter
    private String lote;

    @Setter
    private float precioUnitario;

    @Setter
    private LocalDate fechaCaducidad;

    @JsonManagedReference
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    @Setter
    private Set<Comment> comentarios = new HashSet<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    @Setter
    private Set<Image> imagenes = new HashSet<>();

    @JsonBackReference
    @ManyToMany(mappedBy = "productos", fetch = FetchType.EAGER)
    @Builder.Default
    @Setter
    private Set<Department> deparments = new HashSet<>();
}
```

**Reemplazar completo:** `demo/src/main/java/com/example/demo/model/Department.java`

```java
package com.example.demo.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="Categorias")
@Entity
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private String id;

    @Setter
    private String nombre;

    @Setter
    private String descripcion;

    @Setter
    private String tipo;

    @Setter
    private String icono;

    @Setter
    private String imagenNavbar;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "producto_deparments",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "deparments_id")
    )
    @Builder.Default
    @Setter
    private Set<Producto> productos = new HashSet<>();
}
```

**Reemplazar completo:** `demo/src/main/java/com/example/demo/model/Comment.java`

```java
package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="comentarios")
@Entity
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private String id;

    @Setter
    private String username;

    @Setter
    private String comentario_corto;

    @Setter
    private Short estrellas;

    @Setter
    private String comentario_largo;

    @Setter
    private String imagen1;

    @Setter
    private String imagen2;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @Setter
    private Producto producto;
}
```

**Reemplazar completo:** `demo/src/main/java/com/example/demo/model/Image.java`

```java
package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="imagenes")
@Entity
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private String id;

    @Setter
    private String url;

    @Setter
    private Short prioridad;

    @Setter
    private String tamanio;

    @Setter
    private boolean responsiva;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @Setter
    private Producto producto;
}
```

### H.2 Configuración de la clave JWT

En `demo/src/main/resources/application.properties`, agrega esta línea:

```properties
demo.security.jwt-secret=${DEMO_JWT_SECRET:}
```

La propiedad permite que Spring lea `DEMO_JWT_SECRET`; si queda vacía,
`SecurityConfig` genera una clave aleatoria de desarrollo al iniciar.

### I. Emisión de JWT

**Crear:** `demo/src/main/java/com/example/demo/service/JwtTokenService.java`

```java
package com.example.demo.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final long TOKEN_LIFETIME_SECONDS = 3600;

    private final JwtEncoder jwtEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String createToken(Authentication authentication) {
        Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_")
                        ? authority.substring("ROLE_".length())
                        : authority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("spring-many-to-many-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_LIFETIME_SECONDS))
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
```

### J. Controlador de autenticación

**Crear:** `demo/src/main/java/com/example/demo/controllers/AuthController.java`

```java
package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.model.AppUser;
import com.example.demo.repo.appUserRepo;
import com.example.demo.service.JwtTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y emisión de JWT")
public class AuthController {

    @Autowired
    appUserRepo users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenService jwtTokenService;

    @PostMapping("register")
    @Operation(summary = "Registrar una cuenta de usuario")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (users.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        AppUser account = users.save(new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                "USER"));
        return ResponseEntity.status(201)
                .body(new UserResponse(account.getId(), account.getUsername(), account.getRole()));
    }

    @PostMapping("login")
    @Operation(summary = "Iniciar sesión y obtener un JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            request.username(), request.password()));
            String token = jwtTokenService.createToken(authentication);
            return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(401).build();
        }
    }
}
```

### K. Controlador de productos

**Reemplazar completo:** `demo/src/main/java/com/example/demo/controllers/ProductoController.java`

```java
package com.example.demo.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ProductoPatchRequest;
import com.example.demo.dto.ProductoRequest;
import com.example.demo.dto.ProductoResponse;
import com.example.demo.model.Producto;
import com.example.demo.repo.productoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Productos", description = "Operaciones para consultar y administrar productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {

    @Autowired
    productoRepo repo;

    @GetMapping("productos")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar productos")
    public ResponseEntity<List<ProductoResponse>> findAllProductos() {
        List<ProductoResponse> response = repo.findAll().stream()
                .map(ProductoResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("producto")
    @Operation(summary = "Crear producto")
    public ResponseEntity<ProductoResponse> addProducto(@Valid @RequestBody ProductoRequest request) {
        Producto producto = Producto.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .lote(request.lote())
                .precioUnitario(request.precioUnitario())
                .fechaCaducidad(request.fechaCaducidad())
                .build();

        Producto saved = repo.save(producto);
        URI location = URI.create("/producto/" + saved.getId());
        return ResponseEntity.created(location).body(ProductoResponse.from(saved));
    }

    @GetMapping("producto/{id}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Buscar producto por ID")
    public ResponseEntity<ProductoResponse> getById(@PathVariable String id) {
        return repo.findById(id)
                .map(producto -> ResponseEntity.ok(ProductoResponse.from(producto)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("producto/{id}")
    @Operation(summary = "Reemplazar producto")
    public ResponseEntity<ProductoResponse> putProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductoRequest request) {
        return repo.findById(id)
                .map(producto -> {
                    updateProducto(producto, request);
                    return ResponseEntity.ok(ProductoResponse.from(repo.save(producto)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("producto/{id}")
    @Operation(summary = "Actualizar parcialmente un producto")
    public ResponseEntity<ProductoResponse> patchProducto(
            @PathVariable String id,
            @Valid @RequestBody ProductoPatchRequest patch) {
        if (patch.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        return repo.findById(id)
                .map(producto -> {
                    applyPatch(producto, patch);
                    return ResponseEntity.ok(ProductoResponse.from(repo.save(producto)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("producto/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<Void> deleteProducto(@PathVariable String id) {
        if (repo.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void updateProducto(Producto producto, ProductoRequest request) {
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setLote(request.lote());
        producto.setPrecioUnitario(request.precioUnitario());
        producto.setFechaCaducidad(request.fechaCaducidad());
    }

    private void applyPatch(Producto producto, ProductoPatchRequest patch) {
        if (patch.nombre() != null) {
            producto.setNombre(patch.nombre());
        }
        if (patch.descripcion() != null) {
            producto.setDescripcion(patch.descripcion());
        }
        if (patch.lote() != null) {
            producto.setLote(patch.lote());
        }
        if (patch.precioUnitario() != null) {
            producto.setPrecioUnitario(patch.precioUnitario());
        }
        if (patch.fechaCaducidad() != null) {
            producto.setFechaCaducidad(patch.fechaCaducidad());
        }
    }
}
```

### L. Controlador de categorías

**Reemplazar completo:** `demo/src/main/java/com/example/demo/controllers/CategoriaController.java`

```java
package com.example.demo.controllers;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CategoriaRequest;
import com.example.demo.dto.CategoriaResponse;
import com.example.demo.model.Department;
import com.example.demo.repo.categoriaRepo;
import com.example.demo.repo.productoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Categorías", description = "Operaciones para consultar y administrar categorías")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {

    @Autowired
    categoriaRepo repo;

    @Autowired
    productoRepo repoProducto;

    @GetMapping("categorias")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar categorías")
    public ResponseEntity<List<CategoriaResponse>> findAllCategoria() {
        List<CategoriaResponse> response = repo.findAll().stream()
                .map(CategoriaResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("categoria")
    @Operation(summary = "Crear categoría")
    public ResponseEntity<CategoriaResponse> addCategoria(@Valid @RequestBody CategoriaRequest request) {
        Department saved = repo.save(request.toEntity());
        URI location = URI.create("/categoria/" + saved.getId());
        return ResponseEntity.created(location).body(CategoriaResponse.from(saved));
    }

    @PostMapping("addProductoCategoria/{productId}/{categoriaId}")
    @Operation(summary = "Asociar un producto con una categoría")
    public ResponseEntity<CategoriaResponse> addProductoCategoria(
            @PathVariable String productId,
            @PathVariable String categoriaId) {
        var producto = repoProducto.findById(productId).orElse(null);
        var categoria = repo.findById(categoriaId).orElse(null);
        if (producto == null || categoria == null) {
            return ResponseEntity.notFound().build();
        }

        categoria.getProductos().add(producto);
        producto.getDeparments().add(categoria);
        Department saved = repo.save(categoria);
        return ResponseEntity.ok(CategoriaResponse.from(saved));
    }

    @GetMapping("categoria/producto/{productId}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar categorías asociadas a un producto")
    public ResponseEntity<Set<CategoriaResponse>> getDepartmentsByProduct(@PathVariable String productId) {
        return repoProducto.findById(productId)
                .map(producto -> producto.getDeparments().stream()
                        .map(CategoriaResponse::from)
                        .collect(Collectors.toSet()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("categoria/{id}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Buscar categoría por ID")
    public ResponseEntity<CategoriaResponse> getById(@PathVariable String id) {
        return repo.findById(id)
                .map(categoria -> ResponseEntity.ok(CategoriaResponse.from(categoria)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("categoria/{id}")
    @Operation(summary = "Reemplazar categoría")
    public ResponseEntity<CategoriaResponse> putDepartment(
            @PathVariable String id,
            @Valid @RequestBody CategoriaRequest request) {
        return repo.findById(id)
                .map(categoria -> {
                    request.updateEntity(categoria);
                    return ResponseEntity.ok(CategoriaResponse.from(repo.save(categoria)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("categoria/{id}")
    @Operation(summary = "Eliminar categoría")
    public ResponseEntity<Void> deleteCategoria(@PathVariable String id) {
        if (repo.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

### M. Controlador de comentarios

**Reemplazar completo:** `demo/src/main/java/com/example/demo/controllers/ComentarioController.java`

```java
package com.example.demo.controllers;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ComentarioRequest;
import com.example.demo.dto.ComentarioResponse;
import com.example.demo.repo.comentarioRepo;
import com.example.demo.repo.productoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Validated
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Comentarios", description = "Operaciones para consultar y administrar comentarios")
@SecurityRequirement(name = "bearerAuth")
public class ComentarioController {

    @Autowired
    comentarioRepo repo;

    @Autowired
    productoRepo repoProducto;

    @GetMapping("comentarios")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar comentarios")
    public ResponseEntity<List<ComentarioResponse>> findAllComentarios() {
        List<ComentarioResponse> response = repo.findAll().stream()
                .map(ComentarioResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("comentario")
    @Operation(summary = "Crear comentario")
    public ResponseEntity<ComentarioResponse> addComentario(@Valid @RequestBody ComentarioRequest request) {
        var saved = repo.save(request.toEntity());
        URI location = URI.create("/comentario/" + saved.getId());
        return ResponseEntity.created(location).body(ComentarioResponse.from(saved));
    }

    @PostMapping("comentario/{productId}")
    @Operation(summary = "Crear comentario para un producto")
    public ResponseEntity<ComentarioResponse> addComentario(
            @Valid @RequestBody ComentarioRequest request,
            @PathVariable String productId) {
        return repoProducto.findById(productId)
                .map(producto -> {
                    var comentario = request.toEntity();
                    comentario.setProducto(producto);
                    var saved = repo.save(comentario);
                    URI location = URI.create("/comentario/" + saved.getId());
                    return ResponseEntity.created(location).body(ComentarioResponse.from(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("comentarios/{productId}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar comentarios de un producto")
    public ResponseEntity<Set<ComentarioResponse>> getCommentsByProduct(@PathVariable String productId) {
        return repoProducto.findById(productId)
                .map(producto -> producto.getComentarios().stream()
                        .map(ComentarioResponse::from)
                        .collect(Collectors.toSet()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("comentario/{id}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Buscar comentario por ID")
    public ResponseEntity<ComentarioResponse> getById(@PathVariable String id) {
        return repo.findById(id)
                .map(comentario -> ResponseEntity.ok(ComentarioResponse.from(comentario)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("comentario/{id}")
    @Operation(summary = "Reemplazar comentario")
    public ResponseEntity<ComentarioResponse> putComentario(
            @PathVariable String id,
            @Valid @RequestBody ComentarioRequest request) {
        return repo.findById(id)
                .map(comentario -> {
                    request.updateEntity(comentario);
                    return ResponseEntity.ok(ComentarioResponse.from(repo.save(comentario)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("comentario/{id}")
    @Operation(summary = "Eliminar comentario")
    public ResponseEntity<Void> deleteComentario(@PathVariable String id) {
        if (repo.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

### N. Controlador de imágenes

**Reemplazar completo:** `demo/src/main/java/com/example/demo/controllers/ImagenController.java`

```java
package com.example.demo.controllers;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ImagenRequest;
import com.example.demo.dto.ImagenResponse;
import com.example.demo.repo.imagenRepo;
import com.example.demo.repo.productoRepo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Validated
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Imágenes", description = "Operaciones para consultar y administrar imágenes")
@SecurityRequirement(name = "bearerAuth")
public class ImagenController {

    @Autowired
    imagenRepo repo;

    @Autowired
    productoRepo repoProducto;

    @GetMapping("imagenes")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar imágenes")
    public ResponseEntity<List<ImagenResponse>> findAllImagenes() {
        List<ImagenResponse> response = repo.findAll().stream()
                .map(ImagenResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("imagen")
    @Operation(summary = "Crear imagen")
    public ResponseEntity<ImagenResponse> addImagen(@Valid @RequestBody ImagenRequest request) {
        var saved = repo.save(request.toEntity());
        URI location = URI.create("/imagen/" + saved.getId());
        return ResponseEntity.created(location).body(ImagenResponse.from(saved));
    }

    @PostMapping("imagen/{productId}")
    @Operation(summary = "Crear imagen para un producto")
    public ResponseEntity<ImagenResponse> addImagen(
            @Valid @RequestBody ImagenRequest request,
            @PathVariable String productId) {
        return repoProducto.findById(productId)
                .map(producto -> {
                    var imagen = request.toEntity();
                    imagen.setProducto(producto);
                    var saved = repo.save(imagen);
                    URI location = URI.create("/imagen/" + saved.getId());
                    return ResponseEntity.created(location).body(ImagenResponse.from(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("imagenes/producto/{productId}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Listar imágenes de un producto")
    public ResponseEntity<Set<ImagenResponse>> getImagenesByProduct(@PathVariable String productId) {
        return repoProducto.findById(productId)
                .map(producto -> producto.getImagenes().stream()
                        .map(ImagenResponse::from)
                        .collect(Collectors.toSet()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("imagen/{id}")
    @PreAuthorize("permitAll()")
    @SecurityRequirements
    @Operation(summary = "Buscar imagen por ID")
    public ResponseEntity<ImagenResponse> getById(@PathVariable String id) {
        return repo.findById(id)
                .map(imagen -> ResponseEntity.ok(ImagenResponse.from(imagen)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("imagen/{id}")
    @Operation(summary = "Reemplazar imagen")
    public ResponseEntity<ImagenResponse> putImagen(
            @PathVariable String id,
            @Valid @RequestBody ImagenRequest request) {
        return repo.findById(id)
                .map(imagen -> {
                    request.updateEntity(imagen);
                    return ResponseEntity.ok(ImagenResponse.from(repo.save(imagen)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("imagen/{id}")
    @Operation(summary = "Eliminar imagen")
    public ResponseEntity<Void> deleteImagen(@PathVariable String id) {
        if (repo.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

### O. Configuración completa de Spring Security

**Crear:** `demo/src/main/java/com/example/demo/SecurityConfig.java`

```java
package com.example.demo;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.example.demo.model.AppUser;
import com.example.demo.repo.appUserRepo;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(appUserRepo users) {
        return username -> {
            AppUser account = users.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("No existe la cuenta"));
            return User.withUsername(account.getUsername())
                    .password(account.getPasswordHash())
                    .roles(account.getRole())
                    .build();
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${demo.security.jwt-secret:}") String configuredSecret) {
        byte[] keyBytes;
        if (configuredSecret.isBlank()) {
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            logger.warn("No DEMO_JWT_SECRET configured; JWTs will be invalidated when the app restarts.");
        } else {
            try {
                keyBytes = Base64.getDecoder().decode(configuredSecret);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException(
                        "DEMO_JWT_SECRET must be a Base64-encoded key with at least 32 bytes.", exception);
            }
            if (keyBytes.length < 32) {
                throw new IllegalStateException("DEMO_JWT_SECRET must contain at least 32 bytes.");
            }
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

En el código, `GET /**` se permite antes de la regla general de autenticación.
Las anotaciones `@PreAuthorize` de cada controlador asignan roles a los
métodos de escritura; cada método GET contiene `@PreAuthorize("permitAll()")`
para mantener públicas también las lecturas cuando la clase tiene una regla
de rol.

### P. Ejecución y comprobación después de copiar los archivos

Desde `demo/`, compila y ejecuta las pruebas:

```bash
./mvnw test
```

Inicia la aplicación:

```bash
./mvnw spring-boot:run
```

Abre <http://localhost:8080/swagger-ui/index.html>. Registra e inicia sesión
con `POST /auth/register` y `POST /auth/login`, copia `accessToken` y utiliza
el botón **Authorize** de Swagger UI para probar las escrituras. Todos los
GET deben funcionar también en una ventana nueva o `curl` sin token.

Compara al final los archivos con sus equivalentes en `../Spring/demo/`.
Si algún nombre de entidad, campo o ruta difiere en la versión base, conserva
el esquema real del proyecto y adapta los mapeos DTO; no reemplaces las
relaciones de base de datos por las de otro proyecto.
