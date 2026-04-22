package com.goblinscheduler.repository;

import com.goblinscheduler.model.UserToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class UserTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UserToken> mapper = (rs, rowNum) -> {
        UserToken t = new UserToken();
        t.setId(rs.getLong("id"));
        t.setProvider(rs.getString("provider"));
        t.setProviderUserId(rs.getString("provider_user_id"));
        t.setAccessToken(rs.getString("access_token"));
        t.setRefreshToken(rs.getString("refresh_token"));
        var ts = rs.getTimestamp("expires_at");
        if (ts != null) t.setExpiresAt(ts.toInstant());
        t.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        t.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return t;
    };

    public UserToken findByProviderUserId(String provider, String providerUserId) {
        List<UserToken> list = jdbcTemplate.query(
            "SELECT * FROM user_tokens WHERE provider = ? AND provider_user_id = ?",
            mapper, provider, providerUserId);
        return list.isEmpty() ? null : list.get(0);
    }

    public UserToken save(UserToken t) {
        String sql = "INSERT INTO user_tokens (provider, provider_user_id, access_token, refresh_token, expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW()) RETURNING id, created_at, updated_at";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            t.setId(rs.getLong("id"));
            t.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            t.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return t;
        }, t.getProvider(), t.getProviderUserId(), t.getAccessToken(), t.getRefreshToken(),
            t.getExpiresAt() == null ? null : Timestamp.from(t.getExpiresAt()));
    }

    public void updateTokens(String provider, String providerUserId, String accessToken, String refreshToken, Instant expiresAt) {
        String sql = "UPDATE user_tokens SET access_token = ?, refresh_token = ?, expires_at = ?, updated_at = NOW() WHERE provider = ? AND provider_user_id = ?";
        jdbcTemplate.update(sql, accessToken, refreshToken,
            expiresAt == null ? null : Timestamp.from(expiresAt), provider, providerUserId);
    }
}
