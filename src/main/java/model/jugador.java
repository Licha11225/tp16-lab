package model;

public abstract class jugador implements alineable {
	private int id;
    private String nombre;
    private int clubId;

    public jugador(int id, String nombre, int clubId) {
        this.id = id;
        this.nombre = nombre;
        this.clubId = clubId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getClubId() {
        return clubId;
    }

    public void setClubId(Integer clubId) {
        this.clubId = clubId;
    }

    @Override
    public String toString() {
        return "Jugador{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", clubId=" + clubId +
                '}';
    }
}
