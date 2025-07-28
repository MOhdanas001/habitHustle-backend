package com.habithustle.habithustle_backend.repository;

import com.habithustle.habithustle_backend.model.Proofs;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProofRepository extends MongoRepository<Proofs,String> {

    Optional<Proofs> findProofsById(String proofId);
}
