package com.shiptrack.config;

import com.shiptrack.dto.ShipmentDtos;
import com.shiptrack.model.*;
import com.shiptrack.repository.ShipmentRepository;
import com.shiptrack.repository.UserRepository;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo accounts and a handful of shipments the first time the app talks to an empty
 * database, so the dashboards have something to show. Disable with app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentService shipmentService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      ShipmentRepository shipmentRepository,
                      ShipmentService shipmentService,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.shipmentRepository = shipmentRepository;
        this.shipmentService = shipmentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already has data, skipping the demo seed");
            return;
        }

        User admin = save("Asha Rao", "admin@shiptrack.dev", Role.ADMIN, "+91 90000 11111", null);
        User operator = save("Vikram Nair", "driver@shiptrack.dev", Role.LOGISTICS_OPERATOR, "+91 90000 22222", null);
        save("Neha Gupta", "support@shiptrack.dev", Role.SUPPORT_AGENT, "+91 90000 33333", null);
        User business = save("Priya Sharma", "business@shiptrack.dev", Role.BUSINESS_CLIENT, "+91 90000 44444", "Kirana Direct");
        User customer = save("Rahul Verma", "customer@shiptrack.dev", Role.CUSTOMER, "+91 90000 55555", null);

        log.info("Seeded demo accounts. Every password is: Password123");

        UserPrincipal businessPrincipal = new UserPrincipal(business);
        UserPrincipal customerPrincipal = new UserPrincipal(customer);
        UserPrincipal operatorPrincipal = new UserPrincipal(operator);

        Address hyderabad = address("Kirana Direct Warehouse", "+91 90000 44444", "business@shiptrack.dev",
                "Plot 44, IDA Mallapur", "Hyderabad", "Telangana", "500076", 17.4065, 78.4772);
        Address bengaluru = address("Sunita Iyer", "+91 98450 12345", "sunita@example.com",
                "12 Cunningham Road", "Bengaluru", "Karnataka", "560052", 12.9716, 77.5946);
        Address mumbai = address("Arjun Desai", "+91 98200 55667", "arjun@example.com",
                "8 Carter Road, Bandra West", "Mumbai", "Maharashtra", "400050", 19.0760, 72.8777);
        Address delhi = address("Meera Kapoor", "+91 98110 44556", "meera@example.com",
                "221 Hauz Khas", "New Delhi", "Delhi", "110016", 28.6139, 77.2090);
        Address chennai = address("Karthik Raman", "+91 98400 77889", "karthik@example.com",
                "5 Besant Nagar", "Chennai", "Tamil Nadu", "600090", 13.0827, 80.2707);

        Shipment delivered = shipmentService.create(new ShipmentDtos.CreateShipmentRequest(
                hyderabad, bengaluru,
                pack("Packaged foods, 2 cartons", 2, 8.4, true), "EXPRESS",
                "Leave with building security if nobody answers"), businessPrincipal);
        advance(delivered.getId(), operatorPrincipal, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED);

        Shipment inTransit = shipmentService.create(new ShipmentDtos.CreateShipmentRequest(
                hyderabad, mumbai,
                pack("Retail display units", 5, 22.0, false), "STANDARD", null), businessPrincipal);
        advance(inTransit.getId(), operatorPrincipal, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT);
        shipmentService.assignDriver(inTransit.getId(), operator.getId(), operatorPrincipal);

        Shipment outForDelivery = shipmentService.create(new ShipmentDtos.CreateShipmentRequest(
                hyderabad, delhi,
                pack("Documents and samples", 1, 1.2, true), "EXPRESS", null), businessPrincipal);
        advance(outForDelivery.getId(), operatorPrincipal, ShipmentStatus.PICKED_UP,
                ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY);
        shipmentService.assignDriver(outForDelivery.getId(), operator.getId(), operatorPrincipal);

        Shipment personal = shipmentService.create(new ShipmentDtos.CreateShipmentRequest(
                address("Rahul Verma", "+91 90000 55555", "customer@shiptrack.dev",
                        "Flat 3B, Jubilee Hills", "Hyderabad", "Telangana", "500033", 17.4239, 78.4738),
                chennai,
                pack("Birthday gift, handle with care", 1, 3.0, true), "SAME_DAY", "Fragile"), customerPrincipal);
        advance(personal.getId(), operatorPrincipal, ShipmentStatus.PICKED_UP);

        shipmentService.create(new ShipmentDtos.CreateShipmentRequest(
                hyderabad, chennai,
                pack("Spare parts", 3, 14.5, false), "STANDARD", null), businessPrincipal);

        log.info("Seeded {} demo shipments", shipmentRepository.count());
        log.info("Sign in as admin@shiptrack.dev / Password123 to see the full operations view");
    }

    private void advance(String shipmentId, UserPrincipal actor, ShipmentStatus... statuses) {
        for (ShipmentStatus status : statuses) {
            shipmentService.updateStatus(shipmentId,
                    new ShipmentDtos.StatusUpdateRequest(status, null, null, null, null), actor);
        }
    }

    private User save(String name, String email, Role role, String phone, String company) {
        return userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .password(passwordEncoder.encode("Password123"))
                .role(role)
                .phone(phone)
                .companyName(company)
                .build());
    }

    private Address address(String contact, String phone, String email, String line1,
                            String city, String state, String postal, double lat, double lng) {
        return Address.builder()
                .contactName(contact).contactPhone(phone).email(email)
                .addressLine1(line1).city(city).state(state).postalCode(postal).country("India")
                .latitude(lat).longitude(lng)
                .build();
    }

    private PackageDetails pack(String description, int quantity, double weight, boolean signature) {
        return PackageDetails.builder()
                .description(description)
                .quantity(quantity)
                .weightKg(weight)
                .lengthCm(40d).widthCm(30d).heightCm(25d)
                .declaredValue(weight * 450)
                .fragile(signature)
                .requiresSignature(signature)
                .build();
    }
}
