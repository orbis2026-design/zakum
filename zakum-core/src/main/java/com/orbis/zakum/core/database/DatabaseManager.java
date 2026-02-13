package com.orbis.zakum.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.nio.ByteBuffer;

public class DatabaseManager {

    private final HikariDataSource dataSource;

    public DatabaseManager(FileConfiguration config) {
        HikariConfig hikari = new HikariConfig();
        
        // JDBC Driver Settings
        hikari.setJdbcUrl("jdbc:mysql://" + config.getString("database.host") + ":" + 
                          config.getInt("database.port") + "/" + config.getString("database.database"));
        hikari.setUsername(config.getString("database.username"));
        hikari.setPassword(config.getString("database.password"));
        
        // Pool Settings (Iron Dome Optimized)
        hikari.setMaximumPoolSize(config.getInt("database.maximumPoolSize", 20));
        hikari.setConnectionTimeout(config.getLong("database.connectionTimeout", 5000));
        hikari.setIdleTimeout(config.getLong("database.idleTimeout", 300000));
        hikari.setMaxLifetime(config.getLong("database.maxLifetime", 1800000));
        
        // Performance Properties
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        hikari.addDataSourceProperty("rewriteBatchedStatements", "true");

        this.dataSource = new HikariDataSource(hikari);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    // Helper: UUID <-> Binary(16)
    public byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
    
    public UUID bytesToUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
