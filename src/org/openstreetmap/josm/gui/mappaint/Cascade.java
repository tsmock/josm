// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.gui.mappaint.mapcss.CSSColors;

/**
 * Simple map of properties with dynamic typing.
 */
public class Cascade implements Cloneable {
    
    public static final Cascade EMPTY_CASCADE = new Cascade();

    protected Map<String, Object> prop = new HashMap<String, Object>();

    /**
     * Get value for the given key
     * @param key the key
     * @param def default value, can be null
     * @param klass the same as T
     * @return if a value with class klass has been mapped to key, returns this
     *      value, def otherwise
     */
    public <T> T get(String key, T def, Class klass) {
        if (def != null && !klass.isInstance(def))
            throw new IllegalArgumentException();
        Object o = prop.get(key);
        if (o == null)
            return def;
        if (klass.isInstance(o)) {
            @SuppressWarnings("unchecked") T res = (T) klass.cast(o);
            return res;
        }
        System.err.println(String.format("Warning: wrong type for mappaint property %s: %s expected, but %s of type %s found!", key, klass, o, o.getClass()));
        return def;
    }

    public Object get(String key) {
        return prop.get(key);
    }

    public Float getFloat(String key, Float def) {
        Object o = prop.get(key);
        if (o == null)
            return def;
        if (o instanceof Float)
            return (Float) o;
        if (o instanceof Integer)
            return new Float((Integer) o);
        return def;
    }

    public Color getColor(String key, Color def) {
        Object o = prop.get(key);
        if (o == null)
            return def;
        if (o instanceof Color)
            return (Color) o;
        if (o instanceof String) {
            Color clr = CSSColors.get((String) o);
            if (clr != null)
                return clr;
            else
                return def;
        }
        return def;
    }

    public void put(String key, Object val) {
        prop.put(key, val);
    }

    public void putOrClear(String key, Object val) {
        if (val != null) {
            prop.put(key, val);
        } else {
            prop.remove(key);
        }
    }

    public void remove(String key) {
        prop.remove(key);
    }

    @Override
    public Cascade clone() {
        @SuppressWarnings("unchecked") 
        HashMap<String, Object> clonedProp = (HashMap) ((HashMap) this.prop).clone();
        Cascade c = new Cascade();
        c.prop = clonedProp;
        return c;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("Cascade{ ");
        for (String key : prop.keySet()) {
            res.append(key+":");
            Object val = prop.get(key);
            if (val instanceof float[]) {
                res.append(Arrays.toString((float[]) val));
            } else {
                res.append(val+"");
            }
            res.append("; ");
        }
        return res.append("}").toString();
    }
}
