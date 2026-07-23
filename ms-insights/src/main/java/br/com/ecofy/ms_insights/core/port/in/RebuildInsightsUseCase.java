package br.com.ecofy.ms_insights.core.port.in;

import br.com.ecofy.ms_insights.core.application.command.RebuildInsightsCommand;
import br.com.ecofy.ms_insights.core.application.result.RebuildRunResult;

public interface RebuildInsightsUseCase {

    RebuildRunResult rebuild(RebuildInsightsCommand cmd);
}
