package vn.uet.auction.persistence;

import java.io.IOException;

public interface StateStore {

    ApplicationState load() throws IOException;

    void save(ApplicationState state) throws IOException;
}