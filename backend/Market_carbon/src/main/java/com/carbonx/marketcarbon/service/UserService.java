package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.exception.UserException;
import com.carbonx.marketcarbon.model.User;

import java.util.List;

public interface UserService {
    public User findUserProfileByJwt(String jwt) throws UserException;
    public User findUserByEmail(String email) throws UserException;
    public List<User> findALlUser();
    void updatePassword(User user, String newPassword);
    void sendPasswordResetEmail(User user);
}
