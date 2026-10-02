package com.tcc.gestaorural.ui.chat;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.util.ComprovanteUtils;
import com.tcc.gestaorural.util.GravadorAudio;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;
import com.tcc.gestaorural.data.repository.ChatConsultaExecutor;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Acao {
        void confirmar(ChatItem item);
        void descartar(ChatItem item);
        void sugestao(String texto);
        void abrirComprovante(ChatConsultaExecutor.Comprovante c);
        void compartilharComprovante(ChatConsultaExecutor.Comprovante c);
        void anexarComprovante(ChatConsultaExecutor.Comprovante c);
        void tocarAudio(ChatItem item);
    }

    private final List<ChatItem> itens = new ArrayList<>();
    private final Acao acao;
    /** Caminho do áudio tocando agora (mostra o ícone de pausa nessa bolha). */
    private String audioTocando;

    public void setAudioTocando(String caminho) {
        audioTocando = caminho;
        notifyDataSetChanged();
    }

    public ChatAdapter(Acao acao) { this.acao = acao; }

    public void submeter(List<ChatItem> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) { return itens.get(position).getTipo(); }
    @Override public int getItemCount() { return itens.size(); }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ChatItem.USUARIO:   return new VHTexto(inf.inflate(R.layout.item_chat_usuario, parent, false));
            case ChatItem.PROPOSTA:  return new VHProposta(inf.inflate(R.layout.item_chat_proposta, parent, false));
            case ChatItem.RESULTADO: return new VHResultado(inf.inflate(R.layout.item_chat_resultado, parent, false));
            case ChatItem.SUGESTOES: return new VHSugestoes(inf.inflate(R.layout.item_chat_sugestoes, parent, false));
            case ChatItem.COMPROVANTE: return new VHComprovante(inf.inflate(R.layout.item_chat_comprovante, parent, false));
            case ChatItem.AUDIO:     return new VHAudio(inf.inflate(R.layout.item_chat_audio, parent, false));
            default:                 return new VHTexto(inf.inflate(R.layout.item_chat_assistente, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatItem item = itens.get(position);
        if (holder instanceof VHTexto) ((VHTexto) holder).txt.setText(item.getTexto());
        else if (holder instanceof VHProposta) ((VHProposta) holder).bind(item);
        else if (holder instanceof VHResultado) ((VHResultado) holder).bind(item.getResultado());
        else if (holder instanceof VHSugestoes) ((VHSugestoes) holder).bind(item.getSugestoes());
        else if (holder instanceof VHComprovante) ((VHComprovante) holder).bind(item.getComprovante());
        else if (holder instanceof VHAudio) ((VHAudio) holder).bind(item);
    }

    static class VHTexto extends RecyclerView.ViewHolder {
        final TextView txt;
        VHTexto(@NonNull View v) { super(v); txt = v.findViewById(R.id.txtTexto); }
    }

    class VHProposta extends RecyclerView.ViewHolder {
        final TextView resumo, rodape;
        final View rowBotoes;
        final Button btnConfirmar, btnDescartar;

        VHProposta(@NonNull View v) {
            super(v);
            resumo = v.findViewById(R.id.txtResumo);
            rodape = v.findViewById(R.id.txtRodape);
            rowBotoes = v.findViewById(R.id.rowBotoes);
            btnConfirmar = v.findViewById(R.id.btnConfirmar);
            btnDescartar = v.findViewById(R.id.btnDescartar);
        }

        void bind(ChatItem item) {
            resumo.setText(montarResumo(item.getLancamentos()));
            if (item.isResolvido()) {
                rowBotoes.setVisibility(View.GONE);
                rodape.setVisibility(View.VISIBLE);
                rodape.setText(item.getRodape());
            } else {
                rowBotoes.setVisibility(View.VISIBLE);
                rodape.setVisibility(View.GONE);
                btnConfirmar.setOnClickListener(v -> acao.confirmar(item));
                btnDescartar.setOnClickListener(v -> acao.descartar(item));
            }
        }
    }

    static class VHResultado extends RecyclerView.ViewHolder {
        final TextView titulo, observacao, linhas, total;

        VHResultado(@NonNull View v) {
            super(v);
            titulo = v.findViewById(R.id.txtTitulo);
            observacao = v.findViewById(R.id.txtObservacao);
            linhas = v.findViewById(R.id.txtLinhas);
            total = v.findViewById(R.id.txtTotal);
        }

        void bind(ChatConsultaExecutor.Resultado r) {
            if (r == null) return;
            titulo.setText(r.getTitulo());
            observacao.setVisibility(r.getObservacao() == null ? View.GONE : View.VISIBLE);
            observacao.setText(r.getObservacao());
            linhas.setText(android.text.TextUtils.join("\n", r.getLinhas()));
            total.setVisibility(r.getTotal() == null ? View.GONE : View.VISIBLE);
            total.setText(r.getTotal());
        }
    }

    class VHSugestoes extends RecyclerView.ViewHolder {
        final ChipGroup grupo;

        VHSugestoes(@NonNull View v) { super(v); grupo = v.findViewById(R.id.grupoSugestoes); }

        void bind(List<String> sugestoes) {
            grupo.removeAllViews();
            if (sugestoes == null) return;
            for (String s : sugestoes) {
                Chip chip = new Chip(grupo.getContext());
                chip.setText(s);
                chip.setCheckable(false);
                chip.setClickable(true);
                chip.setChipBackgroundColor(ColorStateList.valueOf(
                        ContextCompat.getColor(grupo.getContext(), R.color.green_100)));
                chip.setTextColor(ContextCompat.getColor(grupo.getContext(), R.color.green_900));
                chip.setOnClickListener(v -> acao.sugestao(s));
                grupo.addView(chip);
            }
        }
    }

    class VHAudio extends RecyclerView.ViewHolder {
        final ImageButton btnTocar;
        final TextView duracao, transcricao;

        VHAudio(@NonNull View v) {
            super(v);
            btnTocar = v.findViewById(R.id.btnTocar);
            duracao = v.findViewById(R.id.txtDuracao);
            transcricao = v.findViewById(R.id.txtTranscricao);
        }

        void bind(ChatItem item) {
            boolean tocando = item.getCaminhoAudio() != null && item.getCaminhoAudio().equals(audioTocando);
            btnTocar.setImageResource(tocando ? R.drawable.ic_chat_pause : R.drawable.ic_chat_play);
            btnTocar.setContentDescription(btnTocar.getContext().getString(
                    tocando ? R.string.chat_audio_pausar : R.string.chat_audio_tocar));
            btnTocar.setOnClickListener(v -> acao.tocarAudio(item));
            duracao.setText(GravadorAudio.formatar(item.getDuracaoSegundos() * 1000L));
            switch (item.getEstadoTranscricao()) {
                case ChatItem.TRANSCRICAO_OK:
                    transcricao.setText("“" + item.getTexto() + "”");
                    break;
                case ChatItem.TRANSCRICAO_INDISPONIVEL:
                    transcricao.setText(R.string.chat_audio_sem_transcricao);
                    break;
                default:
                    transcricao.setText(R.string.chat_audio_transcrevendo);
            }
        }
    }

    class VHComprovante extends RecyclerView.ViewHolder {
        final ImageView img;
        final TextView legenda, semFoto;
        final Button btn;
        String carregando;

        VHComprovante(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgComprovante);
            legenda = v.findViewById(R.id.txtLegenda);
            semFoto = v.findViewById(R.id.txtSemFoto);
            btn = v.findViewById(R.id.btnComprovanteAcao);
        }

        void bind(ChatConsultaExecutor.Comprovante c) {
            if (c == null) return;
            legenda.setText(c.getLegenda());
            if (!c.temFoto()) {
                img.setVisibility(View.GONE);
                semFoto.setVisibility(View.VISIBLE);
                btn.setText(R.string.comprovante_anexar);
                btn.setOnClickListener(v -> acao.anexarComprovante(c));
                carregando = null;
                return;
            }
            img.setVisibility(View.VISIBLE);
            semFoto.setVisibility(View.GONE);
            btn.setText(R.string.comprovante_compartilhar);
            btn.setOnClickListener(v -> acao.compartilharComprovante(c));
            img.setOnClickListener(v -> acao.abrirComprovante(c));
            if (c.getCaminho().equals(carregando)) return;
            carregando = c.getCaminho();
            img.setImageDrawable(null);
            final String alvo = carregando;
            AppDatabase.IO.execute(() -> {
                Bitmap b = ComprovanteUtils.miniatura(alvo, 800);
                img.post(() -> { if (alvo.equals(carregando) && b != null) img.setImageBitmap(b); });
            });
        }
    }

    private static String montarResumo(List<ChatMensagemResponseDto.Lancamento> lancamentos) {
        StringBuilder sb = new StringBuilder();
        if (lancamentos == null) return "";
        for (int i = 0; i < lancamentos.size(); i++) {
            ChatMensagemResponseDto.Lancamento l = lancamentos.get(i);
            boolean receita = "RECEITA".equalsIgnoreCase(l.getTipo());
            sb.append("• ").append(receita ? "Receita" : "Despesa");
            sb.append(" — ").append(l.getValor() != null ? MoneyUtils.formatar(l.getValor()) : "—");
            if (l.getDescricao() != null && !l.getDescricao().isEmpty()) sb.append(" — ").append(l.getDescricao());
            if (!receita && l.getCategoria() != null && !l.getCategoria().isEmpty()) sb.append(" — ").append(l.getCategoria());
            if (l.getData() != null && !l.getData().isEmpty()) sb.append(" — ").append(DateUtils.isoParaBr(l.getData()));
            if (i < lancamentos.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
