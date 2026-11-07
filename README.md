# Fútbol 11 Arcade (tp15)

Swing + FlatLaf + Maven (Java 21).

## Cómo correrlo
    mvn compile exec:java        # corre el juego directo
    mvn package                  # genera target/futbol11-arcade-1.0.jar
    java -jar target/futbol11-arcade-1.0.jar

## Cómo se juega
1. Poné tu nombre de DT y tocá JUGAR.
2. Armá el 11 (4-3-3): en cada turno sale un club al azar. Elegí la posición a llenar (aro amarillo en la cancha)
   y la lista te muestra solo los jugadores de ese club que pueden jugar ahí (según la BDD). Cada jugador queda
   en el casillero que elegiste y juega el partido en esa posición. Si ningún jugador del club sirve para los
   puestos que te quedan, aparece "Sortear otro club".
3. Eliminatoria de 3 partidos: Cuartos, Semifinal y Final contra clubes de la BDD (cada ronda el rival es algo
   más duro; su nivel sale de su plantel). Cada partido son 6 jugadas con 3 opciones. Si termina empatado
   se define por tanda de penales. Si perdés, quedás eliminado.

## Datos
- Si MySQL responde (config/ConexionBD.java) y tiene clubes, usa los DAO originales.
- Si no, usa datos de demostración en memoria (daoimpl/DatosDemo.java), así corre en cualquier PC.

## Estructura
- model / dao / daoimpl / exception / service / config: tu código original
  (se agregaron los DAO *memoria* y listarJugadoresDeClub en el service).
- arcade: reglas del partido (Partido, Situacion, Resultado, Escena).
- ui: ventanas Swing (CanchaPanel dibuja la cancha y reproduce las cinemáticas).
