package com.tonyodev.fetch2;

import io.realm.RealmObject;

public class RealmHeader extends RealmObject {

    private String key;
    private String value;

    public RealmHeader() {}

    public RealmHeader(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static RealmHeader newInstance(String key, String value) {
        return new RealmHeader(key,value);
    }
}
