package org.opennms.horizon.inventory.exception;

import org.springframework.dao.DataIntegrityViolationException;

public class DuplicateDataException  extends  DataIntegrityViolationException {

    public DuplicateDataException(String message) {
        super(message);
    }


}
