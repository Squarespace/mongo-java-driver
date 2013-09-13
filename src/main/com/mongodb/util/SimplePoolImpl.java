package com.mongodb.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class SimplePoolImpl<T> extends AbstractSimplePool<T> {

    protected final Object lock = new Object();
    
    protected final Set<T> out = new HashSet<T>();
    
    protected final List<T> available = new ArrayList<T>();
    
    private boolean closed = false;
    
    public SimplePoolImpl(String name, int maxSize) {
        super(name, maxSize);
    }

    protected abstract T createNew();
    
    /**
     * Pick a member of {@code _avail}.  This method is called with a lock held on {@code _avail}, so it may be used safely.
     *
     * @param recommended the recommended member to choose.
     * @param couldCreate  true if there is room in the pool to create a new object
     * @return >= 0 the one to use, -1 create a new one
     */
    protected int pick(int recommended , boolean couldCreate) {
        return recommended;
    }
    
    @Override
    public void cleanup(T t) {
    }

    @Override
    public void done(T t) {
        synchronized (lock) {
            if (closed) {
                cleanup(t);
                return;
            }
            
            if (!out.remove(t)) {
                throw new RuntimeException("trying to put something back in the pool wasn't checked out");
            }
            
            available.add(t);
            lock.notifyAll();
        }
    }

    @Override
    public void remove(T t) {
        done(t);
    }

    /**
     * Returns {@code true} if it's possible to create more objects.
     */
    private boolean couldCreate() {
        return getTotal() < getMaxSize();
    }
    
    @Override
    public T get(long waitTime, TimeUnit unit) throws InterruptedException, TimeoutException {
        
        synchronized (lock) {
            
            long eol = System.currentTimeMillis() + unit.toMillis(waitTime);
            
            while (!closed) {
                boolean couldCreate = couldCreate();
                int pick = pick(available.size()-1, couldCreate);
                
                if (pick < 0 && !couldCreate) {
                    if (waitTime == 0L) {
                        // Not going to wait!
                        return null;
                    }
                    
                    if (waitTime < 0L) {
                        lock.wait();
                    } else {
                        
                        long remaining = eol - System.currentTimeMillis();
                        if (remaining < 0L) {
                            // TimeoutException
                            break;
                        }
                        
                        TimeUnit.MILLISECONDS.timedWait(lock, remaining);
                    }
                    
                    // Try again!
                    continue;
                }
                
                T element = null;
                if (pick >= 0) {
                    element = available.remove(pick);
                } else {
                    element = createNew();
                    
                    if (element == null) {
                        throw new IllegalStateException("null pool members are not allowed");
                    }
                }
                
                out.add(element);
                beforeReturn(element);
                return element;
            }
        }
        
        throw new TimeoutException("waitTime=" + waitTime + ", unit=" + unit);
    }
    
    /**
     * Called by {@link #get(long, TimeUnit)} before the value is returned to the user.
     */
    protected void beforeReturn(T element) {
        
    }
    
    protected void close() {
        synchronized (lock) {
            closed = true;
            for (T element : available) {
                cleanup(element);
            }
            out.clear();
            lock.notifyAll();
        }
    }

    @Override
    public int getTotal() {
        synchronized (lock) {
            return out.size() + available.size();
        }
    }

    @Override
    public int getInUse() {
        synchronized (lock) {
            return out.size();
        }
    }

    @Override
    public int getAvailable() {
        synchronized (lock) {
            return available.size();
        }
    }
    
    @Override
    public String toString(){
        StringBuilder buf = new StringBuilder();
        buf.append("pool: ").append(getName())
            .append(" maxToKeep: ").append(getMaxSize());
        
        synchronized (lock) {
            buf.append(" avail ").append(getAvailable())
                .append(" out ").append(getInUse());
        }
        
        return buf.toString();
    }
}
