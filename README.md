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
