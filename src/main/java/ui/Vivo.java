package ui;

import java.util.Random;

import arcade.Escena;

/**
 * Simulación visual del partido mientras corre el reloj: los 22 jugadores se mueven en bloque según dónde está la
 * pelota, el que la tiene la pasa a un compañero, a veces se la cortan y a veces remata. No cambia el marcador:
 * los goles salen de las situaciones donde el DT decide. Sirve para que se vea que el partido se está jugando.
 *
 * Equipo 0 = el del usuario (ataca hacia x = 1). Equipo 1 = el rival (ataca hacia x = 0). Coordenadas de 0 a 1.
 */
final class Vivo {

    static final int N = 11;

    final double[][][] home = new double[2][N][2];
    final double[][][] pos = new double[2][N][2];
    private final double[][] fase = new double[2][N];
    private final Random rnd = new Random();

    double bx = 0.5, by = 0.5;      // pelota
    double alt;                     // altura de la pelota (píxeles) mientras vuela
    int eq = 0, idx = 6;            // quién la tiene
    boolean corriendo;              // true: el juego avanza (pases, cortes, remates)
    boolean enFormacion;            // true: durante una escena, vuelven a su lugar y se acercan a la jugada
    double posesion = 0.5;          // probabilidad de que el usuario conserve la pelota
    double directo = 0.25;          // 0 = pases cortos, 1 = juego directo
    final double[] tiempo = new double[2];   // tiempo con la pelota de cada equipo

    private double magnetismo;
    private double reloj;
    private boolean enVuelo, tiro;
    private double x0, y0, x1, y1, t, dur, arco;
    private int destEq, destIdx;
    private double espera = 500;

    // ------------------------------------------------------------------ configuración

    void setFormaciones(double[][] propia, double[][] rival) {
        for (int i = 0; i < N; i++) {
            home[0][i] = propia[i].clone();
            home[1][i] = rival[i].clone();
            fase[0][i] = rnd.nextDouble() * 6.28;
            fase[1][i] = rnd.nextDouble() * 6.28;
        }
        reiniciar();
    }

    /** Todos en su lugar y la pelota en el círculo central, con el usuario sacando. */
    void reiniciar() {
        for (int e = 0; e < 2; e++) {
            for (int i = 0; i < N; i++) pos[e][i] = home[e][i].clone();
        }
        eq = 0;
        idx = masCercanoAlCentro(0);
        bx = 0.5;
        by = 0.5;
        alt = 0;
        enVuelo = false;
        espera = 600;
        enFormacion = false;
        magnetismo = 0;
        tiempo[0] = tiempo[1] = 0;
    }

    private int masCercanoAlCentro(int e) {
        int mejor = 1;
        double d = 9;
        for (int i = 1; i < N; i++) {
            double dd = Math.abs(home[e][i][0] - 0.5) + Math.abs(home[e][i][1] - 0.5) * 0.4;
            if (dd < d) {
                d = dd;
                mejor = i;
            }
        }
        return mejor;
    }

    /** Porcentaje medido de posesión del usuario. Arranca en 50% y se va ajustando con lo que pasa. */
    double posesionMedida() {
        double prior = 4000;      // equivale a 4 segundos de juego parejo, para que al principio no salte a 100%
        return (tiempo[0] + prior) / (tiempo[0] + tiempo[1] + 2 * prior);
    }

    /** Después de una escena: el juego sigue según cómo terminó la jugada. */
    void reanudarDespuesDe(Escena.Fin fin) {
        enFormacion = false;
        enVuelo = false;
        alt = 0;
        espera = 700;
        switch (fin) {
            case GOL_FAVOR -> sacaDelCentro(1);
            case GOL_CONTRA -> sacaDelCentro(0);
            case ATAJADA, AFUERA, PALO -> {
                // si el remate era nuestro, la toma el arquero rival; si era del rival, nuestro arquero
                boolean cercaDelArcoRival = bx > 0.5;
                eq = cercaDelArcoRival ? 1 : 0;
                idx = 0;
            }
            case RECUPERACION -> {
                eq = 0;
                idx = masCercanoA(0, bx, by);
            }
            case PERDIDA -> {
                eq = 1;
                idx = masCercanoA(1, bx, by);
            }
            default -> {
                eq = 0;
                idx = masCercanoA(0, bx, by);
            }
        }
        if (idx < 0) idx = 1;
        bx = pos[eq][idx][0];
        by = pos[eq][idx][1];
    }

    private void sacaDelCentro(int e) {
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < N; i++) pos[k][i] = home[k][i].clone();
        }
        eq = e;
        idx = masCercanoAlCentro(e);
        bx = 0.5;
        by = 0.5;
    }

    private int masCercanoA(int e, double x, double y) {
        int mejor = 1;
        double d = 9;
        for (int i = 1; i < N; i++) {
            double dd = Math.hypot((pos[e][i][0] - x) * 1.7, pos[e][i][1] - y);
            if (dd < d) {
                d = dd;
                mejor = i;
            }
        }
        return mejor;
    }

    // ------------------------------------------------------------------ paso de simulación

    void actualizar(double dt) {
        reloj += dt;
        magnetismo += ((enFormacion ? 1.0 : 0.0) - magnetismo) * (1 - Math.exp(-dt / 150.0));
        if (corriendo && !enFormacion) {
            tiempo[eq] += dt;
            simularPelota(dt);
        }
        double k = 1 - Math.exp(-dt / 240.0);
        for (int e = 0; e < 2; e++) {
            for (int i = 0; i < N; i++) {
                double[] o = objetivo(e, i);
                pos[e][i][0] += (o[0] - pos[e][i][0]) * k;
                pos[e][i][1] += (o[1] - pos[e][i][1]) * k;
            }
        }
        if (corriendo && !enFormacion && !enVuelo) {
            double dir = eq == 0 ? 1 : -1;
            bx = pos[eq][idx][0] + dir * 0.012;
            by = pos[eq][idx][1];
        }
    }

    private double[] objetivo(int e, int i) {
        double hx = home[e][i][0], hy = home[e][i][1];
        double dir = e == 0 ? 1 : -1;
        if (enFormacion) {
            // vuelven a su lugar, y los cercanos a la jugada se acercan un poco a la pelota
            double dx = bx - hx, dy = by - hy;
            double d = Math.hypot(dx * 1.7, dy);
            double tx = hx, ty = hy;
            if (i != 0 && d < 0.5 && d > 0.001) {
                double w = (1 - d / 0.5) * 0.06 * magnetismo;
                tx += (dx * 1.7 / d) * w / 1.7;
                ty += (dy / d) * w;
            }
            return new double[]{tx, ty};
        }
        double gk = i == 0 ? 0.15 : 1.0;
        double tx = hx + (bx - 0.5) * 0.30 * gk;          // el bloque sube y baja con la pelota
        double ty = hy + (by - hy) * 0.14 * gk;
        boolean conPelota = (e == eq);
        if (i != 0) {
            tx += dir * (conPelota ? 0.035 : -0.025);      // el que ataca se proyecta, el otro se achica
            tx += Math.sin(reloj / 900.0 + fase[e][i]) * 0.010;
            ty += Math.cos(reloj / 1100.0 + fase[e][i] * 1.3) * 0.014;
        }
        if (!conPelota && i != 0 && corriendo) {
            // los dos más cercanos a la pelota salen a marcarla
            int rank = 0;
            double mio = dist(e, i);
            for (int j = 1; j < N; j++) if (j != i && dist(e, j) < mio) rank++;
            if (rank < 2 && mio < 0.30) {
                tx += (bx - tx) * 0.55;
                ty += (by - ty) * 0.55;
            }
        }
        tx = Math.max(0.02, Math.min(0.98, tx));
        ty = Math.max(0.03, Math.min(0.97, ty));
        return new double[]{tx, ty};
    }

    private double dist(int e, int j) {
        return Math.hypot((pos[e][j][0] - bx) * 1.7, pos[e][j][1] - by);
    }

    // ------------------------------------------------------------------ la pelota

    private void simularPelota(double dt) {
        if (enVuelo) {
            t += dt;
            double u = Math.min(1.0, t / dur);
            bx = x0 + (x1 - x0) * u;
            by = y0 + (y1 - y0) * u;
            alt = Math.sin(Math.PI * u) * arco;
            if (u >= 1.0) {
                enVuelo = false;
                alt = 0;
                if (tiro) {
                    tiro = false;            // el arquero se queda con la pelota
                    eq = destEq;
                    idx = 0;
                    espera = 1100;
                } else {
                    eq = destEq;
                    idx = destIdx;
                    espera = (380 + rnd.nextDouble() * 620) * (1 - 0.5 * directo);
                }
            }
            return;
        }
        espera -= dt;
        if (espera <= 0) decidir();
    }

    private void decidir() {
        int e = eq;
        double dir = e == 0 ? 1 : -1;
        double hx = pos[e][idx][0], hy = pos[e][idx][1];

        // en el último tercio, a veces patea al arco (sin gol: los goles los deciden las situaciones)
        if (idx != 0 && dir * (hx - 0.5) > 0.22 && rnd.nextDouble() < 0.09) {
            x0 = bx;
            y0 = by;
            x1 = e == 0 ? 1.01 : -0.01;
            y1 = 0.5 + rnd.nextGaussian() * 0.07;
            dur = 420;
            t = 0;
            arco = 12;
            enVuelo = true;
            tiro = true;
            destEq = 1 - e;
            destIdx = 0;
            return;
        }

        // elegir a quién se la pasa: prefiere a los cercanos y, si el juego es directo, a los de adelante
        int mejor = -1;
        double[] pesos = new double[N];
        double total = 0;
        for (int j = 0; j < N; j++) {
            if (j == idx) continue;
            double rx = pos[e][j][0], ry = pos[e][j][1];
            double d = Math.hypot((rx - hx) * 1.7, ry - hy);
            if (d > 0.9) continue;
            double w = Math.exp(-d * 3.2);
            double adelante = dir * (rx - hx);
            if (adelante > 0) w *= 1 + directo * 3.0 * adelante * 4;
            else w *= 0.55;
            if (j == 0) w *= (dir * (hx - 0.5) < -0.2) ? 0.25 : 0.0;
            pesos[j] = w;
            total += w;
        }
        if (total <= 0) {
            espera = 300;
            return;
        }
        double r = rnd.nextDouble() * total;
        for (int j = 0; j < N; j++) {
            r -= pesos[j];
            if (r <= 0 && pesos[j] > 0) {
                mejor = j;
                break;
            }
        }
        if (mejor < 0) mejor = idx == 1 ? 2 : 1;

        double dx = pos[e][mejor][0] + dir * 0.03;
        double dy = pos[e][mejor][1];
        x0 = bx;
        y0 = by;
        destEq = e;
        destIdx = mejor;

        // ¿se la cortan? Con estas probabilidades, a la larga cada equipo tiene la pelota el % de tiempo que le toca:
        // si el usuario domina (posesion alta), a él le cortan poco y al rival mucho.
        double s = Math.max(0.2, Math.min(0.8, posesion));
        double pCorte = 0.22 * Math.sqrt(e == 0 ? (1 - s) / s : s / (1 - s));
        if (rnd.nextDouble() < pCorte) {
            int rival = 1 - e;
            int mejorRival = 1;
            double dm = 9;
            for (int j = 1; j < N; j++) {
                double dd = Math.hypot((pos[rival][j][0] - dx) * 1.7, pos[rival][j][1] - dy);
                if (dd < dm) {
                    dm = dd;
                    mejorRival = j;
                }
            }
            destEq = rival;
            destIdx = mejorRival;
            dx = pos[rival][mejorRival][0];
            dy = pos[rival][mejorRival][1];
        }
        x1 = Math.max(0.01, Math.min(0.99, dx));
        y1 = Math.max(0.02, Math.min(0.98, dy));
        double d = Math.hypot((x1 - x0) * 1.7, y1 - y0);
        dur = 280 + d * 700;
        t = 0;
        arco = Math.min(30, d * 45);
        enVuelo = true;
        tiro = false;
    }

    boolean enVuelo() { return enVuelo; }
}
