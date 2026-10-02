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
    /**
     * Última foto mandada no chat, esperando a IA transformar em proposta (pode levar
     * algumas mensagens se a IA perguntar algo antes). Ao confirmar, vira comprovante.
     */
    private byte[] fotoPendente;

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

    public boolean estaEnviando() { return Boolean.TRUE.equals(enviando.getValue()); }

    public void enviar(String texto) {
        if (texto == null) return;
        texto = texto.trim();
        if (texto.isEmpty()) return;
        enviarInterno(texto, texto, null, null);
    }

    /**
     * Foto ou arquivo (com legenda opcional). Na bolha aparece o rótulo do anexo; no
     * histórico mandado à IA vai só texto (o servidor não reenvia anexos antigos).
     */
    public void enviarAnexo(String legenda, String rotulo,
                            ChatMensagemRequestDto.Imagem imagem, ChatMensagemRequestDto.Arquivo arquivo,
                            byte[] fotoJpeg) {
        if (estaEnviando()) return;
        if (imagem != null) fotoPendente = fotoJpeg; // arquivo e áudio não viram comprovante
        String l = legenda == null ? "" : legenda.trim();
        String bolha = rotulo + (l.isEmpty() ? "" : "\n" + l);
        String paraIa = !l.isEmpty() ? l
                : (imagem != null ? "Enviei uma foto." : "Enviei o arquivo " + (arquivo != null ? arquivo.getNome() : "") + ".");
        enviarInterno(bolha, paraIa, imagem, arquivo);
    }

    /**
     * Mensagem de voz: a bolha mostra o áudio; o servidor transcreve e devolve o texto, que
     * aparece embaixo da bolha e passa a valer no histórico (as próximas mensagens têm contexto).
     */
    public void enviarAudio(String caminho, String mime, int duracaoSegundos, String base64) {
        if (estaEnviando()) return;
        removerSugestoes();
        final ChatItem bolha = ChatItem.audio(caminho, duracaoSegundos);
        itens.add(bolha);
        final ChatMensagemRequestDto.Mensagem msg =
                new ChatMensagemRequestDto.Mensagem("usuario", "(mensagem de voz)");
        historico.add(msg);
        enviando.setValue(true);
        publicar();

        repo.enviar(usuarioId, new ArrayList<>(historico), null, null,
                new ChatMensagemRequestDto.Audio(mime, base64, duracaoSegundos),
                new ChatIaRepository.RespostaCallback() {
                    @Override
                    public void onResposta(ChatMensagemResponseDto resp) {
                        String t = resp.getTranscricao() == null ? "" : resp.getTranscricao().trim();
                        if (!t.isEmpty()) {
                            bolha.setTexto(t);
                            bolha.setEstadoTranscricao(ChatItem.TRANSCRICAO_OK);
                            msg.setTexto(t);
                        } else {
                            bolha.setEstadoTranscricao(ChatItem.TRANSCRICAO_INDISPONIVEL);
                        }
                        tratarResposta(resp);
                    }
                    @Override
                    public void onErro(String mensagem) {
                        bolha.setEstadoTranscricao(ChatItem.TRANSCRICAO_INDISPONIVEL);
                        enviando.setValue(false);
                        itens.add(ChatItem.assistente("⚠️ " + mensagem));
                        publicar();
                    }
                });
    }

    private void enviarInterno(String textoBolha, String textoHistorico,
                               ChatMensagemRequestDto.Imagem imagem, ChatMensagemRequestDto.Arquivo arquivo) {
        if (estaEnviando()) return;

        removerSugestoes(); // sugestões antigas deixam de valer quando o produtor responde
        itens.add(ChatItem.usuario(textoBolha));
        historico.add(new ChatMensagemRequestDto.Mensagem("usuario", textoHistorico));
        enviando.setValue(true);
        publicar();

        repo.enviar(usuarioId, new ArrayList<>(historico), imagem, arquivo, new ChatIaRepository.RespostaCallback() {
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
        String botTexto = (resp.getResposta() != null) ? resp.getResposta().trim() : "";
        if (!botTexto.isEmpty()) {
            itens.add(ChatItem.assistente(botTexto));
            historico.add(new ChatMensagemRequestDto.Mensagem("assistente", botTexto));
        }
        if (resp.getAvisos() != null) {
            for (String a : resp.getAvisos()) if (a != null && !a.trim().isEmpty()) itens.add(ChatItem.assistente("ℹ️ " + a.trim()));
        }
        if ("PROPOSTA".equalsIgnoreCase(resp.getStatus()) && resp.getLancamentos() != null && !resp.getLancamentos().isEmpty()) {
            ChatItem proposta = ChatItem.proposta(resp.getLancamentos());
            proposta.setFotoJpeg(fotoPendente);
            fotoPendente = null;
            itens.add(proposta);
        }

        // A foto só segue esperando enquanto a IA ainda está montando o lançamento (PRECISA_INFORMACAO).
        if ("SEM_LANCAMENTO".equalsIgnoreCase(resp.getStatus()) || "CONSULTA".equalsIgnoreCase(resp.getStatus())) {
            fotoPendente = null;
        }

        if ("CONSULTA".equalsIgnoreCase(resp.getStatus()) && resp.getConsulta() != null) {
            // A IA só disse O QUE consultar; o app calcula no Room e mostra o resultado.
            publicar();
            executor.executar(usuarioId, resp.getConsulta(), r -> {
                if (r.getComprovantes() != null) {
                    // COMPROVANTE: mensagem (se houver) + um card por foto achada.
                    if (!r.getLinhas().isEmpty()) itens.add(ChatItem.assistente(android.text.TextUtils.join("\n", r.getLinhas())));
                    for (ChatConsultaExecutor.Comprovante c : r.getComprovantes()) itens.add(ChatItem.comprovante(c));
                } else {
                    itens.add(ChatItem.resultado(r));
                }
                adicionarSugestoes(resp.getSugestoes());
                enviando.setValue(false);
                publicar();
            });
            return;
        }

        adicionarSugestoes(resp.getSugestoes());
        enviando.setValue(false);
        publicar();
    }

    public void confirmar(ChatItem item) {
        if (item.getTipo() != ChatItem.PROPOSTA || item.isResolvido() || item.getLancamentos() == null) return;
        if (item.isDuplicataAvisada()) { gravar(item); return; }

        // Antes de gravar: já existe lançamento igual (mesma safra, valor e data)?
        repo.contarDuplicatas(item.getLancamentos(), n -> {
            if (n > 0) {
                item.setDuplicataAvisada(true);
                itens.add(ChatItem.assistente("⚠️ Já existe " + (n == 1 ? "um lançamento" : n + " lançamentos")
                        + " com o mesmo valor e data nessa safra. Se não for repetido, toque em \"Confirmar e lançar\" de novo."));
                publicar();
            } else {
                gravar(item);
            }
        });
    }

    private void gravar(ChatItem item) {
        final int total = item.getLancamentos().size();
        final int[] ok = {0};
        final boolean[] falhou = {false};
        final boolean[] guardouFoto = {false};
        for (ChatMensagemResponseDto.Lancamento l : item.getLancamentos()) {
            repo.confirmar(l, item.getFotoJpeg(), new ChatIaRepository.GravarCallback() {
                @Override
                public void onGravado(String tipo, boolean comprovanteGuardado) {
                    ok[0]++;
                    if (comprovanteGuardado) guardouFoto[0] = true;
                    if (ok[0] >= total && !falhou[0]) {
                        item.setResolvido(true);
                        item.setFotoJpeg(null); // já está no arquivo; libera a memória
                        item.setRodape((total == 1) ? "✓ Lançamento salvo" : "✓ " + total + " lançamentos salvos");
                        itens.add(ChatItem.assistente(item.getRodape() + " no app. Já aparece no seu financeiro."
                                + (guardouFoto[0] ? " Guardei a foto como comprovante." : "")));
                        publicar();
                    }
                }
                @Override
                public void onErro(String mensagem) {
                    if (falhou[0]) return;
                    falhou[0] = true;
                    item.setResolvido(true);
                    item.setRodape("⚠️ " + mensagem);
                    itens.add(ChatItem.assistente("⚠️ " + mensagem));
                    publicar();
                }
            });
        }
    }

    public void descartar(ChatItem item) {
        if (item.getTipo() != ChatItem.PROPOSTA || item.isResolvido()) return;
        item.setResolvido(true);
        item.setRodape("Descartado");
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
        while (it.hasNext()) if (it.next().getTipo() == ChatItem.SUGESTOES) it.remove();
    }

    private void publicar() { lista.setValue(new ArrayList<>(itens)); }
}
