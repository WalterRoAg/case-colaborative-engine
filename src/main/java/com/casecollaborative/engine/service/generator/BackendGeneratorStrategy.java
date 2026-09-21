package com.casecollaborative.engine.service.generator;

import com.casecollaborative.engine.model.entity.DiagramaUml;

public interface BackendGeneratorStrategy {
    String getTargetIdentifier();
    byte[] generarProyecto(DiagramaUml diagrama);
    String generarScriptDdl(DiagramaUml diagrama);
}
