/**
 * Serviciu pentru gestionarea spectacolelor: listare, creare, recomandări și rapoarte.
 *
 * @author Necula Mihai
 * @version 12 ianuarie 2026
 */
package com.standup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

@Service
public class ShowService {

    @Autowired
    private JdbcTemplate jdbc;

    public Iterable<Map<String, Object>> getAllShows() {
        String sql = """
                SELECT s.*, l.*,
                       CASE
                           WHEN s.data_spectacol > CURRENT_TIMESTAMP THEN 'Urmează'
                           ELSE 'S-a desfasurat'
                       END AS status,
                       (l.capacitate - (
                           SELECT COUNT(*) FROM Bilete b
                           WHERE b.id_spectacol = s.id_spectacol
                       )) AS remaining_seats
                FROM Spectacole s
                JOIN Locatii l ON s.id_locatie = l.id_locatie
                """;
        return jdbc.queryForList(sql);
    }

    public Iterable<Map<String, Object>> getUpcomingShows() {
        String sql = """
                SELECT s.*, l.*,
                       (l.capacitate - (
                           SELECT COUNT(*) FROM Bilete b
                           WHERE b.id_spectacol = s.id_spectacol
                       )) AS remaining_seats
                FROM Spectacole s
                JOIN Locatii l ON s.id_locatie = l.id_locatie
                WHERE s.data_spectacol > CURRENT_TIMESTAMP
                ORDER BY s.data_spectacol ASC
                """;
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> getPopularShows(int minTickets) {
        String sql = """
                SELECT s.id_spectacol,
                       s.titlu,
                       l.nume_locatie,
                       l.oras,
                       COUNT(b.id_bilet) AS ticket_count,
                       COALESCE(SUM(s.pret_bilet), 0) AS total_revenue
                FROM Spectacole s
                LEFT JOIN Bilete b ON s.id_spectacol = b.id_spectacol
                JOIN Locatii l ON s.id_locatie = l.id_locatie
                WHERE s.data_spectacol > CURRENT_TIMESTAMP
                GROUP BY s.id_spectacol, s.titlu, l.nume_locatie, l.oras
                HAVING COUNT(b.id_bilet) >= ?
                ORDER BY ticket_count DESC, total_revenue DESC
                """;
        return jdbc.queryForList(sql, minTickets);
    }

    public List<Map<String, Object>> getRecommendedShows(int clientId) {
        String sql = """
                SELECT TOP 5
                     s.id_spectacol,
                       s.titlu,
                       s.data_spectacol,
                       l.nume_locatie,
                       l.oras,
                       s.pret_bilet
                FROM Spectacole s
                JOIN Locatii l ON s.id_locatie = l.id_locatie
                WHERE s.id_spectacol NOT IN (
                    SELECT b.id_spectacol
                    FROM Bilete b
                    WHERE b.id_client = ?
                )
                  AND l.oras IN (
                    SELECT DISTINCT l2.oras
                    FROM Bilete b2
                    JOIN Spectacole s2 ON b2.id_spectacol = s2.id_spectacol
                    JOIN Locatii l2 ON s2.id_locatie = l2.id_locatie
                    WHERE b2.id_client = ?
                )
                  AND s.data_spectacol > CURRENT_TIMESTAMP
                ORDER BY (
                    SELECT COUNT(*)
                    FROM Bilete b3
                    WHERE b3.id_spectacol = s.id_spectacol
                ) DESC
                """;
        return jdbc.queryForList(sql, clientId, clientId);
    }

    @Transactional
    public void createShow(String titlu, int idLocatie, Timestamp dataSpectacol,
                           double pretBilet, int durata_minute, Integer artist1Id, Integer artist2Id, Integer artist3Id) {
        
        String checkSql = "SELECT COUNT(*) FROM Spectacole WHERE id_locatie = ? AND data_spectacol = ?";
        Integer count = jdbc.queryForObject(checkSql, Integer.class, idLocatie, dataSpectacol);
        if (count != null && count > 0) {
            throw new IllegalStateException("Există deja un spectacol la aceeași oră în această locație.");
        }

        
        int providedCount = 0;
        java.util.LinkedHashSet<Integer> artistIds = new java.util.LinkedHashSet<>();
        if (artist1Id != null) { artistIds.add(artist1Id); providedCount++; }
        if (artist2Id != null) { artistIds.add(artist2Id); providedCount++; }
        if (artist3Id != null) { artistIds.add(artist3Id); providedCount++; }

        if (artistIds.isEmpty()) {
            throw new IllegalStateException("Fiecare spectacol trebuie să aibă cel puțin un artist.");
        }
        if (artistIds.size() < providedCount) {
            throw new IllegalStateException("Același artist nu poate fi selectat de mai multe ori pentru același spectacol.");
        }
        if (artistIds.size() > 3) {
            throw new IllegalStateException("Un spectacol poate avea maximum 3 artiști.");
        }

        
        String artistCheckSql = "SELECT COUNT(*) FROM Participari p JOIN Spectacole s ON p.id_spectacol = s.id_spectacol " +
                "WHERE p.id_artist = ? AND s.data_spectacol = ?";
        for (Integer artistId : artistIds) {
            Integer artistCount = jdbc.queryForObject(artistCheckSql, Integer.class, artistId, dataSpectacol);
            if (artistCount != null && artistCount > 0) {
                throw new IllegalStateException("Artistul selectat este deja programat la un alt spectacol în același timp.");
            }
        }

        String insertSql = "INSERT INTO Spectacole (titlu, id_locatie, data_spectacol, pret_bilet, durata_minute) VALUES (?, ?, ?, ?, ?)";
        jdbc.update(insertSql, titlu, idLocatie, dataSpectacol, pretBilet, durata_minute);

        
        String idSql = "SELECT MAX(id_spectacol) FROM Spectacole WHERE titlu = ? AND id_locatie = ? AND data_spectacol = ? AND pret_bilet = ? AND durata_minute = ?";
        Integer showId = jdbc.queryForObject(idSql, Integer.class, titlu, idLocatie, dataSpectacol, pretBilet, durata_minute);

        if (showId != null) {
            String insertParticipareSql = "INSERT INTO Participari (id_spectacol, id_artist) VALUES (?, ?)";
            for (Integer artistId : artistIds) {
                jdbc.update(insertParticipareSql, showId, artistId);
            }
        }
    }

    @Transactional
    public void deleteShow(int id) {
        try {
            
            String deleteParticipariSql = "DELETE FROM Participari WHERE id_spectacol = ?";
            int participariDeleted = jdbc.update(deleteParticipariSql, id);
            System.out.println("Deleted " + participariDeleted + " participations for show " + id);

           
            String deleteTicketsSql = "DELETE FROM Bilete WHERE id_spectacol = ?";
            int ticketsDeleted = jdbc.update(deleteTicketsSql, id);
            System.out.println("Deleted " + ticketsDeleted + " tickets for show " + id);

           
            String deleteShowSql = "DELETE FROM Spectacole WHERE id_spectacol = ?";
            int showsDeleted = jdbc.update(deleteShowSql, id);
            System.out.println("Deleted " + showsDeleted + " shows with id " + id);
            
            if (showsDeleted == 0) {
                throw new RuntimeException("No show found with id: " + id);
            }
        } catch (Exception e) {
            System.err.println("Error deleting show " + id + ": " + e.getMessage());
            throw e;            
        }
    }

    
}
