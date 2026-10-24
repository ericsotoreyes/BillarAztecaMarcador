# Club Billar Marcothon — Marcador v1

Versión independiente creada a partir de la plantilla funcional v12 del marcador.

## Identidad

- Club: **Club Billar Marcothon**
- Colores principales: negro, blanco, verde, rojo y amarillo.
- Logo basado en la referencia proporcionada por el club.
- Aplicación independiente con `applicationId com.clubmarcothon.marcador`.

## Funciones conservadas de la plantilla v12

- Marcador para dos jugadores.
- Nombres de jugadores editables directamente.
- Nombre de mesa editable.
- Distancia del partido configurable.
- Modo **con cronómetro** o **sin cronómetro**.
- Botón **INICIAR PARTIDA** para evitar que el reloj corra durante la preparación.
- Cronómetro configurable por tiro.
- Beep al llegar a cero.
- Dos extensiones de tiempo por jugador cuando se usa reloj.
- Promedio automático.
- Serie mayor.
- Entradas.
- +1 / −1.
- Fin de turno.
- Pausa / reanudar.
- Reiniciar reloj.
- Deshacer.
- Nuevo partido.
- Detención automática al terminar el partido.

## Versión

- Version code: 1
- Version name: 1.0.0
- Rama: `marcothon-v1`


## Marcothon v2

Cambios exclusivamente visuales sobre Marcothon v1:

- Logo del club ligeramente más grande en el encabezado.
- El borde amarillo del jugador activo se conserva sin cambios.
- El botón **FIN DE TURNO** ahora tiene fondo amarillo y texto negro.
- La lógica y todas las funciones permanecen intactas.
- Aplicación independiente: `com.clubmarcothon.marcador.v2`.


## Marcothon v3

- Basada en Marcothon v2.
- El rótulo “MARCADOR” de la esquina superior derecha fue reemplazado por un botón rojo con una **X blanca**, estilo ventana de Windows.
- Al presionar la X, se solicita confirmación antes de cerrar la aplicación.
- Si se confirma, la app se cierra; si se cancela, continúa normalmente.
- No se modificó ninguna otra lógica ni función.


## Marcothon v4

- Basada en Marcothon v3.
- Se aumentó al máximo práctico el tamaño del número principal de carambolas de cada jugador.
- El número utiliza autoajuste de tamaño para aprovechar el espacio disponible sin cortarse ni invadir otros elementos.
- Se conservan sin cambios nombres, promedio, serie mayor, entrada, cronómetro, controles y toda la lógica del marcador.


## Marcothon v5

- Basada en Marcothon v4.
- Agrega recuperación automática local de partidas interrumpidas.
- Guarda jugadores, marcador, entradas, series, turno, distancia, extensiones, modo de reloj y tiempo restante.
- Los cambios importantes se guardan inmediatamente; el tiempo restante se actualiza periódicamente.
- Si la tablet se apaga, Android cierra la app o hay un corte de energía, al volver a abrir se ofrece continuar o descartar la partida recuperada.
- Si había cronómetro activo, siempre se recupera en pausa para evitar descontar tiempo mientras la tablet estuvo apagada.
- Al terminar una partida normalmente se elimina el respaldo.
- No requiere internet y no cambia la lógica de juego existente.


## Marcothon v6

- Basada en Marcothon v5 estable.
- En **Nuevo partido**, la opción **Usar cronómetro** aparece desmarcada por defecto.
- Los segundos por tiro permanecen deshabilitados hasta seleccionar **Usar cronómetro**.
- La ventana **Nuevo partido** ahora tiene desplazamiento vertical para que todos los campos sean visibles en distintas resoluciones.
- Al abrir el flujo de **Nuevo partido**, el cronómetro activo se pausa inmediatamente.
- Si el usuario cancela el cambio de partido, el cronómetro anterior se reanuda desde donde estaba, sin descontar el tiempo empleado en la ventana.
- No se modificó ninguna otra lógica del marcador.


## Marcothon v7 (en desarrollo)

- Desde 10 segundos restantes, el cronómetro emite un bip cada segundo hasta llegar a cero.
- Al agotarse el tiempo definitivo, emite un bip más fuerte y pausa el reloj.
- Se abre una confirmación de falta por tiempo.
- Si se confirma la falta, cambia el turno al rival y reinicia el cronómetro.
- Si se cancela, el mismo jugador conserva el turno y el cronómetro se reinicia.
- Las extensiones existentes se respetan: si una extensión estaba activada, el primer cero consume la extensión y la confirmación de falta solo aparece al agotarse el tiempo definitivo.


### Ajuste adicional de extensiones automáticas

- Las extensiones ya no necesitan activarse manualmente.
- Al llegar el reloj a **0**, si el jugador todavía tiene extensiones disponibles, suena un bip, se descuenta automáticamente una extensión y el cronómetro reinicia con el tiempo completo.
- Esto ocurre con la primera y la segunda extensión.
- Cuando el jugador ya agotó sus dos extensiones, el siguiente **0** emite el bip fuerte y abre la confirmación de falta por tiempo.
- Los indicadores de extensión quedan como información de cuántas extensiones restan.
