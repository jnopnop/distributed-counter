package org.jnopnop.concurrency.generator.id.domain;

public record Range(long start, long end) {

    public boolean isWithin(long id) {
        return id >= start && id < end;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}
