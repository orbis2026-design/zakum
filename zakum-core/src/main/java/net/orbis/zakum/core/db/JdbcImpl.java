package net.orbis.zakum.core.db;

import net.orbis.zakum.api.db.Jdbc;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class JdbcImpl implements Jdbc {

  private final SqlManager sql;

  JdbcImpl(SqlManager sql) {
    this.sql = sql;
  }

  @Override
  public int update(String sqlText, Object... params) {
    DataSource ds = sql.dataSource();

    try (var c = ds.getConnection();
         var ps = c.prepareStatement(sqlText)) {

      bind(ps, params);
      return ps.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException("SQL update failed: " + e.getMessage(), e);
    }
  }

  @Override
  public <T> List<T> query(String sqlText, RowMapper<T> mapper, Object... params) {
    DataSource ds = sql.dataSource();

    try (var c = ds.getConnection();
         var ps = c.prepareStatement(sqlText)) {

      bind(ps, params);

      try (var rs = ps.executeQuery()) {
        List<T> out = new ArrayList<>();
        while (rs.next()) out.add(mapper.map(rs));
        return out;
      }

    } catch (SQLException e) {
      throw new RuntimeException("SQL query failed: " + e.getMessage(), e);
    }
  }

  private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
    if (params == null) return;
    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
  }
}
