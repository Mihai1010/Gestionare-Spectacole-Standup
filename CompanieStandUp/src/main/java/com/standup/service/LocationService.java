package com.standup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class LocationService {
    @Autowired
    private JdbcTemplate jdbc;

    public Iterable<Map<String, Object>> getAllLocations() {
        String sql = "SELECT * FROM Locatii";
        return jdbc.queryForList(sql);
    }

    @Transactional
    public void deleteLocation(int id) {
        try {
            String deleteLocationSql = "DELETE FROM Locatii WHERE id_locatie = ?";
            int rowsAffected = jdbc.update(deleteLocationSql, id);
            if (rowsAffected == 0) {
                throw new RuntimeException("No location found with id: " + id);
            }
        } catch (Exception e) {
            System.err.println("Error deleting location: " + e.getMessage());
            throw e;
        }
    }
}
