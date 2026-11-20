package arcade;

public class Situacion {
    public final TipoJugada tipo;
    public final int minuto;
    public final String titulo;
    public final String descripcion;
    public final String[] opciones;

    public Situacion(TipoJugada tipo, int minuto, String titulo, String descripcion, String[] opciones) {
        this.tipo = tipo;
        this.minuto = minuto;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.opciones = opciones;
    }
}
