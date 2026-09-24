package com.shiptrack.security;

import com.shiptrack.model.Role;
import com.shiptrack.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Wraps the domain user so controllers can read the id and role straight off the principal. */
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public String getId() {
        return user.getId();
    }

    public Role getRole() {
        return user.getRole();
    }

    public boolean isAdmin() {
        return user.getRole() == Role.ADMIN;
    }

    public boolean isStaff() {
        return user.getRole() == Role.ADMIN
                || user.getRole() == Role.LOGISTICS_OPERATOR
                || user.getRole() == Role.SUPPORT_AGENT;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
