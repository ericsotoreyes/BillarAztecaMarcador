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


### Ajuste visual para partidas sin cronómetro

- Cuando una partida se configura **sin cronómetro**, el reloj desaparece completamente del área central.
- En su lugar se muestra el **logo de Club Billar Marcothon** ocupando el mismo espacio.
- Si la partida usa cronómetro, el logo se oculta y se muestra el reloj normalmente.


## Marcothon v9

- Corrige la navegación dentro de **Repetición instantánea**.
- Los botones **Anterior** y **Siguiente** del reproductor ahora cargan el fragmento temporal anterior o siguiente, respectivamente.
- Al llegar al primer o último fragmento se muestra un aviso y se mantiene el clip actual.
- La reproducción automática sigue avanzando al siguiente fragmento cuando termina el actual.
- La barra de progreso y reproducir/pausa siguen actuando sobre el fragmento actual.


### Reproducción a cámara lenta

- La ventana de **Repetición instantánea** incorpora un botón de velocidad.
- El botón alterna entre **1×** y **0.5×**.
- La velocidad elegida se conserva al pasar al fragmento anterior o siguiente y durante el avance automático entre fragmentos.


## Marcothon v10

- Corrige la visibilidad de los controles de repetición en modo horizontal.
- Se añade una fila fija sobre el video con **Anterior**, **Velocidad 1×/0.5×** y **Siguiente**.
- La altura del video se adapta a la pantalla para evitar que los controles queden fuera de la ventana.
- Anterior y Siguiente cambian realmente entre fragmentos temporales.
- La velocidad elegida se conserva al cambiar entre fragmentos.


## Marcothon v11

- La ventana de repetición prioriza el área de video.
- Se reducen márgenes, textos y altura de los controles superiores.
- Los botones **Anterior**, **Velocidad** y **Siguiente** usan tipografía y altura más compactas.
- El texto informativo inferior es más pequeño.
- El área del video crece de forma adaptativa en pantallas horizontales.
- El botón **Volver al marcador** también usa un formato más compacto.


## Marcothon v12

- En partidas **sin cronómetro**, los indicadores de extensiones de ambos jugadores se ocultan por completo.
- Ya no aparece el texto **“Sin reloj”**.
- Si una partida usa cronómetro, los indicadores de extensiones vuelven a mostrarse normalmente.


## Marcothon v13

- El reproductor superpuesto ya no aparece automáticamente al iniciar cada nuevo fragmento de repetición.
- Los controles de video siguen disponibles si el usuario toca el video manualmente.
- Los botones propios **Anterior**, **Velocidad** y **Siguiente** permanecen siempre visibles.


## Marcothon v14 — cronómetro con cámara (prueba en mesa)

Basada en `marcothon-v13`, commit `1e09376cc2f6d2286e3bcd4529f3024f80841478`.
Aplicación independiente: `com.clubmarcothon.marcador.v14`. No modifica ramas estables.

### Activación

1. En **Nuevo partido**, marca **Usar cronómetro** y **Cronómetro automático con cámara (prueba)**.
   También puedes abrir **Cámara · reloj** en el panel central de una partida con reloj.
2. Fija la cámara elevada en un extremo. Deben verse el paño completo y las tres bolas.
3. Marca las esquinas interiores del paño: lejana izquierda, lejana derecha, cercana derecha,
   cercana izquierda. La vista no está reflejada.
4. Comprueba que los tres círculos verdes corresponden a las bolas reales y pulsa **Activar**.
   No se permite activar si no se distinguen tres bolas durante dos segundos.
5. Inicia la partida. El reloj espera una lectura estable antes de comenzar.

### Comportamiento

- Una bola se desplaza: el reloj se pausa; no se consumen extensiones durante el movimiento.
- Tres bolas visibles y quietas durante 2 s tras un tiro observado: tiempo completo para el
  mismo jugador, sin sumar carambolas ni cambiar turno ni devolver extensiones usadas.
- Añadir puntos en este modo no reinicia el reloj; se puede registrar la serie al finalizar.
- **Fin de turno** sigue siendo manual; reinicia el reloj del rival y descarta el tiro anterior.
- **Pausa/Reanudar**, reinicio manual y Deshacer siguen disponibles. La cámara no anula
  una pausa manual ni una confirmación de falta. Al reanudar se adquiere una lectura nueva.
- Si faltan bolas, hay objetos ambiguos, una obstrucción grande o no llegan imágenes en
  800 ms, se mantiene el tiempo en pausa. Recuperar visión sin haber observado un tiro
  no otorga tiempo completo. Si el tiro ya estaba en curso, espera 2 s de quietud visible.
- Repeticiones, cambio de cámara, nuevo partido y segundo plano invalidan lecturas pendientes.
- Tras reiniciar la app, el control vuelve al modo manual; se debe comprobar y activar la
  cámara nuevamente. No se reutilizan calibraciones guardadas de otra posición.
- En **Cámara · reloj → Desactivar** vuelve al control manual conservando el tiempo restante.

### Alcance de esta primera implementación

Detección local por color y forma dentro de un cuadrilátero con corrección de perspectiva.
Admite roja/blanca/amarilla o roja/dos blancas. No reconoce carambolas ni bandas.
Una bola demasiado pequeña, poco iluminada, tapada o desenfocada puede impedir la lectura.
La configuración permite comprobarlo antes de activar. Si se mueve la cámara hay que
volver a configurarla. No se afirma precisión arbitral: hace falta probar con la mesa real.

Análisis y grabación comparten CameraX. El modo automático solicita calidad SD de grabación
para favorecer compatibilidad; si el dispositivo no admite ambas funciones simultáneas,
se conserva la repetición y se informa que se debe usar reloj manual. USB mantiene el
soporte anterior para cámaras que Android/CameraX expongan como externas; no añade un
controlador USB/UVC nuevo.

### Verificación

`gradle testDebugUnitTest assembleDebug --stacktrace`

Pruebas de movimiento, quietud, tiro repetido, rodamiento lento, oclusión, pérdida de
fotogramas, adquisición inicial sin reinicios, dos bolas blancas, segmentación sintética,
objetos extra, cambio de resolución y rechazo de esquinas inválidas.

Antes de usar en un partido, ensayar tiros rápidos/lentos, cerca de ambas bandas,
oclusiones, luz/reflejos, los dos jugadores, fin de turno, pausa/repetición y extensiones.


## Marcothon v15 — botones para sumar carambolas

- Basada en v14, conserva el reloj automático y las repeticiones.
- Cada jugador tiene **−1, +1, +3 y +5** en una sola fila, con texto adaptable.
- Las sumas solo se permiten para el jugador en turno durante una partida activa.
- Actualizan marcador, promedio y serie mayor. Cada pulsación se revierte con un Deshacer.
- En modo cámara no reinician el reloj; el cambio de turno sigue siendo manual.
- Si una suma alcanza la distancia configurada, se limita a esa distancia y finaliza la partida.
- Aplicación independiente `com.clubmarcothon.marcador.v15`; no modifica versiones anteriores.
