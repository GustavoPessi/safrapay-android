package com.tcc.gestaorural.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.remote.dto.ChatMensagemRequestDto;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;
import com.tcc.gestaorural.data.repository.ChatIaRepository;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ChatIaViewModel extends AndroidViewModel {

    private final ChatIaRepository repo;
    private final long usuarioId;

    private final List<ChatItem> itens = new ArrayList<>();
    private final List<ChatMensagemRequestDto.Mensagem> historico = new ArrayList<>();

    private final MutableLiveData<List<ChatItem>> lista = new MutableLiveData<>();
    private final MutableLiveData<Boolean> enviando = new MutableLiveData<>(false);

    public ChatIaViewModel(@NonNull Application app) {
        super(app);
        repo = new ChatIaRepository(app);
        usuarioId = new SessionManager(app).getUsuarioId();
    }

    public LiveData<List<ChatItem>> getLista() { return lista; }
    public LiveData<Boolean> getEnviando() { return enviando; }
    public boolean temToken() { return repo.temToken(); }

    /** Mensagem de boas-vindas (só UI, não entra no histórico mandado à IA). */
    public void saudacaoInicial(String texto) {
        if (!itens.isEmpty()) return;
        itens.add(ChatItem.assistente(texto));
        publicar();
    }

    public void enviar(String texto) {
        if (texto == null) return;
        texto = texto.trim();
        if (texto.isEmpty() || Boolean.TRUE.equals(enviando.getValue())) return;

        itens.add(ChatItem.usuario(texto));
        historico.add(new ChatMensagemRequestDto.Mensagem("usuario", texto));
        enviando.setValue(true);
        publicar();

        repo.enviar(usuarioId, new ArrayList<>(historico), new ChatIaRepository.RespostaCallback() {
            @Override
            public void onResposta(ChatMensagemResponseDto resp) {
                enviando.setValue(false);
                String botTexto = (resp.resposta != null) ? resp.resposta.trim() : "";
                if (!botTexto.isEmpty()) {
                    itens.add(ChatItem.assistente(botTexto));
                    historico.add(new ChatMensagemRequestDto.Mensagem("assistente", botTexto));
                }
                if ("PROPOSTA".equalsIgnoreCase(resp.status)
                        && resp.lancamentos != null && !resp.lancamentos.isEmpty()) {
                    itens.add(ChatItem.proposta(resp.lancamentos));
                }
                publicar();
            }
            @Override
            public void onErro(String mensagem) {
                enviando.setValue(false);
                itens.add(ChatItem.assistente("⚠️ " + mensagem));
                publicar();
            }
        });
    }

    public void confirmar(ChatItem item) {
        if (item.tipo != ChatItem.PROPOSTA || item.resolvido || item.lancamentos == null) return;
        final int total = item.lancamentos.size();
        final int[] ok = {0};
        final boolean[] falhou = {false};
        for (ChatMensagemResponseDto.Lancamento l : item.lancamentos) {
            repo.confirmar(l, new ChatIaRepository.GravarCallback() {
                @Override
                public void onGravado(String tipo) {
                    ok[0]++;
                    if (ok[0] >= total && !falhou[0]) {
                        item.resolvido = true;
                        item.rodape = (total == 1) ? "✓ Lançamento salvo" : "✓ " + total + " lançamentos salvos";
                        itens.add(ChatItem.assistente(item.rodape + " no app. Já aparece no seu financeiro."));
                        publicar();
                    }
                }
                @Override
                public void onErro(String mensagem) {
                    if (falhou[0]) return;
                    falhou[0] = true;
                    item.resolvido = true;
                    item.rodape = "⚠️ " + mensagem;
                    itens.add(ChatItem.assistente("⚠️ " + mensagem));
                    publicar();
                }
            });
        }
    }

    public void descartar(ChatItem item) {
        if (item.tipo != ChatItem.PROPOSTA || item.resolvido) return;
        item.resolvido = true;
        item.rodape = "Descartado";
        itens.add(ChatItem.assistente("Ok, descartei. Pode me dizer de outro jeito se quiser."));
        publicar();
    }

    private void publicar() { lista.setValue(new ArrayList<>(itens)); }
}
