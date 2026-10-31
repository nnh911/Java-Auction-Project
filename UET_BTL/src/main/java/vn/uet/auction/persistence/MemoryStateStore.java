package vn.uet.auction.persistence;

import java.io.IOException;

public class MemoryStateStore implements StateStore {

    private ApplicationState state;

    public MemoryStateStore() {
        state = new ApplicationState();
    }

    @Override
    public ApplicationState load() throws IOException {
        return state;
    }

    @Override
    public void save(ApplicationState state) throws IOException {
        if (state == null) {
            throw new IllegalArgumentException("State must not be null");
        }

        this.state = state;
    }
}