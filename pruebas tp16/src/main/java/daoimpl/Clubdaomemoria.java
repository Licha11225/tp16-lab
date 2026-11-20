package daoimpl;

import java.util.List;
import java.util.Random;

import dao.ClubDAO;
import model.Club;

public class Clubdaomemoria implements ClubDAO {

    private final List<Club> clubes = DatosDemo.clubes();
    private final Random rnd = new Random();

    @Override
    public Club obtenerAleatorio() {
        return clubes.get(rnd.nextInt(clubes.size()));
    }

    @Override
    public Club buscarPorId(int id) {
        for (Club c : clubes) {
            if (c.getId() == id) return c;
        }
        return null;
    }
}
