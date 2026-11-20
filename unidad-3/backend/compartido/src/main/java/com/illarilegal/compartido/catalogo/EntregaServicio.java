package com.illarilegal.compartido.catalogo;

/** Contrato de entrega que se guarda junto al precio de cada detalle comprado. */
public enum EntregaServicio {
    ASESORIA,
    ARCHIVO;

    public static EntregaServicio resolver(String tipo, String categoria) {
        if (tipo != null && !tipo.isBlank()) return valueOf(tipo);
        String text = java.text.Normalizer.normalize(categoria == null ? "" : categoria,
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
        return text.contains("asesor") ? ASESORIA : ARCHIVO;
    }

    public static EntregaServicio historico(String nombre) {
        String text = java.text.Normalizer.normalize(nombre == null ? "" : nombre,
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
        return text.contains("asesor") || text.contains("consulta") || text.contains("revision de documentos")
                ? ASESORIA : ARCHIVO;
    }
}
