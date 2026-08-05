package com.dabhaejwo.domain.provider.repository;

import com.dabhaejwo.domain.provider.entity.ProviderCredential;
import com.dabhaejwo.global.llm.LlmProviderName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderCredentialRepository
        extends JpaRepository<ProviderCredential, LlmProviderName> {

    List<ProviderCredential> findAllByOrderByProviderAsc();
}
