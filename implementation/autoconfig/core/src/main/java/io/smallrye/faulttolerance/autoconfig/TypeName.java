package io.smallrye.faulttolerance.autoconfig;

import java.lang.reflect.Array;

public class TypeName {
    // type name as returned by Class.getName()
    // - name of the type in case of void or primitive types
    // - binary name per JLS in case of class types
    // - special syntax for array types
    public String binaryName;

    @Override
    public String toString() {
        return binaryName;
    }

    public Class<?> loadFromTCCL() throws ClassNotFoundException {
        return loadFromTCCL(binaryName);
    }

    private static Class<?> loadFromTCCL(String binaryName) throws ClassNotFoundException {
        if (binaryName.startsWith("[")) {
            int dimensions = 0;
            while (binaryName.startsWith("[")) {
                dimensions++;
                binaryName = binaryName.substring(1);
            }

            Class<?> elementClass;
            switch (binaryName) {
                case "Z":
                    elementClass = boolean.class;
                    break;
                case "B":
                    elementClass = byte.class;
                    break;
                case "S":
                    elementClass = short.class;
                    break;
                case "I":
                    elementClass = int.class;
                    break;
                case "J":
                    elementClass = long.class;
                    break;
                case "F":
                    elementClass = float.class;
                    break;
                case "D":
                    elementClass = double.class;
                    break;
                case "C":
                    elementClass = char.class;
                    break;
                default:
                    if (binaryName.startsWith("L") && binaryName.endsWith(";")) {
                        binaryName = binaryName.substring(1, binaryName.length() - 1);
                    }
                    elementClass = loadFromTCCL(binaryName);
                    break;
            }

            return Array.newInstance(elementClass, new int[dimensions]).getClass();
        }

        switch (binaryName) {
            case "void":
                return void.class;
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            default:
                return Thread.currentThread().getContextClassLoader().loadClass(binaryName);
        }
    }
}
