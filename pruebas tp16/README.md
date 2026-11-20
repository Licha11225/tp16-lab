# Futbol 11 Arcade

## Requisitos
- Java 21
- Maven 3.9 o superior
- MySQL es opcional: si no hay conexión, el juego utiliza los datos demo en memoria.

## Ejecutar
Desde esta carpeta:

```bash
mvn clean package
java -jar target/futbol11-arcade-1.0.jar
```

También se puede iniciar con `mvn exec:java`.

## Mejoras implementadas
- Ajuste de coordenadas tácticas para las formaciones con línea de cinco y visualización de la formación elegida durante el partido.
- Simulación animada de movimientos y circulación de pelota durante las fases sin intervención, con reloj de partido acelerado para una duración cercana a dos minutos. El reloj se detiene durante las jugadas de pelota parada y las decisiones directas.
- Nuevos controles de tiro libre y penal a favor: flechas para elegir dirección y altura, barra de potencia y espacio para cargar/disparar.
- Penal en contra: flechas para mover al arquero en horizontal y vertical, y espacio para lanzarse. Incluye carrera previa y animación del disparo.
