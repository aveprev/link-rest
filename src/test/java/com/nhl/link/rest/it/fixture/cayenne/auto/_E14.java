package com.nhl.link.rest.it.fixture.cayenne.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

import com.nhl.link.rest.it.fixture.cayenne.E15;

/**
 * Class _E14 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _E14 extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    @Deprecated
    public static final String NAME_PROPERTY = "name";
    @Deprecated
    public static final String E15_PROPERTY = "e15";

    public static final String LONG_ID_PK_COLUMN = "long_id";

    public static final Property<String> NAME = new Property<String>("name");
    public static final Property<E15> E15 = new Property<E15>("e15");

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }

    public void setE15(E15 e15) {
        setToOneTarget("e15", e15, true);
    }

    public E15 getE15() {
        return (E15)readProperty("e15");
    }


}
