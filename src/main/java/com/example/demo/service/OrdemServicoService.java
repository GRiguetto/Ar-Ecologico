package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.example.demo.database.model.OrdemServicoEntity;
import com.example.demo.database.procedure.Procedures;

import static com.example.demo.database.procedure.Procedures.data;
import static com.example.demo.database.procedure.Procedures.decimal;
import static com.example.demo.database.procedure.Procedures.inteiro;
import static com.example.demo.database.procedure.Procedures.texto;
import com.example.demo.database.repository.IOrdemServicoRepository;
import com.example.demo.dto.OrdemServicoDto;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.RegraNegocioException;

@Service
public class OrdemServicoService {

    private final IOrdemServicoRepository repository;
    private final Referencias referencias;
    private final Procedures procedures;

    public OrdemServicoService(IOrdemServicoRepository repository, Referencias referencias, Procedures procedures){
        this.repository = repository;
        this.referencias = referencias;
        this.procedures = procedures;
    }

    /** Valida em Java (cliente, contrato e prestador existem; PF ou PJ; status) e grava pela sp_CadOrdemServico. */
    public OrdemServicoEntity salvaOrdemServico(OrdemServicoDto dados) {
        OrdemServicoEntity os = new OrdemServicoEntity();
        preencher(os, dados);

        Integer codOs = procedures.executarRetornandoCodigo("sp_CadOrdemServico",
                texto("tipoOs", os.getTipoOs()),
                data("dataAbertura", os.getDataAbertura()),
                texto("descricaoProblema", os.getDescricaoProblema()),
                inteiro("status", os.getStatus()),
                decimal("valor", os.getValor()),
                texto("obs", os.getObs()),
                texto("descricaoExecutado", os.getDescricaoExecutado()),
                data("dataExecucao", os.getDataExecucao()),
                inteiro("pessoaFCod", dados.getPessoaFisicaCod()),
                inteiro("pessoaJCod", dados.getPessoaJuridicaCod()),
                inteiro("contratoCod", dados.getContratoCod()),
                inteiro("prestadorServicoCod", dados.getPrestadorServicoCod()));

        return buscar(codOs);
    }

    public List<OrdemServicoEntity> listarTodasOrdensServico() {
        return repository.findAll();
    }

    public List<OrdemServicoEntity> verOrdemServico(Integer codOrdemServico) {
        return List.of(buscar(codOrdemServico));
    }

    public OrdemServicoEntity atualizarOrdemServico(Integer codOrdemServico, OrdemServicoDto dadosOrdemServico) {
        OrdemServicoEntity ordemServicoExistente = buscar(codOrdemServico);
        preencher(ordemServicoExistente, dadosOrdemServico);

        return repository.save(ordemServicoExistente);
    }

    public OrdemServicoEntity atualizaOrdemServicoParcial(Integer codOrdemServico, Map<String, Object> dados) {
        OrdemServicoEntity os = buscar(codOrdemServico);

        if(dados.containsKey("tipoOs")) {
            os.setTipoOs(ValoresPatch.texto(dados.get("tipoOs")));
        }
        if(dados.containsKey("valor")) {
            os.setValor(ValoresPatch.decimal("valor", dados.get("valor")));
        }
        if(dados.containsKey("obs")) {
            os.setObs(ValoresPatch.texto(dados.get("obs")));
        }
        if(dados.containsKey("dataAbertura")) {
            os.setDataAbertura(ValoresPatch.data("dataAbertura", dados.get("dataAbertura")));
        }
        if(dados.containsKey("status")) {
            os.setStatus(ValoresPatch.inteiro("status", dados.get("status")));
        }
        if(dados.containsKey("descricaoProblema")) {
            os.setDescricaoProblema(ValoresPatch.texto(dados.get("descricaoProblema")));
        }
        if(dados.containsKey("descricaoExecutado")) {
            os.setDescricaoExecutado(ValoresPatch.texto(dados.get("descricaoExecutado")));
        }
        if(dados.containsKey("dataExecucao")) {
            os.setDataExecucao(ValoresPatch.data("dataExecucao", dados.get("dataExecucao")));
        }
        if(dados.containsKey("pessoaFisicaCod")) {
            os.setPessoaFisicaCod(referencias.pessoaFisica(
                    ValoresPatch.inteiro("pessoaFisicaCod", dados.get("pessoaFisicaCod"))));
        }
        if(dados.containsKey("pessoaJuridicaCod")) {
            os.setPessoaJuridicaCod(referencias.pessoaJuridica(
                    ValoresPatch.inteiro("pessoaJuridicaCod", dados.get("pessoaJuridicaCod"))));
        }
        if(dados.containsKey("contratoCod")) {
            os.setContratoCod(referencias.contrato(
                    ValoresPatch.inteiro("contratoCod", dados.get("contratoCod"))));
        }
        if(dados.containsKey("prestadorServicoCod")) {
            os.setPrestadorServicoCod(referencias.prestador(
                    ValoresPatch.inteiro("prestadorServicoCod", dados.get("prestadorServicoCod"))));
        }
        validar(os);

        return repository.save(os);
    }

    public void deleta(Integer codOrdemServico) {
        repository.delete(buscar(codOrdemServico));
    }

    private OrdemServicoEntity buscar(Integer codOrdemServico) {
        return repository.findById(codOrdemServico)
                .orElseThrow(() -> new NotFoundException("Ordem de serviço " + codOrdemServico + " não encontrada"));
    }

    private void preencher(OrdemServicoEntity os, OrdemServicoDto dados) {
        os.setTipoOs(dados.getTipoOs());
        os.setValor(dados.getValor());
        os.setObs(dados.getObs());
        os.setDataAbertura(dados.getDataAbertura() == null ? LocalDate.now() : dados.getDataAbertura());
        os.setStatus(dados.getStatus() == null ? 1 : dados.getStatus());
        os.setDescricaoProblema(dados.getDescricaoProblema());
        os.setDescricaoExecutado(dados.getDescricaoExecutado());
        os.setDataExecucao(dados.getDataExecucao());
        os.setPessoaFisicaCod(referencias.pessoaFisica(dados.getPessoaFisicaCod()));
        os.setPessoaJuridicaCod(referencias.pessoaJuridica(dados.getPessoaJuridicaCod()));
        os.setContratoCod(referencias.contrato(dados.getContratoCod()));
        os.setPrestadorServicoCod(referencias.prestador(dados.getPrestadorServicoCod()));
        validar(os);
    }

    private void validar(OrdemServicoEntity os) {
        if ((os.getPessoaFisicaCod() == null) == (os.getPessoaJuridicaCod() == null)) {
            throw new RegraNegocioException(
                    "Informe exatamente um entre pessoaFisicaCod e pessoaJuridicaCod");
        }
        if (os.getStatus() == null || os.getStatus() < 1 || os.getStatus() > 4) {
            throw new RegraNegocioException(
                    "status da OS deve ser 1 (aberta), 2 (em andamento), 3 (concluída) ou 4 (cancelada)");
        }
    }

}
