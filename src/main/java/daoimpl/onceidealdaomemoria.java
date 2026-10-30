package daoimpl;

import dao.onceidealDAO;
import model.onceideal;

import java.util.ArrayList;
import java.util.List;

public class onceidealdaomemoria implements onceidealDAO {

    private final List<onceideal> guardados = new ArrayList<>();

    @Override
    public void insertarOnce(onceideal once) {
        once.setId(guardados.size() + 1);
        guardados.add(once);
    }
}
