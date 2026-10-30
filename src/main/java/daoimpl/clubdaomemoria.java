package daoimpl;

import dao.clubDAO;
import model.club;

import java.util.List;
import java.util.Random;

public class clubdaomemoria implements clubDAO {

    private final List<club> clubes = DatosDemo.clubes();
    private final Random rnd = new Random();

    @Override
    public club obtenerAleatorio() {
        return clubes.get(rnd.nextInt(clubes.size()));
    }

    @Override
    public club buscarPorId(int id) {
        for (club c : clubes) {
            if (c.getId() == id) return c;
        }
        return null;
    }
}
