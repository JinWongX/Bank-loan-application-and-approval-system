package com.bank.loan.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@MappedTypes(LocalDate.class)
public class SqliteLocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.format(FORMATTER));
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return parse(value);
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return parse(value);
    }

    @Override
    public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return parse(value);
    }

    private LocalDate parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value;
        if (normalized.contains(" ")) {
            normalized = normalized.substring(0, normalized.indexOf(" "));
        }
        if (normalized.contains("T")) {
            normalized = normalized.substring(0, normalized.indexOf("T"));
        }
        return LocalDate.parse(normalized, FORMATTER);
    }
}
