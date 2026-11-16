package daoimpl;

import dao.OnceidealDAO;
import model.Onceideal;

import java.util.ArrayList;
import java.util.List;

public class Onceidealdaomemoria implements OnceidealDAO {

    private final List<Onceideal> guardados = new ArrayList<>();

    @Override
    public void insertarOnce(Onceideal once) {
        once.setId(guardados.size() + 1);
        guardados.add(once);
    }
}
