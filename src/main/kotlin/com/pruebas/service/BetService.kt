package com.pruebas.service

import com.pruebas.model.BetDocument
import com.pruebas.repository.BetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BetService {

    @Autowired
    private lateinit var betRepository: BetRepository

    fun getBetsByUserId(userId: String) : List<BetDocument>{
        return betRepository.findAllByUserId(userId)
    }

    fun insertBet(bet: BetDocument): BetDocument{
        return betRepository.save(bet)
    }



}