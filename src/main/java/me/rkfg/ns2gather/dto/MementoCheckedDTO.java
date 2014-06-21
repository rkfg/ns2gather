package me.rkfg.ns2gather.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MementoCheckedDTO<T extends CheckedDTO> {
    Set<Long> state = new HashSet<Long>();

    public void storeChecks(List<T> list) {
        for (T elem : list) {
            if (elem.getChecked()) {
                state.add(elem.getId());
            }
        }
    }

    public void restoreChecks(List<T> list) {
        for (T elem : list) {
            elem.setChecked(state.contains(elem.getId()));
        }
    }

}
