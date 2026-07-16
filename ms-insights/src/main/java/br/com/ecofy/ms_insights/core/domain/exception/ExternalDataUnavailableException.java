package br.com.ecofy.ms_insights.core.domain.exception;

/**
 * Correção Dia 8 (item #7): sinaliza que uma integração externa (ex.: ms-categorization, ms-budgeting)
 * estava HABILITADA mas FALHOU. Antes os clients retornavam {@code List.of()} em falha, transformando
 * um erro externo em "sem dados" e gerando insights potencialmente incorretos com aparência de sucesso.
 *
 * <p>Distinção importante:
 * <ul>
 *   <li>client desabilitado por config -&gt; lista vazia legítima (não lança);</li>
 *   <li>sem dados legítimo -&gt; lista vazia (não lança);</li>
 *   <li>falha externa (client habilitado) -&gt; esta exceção (falha observável, mapeada para 503).</li>
 * </ul>
 */
public class ExternalDataUnavailableException extends RuntimeException {

    private final String source;

    public ExternalDataUnavailableException(String source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
