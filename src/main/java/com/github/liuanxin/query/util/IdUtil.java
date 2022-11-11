package com.github.liuanxin.query.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <pre>
 * https://github.com/mongodb/mongo-java-driver/blob/master/bson/src/main/org/bson/types/ObjectId.java
 *
 * https://github.com/callicoder/java-snowflake/blob/master/src/main/java/com/callicoder/snowflake/Snowflake.java
 *
 * 1. 时间部分(当前时间 - 某个起始时间)
 * 2. 本机 mac 数据
 * 3. 进程数据
 * 4. 自增数据
 *
 * 当 1 毫秒内自增达到了最大则使用下一毫秒; 如果碰到时钟回拨会休眠到下一时钟;
 * </pre>
 */
public class IdUtil {

    private static final Lock LOCK = new ReentrantLock();

    /** 起始时间截 */
    private static final long START_MS = 1391371506897L;

    /** 机器 mac 地址 id 所占的位数 */
    private static final long DATACENTER_ID_BITS = 5L;
    /** 机器进程 id 所占的位数 */
    private static final long WORKER_ID_BITS = 5L;
    /** 序列在 id 中占的位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大 mac 地址 id */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    /** 最大进程 id */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 时间截向左移位数 */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    /** mac 地址 id 向左移位数 */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    /** 进程 id 的左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 同一毫秒内的最大自增序列, 达到了将会使用下一毫秒 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 根据 mac 地址得到的 id */
    private static final long DATACENTER_ID;
    /** 根据进程得到的 id */
    private static final long WORKER_ID;

    /** 同一毫秒内的自增序列 */
    private static long sequence = 0L;

    /** 上次生成 id 的时间截 */
    private static long lastTimestamp = -1L;

    static {
        DATACENTER_ID = getDatacenterId();
        WORKER_ID = getMaxWorkerId();
    }
    private static long getDatacenterId() {
        long id = 0L;
        try {
            byte[] mac = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
            if (mac != null) {
                id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                id = id % (MAX_DATACENTER_ID + 1);
            }
        } catch (Exception ignore) {
            id = ThreadLocalRandom.current().nextLong(MAX_DATACENTER_ID + 1);
        }
        return id;
    }
    private static long getMaxWorkerId() {
        StringBuilder sbd = new StringBuilder();
        sbd.append(IdUtil.DATACENTER_ID);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name != null && !name.isEmpty()) {
            sbd.append(name.split("@")[0]);
        }
        return (sbd.toString().hashCode() & 0xffff) % (MAX_WORKER_ID + 1);
    }
    private static long nextMillis(long lastTimestamp) {
        long timestamp = getMs();
        while (timestamp <= lastTimestamp) {
            timestamp = getMs();
        }
        return timestamp;
    }
    private static long getMs() {
        return TimeMillis.getMs();
    }

    public static long getId() {
        long timestamp;
        LOCK.lock();
        try {
            timestamp = getMs();
            if (timestamp < lastTimestamp) {
                long offset = lastTimestamp - timestamp;
                if (offset <= 5) {
                    try {
                        Thread.sleep(5 - offset);
                        timestamp = getMs();
                        if (timestamp < lastTimestamp) {
                            throw new RuntimeException(String.format("时钟回拨. %d 毫秒内不生成 id", (lastTimestamp - timestamp)));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException(String.format("时钟回拨. %d 毫秒内拒绝生成 id", offset));
                }
            }
            if (lastTimestamp == timestamp) {
                // 同毫秒时序列号自增
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    // 同一毫秒自增达到最大时用下一毫秒
                    timestamp = nextMillis(lastTimestamp);
                }
            } else {
                // 不同毫秒序列号随机 1 或 2
                sequence = ThreadLocalRandom.current().nextLong(1, 3);
            }
            // 上次的时间截
            lastTimestamp = timestamp;
        } finally {
            LOCK.unlock();
        }
        // 移位 及 或运算 组成 64 位 id
        return ((timestamp - START_MS) << TIMESTAMP_LEFT_SHIFT)
                | (DATACENTER_ID << DATACENTER_ID_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    private static final class TimeMillis {
        private final AtomicLong now;
        private TimeMillis() {
            now = new AtomicLong(System.currentTimeMillis());
            // 每毫秒将当前毫秒的时间戳存起来
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "get-ms-schedule");
                thread.setDaemon(true);
                return thread;
            }).scheduleAtFixedRate(() -> now.set(System.currentTimeMillis()), 1, 1, TimeUnit.MILLISECONDS);
        }

        public static long getMs() {
            // 使用 static class 来确保延迟加载的单例
            return InstanceHolder.INSTANCE.now.get();
        }
        private static class InstanceHolder {
            public static final TimeMillis INSTANCE = new TimeMillis();
        }
    }
}
