/*
 * Copyright (c) 2020 ShardingSphere.
 * All rights reserved.
 */

package com.jdd.global.shardingsphere.workshop.proxy.packet.error;

import com.jdd.global.shardingsphere.workshop.proxy.constant.MySQLServerErrorCode;
import com.jdd.global.shardingsphere.workshop.proxy.packet.generic.MySQLErrPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.SQLException;

/**
 * ERR packet factory for MySQL.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MySQLErrPacketFactory {
    
    /**
     * New instance of MySQL ERR packet.
     * 
     * @param sequenceId sequence ID
     * @param cause cause
     * @return instance of MySQL ERR packet
     */
    public static MySQLErrPacket newInstance(final int sequenceId, final Exception cause) {
        if (cause instanceof SQLException) {
            SQLException sqlException = (SQLException) cause;
            return null != sqlException.getSQLState() ? new MySQLErrPacket(sequenceId, sqlException.getErrorCode(), sqlException.getSQLState(), sqlException.getMessage())
                : new MySQLErrPacket(sequenceId, MySQLServerErrorCode.ER_INTERNAL_ERROR, sqlException.getCause().getMessage());
        }
        return new MySQLErrPacket(sequenceId, MySQLServerErrorCode.UNKNOWN_EXCEPTION, cause.getMessage());
    }
}
