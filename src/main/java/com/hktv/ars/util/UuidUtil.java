package com.hktv.ars.util;


import com.hktv.ars.repository.UuidDao;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class UuidUtil {

    public static String checkAndGetUniqueUuid(UuidDao dao) {
        String uuid = UUID.randomUUID().toString();

        boolean findNewUuid = false;
        do {
            if (dao.findByUuid(uuid).isPresent()) {
                uuid = UUID.randomUUID().toString();
                findNewUuid = true;
            }
        } while (findNewUuid);

        return uuid;
    }
}
