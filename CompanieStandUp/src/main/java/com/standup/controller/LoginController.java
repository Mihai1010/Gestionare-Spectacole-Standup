package com.standup.controller;

import com.standup.service.ClientService;
import com.standup.service.ShowService;
import com.standup.service.LocationService;
import com.standup.service.ArtistService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;



@Controller

public class LoginController {

    @Autowired

    private ClientService clientService;
    @Autowired
    private ShowService showService;
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private ArtistService artistService;

    @GetMapping("/")

    public String login() {

        return "redirect:/login";

    }

    @GetMapping("/login")

    public String home() {

        return "login";

    }

    @GetMapping("/signup")

    public String signupForm() {

        return "signup";

    }

    @PostMapping("/login")

    public String loginSubmit(@RequestParam String email,

            @RequestParam String parola,

            Model model) {

        if ("admin@admin.com".equals(email) && "admin".equals(parola)) {
            return "redirect:/admin";
        }

        Map<String, Object> client = clientService.autentificare(email, parola);

        if (client != null) {

            model.addAttribute("client", client);

            return "redirect:/welcome?email=" + email;

        } else {

            model.addAttribute("error", "Email sau parolă incorectă");

            return "login";

        }

    }

    @PostMapping("/signup")

    public String signupSubmit(@RequestParam String nume,

                               @RequestParam String prenume,

                               @RequestParam String email,

                               @RequestParam String telefon,

                               @RequestParam String parola) {

        clientService.createClient(nume, prenume, email, telefon, parola);

        return "redirect:/login";

    }

    @GetMapping("/admin")

    public String adminDashboard(Model model) {
        int userCount = clientService.countTotalUsers();
        int showsCount = clientService.countTotalShows();
        int ticketsCount = clientService.countTotalTickets();
        model.addAttribute("adminEmail", "admin@admin.com");
        model.addAttribute("userCount", userCount);
        model.addAttribute("showsCount",showsCount);
        model.addAttribute("ticketsCount",ticketsCount);
        return "admin"; 
    }

    @GetMapping("/admin/reports/popular")
    public String popularShowsReport(@RequestParam(name = "minTickets", defaultValue = "5") int minTickets,
                                     Model model) {
        List<Map<String, Object>> popularShows = showService.getPopularShows(minTickets);
        model.addAttribute("popularShows", popularShows);
        model.addAttribute("minTickets", minTickets);
        return "popular-shows";
    }

    @GetMapping("/admin/reports/vip")
    public String vipClientsReport(@RequestParam(name = "minShows", defaultValue = "3") int minShows,
                                   Model model) {
        List<Map<String, Object>> vipClients = clientService.getVipClients(minShows);
        model.addAttribute("vipClients", vipClients);
        model.addAttribute("minShows", minShows);
        return "vip-clients";
    }

    @GetMapping("/admin/reports/artists")
    public String artistPerformanceReport(Model model) {
        List<Map<String, Object>> artistReport = artistService.getArtistPerformanceReport();
        model.addAttribute("artistReport", artistReport);
        return "artist-performance";
    }

    @GetMapping("/admin/reports/client-history")
    public String clientPurchaseHistory(@RequestParam("clientId") int clientId, Model model) {
        List<Map<String, Object>> history = clientService.getClientPurchaseHistory(clientId);
        model.addAttribute("history", history);
        model.addAttribute("clientId", clientId);
        return "client-purchase-history";
    }

    @GetMapping("/admin/clients")
    public String viewAllClients(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "clients";
    }
    
    @GetMapping("/admin/clients/delete/{id}")
    public String deleteClient(@PathVariable int id, HttpSession session) {
        try {
            clientService.deleteClient(id);
            return "redirect:/admin/clients";
        } catch (Exception e) {
            // Log the error for debugging
            e.printStackTrace();
            // Redirect back to clients page with an error message
            return "redirect:/admin/clients?error=Error deleting client";
        }
    }

    @GetMapping("/admin/clients/edit/{id}")
    public String editClientForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> client = clientService.getClientByIdSingle(id);
            model.addAttribute("client", client);
            return "client-edit";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/clients";
        }
    }

    @PostMapping("/admin/clients/edit/{id}")
    public String editClientSubmit(@PathVariable int id,
                                  @RequestParam String nume,
                                  @RequestParam String prenume,
                                  @RequestParam String email,
                                  @RequestParam String telefon,
                                  @RequestParam String parola,
                                  RedirectAttributes redirectAttributes) {
        try {
            clientService.updateClient(id, nume, prenume, email, telefon, parola);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/admin/clients";
    }

    @GetMapping("/admin/shows")
    public String viewAllShows(Model model) {
        model.addAttribute("shows", showService.getAllShows());
        model.addAttribute("locations", locationService.getAllLocations());
        return "shows";
    }
    
    @GetMapping("/admin/shows/delete/{id}")
    public String deleteShow(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            showService.deleteShow(id);
            return "redirect:/admin/shows";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/shows";
        }
    }

    @GetMapping("/admin/shows/add")
    public String showAddShowForm(Model model) {
        model.addAttribute("locations", locationService.getAllLocations());
        model.addAttribute("artists", artistService.getAllArtists());
        return "show-add";
    }

    @PostMapping("/admin/shows/add")
    public String addShow(@RequestParam String titlu,
                          @RequestParam("id_locatie") int idLocatie,
                          @RequestParam("data_spectacol") String dataSpectacolStr,
                          @RequestParam("pret_bilet") double pretBilet,
                          @RequestParam(value = "artist1Id", required = false) Integer artist1Id,
                          @RequestParam(value = "artist2Id", required = false) Integer artist2Id,
                          @RequestParam(value = "artist3Id", required = false) Integer artist3Id,
                          RedirectAttributes redirectAttributes) {
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dataSpectacolStr);
            java.sql.Timestamp ts = java.sql.Timestamp.valueOf(ldt);
            showService.createShow(titlu, idLocatie, ts, pretBilet, artist1Id, artist2Id, artist3Id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/admin/shows";
    }

    @GetMapping("/admin/participations")
    public String viewAllParticipations(Model model) {
        model.addAttribute("participations", artistService.getAllParticipations());
        return "participations";
    }

    @GetMapping("/admin/participations/delete/{showId}/{artistId}")
    public String deleteParticipation(@PathVariable int showId,
                                     @PathVariable int artistId,
                                     RedirectAttributes redirectAttributes) {
       try {
           artistService.deleteParticipation(showId, artistId);
       } catch (Exception e) {
           e.printStackTrace();
       }
       return "redirect:/admin/participations";
   }

    @GetMapping("/admin/locations")
    public String viewAllLocations(Model model) {
        model.addAttribute("locations", locationService.getAllLocations());
        return "locations";
    }
    
    @GetMapping("/admin/locations/delete/{id}")
    public String deleteLocation(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            locationService.deleteLocation(id);
            
            return "redirect:/admin/locations";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/locations";
        }
    }
    
    @GetMapping("/admin/artists")
    public String viewAllArtists(Model model) {
        model.addAttribute("artists", artistService.getAllArtists());
        return "artists";
    }
    
    @GetMapping("/admin/artists/delete/{id}")
    public String deleteArtist(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            artistService.deleteArtist(id);
            return "redirect:/admin/artists";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/artists";
        }
    }
    
    @GetMapping("/admin/artists/edit/{id}")
    public String editArtistForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> artist = artistService.getArtistById(id);
            model.addAttribute("artist", artist);
            return "artist-edit";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/artists";
        }
    }

    @PostMapping("/admin/artists/edit/{id}")
    public String editArtistSubmit(@PathVariable int id,
                                   @RequestParam String nume,
                                   @RequestParam String prenume,
                                   @RequestParam String email,
                                   @RequestParam String telefon,
                                   @RequestParam("data_nasterii") @DateTimeFormat(pattern = "yyyy-MM-dd") java.util.Date dataNasterii,
                                   RedirectAttributes redirectAttributes) {
        try {
            java.sql.Date sqlDate = new java.sql.Date(dataNasterii.getTime());
            artistService.updateArtist(id, nume, prenume, email, telefon, sqlDate);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/admin/artists";
    }
    
    @GetMapping("/admin/tickets")
    public String viewAllTickets(Model model) {
        model.addAttribute("tickets", clientService.getAllTickets());
        return "tickets";
    }
    
    @GetMapping("/admin/tickets/delete/{id}")
    public String deleteTicket(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            clientService.deleteTicket(id);
            return "redirect:/admin/tickets";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/tickets";
        }
    }
    
    @GetMapping("/welcome")

    public String welcome(@RequestParam String email, HttpSession session, Model model) {

        if (email != null) {

            session.setAttribute("email", email);

        }

        else {

            email = (String) session.getAttribute("email");

        }

        Map<String, Object> client = clientService.getClientByEmail(email);

        model.addAttribute("client", client);

        return "welcome";

    }

    @GetMapping("/recomandari")
    public String recomandari(HttpSession session, Model model) {

        String email = (String) session.getAttribute("email");
        if (email == null) {
            return "redirect:/login";
        }

        Map<String, Object> client = clientService.getClientByEmail(email);
        model.addAttribute("client", client);

        Integer clientId = (Integer) client.get("id_client");
        if (clientId != null) {
            model.addAttribute("recommendedShows", showService.getRecommendedShows(clientId));
        }

        return "recommended-shows";
    }

    @GetMapping("/logout")

    public String logout() {

        return "redirect:/login";

    }

    @GetMapping("/bilete/{idClient}")

    public String bilete(@PathVariable int idClient, Model model) {

        model.addAttribute("bilete", clientService.getBileteClient(idClient));

        String email = clientService.getEmailByIdClient(idClient);

        model.addAttribute("email", email);

        return "bilete";

    }

    @GetMapping("/locatii")

    public String locatii(Model model, HttpSession session) {

        model.addAttribute("locatii", clientService.getLocatii());

        return "locatii";

    }

    @GetMapping("/cumpara-bilet")
    public String cumparaBilet(HttpSession session,
                               Model model,
                               @RequestParam(value = "city", required = false) String city,
                               @RequestParam(value = "showId", required = false) Integer showId) {

        String email = (String) session.getAttribute("email");

        if (email == null) {
            return "redirect:/login";
        }

        Map<String, Object> client = clientService.getClientByEmail(email);
        model.addAttribute("client", client);
        model.addAttribute("locatii", clientService.getLocatii());
        model.addAttribute("shows", showService.getAllShows());

        // Preselect city and show if coming from recommendations
        if (city != null && !city.isEmpty()) {
            model.addAttribute("selectedCity", city);
        }
        if (showId != null) {
            model.addAttribute("selectedShowId", showId);
        }

        return "cumpara-bilet";

    }

    @PostMapping("/cumpara-bilet")
    public String cumparaBiletSubmit(@RequestParam int idClient,
                                     @RequestParam int idSpectacol,
                                     @RequestParam int rand,
                                     @RequestParam int loc,
                                     RedirectAttributes redirectAttributes) {

        try {
            clientService.createTicket(idClient, idSpectacol, rand, loc);
            redirectAttributes.addFlashAttribute("successMessage", "Biletul a fost achiziționat cu succes.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "A apărut o eroare la achiziționarea biletului.");
        }

        return "redirect:/cumpara-bilet";

    }
   


}