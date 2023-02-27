package org.jnopnop.concurrency.generator.id.support;

import org.jnopnop.concurrency.generator.id.IdGenerator;

public interface LoadBalancer {

    IdGenerator nextInstance();
}
