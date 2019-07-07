package com.gs.obevo.db.impl.core.changeauditdao;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.gs.obevo.api.platform.AuditLock;

public class InMemLock implements AuditLock {
    private final Lock lock = new ReentrantLock();

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }
}
