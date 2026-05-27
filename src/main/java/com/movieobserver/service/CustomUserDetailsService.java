package com.movieobserver.service;

import com.movieobserver.domain.entity.User;
import com.movieobserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new com.movieobserver.security.CustomUserDetails(
                user.getEmail(),
                user.getPasswordHash(),
                user.getRoles().stream()
                        .map(role -> {
                            String roleName = role.getName();
                            if (!roleName.toUpperCase().startsWith("ROLE_")) {
                                roleName = "ROLE_" + roleName;
                            }
                            return new SimpleGrantedAuthority(roleName.toUpperCase());
                        })
                        .collect(Collectors.toList()),
                user.getDisplayName()
        );
    }
}
