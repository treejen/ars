package com.hktv.ars.repository;

import java.util.Optional;

public interface UuidDao<T> {
    Optional<T> findByUuid(String uuid);
}
