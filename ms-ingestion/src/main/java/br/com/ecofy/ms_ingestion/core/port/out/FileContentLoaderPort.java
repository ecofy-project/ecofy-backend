package br.com.ecofy.ms_ingestion.core.port.out;

import java.io.InputStream;

public interface FileContentLoaderPort {

    InputStream open(String storedPath);
}
