package com.github.thaynarasilvapinto.simuladorbanco

import com.github.thaynarasilvapinto.simuladorbanco.api.request.OperacaoRequest
import com.github.thaynarasilvapinto.simuladorbanco.domain.Cliente
import com.github.thaynarasilvapinto.simuladorbanco.domain.Conta
import com.github.thaynarasilvapinto.simuladorbanco.domain.Operacao
import com.github.thaynarasilvapinto.simuladorbanco.services.ClienteService
import com.github.thaynarasilvapinto.simuladorbanco.services.ContaService
import com.github.thaynarasilvapinto.simuladorbanco.services.OperacaoService
import com.google.gson.Gson
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class OperacaoControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc
    @Autowired
    private lateinit var clienteService: ClienteService
    @Autowired
    private lateinit var contaService: ContaService
    @Autowired
    private lateinit var operacaoService: OperacaoService
    private lateinit var gson: Gson
    private lateinit var joao: Cliente
    private lateinit var maria: Cliente

    private lateinit var joaoConta: Conta
    private lateinit var mariaConta: Conta

    private lateinit var operacaoDepositoJoao: Operacao
    private lateinit var operacaoSaqueJoao: Operacao
    private lateinit var operacaoTransferencia: Operacao

    @Before
    fun setUp() {
        createClient()
        this.gson = Gson()

        this.operacaoDepositoJoao = Operacao(
                contaOrigem = joaoConta.id,
                contaDestino = joaoConta.id,
                valorOperacao = 200.00,
                tipoOperacao = Operacao.TipoOperacao.DEPOSITO)
        this.operacaoSaqueJoao = Operacao(
                contaOrigem = joaoConta.id,
                contaDestino = joaoConta.id,
                valorOperacao = 100.00,
                tipoOperacao = Operacao.TipoOperacao.SAQUE)
        this.operacaoTransferencia = Operacao(
                contaOrigem = joaoConta.id,
                contaDestino = mariaConta.id,
                valorOperacao = 100.00,
                tipoOperacao = Operacao.TipoOperacao.TRANSFERENCIA)
    }

    private fun createClient() {
        joao = clienteService.criarCliente(Cliente(
                nome = "Cliente Test ClienteController",
                cpf = "055.059.396-94",
                conta = -1))
        maria = clienteService.criarCliente(Cliente(
                nome = "Cliente Test ClienteController",
                cpf = "177.082.896-67",
                conta = -1))
        joaoConta = contaService.find(joao.conta).get()
        mariaConta = contaService.find(maria.conta).get()
    }

    @After
    fun delete() {
        clienteService.delete(joao.id)
        clienteService.delete(maria.id)
        var extrato = operacaoService.findAllContaOrigem(joaoConta)
        for (i in extrato.indices) {
            operacaoService.delete(extrato[i].idOperacao)
        }
        extrato = operacaoService.findAllContaOrigem(mariaConta)
        for (i in extrato.indices) {
            operacaoService.delete(extrato[i].idOperacao)
        }
        contaService.delete(joaoConta.id)
        contaService.delete(mariaConta.id)
    }

    @Test
    fun `deve fazer deposito`() {
        val operacaoDepositoRequest = OperacaoRequest(valorOperacao = 500.00, contaDestino = null)
        val content = gson.toJson(operacaoDepositoRequest)
        this.mvc.perform(post("/conta/{id}/deposito", joaoConta.id)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk)
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.idOperacao", notNullValue()))
    }

    @Test
    fun `Nao deve depositar em uma conta que nao existe`() {
        val operacaoDepositoRequest = OperacaoRequest(valorOperacao = 500.00, contaDestino = null)
        val content = gson.toJson(operacaoDepositoRequest)
        this.mvc.perform(post("/conta/{id}/deposito", -1)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `Deve realizar saque`() {

        joaoConta.saldo = 300.00
        contaService.update(joaoConta)

        val operacaoSaqueRequest = OperacaoRequest(valorOperacao = 200.00, contaDestino = null)
        val content = gson.toJson(operacaoSaqueRequest)
        this.mvc.perform(post("/conta/{id}/saque", joaoConta.id)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk)
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.idOperacao", notNullValue()))
    }

    @Test
    fun `Nao deve realizar saque de conta que nao existe`() {
        val operacaoSaqueRequest = OperacaoRequest(valorOperacao = 200.00, contaDestino = null)
        val content = gson.toJson(operacaoSaqueRequest)
        this.mvc.perform(post("/conta/{id}/saque", -1)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun `deve realizar transferencia`() {
        joaoConta.saldo = 300.00
        contaService.update(joaoConta)

        val operacaoTransferenciaRequest = OperacaoRequest(valorOperacao = 100.00, contaDestino = mariaConta.id)
        val content = gson.toJson(operacaoTransferenciaRequest)

        this.mvc.perform(post("/conta/{id}/transferencia", joaoConta.id)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk)
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.idOperacao", notNullValue()))
    }

    @Test
    fun `Nao deve realizar deposito abaixo de 1`() {
        val operacaoDepositoRequest = OperacaoRequest(valorOperacao = -500.00, contaDestino = null)
        val content = gson.toJson(operacaoDepositoRequest)
        this.mvc.perform(post("/conta/{id}/deposito", joaoConta.id)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest)
    }

    @Test
    fun `Nao deve realizar uma saque negativo`() {
        val operacaoSaqueRequest = OperacaoRequest(valorOperacao = -200.00, contaDestino = null)
        val content = gson.toJson(operacaoSaqueRequest)
        this.mvc.perform(post("/conta/{id}/saque", joaoConta.id)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest)
    }

    @Test
    fun `Nao deve realizar transferencia negativa`() {
        val operacaoTransferenciaRequest = OperacaoRequest(valorOperacao = -100.00, contaDestino = mariaConta.id)
        val content = gson.toJson(operacaoTransferenciaRequest)

        this.mvc.perform(post("/conta/{id}/transferencia", joaoConta.id)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest)
    }

    @Test
    fun `Nao deve realizar transferencia para a mesma conta`() {
        joaoConta.saldo = 300.00
        contaService.update(joaoConta)
        val operacaoTransferenciaRequest = OperacaoRequest(valorOperacao = 300.00, contaDestino = joaoConta.id)
        val content = gson.toJson(operacaoTransferenciaRequest)
        this.mvc.perform(post("/conta/{id}/transferencia", joaoConta.id)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `Nao deve transferir para uma conta invalida`() {
        joaoConta.saldo = 300.00
        contaService.update(joaoConta)
        val operacaoTransferenciaRequest = OperacaoRequest(valorOperacao = 300.00, contaDestino = -1)
        val content = gson.toJson(operacaoTransferenciaRequest)
        this.mvc.perform(post("/conta/{id}/transferencia", joaoConta.id)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `Nao deve transferir de uma conta que nao existe`() {
        val operacaoTransferenciaRequest = OperacaoRequest(valorOperacao = 300.00, contaDestino = mariaConta.id)
        val content = gson.toJson(operacaoTransferenciaRequest)
        this.mvc.perform(post("/conta/{id}/transferencia", -1)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isUnprocessableEntity)
    }

}