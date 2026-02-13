package net.orbis.zakum.api.db;

import javax.sql.DataSource;

public interface ZakumDatabase {

  DatabaseState state();

  DataSource dataSource();

  Jdbc jdbc();
}
