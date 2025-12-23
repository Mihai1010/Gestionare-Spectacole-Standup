package com.standup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class ArtistService {
    @Autowired
    private JdbcTemplate jdbc;

    public Iterable<Map<String, Object>> getAllArtists() {
        String sql = "SELECT * FROM Artisti";
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> getArtistPerformanceReport() {
        String sql = """
                SELECT a.id_artist,
                       a.nume,
                       a.prenume,
                       COUNT(DISTINCT p.id_spectacol)                    AS shows_count,
                       COALESCE(SUM(s.pret_bilet), 0)                     AS total_revenue,
                       COALESCE(AVG(s.pret_bilet), 0)                     AS avg_ticket_price,
                       MIN(s.data_spectacol)                              AS first_show_date,
                       MAX(s.data_spectacol)                              AS last_show_date
                FROM Artisti a
                         LEFT JOIN Participari p ON a.id_artist = p.id_artist
                         LEFT JOIN Spectacole s ON p.id_spectacol = s.id_spectacol
                GROUP BY a.id_artist, a.nume, a.prenume
                HAVING COUNT(DISTINCT p.id_spectacol) > 0
                ORDER BY total_revenue DESC
                """;
        return jdbc.queryForList(sql);
    }

    public Iterable<Map<String, Object>> getAllParticipations() {
        String sql = """
                SELECT p.id_spectacol,
                       p.id_artist,
                       a.nume       AS artist_nume,
                       a.prenume    AS artist_prenume,
                       s.titlu      AS show_titlu,
                       s.data_spectacol,
                       l.nume_locatie AS locatie_nume,
                       l.oras       AS locatie_oras
                FROM Participari p
                JOIN Artisti a    ON p.id_artist = a.id_artist
                JOIN Spectacole s ON p.id_spectacol = s.id_spectacol
                JOIN Locatii l    ON s.id_locatie = l.id_locatie
                ORDER BY s.data_spectacol, a.nume, a.prenume
                """;
        return jdbc.queryForList(sql);
    }

    public Map<String, Object> getArtistById(int id) {
        String sql = "SELECT * FROM Artisti WHERE id_artist = ?";
        return jdbc.queryForMap(sql, id);
    }

    @Transactional
    public void deleteArtist(int id) {
        try {
            // First delete from Participari
            String deleteParticipariSql = "DELETE FROM Participari WHERE id_artist = ?";
            jdbc.update(deleteParticipariSql, id);
            
            // Then delete the artist
            String deleteArtistSql = "DELETE FROM Artisti WHERE id_artist = ?";
            int rowsAffected = jdbc.update(deleteArtistSql, id);
            
            if (rowsAffected == 0) {
                throw new RuntimeException("No artist found with id: " + id);
            }
        } catch (Exception e) {
            System.err.println("Error deleting artist: " + e.getMessage());
            throw e;
        }
    }
    
    @Transactional
    public void updateArtist(int id, String nume, String prenume, String email, String telefon, java.sql.Date dataNasterii) {
        String sql = "UPDATE Artisti SET nume = ?, prenume = ?, email = ?, telefon = ?, data_nasterii = ? WHERE id_artist = ?";
        jdbc.update(sql, nume, prenume, email, telefon, dataNasterii, id);
    }
    
    @Transactional
    public void deleteParticipation(int showId, int artistId) {
        String sql = "DELETE FROM Participari WHERE id_spectacol = ? AND id_artist = ?";
        jdbc.update(sql, showId, artistId);
    }
}
