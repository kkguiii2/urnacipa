package com.cipa.votacao.service;

import com.cipa.votacao.dto.importacao.DetalheImportacaoEleitorDto;
import com.cipa.votacao.dto.importacao.LinhaImportacaoEleitorDto;
import com.cipa.votacao.dto.importacao.PlanilhaEleitoresDto;
import com.cipa.votacao.dto.importacao.ResultadoImportacaoEleitoresDto;
import com.cipa.votacao.dto.importacao.StatusImportacaoEleitor;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orquestra leitura, validação, detecção de duplicidades e persistência parcial
 * de eleitores importados de uma planilha.
 */
@Service
@RequiredArgsConstructor
public class ImportacaoEleitoresService {

    private static final int MATRICULA_MAX_LENGTH = 20;
    private static final int NOME_MAX_LENGTH = 255;

    private final UsuarioRepository usuarioRepository;
    private final PersistenciaEleitorService persistenciaEleitorService;
    private final LeitorPlanilhaEleitores leitorPlanilhaEleitores;

    /**
     * Processa todo o arquivo estruturalmente antes de persistir linhas válidas
     * e devolve a classificação de cada linha.
     *
     * @throws com.cipa.votacao.exception.PlanilhaImportacaoException se o
     *         arquivo ou sua estrutura não puderem ser aceitos
     */
    public ResultadoImportacaoEleitoresDto importar(MultipartFile arquivo) {
        long inicio = System.nanoTime();
        PlanilhaEleitoresDto planilha = leitorPlanilhaEleitores.ler(arquivo);
        List<LinhaProcessamento> linhas = validar(planilha.linhas());
        Set<String> existentes = buscarMatriculasExistentes(linhas);
        persistir(linhas, existentes);

        List<DetalheImportacaoEleitorDto> detalhes = linhas.stream()
                .map(LinhaProcessamento::detalhe)
                .toList();
        long duracaoMillis = (System.nanoTime() - inicio) / 1_000_000;
        return ResultadoImportacaoEleitoresDto.criar(planilha.nomeArquivo(), duracaoMillis, detalhes);
    }

    private List<LinhaProcessamento> validar(List<LinhaImportacaoEleitorDto> linhas) {
        List<LinhaProcessamento> processadas = new ArrayList<>();
        Map<String, Integer> primeiraLinhaPorMatricula = new LinkedHashMap<>();

        for (LinhaImportacaoEleitorDto linha : linhas) {
            LinhaProcessamento atual = new LinhaProcessamento(linha);
            String motivo = validarLinha(linha);
            if (motivo != null) {
                atual.definir(StatusImportacaoEleitor.INVALIDO, motivo);
            } else {
                Integer primeiraLinha = primeiraLinhaPorMatricula.putIfAbsent(linha.matricula(), linha.linha());
                if (primeiraLinha != null) {
                    atual.definir(
                            StatusImportacaoEleitor.DUPLICADO_NA_PLANILHA,
                            "Matrícula repetida na linha " + primeiraLinha + ".");
                }
            }
            processadas.add(atual);
        }
        return processadas;
    }

    private String validarLinha(LinhaImportacaoEleitorDto linha) {
        if (linha.erroLeitura() != null) {
            return linha.erroLeitura();
        }
        if (linha.matricula().isBlank()) {
            return "Matrícula não informada.";
        }
        if (linha.nome().isBlank()) {
            return "Nome não informado.";
        }
        if (linha.matricula().length() > MATRICULA_MAX_LENGTH) {
            return "Matrícula ultrapassa o limite permitido de 20 caracteres.";
        }
        if (!linha.matricula().matches("^[0-9]+$")) {
            return "Formato de matrícula inválido.";
        }
        if (linha.nome().length() > NOME_MAX_LENGTH) {
            return "Nome ultrapassa o limite permitido de 255 caracteres.";
        }
        return null;
    }

    private Set<String> buscarMatriculasExistentes(List<LinhaProcessamento> linhas) {
        Set<String> matriculas = new HashSet<>();
        for (LinhaProcessamento linha : linhas) {
            if (linha.pendente()) {
                matriculas.add(linha.matricula());
            }
        }
        if (matriculas.isEmpty()) {
            return Set.of();
        }
        Collection<Usuario> usuarios = usuarioRepository.findByMatriculaIn(matriculas);
        Set<String> existentes = new HashSet<>();
        for (Usuario usuario : usuarios) {
            existentes.add(usuario.getMatricula());
        }
        return existentes;
    }

    private void persistir(List<LinhaProcessamento> linhas, Set<String> existentes) {
        for (LinhaProcessamento linha : linhas) {
            if (!linha.pendente()) {
                continue;
            }
            if (existentes.contains(linha.matricula())) {
                linha.definir(StatusImportacaoEleitor.JA_CADASTRADO, "Matrícula já cadastrada.");
                continue;
            }

            try {
                persistenciaEleitorService.salvarNovo(novoUsuario(linha));
                linha.definir(StatusImportacaoEleitor.IMPORTADO, "Usuário cadastrado com sucesso.");
            } catch (DataIntegrityViolationException e) {
                if (isUniqueViolation(e)) {
                    linha.definir(StatusImportacaoEleitor.JA_CADASTRADO, "Matrícula já cadastrada.");
                } else {
                    linha.definir(StatusImportacaoEleitor.ERRO, "Não foi possível cadastrar esta matrícula.");
                }
            } catch (RuntimeException e) {
                linha.definir(StatusImportacaoEleitor.ERRO, "Não foi possível cadastrar esta matrícula.");
            }
        }
    }

    private boolean isUniqueViolation(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private Usuario novoUsuario(LinhaProcessamento linha) {
        Usuario usuario = new Usuario();
        usuario.setMatricula(linha.matricula());
        usuario.setNome(linha.nome());
        usuario.setAtivo(true);
        usuario.setVotou(false);
        return usuario;
    }

    private static final class LinhaProcessamento {
        private final LinhaImportacaoEleitorDto linha;
        private StatusImportacaoEleitor status;
        private String motivo;

        private LinhaProcessamento(LinhaImportacaoEleitorDto linha) {
            this.linha = linha;
        }

        private boolean pendente() {
            return status == null;
        }

        private String matricula() {
            return linha.matricula();
        }

        private String nome() {
            return linha.nome();
        }

        private void definir(StatusImportacaoEleitor status, String motivo) {
            this.status = status;
            this.motivo = motivo;
        }

        private DetalheImportacaoEleitorDto detalhe() {
            return new DetalheImportacaoEleitorDto(
                    linha.linha(), linha.matricula(), linha.nome(), status, motivo);
        }
    }
}
