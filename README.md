# Billar Azteca Marcador

Aplicación Android táctil para llevar el marcador de billar en Billar Azteca Club.

## Versiones conservadas

- `v3-stable`: versión funcional original.
- `v4-stable`: versión visual anterior.
- `v5-stable`: versión v5 anterior.
- `v6-stable`: versión funcional con logo visible.
- `v7-stable`: versión visual refinada anterior.

## v8

La v8 conserva exactamente el diseño y la lógica de la v7, pero reemplaza el PNG del logo por un archivo compacto y validado para evitar que Android ignore un recurso PNG dañado.

- Logo real de Billar Azteca con transparencia exterior.
- Tamaño y alineación del encabezado iguales a v7.
- Sin cambios en marcador, cronómetro, turnos, promedio, serie mayor, entradas, pausa, deshacer ni nuevo partido.


## v9

- Basada en la v8 estable.
- Tocar el nombre de la mesa permite editarlo.
- Tocar el nombre de cada jugador permite editarlo directamente.
- Tocar “Partido a …” permite cambiar la distancia.
- Pulsación larga sobre el cronómetro permite cambiar los segundos por tiro; toque normal reinicia el reloj.
- Nombres largos se reducen automáticamente para caber en el panel.
- El botón Pausa se vuelve amarillo al pausar y el cronómetro muestra PAUSA.
- Al llegar a 0, el aro queda rojo y muestra TIEMPO.
- Confirmación antes de borrar una partida ya iniciada.
- Cada jugador dispone de 2 extensiones por partida.
- La extensión se solicita desde el panel del jugador activo; al llegar el reloj a 0, se reinicia automáticamente una sola vez.
- El contador de extensiones restantes se muestra en cada panel y se reinicia a 2 al comenzar un nuevo partido.
- Deshacer también restaura una solicitud de extensión.


## v10

- Basada en la v9 estable.
- Al alcanzar la distancia del partido, el cronómetro se detiene inmediatamente en el tiempo que tenga en ese momento.
- Ya no se reinicia el reloj después de la carambola ganadora.
- Se eliminó la opción “Continuar” del aviso de partido terminado para evitar que el cronómetro vuelva a correr accidentalmente.
- Desde el aviso final se puede cerrar o iniciar un nuevo partido.


## v11

- Basada en la v10 estable.
- Al abrir la app, el cronómetro permanece detenido.
- El botón central de control muestra “INICIAR PARTIDA” hasta que el operador decide comenzar.
- Antes de iniciar se pueden capturar o editar mesa, jugadores, distancia y tiempo sin que corra el reloj.
- Al pulsar “INICIAR PARTIDA”, el cronómetro comienza desde el tiempo configurado.
- Después de iniciar, ese mismo control funciona como Pausa / Reanudar.
- Al preparar un nuevo partido, la app vuelve a quedar en estado LISTO y el reloj permanece detenido.
- Al llegar el cronómetro a cero se reproduce un beep de alerta.
- Si había una extensión activa, suena el beep al llegar a cero y luego el reloj se reinicia automáticamente una vez.
- El estado del cronómetro muestra LISTO antes de empezar y FINAL al terminar la partida.


## v12

- Basada en la v11 estable.
- Al preparar un nuevo partido se puede elegir “Usar cronómetro” o jugar sin reloj.
- En modo sin cronómetro, el círculo central muestra “SIN RELOJ”.
- En modo sin cronómetro no hay cuenta regresiva, beep, pausa, reinicio de reloj ni extensiones.
- El botón “INICIAR PARTIDA” se conserva para permitir capturar nombres y configuración antes de comenzar.
- Durante una partida sin cronómetro el botón central indica “PARTIDA EN CURSO”.
- Puntos, entradas, promedio, serie mayor, fin de turno, deshacer y fin de partida funcionan normalmente en ambos modos.


## v13

- Basada en la v12 estable.
- En la esquina superior derecha se reemplazó el rótulo “MARCADOR” por un botón rojo con una **X**, inspirado en el control de cierre de una ventana de Windows.
- Al tocar la X, la app pregunta si se desea cerrar el marcador.
- Si se confirma, la aplicación se cierra.
- Si se cancela, la partida continúa sin cambios.
- No se modificó ninguna otra función del marcador.
