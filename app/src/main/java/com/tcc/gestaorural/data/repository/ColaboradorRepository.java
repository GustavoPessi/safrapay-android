package com.tcc.gestaorural.data.repository;

import android.content.Context;

import com.tcc.gestaorural.data.remote.ApiService;
import com.tcc.gestaorural.data.remote.ErroApi;
import com.tcc.gestaorural.data.remote.RetrofitClient;
import com.tcc.gestaorural.data.remote.dto.MaquinaColaboradorDto;
import com.tcc.gestaorural.data.remote.dto.TarefaDto;
import com.tcc.gestaorural.data.remote.dto.TarefaRequestDto;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/** App do colaborador (online): máquinas, tarefas, concluir/reabrir e reportar problema. */
public class ColaboradorRepository {

    public interface ListaCallback<T> {
        void onLista(List<T> itens);
        /** codigo = status HTTP (0 = sem internet). 403 = o papel mudou. */
        void onErro(String mensagem, int codigo);
    }

    public interface TarefaCallback {
        void onOk(TarefaDto tarefa);
        void onErro(String mensagem, int codigo);
    }

    private static final String SEM_INTERNET = "Sem conexão. O app do colaborador precisa de internet.";

    private final ApiService api;
    private final SessionManager session;

    public ColaboradorRepository(Context context) {
        this.api = RetrofitClient.getApi();
        this.session = new SessionManager(context);
    }

    private String bearer() { return "Bearer " + session.getToken(); }

    public void maquinas(ListaCallback<MaquinaColaboradorDto> cb) {
        api.maquinasDoColaborador(bearer()).enqueue(lista(cb, "Não consegui carregar as máquinas."));
    }

    /** filtro: TODAS | PENDENTES | ATRASADAS | FEITAS; maquinaUuid pode ser null. */
    public void tarefas(String filtro, String maquinaUuid, ListaCallback<TarefaDto> cb) {
        api.tarefasDoColaborador(bearer(), filtro, maquinaUuid).enqueue(lista(cb, "Não consegui carregar as tarefas."));
    }

    public void concluir(String tarefaUuid, TarefaCallback cb) {
        api.concluirTarefa(bearer(), tarefaUuid).enqueue(tarefa(cb, "Não consegui marcar como realizada."));
    }

    public void reabrir(String tarefaUuid, TarefaCallback cb) {
        api.reabrirTarefa(bearer(), tarefaUuid).enqueue(tarefa(cb, "Só quem concluiu a tarefa pode reabrir."));
    }

    public void reportar(String maquinaUuid, String texto, TarefaCallback cb) {
        api.reportarProblema(bearer(), maquinaUuid, new TarefaRequestDto(texto))
                .enqueue(tarefa(cb, "Não consegui reportar o problema."));
    }

    private <T> retrofit2.Callback<List<T>> lista(ListaCallback<T> cb, String padrao) {
        return new retrofit2.Callback<List<T>>() {
            @Override public void onResponse(Call<List<T>> c, Response<List<T>> r) {
                if (r.isSuccessful()) cb.onLista(r.body() != null ? r.body() : new ArrayList<>());
                else cb.onErro(ErroApi.mensagem(r, padrao), r.code());
            }
            @Override public void onFailure(Call<List<T>> c, Throwable t) { cb.onErro(SEM_INTERNET, 0); }
        };
    }

    private retrofit2.Callback<TarefaDto> tarefa(TarefaCallback cb, String padrao) {
        return new retrofit2.Callback<TarefaDto>() {
            @Override public void onResponse(Call<TarefaDto> c, Response<TarefaDto> r) {
                if (r.isSuccessful()) cb.onOk(r.body());
                else cb.onErro(ErroApi.mensagem(r, padrao), r.code());
            }
            @Override public void onFailure(Call<TarefaDto> c, Throwable t) { cb.onErro(SEM_INTERNET, 0); }
        };
    }
}
