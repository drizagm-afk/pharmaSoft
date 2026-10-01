package com.edu.upeu.PharmaBackend.service.generic;

import java.util.List;

public interface CrudService<REQ, RES, ID> {
    RES create(REQ req);

    RES update(ID id, REQ req);

    RES read(ID id);

    List<RES> readAll();

    void delete(ID id);
}
