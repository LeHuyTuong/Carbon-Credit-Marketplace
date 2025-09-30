package com.carbonx.marketcarbon.service.impl;

import com.carbonx.marketcarbon.common.USER_ROLE;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomeUserServiceImpl implements UserDetailsService {

    private UserRepository userRepository;

    public CustomeUserServiceImpl(UserRepository userRepository) {
        this.userRepository=userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {


        User user = userRepository.findByEmail(username);

        if(user==null) {

            throw new UsernameNotFoundException("user not found with email  - "+username);
        }

        USER_ROLE role=user.getRole();

        if(role==null) role=USER_ROLE.ROLE_EV_OWNER;

        System.out.println("role  ---- "+role);

        List<GrantedAuthority> authorities=new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(role.toString()));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),user.getPasswordHash(),authorities);
    }

}

