package com.habithustle.habithustle_backend.repository;

import com.habithustle.habithustle_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends MongoRepository<User,String> {
     Optional<User> findUserByEmail(String email);
     Boolean existsByEmail(String email);
     Boolean existsByUsername(String username);
     Optional<User> findByUsername(String username);
     Optional<User> findUserById(String userId);


     @Query("{ 'username': { $regex: ?0, $options: 'i' } }")
     List<User> searchByUsername(String keyword);

     
}
