package com.tcc.gestaorural.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tcc.gestaorural.data.local.dao.*;
import com.tcc.gestaorural.data.local.entity.*;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        Usuario.class, Propriedade.class, Talhao.class, Safra.class,
        CategoriaDespesa.class, Receita.class, Despesa.class, MetaFinanceira.class,
        Maquina.class, AnotacaoMaquina.class, Cultura.class, Anexo.class
    },
    version = 10,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UsuarioDao usuarioDao();
    public abstract PropriedadeDao propriedadeDao();
    public abstract TalhaoDao talhaoDao();
    public abstract SafraDao safraDao();
    public abstract CategoriaDespesaDao categoriaDespesaDao();
    public abstract ReceitaDao receitaDao();
    public abstract DespesaDao despesaDao();
    public abstract MetaFinanceiraDao metaFinanceiraDao();
    public abstract CulturaDao culturaDao();
    public abstract MaquinaDao maquinaDao();
    public abstract AnotacaoMaquinaDao anotacaoMaquinaDao();
    public abstract AnexoDao anexoDao();
    public abstract RelatorioExportDao relatorioExportDao(); // só consultas (exportação), não muda o schema

    private static volatile AppDatabase INSTANCE;

    /**
     * Migração 1 → 2: adiciona as tabelas de máquina/anotação e a coluna opcional
     * despesa.maquina_id. DDL escrito para casar exatamente com o schema esperado pelo Room.
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `maquina` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`uuid` TEXT NOT NULL, `usuario_id` INTEGER NOT NULL, `nome` TEXT NOT NULL, " +
                    "`marca` TEXT, `tipo` TEXT, `modelo` TEXT, `ano` TEXT, `identificacao` TEXT, " +
                    "`observacoes` TEXT, `criado_em` TEXT NOT NULL, `atualizado_em` TEXT NOT NULL, " +
                    "`sincronizado` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`usuario_id`) REFERENCES `usuario`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_maquina_uuid` ON `maquina` (`uuid`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_maquina_usuario_id` ON `maquina` (`usuario_id`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `anotacao_maquina` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`uuid` TEXT NOT NULL, `maquina_id` INTEGER NOT NULL, `texto` TEXT NOT NULL, " +
                    "`data` TEXT, `criado_em` TEXT NOT NULL, `atualizado_em` TEXT NOT NULL, " +
                    "`sincronizado` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`maquina_id`) REFERENCES `maquina`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_anotacao_maquina_uuid` ON `anotacao_maquina` (`uuid`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_anotacao_maquina_maquina_id` ON `anotacao_maquina` (`maquina_id`)");

            db.execSQL("ALTER TABLE `despesa` ADD COLUMN `maquina_id` INTEGER");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_despesa_maquina_id` ON `despesa` (`maquina_id`)");
        }
    };

    /** Migração 2 → 3: status de conclusão nas anotações de máquina. */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `anotacao_maquina` ADD COLUMN `concluida` INTEGER NOT NULL DEFAULT 0");
        }
    };

    /** Migração 3 → 4: campo CIB (Cadastro do Imóvel Rural) na propriedade. */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `propriedade` ADD COLUMN `cib` TEXT");
        }
    };

    /** Migração 4 → 5: variedade da semente na safra. */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `safra` ADD COLUMN `variedade` TEXT");
        }
    };

    /** Migração 5 → 6: campo valor (opcional) em propriedade e máquina. */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `propriedade` ADD COLUMN `valor` REAL");
            db.execSQL("ALTER TABLE `maquina` ADD COLUMN `valor` REAL");
        }
    };

    /**
     * Migração 8 → 9: coordenadas de GPS (Geolocalização Nível 1 — ponto/pin) em
     * propriedade e talhão. Colunas opcionais, aditivas — não exige reset de dados.
     */
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `propriedade` ADD COLUMN `latitude` REAL");
            db.execSQL("ALTER TABLE `propriedade` ADD COLUMN `longitude` REAL");
            db.execSQL("ALTER TABLE `talhao` ADD COLUMN `latitude` REAL");
            db.execSQL("ALTER TABLE `talhao` ADD COLUMN `longitude` REAL");
        }
    };

    /**
     * Migração 9 → 10: tabela de comprovantes (foto da nota/recibo) das despesas e receitas.
     * Tabela nova, aditiva — não mexe nos dados existentes.
     */
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `anexo` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`uuid` TEXT NOT NULL, " +
                    "`lancamento_tipo` TEXT NOT NULL, " +
                    "`lancamento_uuid` TEXT NOT NULL, " +
                    "`caminho` TEXT NOT NULL, " +
                    "`mime` TEXT NOT NULL, " +
                    "`tamanho_bytes` INTEGER NOT NULL, " +
                    "`origem` TEXT NOT NULL, " +
                    "`criado_em` TEXT NOT NULL, " +
                    "`atualizado_em` TEXT NOT NULL, " +
                    "`sincronizado` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_anexo_uuid` ON `anexo` (`uuid`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_anexo_lancamento_uuid` ON `anexo` (`lancamento_uuid`)");
        }
    };

    /** Pool para operações de banco fora da main thread (Room exige). */
    public static final ExecutorService IO = Executors.newFixedThreadPool(4);

    /** Seed das 7 categorias de despesa (schema_sistema_rural.sql). */
    public static final String[] CATEGORIAS_SEED = {
        "Máquinas", "Combustível", "Insumos", "Sementes",
        "Impostos", "Documentos", "Outras despesas operacionais"
    };

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "gestao_rural.db")
                        .addCallback(SEED_CALLBACK)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_8_9, MIGRATION_9_10)
                        .fallbackToDestructiveMigration()
                        .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Semeia as categorias de despesa se a tabela estiver vazia.
     * Rodava só em onCreate (1ª criação do banco); passou a rodar também em onOpen
     * para se autocorrigir caso a tabela fique vazia por qualquer outro motivo
     * (reset manual, migração destrutiva, estado de teste) — evita os chips de
     * categoria aparecerem em branco na tela de Nova Despesa.
     */
    private static void seedCategoriasSeVazio() {
        IO.execute(() -> {
            if (INSTANCE == null) return;
            CategoriaDespesaDao dao = INSTANCE.categoriaDespesaDao();
            if (dao.contar() == 0) {
                CategoriaDespesa[] cats = new CategoriaDespesa[CATEGORIAS_SEED.length];
                for (int i = 0; i < CATEGORIAS_SEED.length; i++) {
                    cats[i] = new CategoriaDespesa(CATEGORIAS_SEED[i]);
                }
                dao.inserirTodas(Arrays.asList(cats));
            }
        });
    }

    private static final Callback SEED_CALLBACK = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            seedCategoriasSeVazio();
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            seedCategoriasSeVazio();
        }
    };
}
