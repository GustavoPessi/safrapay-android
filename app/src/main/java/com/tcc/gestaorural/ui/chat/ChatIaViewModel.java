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
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;
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
    /** Máximo de fotos por mensagem (limite do servidor). */
    public static final int MAX_FOTOS = 5;
    /** Soma dos JPEGs de uma mensagem: 5 MB de bytes ≈ 6,7 MB em base64 (servidor aceita 7 MB). */
    public static final long MAX_BYTES_FOTOS = 5L * 1024 * 1024;
    /** Quantas fotos pendentes guardar na memória no máximo (as mais antigas saem). */
    private static final int MAX_PENDENTES = 15;

    /**
     * Fotos mandadas na conversa e ainda não usadas: número na conversa ("Foto N") → JPEG.
     * Uma foto só sai daqui quando um lançamento com aquele "foto" é confirmado (vira o
     * comprovante dele) ou quando a conversa termina (a tela fecha). Foto nova não descarta
     * as anteriores.
     */
    private final java.util.LinkedHashMap<Integer, byte[]> fotosPendentes = new java.util.LinkedHashMap<>();
    /** Contador da conversa: Foto 1, 2, 3... (só cresce, mesmo entre mensagens). */
    private int proximaFoto = 1;
    /** Fotos escolhidas/tiradas que ainda estão na bandeja, antes de enviar (sobrevive à rotação). */
    private final List<byte[]> bandeja = new ArrayList<>();

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
        enviarInterno(texto, texto, null, null, null);
    }

    // --- Bandeja de fotos (antes de enviar) -----------------------------

    public List<byte[]> getBandeja() { return bandeja; }

    /** @return false se a bandeja já está cheia ou se passaria do tamanho total permitido. */
    public boolean adicionarNaBandeja(byte[] jpeg) {
        if (bandeja.size() >= MAX_FOTOS) return false;
        long total = jpeg.length;
        for (byte[] b : bandeja) total += b.length;
        if (total > MAX_BYTES_FOTOS) return false;
        bandeja.add(jpeg);
        return true;
    }

    public void removerDaBandeja(int posicao) {
        if (posicao >= 0 && posicao < bandeja.size()) bandeja.remove(posicao);
    }

    /**
     * Envia as fotos da bandeja (numeradas na conversa) + o texto opcional, tudo numa mensagem.
     * Na bolha e no histórico: "[Foto 1] [Foto 2] <texto>" — a IA usa esses números pra dizer de
     * qual foto saiu cada lançamento (campo "foto"), inclusive nas mensagens seguintes.
     */
    public void enviarFotos(String legenda) {
        if (estaEnviando() || bandeja.isEmpty()) return;
        List<ChatMensagemRequestDto.Imagem> imagens = new ArrayList<>();
        StringBuilder marcas = new StringBuilder();
        for (byte[] jpeg : bandeja) {
            int n = proximaFoto++;
            imagens.add(new ChatMensagemRequestDto.Imagem("image/jpeg",
                    android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP), n));
            fotosPendentes.put(n, jpeg);
            marcas.append("[Foto ").append(n).append("] ");
        }
        int qtd = bandeja.size();
        bandeja.clear();
        // Não deixa a memória crescer sem limite numa conversa longa: as mais antigas saem.
        while (fotosPendentes.size() > MAX_PENDENTES) {
            fotosPendentes.remove(fotosPendentes.keySet().iterator().next());
        }

        String l = legenda == null ? "" : legenda.trim();
        String texto = marcas.toString().trim() + " "
                + (!l.isEmpty() ? l : (qtd == 1 ? "Enviei uma foto." : "Enviei " + qtd + " fotos."));
        enviarInterno("📷 " + texto, texto, null, imagens, null);
    }

    /** Arquivo (.xlsx/.csv/.txt) com legenda opcional. Arquivo não vira comprovante. */
    public void enviarArquivo(String legenda, String rotulo, ChatMensagemRequestDto.Arquivo arquivo) {
        if (estaEnviando()) return;
        String l = legenda == null ? "" : legenda.trim();
        String bolha = rotulo + (l.isEmpty() ? "" : "\n" + l);
        String paraIa = !l.isEmpty() ? l : "Enviei o arquivo " + (arquivo != null ? arquivo.getNome() : "") + ".";
        enviarInterno(bolha, paraIa, null, null, arquivo);
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
                               ChatMensagemRequestDto.Imagem imagem, List<ChatMensagemRequestDto.Imagem> imagens,
                               ChatMensagemRequestDto.Arquivo arquivo) {
        if (estaEnviando()) return;

        removerSugestoes(); // sugestões antigas deixam de valer quando o produtor responde
        itens.add(ChatItem.usuario(textoBolha));
        historico.add(new ChatMensagemRequestDto.Mensagem("usuario", textoHistorico));
        enviando.setValue(true);
        publicar();

        repo.enviar(usuarioId, new ArrayList<>(historico), imagem, imagens, arquivo, null, new ChatIaRepository.RespostaCallback() {
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
            substituirPropostasAbertas();
            itens.add(ChatItem.proposta(resp.getLancamentos()));
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
                itens.add(ChatItem.assistente("⚠️ " + (n == 1 ? "Já existe um lançamento" : "Já existem " + n + " lançamentos")
                        + " com o mesmo valor e data nessa safra. Se não for repetido, toque em \"Confirmar e lançar\" de novo."));
                publicar();
            } else {
                gravar(item);
            }
        });
    }

    private void gravar(ChatItem item) {
        final List<ChatMensagemResponseDto.Lancamento> lancamentos = item.getLancamentos();
        final int total = lancamentos.size();

        // Some com os botões já (evita gravar duas vezes num toque duplo).
        item.setResolvido(true);
        item.setRodape("Salvando…");
        publicar();

        final int[] concluidos = {0};
        final List<String> salvos = new ArrayList<>();
        final java.util.Set<Integer> fotosUsadas = new java.util.HashSet<>();
        final String[] erro = {null};
        final boolean[] guardouFoto = {false};
        for (ChatMensagemResponseDto.Lancamento l : lancamentos) {
            final Integer numeroFoto = fotoDoLancamento(l, lancamentos);
            byte[] fotoDeste = numeroFoto != null ? fotosPendentes.get(numeroFoto) : null;
            repo.confirmar(l, fotoDeste, new ChatIaRepository.GravarCallback() {
                @Override
                public void onGravado(String tipo, boolean comprovanteGuardado) {
                    salvos.add(descreverSalvo(l));
                    if (comprovanteGuardado) { guardouFoto[0] = true; fotosUsadas.add(numeroFoto); }
                    if (++concluidos[0] == total) concluir(item, salvos, fotosUsadas, erro[0], guardouFoto[0]);
                }
                @Override
                public void onErro(String mensagem) {
                    if (erro[0] == null) erro[0] = mensagem;
                    if (++concluidos[0] == total) concluir(item, salvos, fotosUsadas, erro[0], guardouFoto[0]);
                }
            });
        }
    }

    /**
     * Qual foto pendente vira comprovante deste lançamento:
     * 1. "foto" = N (backend novo) → a Foto N;
     * 2. sem "foto", mas doComprovante = true e só 1 foto pendente → ela (compatibilidade);
     * 3. backend antigo (proposta sem doComprovante nem foto) e só 1 foto pendente → ela (regra anterior);
     * senão, nenhuma.
     */
    private Integer fotoDoLancamento(ChatMensagemResponseDto.Lancamento l,
                                     List<ChatMensagemResponseDto.Lancamento> todos) {
        if (l.getFoto() != null) return fotosPendentes.containsKey(l.getFoto()) ? l.getFoto() : null;
        if (fotosPendentes.size() != 1) return null;
        Integer unica = fotosPendentes.keySet().iterator().next();
        if (Boolean.TRUE.equals(l.getDoComprovante())) return unica;
        boolean backendAntigo = true;
        for (ChatMensagemResponseDto.Lancamento x : todos) {
            if (x.getDoComprovante() != null || x.getFoto() != null) { backendAntigo = false; break; }
        }
        return backendAntigo ? unica : null;
    }

    /** Fecha a proposta depois que todos os lançamentos foram tentados. */
    private void concluir(ChatItem item, List<String> salvos, java.util.Set<Integer> fotosUsadas,
                          String erro, boolean guardouFoto) {
        // As fotos que viraram comprovante saem das pendentes (as outras continuam esperando).
        for (Integer n : fotosUsadas) fotosPendentes.remove(n);
        if (!salvos.isEmpty()) {
            // Só no histórico mandado ao servidor (o servidor não guarda a conversa): assim a IA
            // sabe o que já foi salvo e não propõe de novo.
            historico.add(new ChatMensagemRequestDto.Mensagem("assistente",
                    "(Salvo no app: " + android.text.TextUtils.join("; ", salvos) + ")"));
        }
        if (erro == null) {
            int total = salvos.size();
            item.setRodape((total == 1) ? "✓ Lançamento salvo" : "✓ " + total + " lançamentos salvos");
            itens.add(ChatItem.assistente(item.getRodape() + " no app. Já aparece no seu financeiro."
                    + (guardouFoto ? (fotosUsadas.size() > 1 ? " Guardei as fotos como comprovantes." : " Guardei a foto como comprovante.") : "")));
        } else {
            item.setRodape(salvos.isEmpty() ? "⚠️ " + erro : "⚠️ " + salvos.size() + " salvo(s); " + erro);
            itens.add(ChatItem.assistente("⚠️ " + erro));
        }
        publicar();
    }

    /** "despesa R$ 200,00 Compras no mercado 02/10/2026" (mesmos padrões que o repositório grava). */
    private static String descreverSalvo(ChatMensagemResponseDto.Lancamento l) {
        String tipo = "RECEITA".equalsIgnoreCase(l.getTipo()) ? "receita" : "despesa";
        String descricao = (l.getDescricao() != null && !l.getDescricao().isEmpty()) ? l.getDescricao() : "Lançamento via assistente";
        String data = (l.getData() != null && !l.getData().isEmpty()) ? l.getData() : DateUtils.hojeIso();
        return tipo + " " + MoneyUtils.formatar(l.getValor() != null ? l.getValor() : 0d)
                + " " + descricao + " " + DateUtils.isoParaBr(data);
    }

    public void descartar(ChatItem item) {
        if (item.getTipo() != ChatItem.PROPOSTA || item.isResolvido()) return;
        item.setResolvido(true);
        item.setRodape("Descartado");
        historico.add(new ChatMensagemRequestDto.Mensagem("assistente", "(Proposta descartada pelo produtor)"));
        itens.add(ChatItem.assistente("Ok, descartei. Pode me dizer de outro jeito se quiser."));
        publicar();
    }

    /** Proposta nova da IA: as anteriores ainda abertas deixam de valer (botões somem). */
    private void substituirPropostasAbertas() {
        for (ChatItem c : itens) {
            if (c.getTipo() == ChatItem.PROPOSTA && !c.isResolvido()) {
                c.setResolvido(true);
                c.setRodape("Substituída pela proposta abaixo");
            }
        }
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
