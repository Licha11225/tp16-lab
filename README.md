# Fútbol 11 Arcade (tp15)

Swing + FlatLaf + Maven (Java 21).

## Cómo correrlo
    mvn compile exec:java        # corre el juego directo
    mvn package                  # genera target/futbol11-arcade-1.0.jar
    java -jar target/futbol11-arcade-1.0.jar

## Cómo se juega
1. Poné tu nombre de DT y tocá JUGAR.
2. Armá el 11 (4-3-3): en cada turno sale un club al azar. Elegí un jugador de ese club que sirva
   para la posición marcada (aro amarillo). El service valida club, duplicados y posición.
3. Jugá un partido de 6 jugadas. En cada una elegís entre 3 opciones (tenencia, atacar por una zona,
   defender, contragolpe, mano a mano, penal a favor / en contra) y se ve una mini cinemática.

## Datos
- Si MySQL responde (config/ConexionBD.java) y tiene clubes, usa los DAO originales.
- Si no, usa datos de demostración en memoria (daoimpl/DatosDemo.java), así corre en cualquier PC.

## Estructura
- model / dao / daoimpl / exception / service / config: tu código original
  (se agregaron los DAO *memoria* y listarJugadoresDeClub en el service).
- arcade: reglas del partido (Partido, Situacion, Resultado, Escena).
- ui: ventanas Swing (CanchaPanel dibuja la cancha y reproduce las cinemáticas).
