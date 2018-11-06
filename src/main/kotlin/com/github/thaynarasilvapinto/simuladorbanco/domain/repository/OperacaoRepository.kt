package com.github.thaynarasilvapinto.simuladorbanco.domain.repository

import com.github.thaynarasilvapinto.simuladorbanco.domain.Conta
import com.github.thaynarasilvapinto.simuladorbanco.domain.Operacao

interface OperacaoRepository {
    fun save(operacao: Operacao): Int

    fun find(operacaoId: Int): Operacao?

    fun update(operacao: Operacao): Int

    fun delete(id: Int): Int

    fun findAllByContaOrigem(conta: Conta): List<Operacao>

    fun findAllByContaDestinoAndTipoOperacao(conta: Conta, operacao: Operacao.TipoOperacao): List<Operacao>
}