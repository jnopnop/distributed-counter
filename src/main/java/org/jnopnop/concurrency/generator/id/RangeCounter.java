package org.jnopnop.concurrency.generator.id;

import org.jnopnop.concurrency.generator.id.domain.Range;

import java.util.concurrent.atomic.AtomicLong;

// TODO: Rename state
public record RangeCounter(Range range, AtomicLong counter) {

    public RangeCounter(Range range) {
        this(range, new AtomicLong(range.start()));
    }

    public long nextId() {
        return counter.getAndIncrement();
    }

    public boolean isWithin(long id) {
        return range.isWithin(id);
    }

    public boolean isRangeEnd(long id) {
        return id == range.end();
    }
}
