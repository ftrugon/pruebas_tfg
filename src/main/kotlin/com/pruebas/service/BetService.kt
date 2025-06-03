package com.pruebas.service

import com.pruebas.model.BetDocument
import com.pruebas.repository.BetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * clase que se encarga de la loguica de negocio de las aapuestas
 */
@Service
class BetService {

    @Autowired
    private lateinit var betRepository: BetRepository

    /**
     * obtiene todas las apuestas hechas por un cliente
     * @param userId el usuario del que se quieren obtener las apuestas
     * @return la lista de apuestas
     */
    fun getBetsByUserId(userId: String) : List<BetDocument>{
        return betRepository.findAllByUserId(userId)
    }

    /**
     * inserta una [BetDocument] a la base de datos
     * @param bet la apuesta que se quiere guardar
     * @return la apuesta hecha
     */
    fun insertBet(bet: BetDocument): BetDocument{
        return betRepository.save(bet)
    }



}