package com.solnotfound.repository;

import com.solnotfound.entity.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;

interface MongoUserRepository extends MongoRepository<User, String> {}
