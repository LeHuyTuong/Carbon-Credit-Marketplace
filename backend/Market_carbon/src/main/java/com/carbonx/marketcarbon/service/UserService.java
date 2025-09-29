package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.model.User;

import java.util.List;

public interface UserService {
    public User findUserProfileByJwt(String jwt);
    public User findUserByEmail(String email);
    public List<User> findALlUser();
    void updatePassword(User user, String newPassword);
    void sendPasswordResetEmail(User user);
}
