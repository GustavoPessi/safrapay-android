package com.tcc.gestaorural.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.remote.dto.ChatMensagemRequestDto;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;
import com.tcc.gestaorural.data.repository.ChatConsultaExecutor;
import com.tcc.gestaorural.data.repository.ChatIaRepository;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChatIaViewModel extends AndroidViewModel {

    private final ChatIaRepository repo;
    private final ChatConsultaExecutor executor;
    private final long usuarioId;

    private final List<ChatItem> itens = new ArrayList<>();
    private final List<ChatMensagemRequestDto.Mensagem> historico = new ArrayList<>();

    private final MutableLiveData<List<ChatItem>> lista = new MutableLiveData<>();
    private final MutableLiveData<Boolean> enviando = new MutableLiveData<>(false);

    public ChatIaViewModel(@NonNull Application app) {
        super(app);
        repo = new ChatIaRepository(app);
        executor = new ChatConsultaExecutor(app);
        usuarioId = new SessionManager(app).getUsuarioId();
    }

    public LiveData<List<ChatItem>> getLista() { return lista; }
    public LiveData<Boolean> getEnviando() { return enviando; }
    public boolean temToken() { return repo.temToken(); }

    /** Boas-vindas + atalhos (só UI, não entram no histórico mandado à IA). */
    public void saudacaoInicial(String texto, List<String> sugestoesIniciais) {
        if (!itens.isEmpty()) return;
        itens.add(ChatItem.assistente(texto));
        if (sugestoesIniciais != null && !sugestoesIniciais.isEmpty()) itens.add(ChatItem.sugestoes(sugestoesIniciais));
        publicar();
    }

    /** Toque num botão de sugestão = o produtor mandou aquele texto. */
    public void sugestao(String texto) { enviar(texto); }

    public void enviar(String texto) {
        if (texto == null) return;
        texto = texto.trim();
        if (texto.isEmpty() || Boolean.TRUE.equals(enviando.getValue())) return;

        removerSugestoes(); // sugestões antigas deixam de valer quando o produtor responde
        itens.add(ChatItem.usuario(texto));
        historico.add(new ChatMensagemRequestDto.Mensagem("usuario", texto));
        enviando.setValue(true);
        publicar();

        repo.enviar(usuarioId, new ArrayList<>(historico), new ChatIaRepository.RespostaCallback() {
            @Override
            public void onResposta(ChatMensagemResponseDto resp) { tratarResposta(resp); }
            @Override
            public void onErro(String mensagem) {
                enviando.setValue(false);
                itens.add(ChatItem.assistente("⚠️ " + mensagem));
                publicar();
            }
        });
    }

    private void tratarResposta(ChatMensagemResponseDto resp) {
        String botTexto = (resp.resposta != null) ? resp.resposta.trim() : "";
        if (!botTexto.isEmpty()) {
            itens.add(ChatItem.assistente(botTexto));
            historico.add(new ChatMensagemRequestDto.Mensagem("assistente", botTexto));
        }
        if (resp.avisos != null) {
            for (String a : resp.avisos) if (a != null && !a.trim().isEmpty()) itens.add(ChatItem.assistente("ℹ️ " + a.trim()));
        }
        if ("PROPOSTA".equalsIgnoreCase(resp.status) && resp.lancamentos != null && !resp.lancamentos.isEmpty()) {
            itens.add(ChatItem.proposta(resp.lancamentos));
        }

        if ("CONSULTA".equalsIgnoreCase(resp.status) && resp.consulta != null) {
            // A IA só disse O QUE consultar; o app calcula no Room e mostra o resultado.
            publicar();
            executor.executar(usuarioId, resp.consulta, r -> {
                itens.add(ChatItem.resultado(r));
                adicionarSugestoes(resp.sugestoes);
                enviando.setValue(false);
                publicar();
            });
            return;
        }

        adicionarSugestoes(resp.sugestoes);
        enviando.setValue(false);
        publicar();
    }

    public void confirmar(ChatItem item) {
        if (item.tipo != ChatItem.PROPOSTA || item.resolvido || item.lancamentos == null) return;
        if (item.duplicataAvisada) { gravar(item); return; }

        // Antes de gravar: já existe lançamento igual (mesma safra, valor e data)?
        repo.contarDuplicatas(item.lancamentos, n -> {
            if (n > 0) {
                item.duplicataAvisada = true;
                itens.add(ChatItem.assistente("⚠️ Já existe " + (n == 1 ? "um lançamento" : n + " lançamentos")
                        + " com o mesmo valor e data nessa safra. Se não for repetido, toque em \"Confirmar e lançar\" de novo."));
                publicar();
            } else {
                gravar(item);
            }
        });
    }

    private void gravar(ChatItem item) {
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

    private void adicionarSugestoes(List<String> sugestoes) {
        if (sugestoes == null) return;
        List<String> limpas = new ArrayList<>();
        for (String s : sugestoes) {
            if (s != null && !s.trim().isEmpty() && limpas.size() < 5) limpas.add(s.trim());
        }
        if (!limpas.isEmpty()) itens.add(ChatItem.sugestoes(limpas));
    }

    private void removerSugestoes() {
        Iterator<ChatItem> it = itens.iterator();
        while (it.hasNext()) if (it.next().tipo == ChatItem.SUGESTOES) it.remove();
    }

    private void publicar() { lista.setValue(new ArrayList<>(itens)); }
}
