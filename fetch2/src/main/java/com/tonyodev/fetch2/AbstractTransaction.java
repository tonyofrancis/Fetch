package com.tonyodev.fetch2;

/**
 * Created by tonyofrancis on 6/11/17.
 */

abstract class AbstractTransaction<T> implements Transaction {

    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onPostExecute() {

    }
}
