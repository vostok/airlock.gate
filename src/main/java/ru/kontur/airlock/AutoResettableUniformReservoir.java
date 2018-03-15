package ru.kontur.airlock;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformSnapshot;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class AutoResettableUniformReservoir implements Reservoir {
    private final AtomicLong count;
    private final int size;
    private AtomicLongArray values;
    private static final Random random = new Random();

    public AutoResettableUniformReservoir() {
        this(1028);
    }

    public AutoResettableUniformReservoir(int size) {
        this.size = size;
        this.count = new AtomicLong();
        this.count.set(0L);
        this.values = new AtomicLongArray(this.size);
        for(int i = 0; i < this.values.length(); ++i) {
            this.values.set(i, 0L);
        }
    }

    public int size() {
        long c = this.count.get();
        return c > (long)this.values.length() ? this.values.length() : (int)c;
    }

    public void update(long value) {
        long c = this.count.incrementAndGet();
        if (c <= (long)this.values.length()) {
            this.values.set((int)c - 1, value);
        } else {
            long r = nextLong(c);
            if (r < (long)this.values.length()) {
                this.values.set((int)r, value);
            }
        }
    }

    private static long nextLong(long n) {
        long bits;
        long val;
        do {
            bits = random.nextLong() & 9223372036854775807L;
            val = bits % n;
        } while(bits - val + (n - 1L) < 0L);

        return val;
    }

    public Snapshot getSnapshot() {
        int s = this.size();
        final AtomicLongArray curValues = this.values;
        long[] copy = new long[s];

        for(int i = 0; i < s; ++i) {
            copy[i] = curValues.getAndSet(i,0);
        }
        this.count.set(0L);

        return new UniformSnapshot(copy);
    }
}
