package com.tonyodev.fetch2.download;

/**
 * Created by tonyofrancis on 6/29/17.
 */

public interface Downloadable {
    void pause(long id);
    void resume(long id);
    void retry(long id);
    void remove(long id);
}
