package com.app.shared.model.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserHierarchyTest {
    @Test
    void userShouldExposeBasicState() {
        User user = new User("john", "secret", "john@example.com", "USER");

        assertThat(user.getUsername()).isEqualTo("john");
        assertThat(user.getPassword()).isEqualTo("secret");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(user.toString()).contains("username='john'");

        user.setUsername("johnny");
        user.setPassword("new-secret");
        user.setEmail("johnny@example.com");
        user.setRole("MEMBER");
        user.setId("user-1");

        assertThat(user.getId()).isEqualTo("user-1");
        assertThat(user.getUsername()).isEqualTo("johnny");
        assertThat(user.getPassword()).isEqualTo("new-secret");
        assertThat(user.getEmail()).isEqualTo("johnny@example.com");
        assertThat(user.getRole()).isEqualTo("MEMBER");
    }

    @Test
    void bidderShouldTrackBalancesAndCapabilities() {
        Bidder bidder = new Bidder("alice", "secret", "alice@example.com", 2_000.0, 250.0);

        assertThat(bidder.canBid()).isTrue();
        assertThat(bidder.canSell()).isFalse();
        assertThat(bidder.isAdmin()).isFalse();
        assertThat(bidder.getBalance()).isEqualTo(2_000.0);
        assertThat(bidder.getReservedBalance()).isEqualTo(250.0);
        assertThat(bidder.getRole()).isEqualTo("BIDDER");
    }

    @Test
    void sellerShouldTrackRevenueAndCapability() {
        Seller seller = new Seller("seller", "secret", "seller@example.com", 300.0);

        assertThat(seller.canSell()).isTrue();
        assertThat(seller.canBid()).isFalse();
        assertThat(seller.getTotalRevenue()).isEqualTo(300.0);
        assertThat(seller.getRole()).isEqualTo("SELLER");

        seller.collectRevenue(450.0);
        assertThat(seller.getTotalRevenue()).isEqualTo(750.0);
    }

    @Test
    void adminShouldHaveAllPrivileges() {
        Admin admin = new Admin("admin", "secret", "admin@example.com");

        assertThat(admin.canBid()).isTrue();
        assertThat(admin.canSell()).isTrue();
        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.getRole()).isEqualTo("ADMIN");
    }
}
