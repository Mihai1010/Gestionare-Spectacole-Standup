/**
 * Serviciu pentru gestionarea locațiilor în care se desfășoară spectacolele.
 *
 * @author Necula Mihai
 * @version 12 ianuarie 2026
 */
package com.standup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.List;

@Service
public class LocationService {
    @Autowired
    private JdbcTemplate jdbc;

    public Iterable<Map<String, Object>> getAllLocations() {
        String sql = "SELECT * FROM Locatii";
        return jdbc.queryForList(sql);
    }
    
    public List<String> getAllCities() {
        String sql = "SELECT DISTINCT oras FROM Locatii ORDER BY oras";
        return jdbc.queryForList(sql, String.class);
    }

   
    public Iterable<Map<String, Object>> getLocationsByCityWithShowCount(String oras) {
    String sql = """
            SELECT l.id_locatie, 
                   l.nume_locatie, 
                   l.oras, 
                   l.strada, 
                   l.numar,
                   l.capacitate,
                   COUNT(s.id_spectacol) AS upcoming_shows_count
            FROM Locatii l
            LEFT JOIN Spectacole s ON l.id_locatie = s.id_locatie 
                AND s.data_spectacol > CURRENT_TIMESTAMP
            WHERE LOWER(l.oras) = LOWER(?)
            GROUP BY l.id_locatie, l.nume_locatie, l.oras, l.strada, l.numar, l.capacitate
            ORDER BY l.nume_locatie
            """;
    return jdbc.queryForList(sql, oras);
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
