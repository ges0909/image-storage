package com.valantic.sti.image.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // Extract user info
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        // Assign roles based on email or other criteria
        Set<GrantedAuthority> authorities = new HashSet<>(oauth2User.getAuthorities());
        
        // Example: Admin role for specific email
        if ("admin@example.com".equals(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Return user with updated authorities
        return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), "email");
    }
}