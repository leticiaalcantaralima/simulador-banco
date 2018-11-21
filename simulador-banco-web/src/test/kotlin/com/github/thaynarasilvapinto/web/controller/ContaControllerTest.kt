package com.github.thaynarasilvapinto.web.controller


import com.github.thaynarasilvapinto.model.Cliente
import com.github.thaynarasilvapinto.model.Conta
import com.github.thaynarasilvapinto.model.Operacao
import com.github.thaynarasilvapinto.service.ClienteService
import com.github.thaynarasilvapinto.service.ContaService
import com.github.thaynarasilvapinto.service.OperacaoService
import com.github.thaynarasilvapinto.web.config.ControllerBaseTest
import com.github.thaynarasilvapinto.web.utils.toResponseSaldo
import com.google.gson.Gson
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


class ContaControllerTest : ControllerBaseTest() {
    @Autowired
    private lateinit var mvc: MockMvc
    @InjectMocks
    private lateinit var clienteService: ClienteService
    @InjectMocks
    private lateinit var contaService: ContaService
    @InjectMocks
    private lateinit var operacaoService: OperacaoService
    private lateinit var joao: Cliente
    private lateinit var joaoConta: Conta
    private lateinit var gson: Gson

    @Before
    fun setUp() {
        createClient()
        this.gson = Gson()
    }

    private fun createClient() {
        joao = clienteService.criarCliente(
            Cliente(
                nome = "Cliente Test Cliente Controller",
                cpf = "055.059.396-94",
                conta = Conta(saldo = 0.00)
            )
        )
        joaoConta = joao.conta
    }

    @After
    fun tearDown() {
        clienteService.delete(joao.id)
        val extrato = operacaoService.findAllContaOrigem(joaoConta)
        for (i in extrato.indices) {
            operacaoService.delete(extrato[i].idOperacao)
        }
        contaService.delete(joaoConta.id)
    }

    @Test
    fun `Deve retornar a conta`() {
        this.mvc.perform(get("/conta/{id}", joaoConta.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))

    }

    @Test
    fun `Nao deve retornar uma conta que nao existe no banco`() {
        this.mvc.perform(get("/conta/{id}", "-1"))
            .andExpect(status().isUnprocessableEntity)

    }

    @Test
    fun `Deve retornar o saldo`() {
        val body = gson.toJson(joaoConta.toResponseSaldo())
        this.mvc.perform(get("/conta/{id}/saldo", joaoConta.id))
            .andExpect(status().isOk)
            .andExpect(content().string(body))
    }

    @Test
    fun `Nao deve retornar o saldo de uma conta que nao existe no banco`() {
        this.mvc.perform(get("/conta/{id}/saldo", "-1"))
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `Deve retornar o extrato de um cliente`() {

        val operacaoDeposito = Operacao(
            contaOrigem = joaoConta,
            contaDestino = joaoConta,
            valorOperacao = 200.00,
            tipoOperacao = Operacao.TipoOperacao.DEPOSITO
        )
        val operacaoSaque = Operacao(
            contaOrigem = joaoConta,
            contaDestino = joaoConta,
            valorOperacao = 100.00,
            tipoOperacao = Operacao.TipoOperacao.SAQUE
        )

        operacaoService.insert(operacaoDeposito)
        operacaoService.insert(operacaoSaque)

        this.mvc.perform(get("/conta/{id}/extrato", joaoConta.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
    }

    @Test
    fun `Nao deve retornar o extrato de um cliente que nao existe no banco`() {
        this.mvc.perform(get("/conta/{id}/extrato", "-1"))
            .andExpect(status().isUnprocessableEntity)
    }
}