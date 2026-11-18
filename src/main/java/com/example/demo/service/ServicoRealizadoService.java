package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.database.model.ServicoRealizadoEntity;
import com.example.demo.database.model.ServicoRealizadoId;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.decimal;
import static com.example.demo.database.procedure.Procedures.inteiro;
import static com.example.demo.database.procedure.Procedures.texto;
import com.example.demo.database.repository.IServicoRealizadoRepository;
import com.example.demo.dto.ServicoRealizadoDto;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.RegraNegocioException;

@Service
public class ServicoRealizadoService {

    private final IServicoRealizadoRepository repository;
    private final Referencias referencias;
    private final Procedures procedures;

    public ServicoRealizadoService(IServicoRealizadoRepository repository, Referencias referencias,
            Procedures procedures) {
        this.repository = repository;
        this.referencias = referencias;
        this.procedures = procedures;
    }

    public ServicoRealizadoEntity salvaServicoRealizado(ServicoRealizadoDto dados) {
        if (dados.getOsCod() == null || dados.getServicoCod() == null) {
            throw new RegraNegocioException("Escolha a ordem de serviço e o serviço.");
        }
        ServicoRealizadoId id = new ServicoRealizadoId(dados.getOsCod(), dados.getServicoCod());
        if (repository.existsById(id)) {
            throw new RegraNegocioException("Esse serviço já foi lançado na OS " + dados.getOsCod()
                    + ". Edite o lançamento existente em vez de cadastrar de novo.");
        }

        ServicoRealizadoEntity servicoRealizado = new ServicoRealizadoEntity(
                referencias.ordemServico(dados.getOsCod()),
                referencias.servico(dados.getServicoCod()),
                dados.getQtd(),
                dados.getValor(),
                dados.getDesconto(),
                dados.getObs(),
                dados.getFormaPagamento());
        validar(servicoRealizado);

        // Gravação pela procedure sp_CadServicoRealizado (chave composta: não devolve código).
        procedures.executar("sp_CadServicoRealizado",
                inteiro("osCod", dados.getOsCod()),
                inteiro("servicoCod", dados.getServicoCod()),
                inteiro("qtd", servicoRealizado.getQtd()),
                decimal("valor", servicoRealizado.getValor()),
                texto("formaPagamento", servicoRealizado.getFormaPagamento()),
                decimal("desconto", servicoRealizado.getDesconto()),
                texto("obs", servicoRealizado.getObs()));

        return buscar(id);
    }

    public List<ServicoRealizadoEntity> listarTodosServicosRealizados() {
        return repository.findAll();
    }

    public List<ServicoRealizadoEntity> verServicoRealizado(ServicoRealizadoId id) {
        return List.of(buscar(id));
    }

    public ServicoRealizadoEntity atualizarServicoRealizado(ServicoRealizadoId id, ServicoRealizadoDto dados) {
        ServicoRealizadoEntity existente = buscar(id);

        existente.setQtd(dados.getQtd());
        existente.setValor(dados.getValor());
        existente.setDesconto(dados.getDesconto() == null ? BigDecimal.ZERO : dados.getDesconto());
        existente.setObs(dados.getObs());
        existente.setFormaPagamento(dados.getFormaPagamento());
        validar(existente);

        return repository.save(existente);
    }

    public ServicoRealizadoEntity atualizaServicoRealizadoParcial(ServicoRealizadoId id, Map<String, Object> dados) {
        ServicoRealizadoEntity existente = buscar(id);

        if (dados.containsKey("qtd")) {
            existente.setQtd(ValoresPatch.inteiro("qtd", dados.get("qtd")));
        }
        if (dados.containsKey("valor")) {
            existente.setValor(ValoresPatch.decimal("valor", dados.get("valor")));
        }
        if (dados.containsKey("desconto")) {
            BigDecimal desconto = ValoresPatch.decimal("desconto", dados.get("desconto"));
            existente.setDesconto(desconto == null ? BigDecimal.ZERO : desconto);
        }
        if (dados.containsKey("obs")) {
            existente.setObs(ValoresPatch.texto(dados.get("obs")));
        }
        if (dados.containsKey("formaPagamento")) {
            existente.setFormaPagamento(ValoresPatch.texto(dados.get("formaPagamento")));
        }
        validar(existente);

        return repository.save(existente);
    }

    public void deletaServicoRealizado(ServicoRealizadoId id) {
        repository.delete(buscar(id));
    }

    private ServicoRealizadoEntity buscar(ServicoRealizadoId id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço " + id.getServicoCod()
                        + " não encontrado na OS " + id.getOsCod()));
    }

    /** Mesmas regras dos CHECKs de SERVICOS_REALIZADOS, com mensagem clara. */
    private void validar(ServicoRealizadoEntity sr) {
        if (sr.getQtd() == null || sr.getQtd() <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }
        if (sr.getValor() == null || sr.getValor().signum() < 0) {
            throw new RegraNegocioException("Informe o valor unitário (não pode ser negativo).");
        }
        if (sr.getDesconto().signum() < 0) {
            throw new RegraNegocioException("O desconto não pode ser negativo.");
        }
        BigDecimal total = sr.getValor().multiply(BigDecimal.valueOf(sr.getQtd()));
        if (sr.getDesconto().compareTo(total) > 0) {
            throw new RegraNegocioException("O desconto (R$ " + sr.getDesconto().setScale(2, java.math.RoundingMode.HALF_UP)
                    + ") não pode ser maior que o total do item (R$ " + total.setScale(2, java.math.RoundingMode.HALF_UP) + ").");
        }
        if (sr.getFormaPagamento() == null || sr.getFormaPagamento().isBlank()) {
            throw new RegraNegocioException("Escolha a forma de pagamento.");
        }
    }
}
