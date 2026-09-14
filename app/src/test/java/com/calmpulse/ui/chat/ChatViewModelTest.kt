package com.calmpulse.ui.chat

import com.calmpulse.data.model.MessageSender
import com.calmpulse.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake Repository para isolamento de testes unitários (Clean Architecture).
 * Não depende de chamadas de rede ou APIs externas.
 */
class FakeChatRepository(
    private val tokensToEmit: List<String> = listOf("Olá. ", "Estou aqui ", "com você."),
    private val shouldFail: Boolean = false
) : ChatRepository {

    var wasResetCalled = false
    var lastPromptReceived: String? = null

    override fun sendMessageStream(userPrompt: String): Flow<String> = flow {
        lastPromptReceived = userPrompt
        if (shouldFail) {
            emit("Estou aqui com você. Respire fundo devagar... já vamos continuar.")
        } else {
            for (token in tokensToEmit) {
                emit(token)
            }
        }
    }

    override fun resetChat() {
        wasResetCalled = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Redireciona o Dispatchers.Main para o testDispatcher para testes em JVM pura
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty and not streaming`() {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(fakeRepo)

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isStreaming)
        assertEquals("", state.inputText)
    }

    @Test
    fun `onInputTextChanged should update inputText in state`() {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(fakeRepo)

        viewModel.onInputTextChanged("Estou com falta de ar")

        assertEquals("Estou com falta de ar", viewModel.uiState.value.inputText)
    }

    @Test
    fun `sendMessage should create user message, stream AI response and clear input`() = runTest(testDispatcher) {
        val fakeRepo = FakeChatRepository(tokensToEmit = listOf("Estou ", "aqui."))
        val viewModel = ChatViewModel(fakeRepo)

        viewModel.onInputTextChanged("Preciso de ajuda")
        viewModel.sendMessage()

        // Avança todas as coroutines do viewModelScope
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertFalse(state.isStreaming)
        assertEquals(2, state.messages.size)

        // Verifica mensagem do usuário
        val userMsg = state.messages[0]
        assertEquals("Preciso de ajuda", userMsg.text)
        assertEquals(MessageSender.USER, userMsg.sender)

        // Verifica resposta da IA acumulada via streaming
        val aiMsg = state.messages[1]
        assertEquals("Estou aqui.", aiMsg.text)
        assertEquals(MessageSender.AI, aiMsg.sender)
        assertFalse(aiMsg.isStreaming)
    }

    @Test
    fun `blank or whitespace message should not trigger send`() = runTest(testDispatcher) {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(fakeRepo)

        viewModel.sendMessage("    ")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertFalse(viewModel.uiState.value.isStreaming)
    }

    @Test
    fun `resetChat should clear all messages and reset repository session`() = runTest(testDispatcher) {
        val fakeRepo = FakeChatRepository()
        val viewModel = ChatViewModel(fakeRepo)

        viewModel.sendMessage("Olá")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        // Executa o reset do acolhimento
        viewModel.resetChat()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertTrue(fakeRepo.wasResetCalled)
    }

    @Test
    fun `graceful fallback should emit comforting message on failure without crashing`() = runTest(testDispatcher) {
        val fakeRepo = FakeChatRepository(shouldFail = true)
        val viewModel = ChatViewModel(fakeRepo)

        viewModel.sendMessage("Falha de rede simulada")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        val aiMsg = state.messages[1]
        assertEquals("Estou aqui com você. Respire fundo devagar... já vamos continuar.", aiMsg.text)
    }
}
