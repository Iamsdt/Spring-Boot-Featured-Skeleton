package com.firti.webservice.services.impl;

import com.firti.webservice.commons.utils.DateUtil;
import com.firti.webservice.entities.AcValidationToken;
import com.firti.webservice.entities.User;
import com.firti.webservice.exceptions.forbidden.ForbiddenException;
import com.firti.webservice.repositories.AcValidationTokenRepository;
import com.firti.webservice.services.AcValidationTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AcValidationTokenServiceImpl implements AcValidationTokenService {
    private final AcValidationTokenRepository tokenRepo;

    @Autowired
    public AcValidationTokenServiceImpl(AcValidationTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    @Override
    public AcValidationToken save(AcValidationToken acValidationToken) {
        return this.tokenRepo.save(acValidationToken);
    }

    @Override
    public AcValidationToken findOne(Long id) {
        return this.tokenRepo.findOne(id);
    }

    @Override
    public AcValidationToken findByToken(String token) throws ForbiddenException {
        if (token == null) throw new ForbiddenException("Invalid Token");
        AcValidationToken tokenEntity = this.tokenRepo.findFirstByTokenOrderByIdDesc(token);
        if (tokenEntity == null || !tokenEntity.isTokenValid()) throw new ForbiddenException("Invalid Token");
        return tokenEntity;
    }

    @Override
    public void delete(Long id) {
        this.tokenRepo.delete(id);
    }

    @Override
    public boolean isTokenValid(String token) throws ForbiddenException {
        if (token == null || token.isEmpty()) return false;
        AcValidationToken acValidationToken = this.findByToken(token);
        return acValidationToken != null && acValidationToken.isTokenValid();
    }

    @Override
    public boolean isLimitExceeded(User user) {
        if (user == null) return true;
        Date date = new Date();
        Date fromDate = DateUtil.getDayStart(date);
        Date toDate = DateUtil.getDayEnd(date);
        return this.tokenRepo.countByUserIdAndCreatedBetween(user.getId(), fromDate, toDate) >= 3;
    }
}
