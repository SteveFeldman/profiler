package test.kbay.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ProfCollection {

    public static <T> HashSet<T> asHashSet(T... arr) {
        HashSet<T> res = new HashSet<>();
        for ( T itm : arr ) {
            res.add(itm);
        }
        return res;
    }

}
