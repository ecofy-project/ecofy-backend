package br.com.ecofy.ms_insights.core.port.in;

import br.com.ecofy.ms_insights.core.application.result.RebuildRunResult;
import br.com.ecofy.ms_insights.core.port.out.PageResult;

import java.util.UUID;

public interface RebuildRunQueryUseCase {

    RebuildRunResult getStatus(UUID runId);

    PageResult<RebuildRunResult> listByUser(UUID userId, int page, int size);
}
