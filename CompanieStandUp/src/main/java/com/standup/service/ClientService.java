package com.standup.service;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service

public class ClientService {

    @Autowired

    private JdbcTemplate jdbc;

    public Map<String, Object> autentificare(String email, String parola) {

        String sql = "SELECT * FROM Clienti WHERE email = ? AND parola = ?";

        try {

            return jdbc.queryForMap(sql, email, parola);

        } catch (Exception e) {

            return null;

        }

    }

    public Iterable<Map<String, Object>> getBileteClient(int idClient) {

        String sql = """
                SELECT B.id_bilet, S.titlu, L.nume_locatie, B.rand, B.loc, B.data_achizitie, S.pret_bilet, S.data_spectacol
                FROM Bilete B
                JOIN Spectacole S ON B.id_spectacol = S.id_spectacol
                JOIN Locatii L ON S.id_locatie = L.id_locatie
                WHERE B.id_client = ?
                """;

        return jdbc.queryForList(sql, idClient);

    }

    public List<Map<String, Object>> getVipClients(int minShows) {

        String sql = """
                SELECT c.id_client,
                       c.nume,
                       c.prenume,
                       c.email,
                       COUNT(DISTINCT b.id_spectacol) AS shows_attended,
                       COALESCE(SUM(s.pret_bilet), 0) AS total_spent
                FROM Clienti c
                JOIN Bilete b ON c.id_client = b.id_client
                JOIN Spectacole s ON b.id_spectacol = s.id_spectacol
                WHERE s.data_spectacol > DATEADD(YEAR, -1, CAST(GETDATE() AS date)) 
                AND s.data_spectacol <= CAST(GETDATE() AS date)
                GROUP BY c.id_client, c.nume, c.prenume, c.email
                HAVING COUNT(DISTINCT b.id_spectacol) >= ?
                ORDER BY total_spent DESC
                """;

        return jdbc.queryForList(sql, minShows);

    }

    public List<Map<String, Object>> getClientPurchaseHistory(int clientId) {

        String sql = """
                SELECT c.id_client,
                       c.nume,
                       c.prenume,
                       (SELECT COUNT(*)
                        FROM Bilete b
                        WHERE b.id_client = c.id_client) AS total_tickets,
                       (SELECT AVG(s.pret_bilet)
                        FROM Bilete b2
                                 JOIN Spectacole s ON b2.id_spectacol = s.id_spectacol
                        WHERE b2.id_client = c.id_client) AS avg_ticket_price,
                       (SELECT TOP 1 s2.titlu
                        FROM Bilete b3
                                 JOIN Spectacole s2 ON b3.id_spectacol = s2.id_spectacol
                        WHERE b3.id_client = c.id_client
                        ORDER BY s2.pret_bilet DESC) AS most_expensive_show
                FROM Clienti c
                WHERE c.id_client = ?
                GROUP BY c.id_client, c.nume, c.prenume
                """;

        return jdbc.queryForList(sql, clientId);

    }

    public Iterable<Map<String, Object>> getLocatii() {

        String sql = "SELECT * FROM Locatii";

        return jdbc.queryForList(sql);

    }

    public Map<String, Object> getClientByEmail(String email) {

        String sql = "SELECT * FROM Clienti WHERE email = ?";

        return jdbc.queryForMap(sql, email);

    }

    public Iterable<Map<String, Object>> getClientById(int idClient) {

        String sql = "SELECT * FROM Clienti WHERE id_client = ?";

        return jdbc.queryForList(sql, idClient);

    }

    public String getEmailByIdClient(int idClient) {

        String sql = "SELECT email FROM Clienti WHERE id_client = ?";

        return jdbc.queryForObject(sql, String.class, idClient);

    }
    
    public int countTotalUsers() {
    String sql = "SELECT COUNT(id_client) FROM Clienti";
    Integer count = jdbc.queryForObject(sql, Integer.class);
    return count != null ? count : 0;  // Return 0 if count is null
    }

    public int countTotalShows(){
    String sql = "select count(id_spectacol) from Spectacole";
    Integer count = jdbc.queryForObject(sql, Integer.class);
    return count!=null ? count : 0;
    }

    public int countTotalTickets(){
    String sql = "select count(id_bilet) from Bilete";
    Integer count = jdbc.queryForObject(sql, Integer.class);
    return count!=null ? count : 0;
    }

    public Iterable<Map<String, Object>> getAllClients() {
        String sql = "SELECT * FROM Clienti";
        return jdbc.queryForList(sql);
    }

    public Iterable<Map<String, Object>> getAllTickets() {
        String sql = """
                SELECT B.id_bilet,
                       C.nume      AS client_nume,
                       C.prenume   AS client_prenume,
                       C.email     AS client_email,
                       S.titlu     AS show_titlu,
                       L.nume_locatie AS locatie_nume,
                       B.rand,
                       B.loc,
                       B.data_achizitie,
                       S.pret_bilet
                FROM Bilete B
                JOIN Clienti C ON B.id_client = C.id_client
                JOIN Spectacole S ON B.id_spectacol = S.id_spectacol
                JOIN Locatii L ON S.id_locatie = L.id_locatie
                """;

        return jdbc.queryForList(sql);
    }

    public Map<String, Object> getClientByIdSingle(int idClient) {
        String sql = "SELECT * FROM Clienti WHERE id_client = ?";
        return jdbc.queryForMap(sql, idClient);
    }

    @Transactional
    public void deleteClient(int id) {
        String deleteTicketsSql = "DELETE FROM Bilete WHERE id_client = ?";
        jdbc.update(deleteTicketsSql, id);

        String deleteClientSql = "DELETE FROM Clienti WHERE id_client = ?";
        jdbc.update(deleteClientSql, id);
    }

    @Transactional
    public void deleteTicket(int idBilet) {
        String sql = "DELETE FROM Bilete WHERE id_bilet = ?";
        jdbc.update(sql, idBilet);
    }

    @Transactional
    public void createClient(String nume, String prenume, String email, String telefon, String parola) {
        String sql = "INSERT INTO Clienti (nume, prenume, email, telefon, parola) VALUES (?, ?, ?, ?, ?)";
        jdbc.update(sql, nume, prenume, email, telefon, parola);
    }

    @Transactional
    public void updateClient(int idClient, String nume, String prenume, String email, String telefon, String parola) {
        String sql = "UPDATE Clienti SET nume = ?, prenume = ?, email = ?, telefon = ?, parola = ? WHERE id_client = ?";
        jdbc.update(sql, nume, prenume, email, telefon, parola, idClient);
    }

    @Transactional
    public void createTicket(int idClient, int idSpectacol, int rand, int loc) {
        String checkSql = "SELECT COUNT(*) FROM Bilete WHERE id_spectacol = ? AND rand = ? AND loc = ?";
        Integer count = jdbc.queryForObject(checkSql, Integer.class, idSpectacol, rand, loc);

        if (count != null && count > 0) {
            throw new IllegalStateException("Locul selectat este deja ocupat pentru acest spectacol.");
        }

        String insertSql = "INSERT INTO Bilete (id_client, id_spectacol, rand, loc, data_achizitie) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        jdbc.update(insertSql, idClient, idSpectacol, rand, loc);
    }
    
}