package org.lic.ip.crawler;

import java.util.Queue;

/**
 * Created by lc on 15/1/9.
 */
public class LimitRate {

    private Queue<Long> queue;

    private long duration;

    public LimitRate(long duration, int limit) {
        queue = new LimitQueue<Long>(limit);
        this.duration = duration;
    }

    public boolean check() {
        Long first = queue.peek();
        if (first == null) return true;
        long now = System.currentTimeMillis();
        if (now - first <= duration) {
            return false;
        } else {
            return true;
        }
    }

    public void update(long timetag) {
        queue.offer(timetag);
    }
}
