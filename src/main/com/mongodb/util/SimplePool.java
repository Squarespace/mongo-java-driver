package com.mongodb.util;

/**
 * 
 */
public interface SimplePool<T> {

    public String getName();
    
    /**
     * override this if you need to do any cleanup
     */
    public void cleanup(T t);

    /**
     * call done when you are done with an object form the pool
     * if there is room and the object is ok will get added
     * @param t Object to add
     */
    public void done(T t);

    /**
     * 
     */
    public void remove(T t);

    /** Gets an object from the pool - will block if none are available
     * @return An object from the pool
     */
    public T get() throws InterruptedException;

    /** Gets an object from the pool - will block if none are available
     * @param waitTime 
     *                negative - forever
     *                0                - return immediately no matter what
     *                positive ms to wait
     * @return An object from the pool, or null if can't get one in the given waitTime
     */
    public T get(long waitTime) throws InterruptedException;
    
    /**
     * Returns the total number of the objects in the {@link SimplePool}.
     * 
     * @see #getInUse()
     * @see #getAvailable()
     */
    public int getTotal();

    /**
     * Returns the number of objects currently in use.
     */
    public int getInUse();

    /**
     * Returns the number of objects in the {@link SimplePool}.
     */
    public int getAvailable();

    /**
     * Returns the maximum size of the {@link SimplePool}
     */
    public int getMaxSize();
}