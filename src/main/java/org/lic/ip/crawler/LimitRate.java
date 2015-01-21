package org.lic.ip.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

/**
 * Created by lc on 15/1/9.
 */
public class LimitRate {
    private static final Logger logger = LoggerFactory.getLogger(LimitRate.class);

    private LimitQueue<Long> queue;

    private long duration;

    private int limit;

    public LimitRate(long duration, int limit) {
        queue = new LimitQueue<Long>(limit);
        this.duration = duration;
        this.limit = limit;
    }

    public void check() throws InterruptedException {
        if (queue.size() < limit)
            return;
        Long first = queue.peek();
        if (first == null)
            return;
        long now = System.currentTimeMillis();
        if (now - first <= duration) {
            logger.info("limit rate checked, sleep a while");
            Thread.sleep(duration - now + first + 1);
        }
        queue.offer(now);
    }
}
