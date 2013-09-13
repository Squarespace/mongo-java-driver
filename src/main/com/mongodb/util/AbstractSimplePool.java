package com.mongodb.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An abstract implementation of {@link SimplePool}.
 */
abstract class AbstractSimplePool<T> implements SimplePool<T> {

    private final String name;
  
    private final int maxSize;

    public AbstractSimplePool(String name, int maxSize) {
      this.name = name;
      this.maxSize = maxSize;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T get() throws InterruptedException {
        try {
            return get(-1L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException err) {
            throw new IllegalStateException("TimeoutException", err);
        }
    }

    @Override
    public T get(long waitTime) throws InterruptedException {
        try {
            return get(waitTime, TimeUnit.MILLISECONDS);
        } catch (TimeoutException err) {
            throw new IllegalStateException("TimeoutException", err);
        }
    }
    
    /**
     * @see #get()
     * @see #get(long)
     */
    public abstract T get(long waitTime, TimeUnit unit) throws InterruptedException, TimeoutException;

    @Override
    public int getMaxSize() {
        return maxSize;
    }
}
