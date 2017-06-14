package com.tonyodev.fetch2;

/**
 * Created by tonyofrancis on 6/11/17.
 */

interface Transaction {
    void onPreExecute();
    void onExecute(Database database);
    void onPostExecute();
}
