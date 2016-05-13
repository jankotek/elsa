package org.mapdb.elsa;

import java.io.Serializable;

/**
 * Used for testing
 */
public class IntBean implements Serializable {

    public final int f;

    public IntBean(int f) {
        this.f = f;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntBean intBean = (IntBean) o;

        return f == intBean.f;

    }

    @Override
    public int hashCode() {
        return f;
    }
}
