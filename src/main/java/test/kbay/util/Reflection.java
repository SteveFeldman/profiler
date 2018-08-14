package test.kbay.util;

import java.lang.reflect.Method;

/**
 * Java reflection helper methods. Since Agent wasn't build with jars that it run in, we
 * have to use reflection.
 */
public class Reflection {
    public static Method findMethodByName( Object obj, String methodName ) {
        Class objClass = obj.getClass();
        Method[] methods = objClass.getMethods();
        for (Method m : methods) {
            if ( methodName.equals(m.getName()) )
                return m;
        }
        return null;
    }
}
